#!/usr/bin/env bash
# Loads the identical parity seed (parity/seed/seed-users.sql) into the migrated estate's `user` table.
# Run from the repo root after `docker compose up -d --build --wait` (Flyway has created the schema by then).
# The legacy estate loads the same file through docker-entrypoint-initdb.d (parity/legacy/docker-compose.yml).
set -euo pipefail
cd "$(dirname "$0")/../.."
DB_USER="${PHARMA_DB_USER:-pharma}"
DB_NAME="${PHARMA_DB_NAME:-PHARMA_ERP}"
export MYSQL_PWD="${PHARMA_DB_PASSWORD:-pharma}"
existing=$(docker compose exec -T -e MYSQL_PWD mysql mysql -N -u"$DB_USER" "$DB_NAME" -e 'SELECT COUNT(*) FROM `user`')
if [ "$existing" != "0" ]; then
  echo "migrated user table already has $existing row(s); reset with 'docker compose down -v' for a clean parity run" >&2
  exit 1
fi
docker compose exec -T -e MYSQL_PWD mysql mysql -u"$DB_USER" "$DB_NAME" < parity/seed/seed-users.sql
docker compose exec -T -e MYSQL_PWD mysql mysql -u"$DB_USER" "$DB_NAME" -e 'SELECT username FROM `user` ORDER BY username'
