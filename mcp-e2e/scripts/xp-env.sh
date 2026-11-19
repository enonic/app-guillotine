# Shared settings for local XP runs. Source it; every value below can be overridden from the environment.
MCP_E2E_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="$(cd "$MCP_E2E_DIR/.." && pwd)"
# Scratch area for XP home, built fixture jars, logs and client transcripts. Nothing here is committed.
export MCP_E2E_WORK="${MCP_E2E_WORK:-$REPO_ROOT/.tmp/mcp-e2e}"
# XP distribution to run: <xp checkout>/modules/runtime/build/install. Required by xp-start.sh only.
export XP_DIST="${XP_DIST:-}"
# Script engine for xp-start.sh: empty = XP default (Nashorn), or GraalJS.
export XP_SCRIPT_ENGINE="${XP_SCRIPT_ENGINE:-}"
export XP_HOME="${XP_HOME:-$MCP_E2E_WORK/xp-home}"
export XP_URL="${XP_URL:-http://localhost:8080}"
export MCP_URL="${MCP_URL:-$XP_URL/api/system:mcp}"
export XP_PID_FILE="${XP_PID_FILE:-$MCP_E2E_WORK/xp.pid}"
export LOG_DIR="${LOG_DIR:-$MCP_E2E_WORK/logs}"
# Which XP MCP contract the suites expect: epic (epic/12627-xp-mcp-server) or fix (issue-12662 branch).
# xp-smoke.sh and run-all.sh refuse to run without it.
export XP_EXPECT="${XP_EXPECT:-}"
# Local-only throwaway su password, written into a fresh XP_HOME by xp-start.sh. Never reuse it anywhere real.
export XP_SU_PASSWORD="${XP_SU_PASSWORD:-mcp-e2e-local}"
export GUILLOTINE_JAR="${GUILLOTINE_JAR:-$REPO_ROOT/build/libs/guillotine.jar}"
export FIXTURE_BUILD_DIR="${FIXTURE_BUILD_DIR:-$MCP_E2E_WORK/build}"
export ECHO_JAR="${ECHO_JAR:-$FIXTURE_BUILD_DIR/echo-app/libs/echo-app.jar}"
export BROKEN_JAR="${BROKEN_JAR:-$FIXTURE_BUILD_DIR/broken-app/libs/broken-app.jar}"
export DATASET_JAR="${DATASET_JAR:-$FIXTURE_BUILD_DIR/dataset-app/libs/dataset-app.jar}"

require_xp_expect() {
    case "$XP_EXPECT" in
        epic|fix) ;;
        *) echo "Set XP_EXPECT=epic (plain epic/12627-xp-mcp-server) or XP_EXPECT=fix (issue-12662 branch)" >&2; exit 2 ;;
    esac
}

mcp_call() {
    # mcp_call <method> <params-json> [extra curl args...]
    local method="$1" params="$2"; shift 2
    local name
    name=$(printf '%s' "$params" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("name",""))')
    local body
    body=$(python3 - "$method" "$params" <<'PY'
import json, sys
method, params = sys.argv[1], json.loads(sys.argv[2])
params["_meta"] = {
    "io.modelcontextprotocol/protocolVersion": "2026-07-28",
    "io.modelcontextprotocol/clientInfo": {"name": "mcp-e2e", "version": "1.0"},
    "io.modelcontextprotocol/clientCapabilities": {},
}
print(json.dumps({"jsonrpc": "2.0", "id": 1, "method": method, "params": params}))
PY
)
    local headers=(-H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream'
                   -H 'MCP-Protocol-Version: 2026-07-28' -H "Mcp-Method: $method")
    [ -n "$name" ] && headers+=(-H "Mcp-Name: $name")
    curl -sS "$MCP_URL" "${headers[@]}" --data "$body" "$@"
}

mcp_name() {
    # mcp_name <app-key> <group> <local-name>: the public tool name XP's McpNames.qualify gives.
    python3 "$MCP_E2E_DIR/checks/mcpnames.py" "$@"
}
