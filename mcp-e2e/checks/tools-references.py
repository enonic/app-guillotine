#!/usr/bin/env python3
"""Checks com.enonic.app.guillotine__content__{find-references,check-links} on a running local XP, anonymous caller (master)."""
import json
import os
import sys
import urllib.request

sys.dont_write_bytecode = True  # keep __pycache__ out of the source tree
from mcpnames import GUILLOTINE_PREFIX, client_name  # noqa: E402

E2E_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
XP_URL = os.environ.get("XP_URL", "http://localhost:8080")
MCP_URL = os.environ.get("MCP_URL", XP_URL + "/api/system:mcp")
MAX_RESULT_CHARS = 20000
PREFIX = GUILLOTINE_PREFIX

expected = json.load(open(os.path.join(E2E_DIR, "fixtures/expected.json")))
dataset = json.load(open(os.path.join(E2E_DIR, "fixtures/dataset-app/src/main/resources/dataset/dataset.json")))
failures = 0
sizes = []


def check(name, ok, detail=""):
    global failures
    print(("PASS  " if ok else "FAIL  ") + name + ("" if ok or not detail else "  -- " + str(detail)[:600]))
    if not ok:
        failures += 1


def rpc(method, params, name=""):
    params = dict(params)
    params["_meta"] = {
        "io.modelcontextprotocol/protocolVersion": "2026-07-28",
        "io.modelcontextprotocol/clientInfo": {"name": "mcp-e2e", "version": "1.0"},
        "io.modelcontextprotocol/clientCapabilities": {},
    }
    body = json.dumps({"jsonrpc": "2.0", "id": 1, "method": method, "params": params}).encode()
    headers = {"Content-Type": "application/json", "Accept": "application/json, text/event-stream",
               "MCP-Protocol-Version": "2026-07-28", "Mcp-Method": method}
    if name:
        headers["Mcp-Name"] = name
    with urllib.request.urlopen(urllib.request.Request(MCP_URL, data=body, headers=headers)) as response:
        return json.loads(response.read())


def call(tool, arguments):
    """Returns the tool result; records its budgeted size (structuredContent, or the whole error result) and the response size."""
    response = rpc("tools/call", {"name": PREFIX + tool, "arguments": arguments}, PREFIX + tool)
    if "result" not in response:
        return {"rpcError": response.get("error")}
    result = response["result"]
    own = {k: result[k] for k in ("content", "structuredContent", "isError") if k in result}
    measured = result.get("structuredContent", own)
    sizes.append((tool, len(json.dumps(measured, separators=(",", ":"), ensure_ascii=False)), len(json.dumps(response))))
    return result


def ok(result):
    return "structuredContent" in result and not result.get("isError")


def error_text(result):
    return result["content"][0]["text"] if result.get("isError") and result.get("content") else ""


def walk(tool, arguments, list_key, max_pages=30):
    """Follows nextCursor to the end; returns (items, pages, last structured, error)."""
    items, cursor, pages, last = [], None, 0, {}
    while pages < max_pages:
        args = dict(arguments)
        if cursor:
            args["cursor"] = cursor
        r = call(tool, args)
        if not ok(r):
            return items, pages, last, error_text(r) or r
        last = r["structuredContent"]
        items += last.get(list_key, [])
        pages += 1
        cursor = last.get("nextCursor")
        if not cursor:
            break
    return items, pages, last, None


# tools/list
tools = {t["name"]: t for t in rpc("tools/list", {})["result"]["tools"]}
for tool in ("find-references", "check-links"):
    t = tools.get(PREFIX + tool, {})
    check("tools/list has " + PREFIX + tool + " (read-only annotations, no outputSchema)",
          t.get("annotations") == {"readOnlyHint": True, "destructiveHint": False, "idempotentHint": True,
                                   "openWorldHint": False} and "outputSchema" not in t, t.get("annotations"))

# Q3: unused images on master, walking all pages
unused, pages, last, err = walk("find-references", {"unusedMedia": True, "count": 2}, "unused")
got = [u["_path"] for u in unused]
want = expected["unusedImages"]["master"]
check("Q3 unusedMedia (count 2, %d pages) == expected.unusedImages.master" % pages,
      err is None and sorted(got) == sorted(want) and len(got) == len(set(got)), err or (got, want))
check("Q3 total == %d, mediaChecked == %d (master media:image)" % (len(want), expected["countPerType"]["master"]["media:image"]),
      last.get("total") == len(want) and last.get("mediaChecked") == expected["countPerType"]["master"]["media:image"], last)
r = call("find-references", {"unusedMedia": True, "types": ["image"], "count": 50})
check("Q3 with types [image] gives the same set in one page",
      ok(r) and sorted(u["_path"] for u in r["structuredContent"].get("unused", [])) == sorted(want) and
      "nextCursor" not in r["structuredContent"], r.get("structuredContent") or error_text(r))
check("Q3 items carry _id, _path, displayName, type",
      all(set(u) == {"_id", "_path", "displayName", "type"} and u["type"] == "media:image" for u in unused), unused[:2])

# Q6: broken internal links on landings
broken, pages, last, err = walk("check-links", {"path": "/landings", "count": 1}, "broken")
print("      broken on /landings: " + json.dumps(broken))
check("Q6 check-links /landings: %d pages, total %s, no error" % (pages, last.get("total")),
      err is None and last.get("total") == len(dataset["landings"]), err or last)
check("Q6 broken count == %d" % len(expected["brokenLinks"]), len(broken) == len(expected["brokenLinks"]), broken)
for item in expected["brokenLinks"]:
    if "targetId" in item:
        match = [b for b in broken if b.get("source") == item["landing"] and b.get("kind") == item["kind"] and
                 b.get("targetId") == item["targetId"]]
    else:
        # the deleted target's id is random and its path is gone; the link text names it
        name = item["target"].rsplit("/", 1)[-1]
        match = [b for b in broken if b.get("source") == item["landing"] and b.get("kind") == item["kind"] and
                 name in b.get("text", "") and b.get("targetId")]
    check("Q6 broken %s %s -> %s" % (item["landing"], item["kind"], item.get("targetId") or item["target"]),
          len(match) == 1 and match[0].get("field") == "data.body" and match[0].get("reason"), match or broken)
r = call("check-links", {"count": 50})
s = r.get("structuredContent", {})
check("check-links over the whole project finds the same broken links (no other broken targets on master)",
      ok(r) and sorted((b["source"], b["targetId"]) for b in s.get("broken", [])) ==
      sorted((b["source"], b["targetId"]) for b in broken) and s.get("scanned") == s.get("total"), s or error_text(r))

# find-references inbound: an image used by published articles
published_articles = [a for a in dataset["articles"] if a.get("publish")]
image = "img-06.png"
referrers = sorted("/articles/" + a["name"] for a in published_articles
                   if a.get("cover") == image or image in (a.get("bodyImages") or []))
r = call("find-references", {"key": "/images/" + image, "direction": "inbound", "count": 50})
s = r.get("structuredContent", {})
check("inbound /images/%s == published referrers %s" % (image, referrers),
      ok(r) and sorted(i["_path"] for i in s.get("inbound", [])) == referrers and s.get("total") == len(referrers),
      s or error_text(r))
check("inbound target summary is the image", s.get("target", {}).get("_path") == "/images/" + image, s.get("target"))
draft_only = sorted("/articles/" + a["name"] for a in dataset["articles"] if not a.get("publish") and
                    (a.get("cover") == image or image in (a.get("bodyImages") or [])))
check("inbound on master does not list draft-only referrers %s" % draft_only,
      not set(draft_only) & set(i["_path"] for i in s.get("inbound", [])))

# find-references outbound: a landing with a deleted link
r = call("find-references", {"key": "/landings/landing-deleted-link", "direction": "outbound"})
out = r.get("structuredContent", {}).get("outbound", [])
print("      outbound landing-deleted-link: " + json.dumps(out))
check("outbound landing-deleted-link: article-01 resolved",
      any(o.get("_path") == "/articles/article-01" and o.get("kind") == "content" for o in out), out)
check("outbound landing-deleted-link: image img-08 resolved",
      any(o.get("_path") == "/images/img-08.png" and o.get("kind") == "image" for o in out), out)
check("outbound landing-deleted-link: deleted article flagged missing",
      any(o.get("missing") is True and o.get("kind") == "content" and "deleted-article" in o.get("text", "") for o in out), out)
check("outbound only: no inbound/total", "inbound" not in r.get("structuredContent", {}) and
      "total" not in r.get("structuredContent", {}))

r = call("find-references", {"key": "/landings/landing-missing-image"})
s = r.get("structuredContent", {})
check("both on landing-missing-image: nothing references it, missing image flagged",
      ok(r) and s.get("total") == 0 and
      any(o.get("_id") == "00000000-0000-4000-8000-00000000dead" and o.get("missing") for o in s.get("outbound", [])), s)

r = call("find-references", {"key": "00000000-0000-4000-8000-00000000dead", "direction": "inbound"})
s = r.get("structuredContent", {})
check("inbound for a never-existing id: target missing, referrer /landings/landing-missing-image",
      ok(r) and s.get("target", {}).get("missing") is True and
      [i["_path"] for i in s.get("inbound", [])] == ["/landings/landing-missing-image"], s or error_text(r))

# pagination of inbound
author = "/persons/ada-lovelace"
items, pages, last, err = walk("find-references", {"key": author, "direction": "inbound", "count": 3}, "inbound")
want_count = [a["count"] for a in expected["articlesPerAuthor"]["master"] if a["authorPath"] == author][0]
check("inbound pagination for %s: %d items over %d pages, distinct" % (author, len(items), pages),
      err is None and len(items) == want_count == len({i["_id"] for i in items}) and last.get("total") == want_count,
      err or last)

# errors
r = call("find-references", {})
check("missing key without unusedMedia -> isError with hint",
      r.get("isError") is True and "key is required" in error_text(r) and "search" in error_text(r), error_text(r) or r)
r = call("find-references", {"key": "/landings/landing-deleted-link", "direction": "sideways"})
check("bad direction -> isError naming direction", r.get("isError") is True and "rpcError" not in r and
      "direction" in error_text(r), error_text(r) or r)
r = call("find-references", {"key": "/images/img-01.png", "unusedMedia": True})
check("key with unusedMedia -> isError with hint", r.get("isError") is True and "either key or unusedMedia" in error_text(r),
      error_text(r) or r)
first = call("find-references", {"unusedMedia": True, "count": 1}).get("structuredContent", {})
r = call("find-references", {"key": "/images/img-06.png", "direction": "inbound", "cursor": first.get("nextCursor", "x")})
check("foreign cursor (from unusedMedia) on find-references -> isError with hint",
      r.get("isError") is True and "does not match this query" in error_text(r) and "without cursor" in error_text(r),
      error_text(r) or r)
r = call("check-links", {"path": "/articles", "cursor": first.get("nextCursor", "x")})
check("foreign cursor on check-links -> isError with hint",
      r.get("isError") is True and "does not match this query" in error_text(r), error_text(r) or r)
r = call("find-references", {"key": "/articles/no-such-article"})
check("unknown path -> isError naming search", r.get("isError") is True and "search" in error_text(r), error_text(r) or r)
r = call("check-links", {"branch": "draft"})
check("anonymous check-links on draft -> readable error", r.get("isError") is True and "master" in error_text(r),
      error_text(r) or r)

# round 2: forged cursors and inputs that used to be accepted silently
import base64


def forge(cursor, offset):
    raw = json.loads(base64.urlsafe_b64decode(cursor + "=" * (-len(cursor) % 4)))
    raw["offset"] = offset
    return base64.urlsafe_b64encode(json.dumps(raw, separators=(",", ":")).encode()).decode().rstrip("=")


page1 = call("find-references", {"key": author, "direction": "inbound", "count": 3}).get("structuredContent", {})
r = call("find-references", {"key": author, "direction": "inbound", "count": 3,
                             "cursor": forge(page1.get("nextCursor", ""), 2147483647)})
check("forged cursor offset 2147483647 (valid queryHash) on find-references -> isError with hint, not internal error",
      r.get("isError") is True and "at most 10000" in error_text(r) and "Internal error" not in error_text(r), error_text(r) or r)
links1 = call("check-links", {"count": 1}).get("structuredContent", {})
r = call("check-links", {"count": 1, "cursor": forge(links1.get("nextCursor", ""), 2147483647)})
check("forged cursor offset on check-links -> isError with hint, not internal error",
      r.get("isError") is True and "at most 10000" in error_text(r) and "Internal error" not in error_text(r), error_text(r) or r)
for bad in ("/landings/..", "/landings/./x", "landings"):
    r = call("check-links", {"path": bad})
    check("check-links path %r -> isError Invalid path" % bad, r.get("isError") is True and "Invalid path" in error_text(r),
          error_text(r) or r.get("structuredContent"))
r = call("check-links", {"path": "/no/such/path"})
check("check-links unknown path -> isError 'not found or not readable' naming search",
      r.get("isError") is True and "not found or not readable" in error_text(r) and "search" in error_text(r),
      error_text(r) or r.get("structuredContent"))
r = call("check-links", {"path": "/landings/"})
check("check-links trailing slash path still works", ok(r) and r["structuredContent"].get("total") == len(dataset["landings"]),
      error_text(r) or r.get("structuredContent"))
r = call("find-references", {"unusedMedia": True, "types": ["article"]})
check("unusedMedia with types [article] -> isError naming media types",
      r.get("isError") is True and "media types like media:image" in error_text(r), error_text(r) or r.get("structuredContent"))
r = call("find-references", {"key": "/images/../persons"})
check("find-references key with .. -> isError", r.get("isError") is True and "Invalid key" in error_text(r), error_text(r) or r)
r = call("query", {"query": "{ bogus }"})
check("query { bogus } -> isError with next step (guillotine root, describe-type)",
      r.get("isError") is True and "guillotine { ... }" in error_text(r) and "describe-type" in error_text(r), error_text(r) or r)
r = call("check-links", {"path": "/landings"})
s = r.get("structuredContent", {})
check("check-links: brokenOnPage == len(broken), complete without nextCursor, and summary count matches",
      ok(r) and s.get("brokenOnPage") == len(s.get("broken", [])) and s.get("complete") is ("nextCursor" not in s) and
      (": %d broken link" % len(s.get("broken", []))) in r["content"][0]["text"], s)

r = call("check-links", {})
s = r.get("structuredContent", {})
check("check-links default count (50) scans the whole project in one page: complete, no nextCursor, scanned == total",
      ok(r) and s.get("complete") is True and "nextCursor" not in s and s.get("scanned") == s.get("total") and
      s.get("brokenOnPage") == 2, s if not ok(r) else {k: v for k, v in s.items() if k != "broken"})
r = call("check-links", {"count": 5})
s = r.get("structuredContent", {})
check("check-links count 5: first page not complete and has nextCursor",
      ok(r) and s.get("complete") is False and "nextCursor" in s and s.get("scanned") == 5,
      s if not ok(r) else {k: v for k, v in s.items() if k != "broken"})

largest = max(sizes, key=lambda item: item[1])
check("every tool result <= %d chars (%d calls; largest %s: %d, with JSON-RPC envelope %d)" %
      (MAX_RESULT_CHARS, len(sizes), largest[0], largest[1], largest[2]),
      all(size <= MAX_RESULT_CHARS for _, size, _ in sizes))

print("%d failure(s)" % failures)
sys.exit(1 if failures else 0)
