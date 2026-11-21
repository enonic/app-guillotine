#!/usr/bin/env python3
"""Cross-checks expected.json against Guillotine's HTTP GraphQL API on master (anonymous), independent of MCP code."""
import json, os, sys, urllib.request, pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
EXPECTED = json.loads((ROOT / "fixtures/expected.json").read_text())
URL = os.environ.get("XP_URL", "http://localhost:8080") + "/api/com.enonic.app.guillotine:graphql"

def gql(query, variables=None):
    req = urllib.request.Request(URL, json.dumps({"query": query, "variables": variables or {}}).encode(),
                                 {"Content-Type": "application/json"})
    return json.load(urllib.request.urlopen(req))

fail = 0
def check(name, ok, detail=""):
    global fail
    print(("PASS  " if ok else "FAIL  ") + name + ("" if ok else "  " + detail))
    fail |= not ok

Q = """query($agg:[AggregationInput]){guillotine(project:"mcp-test",branch:"master"){
  queryDslConnection(query:{boolean:{mustNot:[{term:{field:"_path",value:{string:"/content"}}}]}},first:0,aggregations:$agg){totalCount aggregationsAsJson}}}"""
r = gql(Q, {"agg": [{"name": "types", "terms": {"field": "type", "size": 50}}]})
conn = r["data"]["guillotine"]["queryDslConnection"]
types = {b["key"]: b["docCount"] for b in conn["aggregationsAsJson"]["types"]["buckets"]}
exp = {k: v for k, v in EXPECTED["countPerType"]["master"].items()}
check("master count per type", types == exp, f"got {types} expected {exp}")
check("master total", conn["totalCount"] == EXPECTED["totalItems"]["master"],
      f"got {conn['totalCount']} expected {EXPECTED['totalItems']['master']}")

Q2 = """query{guillotine(project:"mcp-test",branch:"master"){
  queryDslConnection(query:{term:{field:"type",value:{string:"com.example.mcpdataset:article"}}},first:0,
    aggregations:[{name:"authors",terms:{field:"data.author",size:50}}]){aggregationsAsJson}}}"""
buckets = gql(Q2)["data"]["guillotine"]["queryDslConnection"]["aggregationsAsJson"]["authors"]["buckets"]
ids = [b["key"] for b in buckets]
Q3 = """query($ids:[String]){guillotine(project:"mcp-test",branch:"master"){
  queryDsl(query:{in:{field:"_id",stringValues:$ids}},first:50){_id _path}}}"""
paths = {c["_id"]: c["_path"] for c in gql(Q3, {"ids": ids})["data"]["guillotine"]["queryDsl"]}
got = {paths.get(b["key"], b["key"]).split("/")[-1]: b["docCount"] for b in buckets}
exp = {a["author"]: a["count"] for a in EXPECTED["articlesPerAuthor"]["master"]}
check("master articles per author", got == exp, f"got {got} expected {exp}")

Q4 = """query{guillotine(project:"mcp-test",branch:"master"){
  queryDsl(query:{term:{field:"type",value:{string:"com.example.mcpdataset:article"}}},first:100,sort:{field:"_path",direction:ASC}){
    _path displayName ... on com_example_mcpdataset_Article{data{title author{displayName}}}}}}"""
got = [{"path": c["_path"], "displayName": c["displayName"], "title": c["data"]["title"], "author": c["data"]["author"]["displayName"]}
       for c in gql(Q4)["data"]["guillotine"]["queryDsl"]]
check("master articles (path, displayName, title, author)", got == EXPECTED["articles"]["master"],
      f"got {json.dumps(got)[:300]} expected {json.dumps(EXPECTED['articles']['master'])[:300]}")

r = gql("""query{guillotine(project:"mcp-test",branch:"draft"){queryDsl(first:1){_path}}}""")
check("anonymous draft is denied", bool(r.get("errors")), json.dumps(r)[:200])
sys.exit(fail)
