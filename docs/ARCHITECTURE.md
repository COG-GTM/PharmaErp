# Pharma ERP — architecture: legacy vs target

The modernisation replaces the Java 8 servlet/JSP application with a Spring Boot 3 REST backend and a React SPA,
packaged as a Docker Compose estate. The legacy code is kept unchanged in the repository and runs side by side in
the parity harness (`parity/legacy`) so behaviour can be compared flow by flow.

Detailed coding rules (module folders, Flyway ranges, test base classes) live in
[`modernisation/CONVENTIONS.md`](modernisation/CONVENTIONS.md). Operating the estate is covered in
[`RUNBOOK.md`](RUNBOOK.md).

## Legacy estate (as found)

```mermaid
flowchart LR
    browser["Browser"]

    subgraph tomcat["Servlet container (Tomcat, Java 8) — PharmaWebClinetIntellij WAR"]
        direction TB
        jsp["JSP login page<br/>GET / → 302 /redirect"]
        login["LoginServlet<br/>POST/GET /login"]
        token["TokenServlet<br/>GET /token<br/>(mints admin JWT for hard-coded user)"]
        jwt["JJWT 0.2 RS256 signer<br/>private key committed in WEB-INF/StoredObject"]
        subgraph hib["Hibernate module (Hibernate 5.2, HQL by string concatenation)"]
            entities["Entities: User, Student<br/>UserValidation rules"]
        end
        login --> entities
        login --> jwt
        token --> jwt
    end

    mysql57[("MySQL 5.x<br/>user(username, password) — plaintext<br/>student")]

    spec["Spec only (not implemented):<br/>ERP-Pharma Excel Sheets.xlsx<br/>Pharma ERP USE-CASE.pdf<br/>staff, stores, clients, manufacturers,<br/>inventory, finance/GST, payments, feedback"]

    browser -->|"form post / cookie token"| jsp
    browser --> login
    browser --> token
    entities -->|"JDBC (mysql-connector 5.1)"| mysql57
    spec -.-> tomcat
```

- Build: Gradle 4.4 multi-project (`Hibernate`, `MainApp`, `PharmaWebClinetIntellij`), Java 8, no automated tests.
- Only authentication is implemented; every other business area exists solely as the spreadsheet / use-case PDF.
- Known defects (recorded in `parity/baseline`): plaintext passwords, `/token` issues an admin JWT to anyone,
  Hibernate exception text leaks to clients, RSA private key committed.

## Target estate (Docker Compose)

```mermaid
flowchart LR
    browser["Browser<br/>http://localhost:8080"]

    subgraph compose["docker compose (project: pharmaerp)"]
        direction LR
        subgraph fe["frontend — nginx 1.27"]
            spa["React 18 + TypeScript SPA<br/>(Vite build, react-router)<br/>feature modules: src/modules/*"]
            proxy["Reverse proxy<br/>/api/* · /login · /token · /actuator/*"]
        end

        subgraph be["backend — Spring Boot 3.3 on JRE 21"]
            direction TB
            sec["Spring Security<br/>stateless RS256 JWT<br/>(Bearer header or 'token' cookie)"]
            api["REST controllers /api/&lt;resource&gt;<br/>DTO records · Bean Validation<br/>problem+json errors"]
            svc["Services (@Transactional)"]
            repo["Spring Data JPA / Hibernate 6"]
            fly["Flyway migrations<br/>V1 baseline + per-module ranges"]
            act["Actuator /actuator/health"]
            sec --> api --> svc --> repo
        end

        keys[["jwt-keys (one-shot)<br/>generates RSA key pair<br/>into volume 'jwt-keys'"]]
        mysql8[("mysql:8.0<br/>volume 'mysql-data'<br/>PHARMA_ERP schema")]
    end

    browser --> spa
    browser --> proxy
    proxy -->|"http://backend:8090"| sec
    proxy --> act
    keys -->|"/run/keys (read-only)"| sec
    repo -->|"JDBC (mysql-connector-j)"| mysql8
    fly --> mysql8
```

| Concern | Legacy | Target |
|---------|--------|--------|
| Runtime | Java 8, servlet container, exploded WAR | Java 21, Spring Boot 3.3 executable jar on `eclipse-temurin:21-jre` |
| Build | Gradle 4.4 (Groovy), no tests | Gradle 8 (Kotlin DSL, wrapper) + JUnit 5 / Testcontainers; Vite + Vitest |
| UI | JSP login page | React 18 + TypeScript SPA served by nginx |
| API | `/login`, `/token` servlets returning JSON strings | REST `/api/**` with JSON DTO records, OpenAPI at `/v3/api-docs`; legacy-compat `/login`, `/token` kept by the auth module |
| Persistence | Hibernate 5.2 XML config, HQL string concatenation, MySQL 5.x | Spring Data JPA / Hibernate 6, `ddl-auto=validate`, Flyway-managed MySQL 8 |
| Security | Plaintext passwords; RS256 key committed; `/token` mints admin tokens | Spring Security, stateless RS256 JWT (same claim shape), keys provided at runtime (`JWT_*_KEY_PATH`) |
| Errors | Exception text leaked / forwards to JSP | RFC 9457 `application/problem+json` (400 / 401 / 403 / 404 / 409) |
| Operations | Manual WAR deploy | `docker compose up -d --build`, health checks on every service, GitHub Actions CI |

## Request flow (target)

```mermaid
sequenceDiagram
    autonumber
    participant B as Browser (SPA)
    participant N as nginx (frontend)
    participant S as Spring Boot (backend)
    participant D as MySQL 8

    B->>N: GET /signin
    N-->>B: index.html + hashed assets (SPA fallback)
    B->>N: POST /api/auth/... or legacy POST /login
    N->>S: proxy_pass http://backend:8090
    S->>D: look up user
    S-->>B: RS256 JWT (body and/or HttpOnly 'token' cookie)
    B->>N: GET /api/<resource> (Authorization: Bearer …)
    N->>S: proxy
    S->>S: JwtAuthenticationFilter → SCOPE_* authorities
    S->>D: JPA query
    S-->>B: JSON DTOs / PageResponse
```

## Repository layout

| Path | Purpose |
|------|---------|
| `backend/` | Spring Boot application (`com.pharma.erp.<module>`), `backend/Dockerfile` |
| `frontend/` | Vite React SPA (`src/modules/<module>`), `frontend/Dockerfile`, `frontend/nginx.conf` |
| `docker-compose.yml` | Target estate (mysql, jwt-keys, backend, frontend) |
| `parity/` | Legacy estate in Docker + recorded flows and baseline transcripts |
| `.github/workflows/ci.yml` | Backend build/tests, frontend lint/test/build, compose smoke test |
| `Hibernate/`, `MainApp/`, `PharmaWebClinetIntellij/`, root Gradle files | Legacy source, kept unchanged for parity |
