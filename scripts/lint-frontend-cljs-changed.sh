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
    ':(glob)frontend/src/**/*.clj' \
    ':(glob)frontend/src/**/*.cljs' \
    ':(glob)frontend/src/**/*.cljc' \
    ':(glob)frontend/test/**/*.clj' \
    ':(glob)frontend/test/**/*.cljs' \
    ':(glob)frontend/test/**/*.cljc' \
    ':(glob)shared/src/cljs/**/*.clj' \
    ':(glob)shared/src/cljs/**/*.cljs' \
    ':(glob)shared/src/cljs/**/*.cljc' > "$FILES_LIST"; then
  echo "Unable to compute changed frontend Clojure source files from $BASE_SHA." >&2
  exit 1
fi

mapfile -t FILES < "$FILES_LIST"

if (( ${#FILES[@]} == 0 )); then
  echo "No changed frontend Clojure source files to lint."
  exit 0
fi

for index in "${!FILES[@]}"; do
  case "${FILES[$index]}" in
    frontend/*) FILES[$index]="${FILES[$index]#frontend/}" ;;
    *) FILES[$index]="../${FILES[$index]}" ;;
  esac
done

cd "$REPO_ROOT/frontend"
clj-kondo --lint "${FILES[@]}" --config .clj-kondo/config.edn
