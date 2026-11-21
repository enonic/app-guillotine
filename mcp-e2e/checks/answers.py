#!/usr/bin/env python3
"""The six stakeholder questions answered through the tools and compared with fixtures/expected.json. A signed-in caller is checked on
draft and master; an anonymous caller on master, where questions that need sign-in must be refused with a reason."""
import sys

sys.dont_write_bytecode = True  # keep __pycache__ out of the source tree
from xpmcp import SIGNED_IN, call, check, check_sizes, finish, leaf, load_expected, ok, readable_error, sc, show, walk  # noqa: E402

expected = load_expected()
CALLER_KEY = sc(call("content", "list-projects", {})).get("caller", {}).get("key", "")
USER = CALLER_KEY.rsplit(":", 1)[-1]
BRANCHES = ["draft", "master"] if SIGNED_IN else ["master"]

for branch in BRANCHES:
    # Q1 articles per author: one search with an aggregation on the author reference field
    r = call("content", "search", {"types": ["article"], "aggregate": [{"by": "data.author"}], "count": 0, "branch": branch})
    buckets = sc(r).get("aggregations", [{}])[0].get("buckets", [])
    got = [(leaf(b.get("_path", "?")), b.get("displayName"), b["count"]) for b in buckets]
    want = [(a["author"], a["displayName"], a["count"]) for a in expected["articlesPerAuthor"][branch]]
    check("Q1 %s articles per author, largest first, with displayName and _path" % branch, ok(r) and got == want, (got, want))

    # Q2 what the caller worked on last
    if SIGNED_IN:
        hits, _, _, error = walk("content", "search", {"mine": True, "sort": "modifiedTime DESC", "count": 10, "branch": branch},
                                 "hits")
        got = [h["_path"] for h in hits]
        check("Q2 %s mine, newest first == expected.recentlyModifiedByUser.%s.%s" % (branch, USER, branch),
              not error and got == expected["recentlyModifiedByUser"].get(USER, {}).get(branch), error or got)
    else:
        r = call("content", "search", {"mine": True})
        check("Q2 anonymous mine -> isError asking to sign in", readable_error(r, "signed-in", "sign in"), show(r))

    # Q3 unused images
    if SIGNED_IN:
        r = call("references", "find-unreferenced", {"types": ["media:image"], "branch": branch})
        got = [i["_path"] for i in sc(r).get("items", [])]
        check("Q3 %s unused images == expected.unusedImages.%s" % (branch, branch),
              ok(r) and got == expected["unusedImages"][branch] and sc(r).get("total") == len(got) and
              "content the caller can read" in sc(r).get("note", ""), show(r))

    # Q4 content in progress
    r = call("content", "search", {"workflow": "IN_PROGRESS", "sort": "path ASC", "count": 50,
                                   "branch": branch})
    got = [h["_path"] for h in sc(r).get("hits", [])]
    check("Q4 %s workflow IN_PROGRESS == expected.inWork.%s" % (branch, branch),
          ok(r) and got == expected["inWork"][branch] and all(h.get("workflow") == "IN_PROGRESS" for h in sc(r)["hits"]), show(r))

    # Q5 items per content type
    r = call("content", "list-types", {"branch": branch})
    got = {t["name"]: t["count"] for t in sc(r).get("types", [])}
    check("Q5 %s list-types counts == expected.countPerType.%s, total %d" % (branch, branch, expected["totalItems"][branch]),
          ok(r) and got == expected["countPerType"][branch] and sc(r).get("total") == expected["totalItems"][branch], show(r))
    counts = [t["count"] for t in sc(r).get("types", [])]
    check("Q5 %s list-types most used first" % branch, counts == sorted(counts, reverse=True), counts)
    r = call("content", "search", {"types": ["com.example.mcpdataset:article"], "count": 0, "branch": branch})
    check("Q5 %s search count of articles == %d" % (branch, expected["countPerType"][branch]["com.example.mcpdataset:article"]),
          ok(r) and sc(r).get("total") == expected["countPerType"][branch]["com.example.mcpdataset:article"], show(r))

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
        r = call("content", "get", {"key": "/landings/landing-deleted-link", "branch": branch})
        links = sc(r).get("data", {}).get("body", {}).get("links", [])
        check("Q6 %s get shows the broken id as a missing rich-text link, next to a resolved one" % branch,
              any(link.get("id") == deleted and link.get("missing") is True for link in links) and
              any(link.get("_path") == "/articles/article-01" for link in links), links)

if not SIGNED_IN:
    for tool in ("find-unreferenced", "find-broken-references"):
        r = call("references", tool, {})
        check("Q3/Q6 anonymous %s -> isError asking to sign in" % tool, readable_error(r, "signed-in", "references tools"), show(r))
    r = call("content", "search", {"branch": "draft"})
    check("anonymous draft -> isError: only published content", readable_error(r, "draft", "master"), show(r))

check_sizes()
finish("answers")
