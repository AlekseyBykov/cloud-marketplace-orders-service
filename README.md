## Cloud Marketplace Orders Service

### Overview

Cloud Marketplace Orders Service is a reactive microservice responsible for managing customer orders.

The service is built using **Spring WebFlux** and **R2DBC**, providing non-blocking data access and asynchronous request handling. 
It is designed as part of a larger microservices ecosystem.

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

### Start containers

```bash
docker compose up -d
```

### Stop containers

```bash
docker compose down
```

### Stop + remove volumes (clean DB)

```bash
docker compose down -v
```

### View logs

```bash
docker compose logs -f
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

Integration tests use:

* Testcontainers (PostgreSQL)
* StepVerifier (Reactor testing)

Run tests:

```bash
./gradlew test
```

### Known Notes

* Flyway uses JDBC (blocking) but only during startup — this is expected
* R2DBC is used for runtime operations (non-blocking)
* Docker is required for integration tests
