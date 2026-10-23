# Pharma ERP — runbook

Operating the modernised estate locally or on a single host. Architecture: [`ARCHITECTURE.md`](ARCHITECTURE.md).
Coding conventions: [`modernisation/CONVENTIONS.md`](modernisation/CONVENTIONS.md).

## Prerequisites

- Docker Engine 24+ with the Compose v2 plugin (`docker compose version`). Docker Engine ≥ 29 needs no special handling for
  the estate; backend *tests* rely on Testcontainers 1.21.4 (pinned for that reason).
- For development outside Docker: JDK 21 (`export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`) and Node 20+.

## Start / stop the estate

```bash
docker compose up -d --build          # build images and start everything
docker compose up -d --build --wait   # same, but block until every service is healthy
docker compose ps                     # STATUS should read "healthy" for mysql, backend, frontend
docker compose logs -f backend        # follow one service
docker compose down                   # stop, keep data (volumes mysql-data, jwt-keys)
docker compose down -v                # stop and wipe the database and the JWT key pair
```

| Service | Image | Host port | Health check |
|---------|-------|-----------|--------------|
| `frontend` | nginx 1.27 + Vite build (`frontend/Dockerfile`) | `8080` → http://localhost:8080 | `GET /healthz` |
| `backend` | Spring Boot jar on JRE 21 (`backend/Dockerfile`) | `127.0.0.1:8090` | `GET /actuator/health` |
| `mysql` | `mysql:8.0`, volume `mysql-data` | `127.0.0.1:3306` | `mysqladmin ping` as the app user over TCP |
| `jwt-keys` | backend image, one-shot | — | exits 0 once `/keys/jwt-{private,public}.pem` exist |

Start order is enforced with `depends_on` conditions: `mysql` healthy and `jwt-keys` completed → `backend` healthy →
`frontend`. Flyway migrates the schema when the backend starts.

nginx routing (`frontend/nginx.conf`): `/api/*`, `/login`, `/token`, `/actuator/*` are proxied to `http://backend:8090`;
`/assets/*` are served with immutable caching; every other path falls back to `index.html` (client-side routes such as
`/signin`).

### Smoke test

```bash
curl -fsS http://localhost:8080/actuator/health           # {"status":"UP"} via nginx
curl -fsS http://localhost:8090/actuator/health           # direct to the backend
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/clients   # 401 without a token
curl -fsS http://localhost:8080/signin | grep -c 'id="root"'                 # SPA fallback
```

## Configuration

Compose reads `PHARMA_*` variables from the shell or from a git-ignored `.env` file next to `docker-compose.yml`.
Generic names such as `DB_PASSWORD` are deliberately **not** read from the host, because some machines export them
globally; inside the backend container they are set explicitly from the `PHARMA_*` values.

| Variable | Default | Effect |
|----------|---------|--------|
| `PHARMA_HTTP_PORT` | `8080` | Host port of the SPA / reverse proxy |
| `PHARMA_BACKEND_HOST_PORT` | `8090` | Host port (loopback only) of the backend |
| `PHARMA_DB_HOST_PORT` | `3306` | Host port (loopback only) of MySQL — change it if a local MySQL already uses 3306 |
| `PHARMA_DB_NAME` | `PHARMA_ERP` | Database / schema name |
| `PHARMA_DB_USER` / `PHARMA_DB_PASSWORD` | `pharma` / `pharma` | Application DB account (backend `DB_USER` / `DB_PASSWORD`) |
| `PHARMA_DB_ROOT_PASSWORD` | `admin` | MySQL root password |
| `PHARMA_JWT_TTL` | `PT1H` | Token lifetime (ISO-8601 duration), backend `JWT_TTL` |
| `PHARMA_CORS_ALLOWED_ORIGINS` | `http://localhost:8080,http://127.0.0.1:8080` | Backend `CORS_ALLOWED_ORIGINS` |
| `PHARMA_MAVEN_MIRROR` | *(empty)* | Maven Central mirror used while building the backend image (see below) |
| `PHARMA_IMAGE_TAG` | `local` | Tag for `pharmaerp-backend` / `pharmaerp-frontend` images |

The defaults are development credentials. For any shared host, put real values in `.env` (never commit it) and
consider removing the published MySQL port.

### JWT signing keys

`jwt-keys` generates a 2048-bit RSA pair (PKCS#8 private / X.509 public PEM) into the `jwt-keys` volume on first start;
the backend mounts it read-only at `/run/keys` and reads `JWT_PRIVATE_KEY_PATH` / `JWT_PUBLIC_KEY_PATH`. Tokens therefore
stay valid across backend restarts and rebuilds.

- **Rotate keys** (invalidates every issued token):
  `docker compose down && docker volume rm pharmaerp_jwt-keys && docker compose up -d`
- **Bring your own keys**: copy them into the volume before starting, e.g.
  `docker compose run --rm -v "$PWD/keys:/src:ro" --entrypoint sh jwt-keys -c 'cp /src/*.pem /keys/'`
  (files must be named `jwt-private.pem` and `jwt-public.pem`).

### Maven Central rate limiting (HTTP 429)

Some CI runners and VMs get HTTP 429 from `repo.maven.apache.org`. Build the backend image through Google's mirror:

```bash
PHARMA_MAVEN_MIRROR=https://maven-central.storage-download.googleapis.com/maven2 docker compose up -d --build
```

For `./gradlew` outside Docker, add the same mirror via a Gradle init script in `~/.gradle/init.d/` (not committed).

## Database operations

```bash
docker compose exec mysql mysql -upharma -ppharma PHARMA_ERP                      # SQL shell
docker compose exec mysql mysql -upharma -ppharma PHARMA_ERP \
  -e 'SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank'
docker compose exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" PHARMA_ERP' > backup.sql   # backup
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" PHARMA_ERP' < backup.sql    # restore
```

Load the parity seed users (same rows as the legacy estate):

```bash
docker compose exec -T mysql mysql -upharma -ppharma PHARMA_ERP < parity/seed/seed-users.sql
```

Reset to an empty, freshly migrated database: `docker compose down -v && docker compose up -d`.

## Troubleshooting

| Symptom | Likely cause / fix |
|---------|--------------------|
| `backend` stays `starting`, logs show `Communications link failure` | MySQL not ready or wrong credentials. Check `docker compose logs mysql`; if `PHARMA_DB_*` changed after the volume was created, the old account is still in the volume — `docker compose down -v`. |
| Backend exits with `FlywayValidateException` / `Schema-validation: missing column` | A migration was edited after it ran, or an entity no longer matches the schema. Never edit merged migrations; for local data `docker compose down -v`. |
| `Bind for 0.0.0.0:8080 failed: port is already allocated` | Another process uses the port; set `PHARMA_HTTP_PORT` (or `PHARMA_DB_HOST_PORT` / `PHARMA_BACKEND_HOST_PORT`). The legacy parity estate uses 8081 / 3307 and can run alongside. |
| `502 Bad Gateway` from http://localhost:8080/api/... | Backend down or restarting: `docker compose ps backend`, `docker compose logs backend`. nginx re-resolves the backend every 10 s. |
| All requests return 401 after `down -v` | Key pair was regenerated; sign in again. |
| Image build fails resolving dependencies with HTTP 429 | Set `PHARMA_MAVEN_MIRROR` (see above). |
| Backend tests fail with `client version 1.32 is too old` | Testcontainers older than 1.21 against Docker ≥ 29; keep the 1.21.4 pin. |
| Backend run outside Docker gets `Access denied for user 'root'` | The host exports `DB_PASSWORD`/`DB_USER`; pass them explicitly, e.g. `DB_USER=root DB_PASSWORD=admin ./gradlew bootRun`. |

## Legacy estate (parity harness)

```bash
docker compose -f parity/legacy/docker-compose.yml up -d --build    # http://127.0.0.1:8081
python3 parity/capture.py --base http://127.0.0.1:8081 --label legacy --out /tmp/legacy.json
docker compose -f parity/legacy/docker-compose.yml down -v
```

It uses separate project name, ports and volumes (`pharmaerp-legacy`), so both estates can run at the same time.

## CI

`.github/workflows/ci.yml` runs on pushes to `devin/**`, on pull requests targeting `devin/hcl-pharmaerp-modernisation`,
and on manual dispatch:

1. **backend** — JDK 21, `cd backend && ./gradlew build` (unit + Testcontainers MySQL integration tests); test reports
   are uploaded on failure.
2. **frontend** — Node 20, `npm ci`, `npm run lint`, `npm run test -- --run`, `npm run build`.
3. **compose** — after both pass: `docker compose up -d --build --wait`, smoke tests through http://localhost:8080
   (SPA, `/signin` fallback, `/actuator/health`, `/api` returns 401), logs, `docker compose down -v`.

Reproduce CI locally with the commands in [`modernisation/CONVENTIONS.md`](modernisation/CONVENTIONS.md#commands).
