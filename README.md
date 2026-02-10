# Trainer Workload Service

A Spring Boot microservice responsible for tracking and aggregating trainer monthly working hours within a gym management system. The service exposes a REST API for updating and querying trainer workload data, secured with JWT authentication and integrated with a Netflix Eureka service registry.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Security](#security)
- [Database](#database)
- [Logging and Tracing](#logging-and-tracing)

---

## Overview

The Trainer Workload Service is a stateless microservice designed to:

- Accept workload update commands (add or delete training minutes) for a given trainer, year, and month
- Aggregate and return a structured summary of a trainer's total training hours grouped by year and month
- Validate incoming JWT tokens issued by a central authentication service
- Register with a Eureka service registry for service discovery

---

## Architecture

The service follows a **hexagonal (ports and adapters) architecture**, ensuring a clean separation between business logic and infrastructure concerns.

```
interfaces/web          - HTTP controllers, request/response DTOs, MapStruct mappers
application/service     - Business logic and use case orchestration
domain/model            - Core domain entities
domain/port             - Repository interfaces (ports)
infrastructure          - JPA persistence, security, logging (adapters)
```

---

## Technology Stack

| Component          | Technology                          |
|--------------------|-------------------------------------|
| Language           | Java 21                             |
| Framework          | Spring Boot 3.5.x                   |
| Security           | Spring Security with JWT (JJWT 0.12)|
| Persistence        | Spring Data JPA / Hibernate         |
| Database           | H2 (in-memory)                      |
| Service Discovery  | Netflix Eureka Client               |
| API Documentation  | SpringDoc OpenAPI / Swagger UI      |
| Build Tool         | Maven                               |
| Code Coverage      | JaCoCo                              |

---

## Prerequisites

- Java 21 or later
- Maven 3.8 or later
- A running Eureka Server instance (default: `http://localhost:8761/eureka/`)
- A compatible JWT secret shared with the issuing authentication service

---

## Getting Started

### Clone and Build

```bash
git clone git@github.com:abay-kulamkadyr/trainer-workload-service.git
cd trainer-workload-service
./mvnw clean install
```

### Run the Application

```bash
./mvnw spring-boot:run
```

The service starts on port `8081` by default.

### Run Tests

```bash
./mvnw test
```

### Apply Code Formatting

```bash
./mvnw spotless:apply
```

### Check Code Formatting

```bash
./mvnw spotless:check
```

---

## Configuration

All configuration is managed via `src/main/resources/application.yml`.

### Key Properties

| Property                                | Default Value                          | Description                                      |
|-----------------------------------------|----------------------------------------|--------------------------------------------------|
| `server.port`                           | `8081`                                 | HTTP port the service listens on                 |
| `spring.datasource.url`                 | `jdbc:h2:mem:workload_db`              | In-memory H2 database URL                        |
| `eureka.client.serviceUrl.defaultZone`  | `http://localhost:8761/eureka/`        | Eureka server registration endpoint              |
| `security.jwt.secret`                   | (required)                             | HMAC-SHA secret key for JWT verification         |
| `security.jwt.lifetime`                 | `30m`                                  | Token lifetime (informational, not enforced here)|
| `security.cors.allowed-origins`         | `http://localhost:8080`                | Comma-separated list of allowed CORS origins     |

### Environment-Specific Configuration

For production deployments, override sensitive properties using environment variables or an external configuration server. In particular:

- `security.jwt.secret` should never be hardcoded. Inject it via environment variable or a secrets manager.
- `spring.datasource.url` should point to a persistent database.
- `spring.h2.console.enabled` should be set to `false`.

---

## API Reference

### Base URL

```
http://localhost:8081/api/workload
```

### Endpoints

#### Update Trainer Workload

```
POST /api/workload
```

Adds or removes training minutes for a trainer for a specific month. If no record exists for the given trainer, year, and month, a new one is created automatically.

**Request Body**

```json
{
  "trainerUsername": "john.doe",
  "trainerFirstname": "John",
  "trainerLastname": "Doe",
  "isActive": true,
  "trainingDate": "2025-06-15T10:00:00",
  "trainingDurationMinutes": 60,
  "actionType": "ADD"
}
```

| Field                    | Type      | Required | Description                                |
|--------------------------|-----------|----------|--------------------------------------------|
| `trainerUsername`        | String    | Yes      | Unique identifier for the trainer          |
| `trainerFirstname`       | String    | Yes      | Trainer first name                         |
| `trainerLastname`        | String    | Yes      | Trainer last name                          |
| `isActive`               | Boolean   | Yes      | Whether the trainer is currently active    |
| `trainingDate`           | DateTime  | Yes      | ISO 8601 date-time of the training session |
| `trainingDurationMinutes`| Integer   | Yes      | Duration in minutes (minimum: 1)           |
| `actionType`             | Enum      | Yes      | `ADD` or `DELETE`                          |

**Response (200 OK)**

```json
{
  "username": "john.doe",
  "year": "2025",
  "month": "JUNE",
  "trainingDurationMinutes": 120
}
```

**Error Responses**

| Status | Condition                                                         |
|--------|-------------------------------------------------------------------|
| `400`  | Request validation failed (missing or invalid fields)            |
| `401`  | Missing or invalid JWT token                                      |
| `422`  | DELETE action would reduce total duration below zero             |

---

#### Get Trainer Workload Summary

```
GET /api/workload/{username}
```

Returns the full workload history for a trainer, grouped by year and month, ordered chronologically.

**Path Parameters**

| Parameter  | Type   | Description                    |
|------------|--------|--------------------------------|
| `username` | String | The trainer's unique username  |

**Response (200 OK)**

```json
{
  "username": "john.doe",
  "firstName": "John",
  "lastName": "Doe",
  "status": true,
  "years": [
    {
      "year": 2024,
      "months": [
        { "month": "NOVEMBER", "trainingSummaryDuration": 180 },
        { "month": "DECEMBER", "trainingSummaryDuration": 240 }
      ]
    },
    {
      "year": 2025,
      "months": [
        { "month": "JANUARY", "trainingSummaryDuration": 300 }
      ]
    }
  ]
}
```

**Error Responses**

| Status | Condition                                       |
|--------|-------------------------------------------------|
| `401`  | Missing or invalid JWT token                    |
| `404`  | No workload records found for the given username|

---

### Interactive API Documentation

Swagger UI is available at:

```
http://localhost:8081/swagger-ui/index.html
```

OpenAPI specification (JSON):

```
http://localhost:8081/v3/api-docs
```

---

## Security

The service uses stateless JWT-based authentication. All endpoints (except Swagger UI and H2 console) require a valid Bearer token.

### Authentication Flow

1. The client obtains a JWT from an external authentication service.
2. The token is included in the `Authorization` header of each request.
3. The `JwtAuthenticationFilter` extracts and delegates the token to `JwtAuthenticationProvider`.
4. The provider validates the token signature and expiration using the configured HMAC-SHA secret.
5. On success, the authenticated principal is stored in the Spring Security context for the duration of the request.

### Request Header

```
Authorization: Bearer <token>
```

### Permitted Paths (No Authentication Required)

```
/h2-console/**
/v3/api-docs/**
/swagger-ui/**
```

### CORS Configuration

Allowed origins are configured via `security.cors.allowed-origins`. Multiple origins can be provided as a comma-separated list.

---

## Database

The service uses an **H2 in-memory database** suitable for development and testing. The schema is initialized automatically on startup via `src/main/resources/schema.sql`.

### Schema

```sql
CREATE TABLE trainer_workload (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(255) NOT NULL,
    first_name     VARCHAR(255),
    last_name      VARCHAR(255),
    is_active      BOOLEAN,
    training_year  INT NOT NULL,
    training_month INT NOT NULL,
    duration_min   INT DEFAULT 0,
    CONSTRAINT unique_trainer_month UNIQUE (username, training_year, training_month),
    CONSTRAINT positive_duration    CHECK (duration_min >= 0)
);

CREATE INDEX idx_trainer_lookup ON trainer_workload (username, training_year, training_month);
```

### H2 Console

The H2 web console is available during development at:

```
http://localhost:8081/h2-console
```

JDBC URL: `jdbc:h2:mem:workload_db`

---

## Logging and Tracing

### Transaction ID Propagation

Every incoming HTTP request is assigned a unique transaction ID in the format `TXN-<UUID>`. If the client provides an `X-Transaction-Id` header, that value is used instead. The transaction ID is:

- Added to the SLF4J MDC for inclusion in all log messages within the request thread
- Returned in the `X-Transaction-Id` response header
- Included in structured error response bodies

### Log Pattern

```
yyyy-MM-dd HH:mm:ss.SSS  LEVEL  [thread]  [TxnId: <id>]  logger  : message
```

### Log Levels

| Logger Area              | Level  | Content                                         |
|--------------------------|--------|-------------------------------------------------|
| Incoming requests        | INFO   | Method, URI, query string, remote address       |
| Completed requests       | INFO   | Method, URI, HTTP status                        |
| Failed requests          | ERROR  | Method, URI, HTTP status, exception message     |
| Request headers          | DEBUG  | All request headers                             |
| JWT validation           | DEBUG  | Token validation outcome and username           |
| Business operations      | DEBUG  | Action type and trainer username                |

---