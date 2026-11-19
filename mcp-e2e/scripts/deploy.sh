#!/usr/bin/env bash
# Copies guillotine.jar, dataset-app.jar and echo-app.jar into XP_HOME/deploy and waits for the dataset to be created.
set -euo pipefail
source "$(dirname "$0")/xp-env.sh"
for jar in "$GUILLOTINE_JAR" "$DATASET_JAR" "$ECHO_JAR"; do
    [ -f "$jar" ] || { echo "Missing $jar" >&2; exit 2; }
done
start=$(wc -l < "$XP_HOME/logs/console.log")
cp "$GUILLOTINE_JAR" "$XP_HOME/deploy/guillotine.jar"
cp "$ECHO_JAR" "$XP_HOME/deploy/echo-app.jar"
if cmp -s "$DATASET_JAR" "$XP_HOME/deploy/dataset-app.jar"; then
    # Unchanged jar: XP does not restart the app, so the outcome of its start in the running XP decides. xp-start.sh truncates
    # console.log when it starts XP, so the whole log belongs to the XP its pid file names.
    echo "dataset-app.jar is already deployed unchanged"
    pid=$(sed -n 1p "$XP_PID_FILE" 2>/dev/null || true)
    started=$(sed -n 2p "$XP_PID_FILE" 2>/dev/null || true)
    if [ -z "$started" ] || [ "$(process_start "$pid")" != "$started" ]; then
        echo "Cannot tell whether dataset-app started in the running XP: no current pid file from xp-start.sh at $XP_PID_FILE." \
            "Restart XP with xp-start.sh, or remove $XP_HOME/deploy/dataset-app.jar and deploy again." >&2
        exit 1
    fi
    start=0
else
    cp "$DATASET_JAR" "$XP_HOME/deploy/dataset-app.jar"
fi
for _ in $(seq 1 120); do
    if line=$(tail -n +"$((start + 1))" "$XP_HOME/logs/console.log" | grep -E '\[mcp-dataset\] (done|failed)' | tail -n 1) &&
        [ -n "$line" ]; then
        echo "$line"
        grep -q '\[mcp-dataset\] done' <<<"$line" && exit 0
        echo "Dataset creation failed; fix the cause, then rebuild or remove $XP_HOME/deploy/dataset-app.jar and deploy again" >&2
        exit 1
    fi
    sleep 1
done
echo "No [mcp-dataset] done line in $XP_HOME/logs/console.log within 120s" >&2; exit 1
