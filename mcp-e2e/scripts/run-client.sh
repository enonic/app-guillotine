#!/usr/bin/env bash
# Asks Claude Code each prompt in a fresh process, with only the local XP MCP server and no other tools.
# usage: run-client.sh [prompt-name ...]   (names from prompts/ without .txt; default: all)
# Transcripts go to $MCP_E2E_WORK/client-runs/<timestamp>/: <name>.jsonl (raw), <name>.stderr, <name>.md (readable).
# Exits 1 when a prompt is missing or a client run or its transcript extraction fails; the answers themselves are checked by hand.
set -uo pipefail
source "$(dirname "$0")/xp-env.sh"
PROMPTS="$MCP_E2E_DIR/prompts"
OUT="$MCP_E2E_WORK/client-runs/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"
# The server name "xp" fixes the client-side tool names (mcp__xp__...); see checks/mcpnames.py.
printf '{"mcpServers":{"xp":{"type":"http","url":"%s"}}}\n' "$MCP_URL" > "$OUT/mcp-config.json"
names=("$@")
if [ ${#names[@]} -eq 0 ]; then
    for f in "$PROMPTS"/*.txt; do names+=("$(basename "$f" .txt)"); done
fi
model_args=()
[ -n "${CLAUDE_MODEL:-}" ] && model_args=(--model "$CLAUDE_MODEL")
failed=()
for name in "${names[@]}"; do
    if [ ! -f "$PROMPTS/$name.txt" ]; then
        echo "$name: no prompt file $PROMPTS/$name.txt" >&2; failed+=("$name"); continue
    fi
    question=$(grep -v '^#' "$PROMPTS/$name.txt")
    start=$(date +%s)
    (cd "$OUT" && claude -p "$question" "${model_args[@]}" --mcp-config mcp-config.json --strict-mcp-config \
        --setting-sources project --tools "" --disable-slash-commands --no-session-persistence \
        --allowedTools "mcp__xp__*" \
        --disallowedTools "Bash,Read,Edit,Write,WebFetch,WebSearch,Glob,Grep,Task,Agent,NotebookEdit" \
        --max-budget-usd "${CLAUDE_MAX_BUDGET_USD:-3}" \
        --output-format stream-json --verbose < /dev/null > "$name.jsonl" 2> "$name.stderr")
    code=$?
    echo "$name: exit=$code wall=$(( $(date +%s) - start ))s"
    python3 "$MCP_E2E_DIR/scripts/extract-transcript.py" "$OUT/$name.jsonl" "$question" || code=1
    [ "$code" -eq 0 ] || failed+=("$name")
done
echo "transcripts: $OUT"
if [ ${#failed[@]} -gt 0 ]; then
    echo "failed: ${failed[*]}" >&2; exit 1
fi
