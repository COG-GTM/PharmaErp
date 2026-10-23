#!/usr/bin/env bash
# Supplementary probes behind the documented intentional differences (known-differences.json). Read-only: the
# token comes from a login of an already-seeded user. Run from the repo root with both estates up.
#   parity/probes.sh > parity/results/probes.txt      (exits 1 if any probe misses its expected result)
set -uo pipefail
LEGACY="${LEGACY:-http://127.0.0.1:8081}"
BACKEND="${BACKEND:-http://127.0.0.1:8090}"
FRONT="${FRONT:-http://127.0.0.1:8080}"
FAILED=0
# probe <label> <url> <curl -w format> <expected glob> [curl args...]
probe() {
  local got verdict=OK
  got=$(curl -s -o /dev/null -w "$3" "${@:5}" "$2")
  # shellcheck disable=SC2053
  [[ $got == $4 ]] || { verdict=FAIL; FAILED=1; }
  printf '%-4s %-62s -> %s\n' "$verdict" "$1" "$got"
}
token_from() { python3 -c 'import json,sys; d=json.load(sys.stdin); print(d["token"] if isinstance(d, dict) else d)' 2>/dev/null; }
TOKEN=$(curl -s -X POST "$BACKEND/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"tarang","password":"parisdkh"}' | token_from)
[[ -n $TOKEN ]] || { echo "FAIL could not sign in to the migrated backend as tarang (seed loaded?)"; exit 1; }
echo "# legacy-compat routes"
probe "F02 front door GET /redirect (follow redirects)" "$FRONT/redirect" '%{http_code} %{url_effective}' "200 */signin" -L
probe "F01 front door GET / (SPA)" "$FRONT/" '%{http_code} %{content_type}' "200 text/html*"
probe "F16 backend GET /nonexistent with Bearer token" "$BACKEND/nonexistent" '%{http_code}' "404" -H "Authorization: Bearer $TOKEN"
probe "F16 front door GET /nonexistent (SPA fallback)" "$FRONT/nonexistent" '%{http_code} %{content_type}' "200 text/html*"
probe "F15 backend GET /token with token cookie" "$BACKEND/token" '%{http_code} %{content_type}' "200 application/json*" -b "token=$TOKEN"
probe "F15 backend GET /token with Bearer token" "$BACKEND/token" '%{http_code} %{content_type}' "200 application/json*" -H "Authorization: Bearer $TOKEN"
probe "F15 backend GET /token with forged/garbage token" "$BACKEND/token" '%{http_code}' "401" -b "token=not.a.jwt"
probe "F15 legacy GET /token, no credentials (legacy defect)" "$LEGACY/token" '%{http_code} %{content_type}' "200 application/json*"
echo "# cross-estate tokens"
LTOKEN=$(curl -s "$LEGACY/token" | token_from)
[[ -n $LTOKEN ]] || { echo "FAIL legacy /token returned no token"; FAILED=1; }
probe "legacy-minted 'tarangparikh' JWT presented to migrated /api/auth/me" "$BACKEND/api/auth/me" '%{http_code}' "401" -H "Authorization: Bearer $LTOKEN"
probe "migrated JWT presented to migrated /api/auth/me" "$BACKEND/api/auth/me" '%{http_code}' "200" -H "Authorization: Bearer $TOKEN"
echo "# result: $([[ $FAILED == 0 ]] && echo 'all probes met their expected result' || echo 'PROBE FAILURES')"
exit $FAILED
