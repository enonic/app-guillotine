#!/usr/bin/env python3
"""Every tool the caller may use: one successful call, at least two failing calls with a readable hint, the result envelope, paging
and cursors. The references tools run only for a signed-in caller; access.py covers what anonymous callers get."""
import base64
import json
import os
import sys

sys.dont_write_bytecode = True  # keep __pycache__ out of the source tree
from xpmcp import (CALLER, E2E_DIR, MAX_RESULT_CHARS, SIGNED_IN, call, check, check_sizes, error_text, finish, info,  # noqa: E402
                   load_expected, ok, readable_error, sc, show)

expected = load_expected()
BRANCH = "draft" if SIGNED_IN else "master"
# what a denied creator/modifier/owner field points to instead; anonymous callers cannot filter or aggregate on them either
PRINCIPAL_ADVICE = "caller.key from list-projects" if SIGNED_IN else "need a signed-in caller too"
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
    ("content", "describe-type", {}, lambda s: s.get("branch") == BRANCH and s.get("total") == TOTAL and
     any(t["name"] == "com.example.mcpdataset:article" and t.get("title") == "Article" and
         t.get("graphqlType") == "com_example_mcpdataset_Article" for t in s.get("types", [])) and
     "describe-type again" in s.get("note", "")),
    ("content", "describe-type", {"type": "article"}, lambda s: s.get("contentType") == "com.example.mcpdataset:article" and
     any(f["name"] == "author" and f["path"] == "data.author" and f.get("targets") for f in s.get("fields", [])) and
     len(s.get("examples", [])) == 3 and "sdl" in s),
    ("graphql", "query", {"query": "{ guillotine { queryDsl(query: {matchAll: {}}, first: 2) { _path creator { value } } } }"},
     lambda s: len(s.get("errors", [])) == 2 and all("Content Studio" in e["message"] for e in s["errors"]) and
     PRINCIPAL_ADVICE in s.get("note", "")),
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
    ("content", "describe-type", {"type": "nope"}, ["nope", "describe-type without type"]),
    ("content", "describe-type", {"type": 3}, ["type"]),
    ("content", "describe-type", {"type": "article", "project": "no-such-project"}, ["no-such-project", "list-projects"]),
    ("content", "describe-type", {"branch": "live"}, ["branch"]),
    ("graphql", "query", {"query": "{ bogus }"}, ["bogus"]),
    ("graphql", "query", {"query": "mutation { x }"}, ["mutation", "read-only"]),
    ("graphql", "query", {"query": "{ guillotine { queryDsl(query: {matchAll: {}}, first: 1) { modifier } } }"},
     ["PrincipalKey has value, type, idProvider, principalId", PRINCIPAL_ADVICE]),
    ("graphql", "query", {"query": "{ guillotine { queryDsl(query: {matchAll: {}}, first: 1) { data } } }"},
     ["Content has", "... on <graphqlType from describe-type>"]),
    ("graphql", "query", {"query": "{ guillotine { queryDsl(query: {in: {field: \"type\", values: [{string: \"x\"}]}}) { _id } } }"},
     ["stringValues: [\"a\", \"b\"]"]),
    ("graphql", "query", {"query": "{ guillotine { queryDsl(query: {matchAll: {}}, first: 1) { totalCount } } }"},
     ["fields of queryDslConnection, not queryDsl"]),
    ("graphql", "query", {"query": "{ guillotine { queryDsl(query: {matchAll: {}}, first: 1) { _references } } }"},
     ["_references is content", "_references { _id _path displayName }"]),
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

# valid is an index field, as find-broken-references tells the model: valid and invalid content add up to every item
VALID = "{ guillotine { queryDslConnection(query: {term: {field: \"valid\", value: {boolean: %s}}}, first: 0) { totalCount } } }"
counts = [(sc(call("graphql", "query", {"query": VALID % v})).get("data") or {}).get("guillotine", {}).get("queryDslConnection", {})
          .get("totalCount") for v in ("true", "false")]
check("query term valid true + valid false == total %d" % TOTAL, None not in counts and sum(counts) == TOTAL, counts)

# Paging and cursors
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

# describe-type on every dataset type and branch the caller reads; each example query runs as given through the query tool
BRANCHES = ["draft", "master"] if SIGNED_IN else ["master"]
for type_name in ("article", "person", "landing"):
    for branch in BRANCHES:
        r = call("content", "describe-type", {"type": type_name, "branch": branch})
        s = sc(r)
        examples = s.get("examples", [])
        check("describe-type %s on %s: 3 examples" % (type_name, branch),
              ok(r) and s.get("contentType") == "com.example.mcpdataset:" + type_name and s.get("branch") == branch and
              len(examples) == 3 and not s.get("truncated"), show(r))
        for i, example in enumerate(examples):
            q = call("graphql", "query", {"query": example["query"], "branch": branch})
            qs = sc(q)
            size = len(json.dumps(qs, separators=(",", ":"), ensure_ascii=False))
            check("  example %d (%s) on %s: no errors, %d <= %d chars, not truncated"
                  % (i + 1, example["use"][:40], branch, size, MAX_RESULT_CHARS),
                  ok(q) and qs.get("branch") == branch and "errors" not in qs and bool(qs.get("data")) and
                  size <= MAX_RESULT_CHARS and not qs.get("truncated"), show(q))
            if i == 0:
                total = qs.get("data", {}).get("guillotine", {}).get("queryDslConnection", {}).get("totalCount")
                check("  example 1 totalCount %s == describe-type count %s" % (total, s.get("count")),
                      total is not None and total == s.get("count"))

check_sizes()
info("caller %s, default branch %s" % (CALLER, BRANCH))
finish("tools")
