#!/usr/bin/env bash
# Signs in to the local XP and prints a Cookie header value (JSESSIONID=...) for signed-in MCP calls.
# usage: login.sh [user [password]]      default: MCP_E2E_USER / MCP_E2E_PASSWORD
#        login.sh logout <cookie>        ends that session
set -euo pipefail
source "$(dirname "$0")/xp-env.sh"
if [ "${1:-}" = logout ]; then
    curl -sS -o /dev/null -H "Cookie: ${2:?usage: login.sh logout <cookie>}" "$XP_URL/_/idprovider/system/logout"
    exit 0
fi
user="${1:-$MCP_E2E_USER}" password="${2:-$MCP_E2E_PASSWORD}"
body=$(python3 -c 'import json,sys; print(json.dumps({"action": "login", "user": sys.argv[1], "password": sys.argv[2]}))' "$user" "$password")
headers=$(mktemp)
trap 'rm -f "$headers"' EXIT
response=$(curl -sS -D "$headers" -X POST "$XP_URL/_/idprovider/system" -H 'Content-Type: application/json' --data "$body")
grep -q '"authenticated":true' <<<"$response" || { echo "Sign-in as $user failed: $response" >&2; exit 1; }
cookie=$(tr -d '\r' < "$headers" | sed -n 's/^[Ss]et-[Cc]ookie: \(JSESSIONID=[^;]*\).*/\1/p' | tail -n 1)
[ -n "$cookie" ] || { echo "Sign-in as $user returned no JSESSIONID cookie" >&2; exit 1; }
echo "$cookie"
