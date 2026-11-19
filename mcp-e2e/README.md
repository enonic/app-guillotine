# Guillotine MCP end-to-end checks

Scripts that run Guillotine's MCP content tools on a real local XP against a fixed dataset and compare every answer with a precomputed oracle. They are not part of the Gradle build; you run them by hand on your own machine.

```
scripts/   start/stop XP, build and deploy the fixture apps, run-all.sh, the live-client runner
checks/    the suites that run-all.sh calls; each prints PASS/FAIL lines and exits non-zero on a failure
fixtures/  dataset-app, echo-app, broken-app, expected.json and the script that computes it
prompts/   questions for the live-client runs, one per file
```

Everything the scripts write (XP home, fixture jars, logs, client transcripts) goes to `.tmp/mcp-e2e/` at the repository root. `.tmp/` is not in `.gitignore`, so add it to `.git/info/exclude` once.

## Prerequisites

- An XP 8 distribution with the MCP server, built from `epic/12627-xp-mcp-server` or from `issue-12662` (enonic/xp#12663, which adds fixes on top of the epic). In the XP checkout, run `./gradlew build -x check`. The install dir is `modules/runtime/build/install`.
- The JDK your XP build needs (these runs used Temurin 25), plus `python3`, `curl`, `lsof` and `flock`. Nothing else is needed: GraalJS ships as jars in the XP distribution and runs on a regular JDK.
- Guillotine built from this repository: `NODE_OPTIONS=--max-old-space-size=8192 ./gradlew build` produces `build/libs/guillotine.jar`.
- For the live-client pack: the Claude Code CLI (`claude`), signed in.

## Settings

All settings are environment variables with defaults in `scripts/xp-env.sh`.

| Variable | Default | Meaning |
|---|---|---|
| `XP_DIST` | none, required by `xp-start.sh` | XP install dir |
| `XP_EXPECT` | none, required by `run-all.sh` and `xp-smoke.sh` | `epic` or `fix`: which XP contract the checks expect |
| `XP_SCRIPT_ENGINE` | empty (Nashorn) | `GraalJS` starts XP with `-Dxp.script-engine=GraalJS` |
| `XP_HOME` | `.tmp/mcp-e2e/xp-home` | XP home; created from `$XP_DIST/home` on the first start. `xp-start.sh` also writes `mcp.defaultProject=mcp-test` to `config/com.enonic.app.guillotine.cfg` when that file is missing: without it an anonymous caller sees no project and every tool asks for `project` |
| `XP_URL` | `http://localhost:8080` | XP base URL; the MCP endpoint is `$XP_URL/api/system:mcp` |
| `GUILLOTINE_JAR` | `build/libs/guillotine.jar` | jar that the scripts deploy |
| `FIXTURE_BUILD_DIR` | `.tmp/mcp-e2e/build` | where `build-fixtures.sh` writes the fixture jars |
| `XP_SU_PASSWORD` | `mcp-e2e-local` | su password written into a new `XP_HOME`; for local use only |

`XP_EXPECT=epic` and `XP_EXPECT=fix` differ in three places. When a controller throws, the epic returns a JSON-RPC `-32603` error and the fix branch returns `isError` with an error id. On Nashorn, `JSON.stringify(args)` returns `undefined` on the epic and works on the fix branch. When another app has an invalid tool name, the epic breaks the whole MCP catalog and the fix branch leaves out only that app.

## Running the checks

```sh
export XP_DIST=~/path/to/xp/modules/runtime/build/install XP_EXPECT=fix
mcp-e2e/scripts/build-fixtures.sh     # dataset-app, echo-app, broken-app -> .tmp/mcp-e2e/build
mcp-e2e/scripts/xp-start.sh           # refuses to start if 8080, 4848 or 2609 is in use
mcp-e2e/scripts/deploy.sh             # deploys guillotine, dataset-app, echo-app; waits for "[mcp-dataset] done", fails on "failed"
mcp-e2e/scripts/run-all.sh
mcp-e2e/scripts/xp-stop.sh            # stops only the XP that xp-start.sh started (same PID and start time)
```

`run-all.sh` runs these suites, in this order:

- `tools-all.py`: the tool catalog, annotations and schemas, one successful call per tool, at least two bad calls per tool, paging to the last page, and truncation.
- `lifecycle`: removes and redeploys `guillotine.jar` and `echo-app.jar` without restarting XP, and checks that the tool list follows each change.
- `broken-app`: deploys an app whose tool name is too long and checks what that does to the catalog.
- `xp-smoke.sh`: the echo-app tools (dispatch, invalid arguments, a controller that throws, undeploy and redeploy).
- `tools-list-projects.sh` (deploys `GUILLOTINE_JAR` if the deployed jar differs), `tools-search.py`, `tools-references.py`: each tool's answers compared with `expected.json`.
- `dataset-check.py`: compares `expected.json` with Guillotine's HTTP GraphQL API, without going through MCP.

The steps that change the deploy folder, from `lifecycle` to `tools-list-projects.sh`, hold a lock. Before the lock is released, also when the run is interrupted, the script puts `guillotine.jar` and `echo-app.jar` back.

The read-only suites can also run on their own against any running XP that has the dataset, for example `python3 mcp-e2e/checks/tools-search.py`. These are `tools-all.py`, `tools-search.py`, `tools-references.py` and `dataset-check.py`. They read `XP_URL` and need nothing else.

### Reading the output

- `PASS` / `FAIL`: a check. Any `FAIL` makes the suite and `run-all.sh` exit non-zero.
- `XPDEF`: a known XP defect, recorded for information. It never fails the run.
- Indented lines: measurements and context, such as result sizes or how many seconds a tool took to appear.
- At the end: one `SUMMARY` line per suite, then `RESULT: PASS` or `RESULT: FAIL`.

`run-all.sh` also writes its output to `.tmp/mcp-e2e/logs/run-all-<timestamp>.log`. The XP log is `$XP_HOME/logs/console.log`.

## Live-client pack

```sh
mcp-e2e/scripts/run-client.sh                  # every prompt in prompts/
mcp-e2e/scripts/run-client.sh q1-articles-per-author q6-broken-links
```

Each prompt runs in a fresh `claude -p` process. Its only MCP server is the local XP, registered as `xp`, and every built-in tool is disabled. Your own Claude settings are not loaded. The caller is anonymous, so Guillotine reads the `master` branch of `mcp-test`. Each run has a budget cap of `CLAUDE_MAX_BUDGET_USD` (default 3). Set `CLAUDE_MODEL` to choose a model.

The output goes to `.tmp/mcp-e2e/client-runs/<timestamp>/`. For each prompt there is `<name>.jsonl` (the raw stream), `<name>.stderr`, and `<name>.md` with the question, the tool calls and their results, and the final answer. The script exits 1 when a prompt file is missing or a client run fails; it does not judge the answers. Compare the final answer by hand with the `# expected:` lines at the top of the prompt file. Most of those lines point into `fixtures/expected.json`. Q2 and Q4 need a signed-in user, so for an anonymous caller the expected answer is that the model explains why it cannot answer.

## Dataset

`fixtures/dataset-app` (`com.example.mcpdataset`) creates the project `mcp-test` when it starts. It reads `src/main/resources/dataset/dataset.json` and creates only what is missing, so redeploying it is safe:

- 3 users, `mcp-alice`, `mcp-bob` and `mcp-carol`, each an editor on the project. Their passwords in `dataset.json` are throwaway values for a local XP only. To change one, set `password.<login>=...` in `$XP_HOME/config/com.example.mcpdataset.cfg`.
- 4 folders and 4 persons.
- 12 images. 10 are published, and 3 published images are not referenced by anything.
- 30 articles with authors, tags, cover images and images in the body. 19 are published. 3 are in workflow `IN_PROGRESS`. 6 are later edited by a different user.
- 2 landing pages with broken links. One links to an article that was deleted after the link was made. The other points to an image id that never existed.

The project is public on `master`. Anonymous callers cannot read `draft`.

`fixtures/compute-expected.py` computes `fixtures/expected.json` from `dataset.json` alone, without XP, Guillotine or MCP code. The result holds articles per author, recently modified content per user, unused images, content in progress, counts per type, totals and broken links, for both branches. Content ids are random, so everything is keyed by path. When you change `dataset.json`, run `python3 mcp-e2e/fixtures/compute-expected.py` and commit both files. `dataset-check.py` then confirms the oracle against the live index.

`fixtures/echo-app` (`com.example.echo`) has four small tools for the XP-level checks: `echo`, `fail`, `undefined-field` and `args-probe`. `fixtures/broken-app` (`com.example.broken`) has a valid tool next to one whose public name is longer than 128 characters.

The fixture apps have their own Gradle builds, which `build-fixtures.sh` runs with the repository's wrapper. They are not part of Guillotine's build.
