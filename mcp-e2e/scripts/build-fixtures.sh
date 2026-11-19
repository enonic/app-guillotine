#!/usr/bin/env bash
# Builds the fixture apps with the repository's Gradle wrapper. Outputs go to FIXTURE_BUILD_DIR, not into the source tree.
# usage: build-fixtures.sh [dataset-app|echo-app|broken-app ...]   (default: all three)
set -euo pipefail
source "$(dirname "$0")/xp-env.sh"
apps=("$@")
[ ${#apps[@]} -gt 0 ] || apps=(dataset-app echo-app broken-app)
for app in "${apps[@]}"; do
    "$REPO_ROOT/gradlew" -p "$MCP_E2E_DIR/fixtures/$app" \
        --project-cache-dir "$FIXTURE_BUILD_DIR/.gradle/$app" \
        -PfixtureBuildDir="$FIXTURE_BUILD_DIR/$app" build
    ls -l "$FIXTURE_BUILD_DIR/$app/libs/$app.jar"
done
