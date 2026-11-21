# Guillotine

Enonic XP app that exposes content as a headless GraphQL API, plus read-only MCP tools for XP's MCP server. User docs are AsciiDoc in `docs/` (pages listed in `docs/menu.json`).

## Build and test

```sh
NODE_OPTIONS=--max-old-space-size=8192 ./gradlew build   # full build, tests, lint; jar in build/libs/guillotine.jar
NODE_OPTIONS=--max-old-space-size=8192 ./gradlew test --tests 'com.enonic.app.guillotine.mcp.*'
```

- Test compilation depends on the webpack build, so `test` needs the larger Node heap too; without it webpack can run out of memory.
- JDK toolchain is Java 25.
- `McpControllerGraalScriptTest` reruns the controller tests on GraalJS; keep both engines passing.

## MCP code

- `src/main/resources/mcp/<group>/<group>.yaml` and `<group>.js`, for the groups `content`, `graphql` and `references`: one descriptor and one controller per group. Names, descriptions and input/output schemas in the descriptors are what the model sees; keep them in sync with the Java code and `docs/mcp.adoc`. Every descriptor declares `allow: role:system.authenticated`. `McpDescriptorTest` checks that descriptors, controllers and registered tools match.
- Each controller passes the call to `McpToolsBean.run(group, name, args)`.
- `src/main/java/com/enonic/app/guillotine/mcp/`
  - `McpTools`: dispatch, the `mcp.enabled` check, the anonymous check (`McpGroup.anonymousOptIn` plus `mcp.allowAnonymous`), and turning exceptions into error results. A new tool is an `McpTool` (group, name, execute) registered in `McpTools.tools(...)` and declared in its group's descriptor.
  - `McpGroup`: the three groups and which of them `mcp.allowAnonymous` may open.
  - `tools/`: one class per tool (`ListProjectsTool`, `ListTypesTool`, `DescribeTypeTool`, `SearchTool`, `GetTool`, `QueryTool`, `FindUnreferencedTool`, `FindBrokenReferencesTool`), plus helpers that know about content: `ContentLookup` (constant GraphQL reads, labels, workflow states), `ContentKeys` (ids and paths), `ContentTypeIndex`, `DataFieldScope`, `DataValues`, `TypeFields`, `TypeSdl`, `TypeExamples`, `SearchQuery`, `QueryLimits`, `ReferenceQueries` (the reference index and outbound dependencies), `RichText`.
  - `shape/`: tool-independent plumbing: result envelope (`McpResult`), size budget (`ResultBudget`), paging (`Cursors`, `Page`), the per-call time limit (`Deadline`), project/branch resolution (`ProjectBranch`, `ProjectBranchResolver`), `ToolError`, GraphQL error cleanup (`GraphQlErrors`), `McpJson`, `Nulls`. `shape/` must not depend on `tools/`.
- Settings are `mcp.*` in `GuillotineConfig` / `GuillotineConfigService` (`enabled`, `defaultProject`, `maxResultChars`, `allowAnonymous`), documented in `docs/configuration.adoc` (`[#mcp]`).

## Rules

- Do not change the behaviour of the GraphQL HTTP API. MCP reuses `SchemaProvider`/`GraphQLApi` in-process; additions there must be opt-in for MCP callers only.
- Tools are read-only and run with the caller's permissions. Guillotine's own code never elevates on a path an MCP call reaches: no admin context, `runAsAdmin` or added principals.
  - Two core paths elevate over HTTP, and MCP runs them without: the default page template in `BasePageDataFetcher` (Content Studio administrator role) and the `scopeKey` check in `GuillotineDataFetcher` (XP's `contentExists` ignores read permissions). The MCP entry `SchemaProvider.executeToSpecification(query, variables, operationName, instrumentation)` sets `Elevation.NONE`, and both check `Elevation.of(environment)`. A core path that elevates must do the same.
  - Outside Guillotine, and the same over MCP as over HTTP: XP's `PortalUrlServiceImpl` builds page, image and attachment URLs and processed rich text with `role:system.admin`, and other apps' extension resolvers run with whatever context their app sets.
  - `mcp-e2e/checks/elevation.py` runs each of these over MCP and HTTP with the same session.
- A caller mistake throws `ToolError` with a message that says what to change (the hint); it becomes an `isError` result. Never let an exception text or Java class name reach the model.
- Every result whose size depends on data goes through a `ResultBudget` (`mcp.maxResultChars`). Paged results recompute `nextCursor` when items are dropped.
- A tool whose number of backend calls depends on data checks a `Deadline` between calls and returns a partial result with `incomplete: true` when it passes. After the deadline it starts only the calls needed to report what it already read.
- Except for the `query` tool's own document, user input reaches GraphQL only as variables of constant documents.
- Structured results must not contain `null` inside arrays: XP drops such items.

## End-to-end checks

`mcp-e2e/` runs the tools on a real local XP with a fixed dataset, as an anonymous and as a signed-in caller; it is not part of the Gradle build. See `mcp-e2e/README.md` for prerequisites (an XP build from the XP MCP epic, and the standard ID provider jar for sign-in) and the commands. In short:

```sh
export XP_DIST=<xp checkout>/modules/runtime/build/install
mcp-e2e/scripts/build-fixtures.sh && mcp-e2e/scripts/xp-start.sh && mcp-e2e/scripts/deploy.sh
mcp-e2e/scripts/run-all.sh          # ends with RESULT: PASS or RESULT: FAIL
mcp-e2e/scripts/xp-stop.sh
```

When a change alters what a tool returns for the dataset, update the suite in `mcp-e2e/checks/`; when it changes the dataset, rerun `mcp-e2e/fixtures/compute-expected.py` and commit `expected.json` with it.

## References

- `docs/mcp.adoc`: user documentation for the tools (access, sign-in, arguments, limits, errors, workflows).
- XP side contract (endpoint `/api/system:mcp`, public tool names, error model): `modules/mcp/mcp-impl/README.md` in the enonic/xp MCP epic branch.
