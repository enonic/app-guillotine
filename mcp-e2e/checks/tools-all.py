#!/usr/bin/env python3
"""All Guillotine MCP tools (no lifecycle): catalog shape, one good call per tool, error quality, pagination, truncation.
Anonymous caller (master). Prints PASS/FAIL lines; exit 1 on any FAIL."""
import json
import os
import re
import sys
import urllib.request

sys.dont_write_bytecode = True  # keep __pycache__ out of the source tree
from mcpnames import GUILLOTINE_PREFIX, client_name  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
XP_URL = os.environ.get("XP_URL", "http://localhost:8080")
MCP_URL = os.environ.get("MCP_URL", XP_URL + "/api/system:mcp")
GQL_URL = XP_URL + "/api/com.enonic.app.guillotine:graphql"
MAX_RESULT_CHARS = 20000
ENVELOPE_ALLOWANCE = 1000
PREFIX = GUILLOTINE_PREFIX
TOOLS = ["list-projects", "list-types", "search", "get", "describe-type", "query", "find-references", "check-links"]
ANNOTATIONS = {"readOnlyHint": True, "destructiveHint": False, "idempotentHint": True, "openWorldHint": False}
STATIC_ENUMS = [{"draft", "master"}, {"inbound", "outbound", "both"}, {"IN_PROGRESS", "READY"}]
JAVA_NAME = re.compile(r"\b[a-z]+(\.[a-z0-9_]+)+\.[A-Z]\w+|Exception")

failures = 0


def check(name, ok, detail=""):
    global failures
    print(("PASS  " if ok else "FAIL  ") + name + ("" if ok or not detail else "  -- " + str(detail)[:700]))
    if not ok:
        failures += 1


def info(line):
    print("      " + line)


def post(url, payload, headers):
    request = urllib.request.Request(url, data=json.dumps(payload).encode(), headers=headers)
    with urllib.request.urlopen(request) as response:
        raw = response.read().decode()
    return raw, json.loads(raw)


def rpc(method, params, name=""):
    params = dict(params)
    params["_meta"] = {
        "io.modelcontextprotocol/protocolVersion": "2026-07-28",
        "io.modelcontextprotocol/clientInfo": {"name": "mcp-e2e", "version": "1.0"},
        "io.modelcontextprotocol/clientCapabilities": {},
    }
    headers = {"Content-Type": "application/json", "Accept": "application/json, text/event-stream",
               "MCP-Protocol-Version": "2026-07-28", "Mcp-Method": method}
    if name:
        headers["Mcp-Name"] = name
    return post(MCP_URL, {"jsonrpc": "2.0", "id": 1, "method": method, "params": params}, headers)


def call(tool, arguments):
    """Returns (response, raw length, budgeted length = structuredContent, or the whole result for an error)."""
    raw, response = rpc("tools/call", {"name": PREFIX + tool, "arguments": arguments}, PREFIX + tool)
    result = response.get("result", {})
    own = {k: result[k] for k in ("content", "structuredContent", "isError") if k in result}
    measured = result.get("structuredContent", own)
    return response, len(raw), len(json.dumps(measured, separators=(",", ":"), ensure_ascii=False))


def good(response):
    result = response.get("result", {})
    return "structuredContent" in result and not result.get("isError")


# 1. discover + catalog
raw, discover = rpc("server/discover", {})
check("server/discover advertises 2026-07-28",
      "2026-07-28" in discover.get("result", {}).get("supportedVersions", []), raw[:300])

tools = {t["name"]: t for t in rpc("tools/list", {})[1]["result"]["tools"]}
ours = {n: t for n, t in tools.items() if n.startswith(PREFIX)}
check("tools/list: all 8 guillotine tools present", set(ours) == {PREFIX + t for t in TOOLS}, sorted(ours))
check("tools/list: full read-only annotations on all 8",
      all(t.get("annotations") == ANNOTATIONS for t in ours.values()),
      {n: t.get("annotations") for n, t in ours.items() if t.get("annotations") != ANNOTATIONS})
check("tools/list: names match [A-Za-z0-9_.-]{1,128} with no _u/_x escapes (hyphenated local names)",
      all(re.fullmatch(r"[A-Za-z0-9_.-]{1,128}", n) and "_u" not in n and "_x" not in n for n in ours), sorted(ours))
clients = {n: client_name(n) for n in ours}
info("public / Claude Code name lengths: " + ", ".join("%s %d/%d" % (n[len(PREFIX):], len(n), len(c))
                                                       for n, c in sorted(clients.items())))
check("Claude Code names mcp__xp__<tool> are <= 128 and distinct after normalization",
      all(len(c) <= 128 for c in clients.values()) and len(set(clients.values())) == len(clients), clients)
check("tools/list: every inputSchema has additionalProperties false",
      all(t.get("inputSchema", {}).get("additionalProperties") is False for t in ours.values()),
      [n for n, t in ours.items() if t.get("inputSchema", {}).get("additionalProperties") is not False])


def walk(node, path, found):
    if isinstance(node, dict):
        if "enum" in node:
            found.append((path, node["enum"]))
        if node.get("type") == "object" and "properties" in node and node.get("additionalProperties") is not False:
            found.append((path, "nested object without additionalProperties:false"))
        for k, v in node.items():
            walk(v, path + "/" + k, found)
    elif isinstance(node, list):
        for i, v in enumerate(node):
            walk(v, path + "/" + str(i), found)


bad_enums, open_objects, all_enums = [], [], []
for name, t in ours.items():
    found = []
    walk(t.get("inputSchema", {}), name, found)
    for path, value in found:
        if isinstance(value, str):
            open_objects.append(path)
        else:
            all_enums.append((path, value))
            if set(value) not in STATIC_ENUMS:
                bad_enums.append((path, value))
info("enums seen: " + ", ".join(sorted({"|".join(v) for _, v in all_enums})))
check("tools/list: no enum derived from live data (only branch/direction/workflow)", not bad_enums, bad_enums)
check("tools/list: nested object schemas closed (additionalProperties false)", not open_objects, open_objects)

# 2. one successful call per tool
GOOD = [
    ("list-projects", {}),
    ("list-types", {}),
    ("search", {"types": ["article"], "count": 3}),
    ("get", {"key": "/articles/article-01"}),
    ("describe-type", {"type": "com.example.mcpdataset:article"}),
    ("describe-type", {"type": "article"}),
    ("query", {"query": "{guillotine{queryDsl(query:{matchAll:{}},first:3){_id displayName}}}"}),
    ("find-references", {"key": "/landings/landing-deleted-link", "direction": "outbound"}),
    ("check-links", {"count": 5}),
]
describe_len = {}
for tool, arguments in GOOD:
    response, raw_len, own_len = call(tool, arguments)
    check("success: %s %s" % (tool, json.dumps(arguments)), good(response), json.dumps(response)[:500])
    if tool == "describe-type":
        describe_len[arguments["type"]] = (own_len, raw_len)
        sdl = response.get("result", {}).get("structuredContent", {}).get("sdl", "")
        info("describe-type %s: result %d chars (JSON-RPC %d), sdl %d chars"
             % (arguments["type"], own_len, raw_len, len(sdl)))
check("describe-type: full name and short 'article' give the same contentType",
      len(describe_len) == 2 and len(set(v[0] for v in describe_len.values())) == 1, describe_len)

introspection = open(os.path.join(HERE, "introspection.graphql")).read()
response, raw_len, own_len = call("query", {"query": introspection})
if good(response):
    info("full introspection via query tool: result %d chars (JSON-RPC %d)" % (own_len, raw_len))
else:
    text = response.get("result", {}).get("content", [{}])[0].get("text", "")
    info("full introspection via query tool refused: " + text)
    raw, body = post(GQL_URL, {"query": introspection}, {"Content-Type": "application/json"})
    check("full introspection via HTTP GraphQL answers", "__schema" in body.get("data", {}), raw[:300])
    info("full introspection via HTTP GraphQL: %d chars, %d types"
         % (len(raw), len(body.get("data", {}).get("__schema", {}).get("types", []))))
    if describe_len:
        own = describe_len.get("com.example.mcpdataset:article", (0, 0))[0]
        info("describe-type is %.1f%% of full introspection (%d / %d)" % (100.0 * own / len(raw), own, len(raw)))

# contract details of single tools
response, _, _ = call("list-projects", {})
s = response.get("result", {}).get("structuredContent", {})
check("list-projects anonymous: caller {anonymous:true}, defaultBranch master, defaultProject mcp-test marked isDefault",
      s.get("caller") == {"anonymous": True} and s.get("defaultBranch") == "master" and s.get("defaultProject") == "mcp-test" and
      [p["name"] for p in s.get("projects", []) if p.get("isDefault")] == ["mcp-test"], s)
response, _, _ = call("describe-type", {"type": "article", "project": "mcp-test"})
text = (response.get("result", {}).get("content") or [{}])[0].get("text", "")
check("describe-type takes no project (schema is the same for all projects): project -> isError naming it",
      response.get("result", {}).get("isError") is True and "project" in text, text or response)
response, _, _ = call("query", {"query": "{guillotine{get(key:\"/no-such-content\"){_id}}}"})
s = response.get("result", {}).get("structuredContent", {})
check("query: null field kept as null (get of a missing key)",
      good(response) and "get" in s.get("data", {}).get("guillotine", {}) and s["data"]["guillotine"]["get"] is None, s)

SCOPE_ESCAPES = [
    ("literal project", {"query": "{guillotine(project:\"other\"){get(key:\"/\"){_id}}}"}, "Pass project and branch as tool arguments"),
    ("project via variable", {"query": "query($p:String){guillotine(project:$p){get(key:\"/\"){_id}}}", "variables": {"p": "other"}},
     "Pass project and branch as tool arguments"),
    ("branch inside a fragment", {"query": "query{...F} fragment F on Query{guillotine(branch:\"draft\"){get(key:\"/\"){_id}}}"},
     "Pass project and branch as tool arguments"),
    ("first 1000 via variable", {"query": "query($n:Int){guillotine{queryDsl(query:{matchAll:{}},first:$n){_id}}}", "variables": {"n": 1000}},
     "first must not exceed 100"),
]
for label, arguments, message in SCOPE_ESCAPES:
    response, _, _ = call("query", arguments)
    text = (response.get("result", {}).get("content") or [{}])[0].get("text", "")
    check("query rejects %s -> isError %r" % (label, message),
          response.get("result", {}).get("isError") is True and text.startswith(message), text or response)

# 3. erroneous calls
BAD = {
    "list-projects": [{"project": "x"}, {"count": 5}],
    "list-types": [{"project": "no-such-project"}, {"branch": "live"}, {"branch": "draft"}],
    "search": [{"count": 500}, {"types": ["article"], "cursor": "%%%not-a-cursor"},
               {"filters": [{"field": "modifiedTime", "from": "2026-13-45"}]}, {"mine": True}],
    "get": [{}, {"key": "/articles/no-such-article"}, {"key": "/articles/article-01", "fields": "title"}],
    "describe-type": [{"type": "nope"}, {}, {"type": "article", "project": "no-such-project"}],
    "query": [{"query": "{ bogus }"}, {"query": "mutation { x }"}, {"query": "{guillotine{get(key:\"/x\"){_id}}}",
                                                                       "branch": "preview"},
              {"query": introspection}],
    "find-references": [{}, {"key": "/landings/landing-deleted-link", "direction": "sideways"},
                        {"key": "/images/img-01.png", "unusedMedia": True}],
    "check-links": [{"branch": "draft"}, {"count": -1}, {"path": "/no/such/path"}],
}
hint_words = {t: set(ours[PREFIX + t]["inputSchema"].get("properties", {})) for t in TOOLS}
tool_words = set(TOOLS) | {PREFIX + t for t in TOOLS}
for tool, cases in BAD.items():
    errors = 0
    for arguments in cases:
        response, _, _ = call(tool, arguments)
        label = "%s %s" % (tool, json.dumps(arguments)[:80])
        result = response.get("result")
        if result is None:
            check("error: %s -> tool result, not JSON-RPC error" % label, False, json.dumps(response)[:400])
            continue
        if not result.get("isError"):
            info("not an error (accepted): %s -> %s" % (label, json.dumps(result.get("structuredContent"))[:200]))
            continue
        errors += 1
        text = (result.get("content") or [{}])[0].get("text", "")
        tokens = re.findall(r"[A-Za-z_]+(?:-[A-Za-z_]+)*", text)
        words = set(tokens) | {part for token in tokens for part in token.split("-")}
        # A schema-validation text that names the offending argument counts as a fix (remove/rename it).
        suggests = sorted(words & (hint_words[tool] | set(arguments) | tool_words))
        java = JAVA_NAME.search(text)
        check("error: %s -> isError, readable, no Java names, names a field/tool" % label,
              bool(text) and not text.lstrip().startswith("{") and not java and bool(suggests),
              "text=%r java=%s suggests=%s" % (text, java.group(0) if java else None, suggests))
        info("text: " + text[:220])
    check("error: %s has >= 2 isError cases" % tool, errors >= 2, errors)

# 4. pagination to the end
for tool, arguments, items_key in (("search", {"count": 10}, "hits"),
                                   ("find-references", {"unusedMedia": True, "count": 1}, "unused")):
    ids, pages, cursor, total = [], 0, None, None
    while pages < 100:
        args = dict(arguments, **({"cursor": cursor} if cursor else {}))
        response, _, _ = call(tool, args)
        s = response.get("result", {}).get("structuredContent", {})
        total = s.get("total", total)
        ids += [i["_id"] for i in s.get(items_key, [])]
        pages += 1
        cursor = s.get("nextCursor")
        if not cursor:
            break
    info("%s pagination: %d pages, %d ids, %d distinct, total %s" % (tool, pages, len(ids), len(set(ids)), total))
    check("pagination to the end: %s %s -> distinct ids == total" % (tool, json.dumps(arguments)),
          total is not None and len(ids) == len(set(ids)) == total, (len(ids), len(set(ids)), total))

# 5. truncation
response, raw_len, own_len = call("search", {"count": 50})
s = response.get("result", {}).get("structuredContent", {})
info("truncation: search count 50 -> %d hits of %s, truncated=%s, result %d chars, JSON-RPC %d chars"
     % (len(s.get("hits", [])), s.get("total"), s.get("truncated"), own_len, raw_len))
info("hint: " + str(s.get("hint")))
check("search count 50: every hit up to the total, or truncated:true + hint + nextCursor",
      len(s.get("hits", [])) == min(50, s.get("total", 0)) or
      (s.get("truncated") is True and bool(s.get("hint")) and bool(s.get("nextCursor"))))
check("truncation hint names only search arguments", "fields" not in str(s.get("hint", "")), s.get("hint"))
check("search count 50: structuredContent <= %d and JSON-RPC response <= 3.25 x %d + %d envelope"
      % (MAX_RESULT_CHARS, MAX_RESULT_CHARS, ENVELOPE_ALLOWANCE),
      own_len <= MAX_RESULT_CHARS and raw_len <= MAX_RESULT_CHARS * 13 // 4 + ENVELOPE_ALLOWANCE, (own_len, raw_len))

print("tools-all: %d failure(s)" % failures)
sys.exit(1 if failures else 0)
