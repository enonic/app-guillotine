#!/usr/bin/env python3
"""The tool catalog: 5 Guillotine tools in 3 groups, public names, annotations, input and output schemas, alwaysLoad."""
import re
import sys

sys.dont_write_bytecode = True  # keep __pycache__ out of the source tree
from xpmcp import GROUPS, PREFIX, check, finish, info, rpc, tools_list  # noqa: E402

ANNOTATIONS = {"readOnlyHint": True, "destructiveHint": False, "idempotentHint": True, "openWorldHint": False}
STATIC_ENUMS = [{"draft", "master"}]
WITHOUT_OUTPUT_SCHEMA = {"query"}
ALWAYS_LOAD = {"list-projects", "query"}
SCOPED = {"describe-type", "query", "find-unreferenced", "find-broken-references"}

raw, discover = rpc("server/discover", {})
check("server/discover advertises protocol 2026-07-28",
      "2026-07-28" in ((discover or {}).get("result") or {}).get("supportedVersions", []), raw[:300])

tools = tools_list()
ours = {n: t for n, t in tools.items() if n.startswith(PREFIX)}
want = {PREFIX + g + "__" + t for g, names in GROUPS.items() for t in names}
check("tools/list: exactly the 5 tools, named com.enonic.app.guillotine__<group>__<tool>", set(ours) == want,
      sorted(set(ours) ^ want))
check("tools/list: 3 groups (content 2, graphql 1, references 2)",
      {g: sum(1 for n in ours if n.startswith(PREFIX + g + "__")) for g in GROUPS} == {"content": 2, "graphql": 1, "references": 2})
check("names: [A-Za-z0-9_.-]{1,128} without _u/_x escapes",
      all(re.fullmatch(r"[A-Za-z0-9_.-]{1,128}", n) and "_u" not in n and "_x" not in n for n in ours), sorted(ours))
check("names: Claude Code names mcp__<server>__<tool> stay within 128 characters for a server name of up to 16",
      all(len("mcp__" + "x" * 16 + "__" + re.sub(r"[^A-Za-z0-9_-]", "_", n)) <= 128 for n in ours))
info("longest public name: %d characters" % max(len(n) for n in ours))


def local(name):
    return name.rsplit("__", 1)[-1]


check("annotations: read-only, non-destructive, idempotent, closed world on all 5",
      all(t.get("annotations") == ANNOTATIONS for t in ours.values()),
      {local(n): t.get("annotations") for n, t in ours.items() if t.get("annotations") != ANNOTATIONS})
check("every tool has a title and a description",
      all(t.get("title") and len(t.get("description", "")) > 40 for t in ours.values()))
check("inputSchema: object with additionalProperties false on all 5",
      all(t.get("inputSchema", {}).get("type") == "object" and t["inputSchema"].get("additionalProperties") is False
          for t in ours.values()))
check("inputSchema: project and branch (enum draft, master) on every tool that reads a project",
      all({"project", "branch"} <= set(t["inputSchema"].get("properties", {})) and
          t["inputSchema"]["properties"]["branch"].get("enum") == ["draft", "master"]
          for n, t in ours.items() if local(n) in SCOPED))
check("inputSchema: list-projects takes no arguments",
      ours.get(PREFIX + "content__list-projects", {}).get("inputSchema", {}).get("properties") == {})
check("inputSchema: required arguments (query query; describe-type none, it lists types without one)",
      ours[PREFIX + "content__describe-type"]["inputSchema"].get("required") is None and
      ours[PREFIX + "graphql__query"]["inputSchema"].get("required") == ["query"])


def walk(node, path, found):
    if isinstance(node, dict):
        if "enum" in node:
            found.append((path, node["enum"]))
        for k, v in node.items():
            walk(v, path + "/" + k, found)
    elif isinstance(node, list):
        for i, v in enumerate(node):
            walk(v, path + "/" + str(i), found)


enums = []
for name, t in ours.items():
    walk(t.get("inputSchema", {}), local(name), enums)
check("inputSchema: no enum derived from live data (only branch)",
      all(set(values) in STATIC_ENUMS for _, values in enums), [e for e in enums if set(e[1]) not in STATIC_ENUMS])

with_output = {local(n) for n, t in ours.items() if "outputSchema" in t}
check("outputSchema on every tool except query", with_output == {local(n) for n in ours} - WITHOUT_OUTPUT_SCHEMA,
      sorted(with_output))
check("outputSchema: closed objects (additionalProperties false at the top level)",
      all(t["outputSchema"].get("additionalProperties") is False for t in ours.values() if "outputSchema" in t))
loaded = {local(n) for n, t in ours.items() if (t.get("_meta") or {}).get("anthropic/alwaysLoad") is True}
check("_meta anthropic/alwaysLoad true on list-projects and query only", loaded == ALWAYS_LOAD, sorted(loaded))

finish("catalog")
