#!/usr/bin/env python3
"""Result budget with mcp.maxResultChars=2000 (run-all.sh sets it for this suite): cut results, hints and recomputed cursors."""
import json
import os
import sys

sys.dont_write_bytecode = True  # keep __pycache__ out of the source tree
os.environ.setdefault("MCP_MAX_RESULT_CHARS", "2000")
from xpmcp import MAX_RESULT_CHARS, SIGNED_IN, call, check, check_sizes, finish, load_expected, ok, readable_error, sc, show, walk  # noqa

expected = load_expected()
TOTAL = expected["totalItems"]["draft" if SIGNED_IN else "master"]

r = call("content", "search", {"count": 50, "fields": ["title", "author"]})
s = sc(r)
check("search count 50 with fields: truncated, hint, nextCursor right after the last hit returned",
      ok(r) and s.get("truncated") is True and "Continue with nextCursor" in s.get("hint", "") and
      0 < len(s.get("hits", [])) < min(50, TOTAL) and bool(s.get("nextCursor")), show(r))
check("  text equals structuredContent on a cut result", ok(r) and json.loads(r["content"][0]["text"]) == s)

hits, pages, results, error = walk("content", "search", {"count": 50, "fields": ["title", "author"]}, "hits")
ids = [h["_id"] for h in hits]
check("search pages through cut results: %d pages, %d ids, all distinct, == total %d" % (pages, len(ids), TOTAL),
      not error and len(ids) == len(set(ids)) == TOTAL and pages > 1, error or (pages, len(ids)))
check("  every page but the last is marked truncated",
      not error and all(p.get("truncated") is True for p in results[:-1]))

r = call("graphql", "query", {"query": "{ guillotine { queryDsl(query: {matchAll: {}}, first: 100) { _path displayName } } }"})
s = sc(r)
check("query first 100: truncated with a hint to select less, data kept as far as it fits",
      ok(r) and s.get("truncated") is True and "pass a smaller first" in s.get("hint", "") and
      len(s.get("data", {}).get("guillotine", {}).get("queryDsl", [])) > 0, show(r))

r = call("content", "describe-type", {"type": "article"})
check("describe-type that cannot fit -> isError asking the administrator to raise mcp.maxResultChars",
      readable_error(r, "does not fit mcp.maxResultChars (%d)" % MAX_RESULT_CHARS), show(r))

check_sizes()
finish("budget")
