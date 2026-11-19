#!/usr/bin/env bash
# Stops only the XP instance started by xp-start.sh.
set -uo pipefail
source "$(dirname "$0")/xp-env.sh"
[ -f "$XP_PID_FILE" ] || { echo "No pid file at $XP_PID_FILE"; exit 0; }
pid=$(sed -n 1p "$XP_PID_FILE")
started=$(sed -n 2p "$XP_PID_FILE")
if [ -z "$started" ] || [ "$(process_start "$pid")" != "$started" ]; then
    echo "PID $pid is not the XP that xp-start.sh started (it exited, or the PID was reused); nothing to stop"
    rm -f "$XP_PID_FILE"; exit 0
fi
pkill -TERM -P "$pid" 2>/dev/null; kill "$pid" 2>/dev/null
# Wait for the process itself, not only the port: a JVM still shutting down keeps writing to console.log.
for i in $(seq 1 60); do
    if ! kill -0 "$pid" 2>/dev/null && ! lsof -ti :8080 -sTCP:LISTEN >/dev/null; then break; fi
    sleep 1
done
rm -f "$XP_PID_FILE"; echo "XP stopped"
