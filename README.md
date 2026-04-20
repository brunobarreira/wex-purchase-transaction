# WEX Purchase Transaction API

REST API built with **Java 21** and **Spring Boot 3.3** for storing purchase transactions in USD and retrieving them with real-time currency conversion using the [US Treasury Reporting Rates of Exchange API](https://fiscaldata.treasury.gov/datasets/treasury-reporting-rates-exchange/treasury-reporting-rates-of-exchange).

## Features

- **Store** purchase transactions in USD with automatic UUID generation
- **Retrieve** transactions with live currency conversion to any supported currency
- **Swagger UI** for interactive API documentation and testing
- **H2 embedded database** — no external installation required
- **PostgreSQL support** for production deployments
- **80%+ test coverage** with unit, integration, and E2E tests

---

## Prerequisites

| Requirement | Version | Check Command |
|-------------|---------|---------------|
| Java JDK | 21+ | `java -version` |
| Gradle | 8.x (or use included wrapper) | `./gradlew --version` |
| Docker (optional) | 20+ | `docker --version` |

---

## Quick Start

### 1. Run the Application

```bash
./gradlew bootRun
```

The application starts at **http://localhost:8080** with an embedded H2 database (no external setup required).

### 2. Open Swagger UI

Navigate to: **http://localhost:8080/swagger-ui.html**

### 3. Test the API

#### Store a Transaction (POST)

1. Expand **POST /api/v1/transactions**
2. Click **"Try it out"**
3. Enter the request body:
   ```json
   {
     "description": "Hotel stay in NYC",
     "transactionDate": "2024-03-15",
     "amountUsd": 123.45
   }
   ```
4. Click **"Execute"**
5. Copy the `id` from the response

#### Retrieve with Currency Conversion (GET)

1. Expand **GET /api/v1/transactions/{id}**
2. Click **"Try it out"**
3. Enter:
   - **id**: The UUID from the previous step
   - **currency**: `BRL` (or `EUR`, `JPY`, `GBP`, `CAD`)
4. Click **"Execute"**
5. View the converted amount in the response

---

## API Reference

### POST /api/v1/transactions

Store a purchase transaction in USD.

**Request:**
```json
{
  "description": "Hotel stay in NYC",
  "transactionDate": "2024-03-15",
  "amountUsd": 123.45
}
```

**Validations:**
| Field | Rules |
|-------|-------|
| `description` | Required, max 50 characters |
| `transactionDate` | Required, ISO 8601 format, must be today or in the past |
| `amountUsd` | Required, positive number, rounded to nearest cent |

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Hotel stay in NYC",
  "transactionDate": "2024-03-15",
  "amountUsd": 123.45
}
```

---

### GET /api/v1/transactions/{id}?currency={code}

Retrieve a transaction with currency conversion.

**Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Path | UUID of the transaction |
| `currency` | Query | ISO 4217 currency code (e.g., `BRL`, `EUR`, `JPY`) |

**Response (200 OK):**
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

**Error Responses:**
| Status | Description |
|--------|-------------|
| 400 | Invalid request or unsupported currency code |
| 404 | Transaction not found |
| 422 | No exchange rate available within 6 months of transaction date |

---

## Supported Currencies

| ISO Code | Currency Name | Country/Region |
|----------|---------------|----------------|
| `BRL` | Real | Brazil |
| `EUR` | Euro | European Union |
| `JPY` | Yen | Japan |
| `GBP` | Pound | United Kingdom |
| `CAD` | Dollar | Canada |
| `USD` | Dollar | United States |

---

## Running with Docker

### Standalone (H2 Embedded)

```bash
# Build the image
docker build -t wex-purchase-transaction .

# Run the container
docker run -p 8080:8080 wex-purchase-transaction
```

### Production Mode (PostgreSQL)

```bash
# Start PostgreSQL and the application
docker compose --profile prod up

# Stop all services
docker compose --profile prod down
```

---

## Development

### Gradle Commands

| Command | Description |
|---------|-------------|
| `./gradlew bootRun` | Run the application locally |
| `./gradlew test` | Run all tests (unit + integration + E2E) |
| `./gradlew jacocoTestReport` | Generate HTML coverage report |
| `./gradlew jacocoTestCoverageVerification` | Verify coverage >= 80% |
| `./gradlew checkstyleMain checkstyleTest` | Check code style |
| `./gradlew build` | Full build (compile + test + jar) |

### Test Reports

After running tests, reports are available at:
- **Test Results:** `build/reports/tests/test/index.html`
- **Coverage Report:** `build/reports/jacoco/html/index.html`

### H2 Database Console

Access the database console at **http://localhost:8080/h2-console**

| Setting | Value |
|---------|-------|
| JDBC URL | `jdbc:h2:file:./data/wexdb` |
| Username | `sa` |
| Password | *(leave empty)* |

---

## Architecture

The project follows **Clean Architecture** with four layers:

```
┌─────────────────────────────────────────────────────┐
│                  PRESENTATION                        │
│         (Controllers, REST DTOs, Mappers)           │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│                   APPLICATION                        │
│            (Use Cases, Ports, Services)             │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│                     DOMAIN                           │
│      (Entities, Value Objects, Repositories)        │
└────────────────────────┬────────────────────────────┘
                         ▲
┌────────────────────────┴────────────────────────────┐
│                  INFRASTRUCTURE                      │
│        (JPA Adapters, Treasury API Client)          │
└─────────────────────────────────────────────────────┘
```

**Key Design Decisions:**
- **Hexagonal Architecture** with Ports & Adapters pattern
- **Domain-Driven Design** with `Money` value object for currency handling
- **Strategy Pattern** via `ExchangeRatePort` for external API abstraction
- **Spring Cache** to avoid redundant Treasury API calls

---

## Testing with cURL

```bash
# Store a transaction
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Coffee",
    "transactionDate": "2024-03-15",
    "amountUsd": 5.50
  }'

# Retrieve with conversion (replace YOUR_UUID with actual ID)
curl "http://localhost:8080/api/v1/transactions/YOUR_UUID?currency=BRL"
```

---

## Environment Variables (Production)

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | — | Set to `prod` for PostgreSQL |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `wexdb` | Database name |
| `DB_USERNAME` | `wex` | Database user |
| `DB_PASSWORD` | `wex` | Database password |

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Port 8080 in use | `./gradlew bootRun --args='--server.port=8081'` |
| Java version error | Ensure `JAVA_HOME` points to JDK 21+ |
| H2 database locked | Delete `./data/wexdb.*` files and restart |
| Treasury API timeout | Check internet connection |
| Build fails | `./gradlew clean build --refresh-dependencies` |

---

## License

This project was developed for the WEX technical assessment (SDE3 — R21264).
