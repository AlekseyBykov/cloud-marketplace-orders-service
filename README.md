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

Authentication is not implemented yet.

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

The project uses a combination of integration and HTTP-level testing.

#### Integration Testing

Integration tests run against real infrastructure:

* **PostgreSQL (Testcontainers)** — real database instance
* **Spring Boot context** — full application context is started
* **StepVerifier** — for reactive flow assertions

This allows testing:

* persistence layer (R2DBC)
* service layer logic
* reactive pipelines

#### HTTP Integration Testing

External HTTP dependencies (Menu Service) are not called directly in tests.

Instead, they are replaced with controlled test servers:

* **WireMock** — used for HTTP integration testing
  (simulates external API behavior via request/response stubs)

* **MockWebServer** — used for HTTP client testing
  (simulates low-level scenarios like retries, timeouts, failures)

This approach allows:

* deterministic test execution
* simulation of edge cases (timeouts, 5xx, partial responses)
* verification of outgoing HTTP calls

#### Running tests

```bash
./gradlew test
```

### Additional Notes

* Flyway uses JDBC (blocking) but only during startup — this is expected
* R2DBC is used for runtime operations (non-blocking)
* Docker is required for integration tests
* External services are mocked in tests (no real HTTP calls)
