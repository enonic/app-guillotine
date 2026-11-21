#!/usr/bin/env python3
"""Permission matrix: every tool, called by real XP identities, sees exactly what XP's ACLs let that caller read.

Callers: anonymous (needs mcp.allowAnonymous=true), mcp-dave (no project role), mcp-erin (viewer), mcp-alice (editor), mcp-frank
(editor who can read /team but not /restricted), mcp-carol (editor who can read both) and su (administrator). /team and /restricted
come from the "restricted" section of dataset.json; everything else is the regular dataset that expected.json describes.

For each caller and branch it checks the visible projects, items, counts, aggregation buckets, references and labels, and that
unreadable content never shows up by id, path, display name, count, bucket, label or error text: asking for a hidden item gives the
same answer as asking for one that does not exist. The suite signs the users in itself and signs them out at the end."""
import atexit
import json
import os
import sys
import urllib.error
import urllib.request

sys.dont_write_bytecode = True  # keep __pycache__ out of the source tree
import xpmcp  # noqa: E402
from xpmcp import E2E_DIR, XP_URL, call, check, error_text, finish, info, load_expected, ok, sc, show  # noqa: E402

PROJECT = "mcp-test"
ARTICLE = "com.example.mcpdataset:article"
FOLDER = "base:folder"
IMAGE = "media:image"
PRINCIPAL_ERROR = "Unauthorized: creator, modifier and owner need Content Studio access or an administrator role."
MISSING_ID = "00000000-0000-4000-8000-000000000000"

expected = load_expected()
with open(os.path.join(E2E_DIR, "fixtures/dataset-app/src/main/resources/dataset/dataset.json")) as f:
    dataset = json.load(f)
PASSWORDS = {u["login"]: u["password"] for u in dataset["users"]}
# the same overrides the dataset app applies: password.<login>=... in its config file, then MCP_E2E_PASSWORD for MCP_E2E_USER
DATASET_CFG = os.path.join(os.environ.get("XP_HOME", ""), "config", "com.example.mcpdataset.cfg")
if os.environ.get("XP_HOME") and os.path.isfile(DATASET_CFG):
    with open(DATASET_CFG) as f:
        for line in f:
            key, _, value = line.strip().partition("=")
            if key.strip().startswith("password.") and value:
                PASSWORDS[key.strip()[len("password."):]] = value.strip()
if os.environ.get("MCP_E2E_USER") and os.environ.get("MCP_E2E_PASSWORD"):
    PASSWORDS[os.environ["MCP_E2E_USER"]] = os.environ["MCP_E2E_PASSWORD"]
PASSWORDS["su"] = os.environ.get("XP_SU_PASSWORD", "mcp-e2e-local")
READERS = {folder["name"]: set(folder["readers"]) | {"su"} for folder in dataset["restricted"]["folders"]}
DRAFT_READERS = {"mcp-erin", "mcp-alice", "mcp-frank", "mcp-carol", "su"}
CALLERS = ["anonymous", "mcp-dave", "mcp-erin", "mcp-alice", "mcp-frank", "mcp-carol", "su"]


def login(user):
    body = json.dumps({"action": "login", "user": user, "password": PASSWORDS[user]}).encode()
    request = urllib.request.Request(XP_URL + "/_/idprovider/system", body, {"Content-Type": "application/json"})
    with urllib.request.urlopen(request) as response:
        answer = json.load(response)
        cookies = [v.split(";", 1)[0] for k, v in response.getheaders() if k.lower() == "set-cookie" and v.startswith("JSESSIONID=")]
    if not answer.get("authenticated") or not cookies:
        sys.exit("Sign-in as %s failed: %s" % (user, answer))
    return cookies[-1]


def logout(cookie):
    try:
        urllib.request.urlopen(urllib.request.Request(XP_URL + "/_/idprovider/system/logout", headers={"Cookie": cookie})).read()
    except urllib.error.HTTPError:
        pass  # the logout redirect lands on a page that needs a session; the session is ended either way


def paths_by_id():
    """Ids of /team and /restricted, read as su through the query tool."""
    query = ('{ guillotine { queryDsl(query: {boolean: {should: [{like: {field: "_path", value: "/content/team*"}}, '
             '{like: {field: "_path", value: "/content/restricted*"}}]}}, first: 20) { _id _path displayName } } }')
    hits = sc(call("graphql", "query", {"project": PROJECT, "branch": "draft", "query": query}, cookie=COOKIES["su"]))
    return {h["_path"]: h for h in hits.get("data", {}).get("guillotine", {}).get("queryDsl", [])}


COOKIES = {"anonymous": ""}
# also on sys.exit and on a crash: sign out every session opened so far
atexit.register(lambda: [logout(cookie) for cookie in COOKIES.values() if cookie])
for name in CALLERS[1:]:
    COOKIES[name] = login(name)
HIDDEN = paths_by_id()
if len(HIDDEN) != 6:
    sys.exit("Expected 6 items under /team and /restricted, got %s; redeploy dataset-app" % sorted(HIDDEN))
ID = {path: item["_id"] for path, item in HIDDEN.items()}
SECRET_NOTE, SECRET_IMAGE = ID["/restricted/secret-note"], ID["/restricted/secret-image.png"]
TEAM_ARTICLE, TEAM_IMAGE = ID["/team/team-article"], ID["/team/team-image.png"]


def reads(caller, folder):
    return caller in READERS[folder]


def visible_hidden(caller):
    """The /team and /restricted paths this caller can read."""
    return {p for p in HIDDEN if reads(caller, p.split("/")[1])}


def expected_counts(caller, branch):
    """Total and count per type on branch: the regular dataset plus a folder, an article and an image per readable restricted folder."""
    by_type = dict(expected["countPerType"][branch])
    extra = sum(1 for folder in READERS if reads(caller, folder))
    for t in (FOLDER, ARTICLE, IMAGE):
        by_type[t] = by_type.get(t, 0) + extra
    return expected["totalItems"][branch] + 3 * extra, by_type


def leaks(caller, result, allowed_ids=()):
    """Paths, display names and ids of items the caller cannot read that appear anywhere in a result."""
    text = json.dumps(result, ensure_ascii=False)
    found = []
    for path, item in HIDDEN.items():
        if path in visible_hidden(caller):
            continue
        # the folder names alone (team, restricted) are too common to scan for; their paths are scanned
        names = (path.rsplit("/", 1)[-1],) if path.count("/") > 1 else ()
        for needle in (path, item["displayName"]) + names:
            if needle in text:
                found.append(needle)
        if item["_id"] in text and item["_id"] not in allowed_ids:
            found.append(item["_id"])
    return found


def same_as_missing(hidden_result, missing_result, hidden_key, missing_key):
    """A hidden item answers exactly like an item that does not exist, apart from the key the caller passed."""
    a, b = error_text(hidden_result), error_text(missing_result)
    return bool(a) and a.replace(hidden_key, "<key>") == b.replace(missing_key, "<key>")


def buckets(result, by):
    for aggregation in sc(result).get("aggregations", []):
        if aggregation.get("by") == by:
            return {b["key"]: b for b in aggregation["buckets"]}
    return {}


def run(caller, branch):
    xpmcp.CALLER = "%s %s" % (caller, branch)
    cookie = COOKIES[caller]
    base = {"project": PROJECT, "branch": branch}
    team, restricted = reads(caller, "team"), reads(caller, "restricted")
    # ids a caller may see without reading the item: references stored in content it can read
    allowed = (SECRET_IMAGE, SECRET_NOTE) if team else ()
    total, by_type = expected_counts(caller, branch)

    def tool(group, name, arguments, scan=True):
        r = call(group, name, dict(base, **arguments), cookie=cookie)
        if scan:
            found = leaks(caller, r, allowed)
            check("%s %s: nothing unreadable in the result" % (name, json.dumps(arguments)[:60]), not found, found)
        return r

    if branch == "draft" and caller not in DRAFT_READERS:
        for group, name, arguments in [("content", "list-types", {}), ("content", "search", {}), ("content", "get", {"key": "/articles"}),
                                       ("graphql", "query", {"query": "{ guillotine { queryDsl(first: 1) { _path } } }"})]:
            r = tool(group, name, arguments)
            words = ("master",) + (("role in project",) if caller != "anonymous" else ())
            check("%s on draft -> isError naming %s" % (name, " and ".join(words)),
                  r.get("isError") is True and all(w in error_text(r) for w in words), show(r))
        if caller != "anonymous":
            for name, arguments in [("find-unreferenced", {}), ("find-broken-references", {})]:
                r = tool("references", name, arguments)
                check("%s on draft -> isError naming master" % name, r.get("isError") is True and "master" in error_text(r), show(r))
        return

    # list-types, describe-type: counts are the readable items only
    r = tool("content", "list-types", {})
    got = {t["name"]: t["count"] for t in sc(r).get("types", [])}
    check("list-types total %d and counts per type %s" % (total, by_type), sc(r).get("total") == total and got == by_type,
          show(r))
    r = tool("content", "describe-type", {"type": ARTICLE})
    check("describe-type article count %d" % by_type[ARTICLE], sc(r).get("count") == by_type[ARTICLE], show(r))

    # search: totals, paths, text, buckets, inbound references
    r = tool("content", "search", {"count": 0})
    check("search total %d" % total, sc(r).get("total") == total, show(r))
    for folder, can in (("team", team), ("restricted", restricted)):
        r = tool("content", "search", {"path": "/" + folder}, scan=False)
        none = tool("content", "search", {"path": "/nothing-here"})
        if can:
            check("search path /%s: its 3 items" % folder,
                  sorted(h["_path"] for h in sc(r).get("hits", [])) == sorted(p for p in HIDDEN if p.startswith("/" + folder)), show(r))
        else:
            check("search path /%s answers like a path that does not exist" % folder,
                  sc(r).get("total") == 0 and sc(r).get("note", "").replace("/" + folder, "<p>") ==
                  sc(none).get("note", "").replace("/nothing-here", "<p>"), show(r))
    r = tool("content", "search", {"text": "secret"})
    want = sorted(p for p in ("/restricted/secret-note", "/restricted/secret-image.png") if restricted)
    check("search text secret: %s" % (want or "no hits"), sorted(h["_path"] for h in sc(r).get("hits", [])) == want, show(r))

    r = tool("content", "search", {"types": [ARTICLE], "count": 0, "aggregate": [{"by": "data.tags", "size": 20},
                                                                                  {"by": "data.author", "size": 20}]})
    tags = buckets(r, "data.tags")
    check("tag buckets: secret %s, team %s" % (restricted, team), ("secret" in tags) == restricted and ("team" in tags) == team,
          sorted(tags))
    authors = {b.get("_path"): b["count"] for b in buckets(r, "data.author").values()}
    base_authors = {"/persons/" + a["author"]: a["count"] for a in expected["articlesPerAuthor"][branch]}
    want = dict(base_authors)
    for article in dataset["restricted"]["articles"]:
        if reads(caller, article["parent"]):
            key = "/persons/" + article["author"]
            want[key] = want.get(key, 0) + 1
    check("author buckets count readable articles only %s" % want, authors == want, authors)

    r = tool("content", "search", {"count": 0, "aggregate": [{"by": "_references", "size": 100}]})
    refs = buckets(r, "_references")
    image = refs.get(SECRET_IMAGE)
    if restricted:
        check("_references bucket for secret-image is labelled", image is not None and image.get("displayName") == "secret-image",
              image)
    elif team:
        check("_references bucket for secret-image (referenced by team-article) is {key, count, missing} only",
              image is not None and set(image) == {"key", "count", "missing"} and image["count"] == 1, image)
    else:
        check("no _references bucket for secret-image", image is None, image)
    check("_references bucket for team-image (referenced only by secret-note) iff /restricted is readable",
          (TEAM_IMAGE in refs) == restricted, refs.get(TEAM_IMAGE))

    r = tool("content", "search", {"filters": [{"references": SECRET_IMAGE}]})
    want = sorted(p for p, can in (("/team/team-article", team), ("/restricted/secret-note", restricted)) if can)
    check("search references secret-image by id: %s" % (want or "no hits"), sorted(h["_path"] for h in sc(r).get("hits", [])) == want,
          show(r))
    r = tool("content", "search", {"filters": [{"references": "/restricted/secret-image.png"}]}, scan=False)
    none = tool("content", "search", {"filters": [{"references": "/nothing-here"}]})
    if restricted:
        check("search references secret-image by path works", ok(r) and len(sc(r).get("hits", [])) == len(want), show(r))
    else:
        check("search references by hidden path answers like a path that does not exist",
              same_as_missing(r, none, "/restricted/secret-image.png", "/nothing-here"), show(r))

    # get: hidden items look missing; labels of referenced items only when readable
    for path in ("/restricted/secret-note", "/team/team-article"):
        can = path in visible_hidden(caller)
        for key, missing in ((ID[path], MISSING_ID), (path, "/articles/nothing-here")):
            r = tool("content", "get", {"key": key}, scan=False)
            if can:
                check("get %s by %s works" % (path, "id" if key == ID[path] else "path"), sc(r).get("_path") == path, show(r))
            else:
                check("get %s by %s answers like a missing item" % (path, "id" if key == ID[path] else "path"),
                      same_as_missing(r, tool("content", "get", {"key": missing}), key, missing), show(r))
    if team:
        r = tool("content", "get", {"key": TEAM_ARTICLE})
        data = sc(r).get("data", {})
        links = {link.get("id"): link for link in (data.get("body") or {}).get("links", [])}
        if restricted:
            check("get team-article: cover and rich-text targets labelled",
                  (data.get("cover") or {}).get("_path") == "/restricted/secret-image.png" and
                  links.get(SECRET_NOTE, {}).get("_path") == "/restricted/secret-note", data)
        else:
            check("get team-article: cover is {_id, missing}",
                  data.get("cover") == {"_id": SECRET_IMAGE, "missing": True}, data.get("cover"))
            check("get team-article: rich-text link and image to /restricted are missing, without labels",
                  all(links.get(i, {}).get("missing") is True and "_path" not in links.get(i, {}) for i in (SECRET_NOTE, SECRET_IMAGE)),
                  links)

    # query: the caller's own GraphQL document
    document = ('query($k: ID) { guillotine { get(key: $k) { _path } getChildren(key: "/restricted") { _path } '
                'queryDslConnection(query: {matchAll: {}}, first: 0, aggregations: [{name: "tags", terms: {field: "data.tags", size: 20}}, '
                '{name: "modifier", terms: {field: "modifier", size: 20}}]) { totalCount aggregationsAsJson } } }')
    r = tool("graphql", "query", {"query": document, "variables": {"k": SECRET_NOTE}})
    g = sc(r).get("data", {}).get("guillotine", {})
    connection = g.get("queryDslConnection") or {}
    aggs = connection.get("aggregationsAsJson") or {}
    tag_keys = {b["key"] for b in (aggs.get("tags") or {}).get("buckets", [])}
    modifiers = (aggs.get("modifier") or {}).get("buckets", [])
    check("query without errors", ok(r) and not sc(r).get("errors"), show(r))
    check("query totalCount %d" % total, connection.get("totalCount") == total, show(r))
    check("query get(secret-note) %s" % ("returns it" if restricted else "is null"),
          (g.get("get") or {}).get("_path") == ("/restricted/secret-note" if restricted else None), g.get("get"))
    check("query getChildren(/restricted) %s" % ("lists 2" if restricted else "is empty"),
          len(g.get("getChildren") or []) == (2 if restricted else 0), g.get("getChildren"))
    if not restricted:
        missing = sc(tool("graphql", "query", {"query": document.replace('"/restricted"', '"/nothing-here"'),
                                               "variables": {"k": MISSING_ID}}))
        mg = missing.get("data", {}).get("guillotine", {})
        check("query get and getChildren on hidden items answer like a missing id and path",
              not missing.get("errors") and (mg.get("get"), mg.get("getChildren")) == (g.get("get"), g.get("getChildren")),
              missing)
    check("query tag buckets: secret %s" % restricted, ("secret" in tag_keys) == restricted, sorted(tag_keys))
    check("query aggregation on modifier works and counts readable items only",
          bool(modifiers) and sum(b["docCount"] for b in modifiers) == total, modifiers)
    r = tool("graphql", "query", {"query": '{ guillotine { queryDsl(query: {term: {field: "_path", value: {string: '
                                           '"/content/articles/article-01"}}}, first: 1) { _path creator { value } modifier { value } '
                                           'owner { value } } } }'})
    hit = (sc(r).get("data", {}).get("guillotine", {}).get("queryDsl") or [{}])[0]
    if caller == "su":
        check("query creator/modifier/owner for an administrator", (hit.get("creator") or {}).get("value", "").startswith("user:"), show(r))
    else:
        errors = {(e.get("path") or [""])[-1]: e.get("message") for e in sc(r).get("errors", [])}
        check("query creator/modifier/owner each denied with the Content Studio message",
              all(hit.get(field) is None for field in ("creator", "modifier", "owner")) and
              errors == {field: PRINCIPAL_ERROR for field in ("creator", "modifier", "owner")}, show(r))
    by_modifier = {b["key"]: b["docCount"] for b in modifiers}
    r = tool("graphql", "query", {"query": '{ guillotine { queryDslConnection(query: {term: {field: "modifier", value: {string: '
                                           '"user:system:su"}}}, first: 0) { totalCount } } }'})
    count = (sc(r).get("data", {}).get("guillotine", {}).get("queryDslConnection") or {}).get("totalCount")
    check("query filter on modifier su counts readable items only (%s, as its bucket)" % by_modifier.get("user:system:su"),
          count is not None and count == by_modifier.get("user:system:su") and count > 0, show(r))
    if team and not restricted:
        # Known XP behaviour, not a Guillotine check: XP builds page and media URLs as administrator.
        r = call("graphql", "query", dict(base, query='query($k: ID) { guillotine { get(key: $k) { ... on com_example_mcpdataset_Article '
                                                       '{ data { body { processedHtml links { pageUrl { path } } } } } } } }',
                                          variables={"k": TEAM_ARTICLE}), cookie=cookie)
        info("XP url generation (processedHtml, Link.pageUrl) shows unreadable %s" % (leaks(caller, r, allowed) or "nothing"))

    # references group: signed-in only
    if caller == "anonymous":
        for name in ("find-unreferenced", "find-broken-references"):
            r = tool("references", name, {})
            check("%s is refused" % name, r.get("isError") is True, show(r))
        return
    r = tool("references", "find-unreferenced", {"count": 50})
    unused = sorted(i["_path"] for i in sc(r).get("items", []))
    want = sorted(expected["unusedImages"][branch] + (["/team/team-image.png"] if team and not restricted else []))
    check("find-unreferenced: %s" % want, unused == want, show(r))
    check("find-unreferenced says it covers readable content only", "content the caller can read" in sc(r).get("note", ""), sc(r).get("note"))
    r = tool("references", "find-broken-references", {})
    items = {i["_path"]: i for i in sc(r).get("items", [])}
    landings = sorted(b["landing"] for b in expected["brokenLinks"])
    check("find-broken-references lists the broken landings", all(p in items for p in landings), sorted(items))
    article = items.get("/team/team-article")
    if team and not restricted:
        check("find-broken-references: team-article misses secret-image and secret-note, shaped like a deleted target",
              article is not None and sorted(article["missing"]) == sorted([SECRET_IMAGE, SECRET_NOTE]) and
              set(article) == set(items[landings[0]]), article)
    else:
        check("find-broken-references: team-article not listed", article is None, article)


for caller in CALLERS:
    xpmcp.CALLER = caller
    r = call("content", "list-projects", {}, cookie=COOKIES[caller])
    s = sc(r)
    who = s.get("caller", {})
    check("list-projects names the caller",
          who == {"anonymous": True} if caller == "anonymous" else who.get("key") == "user:system:" + caller, show(r))
    branch = s.get("defaultBranch")
    if caller == "mcp-dave":
        check("list-projects lists no project: draft, the default branch, needs a project role",
              s.get("projects") == [] and branch == "draft", show(r))
    else:
        check("list-projects: %s with its readable item count" % PROJECT,
              [(p["name"], p["itemCount"]) for p in s.get("projects", [])] == [(PROJECT, expected_counts(caller, branch)[0])], show(r))
    for branch in ("draft", "master"):
        run(caller, branch)
    xpmcp.CALLER = caller

xpmcp.CALLER = "all callers"
finish("permissions")
