#!/usr/bin/env bash
# Every E2E check on the running local XP, plus app lifecycle without restart. Exit 1 on any FAIL.
# Deploy-folder changes run under a lock; guillotine.jar and echo-app.jar are restored before it is released, also on interrupt.
# Output is also written to $LOG_DIR/run-all-<timestamp>.log.
set -uo pipefail
source "$(dirname "$0")/xp-env.sh"
require_xp_expect
CHECKS="$MCP_E2E_DIR/checks"
DEPLOY="$XP_HOME/deploy"
LOCK="$MCP_E2E_WORK/xp.lock"
BACKUP="$MCP_E2E_WORK/backup"
mkdir -p "$BACKUP" "$LOG_DIR"
LOG="$LOG_DIR/run-all-$(date +%Y%m%d-%H%M%S).log"
exec > >(tee "$LOG") 2>&1
echo "      log: $LOG"
for jar in "$ECHO_JAR" "$BROKEN_JAR"; do
    [ -f "$jar" ] || { echo "Missing $jar; run scripts/build-fixtures.sh" >&2; exit 2; }
done

restore() {
    rm -f "$DEPLOY/broken-app.jar"
    [ -f "$DEPLOY/guillotine.jar" ] || cp "$BACKUP/guillotine.jar" "$DEPLOY/guillotine.jar"
    [ -f "$DEPLOY/echo-app.jar" ] || cp "$ECHO_JAR" "$DEPLOY/echo-app.jar"
}

declare -a SUMMARY=()
fail=0
check() { if eval "$2"; then echo "PASS  $1"; else echo "FAIL  $1"; fail=1; fi; }
suite() {
    local name="$1"; shift
    echo "=== suite: $name"
    local out
    out=$("$@" 2>&1); local code=$?
    echo "$out"
    local p f
    p=$(grep -c '^PASS' <<<"$out"); f=$(grep -c '^FAIL' <<<"$out")
    if [ "$code" -eq 0 ] && [ "$f" -eq 0 ]; then SUMMARY+=("PASS  $name ($p pass)"); else SUMMARY+=("FAIL  $name ($p pass, $f fail, exit $code)"); fail=1; fi
}

G=$(mcp_name com.enonic.app.guillotine content "")
E=$(mcp_name com.example.echo tools "")
names() { mcp_call tools/list '{}' | python3 -c 'import json,sys; print(" ".join(t["name"] for t in json.load(sys.stdin)["result"]["tools"]))'; }
count_prefix() { names | tr ' ' '\n' | awk -v p="$1" 'index($0, p) == 1' | wc -l; }
wait_count() { # prefix op value timeout
    local i; for i in $(seq 1 "$4"); do [ "$(count_prefix "$1")" "$2" "$3" ] && { echo "      $1* $2 $3 after ${i}s"; return 0; }; sleep 1; done; return 1; }
echo_ok() { mcp_call tools/call "{\"name\":\"${E}echo\",\"arguments\":{\"message\":\"run-all\"}}" | grep -q '"structuredContent":{"message":"run-all"}'; }
projects_ok() { mcp_call tools/call "{\"name\":\"${G}list-projects\",\"arguments\":{}}" | grep -q '"structuredContent":{"projects":'; }

lifecycle() {
    echo "=== lifecycle (under lock)"
    check "precondition: 8 guillotine tools and echo listed" "[ \$(count_prefix $G) -eq 8 ] && [ \$(count_prefix $E) -eq 4 ]"
    rm -f "$DEPLOY/guillotine.jar"
    check "undeploy guillotine: all $G* gone within 30s" "wait_count $G -eq 0 30"
    check "  ${E}echo still listed" "[ \$(count_prefix ${E}echo) -eq 1 ]"
    check "  ${E}echo still works" "echo_ok"
    cp "$BACKUP/guillotine.jar" "$DEPLOY/guillotine.jar"
    check "redeploy guillotine: 8 tools back within 60s" "wait_count $G -eq 8 60"
    check "  list-projects works after redeploy" "projects_ok"
    rm -f "$DEPLOY/echo-app.jar"
    check "undeploy echo-app: echo tools gone within 30s" "wait_count $E -eq 0 30"
    check "  8 guillotine tools stay" "[ \$(count_prefix $G) -eq 8 ]"
    check "  list-projects still works" "projects_ok"
    cp "$ECHO_JAR" "$DEPLOY/echo-app.jar"
    check "redeploy echo-app: echo tools back within 30s" "wait_count $E -eq 4 30"
}

broken_app() {
    # An extra app whose tool name gives a public name over 128 characters (fixtures/broken-app).
    echo "=== broken app (under lock, XP_EXPECT=$XP_EXPECT)"
    local start i list call
    start=$(wc -l < "$XP_HOME/logs/console.log")
    cp "$BROKEN_JAR" "$DEPLOY/broken-app.jar"
    for i in $(seq 1 60); do tail -n +"$((start + 1))" "$XP_HOME/logs/console.log" | grep -q 'com.example.broken' && break; sleep 1; done
    sleep 3
    list=$(mcp_call tools/list '{}')
    call=$(mcp_call tools/call "{\"name\":\"${G}list-projects\",\"arguments\":{}}")
    if [ "$XP_EXPECT" = fix ]; then
        check "broken app deployed: tools/list still has 8 guillotine tools" "[ \$(count_prefix $G) -eq 8 ]"
        check "  no com.example.broken tools listed" "! grep -q 'com.example.broken__' <<<\"\$list\""
        check "  list-projects still works" "grep -q '\"structuredContent\":{\"projects\":' <<<\"\$call\""
        check "  log WARN names the broken app" "tail -n +\$((start + 1)) \"\$XP_HOME/logs/console.log\" | grep WARN | grep -q 'com.example.broken'"
    else
        echo "XPDEF broken app on the plain epic: tools/list -> $(head -c 160 <<<"$list")"
        echo "XPDEF broken app on the plain epic: ${G}list-projects -> $(head -c 160 <<<"$call")"
    fi
    rm -f "$DEPLOY/broken-app.jar"
    check "broken app removed: 8 guillotine tools listed within 30s" "wait_count $G -eq 8 30"
}

lifecycle_and_smoke() {
    # xp-smoke.sh also undeploys/redeploys echo-app, so it shares the lock.
    local before=$fail; fail=0
    lifecycle
    [ $fail -eq 0 ] && SUMMARY+=("PASS  lifecycle") || SUMMARY+=("FAIL  lifecycle")
    fail=$((fail | before)); before=$fail; fail=0
    broken_app
    [ $fail -eq 0 ] && SUMMARY+=("PASS  broken-app") || SUMMARY+=("FAIL  broken-app")
    fail=$((fail | before))
    suite "xp-smoke.sh" "$CHECKS/xp-smoke.sh"
}

suite "tools-all.py" python3 "$CHECKS/tools-all.py"
# The backup and every deploy-folder change, including the restore on an interrupted run, happen while the lock is held.
exec 9>"$LOCK"
flock 9
cp "$DEPLOY/guillotine.jar" "$BACKUP/guillotine.jar" 2>/dev/null || cp "$GUILLOTINE_JAR" "$BACKUP/guillotine.jar"
trap restore EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
lifecycle_and_smoke
restore
# tools-list-projects.sh deploys GUILLOTINE_JAR when the deployed jar differs.
suite "tools-list-projects.sh" "$CHECKS/tools-list-projects.sh"
trap - EXIT INT TERM
flock -u 9
exec 9>&-
suite "tools-search.py" python3 "$CHECKS/tools-search.py"
suite "tools-references.py" python3 "$CHECKS/tools-references.py"
suite "dataset-check.py" python3 "$CHECKS/dataset-check.py"

echo "=== SUMMARY"
printf '%s\n' "${SUMMARY[@]}"
[ $fail -eq 0 ] && echo "RESULT: PASS" || echo "RESULT: FAIL"
exit $fail
