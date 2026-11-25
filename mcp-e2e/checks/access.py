#!/usr/bin/env python3
"""Who may call which group. usage: access.py open|closed
open:   mcp.allowAnonymous unset (default true). Anonymous callers use every group on master, without filters, sorts and
        aggregations on creator, modifier and owner.
closed: mcp.allowAnonymous=false. Anonymous callers are refused in every group; signed-in callers are not affected.
Signed-in calls use the cookie in ACCESS_COOKIE; a second session, signed out with LOGOUT_COOKIE, must count as anonymous."""
import os
import sys

sys.dont_write_bytecode = True  # keep __pycache__ out of the source tree
from xpmcp import call, check, finish, ok, readable_error, sc, show  # noqa: E402

MODE = sys.argv[1] if len(sys.argv) > 1 else ""
if MODE not in ("open", "closed"):
    sys.exit("usage: access.py open|closed")
SIGNED = os.environ.get("ACCESS_COOKIE", "")
if not SIGNED:
    sys.exit("Set ACCESS_COOKIE to a session cookie from scripts/login.sh")

CALLS = [
    ("content", "list-projects", {}),
    ("content", "describe-type", {}),
    ("graphql", "query", {"query": "{ guillotine { queryDsl(query: {matchAll: {}}, first: 1) { _path } } }"}),
    ("references", "find-unreferenced", {"count": 1}),
    ("references", "find-broken-references", {"path": "/landings"}),
]
for group, tool, arguments in CALLS:
    r = call(group, tool, arguments, cookie=SIGNED)
    check("%s: signed-in %s works" % (MODE, tool), ok(r), show(r))

    r = call(group, tool, arguments, cookie="")
    if MODE == "open":
        check("open: anonymous %s works on master" % tool, ok(r) and sc(r).get("branch", "master") == "master", show(r))
    else:
        check("closed: anonymous %s -> isError naming mcp.allowAnonymous=false" % tool,
              readable_error(r, "signed-in", "mcp.allowAnonymous=false"), show(r))

r = call("content", "list-projects", {}, cookie=SIGNED)
caller = sc(r).get("caller", {})
check("%s: list-projects names the signed-in user and defaults to draft" % MODE,
      caller.get("anonymous") is False and caller.get("key", "").startswith("user:system:") and sc(r).get("defaultBranch") == "draft",
      show(r))
if MODE == "open":
    r = call("content", "list-projects", {}, cookie="")
    check("open: anonymous list-projects says anonymous and defaults to master",
          sc(r).get("caller") == {"anonymous": True} and sc(r).get("defaultBranch") == "master", show(r))
    for group, tool, arguments in CALLS[1:]:
        r = call(group, tool, dict(arguments, branch="draft"), cookie="")
        check("open: anonymous %s on draft -> isError, only published content" % tool, readable_error(r, "draft", "master"), show(r))
    principals = [
        ("graphql", "query", {"query": '{ guillotine { queryDslConnection(query: {matchAll: {}}, first: 0, aggregations: '
                                       '[{name: "m", terms: {field: "modifier"}}]) { totalCount } } }'}),
    ]
    for group, tool, arguments in principals:
        r = call(group, tool, arguments, cookie=SIGNED)
        check("open: signed-in %s aggregates on modifier" % tool, ok(r) and not sc(r).get("errors"), show(r))
        r = call(group, tool, arguments, cookie="")
        check("open: anonymous %s on modifier -> isError, needs a signed-in caller" % tool,
              readable_error(r, "holds user keys, and this call is anonymous", "need a signed-in caller"), show(r))

logged_out = os.environ.get("LOGOUT_COOKIE", "")
if logged_out:
    r = call("content", "list-projects", {}, cookie=logged_out)
    check("%s: a signed-out session counts as anonymous" % MODE,
          sc(r).get("caller") == {"anonymous": True} if MODE == "open" else readable_error(r, "anonymous"), show(r))

finish("access " + MODE)
