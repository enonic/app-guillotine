#!/usr/bin/env bash
# Copies the standard ID provider, guillotine.jar and dataset-app.jar into XP_HOME/deploy and waits until sign-in works, the tools
# answer and the dataset exists.
set -euo pipefail
source "$(dirname "$0")/xp-env.sh"
[ -n "$IDPROVIDER_JAR" ] && [ -f "$IDPROVIDER_JAR" ] || {
    echo "Set IDPROVIDER_JAR to app-standardidprovider-<version>.jar (system/40 of an XP SDK distribution)" >&2; exit 2; }
for jar in "$GUILLOTINE_JAR" "$DATASET_JAR"; do
    [ -f "$jar" ] || { echo "Missing $jar" >&2; exit 2; }
done
[ -f "$GUILLOTINE_CFG" ] || write_cfg "$BASE_CFG"
start=$(console_lines)
copied=$start
cmp -s "$IDPROVIDER_JAR" "$XP_HOME/deploy/app-standardidprovider.jar" || deploy_jar "$IDPROVIDER_JAR" app-standardidprovider.jar
deploy_jar "$GUILLOTINE_JAR" guillotine.jar
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
    deploy_jar "$DATASET_JAR" dataset-app.jar
    wait_app_settled com.example.mcpdataset "$copied" 120 true
fi
for _ in $(seq 1 120); do
    if line=$(tail -n +"$((start + 1))" "$XP_HOME/logs/console.log" | grep -E '\[mcp-dataset\] (done|failed)' | tail -n 1) &&
        [ -n "$line" ]; then
        echo "$line"
        grep -q '\[mcp-dataset\] done' <<<"$line" && break
        echo "Dataset creation failed; fix the cause, then rebuild or remove $XP_HOME/deploy/dataset-app.jar and deploy again" >&2
        exit 1
    fi
    sleep 1
done
grep -q '\[mcp-dataset\] done' <<<"${line:-}" || {
    echo "No [mcp-dataset] done line in $XP_HOME/logs/console.log within 120s" >&2; exit 1; }
wait_for 120 '"$(dirname "$0")/login.sh" >/dev/null 2>&1' || { echo "Sign-in at $XP_URL/_/idprovider/system does not work" >&2; exit 1; }
echo "sign-in works for $MCP_E2E_USER"
wait_app_settled com.enonic.app.guillotine "$copied" 120 'MCP_COOKIE=$("$(dirname "$0")/login.sh") projects_ok'
