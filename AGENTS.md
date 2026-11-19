# Guillotine

Enonic XP app that exposes content as a headless GraphQL API, plus read-only MCP content tools for XP's MCP server. User docs are AsciiDoc in `docs/` (pages listed in `docs/menu.json`).

## Build and test

```sh
NODE_OPTIONS=--max-old-space-size=8192 ./gradlew build   # full build, tests, lint; jar in build/libs/guillotine.jar
NODE_OPTIONS=--max-old-space-size=8192 ./gradlew test --tests 'com.enonic.app.guillotine.mcp.*'
```

- Test compilation depends on the webpack build, so `test` needs the larger Node heap too; without it webpack can run out of memory.
- JDK toolchain is Java 25.
- `McpControllerGraalScriptTest` reruns the controller tests on GraalJS; keep both engines passing.

## MCP code

- `src/main/resources/mcp/content/content.yaml`: the tool descriptor. Names, descriptions and input/output schemas here are what the model sees; keep them in sync with the Java code and `docs/mcp.adoc`.
- `src/main/resources/mcp/content/content.js`: the XP controller; passes the call to `McpToolsBean`.
- `src/main/java/com/enonic/app/guillotine/mcp/`
  - `McpTools`: dispatch, the `mcp.enabled` check, and turning exceptions into error results.
  - `tools/`: one class per tool, plus helpers that know about content (`ContentGraphQl`, `ContentLinks`, `SearchQuery`, `QueryLimits`, `RichText`, `ContentSummary`).
  - `shape/`: tool-independent plumbing: result envelope (`McpResult`), size budget (`ResultBudget`), paging (`Cursors`, `Page`), project/branch resolution, `ToolError`, GraphQL error cleanup. `shape/` must not depend on `tools/`.
- Settings are `mcp.*` in `GuillotineConfig` / `GuillotineConfigService`, documented in `docs/configuration.adoc` (`[#mcp]`).

## Rules

- Do not change the behaviour of the GraphQL HTTP API. MCP reuses `SchemaProvider`/`GraphQLApi` in-process; additions there must be opt-in for MCP callers only.
- Tools are read-only and run with the caller's permissions. Never elevate the context.
- A caller mistake throws `ToolError` with a message that says what to change (the hint); it becomes an `isError` result. Never let an exception text or Java class name reach the model.
- Every in-process GraphQL execution passes `GraphQlErrors.hideInternalErrors()`: a field that fails with an exception other than a caller error (`GraphQlErrors.isCallerError`) gets a generic message, and the exception goes to the log.
- Every result whose size depends on data goes through a `ResultBudget` (`mcp.maxResultChars`). Paged results recompute `nextCursor` when items are dropped.
- Except for the `query` tool's own document, user input reaches GraphQL only as variables of constant documents.
- Structured results must not contain `null` inside arrays: XP drops such items. The one exception is `query` data, which stays as GraphQL returned it (documented under Known limitations).

## End-to-end checks

`mcp-e2e/` runs the tools on a real local XP with a fixed dataset; it is not part of the Gradle build. See `mcp-e2e/README.md` for prerequisites (an XP build with the MCP server) and the commands. `XP_EXPECT=fix` means XP built from the enonic/xp branch `issue-12662` (PR #12663); on a plain `epic/12627-xp-mcp-server` build use `XP_EXPECT=epic`. In short:

```sh
export XP_DIST=<xp checkout>/modules/runtime/build/install XP_EXPECT=fix
mcp-e2e/scripts/build-fixtures.sh && mcp-e2e/scripts/xp-start.sh && mcp-e2e/scripts/deploy.sh
mcp-e2e/scripts/run-all.sh
mcp-e2e/scripts/xp-stop.sh
```

## References

- `docs/mcp.adoc`: user documentation for the tools (arguments, limits, errors, workflows).
- XP side contract (endpoint `/api/system:mcp`, public tool names, error model): `modules/mcp/mcp-impl/README.md` in the enonic/xp MCP epic branch.
