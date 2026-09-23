# Insurance Policy Management API

A RESTful backend application for managing clients, insurance policies, and claims, built with Java 25 and Spring Boot.

## Table of Contents
- [Features](#features)
- [Technologies Used](#technologies-used)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Environment Configuration](#environment-configuration)
- [API Documentation](#api-documentation)
- [API Endpoints](#api-endpoints)
- [License](#license)

## Features
- **Client Management**: Create, update, delete, and retrieve clients.
- **Insurance Policy Management**: Create, update, delete, and retrieve insurance policies for clients.
- **Claim Management**: Create, update, delete, and retrieve claims associated with policies.
- **Authentication & Security**: User authentication with JWT tokens and Spring Security.
- **API Documentation**: Interactive documentation using OpenAPI 3 and Swagger UI.

## Technologies Used
- Java 25 LTS
- Spring Boot 4.0.0
- Spring Data JPA & Hibernate 7
- Spring Security 7 & JWT (jjwt)
- MySQL Database
- Springdoc OpenAPI 3 (Swagger UI)
- Lombok 1.18.48
- ModelMapper 3.1.1
- H2 Database (for testing)

## Prerequisites
Before running the application, make sure you have:
- **JDK 25** installed
- **MySQL 8.0+** running locally

## Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/j-anurag/enterprise-insurance-ai-agent-platform.git
   cd enterprise-insurance-ai-agent-platform
   ```

2. **Configure Database**:
   Create the database in MySQL:
   ```sql
   CREATE DATABASE IF NOT EXISTS insurance;
   ```
   Set up your credentials using environment variables or in `src/main/resources/application.properties`:
   - `DB_HOST`: Database host (default: `localhost`)
   - `DB_PORT`: Database port (default: `3306`)
   - `DB_NAME`: Database name (default: `insurance`)
   - `DB_USERNAME`: Database username (default: `root`)
   - `DB_PASSWORD`: Your MySQL password

   You can also copy `.env.example` to `.env` and set your credentials.

3. **Build the project**:
   ```bash
   ./mvnw clean install
   ```

4. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

   The application will start on `http://localhost:8080`.

## API Documentation
Once the application is running, you can access the interactive Swagger UI documentation at:
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## API Endpoints

### Public Endpoints
- `GET /welcome` - Welcome health check endpoint
- `POST /register` - Register a new client
- `POST /login` - User login
- `GET /signIn` - Get current authenticated user details

### Client Endpoints
- `GET /api/clients/` - Get all clients
- `GET /api/clients/{id}` - Get client by ID
- `POST /api/clients/` - Create a new client
- `PUT /api/clients/{id}` - Update client details
- `DELETE /api/clients/{id}` - Delete client

### Policy Endpoints
- `GET /api/policies/` - Get all insurance policies
- `GET /api/policies/{id}` - Get policy by ID
- `POST /api/policies/` - Create a new policy
- `PUT /api/policies/{id}` - Update policy
- `DELETE /api/policies/{id}` - Delete policy

### Claim Endpoints
- `GET /api/claims/` - Get all claims
- `GET /api/claims/{id}` - Get claim by ID
- `POST /api/claims/` - File a new claim
- `PUT /api/claims/{id}` - Update claim status
- `DELETE /api/claims/{id}` - Delete claim
- `GET /api/claims/client/{id}` - Get all claims for a client

## License
This project is licensed under the MIT License.
