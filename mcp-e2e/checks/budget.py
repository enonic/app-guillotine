#!/usr/bin/env python3
"""Result budget with mcp.maxResultChars=2000 (run-all.sh sets it for this suite): cut results and their hints. Cursors recomputed after
a cut are covered by FindBrokenReferencesToolTest: the dataset has too few broken references to fill 2000 characters."""
import json
import os
import sys

sys.dont_write_bytecode = True  # keep __pycache__ out of the source tree
os.environ.setdefault("MCP_MAX_RESULT_CHARS", "2000")
from xpmcp import MAX_RESULT_CHARS, call, check, check_sizes, finish, ok, readable_error, sc, show  # noqa

r = call("graphql", "query", {"query": "{ guillotine { queryDsl(query: {matchAll: {}}, first: 100) { _path displayName } } }"})
s = sc(r)
check("query first 100: truncated with a hint to select less, data kept as far as it fits",
      ok(r) and s.get("truncated") is True and "pass a smaller first" in s.get("hint", "") and
      len(s.get("data", {}).get("guillotine", {}).get("queryDsl", [])) > 0, show(r))
check("  text equals structuredContent on a cut result", ok(r) and json.loads(r["content"][0]["text"]) == s)

r = call("content", "describe-type", {"type": "article"})
check("describe-type that cannot fit -> isError asking the administrator to raise mcp.maxResultChars",
      readable_error(r, "does not fit mcp.maxResultChars (%d)" % MAX_RESULT_CHARS), show(r))

check_sizes()
finish("budget")
