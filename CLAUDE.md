# WEX Purchase Transaction API — Developer Guide

## Project Overview

REST API built in Java 21 + Spring Boot 3.3 for the WEX technical assessment (SDE3 — R21264).

**What it does:**
1. **Store** purchase transactions (USD) — POST `/api/v1/transactions`
2. **Retrieve** transactions with live currency conversion — GET `/api/v1/transactions/{id}?currency=BRL`

Currency conversion uses the [US Treasury Reporting Rates of Exchange API](https://fiscaldata.treasury.gov/datasets/treasury-reporting-rates-exchange/treasury-reporting-rates-of-exchange), applying the most recent rate ≤ purchase date within 6 months.

---

## Architecture

Clean Architecture with four layers, inner layers never depend on outer layers:

```
presentation/ → application/ → domain/
                               ↑
infrastructure/    ────────────┘
```

| Layer | Package | Responsibility |
|---|---|---|
| Domain | `domain/` | Entities (`PurchaseTransaction`), value objects (`Money`), repository interfaces, domain exceptions |
| Application | `application/` | Use case interfaces (ports in), `ExchangeRatePort` (port out), service implementations, DTOs |
| Infrastructure | `infrastructure/` | JPA adapters, Treasury WebClient, `CurrencyMapper`, Spring configs |
| Presentation | `presentation/` | REST controllers, request/response DTOs, mapper, `GlobalExceptionHandler` |

---

## Running Locally

### Default mode (H2 embedded, no install required)
```bash
./gradlew bootRun
```
- App starts at http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:file:./data/wexdb`)

### Production mode (PostgreSQL via Docker)
```bash
docker compose --profile prod up
```

### Standalone Docker (H2 embedded)
```bash
docker build -t wex-purchase-transaction .
docker run -p 8080:8080 wex-purchase-transaction
```

---

## Gradle Commands

| Command | Purpose |
|---|---|
| `./gradlew bootRun` | Run locally |
| `./gradlew test` | Run all tests (unit + integration + E2E) |
| `./gradlew jacocoTestReport` | Generate HTML coverage report (`build/reports/jacoco/html/index.html`) |
| `./gradlew jacocoTestCoverageVerification` | Fail build if coverage < 80% |
| `./gradlew checkstyleMain` | Check main source code style |
| `./gradlew checkstyleTest` | Check test code style |
| `./gradlew bootJar` | Build production fat JAR |
| `./gradlew build` | Full build (compile + test + jar) |

---

## API Reference

### POST /api/v1/transactions
Store a purchase transaction in USD.

**Request body:**
```json
{
  "description": "Hotel stay in NYC",
  "transactionDate": "2024-03-15",
  "amountUsd": 123.45
}
```

**Validations:**
- `description`: required, max 50 characters
- `transactionDate`: required, valid date (ISO 8601), past or present
- `amountUsd`: required, > 0, rounded to nearest cent

**Response 201:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Hotel stay in NYC",
  "transactionDate": "2024-03-15",
  "amountUsd": 123.45
}
```

---

### GET /api/v1/transactions/{id}?currency=BRL
Retrieve a stored transaction converted to the target currency.

**Path param:** `id` — UUID of the transaction
**Query param:** `currency` — ISO 4217 code (e.g., `BRL`, `EUR`, `JPY`)

**Response 200:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Hotel stay in NYC",
  "transactionDate": "2024-03-15",
  "amountUsd": 123.45,
  "exchangeRate": 4.97,
  "convertedAmount": 613.55,
  "currencyCode": "BRL",
  "currencyName": "Real"
}
```

**Error responses:**
- `400 Bad Request` — validation failure or unsupported currency code
- `404 Not Found` — transaction ID does not exist
- `422 Unprocessable Entity` — no exchange rate found within 6 months of the transaction date

---

## Currency Conversion Logic

1. ISO 4217 code (e.g., `BRL`) is mapped to Treasury currency name (e.g., `Real`) via `CurrencyMapper`
2. Treasury API queried: `currency:eq:Real, record_date range = [purchaseDate - 6 months, purchaseDate]`, sorted by `-record_date`, page size 1
3. First result = most recent rate within 6 months
4. `convertedAmount = amountUsd × exchangeRate`, rounded to 2 decimal places (HALF_UP)

Currency mappings live in `application.yml` under `treasury.currency.mappings`. Adding new currencies requires no code change.

---

## Database

| Profile | Database | Notes |
|---|---|---|
| default | H2 (file mode) | `jdbc:h2:file:./data/wexdb` — persists between restarts |
| test | H2 (in-memory) | Resets between test runs |
| prod | PostgreSQL 16 | Configured via env vars |

**Flyway migrations:** `src/main/resources/db/migration/`
- `V1__create_purchase_transaction_table.sql` — initial schema

---

## Key Design Decisions

- **H2 embedded (file mode)** satisfies the "no external installation" requirement. Data persists in `./data/wexdb.*` files.
- **Clean Architecture** ensures domain/application logic is completely independent of Spring, JPA, or the Treasury API — all dependencies point inward.
- **ISO 4217 currency codes** used as API input; mapped to Treasury names via YAML config (`CurrencyMapper`).
- **`ExchangeRatePort` interface** allows the Treasury API client to be swapped without touching business logic (Strategy pattern).
- **Spring Cache (`@Cacheable`)** on `TreasuryApiClient` avoids redundant external API calls for identical currency+date combinations.
- **`Money` value object** encapsulates all rounding rules (`BigDecimal`, `HALF_UP`, scale 2).

---

## Test Structure

```
src/test/java/
├── unit/
│   ├── domain/entity/     — PurchaseTransactionTest, MoneyTest
│   ├── application/service/ — StorePurchaseTransactionServiceTest, RetrievePurchaseTransactionServiceTest
│   └── presentation/controller/ — PurchaseTransactionControllerTest (@WebMvcTest)
├── integration/
│   ├── persistence/       — PurchaseTransactionRepositoryAdapterIT (@DataJpaTest)
│   └── external/          — TreasuryApiClientIT (WireMock)
└── e2e/
    └── PurchaseTransactionE2ETest (RestAssured + @SpringBootTest + WireMock)
```

---

## CI/CD Pipeline (GitHub Actions)

Stages run in order: `checkstyle → test → coverage → build → docker`

- All tests run with H2 in-memory (no external services required in CI)
- Docker image pushed to GHCR only on `main` branch merges
- Coverage minimum: 80%

---

## Environment Variables (Production)

| Variable | Default | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | — | Set to `prod` for PostgreSQL |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `wexdb` | Database name |
| `DB_USERNAME` | `wex` | Database user |
| `DB_PASSWORD` | `wex` | Database password |
