#!/usr/bin/env python3
"""Public MCP tool names as XP's McpNames.qualify builds them on epic/12627-xp-mcp-server, and the name Claude Code gives them."""
import re
import sys

GUILLOTINE = "com.enonic.app.guillotine"
CLAUDE_CODE_SERVER = "xp"


def encode(component):
    out = []
    for c in component:
        if c.isascii() and (c.isalnum() or c in ".-"):
            out.append(c)
        elif c == "_":
            out.append("_u")
        else:
            out.append("_x%04x" % ord(c))
    return "".join(out)


def qualify(app, group, local):
    name = encode(app) + "__" + encode(group) + "__" + encode(local)
    if len(name) > 128:
        raise ValueError("public MCP name over 128 characters: " + name)
    return name


def client_name(public_name, server=CLAUDE_CODE_SERVER):
    """Claude Code: mcp__<server>__<tool>, with characters outside [A-Za-z0-9_-] replaced by _; dropped above 128."""
    return "mcp__" + server + "__" + re.sub(r"[^A-Za-z0-9_-]", "_", public_name)


GUILLOTINE_PREFIX = qualify(GUILLOTINE, "content", "")

if __name__ == "__main__":
    print(qualify(*sys.argv[1:4]))
