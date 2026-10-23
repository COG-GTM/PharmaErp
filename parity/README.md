# Parity harness

Legacy baseline captured from the **unchanged** legacy source (servlet WAR on Tomcat 9 / Java 8, MySQL 5.7).

```bash
docker compose -f parity/legacy/docker-compose.yml up -d --build      # legacy on http://127.0.0.1:8081
python3 parity/capture.py --base http://127.0.0.1:8081 --label legacy --out parity/baseline/legacy-transcripts.json
```

- `flows.json` — every user/API flow the legacy code implements (16 flows: login page, login success/failure variants, trimming, case sensitivity, injection probes, `/token`, 404).
- `seed/` — identical seed data (`user` table: 4 users) loaded into legacy and migrated databases.
- `baseline/legacy-transcripts.json` — normalised legacy responses (status, content type, body kind, JWT alg/claims/TTL, cookies).
- Legacy automated test suite: **0 tests** (no `src/test` sources in any module; `./gradlew test` has nothing to run).

Making legacy run required no source changes; only build plumbing in `parity/legacy/` (JDK 8 build image, Google Maven Central mirror because repo.maven.apache.org returns HTTP 429, DB host rewrite, seed SQL).

Pre-existing legacy defects observed in the baseline: F10 leaks `org.hibernate.QueryException` text to the client (HQL built by string concatenation); F15 issues a signed admin JWT for hard-coded user `tarangparikh` to any unauthenticated caller; F07 (missing params) silently falls through; passwords stored and compared in plaintext; RSA private key committed in `WEB-INF/StoredObject/system.key`.
