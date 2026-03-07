# Trainer Workload Service

A microservice responsible for tracking and aggregating trainer training hours across the gym ecosystem. It receives training events from the Gym CRM via Kafka and exposes a REST API for querying trainer workload summaries.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
    - [Configuration](#configuration)
    - [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Security](#security)
- [Database](#database)
- [Messaging](#messaging)
- [Logging](#logging)
- [Development](#development)
- [Testing](#testing)
- [License](#license)

## Overview

The Trainer Workload Service maintains a running summary of training hours per trainer, broken down by year and month. It is updated asynchronously via Kafka events published by the Gym CRM whenever a training session is created or deleted, and can also be updated directly via its REST API.

## Architecture

The service follows the same **Onion Architecture** conventions as the Gym CRM:

```
┌─────────────────────────────────────────┐
│         Interfaces Layer                │
│  REST Controllers, DTOs,                │
│  Kafka Listener, Web Mappers            │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│        Application Layer                │
│   TrainerWorkloadService,               │
│   Commands, Responses, Exceptions       │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│          Domain Layer                   │
│   TrainerWorkload (immutable),          │
│   TrainerWorkloadRepository (port)      │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│      Infrastructure Layer               │
│ MongoDB persistence, Security,          │
│ Logging (MDC / Transaction ID)          │
└─────────────────────────────────────────┘
```

### Layer Responsibilities

- **Domain Layer**: `TrainerWorkload` value object (Lombok `@Value` + `@With` for immutability) and the `TrainerWorkloadRepository` port
- **Application Layer**: Business logic for adding/subtracting durations, exception definitions, command/response DTOs
- **Infrastructure Layer**: MongoDB adapter, JWT validation, `TransactionIdFilter`, request/response logging interceptor
- **Interfaces Layer**: REST controller, Kafka event listener, MapStruct mapper, web DTOs

## Features

- **Event-driven workload tracking** — consumes `TrainerWorkloadEvent` messages from Kafka and updates trainer summaries automatically
- **REST API** — exposes endpoints to update workload directly and query summaries by trainer username
- **Year/month breakdown** — durations are stored and returned per trainer, year, and month
- **Immutable domain model** — `TrainerWorkload` uses `@Value`/`@With`; all mutations produce a new instance
- **JWT authentication** — all endpoints require a valid JWT issued by the Gym CRM
- **Transaction ID propagation** — `X-Transaction-Id` header is forwarded from Kafka events and HTTP requests into MDC for end-to-end log correlation
- **Global exception handling** — structured `ErrorResponse` with transaction ID for `404 Not Found` and `422 Unprocessable Entity`

## Technology Stack

### Core Framework
- **Spring Boot 3.x** — Application framework
- **Java 21** — Programming language

### Persistence
- **Spring Data MongoDB** — Data access layer
- **MongoDB** — Document store (trainer workloads stored as nested year/month summaries)

### Messaging
- **Apache Kafka** — Asynchronous event consumption
- **Spring Kafka** — Kafka listener integration

### Security
- **Spring Security 6.x** — Security framework
- **JWT (JSON Web Tokens)** — Stateless authentication (token issued by Gym CRM, validated here)

### Service Discovery
- **Spring Cloud Netflix Eureka Client** — Registers with Eureka so the Gym CRM can discover this service by name

### Documentation
- **SpringDoc OpenAPI** — Swagger UI

### Build
- **Maven** — Dependency management and build tool

## Getting Started

### Prerequisites

- Java 21
- Maven 3.6+
- MongoDB instance (local Docker or Atlas)
- Kafka broker (shared with Gym CRM)

### Installation

1. **Clone the repository**
   ```bash
   git clone git@github.com:abay-kulamkadyr/trainer-workload-service.git
   cd trainer-workload-service
   ```

2. **Build the project**
   ```bash
   ./mvnw clean install
   ```

### Configuration

Key properties (`src/main/resources/application.yaml`):

```yaml
server:
  port: 8081

spring:
  application:
    name: trainer-workload-service
  data:
    mongodb:
      uri: ${MONGODB_URI}
      auto-index-creation: false

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    consumer:
      key-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        spring.deserializer.key.delegate.class: org.apache.kafka.common.serialization.StringDeserializer
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
        spring.json.use.type.headers: false
        spring.json.trusted.packages: "*"
        spring.json.value.default.type: com.epam.workload.interfaces.messaging.event.TrainerWorkloadEvent

eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}

security:
  jwt:
    secret: ${JWT_SECRET}   # Must match the secret used by Gym CRM
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:8080}

app:
  kafka:
    topics:
      training-created: gym.trainings.created
```

**Important**: `security.jwt.secret` must be the same value configured in the Gym CRM — the workload service only validates tokens, it does not issue them.

To disable Eureka for local development without a registry:
```yaml
eureka:
  client:
    enabled: false
    register-with-eureka: false
    fetch-registry: false
```

### Running the Application

```bash
# Start MongoDB 
docker compose up -d

./mvnw spring-boot:run
```

#### Using JAR

```bash
./mvnw clean package
java -jar target/trainer-workload-service-*.jar
```

The application starts on `http://localhost:8081`.

## API Documentation

### Swagger UI

```
http://localhost:8081/swagger-ui/index.html
```

### OpenAPI Specification

```
http://localhost:8081/v3/api-docs
```

### Endpoints

All endpoints require a valid `Authorization: Bearer <token>` header.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/workload` | Add or delete training hours for a trainer |
| GET | `/api/workload/{username}` | Get workload summary for a trainer |

#### POST `/api/workload` — Update Workload

Request body:
```json
{
  "trainerUsername": "Jane.Smith",
  "trainerFirstname": "Jane",
  "trainerLastname": "Smith",
  "isActive": true,
  "trainingDate": "2025-06-01T09:00:00",
  "trainingDurationMinutes": 60,
  "actionType": "ADD"
}
```

`actionType` is `ADD` or `DELETE`. Returns `200` with `{ "username": "Jane.Smith" }` on success, `422` if a DELETE would result in negative duration.

#### GET `/api/workload/{username}` — Get Summary

```bash
curl http://localhost:8081/api/workload/Jane.Smith \
  -H "Authorization: Bearer <token>"
```

Response:
```json
{
  "username": "Jane.Smith",
  "firstName": "Jane",
  "lastName": "Smith",
  "status": true,
  "years": [
    {
      "year": 2025,
      "months": [
        { "month": "JUNE", "trainingSummaryDuration": 120 }
      ]
    }
  ]
}
```

Returns `404` if the trainer has no workload record yet.

## Security

### Authentication

All endpoints are protected by JWT. The token must be issued by the Gym CRM using the shared secret. The workload service validates the token using `JwtAuthenticationFilter` → `JwtAuthenticationProvider` → `JwtTokenValidationAdapter` (JJWT).

There is no login endpoint — clients authenticate via the Gym CRM and pass the token in the `Authorization: Bearer <token>` header.

### CORS

Configurable via `security.cors.allowed-origins`. Defaults to `http://localhost:8082`.

## Database

### MongoDB Document Structure

Workloads are stored in the `trainer_workloads` collection. Each document represents one trainer:

```json
{
  "_id": "...",
  "username": "Jane.Smith",
  "firstName": "Jane",
  "lastName": "Smith",
  "active": true,
  "years": [
    {
      "year": 2025,
      "months": [
        { "month": 6, "durationMin": 120 },
        { "month": 7, "durationMin": 60 }
      ]
    }
  ]
}
```

`username` has a unique index. The compound index `{ firstName: 1, lastName: 1 }` supports name-based lookups.

## Messaging

### Kafka Consumer

The service listens on the `gym.trainings.created` topic (configurable via `app.kafka.topics.training-created`) using the application name as the consumer group ID.

#### Event Schema — `TrainerWorkloadEvent`

```json
{
  "trainerUsername": "Jane.Smith",
  "trainerFirstname": "Jane",
  "trainerLastname": "Smith",
  "isActive": true,
  "trainingDate": "2025-06-01T09:00:00",
  "trainingDurationMinutes": 60,
  "actionType": "ADD",
  "transactionId": "TXN-1748700000000-a1b2c3d4"
}
```

#### Error Handling

| Scenario | Behaviour |
|----------|-----------|
| `InsufficientDurationException` (DELETE below zero) | Logged as error, offset committed — **no retry, no DLQ** |
| Any other exception | Rethrown — triggers Kafka retry |

The intentional swallowing of `InsufficientDurationException` means a bad DELETE event will never block the consumer, but the duration will remain unchanged. A dead-letter topic or alerting would be needed for production visibility.

## Logging

All logs include a transaction ID:

```
INFO [TxnId: TXN-1748700000000-a1b2c3d4] Processing ADD request for trainer: Jane.Smith [2025/JUNE]
```

Transaction IDs are:
- Extracted from the `X-Transaction-Id` header on HTTP requests (or a new UUID generated if absent)
- Extracted from the `transactionId` field of incoming Kafka events
- Stored in MDC for automatic inclusion in all log lines within the same thread

## Development

### Adding a New Event Type

1. Add the event POJO to `interfaces/messaging/event/`
2. Add a new `@KafkaListener` method in a listener class under `interfaces/messaging/listener/`
3. Map the event to an application-layer command and delegate to the relevant service

### Adding a New Query

1. Add the query method to `TrainerWorkloadRepository` (port) and implement it in `TrainerWorkloadRepositoryImpl`
2. Add the service method to `TrainerWorkloadService` and `TrainerWorkloadServiceImpl`
3. Expose via a new controller method and update the Swagger interface in `TrainerWorkloadControllerApi`

## Testing

### Running Tests

```bash
# All tests
./mvnw test

# Specific test class
./mvnw test -Dtest=TrainerWorkloadServiceImplIntegrationTest

# With coverage report
./mvnw test jacoco:report
```

### Test Strategy

| Layer | Approach |
|-------|----------|
| Domain / utilities | Pure JUnit 5 unit tests |
| Service | Integration tests against real MongoDB via Testcontainers |
| Kafka listener | Integration tests with real Kafka via Testcontainers |
| Controllers | `@WebMvcTest` slices with MockMvc |

Integration tests extend `BaseIntegrationTest` which spins up MongoDB and Kafka via Testcontainers and wires them in using `@ServiceConnection`. Each test cleans its own data in `@BeforeEach`.

## License

This project is licensed under the MIT License — see the LICENSE file for details.

---

## Acknowledgments

- Thanks to [@ValeriyNechayev](https://github.com/ValeriyNechayev) at EPAM Systems for mentorship and architectural/code review guidance.