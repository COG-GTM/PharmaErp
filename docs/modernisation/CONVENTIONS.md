# Pharma ERP modernisation — conventions

Target: Java 21 + Spring Boot 3.3.x (Web, Security, Data JPA / Hibernate 6, Bean Validation, Flyway, MySQL 8),
React 18 + TypeScript (Vite) client, Docker Compose estate. Integration branch: `devin/hcl-pharmaerp-modernisation`
(never push or merge into `master`). Every ticket works on its own branch and PRs into the integration branch.

These rules exist so parallel tickets never edit the same file. If you think a shared file must change, raise it in
your PR description instead of editing it.

## Repository layout

| Path | Owner |
|------|-------|
| `backend/` | Spring Boot app (Gradle Kotlin DSL, wrapper committed), base package `com.pharma.erp` |
| `backend/src/main/java/com/pharma/erp/common/**` | Scaffold (T00) — shared, do not edit from a module ticket |
| `backend/src/main/java/com/pharma/erp/<module>/**` | The module's ticket only |
| `backend/src/test/java/com/pharma/erp/<module>/**` | The module's ticket only |
| `backend/src/main/resources/db/migration/V1__baseline_schema.sql` | Scaffold — never edited |
| `backend/src/main/resources/db/migration/V<NNN>__<module>_<desc>.sql` | The ticket owning range NNN |
| `frontend/src/modules/<module>/**` | The module's ticket only |
| `frontend/src/{api,app,auth,styles,test}/**`, `frontend/src/modules/{types,registry}.ts` | Scaffold — shared |
| `parity/**` | Parity harness — do not modify unless your ticket says so |
| Legacy (`Hibernate/`, `MainApp/`, `PharmaWebClinetIntellij/`, root Gradle files) | Untouched (run by `parity/legacy`) |

## Backend

- One package per module: `com.pharma.erp.<module>` containing `entity`, `repository`, `service`, `dto`, `controller`
  (sub-packages or flat classes — your choice, but nothing outside the module folder).
- REST under `/api/<resource>` (plural nouns, e.g. `/api/clients`, `/api/inventory-items`). Return JSON DTOs as Java
  `record`s — **never expose JPA entities**. Lists that can grow return `common.web.PageResponse<T>`
  (`PageResponse.from(page, mapper)`), accepting `page`/`size`/`sort` via Spring `Pageable`.
- Validation: Bean Validation annotations on request records + `@Valid`. Errors are rendered by
  `common.web.ApiExceptionHandler` as **400 `application/problem+json`** with an `errors` map (`field -> [messages]`).
- Not found: throw `common.web.NotFoundException` (`NotFoundException.of("Client", id)`) -> **404** problem+json.
  Conflicts (duplicates, illegal state transitions): `common.web.ConflictException` -> **409**; DB integrity
  violations are also mapped to 409.
- Money: `BigDecimal` / `DECIMAL(12,2)`; rates in percent as `DECIMAL(5,2)`. Dates: `LocalDate`; timestamps: `Instant`.
- JPA: `spring.jpa.hibernate.ddl-auto=validate` — entities must match the Flyway schema exactly. `open-in-view` is off,
  so load what you need inside `@Transactional` service methods.
- Map entities onto the tables in `V1__baseline_schema.sql`; the legacy table is named `user` — map it with
  `@Table(name = "`user`")`.

## Security

- `common.security.JwtService` issues/verifies **RS256** JWTs in the legacy shape (`sub = "user/login"`, claims
  `username`, `scope`; TTL `pharma.security.jwt.ttl`, default 1h). PEM keys are read from `JWT_PRIVATE_KEY_PATH`
  (PKCS#8) and `JWT_PUBLIC_KEY_PATH` (X.509); when either is missing an ephemeral key pair is generated at startup.
- `common.security.JwtAuthenticationFilter` reads `Authorization: Bearer <jwt>` or the `token` cookie. The principal
  name is the `username` claim; each space-separated `scope` value becomes authority `SCOPE_<value>`
  (use `@PreAuthorize("hasAuthority('SCOPE_admin')")` — method security is enabled).
- `/api/**` requires authentication **except** `/api/auth/**`. Public: `/`, `/login`, `/token`, `/redirect` (legacy-compat
  routes, owned by the auth ticket), `/actuator/health`, `/actuator/info`, `/v3/api-docs`, `/swagger-ui`.
  401/403 are problem+json. Sessions are stateless; CSRF is disabled, so any cookie the auth ticket sets must be
  `HttpOnly` + `SameSite=Strict` (or `Lax`).
- CORS allows the Vite dev server (`CORS_ALLOWED_ORIGINS`, default `http://localhost:5173,http://127.0.0.1:5173`).

## Schema (Flyway)

- `V1__baseline_schema.sql` (scaffold) creates **all** tables (InnoDB, utf8mb4): legacy `user` / `student`
  (column-compatible with `parity/seed/legacy-schema.sql`, so `parity/seed/*.sql` loads into both estates) plus every
  entity of the spec's "Database Schema" sheet: `login_type`, `staff`, `staff_security`, `salary`, `store`, `client`,
  `manufacturer`, `item_status`, `inventory_item`, `stock_tracking`, `prescription`, `prescription_line`, `invoice`,
  `invoice_line`, `expense`, `payment_method`, `payment`, `feedback`.
- A ticket that needs schema changes (new columns, indexes, tables) or reference/demo seed data (e.g. `item_status`
  rows, `login_type` rows, `payment_method` rows) adds files **only** in its reserved range, named
  `V<NNN>__<module>_<desc>.sql` (e.g. `V140__inventory_status_seed.sql`). Never edit a migration that has merged.
- `spring.flyway.out-of-order=true` (env `FLYWAY_OUT_OF_ORDER`) so ranges can merge in any order on existing dev DBs.

| Ticket | Reserved versions |
|--------|-------------------|
| T01 | V100 – V109 |
| T02 | V110 – V119 |
| T03 | V120 – V129 |
| T04 | V130 – V139 |
| T05 | V140 – V149 |
| T06 | V150 – V159 |
| T07 | V160 – V169 |
| T08 | V170 – V179 |
| T09 | V180 – V189 |
| T10 | V190 – V199 |
| T11 | V200 – V209 |
| T12 | V210 – V219 |

(Pattern: ticket `Tnn` owns `V(90 + 10·nn)` – `V(99 + 10·nn)`.)

## Backend tests

- JUnit 5 + Spring Boot Test. Integration tests extend `com.pharma.erp.common.test.AbstractIntegrationTest`
  (test sources): full context, `MockMvc`, Testcontainers **MySQL 8** shared per JVM via `@ServiceConnection`,
  Flyway applied. Helpers: `bearer("username")` (MockMvc post-processor) and `tokenFor("username")`.
- The database is shared across test classes: create your own rows, never assume empty tables or fixed IDs.
- Pure unit tests (services, mappers) need no Spring context.
- Requires Docker. Testcontainers is pinned to 1.21.4 because Docker Engine ≥ 29 rejects the older client API.

## Frontend

- `frontend/`: Vite + React 18 + TypeScript (`strict`, `noUncheckedIndexedAccess`) + react-router-dom 6 + Vitest +
  Testing Library + ESLint (flat config).
- API calls go through `src/api/client.ts` (`api.get/post/put/patch/delete`, `apiFetch`): adds the bearer token from
  localStorage, JSON bodies, throws `ApiError` (`status`, `problem.errors`) on non-2xx, broadcasts 401s. Use
  `PageResponse<T>` from the same file for paginated endpoints.
- Each feature module lives **only** in `src/modules/<module>/` and default-exports a `ModuleDefinition`
  (`src/modules/types.ts`) from `src/modules/<module>/index.ts`:
  `{ id, path, navLabel, element, order?, requiresAuth?, hideFromNav? }`. `src/modules/registry.ts` discovers modules
  with `import.meta.glob('./*/index.ts', { eager: true })`, so **no shared file is edited to add a module**. The route
  is `/<path>/*` — use nested `<Routes>` inside your module for sub-pages. Routes require a token unless
  `requiresAuth: false`.
- Auth: `src/auth` provides `AuthProvider`, `useAuth()` (`token`, `isAuthenticated`, `signIn(token)`, `signOut()`),
  `RequireAuth` and token storage. The sign-in page belongs to the auth module and must use the route `/signin`
  (`SIGN_IN_PATH`) — **not** `/login`, which the Vite dev server proxies to the backend's legacy endpoint.
- Tests: colocate `*.test.ts(x)`; use `renderWithProviders` and `mockFetchJson` from `src/test/utils.tsx`.
- Dev server proxies `/api`, `/login`, `/token`, `/actuator` to `http://localhost:8090` (override with `VITE_BACKEND_URL`).

## Ports

| Port | Service |
|------|---------|
| 8090 | New backend (`PORT` overrides) |
| 5173 | Vite dev server |
| 3306 | MySQL 8 for the new backend (`DB_URL`, `DB_USER`, `DB_PASSWORD`; default `localhost:3306/PHARMA_ERP`, root/admin) |
| 8081 / 3307 | Legacy parity app / its MySQL 5.7 (`parity/legacy`) — reserved |

## Commands

```bash
# Backend (Java 21)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
cd backend && ./gradlew build                     # compile + all tests (needs Docker)

# Run locally
docker run -d --name pharma-mysql -e MYSQL_ROOT_PASSWORD=admin -e MYSQL_DATABASE=PHARMA_ERP -p 3306:3306 mysql:8.0
cd backend && ./gradlew bootRun                   # http://localhost:8090/actuator/health
# NB: some VMs export DB_PASSWORD/DB_USERNAME globally; override explicitly, e.g.
#   DB_USER=root DB_PASSWORD=admin ./gradlew bootRun

# Frontend (Node 20+)
cd frontend && npm ci && npm run lint && npm run test -- --run && npm run build
npm run dev                                       # http://localhost:5173
```

If `repo.maven.apache.org` returns HTTP 429, resolve through the mirror
`https://maven-central.storage-download.googleapis.com/maven2` with a Gradle init script in `~/.gradle/init.d/`
(do not commit it or any credentials).
