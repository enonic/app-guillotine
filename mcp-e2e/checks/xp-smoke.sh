#!/usr/bin/env bash
# XP checks: app tool catalog + dispatch + lifecycle on a running local XP (see scripts/xp-start.sh).
# Deploys and undeploys echo-app.jar, so it needs a local XP_HOME. XP_EXPECT picks the contract: epic = plain epic/12627-xp-mcp-server, fix = issue-12662 branch.
# PASS/FAIL lines check that contract; XPDEF lines record known XP defects without failing the suite.
set -uo pipefail
source "$(dirname "$0")/../scripts/xp-env.sh"
require_xp_expect
JAR="$ECHO_JAR"
E=$(mcp_name com.example.echo tools "")
fail=0
echo "      XP_EXPECT=$XP_EXPECT"
check() { if eval "$2"; then echo "PASS  $1"; else echo "FAIL  $1"; fail=1; fi; }
tool_count() { mcp_call tools/list '{}' | python3 -c 'import json,sys; print(len([t for t in json.load(sys.stdin)["result"]["tools"] if t["name"].startswith(sys.argv[1])]))' "$E"; }
wait_count() { for _ in $(seq 1 30); do [ "$(tool_count)" = "$1" ] && return 0; sleep 1; done; return 1; }
call() { mcp_call tools/call "{\"name\":\"$E$1\",\"arguments\":$2}"; }

discover=$(mcp_call server/discover '{}')
check "server/discover advertises 2026-07-28" "grep -q '\"supportedVersions\":\[\"2026-07-28\"\]' <<<\"\$discover\""

cp "$JAR" "$XP_HOME/deploy/"
check "deploy: echo tools appear without restart" "wait_count 4"
list=$(mcp_call tools/list '{}')
check "tools/list contains ${E}echo" "grep -q '\"name\":\"${E}echo\"' <<<\"\$list\""

ok=$(call echo '{"message":"XP MCP smoke"}')
check "tools/call echo returns message (args reach the script)" "grep -q '\"structuredContent\":{\"message\":\"XP MCP smoke\"}' <<<\"\$ok\""

undef=$(call undefined-field '{"message":"x"}')
echo "XPDEF undefined field in a script result becomes null and fails outputSchema: $(grep -o '"text":"[^"]*"' <<<"$undef" | head -1)"

probe=$(call args-probe '{"message":"p"}')
probe_text=$(python3 -c 'import json,sys; print(json.loads(sys.argv[1])["result"]["content"][0]["text"])' "$probe" 2>/dev/null)
echo "      JSON.stringify(args) in a controller: $probe_text"
if [ "$XP_EXPECT" = fix ]; then
    check "args arrive as a JS object: JSON.stringify(args) works" "[ \"\$probe_text\" = 'stringify:{\"message\":\"p\"}' ]"
fi

bad=$(call echo '{"message":1}')
check "invalid arguments -> isError result" "grep -q '\"isError\":true' <<<\"\$bad\" && ! grep -q '\"error\":' <<<\"\$bad\""

start=$(wc -l < "$XP_HOME/logs/console.log")
thrown=$(call fail '{}')
if [ "$XP_EXPECT" = fix ]; then
    error_id=$(grep -o 'MCP tool execution failed (error id [0-9a-f]*)' <<<"$thrown" | grep -o '[0-9a-f]\{8\})' | tr -d ')')
    check "controller exception -> isError with generic text and error id (fix contract)" "grep -q '\"isError\":true' <<<\"\$thrown\" && ! grep -q '\"error\":{' <<<\"\$thrown\" && [ -n \"\$error_id\" ]"
    check "log has the same error id at WARN" "tail -n +\$((start + 1)) \"\$XP_HOME/logs/console.log\" | grep 'WARN' | grep -q \"error id \$error_id\""
else
    check "controller exception -> JSON-RPC -32603 Internal error (epic contract)" "grep -q '\"error\":{\"code\":-32603,\"message\":\"Internal error\"}' <<<\"\$thrown\""
fi
check "exception message not sent to the client" "! grep -q 'Echo fixture failure' <<<\"\$thrown\""
check "log has the original error" "tail -n +\$((start + 1)) \"\$XP_HOME/logs/console.log\" | grep -q 'Echo fixture failure'"

rm -f "$XP_HOME/deploy/echo-app.jar"
check "undeploy: echo tools disappear without restart" "wait_count 0"
gone=$(call echo '{"message":"x"}')
check "call to removed tool -> Unknown tool" "grep -q \"Unknown tool: ${E}echo\" <<<\"\$gone\""

cp "$JAR" "$XP_HOME/deploy/"
check "redeploy: echo tools come back" "wait_count 4"
exit $fail
