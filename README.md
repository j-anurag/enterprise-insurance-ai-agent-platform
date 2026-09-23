# ClaimWise AI - Stage 2: AI & RAG Foundation

A clean, modular Spring Boot monolith providing core insurance domain entities, database persistence, schema migrations, REST APIs, and an AI/RAG foundation with PDF document extraction, text chunking, Hugging Face embeddings, and native PostgreSQL pgvector cosine similarity search.

> **Note on Stage 2 Scope**: This project establishes the **AI/RAG Foundation** for ClaimWise. It extends the Stage 1 relational backend (Customers, Policies, Claims) with a policy-document knowledge base and vector similarity search. Advanced agent orchestration, autonomous claim decisions, LangGraph, S3 storage, Kafka, Redis, and frontend interfaces are scheduled for subsequent stages. No Groq LLM answer synthesis is performed in Stage 2 (Groq configuration is scaffolding for Stage 3).

---

## Technology Stack

- **Java**: 25 LTS
- **Framework**: Spring Boot 4.0.0
- **Database**: PostgreSQL 16 with pgvector extension (`pgvector/pgvector:pg16` on host port `5433`)
- **Vector Search**: PostgreSQL pgvector with HNSW cosine similarity index (`vector_cosine_ops`, 384 dimensions)
- **Embeddings Provider**: Hugging Face Inference API (`BAAI/bge-small-en-v1.5`, 384-dimensional dense vectors)
- **PDF Extraction**: Apache PDFBox 3.0.4
- **Persistence**: Spring Data JPA & Hibernate 7 (`spring.jpa.hibernate.ddl-auto=validate`)
- **Database Migrations**: Flyway Core & Flyway PostgreSQL (V1 through V5)
- **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`)
- **API Documentation**: Springdoc OpenAPI 3.1 & Swagger UI
- **Boilerplate Reduction**: Project Lombok 1.18.48
- **Testing**: JUnit 5, Mockito, AssertJ, Spring Test (MockMvc), In-Memory H2 with isolated test migrations
- **Containerization**: Docker (multi-stage build) & Docker Compose

---

## Project Structure

```
claimwise-backend
├── src
│   ├── main
│   │   ├── java/com/claimwise
│   │   │   ├── controller      # REST controllers (Customers, Policies, Claims, Documents, Retrieval)
│   │   │   ├── service         # Business logic layer, PDF ingestion, chunking, embeddings, retrieval
│   │   │   ├── repository      # Spring Data JPA repositories & pgvector native queries
│   │   │   ├── model           # JPA entities (Customer, Policy, Claim, PolicyDocument, DocumentChunk)
│   │   │   ├── dto             # Request & response data transfer objects
│   │   │   ├── exception       # Custom domain exceptions & GlobalExceptionHandler
│   │   │   └── ClaimWiseApplication.java  # Application entry point
│   │   └── resources
│   │       ├── db/migration    # Flyway versioned SQL migrations (V1, V2, V3, V4, V5)
│   │       └── application.properties     # Production & local runtime configuration
│   └── test
│       ├── java/com/claimwise  # Controller, service, and repository integration tests
│       └── resources
│           ├── db/migration-h2 # H2-compatible test migrations
│           └── application.properties     # Isolated test database configuration
├── Dockerfile                  # Multi-stage JDK 25 container build
├── docker-compose.yml          # Containerized application and pgvector PostgreSQL services
├── pom.xml                     # Maven build configuration
└── .env.example                # Sample environment variables template
```

---

## Database & Domain Model

ClaimWise uses PostgreSQL with strict schema management via Flyway. Hibernate schema modification is explicitly disabled (`ddl-auto=validate`).

### Relational & Vector Schema

```
customers (1) ───< policies (N) ───< claims (N)
                      │
                      └───< policy_documents (N) ───< document_chunks (N)
                                                            └── embedding vector(384)
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
   - `customer_id`: BIGINT NOT NULL (FK -> `customers.id` ON DELETE RESTRICT)
   - `policy_type`: VARCHAR(50) NOT NULL (`AUTO`, `HEALTH`, `HOME`, `LIFE`, `TRAVEL`)
   - `start_date`, `expiry_date`: DATE NOT NULL
   - `status`: VARCHAR(50) NOT NULL (`ACTIVE`, `PENDING`, `EXPIRED`, `CANCELLED`)
   - `created_at`, `updated_at`: TIMESTAMP WITH TIME ZONE NOT NULL

3. **`claims`** (`V3__create_claims_table.sql`):
   - `id`: BIGSERIAL PRIMARY KEY
   - `claim_number`: VARCHAR(50) NOT NULL UNIQUE
   - `policy_id`: BIGINT NOT NULL (FK -> `policies.id` ON DELETE RESTRICT)
   - `description`: TEXT NOT NULL
   - `incident_date`: DATE NOT NULL
   - `status`: VARCHAR(50) NOT NULL (`SUBMITTED`, `IN_PROGRESS`, `APPROVED`, `DENIED`)
   - `created_at`, `updated_at`: TIMESTAMP WITH TIME ZONE NOT NULL

4. **`policy_documents`** (`V4__create_policy_documents_table.sql`):
   - `id`: BIGSERIAL PRIMARY KEY
   - `policy_id`: BIGINT NOT NULL (FK -> `policies.id` ON DELETE RESTRICT)
   - `document_name`: VARCHAR(255) NOT NULL
   - `document_type`: VARCHAR(100) NOT NULL
   - `storage_reference`: VARCHAR(500) NOT NULL
   - `extracted_text`: TEXT
   - `created_at`, `updated_at`: TIMESTAMP WITH TIME ZONE NOT NULL

5. **`document_chunks`** (`V5__create_document_chunks_and_vector_extension.sql`):
   - `id`: BIGSERIAL PRIMARY KEY
   - `document_id`: BIGINT NOT NULL (FK -> `policy_documents.id` ON DELETE RESTRICT)
   - `chunk_index`: INT NOT NULL
   - `chunk_text`: TEXT NOT NULL
   - `embedding`: `vector(384)` NOT NULL
   - `metadata`: TEXT
   - `created_at`: TIMESTAMP WITH TIME ZONE NOT NULL
   - **Index**: HNSW index on `embedding` using `vector_cosine_ops`

---

## Environment Variables

| Variable | Description | Default (Local) | Default (Docker) |
|---|---|---|---|
| `SERVER_PORT` | HTTP server port | `8080` | `8080` |
| `DB_HOST` | PostgreSQL hostname | `localhost` | `postgres` |
| `DB_PORT` | PostgreSQL port | `5433` (Docker pgvector) | `5432` |
| `DB_NAME` | Database name | `claimwise` | `claimwise` |
| `DB_USERNAME` | Database username | `claimwise` | `claimwise` |
| `DB_PASSWORD` | Database password | `claimwise` | `claimwise` |
| `HUGGINGFACE_API_KEY` | Hugging Face Inference API key | *(empty)* | *(empty)* |
| `HUGGINGFACE_EMBEDDING_MODEL` | Embedding model identifier | `BAAI/bge-small-en-v1.5` | `BAAI/bge-small-en-v1.5` |
| `EMBEDDING_DIMENSIONS` | Embedding vector dimensions | `384` | `384` |
| `GROQ_API_KEY` | Groq API Key (Stage 3 Scaffolding) | *(empty)* | *(empty)* |
| `GROQ_MODEL` | Groq Model (Stage 3 Scaffolding) | `llama-3.3-70b-versatile` | `llama-3.3-70b-versatile` |

---

## Running with pgvector

Local macOS PostgreSQL on port `5432` remains completely untouched. Stage 2 uses a Docker pgvector container on host port **`5433`**.

### 1. Start the pgvector Container

```bash
docker run -d \
  --name claimwise-pgvector \
  -p 5433:5432 \
  -e POSTGRES_DB=claimwise \
  -e POSTGRES_USER=claimwise \
  -e POSTGRES_PASSWORD=claimwise \
  pgvector/pgvector:pg16
```

### 2. Run the Application

```bash
export DB_PORT=5433
export DB_USERNAME=claimwise
export DB_PASSWORD=claimwise
export HUGGINGFACE_API_KEY=your_hf_token_here

./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`. Flyway automatically executes migrations V1 through V5 and creates the pgvector extension and HNSW index.

---

## How to Run Tests

The test suite executes 73 automated tests covering Stage 1 and Stage 2:

```bash
./mvnw clean test
```

### Test Coverage

- **Customer / Policy / Claim Services**: Business validation, relational mappings, duplicate rejections.
- **Document Ingestion Service**: Apache PDFBox 3 extraction, byte validation, encrypted/empty document handling.
- **Text Chunker**: Deterministic chunk generation, sliding window overlap, edge-case rejection.
- **Embedding Service**: Deterministic unit vectors, 384 dimensions verification, format conversion.
- **Policy Document Service**: Ingestion pipeline orchestration (extract -> chunk -> embed -> persist).
- **Retrieval Service**: Cosine similarity query execution and DTO mapping.
- **Controllers (MockMvc)**: HTTP status codes (201, 200, 400, 404, 409), input validation, response formats.
- **Repository Integration**: Tests database persistence, foreign keys, and unique constraints.

---

## API Endpoints

### Customer Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/customers` | Register a new customer |
| `GET` | `/api/v1/customers` | Retrieve all customers |

### Policy Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/policies` | Create a new insurance policy |
| `GET` | `/api/v1/policies` | Retrieve all policies |
| `GET` | `/api/v1/policies/{policyNumber}` | Retrieve a policy by policy number |

### Claim Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/claims` | File a new claim against an active policy |
| `GET` | `/api/v1/claims` | Retrieve all claims |
| `GET` | `/api/v1/claims/policy/{policyId}` | Retrieve all claims for a given policy ID |

### Policy Document Endpoints (Stage 2)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/policies/{policyId}/documents` | Register document metadata for a policy |
| `GET` | `/api/v1/policies/{policyId}/documents` | Retrieve all documents for a policy |
| `GET` | `/api/v1/documents/{documentId}` | Retrieve a document by ID |
| `POST` | `/api/v1/documents/{documentId}/ingest` | Extract text, chunk, embed, and store in pgvector |

#### Example Register Document Request

```json
POST /api/v1/policies/1/documents
{
  "documentName": "home_insurance_policy_wording.pdf",
  "documentType": "application/pdf",
  "storageReference": "/path/to/home_insurance_policy_wording.pdf"
}
```

#### Example Document Ingestion Response

```json
POST /api/v1/documents/1/ingest
{
  "documentId": 1,
  "extractedCharacterCount": 18450,
  "totalChunksCreated": 39
}
```

### Semantic Retrieval Endpoints (Stage 2)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/retrieval/search` | Execute native pgvector cosine similarity search |

#### Example Retrieval Request

```json
POST /api/v1/retrieval/search
{
  "query": "Is roof damage caused by hailstorm covered under the policy?",
  "policyId": 1,
  "topK": 3
}
```

#### Example Retrieval Response

```json
{
  "query": "Is roof damage caused by hailstorm covered under the policy?",
  "totalResults": 3,
  "results": [
    {
      "chunkId": 14,
      "documentId": 1,
      "chunkIndex": 13,
      "chunkText": "Section 4.2 - Storm and Hail Damage: The policy covers sudden and accidental physical damage to roof structures directly caused by hail or windstorms up to the policy limit of $100,000, subject to a $500 deductible.",
      "similarityScore": 0.8841,
      "metadata": "{\"charStart\": 5850, \"charEnd\": 6350}"
    }
  ]
}
```

---

## OpenAPI & Swagger Documentation

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
