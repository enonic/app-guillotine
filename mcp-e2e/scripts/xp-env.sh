# Shared settings for local XP runs. Source it; the values read from the environment can be overridden there. XP_HOME, XP_URL and
# MCP_URL are set from MCP_E2E_* variables only: a shell that exports them for a real XP must not point these scripts at it.
MCP_E2E_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="$(cd "$MCP_E2E_DIR/.." && pwd)"
# Scratch area for XP home, built fixture jars and logs. Nothing here is committed.
export MCP_E2E_WORK="${MCP_E2E_WORK:-$REPO_ROOT/.tmp/mcp-e2e}"
# XP distribution to run: <xp checkout>/modules/runtime/build/install. Required by xp-start.sh only.
export XP_DIST="${XP_DIST:-}"
# Script engine for xp-start.sh: empty = XP default (Nashorn), or GraalJS.
export XP_SCRIPT_ENGINE="${XP_SCRIPT_ENGINE:-}"
export MCP_E2E_XP_HOME="${MCP_E2E_XP_HOME:-$MCP_E2E_WORK/xp-home}"
export XP_HOME="$MCP_E2E_XP_HOME"
export MCP_E2E_XP_URL="${MCP_E2E_XP_URL:-http://localhost:8080}"
export XP_URL="$MCP_E2E_XP_URL"
export MCP_URL="$XP_URL/api/system:mcp"
export XP_PID_FILE="$MCP_E2E_WORK/xp.pid"
export LOG_DIR="$MCP_E2E_WORK/logs"
# The checks run from the source tree; keep Python from writing __pycache__ there.
export PYTHONDONTWRITEBYTECODE=1
# Local-only throwaway su password, written into a fresh XP_HOME by xp-start.sh. Never reuse it anywhere real.
export XP_SU_PASSWORD="${XP_SU_PASSWORD:-mcp-e2e-local}"
export GUILLOTINE_JAR="${GUILLOTINE_JAR:-$REPO_ROOT/build/libs/guillotine.jar}"
export FIXTURE_BUILD_DIR="${FIXTURE_BUILD_DIR:-$MCP_E2E_WORK/build}"
export DATASET_JAR="${DATASET_JAR:-$FIXTURE_BUILD_DIR/dataset-app/libs/dataset-app.jar}"
# The standard ID provider app serves the sign-in endpoint /_/idprovider/system that login.sh uses. XP development builds do not
# bundle it; an XP SDK distribution downloaded by the Enonic CLI has it in system/40.
if [ -z "${IDPROVIDER_JAR:-}" ]; then
    IDPROVIDER_JAR=$(ls -1 "$HOME"/.enonic/distributions/*/system/40/app-standardidprovider-*.jar 2>/dev/null | sort -V | tail -n 1 || true)
fi
export IDPROVIDER_JAR
# Signed-in runs use this dataset user, an editor on mcp-test. The password is the local-only value from dataset.json.
export MCP_E2E_USER="${MCP_E2E_USER:-mcp-alice}"
export MCP_E2E_PASSWORD="${MCP_E2E_PASSWORD:-alice-local-only!}"
export GUILLOTINE_CFG="$XP_HOME/config/com.enonic.app.guillotine.cfg"
# The Guillotine config the suites expect: the dataset project as default, anonymous callers allowed on content and graphql.
BASE_CFG=$'mcp.defaultProject=mcp-test\nmcp.allowAnonymous=true'
GUILLOTINE_PREFIX="com.enonic.app.guillotine__"

process_start() {
    # process_start <pid>: the start time of a running process, empty when there is none. Linux: clock ticks after boot (field 22
    # of /proc/<pid>/stat, counted after the command name, which may hold spaces); elsewhere: ps lstart, to the second.
    if [ -r "/proc/$1/stat" ]; then
        sed 's/^.*) //' "/proc/$1/stat" 2>/dev/null | cut -d' ' -f20
    else
        LC_ALL=C ps -o lstart= -p "$1" 2>/dev/null | sed 's/^ *//'
    fi
}

is_local_xp() {
    # is_local_xp <pid>: whether the process is an XP java process with this XP_HOME (-Dxp.home, or the work dir of older starts).
    local args
    args=$(ps -o args= -p "$1" 2>/dev/null) || return 1
    case "$args" in
        *java*com.enonic.xp.launcher.LauncherMain*) ;;
        *) return 1 ;;
    esac
    case "$args" in
        *"-Dxp.home=$XP_HOME "*|*"-Djava.io.tmpdir=$XP_HOME/work "*) return 0 ;;
    esac
    return 1
}

mcp_call() {
    # mcp_call <method> <params-json> [extra curl args...]. Set MCP_COOKIE (from login.sh) to call as a signed-in user.
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
    [ -n "${MCP_COOKIE:-}" ] && headers+=(-H "Cookie: $MCP_COOKIE")
    curl -sS "$MCP_URL" "${headers[@]}" --data "$body" "$@"
}

deploy_jar() {
    # deploy_jar <jar> <file-name>: copies the jar into XP_HOME/deploy under a name the deploy monitor ignores, then renames it, so
    # the monitor never sees a half-written jar and installs it a second time once the copy completes.
    local dest="$XP_HOME/deploy/$2"
    cp "$1" "$dest.part" && mv -f "$dest.part" "$dest"
}

console_lines() { wc -l < "$XP_HOME/logs/console.log" | tr -d ' '; }

# Seconds without a new install/start log line before an app counts as settled. XP's deploy monitor polls every second
# (com.enonic.xp.server.deploy interval) but an install blocks it, so a second install of one copy has logged ~5 s after the first.
XP_SETTLE_QUIET="${XP_SETTLE_QUIET:-6}"

wait_app_settled() {
    # wait_app_settled <app-key> <console-line> <timeout-s> <ready-command>: after a jar copy, waits until the app has logged
    # install/start lines after <console-line>, then none for XP_SETTLE_QUIET seconds, and <ready-command> (eval'd: a tool count,
    # a probe call) succeeds. A tool count alone passes between two installs of the same jar.
    local key="$1" from="$2" timeout="$3" ready="$4" i n=0 last=-1 quiet=0
    for i in $(seq 1 "$timeout"); do
        n=$(tail -n +"$((from + 1))" "$XP_HOME/logs/console.log" | grep -cF -e "application $key bundle " -e "application [$key]" || true)
        if [ "$n" = "$last" ]; then quiet=$((quiet + 1)); else quiet=0; last=$n; fi
        if [ "$n" -gt 0 ] && [ "$quiet" -ge "$XP_SETTLE_QUIET" ] && eval "$ready"; then
            echo "      $key settled after ${i}s ($n install/start log lines)"; return 0
        fi
        sleep 1
    done
    echo "      $key did not settle within ${timeout}s ($n install/start log lines)"; return 1
}

write_cfg() {
    # write_cfg <properties>: replaces Guillotine's config file; XP applies it to the running app without a restart.
    printf '%s\n' "$1" > "$GUILLOTINE_CFG.part" && mv -f "$GUILLOTINE_CFG.part" "$GUILLOTINE_CFG"
}

wait_for() {
    # wait_for <timeout-s> <command>: runs the eval'd command once a second until it succeeds.
    local i
    for i in $(seq 1 "$1"); do eval "$2" && return 0; sleep 1; done
    return 1
}

projects_ok() {
    # Whether list-projects answers for the current caller (MCP_COOKIE, or anonymous).
    mcp_call tools/call "{\"name\":\"${GUILLOTINE_PREFIX}content__list-projects\",\"arguments\":{}}" |
        grep -q '"structuredContent":{"caller":'
}
