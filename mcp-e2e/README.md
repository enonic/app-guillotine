# Guillotine MCP end-to-end checks

Scripts that run Guillotine's five MCP tools on a real local XP against a fixed dataset, as an anonymous and as a signed-in caller, and compare every answer with a precomputed oracle. They are not part of the Gradle build; you run them by hand.

```
scripts/   start/stop XP, build and deploy the dataset app, sign in, run-all.sh
checks/    the suites that run-all.sh calls; each prints PASS/FAIL lines and exits non-zero on a failure
fixtures/  dataset-app, expected.json and the script that computes it
```

Everything the scripts write (XP home, fixture jar, logs) goes to `.tmp/mcp-e2e/` at the repository root. `.tmp/` is not in `.gitignore`, so add it to `.git/info/exclude` once.

## Prerequisites

- An XP 8 distribution with the MCP server, built from the XP MCP epic `epic/12627-xp-mcp-server`. In the XP checkout, run `./gradlew build -x check`; the install dir is `modules/runtime/build/install`.
- The standard ID provider app, which serves the sign-in endpoint. XP development builds do not bundle it. `deploy.sh` takes `app-standardidprovider-<version>.jar` from `~/.enonic/distributions/*/system/40/` (any XP SDK the Enonic CLI downloaded), or from `IDPROVIDER_JAR`.
- The JDK your XP build needs, plus `python3`, `curl`, `lsof` and `flock`.
- Guillotine built from this repository: `NODE_OPTIONS=--max-old-space-size=8192 ./gradlew build` produces `build/libs/guillotine.jar`.

## Settings

All settings are environment variables with defaults in `scripts/xp-env.sh`. The scripts never read `XP_HOME` or `XP_URL` from the environment, so a shell that exports them for your own XP does not redirect the run there; they set both from `MCP_E2E_XP_HOME` and `MCP_E2E_XP_URL`.

| Variable | Default | Meaning |
|---|---|---|
| `XP_DIST` | none, required by `xp-start.sh` | XP install dir |
| `XP_SCRIPT_ENGINE` | empty (Nashorn) | `GraalJS` starts XP with `-Dxp.script-engine=GraalJS` |
| `MCP_E2E_XP_HOME` | `.tmp/mcp-e2e/xp-home` | XP home, exported as `XP_HOME`; created from `$XP_DIST/home` on the first start |
| `MCP_E2E_XP_URL` | `http://localhost:8080` | XP base URL, exported as `XP_URL`; the MCP endpoint is `$XP_URL/api/system:mcp` |
| `GUILLOTINE_JAR` | `build/libs/guillotine.jar` | jar that `deploy.sh` deploys |
| `IDPROVIDER_JAR` | newest `~/.enonic/distributions/*/system/40/app-standardidprovider-*.jar` | standard ID provider jar |
| `FIXTURE_BUILD_DIR` | `.tmp/mcp-e2e/build` | where `build-fixtures.sh` writes the dataset app jar |
| `MCP_E2E_USER`, `MCP_E2E_PASSWORD` | `mcp-alice` and her password from `dataset.json` | the signed-in caller |
| `XP_SU_PASSWORD` | `mcp-e2e-local` | su password written into a new XP home; for local use only |

`xp-start.sh` and `deploy.sh` write `$XP_HOME/config/com.enonic.app.guillotine.cfg` when it is missing:

```properties
mcp.defaultProject=mcp-test
```

Anonymous callers may use every tool unless `mcp.allowAnonymous=false` is set, and without a default project an anonymous caller sees no project.

## Running the checks

```sh
export XP_DIST=~/path/to/xp/modules/runtime/build/install
mcp-e2e/scripts/build-fixtures.sh     # dataset-app -> .tmp/mcp-e2e/build
mcp-e2e/scripts/xp-start.sh           # refuses to start if 8080, 4848 or 2609 is in use
mcp-e2e/scripts/deploy.sh             # standard ID provider, guillotine, dataset-app; waits for the dataset, sign-in and the tools
mcp-e2e/scripts/run-all.sh
mcp-e2e/scripts/xp-stop.sh            # stops only the XP that xp-start.sh started (same PID and start time)
```

`run-all.sh` signs in as `MCP_E2E_USER` and runs, in this order:

- `catalog.py`, anonymous and signed-in: 5 tools in 3 groups, public names `com.enonic.app.guillotine__<group>__<tool>`, annotations, input and output schemas, `alwaysLoad` on `list-projects` and `query`.
- `tools.py`, anonymous and signed-in: one successful call per tool, at least two failing calls per tool with a readable hint, the result envelope (text equals `structuredContent`), cursors of `find-broken-references`, and the `query` hints for common mistakes. `describe-type` for `article`, `person` and `landing` on each branch the caller reads, with each of its example queries run through `query` on that branch: no errors, within `mcp.maxResultChars`, and the first example's `totalCount` equal to the type's `count`. The `references` tools run on `draft` signed in and on `master` anonymously.
- `answers.py`, anonymous and signed-in: the six questions (articles per author, my recent content, unused images, content in progress, items per type, broken links on landing pages), asked through the tools as `docs/mcp.adoc` describes and compared with `expected.json`, on `draft` and `master` when signed in and on `master` when anonymous.
- `access.py open` and, after the config is rewritten with `mcp.allowAnonymous=false`, `access.py closed`: anonymous callers use every tool on `master` by default and none with `false`; signed-in callers are not affected; a signed-out session counts as anonymous.
- `permissions.py`: the permission matrix. It signs in as each dataset user and as `su` itself and calls all five tools on `draft` and `master` as anonymous, `mcp-dave` (no project role), `mcp-erin` (viewer), `mcp-alice` (editor), `mcp-frank` (editor who cannot read `/restricted`), `mcp-carol` (editor who can read `/team` and `/restricted`) and `su`. Counts, paths, full text, buckets and references cover only what the caller can read, and a hidden item answers exactly like one that does not exist.
- `elevation.py`: the code paths that elevate over HTTP and the extension fields. Each document goes to the `query` tool and to Guillotine's HTTP endpoint with the same session. The query tool never shows more than HTTP; for `mcp-frank` it shows no default page template he cannot read and treats an unreadable `scopeKey` as missing, while HTTP still elevates (printed, not checked). URLs that XP builds with an administrator role must equal the HTTP answer; what they show is printed. The dataset app's extension resolvers see the caller and answer as over HTTP, and `describe-type` lists them under `appFields`.
- `budget.py`, anonymous and signed-in, with `mcp.maxResultChars=2000`: a cut `query` result with its hint, and a `describe-type` that cannot fit. The `nextCursor` after a cut is covered by `FindBrokenReferencesToolTest`.
- `redeploy`: removes `guillotine.jar` and puts it back without restarting XP; the tools and the HTTP GraphQL API go and come back.
- `dataset-check.py`: compares `expected.json` with Guillotine's HTTP GraphQL API, without going through MCP.
- `server log`: no Guillotine or MCP `ERROR` line in `console.log` during the run.

Config and deploy-folder changes hold a lock. When the run ends, also when it is interrupted, Guillotine's config file and `guillotine.jar` are put back as they were.

The suites can also run on their own against any running XP with the dataset and anonymous callers allowed, for example `python3 -B mcp-e2e/checks/answers.py` (`-B` keeps `__pycache__` out of the source tree). Run on their own, they read `XP_URL`, `MCP_URL` and `XP_HOME` from the environment, unlike `login.sh`, so point `XP_URL` at the same XP when you pass them a cookie (see below); set `MCP_COOKIE` to the output of `scripts/login.sh` to run them signed in. `access.py` needs `ACCESS_COOKIE` instead, `permissions.py` and `elevation.py` sign in by themselves (dataset passwords with the same overrides as the dataset app, `su` with `XP_SU_PASSWORD`), and `budget.py` needs `mcp.maxResultChars=2000`.

### Reading the output

- `PASS` / `FAIL`: a check, prefixed with the caller. Any `FAIL` makes the suite and `run-all.sh` exit non-zero.
- Indented lines: measurements and context.
- At the end: one `SUMMARY` line per suite, then `RESULT: PASS` or `RESULT: FAIL`.

`run-all.sh` also writes its output to `.tmp/mcp-e2e/logs/run-all-<timestamp>.log`. The XP log is `$XP_HOME/logs/console.log`.

### Signing in by hand

```sh
cookie=$(mcp-e2e/scripts/login.sh)              # JSESSIONID=...; or: login.sh <user> <password>
env -u MCP_URL MCP_COOKIE="$cookie" XP_URL="${MCP_E2E_XP_URL:-http://localhost:8080}" python3 -B mcp-e2e/checks/tools.py
mcp-e2e/scripts/login.sh logout "$cookie"
```

`login.sh` posts `{"action":"login","user":...,"password":...}` to `/_/idprovider/system`; XP answers with a `JSESSIONID` cookie, and every MCP request that sends it as a `Cookie` header runs as that user.

## Dataset

`fixtures/dataset-app` (`com.example.mcpdataset`) creates the project `mcp-test` when it starts. It reads `src/main/resources/dataset/dataset.json` and creates only what is missing, so redeploying it is safe:

- 6 users. `mcp-alice`, `mcp-bob`, `mcp-carol` and `mcp-frank` are editors on the project, `mcp-erin` is a viewer, and `mcp-dave` has no project role. Their passwords in `dataset.json` are throwaway values for a local XP only. To change one, set `password.<login>=...` in `$XP_HOME/config/com.example.mcpdataset.cfg`.
- 4 folders and 4 persons.
- 12 images. 10 are published, and 3 published images are not referenced by anything.
- 30 articles with authors, tags, cover images and images in the body. 19 are published. 3 are in workflow `IN_PROGRESS`. 6 are later edited by a different user.
- 2 landing pages with broken links. One links to an article that was deleted after the link was made. The other points to an image id that never existed.

- 2 restricted folders with their own ACL, for `permissions.py`, created by `su` and published. `/team` (a folder, `team-article` and `team-image.png`) is readable by `mcp-carol` and `mcp-frank`; `/restricted` (a folder, `secret-note` and `secret-image.png`) only by `mcp-carol`. Project roles and anonymous callers read neither, administrators read both. `team-article` uses `secret-image.png` as cover and in its body and links to `secret-note`; `secret-note` is the only item that uses `team-image.png`.

- A page descriptor `main` and schema extensions in `guillotine/guillotine.js`, for `elevation.py`: `viewer` on articles (the caller's key), and `readableArticleCount` and `articleCountAsAdmin` on `guillotine`, the second of which counts as administrator on purpose.
- A second project, `mcp-sec`, for `elevation.py`, so that `mcp-test` stays as `expected.json` describes it: `mcp-frank` and `mcp-carol` are its viewers. It has the site `/site` with the article `site-article` and the page template `_templates/hidden-template` for articles, on `draft` only. The templates folder and everything below it is readable only by `mcp-carol`, administrators and Content Studio administrators.

`mcp-test` is public on `master`. Anonymous callers and users without a project role cannot read `draft`.

`expected.json` describes the content outside `/team` and `/restricted`, which is what anonymous callers and `mcp-alice` see; `permissions.py` adds the restricted items per caller.

`fixtures/compute-expected.py` computes `fixtures/expected.json` from `dataset.json` alone, without XP, Guillotine or MCP code. Content ids are random, so everything is keyed by path. When you change `dataset.json`, run `python3 mcp-e2e/fixtures/compute-expected.py` and commit both files. `dataset-check.py` then confirms the oracle against the live index.

The dataset app has its own Gradle build, which `build-fixtures.sh` runs with the repository's wrapper. It is not part of Guillotine's build.
