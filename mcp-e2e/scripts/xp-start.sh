#!/usr/bin/env bash
# Starts the XP build in XP_DIST with an isolated XP_HOME. Refuses to touch ports owned by others.
set -euo pipefail
source "$(dirname "$0")/xp-env.sh"
[ -n "$XP_DIST" ] && [ -x "$XP_DIST/bin/server.sh" ] || {
    echo "Set XP_DIST to an XP install dir (<xp checkout>/modules/runtime/build/install)" >&2; exit 2; }
for port in 8080 4848 2609; do
    if pid=$(lsof -ti :$port -sTCP:LISTEN); then
        echo "Port $port is already used by PID $pid" >&2; exit 1
    fi
done
mkdir -p "$MCP_E2E_WORK"
if [ ! -d "$XP_HOME" ]; then
    cp -r "$XP_DIST/home" "$XP_HOME"
    hash=$(printf '%s' "$XP_SU_PASSWORD" | sha256sum | cut -d' ' -f1)
    echo "xp.suPassword={sha256}$hash" >> "$XP_HOME/config/system.properties"
fi
# Anonymous callers (every MCP client today) see no project unless it is the configured default.
[ -f "$XP_HOME/config/com.enonic.app.guillotine.cfg" ] ||
    echo "mcp.defaultProject=mcp-test" > "$XP_HOME/config/com.enonic.app.guillotine.cfg"
mkdir -p "$XP_HOME/logs" "$XP_HOME/deploy"
echo "XP_DIST=$XP_DIST XP_HOME=$XP_HOME engine=${XP_SCRIPT_ENGINE:-default}"
JAVA_OPTS="-XX:-OmitStackTraceInFastThrow -XX:+AlwaysPreTouch -Djava.net.preferIPv4Stack=true -Dxp.home=$XP_HOME${XP_SCRIPT_ENGINE:+ -Dxp.script-engine=$XP_SCRIPT_ENGINE}" \
    XP_HOME="$XP_HOME" nohup "$XP_DIST/bin/server.sh" > "$XP_HOME/logs/console.log" 2>&1 &
pid=$!
# The start time identifies the process, so xp-stop.sh never signals a later process that reuses the PID.
printf '%s\n%s\n' "$pid" "$(process_start "$pid")" > "$XP_PID_FILE"
for i in $(seq 1 180); do
    if curl -s -o /dev/null "$XP_URL/"; then
        if [ "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$MCP_URL" -H 'Content-Type: application/json' --data '{}')" != "000" ]; then
            echo "XP is up (pid $pid) after ${i}s"; exit 0
        fi
    fi
    sleep 1
done
echo "XP did not start in time; see $XP_HOME/logs" >&2; exit 1
