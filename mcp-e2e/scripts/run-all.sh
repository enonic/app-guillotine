#!/usr/bin/env bash
# Every check on the running local XP, as an anonymous and as a signed-in caller, plus config changes and an app redeploy without
# restart. Exit 1 on any FAIL. Config and deploy-folder changes run under a lock; Guillotine's config file and guillotine.jar are put
# back before it is released, also on interrupt. Output is also written to $LOG_DIR/run-all-<timestamp>.log.
set -uo pipefail
source "$(dirname "$0")/xp-env.sh"
CHECKS="$MCP_E2E_DIR/checks"
DEPLOY="$XP_HOME/deploy"
LOCK="$MCP_E2E_WORK/xp.lock"
BACKUP="$MCP_E2E_WORK/backup"
mkdir -p "$BACKUP" "$LOG_DIR"
LOG="$LOG_DIR/run-all-$(date +%Y%m%d-%H%M%S).log"
exec > >(tee "$LOG") 2>&1
echo "      log: $LOG"
[ -f "$DEPLOY/guillotine.jar" ] || { echo "No guillotine.jar in $DEPLOY; run scripts/deploy.sh" >&2; exit 2; }

declare -a SUMMARY=()
fail=0
check() { if eval "$2"; then echo "PASS  $1"; else echo "FAIL  $1"; fail=1; fi; }
suite() {
    # suite <name> <command...>: runs a check script and records one SUMMARY line from its PASS/FAIL lines and exit code.
    local name="$1"; shift
    echo "=== suite: $name"
    local out code p f
    out=$("$@" 2>&1); code=$?
    echo "$out"
    p=$(grep -c '^PASS' <<<"$out"); f=$(grep -c '^FAIL' <<<"$out")
    if [ "$code" -eq 0 ] && [ "$f" -eq 0 ] && [ "$p" -gt 0 ]; then SUMMARY+=("PASS  $name ($p pass)")
    else SUMMARY+=("FAIL  $name ($p pass, $f fail, exit $code)"); fail=1; fi
}
step() {
    # step <name> <function>: a block of check lines in this script, recorded as one SUMMARY line.
    local name="$1" before=$fail; fail=0
    echo "=== $name"
    "$2"
    [ $fail -eq 0 ] && SUMMARY+=("PASS  $name") || SUMMARY+=("FAIL  $name")
    fail=$((fail | before))
}

tool_count() {
    mcp_call tools/list '{}' | python3 -c '
import json, sys
print(sum(1 for t in json.load(sys.stdin)["result"]["tools"] if t["name"].startswith(sys.argv[1])))' "$GUILLOTINE_PREFIX" 2>/dev/null
}
anonymous_open() { MCP_COOKIE= projects_ok; }
anonymous_refused() {
    MCP_COOKIE= mcp_call tools/call "{\"name\":\"${GUILLOTINE_PREFIX}content__list-projects\",\"arguments\":{}}" |
        grep -q 'mcp.allowAnonymous=true'
}
search_truncated() {
    MCP_COOKIE= mcp_call tools/call "{\"name\":\"${GUILLOTINE_PREFIX}content__search\",\"arguments\":{\"count\":50}}" |
        grep -q '"truncated":true'
}
graphql_up() {
    curl -s "$XP_URL/api/com.enonic.app.guillotine:graphql" -H 'Content-Type: application/json' --data '{"query":"{__typename}"}' |
        grep -q '"__typename":"Query"'
}
guillotine_ready() { [ "$(tool_count)" = 8 ] && anonymous_open && MCP_COOKIE="$SIGNED" projects_ok; }

restore() {
    rm -f "$DEPLOY"/*.jar.part "$GUILLOTINE_CFG.part"
    [ -f "$DEPLOY/guillotine.jar" ] || deploy_jar "$BACKUP/guillotine.jar" guillotine.jar
    if [ -f "$BACKUP/guillotine.cfg" ]; then cp "$BACKUP/guillotine.cfg" "$GUILLOTINE_CFG"; else rm -f "$GUILLOTINE_CFG"; fi
    [ -n "${SIGNED:-}" ] && "$MCP_E2E_DIR/scripts/login.sh" logout "$SIGNED"
}

exec 9>"$LOCK"
flock 9
cp "$DEPLOY/guillotine.jar" "$BACKUP/guillotine.jar"
rm -f "$BACKUP/guillotine.cfg"
[ -f "$GUILLOTINE_CFG" ] && cp "$GUILLOTINE_CFG" "$BACKUP/guillotine.cfg"
trap restore EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
START=$(console_lines)

write_cfg "$BASE_CFG"
wait_for 30 anonymous_open || { echo "Guillotine does not answer anonymous calls with $BASE_CFG" >&2; exit 1; }
SIGNED=$("$MCP_E2E_DIR/scripts/login.sh") || { echo "Sign-in failed; is the standard ID provider deployed (scripts/deploy.sh)?" >&2; exit 1; }
SIGNED_OUT=$("$MCP_E2E_DIR/scripts/login.sh") && "$MCP_E2E_DIR/scripts/login.sh" logout "$SIGNED_OUT"
echo "      signed-in caller: $MCP_E2E_USER"

for caller in anonymous signed-in; do
    cookie=""; [ "$caller" = signed-in ] && cookie="$SIGNED"
    suite "catalog [$caller]" env MCP_COOKIE="$cookie" python3 "$CHECKS/catalog.py"
    suite "tools [$caller]" env MCP_COOKIE="$cookie" python3 "$CHECKS/tools.py"
    suite "answers [$caller]" env MCP_COOKIE="$cookie" python3 "$CHECKS/answers.py"
done
suite "access open" env MCP_CALLER=both ACCESS_COOKIE="$SIGNED" LOGOUT_COOKIE="$SIGNED_OUT" python3 "$CHECKS/access.py" open

write_cfg "mcp.defaultProject=mcp-test"
if wait_for 30 anonymous_refused; then
    suite "access closed" env MCP_CALLER=both ACCESS_COOKIE="$SIGNED" python3 "$CHECKS/access.py" closed
else
    SUMMARY+=("FAIL  access closed (config without mcp.allowAnonymous not applied within 30s)"); fail=1
fi

write_cfg "$BASE_CFG"$'\nmcp.maxResultChars=2000'
if wait_for 30 search_truncated; then
    for caller in anonymous signed-in; do
        cookie=""; [ "$caller" = signed-in ] && cookie="$SIGNED"
        suite "budget [$caller]" env MCP_COOKIE="$cookie" MCP_MAX_RESULT_CHARS=2000 python3 "$CHECKS/budget.py"
    done
else
    SUMMARY+=("FAIL  budget (mcp.maxResultChars=2000 not applied within 30s)"); fail=1
fi
write_cfg "$BASE_CFG"
wait_for 30 '! search_truncated && anonymous_open' || { echo "FAIL  config $BASE_CFG not applied again within 30s"; fail=1; }

redeploy() {
    local start
    check "precondition: 8 Guillotine tools listed" "[ \"\$(tool_count)\" = 8 ]"
    rm -f "$DEPLOY/guillotine.jar"
    check "undeploy guillotine.jar: all 8 tools gone within 30s" "wait_for 30 '[ \"\$(tool_count)\" = 0 ]'"
    check "  Guillotine's HTTP GraphQL API is gone with it" "wait_for 30 '! graphql_up'"
    start=$(console_lines)
    deploy_jar "$BACKUP/guillotine.jar" guillotine.jar
    check "redeploy without XP restart: settled with 8 tools, anonymous and signed-in calls working within 90s" \
        "wait_app_settled com.enonic.app.guillotine $start 90 guillotine_ready"
    check "  Guillotine's HTTP GraphQL API answers again" "graphql_up"
}
step "redeploy" redeploy

suite "dataset-check" python3 "$CHECKS/dataset-check.py"

server_log() {
    local errors
    errors=$(tail -n +"$((START + 1))" "$XP_HOME/logs/console.log" | grep -E 'ERROR' | grep -iE 'guillotine|mcp' || true)
    check "no Guillotine or MCP ERROR lines in console.log during the run" "[ -z \"\$errors\" ]"
    [ -n "$errors" ] && head -20 <<<"$errors"
}
step "server log" server_log

restore
trap - EXIT INT TERM
flock -u 9
exec 9>&-

echo "=== SUMMARY"
printf '%s\n' "${SUMMARY[@]}"
[ $fail -eq 0 ] && echo "RESULT: PASS" || echo "RESULT: FAIL"
exit $fail
