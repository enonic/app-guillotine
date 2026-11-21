"""Shared client for the suites: MCP calls to the local XP, anonymous or with the session cookie in MCP_COOKIE, and PASS/FAIL output."""
import json
import os
import re
import sys
import urllib.error
import urllib.request

E2E_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
XP_URL = os.environ.get("XP_URL", "http://localhost:8080")
MCP_URL = os.environ.get("MCP_URL", XP_URL + "/api/system:mcp")
GQL_URL = XP_URL + "/api/com.enonic.app.guillotine:graphql"
COOKIE = os.environ.get("MCP_COOKIE", "")
SIGNED_IN = bool(COOKIE)
CALLER = os.environ.get("MCP_CALLER") or ("signed-in" if SIGNED_IN else "anonymous")
PREFIX = "com.enonic.app.guillotine__"
GROUPS = {
    "content": ["list-projects", "list-types", "describe-type", "search", "get"],
    "graphql": ["query"],
    "references": ["find-unreferenced", "find-broken-references"],
}
MAX_RESULT_CHARS = int(os.environ.get("MCP_MAX_RESULT_CHARS", "20000"))
JAVA_NAME = re.compile(r"\b[a-z]+(\.[a-z0-9_]+)+\.[A-Z]\w+|Exception\b|\bat [a-z]+\.")

failures = 0
sizes = []


def load_expected():
    with open(os.path.join(E2E_DIR, "fixtures/expected.json")) as f:
        return json.load(f)


def check(name, ok, detail=""):
    global failures
    print(("PASS  " if ok else "FAIL  ") + "[" + CALLER + "] " + name +
          ("" if ok or detail == "" else "  -- " + str(detail)[:700]))
    if not ok:
        failures += 1
    return ok


def info(line):
    print("      " + line)


def finish(suite):
    print("%s [%s]: %d failure(s)" % (suite, CALLER, failures))
    sys.exit(1 if failures else 0)


def rpc(method, params, name="", cookie=None):
    """One JSON-RPC request to the MCP endpoint; returns (raw text, parsed JSON or None). HTTP errors return their body too."""
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
    cookie = COOKIE if cookie is None else cookie
    if cookie:
        headers["Cookie"] = cookie
    body = json.dumps({"jsonrpc": "2.0", "id": 1, "method": method, "params": params}).encode()
    try:
        with urllib.request.urlopen(urllib.request.Request(MCP_URL, data=body, headers=headers)) as response:
            raw = response.read().decode()
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
    try:
        return raw, json.loads(raw)
    except ValueError:
        return raw, None


def tools_list():
    return {t["name"]: t for t in rpc("tools/list", {})[1]["result"]["tools"]}


def call(group, tool, arguments, cookie=None):
    """Calls a Guillotine tool and returns its result object; a JSON-RPC error comes back as {"rpcError": ...}. Records the size of
    structuredContent (or of the error result) for the size check."""
    name = PREFIX + group + "__" + tool
    raw, response = rpc("tools/call", {"name": name, "arguments": arguments}, name, cookie)
    if not response or "result" not in response:
        return {"rpcError": (response or {}).get("error", raw[:300])}
    result = response["result"]
    measured = result.get("structuredContent", {k: result[k] for k in ("content", "isError") if k in result})
    sizes.append((tool, len(json.dumps(measured, separators=(",", ":"), ensure_ascii=False)), len(raw)))
    return result


def ok(result):
    return "structuredContent" in result and not result.get("isError")


def sc(result):
    return result.get("structuredContent", {}) if ok(result) else {}


def error_text(result):
    if result.get("isError") and result.get("content"):
        return result["content"][0].get("text", "")
    return ""


def show(result):
    """Short form of a result for failure details."""
    return error_text(result) or json.dumps(result.get("structuredContent", result), ensure_ascii=False)[:500]


def readable_error(result, *words):
    """isError with a plain-language text: no JSON, no Java class names or stack frames, and every given word in it."""
    text = error_text(result)
    return (result.get("isError") is True and bool(text) and not text.lstrip().startswith("{") and not JAVA_NAME.search(text)
            and all(w in text for w in words))


def walk(group, tool, arguments, list_key, max_pages=50):
    """Follows nextCursor to the end; returns (items, pages, structured results, error text)."""
    items, cursor, pages, results = [], None, 0, []
    while pages < max_pages:
        args = dict(arguments)
        if cursor:
            args["cursor"] = cursor
        r = call(group, tool, args)
        if not ok(r):
            return items, pages, results, show(r)
        s = r["structuredContent"]
        results.append(s)
        items += s.get(list_key, [])
        pages += 1
        cursor = s.get("nextCursor")
        if not cursor:
            break
    return items, pages, results, ""


def check_sizes():
    if not sizes:
        return
    biggest = max(sizes, key=lambda item: item[1])
    check("every result <= %d chars (%d calls; largest %s: %d, whole response %d)"
          % (MAX_RESULT_CHARS, len(sizes), biggest[0], biggest[1], biggest[2]),
          all(size <= MAX_RESULT_CHARS for _, size, _ in sizes))


def gql(query, variables=None, cookie=None):
    """Guillotine's HTTP GraphQL API, outside MCP."""
    headers = {"Content-Type": "application/json"}
    if cookie:
        headers["Cookie"] = cookie
    request = urllib.request.Request(GQL_URL, json.dumps({"query": query, "variables": variables or {}}).encode(), headers)
    with urllib.request.urlopen(request) as response:
        return json.load(response)


def leaf(path):
    return path.rsplit("/", 1)[-1]
