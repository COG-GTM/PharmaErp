#!/usr/bin/env bash
# Supplementary probes behind the documented intentional differences (known-differences.json). Read-only: the
# token comes from a login of an already-seeded user. Run from the repo root with both estates up.
#   parity/probes.sh > parity/results/probes.txt
set -uo pipefail
LEGACY="${LEGACY:-http://127.0.0.1:8081}"
BACKEND="${BACKEND:-http://127.0.0.1:8090}"
FRONT="${FRONT:-http://127.0.0.1:8080}"
probe() { printf '%-62s -> %s\n' "$1" "$(curl -s -o /dev/null -w "$3" "${@:4}" "$2")"; }
TOKEN=$(curl -s -X POST "$BACKEND/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"tarang","password":"parisdkh"}' | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')
echo "# legacy-compat routes"
probe "F02 front door GET /redirect (follow redirects)" "$FRONT/redirect" '%{http_code} %{url_effective}' -L
probe "F01 front door GET / (SPA)" "$FRONT/" '%{http_code} %{content_type}'
probe "F16 backend GET /nonexistent with Bearer token" "$BACKEND/nonexistent" '%{http_code}' -H "Authorization: Bearer $TOKEN"
probe "F16 front door GET /nonexistent (SPA fallback)" "$FRONT/nonexistent" '%{http_code} %{content_type}'
probe "F15 backend GET /token with token cookie" "$BACKEND/token" '%{http_code} %{content_type}' -b "token=$TOKEN"
probe "F15 backend GET /token with Bearer token" "$BACKEND/token" '%{http_code} %{content_type}' -H "Authorization: Bearer $TOKEN"
probe "F15 backend GET /token with forged/garbage token" "$BACKEND/token" '%{http_code}' -b "token=not.a.jwt"
probe "F15 legacy GET /token, no credentials" "$LEGACY/token" '%{http_code} %{content_type}'
echo "# cross-estate tokens"
LTOKEN=$(curl -s "$LEGACY/token" | python3 -c 'import json,sys; print(json.load(sys.stdin))')
probe "legacy-minted 'tarangparikh' JWT presented to migrated /api/auth/me" "$BACKEND/api/auth/me" '%{http_code}' -H "Authorization: Bearer $LTOKEN"
probe "migrated JWT presented to migrated /api/auth/me" "$BACKEND/api/auth/me" '%{http_code}' -H "Authorization: Bearer $TOKEN"
