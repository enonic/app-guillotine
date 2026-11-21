#!/usr/bin/env python3
"""Elevated code paths and extension fields: the query tool never shows a caller more than Guillotine's GraphQL API over HTTP shows
the same caller, and where Guillotine's own code elevates over HTTP, it shows nothing the caller cannot read.

Every document goes once to the query tool and once to the HTTP endpoint with the same session cookie. mcp-frank reads the site and
the article of the mcp-sec project but not its templates folder; mcp-carol and su read both (the "security" section of dataset.json).
Covered:
- the default page template (pageTemplate; pageAsJson and components with resolveTemplate), which Guillotine resolves with the
  Content Studio administrator role over HTTP and with the caller's permissions over MCP;
- the scopeKey argument, which over HTTP is checked with XP's contentExists and so finds content the caller cannot read; over MCP an
  unreadable scope answers exactly like a missing one;
- page URLs and processed rich text, which XP builds with an administrator role: MCP equals HTTP, and what they show is printed;
- the extension fields of the dataset app: resolvers run as the caller, over MCP exactly as over HTTP, also the one that elevates on
  purpose; describe-type lists the field as an app field."""
import atexit
import json
import re
import sys

sys.dont_write_bytecode = True  # keep __pycache__ out of the source tree
import xpmcp  # noqa: E402
from xpmcp import call, check, error_text, finish, info, load_dataset, load_expected, login, logout, sc, show, gql  # noqa: E402

dataset = load_dataset()
expected = load_expected()
SEC = dataset["security"]
SEC_PROJECT = SEC["project"]["id"]
PROJECT = dataset["project"]["id"]
ARTICLE = dataset["app"] + ":article"
ARTICLE_TYPE = "com_example_mcpdataset_Article"
SITE_ARTICLE = "/%s/%s" % (SEC["site"]["name"], SEC["article"]["name"])
TEMPLATES = "/%s/_templates" % SEC["site"]["name"]
TEMPLATE = "%s/%s" % (TEMPLATES, SEC["template"]["name"])
TEMPLATE_READERS = set(SEC["templateReaders"]) | {"su"}
CALLERS = ["mcp-frank", "mcp-carol", "su"]

COOKIES = {"anonymous": ""}
atexit.register(lambda: [logout(cookie) for cookie in COOKIES.values() if cookie])
for name in CALLERS:
    COOKIES[name] = login(name)


def both(caller, project, branch, document, guillotine_args="", variables=None):
    """One document over the query tool and over HTTP; {G} in it stands for the guillotine field with its arguments."""
    mcp = call("graphql", "query", {"project": project, "branch": branch, "variables": variables or {},
                                    "query": document.replace("{G}", "guillotine" + ("(%s)" % guillotine_args if guillotine_args else ""))},
               cookie=COOKIES[caller])
    args = 'project: "%s", branch: "%s"' % (project, branch) + (", " + guillotine_args if guillotine_args else "")
    http = gql(document.replace("{G}", "guillotine(%s)" % args), variables, cookie=COOKIES[caller])
    return mcp, http


def leaves(value):
    """Every scalar in a JSON value except null."""
    if isinstance(value, dict):
        return set().union(*(leaves(v) for v in value.values())) if value else set()
    if isinstance(value, list):
        return set().union(*(leaves(v) for v in value)) if value else set()
    return set() if value is None else {json.dumps(value)}


def beyond_http(mcp, http):
    """The query tool's data holds no value that the HTTP answer's data does not hold."""
    return leaves(sc(mcp).get("data")) - leaves(http.get("data"))


def hidden_needles():
    """Ids, paths, names and texts of what mcp-frank cannot read: mcp-sec's templates and mcp-test's /restricted, read as su."""
    template = sc(call("graphql", "query", {"project": SEC_PROJECT, "branch": "draft", "query":
                       '{ guillotine { get(key: "%s") { _id parent { _id } } } }' % TEMPLATE}, cookie=COOKIES["su"]))
    item = template.get("data", {}).get("guillotine", {}).get("get") or {}
    restricted = sc(call("graphql", "query", {"project": PROJECT, "branch": "draft", "query":
                         '{ guillotine { getChildren(key: "/restricted") { _id _path displayName } get(key: "/restricted") { _id } } }'},
                         cookie=COOKIES["su"])).get("data", {}).get("guillotine", {})
    if not item or len(restricted.get("getChildren") or []) != 2:
        sys.exit("Missing %s:%s or the items of %s:/restricted; redeploy dataset-app" % (SEC_PROJECT, TEMPLATE, PROJECT))
    needles = [item["_id"], item["parent"]["_id"], TEMPLATES, SEC["template"]["displayName"], SEC["template"]["heading"],
               restricted["get"]["_id"]]
    for child in restricted["getChildren"]:
        needles += [child["_id"], child["_path"], child["displayName"]]
    return needles


NEEDLES = hidden_needles()


def leaks(result):
    text = json.dumps(result, ensure_ascii=False)
    return [n for n in NEEDLES if n in text]


frank_results = []

# Default page template: Guillotine elevates over HTTP, not over MCP
TEMPLATE_DOC = ('{ {G} { get(key: "%s") { _path pageTemplate { _id _path displayName } pageAsJson(resolveTemplate: true) '
                'components(resolveTemplate: true) { path type } } } }' % SITE_ARTICLE)
for caller in CALLERS:
    xpmcp.CALLER = caller
    mcp, http = both(caller, SEC_PROJECT, "draft", TEMPLATE_DOC)
    item = (sc(mcp).get("data", {}).get("guillotine") or {}).get("get") or {}
    check("query get %s answers without errors" % SITE_ARTICLE, item.get("_path") == SITE_ARTICLE and not sc(mcp).get("errors"), show(mcp))
    check("query shows no value that HTTP does not show", not beyond_http(mcp, http), beyond_http(mcp, http))
    if caller in TEMPLATE_READERS:
        check("template reader: pageTemplate and pageAsJson as over HTTP",
              (item.get("pageTemplate") or {}).get("_path") == TEMPLATE and sc(mcp).get("data") == http.get("data"), show(mcp))
    else:
        frank_results.append(mcp)
        check("pageTemplate is null: the default template is not readable", "pageTemplate" in item and item["pageTemplate"] is None,
              show(mcp))
        check("pageAsJson and components with resolveTemplate show the item's own page, not the template's",
              item.get("pageAsJson") == {} and item.get("components") == [], show(mcp))
        check("no id, path, name or config of the template", not leaks(mcp), leaks(mcp))
        info("HTTP API (unchanged, resolves the template as Content Studio administrator) shows %s" % (leaks(http) or "nothing"))

# scopeKey: XP's contentExists finds unreadable content over HTTP; over MCP an unreadable scope looks missing
SCOPE_DOC = '{ {G} { get(key: "%s") { _path } } }' % SITE_ARTICLE
MISSING = TEMPLATES + "/no-such-template"
xpmcp.CALLER = "mcp-frank"
hidden_mcp, hidden_http = both("mcp-frank", SEC_PROJECT, "draft", SCOPE_DOC, 'scopeKey: "%s"' % TEMPLATE)
missing_mcp, missing_http = both("mcp-frank", SEC_PROJECT, "draft", SCOPE_DOC, 'scopeKey: "%s"' % MISSING)
check("query with an unreadable scopeKey answers exactly like a missing one",
      bool(error_text(hidden_mcp)) and error_text(hidden_mcp).replace(TEMPLATE, "<key>") == error_text(missing_mcp).replace(MISSING, "<key>"),
      [error_text(hidden_mcp), error_text(missing_mcp)])
readable_mcp, readable_http = both("mcp-frank", SEC_PROJECT, "draft", SCOPE_DOC, 'scopeKey: "/%s"' % SEC["site"]["name"])
check("query with a readable scopeKey works as over HTTP",
      (sc(readable_mcp).get("data", {}).get("guillotine") or {}).get("get", {}).get("_path") == SITE_ARTICLE and
      sc(readable_mcp).get("data") == readable_http.get("data"), show(readable_mcp))
frank_results.append(readable_mcp)
info("HTTP API (unchanged): an unreadable scopeKey %s, a missing one %s" % (
    "fails" if hidden_http.get("errors") else "works", "fails" if missing_http.get("errors") else "works"))

def without_refs(data):
    return re.sub(r'data-(image|link)-ref=\\"[^\\]*\\"', "", json.dumps(data))


# URLs: XP builds them with an administrator role, for MCP and HTTP alike
URL_DOC = ('query($k: ID) { {G} { get(key: $k) { pageUrl { path } ... on %s { data { body { processedHtml '
           'links { pageUrl { path } } } } } } } }' % ARTICLE_TYPE)
xpmcp.CALLER = "mcp-frank"
mcp, http = both("mcp-frank", PROJECT, "draft", URL_DOC, variables={"k": "/team/team-article"})
check("page URLs and processed rich text equal the HTTP answer, apart from the per-call data-*-ref ids",
      bool(sc(mcp).get("data")) and without_refs(sc(mcp).get("data")) == without_refs(http.get("data")), show(mcp))
info("XP url generation (pageUrl, processedHtml, Link.pageUrl) shows unreadable %s over both" % (leaks(mcp) or "nothing"))

# Extension fields: the app's resolvers run as the caller, over MCP exactly as over HTTP
EXT_DOC = ('{ {G} { readableArticleCount articleCountAsAdmin '
           'queryDsl(query: {term: {field: "type", value: {string: "%s"}}}, first: 1) { ... on %s { viewer } } } }' % (ARTICLE, ARTICLE_TYPE))
READERS = {f["name"]: set(f["readers"]) | {"su"} for f in dataset["restricted"]["folders"]}
RESTRICTED_ARTICLES = dataset["restricted"]["articles"]
for caller, branch in [("anonymous", "master"), ("mcp-frank", "draft"), ("mcp-frank", "master"), ("mcp-carol", "draft"), ("su", "draft")]:
    xpmcp.CALLER = "%s %s" % (caller, branch)
    mcp, http = both(caller, PROJECT, branch, EXT_DOC)
    data = (sc(mcp).get("data") or {}).get("guillotine") or {}
    readable = sum(1 for a in RESTRICTED_ARTICLES if caller in READERS[a["parent"]])
    regular = expected["countPerType"][branch][ARTICLE]
    check("extension fields answer as over HTTP", bool(data) and sc(mcp).get("data") == http.get("data"), [show(mcp), http])
    check("extension resolver sees the caller (%s)" % caller,
          [h.get("viewer") for h in data.get("queryDsl", [])] == ["anonymous" if caller == "anonymous" else "user:system:" + caller], show(mcp))
    check("extension resolver counts only readable articles (%d)" % (regular + readable),
          data.get("readableArticleCount") == regular + readable, data.get("readableArticleCount"))
    check("extension resolver that elevates on purpose counts every article (%d), as over HTTP" % (regular + len(RESTRICTED_ARTICLES)),
          data.get("articleCountAsAdmin") == regular + len(RESTRICTED_ARTICLES), data.get("articleCountAsAdmin"))
    if caller == "mcp-frank":
        frank_results.append(mcp)

xpmcp.CALLER = "mcp-frank"
r = call("content", "describe-type", {"project": PROJECT, "type": ARTICLE}, cookie=COOKIES["mcp-frank"])
check("describe-type lists the extension field viewer under appFields, with appFieldsNote",
      "viewer" in sc(r).get("appFields", []) and "another application" in sc(r).get("appFieldsNote", ""), show(r))
frank_results.append(r)

check("leaks(): no MCP result above shows mcp-frank an id, path, name or text he cannot read",
      not [n for result in frank_results for n in leaks(result)], [n for result in frank_results for n in leaks(result)])

xpmcp.CALLER = "all callers"
finish("elevation")
