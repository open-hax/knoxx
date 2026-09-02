#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE_SHA="${KNOXX_LINT_BASE_SHA:-}"

if [[ -z "$BASE_SHA" ]]; then
  BASE_SHA="$(git -C "$REPO_ROOT" merge-base HEAD origin/main 2>/dev/null || true)"
fi

if [[ -z "$BASE_SHA" ]]; then
  echo "Unable to determine a base revision for changed-surface frontend lint." >&2
  exit 1
fi

FILES_LIST="$(mktemp)"
trap 'rm -f "$FILES_LIST"' EXIT

if ! git -C "$REPO_ROOT" diff --name-only --diff-filter=ACMR "$BASE_SHA"...HEAD -- \
    'frontend/src/cljs/**/*.cljs' \
    'frontend/src/cljs/**/*.cljc' \
    'frontend/test/cljs/**/*.cljs' \
    'frontend/test/cljs/**/*.cljc' > "$FILES_LIST"; then
  echo "Unable to compute changed frontend CLJS/CLJC files from $BASE_SHA." >&2
  exit 1
fi

mapfile -t FILES < "$FILES_LIST"

if (( ${#FILES[@]} == 0 )); then
  echo "No changed frontend CLJS/CLJC files to lint."
  exit 0
fi

for index in "${!FILES[@]}"; do
  FILES[$index]="${FILES[$index]#frontend/}"
done

cd "$REPO_ROOT/frontend"
clj-kondo --lint "${FILES[@]}" --config .clj-kondo/config.edn
