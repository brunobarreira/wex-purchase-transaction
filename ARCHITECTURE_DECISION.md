# Hexagonal Architecture Decision Document

## WEX Purchase Transaction API - Architecture Decision Record (ADR)

---

## 1. Executive Summary

This document explains the decision to adopt **Hexagonal Architecture (Ports & Adapters)** over the traditional **Layered Architecture (Controller → Service → Repository)** for the WEX Purchase Transaction API.

**Key Decision:** Hexagonal Architecture was chosen to ensure complete isolation of business logic from external dependencies (database, Treasury API), enabling superior testability, maintainability, and flexibility.

---

## 2. Architecture Comparison

### 2.1 Traditional Layered Architecture

```
┌─────────────────────────────────────┐
│           CONTROLLER                │  ← Depends on Service (concrete)
│         (REST Endpoints)            │
└──────────────┬──────────────────────┘
               │ calls directly
               ▼
┌─────────────────────────────────────┐
│            SERVICE                  │  ← Depends on Repository (Spring Data)
│       (Business Logic)              │
└──────────────┬──────────────────────┘
               │ calls directly
               ▼
┌─────────────────────────────────────┐
│          REPOSITORY                 │  ← Coupled to JPA/Database
│      (Spring Data JPA)              │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│           DATABASE                  │
└─────────────────────────────────────┘
```

**Problems with Layered Architecture:**
- All dependencies point **downward** (toward infrastructure)
- Service layer is **tightly coupled** to Spring Data JPA
- Business logic is **mixed** with framework code
- Difficult to test without database/Spring context
- Changing database requires modifying Service layer

---

### 2.2 Hexagonal Architecture (Ports & Adapters)

```
                    ┌─────────────────────────────────────┐
                    │         DRIVING ADAPTERS            │
                    │     (REST Controller, CLI, etc.)    │
                    └──────────────┬──────────────────────┘
                                   │ uses
                                   ▼
                    ┌─────────────────────────────────────┐
                    │          PORTS (Interfaces)         │
                    │      StorePurchaseTransactionUseCase│
                    │    RetrievePurchaseTransactionUseCase│
                    └──────────────┬──────────────────────┘
                                   │ implemented by
┌──────────────────────────────────┼──────────────────────────────────┐
│                                  ▼                                  │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                      APPLICATION CORE                          │  │
│  │  ┌─────────────────────────────────────────────────────────┐  │  │
│  │  │                       DOMAIN                             │  │  │
│  │  │    PurchaseTransaction (Entity)                         │  │  │
│  │  │    Money (Value Object)                                 │  │  │
│  │  │    Business Rules & Validations                         │  │  │
│  │  └─────────────────────────────────────────────────────────┘  │  │
│  │                              │                                 │  │
│  │  ┌─────────────────────────────────────────────────────────┐  │  │
│  │  │                   APPLICATION SERVICES                   │  │  │
│  │  │    StorePurchaseTransactionService                      │  │  │
│  │  │    RetrievePurchaseTransactionService                   │  │  │
│  │  └─────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                  │                                  │
│                                  │ depends on (interfaces only)     │
│                                  ▼                                  │
│                    ┌─────────────────────────────────────┐          │
│                    │       PORTS (Interfaces)            │          │
│                    │  PurchaseTransactionRepository      │          │
│                    │  ExchangeRatePort                   │          │
│                    └─────────────────────────────────────┘          │
└──────────────────────────────────┼──────────────────────────────────┘
                                   │ implemented by
                                   ▼
                    ┌─────────────────────────────────────┐
                    │        DRIVEN ADAPTERS              │
                    │  PurchaseTransactionRepositoryAdapter│
                    │  TreasuryApiClient                  │
                    └─────────────────────────────────────┘
```

**Key Principle:** Dependencies point **INWARD** (toward the domain).

---

## 3. Why Hexagonal Architecture?

### 3.1 Decision Drivers

| Driver | Weight | Hexagonal | Layered |
|--------|--------|-----------|---------|
| Testability | High | ✅ Excellent | ⚠️ Requires Spring Context |
| Domain Isolation | High | ✅ Complete | ❌ Mixed with framework |
| External API Flexibility | High | ✅ Easy to swap | ❌ Requires refactoring |
| Database Independence | Medium | ✅ Just change adapter | ❌ Service layer changes |
| Learning Curve | Low | ⚠️ Higher initially | ✅ Familiar pattern |
| Code Organization | Medium | ✅ Clear boundaries | ⚠️ Can become messy |

### 3.2 Specific Project Requirements

1. **Treasury API Integration**: External API that may change or need mocking
2. **Multiple Database Support**: H2 (dev) and PostgreSQL (prod)
3. **High Test Coverage**: 80%+ coverage requirement
4. **Clean Code Assessment**: Demonstrating architectural knowledge

---

## 4. Layer-by-Layer Explanation

### 4.1 DOMAIN Layer (Innermost)

**Location:** `com.wex.purchasetransaction.domain`

**Components:**
```
domain/
├── entity/
│   └── PurchaseTransaction.java    ← Aggregate Root
├── valueobject/
│   └── Money.java                  ← Value Object
├── repository/
│   └── PurchaseTransactionRepository.java  ← Port (Interface)
└── exception/
    ├── TransactionNotFoundException.java
    ├── UnsupportedCurrencyException.java
    └── CurrencyConversionException.java
```

**Responsibilities:**
- Define business entities and their invariants
- Encapsulate business rules (e.g., description max 50 chars)
- Define repository contracts (interfaces only)
- Define domain-specific exceptions

**Key Characteristics:**
- **ZERO framework dependencies** (no Spring, no JPA)
- Pure Java classes with business logic
- Can be tested without any infrastructure

**Example - Money Value Object:**
```java
public record Money(BigDecimal value) {
    public Money {
        if (value == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        // Enforce HALF_UP rounding to 2 decimal places
        value = value.setScale(2, RoundingMode.HALF_UP);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    public Money multiply(BigDecimal factor) {
        return new Money(value.multiply(factor));
    }
}
```

**Why This Matters:**
- Rounding rules are **centralized** in one place
- Any code using `Money` automatically follows the rules
- Business rule changes affect only this class

---

### 4.2 APPLICATION Layer

**Location:** `com.wex.purchasetransaction.application`

**Components:**
```
application/
├── port/
│   ├── in/                         ← Input Ports (Use Cases)
│   │   ├── StorePurchaseTransactionUseCase.java
│   │   └── RetrievePurchaseTransactionUseCase.java
│   └── out/                        ← Output Ports
│       └── ExchangeRatePort.java
├── service/                        ← Use Case Implementations
│   ├── StorePurchaseTransactionService.java
│   └── RetrievePurchaseTransactionService.java
└── dto/
    ├── StorePurchaseTransactionCommand.java
    ├── PurchaseTransactionResult.java
    └── ConvertedPurchaseTransactionResult.java
```

**Port Types:**

| Port Type | Direction | Purpose | Example |
|-----------|-----------|---------|---------|
| Input Port | Outside → Application | Define use cases | `StorePurchaseTransactionUseCase` |
| Output Port | Application → Outside | Define external dependencies | `ExchangeRatePort` |

**Example - Input Port:**
```java
public interface StorePurchaseTransactionUseCase {
    PurchaseTransactionResult store(StorePurchaseTransactionCommand command);
}
```

**Example - Output Port:**
```java
public interface ExchangeRatePort {
    Optional<ExchangeRateResult> findRate(String currencyName, LocalDate purchaseDate);

    record ExchangeRateResult(
        BigDecimal rate,
        LocalDate recordDate,
        String currencyName
    ) {}
}
```

**Service Implementation:**
```java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RetrievePurchaseTransactionService
        implements RetrievePurchaseTransactionUseCase {

    private final PurchaseTransactionRepository repository;  // Domain Port
    private final ExchangeRatePort exchangeRatePort;         // Application Port
    private final CurrencyMapper currencyMapper;

    @Override
    public ConvertedPurchaseTransactionResult retrieve(UUID id, String currencyCode) {
        // 1. Map currency code to Treasury name
        String treasuryName = currencyMapper.toTreasuryName(currencyCode.toUpperCase())
            .orElseThrow(() -> new UnsupportedCurrencyException(currencyCode));

        // 2. Find transaction
        PurchaseTransaction transaction = repository.findById(id)
            .orElseThrow(() -> new TransactionNotFoundException(id));

        // 3. Get exchange rate from Treasury API
        ExchangeRateResult rate = exchangeRatePort
            .findRate(treasuryName, transaction.getTransactionDate())
            .orElseThrow(() -> new CurrencyConversionException(currencyCode));

        // 4. Convert amount using domain logic
        Money converted = transaction.getAmount().multiply(rate.rate());

        // 5. Return result
        return ConvertedPurchaseTransactionResult.builder()
            .id(transaction.getId())
            .description(transaction.getDescription())
            .transactionDate(transaction.getTransactionDate())
            .amountUsd(transaction.getAmount().value())
            .exchangeRate(rate.rate())
            .convertedAmount(converted.value())
            .currencyCode(currencyCode.toUpperCase())
            .currencyName(rate.currencyName())
            .build();
    }
}
```

**Why This Matters:**
- Service knows **nothing** about HTTP, JPA, or Treasury API details
- Dependencies are **interfaces** (ports), not implementations
- Easy to test by providing mock implementations

---

### 4.3 INFRASTRUCTURE Layer

**Location:** `com.wex.purchasetransaction.infrastructure`

**Components:**
```
infrastructure/
├── persistence/                    ← Database Adapter
│   ├── PurchaseTransactionRepositoryAdapter.java
│   ├── PurchaseTransactionJpaEntity.java
│   └── PurchaseTransactionJpaRepository.java
├── external/treasury/              ← External API Adapter
│   ├── TreasuryApiClient.java
│   ├── TreasuryApiResponse.java
│   ├── CurrencyMapper.java
│   └── CurrencyMappingProperties.java
└── config/
    ├── WebClientConfig.java
    └── CacheConfig.java
```

**Adapter Pattern Implementation:**

**Database Adapter:**
```java
@Repository
@RequiredArgsConstructor
public class PurchaseTransactionRepositoryAdapter
        implements PurchaseTransactionRepository {  // Implements Domain Port

    private final PurchaseTransactionJpaRepository jpaRepository;

    @Override
    public PurchaseTransaction save(PurchaseTransaction domain) {
        PurchaseTransactionJpaEntity jpa = toJpaEntity(domain);
        PurchaseTransactionJpaEntity saved = jpaRepository.save(jpa);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<PurchaseTransaction> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    // Mapping methods keep domain and JPA entities separate
    private PurchaseTransactionJpaEntity toJpaEntity(PurchaseTransaction domain) {
        return PurchaseTransactionJpaEntity.builder()
            .id(domain.getId())
            .description(domain.getDescription())
            .transactionDate(domain.getTransactionDate())
            .amountUsd(domain.getAmount().value())
            .build();
    }

    private PurchaseTransaction toDomainEntity(PurchaseTransactionJpaEntity jpa) {
        return PurchaseTransaction.reconstitute(
            jpa.getId(),
            jpa.getDescription(),
            jpa.getTransactionDate(),
            Money.of(jpa.getAmountUsd())
        );
    }
}
```

**External API Adapter:**
```java
@Component
@RequiredArgsConstructor
public class TreasuryApiClient implements ExchangeRatePort {  // Implements Application Port

    private final WebClient treasuryWebClient;

    @Override
    @Cacheable(value = "exchangeRates", key = "#currencyName + '_' + #purchaseDate")
    public Optional<ExchangeRateResult> findRate(String currencyName, LocalDate purchaseDate) {
        // Implementation details hidden from application layer
        // Uses WebClient, handles HTTP errors, parses JSON
        // Application layer doesn't know about any of this
    }
}
```

**Why Adapters Matter:**
- **Separation of concerns**: JPA annotations stay in infrastructure
- **Domain purity**: Domain entities have no framework dependencies
- **Swappability**: Change database by writing new adapter only

---

### 4.4 PRESENTATION Layer

**Location:** `com.wex.purchasetransaction.presentation`

**Components:**
```
presentation/
├── controller/
│   └── PurchaseTransactionController.java
├── dto/
│   ├── request/
│   │   └── StorePurchaseTransactionRequest.java
│   └── response/
│       ├── PurchaseTransactionResponse.java
│       └── ConvertedPurchaseTransactionResponse.java
├── mapper/
│   └── PurchaseTransactionMapper.java
└── exception/
    └── GlobalExceptionHandler.java
```

**Controller Implementation:**
```java
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class PurchaseTransactionController {

    // Depends on USE CASE interfaces, not services directly
    private final StorePurchaseTransactionUseCase storeUseCase;
    private final RetrievePurchaseTransactionUseCase retrieveUseCase;
    private final PurchaseTransactionMapper mapper;

    @PostMapping
    public ResponseEntity<PurchaseTransactionResponse> store(
            @Valid @RequestBody StorePurchaseTransactionRequest request) {

        // 1. Map HTTP request to application command
        StorePurchaseTransactionCommand command = mapper.toCommand(request);

        // 2. Execute use case
        PurchaseTransactionResult result = storeUseCase.store(command);

        // 3. Map result to HTTP response
        PurchaseTransactionResponse response = mapper.toResponse(result);

        // 4. Return with proper HTTP status
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(result.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }
}
```

**Why This Structure:**
- Controller knows **nothing** about database or Treasury API
- Only knows about **use cases** (business operations)
- HTTP concerns (status codes, headers) stay in presentation layer

---

## 5. Benefits Over Layered Architecture

### 5.1 Testability

**Layered Architecture Testing:**
```java
@SpringBootTest  // Needs full Spring context
@AutoConfigureTestDatabase  // Needs database
class TransactionServiceTest {
    @Autowired
    private TransactionService service;  // Real service with real dependencies

    @Test
    void testStore() {
        // Slow - starts Spring, connects to DB
        // Brittle - depends on external systems
    }
}
```

**Hexagonal Architecture Testing:**
```java
@ExtendWith(MockitoExtension.class)  // No Spring needed
class RetrievePurchaseTransactionServiceTest {
    @Mock
    private PurchaseTransactionRepository repository;  // Mock interface
    @Mock
    private ExchangeRatePort exchangeRatePort;  // Mock interface
    @InjectMocks
    private RetrievePurchaseTransactionService service;

    @Test
    void retrieve_shouldReturnConvertedTransaction() {
        // Fast - no Spring, no DB, no HTTP
        // Reliable - no external dependencies
        when(repository.findById(any())).thenReturn(Optional.of(transaction));
        when(exchangeRatePort.findRate(any(), any())).thenReturn(Optional.of(rate));

        var result = service.retrieve(id, "BRL");

        assertThat(result.convertedAmount()).isEqualByComparingTo("497.00");
    }
}
```

### 5.2 Flexibility Comparison

| Scenario | Layered Architecture | Hexagonal Architecture |
|----------|---------------------|------------------------|
| Change database (H2 → PostgreSQL) | Modify Service layer | Just change adapter config |
| Change database (JPA → MongoDB) | Rewrite Service layer | Write new adapter |
| Mock Treasury API for tests | Complex (mock HTTP client) | Easy (mock port interface) |
| Add GraphQL endpoint | Modify or duplicate Service | Add new driving adapter |
| Replace Treasury API | Modify Service layer | Write new adapter |

### 5.3 Code Organization

**Layered Architecture:**
```
src/main/java/
├── controller/      ← Knows about Service
├── service/         ← Knows about Repository AND external APIs
├── repository/      ← Coupled to JPA
└── entity/          ← Has JPA annotations
```
*Problem: Service layer becomes a "God class" knowing too much*

**Hexagonal Architecture:**
```
src/main/java/
├── domain/          ← Pure business logic, NO dependencies
├── application/     ← Use cases, depends on INTERFACES only
├── infrastructure/  ← Implements interfaces, framework code here
└── presentation/    ← HTTP concerns only
```
*Benefit: Clear boundaries, each layer has single responsibility*

---

## 6. Data Flow Diagrams

### 6.1 Store Transaction Flow

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────────────────────────┐
│ PRESENTATION: PurchaseTransactionController.store()         │
│   - Validates request (@Valid)                              │
│   - Maps request → command                                  │
└─────────────────────────────────────────────────────────────┘
     │
     ▼ StorePurchaseTransactionCommand
┌─────────────────────────────────────────────────────────────┐
│ APPLICATION: StorePurchaseTransactionService.store()        │
│   - Creates Money value object (validates amount)           │
│   - Creates PurchaseTransaction (validates description)     │
│   - Calls repository.save()                                 │
└─────────────────────────────────────────────────────────────┘
     │
     ▼ PurchaseTransaction (Domain Entity)
┌─────────────────────────────────────────────────────────────┐
│ INFRASTRUCTURE: PurchaseTransactionRepositoryAdapter.save() │
│   - Maps domain entity → JPA entity                         │
│   - Calls JpaRepository.save()                              │
│   - Maps JPA entity → domain entity                         │
└─────────────────────────────────────────────────────────────┘
     │
     ▼ PurchaseTransaction (Domain Entity)
┌─────────────────────────────────────────────────────────────┐
│ APPLICATION: Maps to PurchaseTransactionResult              │
└─────────────────────────────────────────────────────────────┘
     │
     ▼ PurchaseTransactionResult
┌─────────────────────────────────────────────────────────────┐
│ PRESENTATION: Maps to PurchaseTransactionResponse           │
│   - Sets HTTP 201 status                                    │
│   - Adds Location header                                    │
└─────────────────────────────────────────────────────────────┘
     │
     ▼
HTTP Response (201 Created)
```

### 6.2 Retrieve with Conversion Flow

```
HTTP Request: GET /transactions/{id}?currency=BRL
     │
     ▼
┌─────────────────────────────────────────────────────────────┐
│ PRESENTATION: PurchaseTransactionController.retrieve()      │
│   - Extracts path variable (UUID id)                        │
│   - Extracts query param (String currency)                  │
└─────────────────────────────────────────────────────────────┘
     │
     ▼ (UUID id, String currency)
┌─────────────────────────────────────────────────────────────┐
│ APPLICATION: RetrievePurchaseTransactionService.retrieve()  │
│                                                             │
│   1. CurrencyMapper.toTreasuryName("BRL")                   │
│      └── Returns "Brazil-Real"                              │
│                                                             │
│   2. PurchaseTransactionRepository.findById(id)             │
│      └── Returns PurchaseTransaction                        │
│                                                             │
│   3. ExchangeRatePort.findRate("Brazil-Real", date)         │
│      └── Returns ExchangeRateResult(5.25, 2024-03-01)       │
│                                                             │
│   4. Money.multiply(5.25)                                   │
│      └── Returns converted Money                            │
│                                                             │
│   5. Build ConvertedPurchaseTransactionResult               │
└─────────────────────────────────────────────────────────────┘
     │                           │
     │                           ▼
     │         ┌─────────────────────────────────────────────┐
     │         │ INFRASTRUCTURE: TreasuryApiClient.findRate()│
     │         │   - Builds filter query                     │
     │         │   - Calls Treasury API via WebClient        │
     │         │   - Parses JSON response                    │
     │         │   - Returns ExchangeRateResult              │
     │         └─────────────────────────────────────────────┘
     │
     ▼ ConvertedPurchaseTransactionResult
┌─────────────────────────────────────────────────────────────┐
│ PRESENTATION: Maps to ConvertedPurchaseTransactionResponse  │
│   - Sets HTTP 200 status                                    │
└─────────────────────────────────────────────────────────────┘
     │
     ▼
HTTP Response (200 OK)
```

---

## 7. Interview Talking Points

### 7.1 Opening Statement

> "I chose Hexagonal Architecture because this project integrates with an external API (US Treasury) and needs to support multiple databases. The architecture ensures our business logic is completely isolated from these external dependencies, making the code highly testable and maintainable."

### 7.2 Key Points to Emphasize

1. **Dependency Inversion**: "Dependencies point inward. The domain knows nothing about Spring, JPA, or HTTP."

2. **Testability**: "I can test all business logic with simple unit tests, no Spring context needed. This helped achieve 80%+ coverage easily."

3. **Flexibility**: "Swapping H2 for PostgreSQL requires zero code changes in business logic - just configuration."

4. **Strategy Pattern**: "The `ExchangeRatePort` interface means I could replace the Treasury API with any other exchange rate provider without touching the service layer."

5. **Value Objects**: "The `Money` class encapsulates all currency rounding rules in one place, preventing bugs across the codebase."

### 7.3 Potential Questions & Answers

**Q: "Isn't this over-engineering for a simple CRUD API?"**

A: "While it adds initial structure, the benefits are clear:
- The Treasury API integration would be tightly coupled in layered architecture
- Testing would require mocking HTTP clients instead of simple interfaces
- The assessment requires demonstrating architectural knowledge
- In real projects, this structure scales well as complexity grows"

**Q: "How do you handle transactions across layers?"**

A: "Transaction boundaries are defined at the application service layer using `@Transactional`. The infrastructure adapters participate in these transactions automatically through Spring's transaction management."

**Q: "Why separate JPA entities from domain entities?"**

A: "This prevents JPA annotations from polluting the domain. The domain entity `PurchaseTransaction` has only business logic, while `PurchaseTransactionJpaEntity` handles persistence concerns. This follows the Single Responsibility Principle."

---

## 8. Summary Comparison Table

| Aspect | Layered Architecture | Hexagonal Architecture |
|--------|---------------------|------------------------|
| **Dependency Direction** | Downward (to infrastructure) | Inward (to domain) |
| **Domain Purity** | Mixed with framework | Pure Java, no dependencies |
| **Testing Speed** | Slow (needs Spring) | Fast (plain unit tests) |
| **External API Changes** | Affects Service layer | Only affects adapter |
| **Database Changes** | Affects Service layer | Only affects adapter |
| **Learning Curve** | Lower | Higher initially |
| **Long-term Maintainability** | Decreases over time | Remains consistent |
| **Code Organization** | Can become messy | Clear boundaries |

---

## 9. Conclusion

Hexagonal Architecture was the right choice for this project because:

1. **External Integration**: The Treasury API is isolated behind `ExchangeRatePort`
2. **Multiple Databases**: H2/PostgreSQL switching is configuration-only
3. **Testability**: 116 tests with 80%+ coverage, most without Spring context
4. **Clean Code**: Clear separation of concerns demonstrates architectural knowledge
5. **Future-Proof**: Adding new features (GraphQL, events) requires only new adapters

The additional initial complexity pays off in maintainability, testability, and flexibility.

---

*Document prepared for WEX Technical Assessment - SDE3 (R21264)*
