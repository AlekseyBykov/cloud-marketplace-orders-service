## Cloud Marketplace Orders Service

### Overview

Cloud Marketplace Orders Service is a reactive microservice responsible for managing customer orders.

The service is built using **Spring WebFlux** and **R2DBC**, providing non-blocking data access and asynchronous request
handling.

It is designed as part of a larger microservices ecosystem.

The service exposes a reactive REST API for:

* creating customer orders
* retrieving user orders
* validating incoming requests
* handling HTTP/API errors using RFC 7807 Problem Details

## Tech Stack

* Java 21
* Spring Boot 3.2
* Spring WebFlux (reactive)
* Spring Data R2DBC
* PostgreSQL 16
* Flyway (database migrations)
* Testcontainers (integration testing)
* Docker / Docker Compose

### Running the Application

#### 1. Start PostgreSQL via Docker

```bash
cd docker
docker compose up -d
```

Check status:

```bash
docker compose ps
```

#### 2. Run the application

```bash
./gradlew bootRun
```

Application will start on:

```
http://localhost:9092
```

#### 3. Docker Commands Cheat Sheet

##### Start containers

```bash
docker compose up -d
```

##### Stop containers

```bash
docker compose down
```

##### Stop + remove volumes (clean DB)

```bash
docker compose down -v
```

##### View logs

```bash
docker compose logs -f
```

### REST API

The service exposes HTTP endpoints for order management.

Base URL:

```text
http://localhost:9092/v1/orders
```

#### Main endpoints

| Method | Endpoint     | Description        |
|--------|--------------|--------------------|
| POST   | `/v1/orders` | Create a new order |
| GET    | `/v1/orders` | Get user orders    |

#### Authentication model

Authentication and authorization are not implemented yet. Current implementation uses request headers only for
demo/testing purposes.

User identity is passed via HTTP header:

```text
X-User-Name
```

Example:

```http
X-User-Name: username1
```

#### OpenAPI / Swagger UI

Swagger UI:

```text
http://localhost:9092/swagger-ui.html
```

OpenAPI spec:

```text
http://localhost:9092/api-docs
```

### Database

* Database is created via environment variable:

```
POSTGRES_DB=orders_service_db
```

* Schema is managed by **Flyway**
* Migrations are located in:

```
src/main/resources/db/migration
```

Example:

```
V1__create_orders_table.sql
```

### Reactive Notes

* Uses **R2DBC** (non-blocking database access)
* No Hibernate / JPA
* All repository methods return:

    * `Flux<T>` — stream of elements
    * `Mono<T>` — single element

⚠️ Important:

* Avoid blocking calls inside reactive pipelines
* Flyway runs via JDBC (blocking) **only at startup**

### Testing

The project uses several testing approaches depending on the application layer.

#### Repository Integration Testing

Repository tests use:

* `@DataR2dbcTest`
* PostgreSQL via Testcontainers
* real R2DBC interaction
* manually initialized schema and test data

These tests verify:

* reactive repository queries
* sorting and pagination
* database interaction
* R2DBC mappings

Flyway is intentionally disabled in repository slice tests to keep them lightweight and isolated.

#### Full HTTP Integration Testing

Controller-level integration tests use:

* `@SpringBootTest`
* `WebTestClient`
* real Spring Boot application context
* PostgreSQL via Testcontainers

These tests verify the complete request flow:

```text
HTTP → Controller → Service → Repository → Database
```

Covered scenarios include:

* request validation
* reactive request handling
* exception handling
* RFC 7807 Problem Details
* HTTP status codes
* database persistence

#### External HTTP Dependency Testing

External HTTP dependencies (Menu Service) are replaced with controlled test servers.

* **WireMock** — simulates external API behavior using request/response stubs.
* **MockWebServer** — simulates low-level HTTP client scenarios (timeouts, retries, failures, delayed responses).

This allows:

* deterministic test execution
* reproducible edge cases
* verification of outgoing HTTP calls
* testing retry and timeout behavior

#### Running tests

```bash
./gradlew test
```

### Additional Notes

* Flyway uses JDBC (blocking) but only during startup — this is expected
* R2DBC is used for runtime operations (non-blocking)
* Docker is required for integration tests
* External services are mocked in tests (no real HTTP calls)
