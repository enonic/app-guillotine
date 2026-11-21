#!/usr/bin/env bash
# Builds the dataset app with the repository's Gradle wrapper. Output goes to FIXTURE_BUILD_DIR, not into the source tree.
set -euo pipefail
source "$(dirname "$0")/xp-env.sh"
"$REPO_ROOT/gradlew" -p "$MCP_E2E_DIR/fixtures/dataset-app" \
    --project-cache-dir "$FIXTURE_BUILD_DIR/.gradle/dataset-app" \
    -PfixtureBuildDir="$FIXTURE_BUILD_DIR/dataset-app" build
ls -l "$DATASET_JAR"
