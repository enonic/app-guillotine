#!/usr/bin/env bash
# Stops only the XP instance started by xp-start.sh.
set -uo pipefail
source "$(dirname "$0")/xp-env.sh"
[ -f "$XP_PID_FILE" ] || { echo "No pid file at $XP_PID_FILE"; exit 0; }
pid=$(cat "$XP_PID_FILE")
pkill -TERM -P "$pid" 2>/dev/null; kill "$pid" 2>/dev/null
# Wait for the process itself, not only the port: a JVM still shutting down keeps writing to console.log.
for i in $(seq 1 60); do
    if ! kill -0 "$pid" 2>/dev/null && ! lsof -ti :8080 -sTCP:LISTEN >/dev/null; then break; fi
    sleep 1
done
rm -f "$XP_PID_FILE"; echo "XP stopped"
