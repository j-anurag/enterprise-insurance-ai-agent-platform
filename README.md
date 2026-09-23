# ClaimWise AI - Stage 1 Backend Foundation

A clean, modular Spring Boot monolith providing the core domain entities, database persistence, schema migrations, and REST APIs for the ClaimWise platform.

> **Note on Stage 1 Scope**: This project currently represents **Stage 1 (Backend Foundation)**. It establishes the robust core data model, PostgreSQL database with Flyway migrations, validation, and REST API foundation. Advanced capabilities such as LLMs, RAG, AI agents, vector databases, S3 storage, Kafka, and frontend interfaces are intentionally scheduled for later stages. No authentication or security layer is required in Stage 1.

---

## Technology Stack

- **Java**: 25 LTS
- **Framework**: Spring Boot 4.0.0
- **Database**: PostgreSQL 16+ (Local port `5432` / Docker port `5433`)
- **Persistence**: Spring Data JPA & Hibernate 7 (`spring.jpa.hibernate.ddl-auto=validate`)
- **Database Migrations**: Flyway Core & Flyway PostgreSQL
- **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`)
- **API Documentation**: Springdoc OpenAPI 3.1 & Swagger UI
- **Boilerplate Reduction**: Project Lombok 1.18.48
- **Testing**: JUnit 5, Mockito, AssertJ, Spring Test (MockMvc), H2 Database in PostgreSQL compatibility mode
- **Containerization**: Docker (multi-stage build) & Docker Compose

---

## Project Structure

```
claimwise-backend
├── src
│   ├── main
│   │   ├── java/com/claimwise
│   │   │   ├── controller      # Thin REST controllers (Customers, Policies, Claims)
│   │   │   ├── service         # Business logic layer and transactional services
│   │   │   ├── repository      # Spring Data JPA repositories
│   │   │   ├── model           # JPA entities (Customer, Policy, Claim) and Enums
│   │   │   ├── dto             # Request & response data transfer objects
│   │   │   ├── exception       # Custom domain exceptions & GlobalExceptionHandler
│   │   │   └── ClaimWiseApplication.java  # Application entry point
│   │   └── resources
│   │       ├── db/migration    # Flyway versioned SQL migrations (V1, V2, V3)
│   │       └── application.properties     # Production & local runtime configuration
│   └── test
│       ├── java/com/claimwise  # Controller, service, and integration tests
│       └── resources
│           └── application.properties     # Isolated test database configuration
├── Dockerfile                  # Multi-stage JDK 25 container build
├── docker-compose.yml          # Containerized application and PostgreSQL services
├── pom.xml                     # Maven build configuration
└── .env.example                # Sample environment variables template
```

---

## Database & Domain Model

ClaimWise Stage 1 uses PostgreSQL with strict schema management via Flyway. Hibernate schema modification is explicitly disabled (`ddl-auto=validate`).

### Relational Schema

```
customers (1) ───< policies (N) ───< claims (N)
```

1. **`customers`** (`V1__create_customers_table.sql`):
   - `id`: BIGSERIAL PRIMARY KEY
   - `customer_number`: VARCHAR(50) NOT NULL UNIQUE
   - `full_name`: VARCHAR(100) NOT NULL
   - `email`: VARCHAR(150) NOT NULL UNIQUE
   - `created_at`, `updated_at`: TIMESTAMP WITH TIME ZONE NOT NULL

2. **`policies`** (`V2__create_policies_table.sql`):
   - `id`: BIGSERIAL PRIMARY KEY
   - `policy_number`: VARCHAR(50) NOT NULL UNIQUE
   - `customer_id`: BIGINT NOT NULL (FK -> `customers.id`)
   - `policy_type`: VARCHAR(50) NOT NULL (`AUTO`, `HEALTH`, `HOME`, `LIFE`, `TRAVEL`)
   - `start_date`, `expiry_date`: DATE NOT NULL
   - `status`: VARCHAR(50) NOT NULL (`ACTIVE`, `PENDING`, `EXPIRED`, `CANCELLED`)
   - `created_at`, `updated_at`: TIMESTAMP WITH TIME ZONE NOT NULL

3. **`claims`** (`V3__create_claims_table.sql`):
   - `id`: BIGSERIAL PRIMARY KEY
   - `claim_number`: VARCHAR(50) NOT NULL UNIQUE
   - `policy_id`: BIGINT NOT NULL (FK -> `policies.id`)
   - `description`: TEXT NOT NULL
   - `incident_date`: DATE NOT NULL
   - `status`: VARCHAR(50) NOT NULL (`SUBMITTED`, `IN_PROGRESS`, `APPROVED`, `DENIED`)
   - `created_at`, `updated_at`: TIMESTAMP WITH TIME ZONE NOT NULL

---

## Environment Variables

| Variable | Description | Default (Local) | Default (Docker) |
|---|---|---|---|
| `SERVER_PORT` | HTTP server port | `8080` | `8080` |
| `DB_HOST` | PostgreSQL hostname | `localhost` | `postgres` |
| `DB_PORT` | PostgreSQL port | `5432` | `5432` |
| `DB_NAME` | Database name | `claimwise` | `claimwise` |
| `DB_USERNAME` | Database username | `${USER}` | `claimwise` |
| `DB_PASSWORD` | Database password | *(empty)* | `claimwise_password` |

A sample `.env.example` file is provided in the repository root.

---

## How to Run Locally

### Prerequisites

- Java 25 installed (`java -version`)
- PostgreSQL installed and running on port `5432`

### 1. Create the Database

```bash
createdb claimwise
# Or via psql:
# psql -c "CREATE DATABASE claimwise;"
```

### 2. Run the Application

Flyway automatically executes pending migrations on startup:

```bash
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`.

---

## How to Run Tests

Tests run against an isolated in-memory H2 database running in PostgreSQL mode with Flyway schema migration validation.

```bash
./mvnw clean test
```

### Test Coverage

- **Customer Service**: Customer creation, listing, duplicate detection.
- **Policy Service**: Policy creation, date validation, customer verification, policy number lookup.
- **Claim Service**: Claim filing, policy verification, incident date validation, lookup by policy ID.
- **Controllers**: MockMvc tests validating HTTP status codes (201, 200, 400, 404, 409), input validation, and JSON structures.
- **Repository Integration**: Tests database persistence, foreign key relationships, and uniqueness constraints.

---

## Docker Instructions

Docker Compose spins up both PostgreSQL and the application container. The Docker PostgreSQL port is mapped to host port **`5433`** by default so it does not conflict with existing local PostgreSQL installations on port `5432`.

### Start Containers

```bash
docker-compose up --build
```

### Stop Containers

```bash
docker-compose down
```

---

## API Endpoints

All endpoints use JSON payloads and return appropriate HTTP status codes.

### Customer Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/customers` | Register a new customer |
| `GET` | `/api/v1/customers` | Retrieve all customers |

#### Example Customer Request

```json
POST /api/v1/customers
{
  "customerNumber": "CUST-1001",
  "fullName": "Jane Doe",
  "email": "jane.doe@example.com"
}
```

### Policy Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/policies` | Create a new insurance policy |
| `GET` | `/api/v1/policies` | Retrieve all policies |
| `GET` | `/api/v1/policies/{policyNumber}` | Retrieve a policy by policy number |

#### Example Policy Request

```json
POST /api/v1/policies
{
  "policyNumber": "POL-1001",
  "customerId": 1,
  "policyType": "HEALTH",
  "startDate": "2026-01-01",
  "expiryDate": "2027-01-01",
  "status": "ACTIVE"
}
```

### Claim Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/claims` | File a new claim against an active policy |
| `GET` | `/api/v1/claims` | Retrieve all claims |
| `GET` | `/api/v1/claims/policy/{policyId}` | Retrieve all claims for a given policy ID |

#### Example Claim Request

```json
POST /api/v1/claims
{
  "claimNumber": "CLM-1001",
  "policyId": 1,
  "description": "Emergency medical treatment expenses",
  "incidentDate": "2026-02-15",
  "status": "SUBMITTED"
}
```

---

## OpenAPI & Swagger Documentation

Interactive Swagger documentation is available when the application is running:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
