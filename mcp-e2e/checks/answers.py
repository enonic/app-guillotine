#!/usr/bin/env python3
"""The six stakeholder questions answered through the tools as docs/mcp.adoc describes them, and compared with fixtures/expected.json.
A signed-in caller is checked on draft and master; an anonymous caller on master, where questions that need sign-in must be refused
with a reason."""
import json
import sys

sys.dont_write_bytecode = True  # keep __pycache__ out of the source tree
from xpmcp import SIGNED_IN, call, check, check_sizes, finish, leaf, load_expected, ok, readable_error, sc, show  # noqa: E402

expected = load_expected()
CALLER_KEY = sc(call("content", "list-projects", {})).get("caller", {}).get("key", "")
USER = CALLER_KEY.rsplit(":", 1)[-1]
BRANCHES = ["draft", "master"] if SIGNED_IN else ["master"]
ARTICLE = "com.example.mcpdataset:article"


def query(document, branch, **variables):
    return call("graphql", "query", {"query": document, "variables": variables, "branch": branch})


def guillotine(r):
    return (sc(r).get("data") or {}).get("guillotine") or {}


def term(field, value):
    return "{term: {field: %s, value: {string: %s}}}" % (json.dumps(field), json.dumps(value))


for branch in BRANCHES:
    # Q1 articles per author: an aggregation on the author reference field, then one query for the authors' names
    r = query("{ guillotine { queryDslConnection(query: %s, first: 0, aggregations: [{name: \"authors\", terms: "
              "{field: \"data.author\", size: 100, order: \"_count DESC\"}}]) { aggregationsAsJson } } }" % term("type", ARTICLE),
              branch)
    buckets = (guillotine(r).get("queryDslConnection") or {}).get("aggregationsAsJson", {}).get("authors", {}).get("buckets", [])
    names = query("query($ids: [String]) { guillotine { queryDsl(query: {in: {field: \"_id\", stringValues: $ids}}, first: 100) "
                  "{ _id _path displayName } } }", branch, ids=[b["key"] for b in buckets])
    by_id = {c["_id"]: c for c in guillotine(names).get("queryDsl") or []}
    got = [(leaf(by_id.get(b["key"], {}).get("_path", "?")), by_id.get(b["key"], {}).get("displayName"), b["docCount"])
           for b in buckets]
    want = [(a["author"], a["displayName"], a["count"]) for a in expected["articlesPerAuthor"][branch]]
    check("Q1 %s articles per author, largest first, with displayName and _path" % branch, ok(r) and ok(names) and got == want,
          (got, want))

    # Q2 what the caller worked on last: a term on modifier with caller.key from list-projects
    r = query("query($key: String) { guillotine { queryDsl(query: {term: {field: \"modifier\", value: {string: $key}}}, "
              "first: 100, sort: [{field: \"modifiedTime\", direction: DESC}, {field: \"_path\", direction: ASC}]) { _path } } }",
              branch, key=CALLER_KEY or "user:system:anonymous")
    if SIGNED_IN:
        got = [c["_path"] for c in guillotine(r).get("queryDsl") or []]
        check("Q2 %s modifier == caller.key, newest first == expected.recentlyModifiedByUser.%s.%s" % (branch, USER, branch),
              ok(r) and got == expected["recentlyModifiedByUser"].get(USER, {}).get(branch), show(r))
    else:
        check("Q2 anonymous term on modifier -> isError asking to sign in", readable_error(r, "signed-in", "sign in"), show(r))

    # Q3 unused images
    if SIGNED_IN:
        r = call("references", "find-unreferenced", {"types": ["media:image"], "branch": branch})
        got = [i["_path"] for i in sc(r).get("items", [])]
        check("Q3 %s unused images == expected.unusedImages.%s" % (branch, branch),
              ok(r) and got == expected["unusedImages"][branch] and sc(r).get("total") == len(got) and
              "content the caller can read" in sc(r).get("note", ""), show(r))

    # Q4 content in progress
    r = query("{ guillotine { queryDsl(query: %s, first: 50, sort: {field: \"_path\", direction: ASC}) { _path } } }"
              % term("workflow.state", "IN_PROGRESS"), branch)
    got = [c["_path"] for c in guillotine(r).get("queryDsl") or []]
    check("Q4 %s workflow.state IN_PROGRESS == expected.inWork.%s" % (branch, branch),
          ok(r) and got == expected["inWork"][branch], show(r))

    # Q5 items per content type
    r = call("content", "describe-type", {"branch": branch})
    got = {t["name"]: t["count"] for t in sc(r).get("types", [])}
    check("Q5 %s describe-type counts == expected.countPerType.%s, total %d" % (branch, branch, expected["totalItems"][branch]),
          ok(r) and got == expected["countPerType"][branch] and sc(r).get("total") == expected["totalItems"][branch], show(r))
    counts = [t["count"] for t in sc(r).get("types", [])]
    check("Q5 %s describe-type most used first" % branch, counts == sorted(counts, reverse=True), counts)
    r = query("{ guillotine { queryDslConnection(query: %s, first: 0) { totalCount } } }" % term("type", ARTICLE), branch)
    check("Q5 %s query count of articles == %d" % (branch, expected["countPerType"][branch][ARTICLE]),
          ok(r) and (guillotine(r).get("queryDslConnection") or {}).get("totalCount") == expected["countPerType"][branch][ARTICLE],
          show(r))

    # Q6 broken links on landing pages
    if SIGNED_IN:
        r = call("references", "find-broken-references", {"types": ["landing"], "branch": branch})
        items = {i["_path"]: i for i in sc(r).get("items", [])}
        landings = sorted({b["landing"] for b in expected["brokenLinks"]})
        dead = [b["targetId"] for b in expected["brokenLinks"] if b.get("targetId")]
        check("Q6 %s broken references on landings == landings in expected.brokenLinks" % branch,
              ok(r) and sorted(items) == landings, show(r))
        check("Q6 %s one missing id per landing, the never-existing image id among them" % branch,
              ok(r) and all(len(i["missing"]) == 1 for i in items.values()) and
              items.get("/landings/landing-missing-image", {}).get("missing") == dead, show(r))
        check("Q6 %s result says URL strings and external links are not checked" % branch,
              "Not checked: external URLs and URL strings" in sc(r).get("note", ""), sc(r).get("note"))
        deleted = items.get("/landings/landing-deleted-link", {}).get("missing", ["?"])[0]
        r = query("{ guillotine { get(key: \"/landings/landing-deleted-link\") { ... on com_example_mcpdataset_Landing "
                  "{ data { body { raw links { content { _path } } } } } } } }", branch)
        body = (((guillotine(r).get("get") or {}).get("data") or {}).get("body")) or {}
        contents = [link.get("content") for link in body.get("links") or []]
        check("Q6 %s query get: the broken id is in the raw body, its link has no content, the other link resolves" % branch,
              deleted in (body.get("raw") or "") and contents.count(None) == 1 and {"_path": "/articles/article-01"} in contents,
              show(r))

if not SIGNED_IN:
    for tool in ("find-unreferenced", "find-broken-references"):
        r = call("references", tool, {})
        check("Q3/Q6 anonymous %s -> isError asking to sign in" % tool, readable_error(r, "signed-in", "references tools"), show(r))
    r = call("graphql", "query", {"query": "{ guillotine { queryDsl(query: {matchAll: {}}, first: 1) { _id } } }", "branch": "draft"})
    check("anonymous draft -> isError: only published content", readable_error(r, "draft", "master"), show(r))

check_sizes()
finish("answers")
