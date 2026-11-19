#!/usr/bin/env python3
"""Turns a Claude Code stream-json transcript into a readable <name>.md next to it and prints a one-line summary.
usage: extract-transcript.py <name>.jsonl <question>"""
import json
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "checks"))
from mcpnames import GUILLOTINE_PREFIX, client_name  # noqa: E402

CLIENT_PREFIX = client_name(GUILLOTINE_PREFIX)


def extract(path, question):
    ev = [json.loads(l) for l in open(path) if l.strip()]
    init = next((e for e in ev if e.get('type') == 'system' and e.get('subtype') == 'init'), {})
    res = next((e for e in ev if e.get('type') == 'result'), {})
    calls, results = [], {}
    for e in ev:
        if e.get('type') == 'assistant':
            for c in e['message'].get('content', []):
                if c.get('type') == 'tool_use':
                    calls.append(c)
        if e.get('type') == 'user':
            cont = e['message'].get('content', [])
            if isinstance(cont, list):
                for c in cont:
                    if c.get('type') == 'tool_result':
                        results[c['tool_use_id']] = c
    out = [f"# {os.path.basename(path)[:-6]}", "", f"**Question:** {question}", "",
           f"- Claude Code: {init.get('claude_code_version')}, model: {init.get('model')}",
           f"- MCP servers: {init.get('mcp_servers')}",
           f"- tools available: {init.get('tools')}",
           f"- turns: {res.get('num_turns')}, duration_ms: {res.get('duration_ms')}, cost_usd: {res.get('total_cost_usd')}, "
           f"is_error: {res.get('is_error')}",
           f"- tool calls: {len(calls)}", "", "## Tool calls", ""]
    errs = 0
    for i, c in enumerate(calls, 1):
        r = results.get(c['id'], {})
        rc = r.get('content')
        txt = rc if isinstance(rc, str) else "\n".join(x.get('text', '') for x in (rc or []) if isinstance(x, dict))
        if r.get('is_error'):
            errs += 1
        out += [f"### {i}. `{c['name']}` {'**isError**' if r.get('is_error') else ''}", "", "```json",
                json.dumps(c['input'], ensure_ascii=False, indent=1), "```", "",
                "Result (first 1500 chars):", "", "```", txt[:1500], "```", ""]
    out += ["## Final answer", "", res.get('result') or '(none)', ""]
    open(path[:-6] + '.md', 'w').write("\n".join(out))
    print(f"  calls={len(calls)} errors={errs} turns={res.get('num_turns')} cost={res.get('total_cost_usd')} "
          f"dur={res.get('duration_ms')} model={init.get('model')}")
    print("  tools:", [c['name'].replace(CLIENT_PREFIX, '') + json.dumps(c['input'], ensure_ascii=False)[:200] for c in calls])


if __name__ == "__main__":
    extract(sys.argv[1], sys.argv[2])
