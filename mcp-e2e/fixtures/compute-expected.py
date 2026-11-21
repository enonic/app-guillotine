#!/usr/bin/env python3
"""Derive expected answers for the mcp-test dataset from dataset.json alone (no XP/Guillotine/MCP code)."""
import json
import os
import sys
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
DATASET = os.path.join(HERE, "dataset-app/src/main/resources/dataset/dataset.json")
OUT = os.path.join(HERE, "expected.json")


def main():
    ds = json.load(open(DATASET))
    app = ds["app"]
    persons = {p["name"]: p for p in ds["persons"]}

    def p(*parts):
        return "/" + "/".join(parts)

    # Every live item in creation order, mirroring main.js: folders, persons, images, articles, (deleted), landings.
    items = []
    for f in ds["folders"]:
        items.append({"path": p(f["name"]), "type": "base:folder", "creator": "su", "publish": f["publish"]})
    for x in ds["persons"]:
        items.append({"path": p("persons", x["name"]), "type": app + ":person", "creator": x["user"], "publish": x["publish"]})
    for x in ds["images"]:
        items.append({"path": p("images", x["name"]), "type": "media:image", "creator": x["user"], "publish": x["publish"]})
    for a in ds["articles"]:
        items.append({"path": p("articles", a["name"]), "type": app + ":article", "creator": a["user"],
                      "publish": a["publish"] and a["workflow"] == "READY", "workflow": a["workflow"], "src": a})
    for l in ds["landings"]:
        items.append({"path": p("landings", l["name"]), "type": app + ":landing", "creator": l["user"],
                      "publish": l["publish"], "src": l})

    branches = {"draft": items, "master": [i for i in items if i["publish"]]}

    # 1. articles per author
    per_author = {}
    for br, its in branches.items():
        c = Counter(i["src"]["author"] for i in its if i["type"] == app + ":article")
        per_author[br] = [
            {"author": name, "authorPath": p("persons", name), "displayName": persons[name]["displayName"], "count": c[name]}
            for name in sorted(c, key=lambda n: (-c[n], n))
        ]

    # 2. most recently modified per test user (draft). Timeline: all creates in order, then modifications in
    # dataset order; publish and updateWorkflow do not touch modifiedTime (observed in the self-check log).
    timeline = [(i["path"], i["creator"]) for i in items]
    for a in ds["articles"]:
        if a.get("modifiedBy"):
            timeline.append((p("articles", a["name"]), a["modifiedBy"]))
    last_modifier, order = {}, {}
    for seq, (path, user) in enumerate(timeline):
        last_modifier[path] = user
        order[path] = seq
    master_paths = {i["path"] for i in branches["master"]}
    recent = {}
    for u in [x["login"] for x in ds["users"]]:
        own = sorted([pth for pth, usr in last_modifier.items() if usr == u], key=lambda x: -order[x])
        recent[u] = {"userKey": "user:system:" + u, "draft": own, "master": [x for x in own if x in master_paths]}

    # 1b. every article by path. displayName is the original title; a modifiedBy edit appends " (edited)" to data.title only,
    # and publishing runs after the edits, so master has the edited title too.
    def article_row(i):
        src = i["src"]
        title = src["title"] + (" (edited)" if src.get("modifiedBy") else "")
        return {"path": i["path"], "displayName": src["title"], "title": title, "author": persons[src["author"]]["displayName"]}

    articles = {br: [article_row(i) for i in sorted(its, key=lambda x: x["path"]) if i["type"] == app + ":article"]
                for br, its in branches.items()}

    # 3. unused images (no reference from cover, article body or landing body)
    def used_images(its):
        used = set()
        for i in its:
            src = i.get("src") or {}
            if src.get("cover"):
                used.add(src["cover"])
            used.update(src.get("bodyImages", []))
            used.update(src.get("images", []))
        return used

    unused = {}
    for br, its in branches.items():
        used = used_images(its)
        unused[br] = [i["path"] for i in its if i["type"] == "media:image" and i["path"].split("/")[-1] not in used]

    # 4. items in work
    in_work = [i["path"] for i in items if i.get("workflow") == "IN_PROGRESS"]

    # 5. count per content type
    per_type = {br: dict(sorted(Counter(i["type"] for i in its).items())) for br, its in branches.items()}
    totals = {br: len(its) for br, its in branches.items()}

    # 6. broken internal links on landings
    broken = []
    for l in ds["landings"]:
        for link in l["links"]:
            if link.get("deleted"):
                broken.append({"landing": p("landings", l["name"]), "kind": "content",
                               "target": p("articles", link["target"]), "reason": "target content deleted"})
        for m in l.get("missingImages", []):
            broken.append({"landing": p("landings", l["name"]), "kind": "image", "targetId": m["id"],
                           "reason": "image id never existed"})

    expected = {
        "project": ds["project"]["id"],
        "note": "Content ids are random; everything is keyed by path. Paths are relative to the project content root.",
        "users": [{"login": u["login"], "userKey": "user:system:" + u["login"]} for u in ds["users"]],
        "articlesPerAuthor": per_author,
        "articles": articles,
        "recentlyModifiedByUser": recent,
        "recentlyModifiedNote": ("Order is deterministic: creates in dataset order (folders by su, persons, images, articles, "
                                 "landings), then declared modifications in article order. Exact timestamps are not."),
        "unusedImages": unused,
        "inWork": {"draft": in_work, "master": []},
        "countPerType": per_type,
        "totalItems": totals,
        "brokenLinks": broken,
    }
    json.dump(expected, open(OUT, "w"), indent=2)
    print("wrote " + OUT)


if __name__ == "__main__":
    sys.exit(main())
