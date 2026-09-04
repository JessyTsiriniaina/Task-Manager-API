# Task Manager API

A production-ready REST API for personal task management, built with **Spring Boot 4**, **Spring Security + JWT**, **Spring Data JPA** and **PostgreSQL**.

Users can register, authenticate with email/password, and manage their own tasks (CRUD, status/pagination/filtering). Every request is scoped to the authenticated user — a user can never access another user's tasks.

> Why this project exists: it demonstrates a professional backend engineering workflow — layered architecture, DTO mapping, validation, centralized error handling, JWT auth with refresh-token rotation, database migrations with Flyway, a Dockerized deployment, and a 120+ test suite (unit, slice and security tests).

---

## Features

- **Authentication**: register, login, refresh-token rotation, logout
  - Access token: signed JWT (HS256, short-lived, 15 min)
  - Refresh token: opaque token stored hashed (SHA-256) in the DB, rotation on every use
  - Logout revokes the refresh tokens and blocks the access token's `jti` until expiry
- **Tasks**: full CRUD + partial status update (`PATCH`)
- **Filtering**: by `status`, `priority`, and case-insensitive `title` contains
- **Pagination**: `page` / `size` (Spring Data `Page`)
- **Validation**: Bean Validation on all payloads (`@NotBlank`, `@Email`, `@Size`, `@FutureOrPresent`, …)
- **Error handling**: uniform `ErrorResponse` JSON from a `@RestControllerAdvice`
- **OpenAPI / Swagger UI**: documented endpoints, request/response schemas, bearer auth
- **Database migrations**: Flyway versioned SQL (validated at startup, `ddl-auto: validate` in prod)
- **Actuator**: `/actuator/health` with Kubernetes-style liveness/readiness probes
- **Tests**: 123 tests — service unit tests (Mockito), controller slice tests (MockMvc), repository tests (`@DataJpaTest`), and JWT/security happy-path tests

---

## Tech Stack

| Layer      | Technology                                                     |
|------------|----------------------------------------------------------------|
| Language   | Java 25                                                        |
| Framework  | Spring Boot 4.1.0 (Spring MVC, Data JPA, Security, Validation) |
| Security   | Spring Security + JWT (`jjwt 0.12`)                            |
| Database   | PostgreSQL 16 (H2 in tests)                                    |
| Migrations | Flyway 12                                                      |
| Docs       | Springdoc OpenAPI 3 / Swagger UI                               |
| Build      | Maven (wrapper)                                                |
| DevOps     | Docker, Docker Compose, GitHub Actions CI                      |

---

Layered packages under `io.jessytsiriniaina.taskmanagerapi`:

```
config/       Spring MVC / OpenAPI configuration
controller/   REST endpoints (DTO in / DTO out)
dto/          Request/response records + validation
entity/       JPA entities
enum/         TaskStatus, TaskPriority
exception/    Domain exceptions
exceptionhandler/  @RestControllerAdvice handlers
mapper/       Entity ↔ DTO mapping
repository/   Spring Data JPA repositories (user-scoped queries)
security/     SecurityFilterChain, JWT service, auth filter, entry point
service/      Business logic
```

---

## Getting Started

### Prerequisites

- Java 25 (JDK)
- Maven (or use the included `./mvnw` wrapper)
- PostgreSQL 16 (local) **or** Docker + Docker Compose

### 1. Clone and configure

```bash
git clone git@github.com:JessyTsiriniaina/Task-Manager-API.git
cd Task-Manager-API

cp .env.example .env
```

Edit `.env` with your own values:

```dotenv
POSTGRES_URL=jdbc:postgresql://localhost:5432/task_manager_db
POSTGRES_USERNAME=postgres
POSTGRES_PASSWORD=change-me

# base64-encoded, at least 256 bits (32 bytes). Generate: openssl rand -base64 64
JWT_SECRET=change-me-base64-encoded-at-least-32-bytes

JWT_EXPIRATION=900000            # 15 minutes (ms)
JWT_REFRESH_EXPIRATION=2592000000 # 30 days (ms)
```

### 2. Run locally

Create the database first:

```sql
CREATE DATABASE task_manager_db;
```

Then run:

```bash
./mvnw spring-boot:run
```

The schema is created by Flyway and validated by Hibernate. The API is available at `http://localhost:8080`.

### 3. Run with Docker Compose (production profile)

```bash
docker compose up --build
```

This starts:

- `app` — the Spring Boot API on `http://localhost:8080`
- `db` — PostgreSQL 16 (named volume `db_data`)

The app waits for the database to be healthy, then runs Flyway migrations on startup.

---

## Configuration

All configuration lives in environment variables (never hard-coded):

| Variable                    | Description                                          | Default            |
|-----------------------------|------------------------------------------------------|--------------------|
| `POSTGRES_URL`              | JDBC URL for PostgreSQL                              | — (required)       |
| `POSTGRES_USERNAME`         | Database user                                        | — (required)       |
| `POSTGRES_PASSWORD`         | Database password                                    | — (required)       |
| `POSTGRES_DB`               | Database name (Docker only)                          | `task_manager_db`  |
| `JWT_SECRET`                | base64-encoded HMAC key (≥ 32 bytes)                 | — (required)       |
| `JWT_EXPIRATION`            | Access token lifetime in ms                          | `900000` (15 min)  |
| `JWT_REFRESH_EXPIRATION`    | Refresh token lifetime in ms                         | `2592000000` (30 d)|

Profiles:

- **default** (`application.yaml`) — dev-oriented baseline; Flyway enabled, schema validated
- **prod** (`application-prod.yaml`) — `ddl-auto: validate`, hidden actuator details, INFO logs, Swagger kept available
- **test** (`application-test.yaml`) — H2 in PostgreSQL mode, Flyway runs the same `V1` migration used in prod

> The project ships without default secrets. The app fails fast at startup if `JWT_SECRET` is missing or invalid.

---

## API Endpoints

Base path: `/api` (all controllers are prefixed automatically). All task endpoints require `Authorization: Bearer <accessToken>`.

### Authentication

| Method | Endpoint            | Description                                          | Errors           |
|--------|---------------------|------------------------------------------------------|------------------|
| POST   | `/api/auth/register`| Register a user and get a token pair                  | 400, 409         |
| POST   | `/api/auth/login`   | Login with email + password and get a token pair      | 400, 401         |
| POST   | `/api/auth/refresh` | Exchange a refresh token for a new token pair         | 400, 401         |
| POST   | `/api/auth/logout`  | Revoke current access + refresh tokens                | 401              |

### Tasks (authenticated)

| Method | Endpoint                  | Description                                     |
|--------|---------------------------|-------------------------------------------------|
| GET    | `/api/tasks`              | List tasks (filter + paginate)                   |
| POST   | `/api/tasks`              | Create a task                                    |
| GET    | `/api/tasks/{id}`         | Get one task                                     |
| PUT    | `/api/tasks/{id}`         | Replace all task fields                          |
| PATCH  | `/api/tasks/{id}`         | Update only the task status                      |
| DELETE | `/api/tasks/{id}`         | Delete a task                                    |

`GET /api/tasks` query parameters:

| Param      | Type   | Description                                                        |
|------------|--------|--------------------------------------------------------------------|
| `status`   | enum   | `TODO`, `IN_PROGRESS`, `DONE`                                      |
| `priority` | enum   | `LOW`, `MEDIUM`, `HIGH`                                            |
| `title`    | string | Case-insensitive substring match                                    |
| `page`     | int    | Zero-based page index (must be combined with `size`)                |
| `size`     | int    | Page size (must be combined with `page`)                            |

> `page` and `size` must be provided together; providing only one returns `400`.

---

## Examples

### Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jessy",
    "email": "jessy@example.com",
    "password": "strongPass123"
  }'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "jessy@example.com", "password": "strongPass123"}'
```

### Create a task

```bash
TOKEN="<access-token>"

curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Write Swagger documentation",
    "description": "Document every endpoint with Springdoc",
    "status": "TODO",
    "priority": "HIGH",
    "dueDate": "2026-09-10T17:30:00"
  }'
```

```json
{
  "id": 1,
  "userId": 1,
  "title": "Write Swagger documentation",
  "description": "Document every endpoint with Springdoc",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2026-09-10T17:30:00",
  "createdAt": "2026-09-02T10:00:00",
  "updatedAt": "2026-09-02T10:00:00"
}
```

### List with filters and pagination

```bash
curl -G "http://localhost:8080/api/tasks" \
  -H "Authorization: Bearer $TOKEN" \
  --data-urlencode "status=TODO" \
  --data-urlencode "priority=HIGH" \
  --data-urlencode "title=swagger" \
  --data-urlencode "page=0" \
  --data-urlencode "size=10"
```

### Update status only

```bash
curl -X PATCH http://localhost:8080/api/tasks/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"status": "IN_PROGRESS"}'
```

### Refresh & logout

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<refresh-token>"}'

curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

---

## Error format

Every error (400, 401, 404, 409, ...) uses a consistent JSON shape:

```json
{
  "status": 404,
  "message": "Task not found",
  "timestamp": "2026-09-02T10:00:00"
}
```

---

## Swagger / OpenAPI

Interactive documentation is available at:

- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

Use the **Authorize** button to paste an access token and try the authenticated endpoints directly.

---

## Health checks (Actuator)

```bash
curl http://localhost:8080/actuator/health
```

```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

- `GET /actuator/health/liveness` and `GET /actuator/health/readiness` are enabled (Kubernetes probes).
- The Docker Compose setup uses the readiness health as its container healthcheck.

---

## Database migrations (Flyway)

Migrations live in `src/main/resources/db/migration` and run automatically at startup:

| Version | Script                  | Content                                        |
|---------|-------------------------|------------------------------------------------|
| V1      | `V1__init_schema.sql`   | Initial schema: `users`, `task`, `refresh_tokens`, `blocked_tokens` + sequences + FKs |

In production (`ddl-auto: validate`) Hibernate **validates** that the entity mapping matches the migrated schema, so schema drift fails loudly at boot instead of silently.

---

## Tests

```bash
./mvnw test
```

The suite (123 tests) covers:

- **Service unit tests** (Mockito) — task CRUD, auth, refresh rotation, token blocking
- **Controller tests** (MockMvc) — status codes, validation errors, auth failures
- **Repository tests** (`@DataJpaTest` + H2) — data access, unique constraints, specifications
- **Security** — unauthenticated access is denied; a user cannot read/update/delete another user's tasks

Tests use H2 in PostgreSQL compatibility mode and run the **same Flyway migrations** as production, validating the scripts on every build.

---

## CI / CD

GitHub Actions (`.github/workflows/ci.yml`):

1. **build-and-test** — JDK 25, Maven cache, `./mvnw test`, package, upload JAR artifact
2. **docker-build** — builds the Docker image (BuildKit + GHA cache), no registry push

Triggers: push to `main` / `develop`, pull requests targeting `main`.

---

## Project structure

```
.
├── .github/workflows/ci.yml      # CI pipeline
├── .env.example                  # Template for environment variables
├── Dockerfile                    # Multi-stage build (JDK 25 → slim JRE)
├── docker-compose.yml            # app + PostgreSQL (healthchecks, volumes)
├── pom.xml
├── mvnw / mvnw.cmd               # Maven wrapper
└── src/
    ├── main/
    │   ├── java/io/jessytsiriniaina/taskmanagerapi/
    │   └── resources/
    │       ├── application.yaml
    │       ├── application-prod.yaml
    │       └── db/migration/V1__init_schema.sql
    └── test/
        ├── resources/application-test.yaml
        └── java/io/jessytsiriniaina/taskmanagerapi/
```

---

## Licence

This project is for learning and portfolio purposes.
