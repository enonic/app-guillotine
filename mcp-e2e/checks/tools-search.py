#!/usr/bin/env python3
"""Checks com.enonic.app.guillotine__content__{list-types,search,get} on a running local XP, anonymous caller (master)."""
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


# tools/list
tools = {t["name"]: t for t in rpc("tools/list", {})["result"]["tools"]}
for tool in ("list-types", "search", "get"):
    t = tools.get(PREFIX + tool, {})
    check("tools/list has " + PREFIX + tool + " (read-only annotations)",
          t.get("annotations") == {"readOnlyHint": True, "destructiveHint": False, "idempotentHint": True,
                                   "openWorldHint": False}, t.get("annotations"))
lt_out = tools.get(PREFIX + "list-types", {}).get("outputSchema", {})
check("outputSchema: closed on list-types (top level and type items), none on search/get",
      lt_out.get("additionalProperties") is False and
      lt_out.get("properties", {}).get("types", {}).get("items", {}).get("additionalProperties") is False and
      set(lt_out.get("required", [])) == {"project", "branch", "types"} and
      "outputSchema" not in tools.get(PREFIX + "search", {}) and "outputSchema" not in tools.get(PREFIX + "get", {}),
      lt_out)

# Q5: content types and counts
r = call("list-types", {})
counts = {t["name"]: t["count"] for t in r.get("structuredContent", {}).get("types", [])}
check("Q5 list-types counts == expected countPerType.master", ok(r) and counts == expected["countPerType"]["master"],
      counts)
check("list-types: project/branch resolved (mcp-test, master)",
      r.get("structuredContent", {}).get("project") == "mcp-test" and r["structuredContent"].get("branch") == "master")
check("list-types: displayName from content type schema",
      any(t.get("displayName") == "Article" for t in r.get("structuredContent", {}).get("types", [])))

# Q1: articles per author in ONE search call
r = call("search", {"types": ["article"], "aggregate": [{"by": "data.author"}], "count": 0})
s = r.get("structuredContent", {})
buckets = s.get("aggregations", [{}])[0].get("buckets", []) if ok(r) else []
got = sorted((b.get("label", {}).get("path", "?").rsplit("/", 1)[-1], b["count"]) for b in buckets)
want = sorted((a["author"], a["count"]) for a in expected["articlesPerAuthor"]["master"])
check("Q1 articles per author (one search, count 0) == expected.articlesPerAuthor.master", got == want, (got, want))
check("Q1 total == 19 and no hits/nextCursor with count 0",
      s.get("total") == 19 and "hits" not in s and "nextCursor" not in s, s.get("total"))
check("Q1 bucket labels carry displayName",
      sorted(b.get("label", {}).get("displayName") for b in buckets) ==
      sorted(a["displayName"] for a in expected["articlesPerAuthor"]["master"]))

# path subtree + full type name; date filter (ISO date -> instant)
r = call("search", {"path": "/articles", "types": ["com.example.mcpdataset:article"], "count": 0})
check("path /articles subtree (folder itself excluded) == 19 articles", r.get("structuredContent", {}).get("total") == 19,
      r.get("structuredContent") or error_text(r))
r = call("search", {"filters": [{"field": "modifiedTime", "from": "2020-01-01"}], "count": 0})
check("filters: modifiedTime from 2020-01-01 matches all 39 items", r.get("structuredContent", {}).get("total") == 39,
      r.get("structuredContent") or error_text(r))
r = call("search", {"text": "Lovelace", "count": 5})
check("text: fulltext finds Ada Lovelace", ok(r) and any(
    h["_path"] == "/persons/ada-lovelace" for h in r["structuredContent"].get("hits", [])), r.get("structuredContent"))

# pagination to the end
ids, cursor, pages, total = [], None, 0, None
while pages < 20:
    arguments = {"count": 7}
    if cursor:
        arguments["cursor"] = cursor
    r = call("search", arguments)
    if not ok(r):
        check("pagination page " + str(pages + 1), False, error_text(r))
        break
    s = r["structuredContent"]
    total = s["total"]
    ids += [h["_id"] for h in s.get("hits", [])]
    pages += 1
    cursor = s.get("nextCursor")
    if not cursor:
        break
check("pagination count 7: %d pages, %d ids, %d distinct, total %s" % (pages, len(ids), len(set(ids)), total),
      total == expected["totalItems"]["master"] and len(ids) == total and len(set(ids)) == total and pages == -(-total // 7))
check("hits: compact summary fields only",
      all(set(h) <= {"_id", "_path", "displayName", "type", "modifiedTime", "modifier", "owner", "workflow", "publish"}
          for h in r.get("structuredContent", {}).get("hits", [])))

# cursors
first = call("search", {"types": ["article"], "count": 5})["structuredContent"]
cursor = first["nextCursor"]
r = call("search", {"types": ["person"], "count": 5, "cursor": cursor})
check("cursor reused with changed filters -> isError with hint",
      r.get("isError") is True and "does not match this query" in error_text(r) and "without cursor" in error_text(r),
      error_text(r) or r)
r = call("search", {"types": ["article"], "count": 5, "cursor": "eyJvZmZzZXQiOjUsInF1ZXJ5SGFzaCI6IjAwMDAwMDAwMDAwMDAwMDAifQ"})
check("foreign cursor -> isError with hint", r.get("isError") is True and "cursor" in error_text(r), error_text(r) or r)
r = call("search", {"types": ["article"], "count": 5, "cursor": "%%%not-a-cursor"})
check("garbage cursor -> isError", r.get("isError") is True and "Repeat the call without cursor" in error_text(r), error_text(r))
r = call("search", {"types": ["article"], "count": 5, "cursor": cursor})
check("same cursor with same arguments -> next page",
      ok(r) and not set(h["_id"] for h in r["structuredContent"]["hits"]) & set(h["_id"] for h in first["hits"]))

# invalid arguments -> isError results, never JSON-RPC errors
for label, arguments, needle in (("bad branch", {"branch": "live"}, ""),
                                 ("count 51", {"count": 51}, ""),
                                 ("unknown sort field", {"sort": "rating DESC"}, "Invalid sort field"),
                                 ("bad sort direction", {"sort": "modifiedTime UP"}, "ASC or DESC"),
                                 ("bad interval", {"aggregate": [{"by": "modifiedTime", "interval": "daily"}]}, "interval")):
    r = call("search", arguments)
    check("invalid args (%s) -> isError result" % label,
          r.get("isError") is True and "rpcError" not in r and needle in error_text(r), error_text(r) or r)

# anonymous restrictions
r = call("search", {"branch": "draft"})
check("anonymous branch draft -> readable error", r.get("isError") is True and "master" in error_text(r) and
      "Access denied" in error_text(r), error_text(r) or r)
r = call("get", {"key": "/articles/article-01", "branch": "draft"})
check("anonymous get on draft -> readable error", r.get("isError") is True and "master" in error_text(r), error_text(r) or r)
r = call("search", {"mine": True})
check("mine:true anonymous -> error mentioning authentication",
      r.get("isError") is True and "authenticated" in error_text(r), error_text(r) or r)

# get: article and landings (rich text)
r = call("get", {"key": "/articles/article-01"})
c = r.get("structuredContent", {}).get("content", {})
data = c.get("data", {})
check("get article by path: metadata + data (title, author, body as text)",
      ok(r) and c.get("_path") == "/articles/article-01" and c.get("type") == "com.example.mcpdataset:article" and
      "title" in data and isinstance(data.get("body"), dict) and "<" not in data["body"].get("text", "<"),
      json.dumps(c)[:500])
r = call("get", {"key": "/articles/article-01", "fields": ["title", "nope"]})
s = r.get("structuredContent", {})
check("get fields projection + missingFields",
      ok(r) and list(s["content"]["data"]) == ["title"] and s.get("missingFields") == ["nope"], s)

r = call("get", {"key": "/landings/landing-deleted-link"})
links = r.get("structuredContent", {}).get("content", {}).get("data", {}).get("body", {}).get("links", [])
print("      landing-deleted-link links: " + json.dumps(links))
check("landing-deleted-link: resolved content link to /articles/article-01",
      any(l["type"] == "content" and l.get("path") == "/articles/article-01" for l in links))
check("landing-deleted-link: deleted target flagged missing",
      any(l["type"] == "content" and l.get("missing") is True for l in links))
check("landing-deleted-link: image resolved to /images/img-08.png",
      any(l["type"] == "image" and l.get("path") == "/images/img-08.png" for l in links))

r = call("get", {"key": "/landings/landing-missing-image"})
links = r.get("structuredContent", {}).get("content", {}).get("data", {}).get("body", {}).get("links", [])
print("      landing-missing-image links: " + json.dumps(links))
check("landing-missing-image: missing image flagged, article-02 link resolved",
      any(l["type"] == "image" and l.get("id") == "00000000-0000-4000-8000-00000000dead" and l.get("missing") for l in links) and
      any(l.get("path") == "/articles/article-02" for l in links))

r = call("get", {"key": "/articles/no-such-article"})
check("get unknown key -> isError naming search", r.get("isError") is True and "search" in error_text(r), error_text(r))

# round 2: discriminating filters, sort, histogram, workflow
def total_of(arguments):
    r = call("search", dict(arguments, count=0))
    return r.get("structuredContent", {}).get("total") if ok(r) else error_text(r)


check("filters: modifiedTime from 2099-01-01 -> 0", total_of({"filters": [{"field": "modifiedTime", "from": "2099-01-01"}]}) == 0)
check("path /persons without types -> 4 persons", total_of({"path": "/persons"}) == 4)
check("path /articles without types -> 19 on master", total_of({"path": "/articles"}) == 19)
check("filters.equals type person -> 4",
      total_of({"filters": [{"field": "type", "equals": "com.example.mcpdataset:person"}]}) == 4)
ada = next(b["key"] for b in buckets if b.get("label", {}).get("path") == "/persons/ada-lovelace")
check("filters.equals data.author = Ada -> 8 on master", total_of({"filters": [{"field": "data.author", "equals": ada}]}) == 8)
check("workflow IN_PROGRESS on master -> 0", total_of({"workflow": "IN_PROGRESS"}) == 0)
check("workflow READY on master -> 39", total_of({"workflow": "READY"}) == 39)

names = [h["displayName"] for h in call("search", {"types": ["article"], "sort": "displayName ASC", "count": 50})
         .get("structuredContent", {}).get("hits", [])]
check("sort displayName ASC orders 19 articles", len(names) == 19 and names == sorted(names), names[:5])
desc = [h["displayName"] for h in call("search", {"types": ["article"], "sort": "displayName DESC", "count": 50})
        .get("structuredContent", {}).get("hits", [])]
check("sort displayName DESC is the reverse", desc == list(reversed(names)))

r = call("search", {"aggregate": [{"by": "modifiedTime", "interval": "1d"}], "count": 0})
hist = r.get("structuredContent", {}).get("aggregations", [{}])[0]
check("dateHistogram by day: bucket counts sum to 39 with ISO keys",
      ok(r) and sum(b["count"] for b in hist.get("buckets", [])) == 39 and
      all(str(b["key"]).startswith("20") for b in hist.get("buckets", [])), hist)

r = call("search", {"aggregate": [{"by": "_allText"}, {"by": "data.title", "interval": "1d"}], "count": 0})
aggs = r.get("structuredContent", {}).get("aggregations", [])
check("empty aggregations explain why (not aggregatable / interval needs a date field)",
      len(aggs) == 2 and "cannot be aggregated" in aggs[0].get("note", "") and "date field" in aggs[1].get("note", ""), aggs)

# truncation keeps text consistent with data
r = call("search", {"count": 50})
s = r.get("structuredContent", {})
print("      count 50 alone: %d hits, truncated=%s, result %d chars" % (len(s.get("hits", [])), s.get("truncated"), sizes[-1][1]))
ids, cursor, consistent, pages = [], None, True, 0
while pages < 10:
    arguments = {"count": 50, "aggregate": [{"by": "_path", "size": 100}]}
    if cursor:
        arguments["cursor"] = cursor
    r = call("search", arguments)
    if not ok(r):
        consistent = False
        break
    s = r["structuredContent"]
    hits = s.get("hits", [])
    summary = r["content"][0]["text"].split("\n", 1)[0]
    expected_part = "showing %d-%d of %d" % (len(ids) + 1, len(ids) + len(hits), s["total"])
    if pages == 0:
        print("      page 1: " + summary)
        first_truncated = s.get("truncated") is True and len(hits) < 39 or not s.get("truncated") and len(hits) == 39
    consistent = consistent and expected_part in summary and (("Result truncated" in summary) == bool(s.get("truncated")))
    ids += [h["_id"] for h in hits]
    pages += 1
    cursor = s.get("nextCursor")
    if not cursor:
        break
check("count 50 + aggregate _path size 100: page 1 holds all 39 hits or is truncated", first_truncated)
check("truncated summaries match returned hits on every page (%d pages)" % pages, consistent)
check("nextCursor after truncation continues right after the last hit: %d ids, %d distinct" % (len(ids), len(set(ids))),
      len(ids) == 39 and len(set(ids)) == 39)

# principal fields hidden from anonymous callers
for label, arguments in (("aggregate by modifier", {"aggregate": [{"by": "modifier"}]}),
                         ("filter on modifier", {"filters": [{"field": "modifier", "equals": "user:system:mcp-bob"}]}),
                         ("sort by owner", {"sort": "owner DESC"}),
                         ("filter on _permissions_read", {"filters": [{"field": "_permissions_read", "equals": "role:system.everyone"}]})):
    r = call("search", arguments)
    check("anonymous %s -> isError (holds user keys)" % label,
          r.get("isError") is True and "holds user keys" in error_text(r) and "mcp-bob" not in json.dumps(r), error_text(r) or r)

# keys and dates
article = call("get", {"key": "/articles/article-01"})["structuredContent"]["content"]
r = call("get", {"key": article["_id"].upper()})
check("get by uppercase UUID -> normalized", ok(r) and r["structuredContent"]["content"]["_path"] == "/articles/article-01",
      error_text(r))
r = call("get", {"key": "  /articles/article-01  "})
check("get key with surrounding spaces -> trimmed", ok(r), error_text(r))
for key in ("articles/article-01", "/../x", "/articles//article-01"):
    r = call("get", {"key": key})
    check("get key %r -> hint about paths" % key,
          r.get("isError") is True and "Paths start with / relative to the project root" in error_text(r), error_text(r))
r = call("search", {"filters": [{"field": "modifiedTime", "from": "2026-13-45"}]})
check("invalid date names the argument", error_text(r).startswith('Invalid filters[0].from: "2026-13-45" is not a valid date.'),
      error_text(r))
r = call("get", {"key": "/articles/article-01", "fields": []})
check("get fields [] -> default data", ok(r) and "title" in r["structuredContent"]["content"]["data"], error_text(r))

# empty results explain themselves when the arguments do
EMPTY_NOTES = [
    ({"types": ["nosuchtype"]}, "No installed content type named nosuchtype"),
    ({"path": "/nope"}, "No content at path /nope on master"),
    ({"workflow": "IN_PROGRESS"}, "master holds published content, which is normally not in progress"),
    ({"path": "/articles/article-01"}, "not the item itself; read it with get"),
]
for arguments, expected_note in EMPTY_NOTES:
    r = call("search", arguments)
    s = r.get("structuredContent", {})
    check("empty search %s -> total 0 with note %r" % (json.dumps(arguments), expected_note),
          ok(r) and s.get("total") == 0 and s.get("hits") == [] and expected_note in s.get("note", ""), error_text(r) or s)
r = call("search", {"text": "zzzqqqnomatch"})
check("empty search on plain text -> no note (nothing in the arguments explains it)",
      ok(r) and r["structuredContent"].get("total") == 0 and "note" not in r["structuredContent"], error_text(r) or r)

# sizes
biggest = max(sizes, key=lambda item: item[1])
check("every tool result <= %d chars (%d calls; largest %s: %d, with JSON-RPC envelope %d)" %
      (MAX_RESULT_CHARS, len(sizes), biggest[0], biggest[1], biggest[2]),
      all(size <= MAX_RESULT_CHARS for _, size, _ in sizes))

sys.exit(1 if failures else 0)
