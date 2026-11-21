#!/usr/bin/env python3
"""Every tool the caller may use: one successful call, at least two failing calls with a readable hint, the result envelope, paging
and cursors. The references tools run only for a signed-in caller; access.py covers what anonymous callers get."""
import base64
import json
import os
import sys

sys.dont_write_bytecode = True  # keep __pycache__ out of the source tree
from xpmcp import (CALLER, E2E_DIR, SIGNED_IN, call, check, check_sizes, error_text, finish, info, load_expected, ok,  # noqa: E402
                   readable_error, sc, show, walk)

expected = load_expected()
BRANCH = "draft" if SIGNED_IN else "master"
TOTAL = expected["totalItems"][BRANCH]


def cursor_at(offset):
    return base64.urlsafe_b64encode(("offset:%d" % offset).encode()).decode().rstrip("=")


def nulls_in_arrays(node):
    if isinstance(node, list):
        return any(item is None or nulls_in_arrays(item) for item in node)
    if isinstance(node, dict):
        return any(nulls_in_arrays(v) for v in node.values())
    return False


def envelope_ok(result):
    """Success envelope: one text content whose JSON equals structuredContent, and no null inside arrays."""
    content = result.get("content") or [{}]
    try:
        same = json.loads(content[0].get("text", "")) == result["structuredContent"]
    except ValueError:
        same = False
    return len(content) == 1 and same and not nulls_in_arrays(result["structuredContent"])


# One success per tool
SUCCESS = [
    ("content", "list-projects", {}, lambda s: s.get("defaultProject") == "mcp-test" and s.get("defaultBranch") == BRANCH and
     s.get("caller", {}).get("anonymous") is (not SIGNED_IN) and
     [p.get("itemCount") for p in s.get("projects", []) if p["name"] == "mcp-test"] == [TOTAL]),
    ("content", "list-types", {}, lambda s: s.get("branch") == BRANCH and s.get("total") == TOTAL and
     any(t["name"] == "com.example.mcpdataset:article" and t.get("title") == "Article" and
         t.get("graphqlType") == "com_example_mcpdataset_Article" for t in s.get("types", []))),
    ("content", "describe-type", {"type": "article"}, lambda s: s.get("contentType") == "com.example.mcpdataset:article" and
     any(f["name"] == "author" and f["path"] == "data.author" and f.get("targets") for f in s.get("fields", [])) and
     len(s.get("examples", [])) == 3 and "sdl" in s),
    ("content", "search", {"types": ["article"], "fields": ["title", "author"], "count": 3},
     lambda s: len(s.get("hits", [])) == 3 and all("title" in h.get("data", {}) and "_path" in h["data"].get("author", {})
                                                    for h in s["hits"])),
    ("content", "search", {"types": ["article"], "fields": ["displayName", "_path", "publish", "title", "creator"], "count": 3},
     lambda s: len(s.get("hits", [])) == 3 and all("title" in h.get("data", {}) for h in s["hits"]) and
     any(h.get("publish", {}).get("from") for h in s["hits"]) and "creator" in s.get("note", "")),
    ("graphql", "query", {"query": "{ guillotine { queryDsl(query: {matchAll: {}}, first: 2) { _path creator { value } } } }"},
     lambda s: len(s.get("errors", [])) == 2 and all("Content Studio" in e["message"] for e in s["errors"]) and
     "search with mine" in s.get("note", "")),
    ("content", "get", {"key": "/articles/article-01"}, lambda s: s.get("_path") == "/articles/article-01" and
     s.get("type") == "com.example.mcpdataset:article" and isinstance(s.get("data", {}).get("body"), dict) and
     "<" not in s["data"]["body"].get("text", "<")),
    ("graphql", "query", {"query": "{ guillotine { queryDsl(query: {matchAll: {}}, first: 3) { _path displayName } } }"},
     lambda s: s.get("branch") == BRANCH and len(s.get("data", {}).get("guillotine", {}).get("queryDsl", [])) == 3),
]
if SIGNED_IN:
    SUCCESS += [
        ("references", "find-unreferenced", {}, lambda s: sorted(i["_path"] for i in s.get("items", [])) ==
         expected["unusedImages"]["draft"] and s.get("checked") == s.get("candidates") and "incomplete" not in s),
        ("references", "find-broken-references", {"path": "/landings"},
         lambda s: s.get("total") == 3 and len(s.get("items", [])) == 2 and "URL" in s.get("note", "")),
    ]
for group, tool, arguments, verify in SUCCESS:
    r = call(group, tool, arguments)
    check("success: %s %s" % (tool, json.dumps(arguments)), ok(r) and verify(r["structuredContent"]), show(r))
    check("  envelope: text equals structuredContent, no null in arrays", ok(r) and envelope_ok(r))

# Failures: an isError result with a plain text that names what to change
FAILURES = [
    ("content", "list-projects", {"project": "mcp-test"}, ["project"]),
    ("content", "list-projects", {"count": 5}, ["count"]),
    ("content", "list-types", {"project": "no-such-project"}, ["no-such-project", "list-projects"]),
    ("content", "list-types", {"branch": "live"}, ["branch"]),
    ("content", "describe-type", {"type": "nope"}, ["nope", "list-types"]),
    ("content", "describe-type", {}, ["type"]),
    ("content", "describe-type", {"type": "article", "project": "no-such-project"}, ["no-such-project"]),
    ("content", "search", {"count": 500}, ["count"]),
    ("content", "search", {"cursor": "%%%not-a-cursor"}, ["cursor", "without cursor"]),
    ("content", "search", {"sort": "rating DESC"}, ["rating", "ASC|DESC"]),
    ("content", "search", {"filters": [{"field": "modifiedTime", "from": "2026-13-45"}]}, ["filters[0].from", "ISO 8601"]),
    ("content", "search", {"filters": [{"field": "data.title", "equals": "x", "exists": True}]}, ["exactly one of"]),
    ("content", "search", {"filters": [{"references": "/articles/no-such"}]}, ["references", "search"]),
    ("content", "get", {}, ["key"]),
    ("content", "get", {"key": "/articles/no-such-article"}, ["/articles/no-such-article", "search"]),
    ("content", "get", {"key": "articles/article-01"}, ["relative path", "starts with /"]),
    ("graphql", "query", {"query": "{ bogus }"}, ["bogus"]),
    ("graphql", "query", {"query": "mutation { x }"}, ["mutation", "read-only"]),
    ("graphql", "query", {"query": "{ guillotine { queryDsl(query: {matchAll: {}}, first: 1) { modifier } } }"},
     ["PrincipalKey has value, type, idProvider, principalId", "search with mine"]),
    ("graphql", "query", {"query": "{ guillotine { queryDsl(query: {matchAll: {}}, first: 1) { data } } }"},
     ["Content has", "... on <graphqlType from list-types>"]),
    ("graphql", "query", {"query": "{ guillotine(project: \"other\") { get(key: \"/\") { _id } } }"},
     ["Pass project and branch as tool arguments"]),
    ("graphql", "query", {"query": "query($n: Int) { guillotine { queryDsl(query: {matchAll: {}}, first: $n) { _id } } }",
                          "variables": {"n": 1000}}, ["first must not exceed 100"]),
    ("graphql", "query", {"query": "query($c: String) { guillotine { queryDslConnection(query: {matchAll: {}}, after: $c) "
                                   "{ totalCount } } }", "variables": {"c": "abc"}},
     ["after is not a cursor from a previous result", "endCursor"]),
    ("graphql", "query", {"query": "query A { guillotine { getSite { _id } } } query B { guillotine { getSite { _id } } }"},
     ["operationName", "A", "B"]),
    ("graphql", "query", {"query": open(os.path.join(E2E_DIR, "checks/introspection.graphql")).read()},
     ["depth", "limit of 10 nested levels"]),
]
if SIGNED_IN:
    FAILURES += [
        ("references", "find-unreferenced", {"path": "images"}, ["path", "starts with /"]),
        ("references", "find-unreferenced", {"count": 99}, ["count"]),
        ("references", "find-unreferenced", {"project": "no-such-project"}, ["no-such-project"]),
        ("references", "find-broken-references", {"cursor": "bad"}, ["cursor"]),
        ("references", "find-broken-references", {"path": "/landings", "cursor": cursor_at(100)}, ["past the end"]),
        ("references", "find-broken-references", {"branch": "live"}, ["branch"]),
    ]
errors = {}
for group, tool, arguments, words in FAILURES:
    r = call(group, tool, arguments)
    label = "%s %s" % (tool, json.dumps(arguments)[:90])
    good = readable_error(r, *words)
    check("failure: %s -> isError naming %s" % (label, ", ".join(words)), good, show(r))
    errors[tool] = errors.get(tool, 0) + (1 if good else 0)
for tool, count in errors.items():
    check("failure: %s has at least two readable isError cases" % tool, count >= 2, count)

# valid is a filter field: valid and invalid content add up to every item
counts = [sc(call("content", "search", {"count": 0, "filters": [{"field": "valid", "equals": v}]})).get("total") for v in (True, False)]
check("search filter valid true + valid false == total %d" % TOTAL, None not in counts and sum(counts) == TOTAL, counts)

# Paging and cursors
hits, pages, results, error = walk("content", "search", {"count": 7}, "hits")
ids = [h["_id"] for h in hits]
check("search paging count 7: %d pages, %d ids, all distinct, sum == total %d" % (pages, len(ids), TOTAL),
      not error and len(ids) == len(set(ids)) == TOTAL and pages == -(-TOTAL // 7), error or (pages, len(ids)))
check("search paging: no nextCursor on the last page", not error and "nextCursor" not in results[-1])
first = sc(call("content", "search", {"types": ["article"], "count": 5}))
second = call("content", "search", {"types": ["article"], "count": 5, "cursor": first.get("nextCursor", "")})
check("search cursor continues with the next 5 items",
      ok(second) and not {h["_id"] for h in sc(second)["hits"]} & {h["_id"] for h in first.get("hits", [])} and
      len(sc(second)["hits"]) == 5, show(second))
r = call("content", "search", {"count": 5, "cursor": cursor_at(TOTAL + 5)})
check("search cursor past the end -> isError", readable_error(r, "past the end"), show(r))
r = call("content", "search", {"types": ["article"], "count": 0, "aggregate": [{"by": "data.author"}]})
check("search count 0 -> buckets only, no hits and no nextCursor",
      ok(r) and sc(r).get("hits") == [] and "nextCursor" not in sc(r) and sc(r).get("aggregations"), show(r))
if SIGNED_IN:
    r = call("references", "find-broken-references", {"cursor": cursor_at(40)})
    s = sc(r)
    check("find-broken-references cursor at offset 40 checks items 41-%d" % TOTAL,
          ok(r) and s.get("checked") == TOTAL - 40 and "Checked items 41-%d of %d" % (TOTAL, TOTAL) in s.get("note", "") and
          "nextCursor" not in s, show(r))
    r = call("references", "find-unreferenced", {"count": 1})
    s = sc(r)
    check("find-unreferenced count 1 lists the first by path and says how many there are",
          ok(r) and [i["_path"] for i in s.get("items", [])] == expected["unusedImages"]["draft"][:1] and
          s.get("total") == len(expected["unusedImages"]["draft"]) and "Listed the first 1 of" in s.get("note", ""), show(r))

check_sizes()
info("caller %s, default branch %s" % (CALLER, BRANCH))
finish("tools")
