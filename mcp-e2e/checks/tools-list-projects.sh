#!/usr/bin/env bash
# Guillotine contributes its list-projects tool on a running local XP; deploys GUILLOTINE_JAR if it differs.
set -uo pipefail
source "$(dirname "$0")/../scripts/xp-env.sh"
JAR="$GUILLOTINE_JAR"
TOOL=$(mcp_name com.enonic.app.guillotine content list-projects)
fail=0
check() { if eval "$2"; then echo "PASS  $1"; else echo "FAIL  $1"; fail=1; fi; }
has_tool() { mcp_call tools/list '{}' | grep -q "\"name\":\"$TOOL\""; }
wait_tool() { for _ in $(seq 1 60); do has_tool && return 0; sleep 1; done; return 1; }
log_lines() { wc -l < "$XP_HOME/logs/console.log"; }

start=$(log_lines)
if ! cmp -s "$JAR" "$XP_HOME/deploy/guillotine.jar"; then
    cp "$JAR" "$XP_HOME/deploy/guillotine.jar"
    sleep 3
fi
check "deploy: $TOOL is listed" "wait_tool"

tool=$(mcp_call tools/list '{}' | python3 -c '
import json, sys
tools = [t for t in json.load(sys.stdin)["result"]["tools"] if t["name"] == sys.argv[1]]
print(json.dumps(tools[0] if tools else {}))' "$TOOL")
check "annotations: read-only, non-destructive, idempotent, closed world" \
    "python3 -c 'import json,sys; a=json.loads(sys.argv[1]).get(\"annotations\",{}); sys.exit(0 if a=={\"readOnlyHint\":True,\"destructiveHint\":False,\"idempotentHint\":True,\"openWorldHint\":False} else 1)' \"\$tool\""
check "outputSchema declared" "python3 -c 'import json,sys; sys.exit(0 if \"outputSchema\" in json.loads(sys.argv[1]) else 1)' \"\$tool\""

call=$(mcp_call tools/call "{\"name\":\"$TOOL\",\"arguments\":{}}")
echo "      anonymous result: $(python3 -c 'import json,sys; print(json.dumps(json.loads(sys.argv[1])["result"].get("structuredContent")))' "$call")"
check "tools/call returns structuredContent.projects array" \
    "python3 -c 'import json,sys; r=json.loads(sys.argv[1])[\"result\"]; sys.exit(0 if not r.get(\"isError\") and isinstance(r[\"structuredContent\"][\"projects\"], list) else 1)' \"\$call\""
check "text content = summary line + JSON of structuredContent" \
    "python3 -c 'import json,sys; r=json.loads(sys.argv[1])[\"result\"]; t=r[\"content\"][0][\"text\"]; sys.exit(0 if json.loads(t.split(\"\\n\",1)[1])==r[\"structuredContent\"] else 1)' \"\$call\""
check "no null fields in structuredContent" "! grep -q ':null' <<<\"\$call\""

extra=$(mcp_call tools/call "{\"name\":\"$TOOL\",\"arguments\":{\"project\":\"x\"}}")
check "unexpected argument -> isError result" "grep -q '\"isError\":true' <<<\"\$extra\" && ! grep -q '\"error\":' <<<\"\$extra\""

gql=$(curl -sS "$XP_URL/api/com.enonic.app.guillotine:graphql" -H 'Content-Type: application/json' --data '{"query":"{__typename}"}')
check "Guillotine HTTP GraphQL still answers" "grep -q '\"__typename\":\"Query\"' <<<\"\$gql\""

errors=$(tail -n +"$((start + 1))" "$XP_HOME/logs/console.log" | grep -E 'ERROR|Exception' | grep -iE 'guillotine' || true)
check "no Guillotine errors in console.log during the run" "[ -z \"\$errors\" ]"
[ -n "$errors" ] && echo "$errors" | head -20
exit $fail
