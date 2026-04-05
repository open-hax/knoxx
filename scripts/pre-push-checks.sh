#!/usr/bin/env bash
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FAILURES=0

if [[ "${KNOXX_SKIP_PRE_PUSH:-0}" == "1" ]]; then
  echo "Skipping Knoxx pre-push checks because KNOXX_SKIP_PRE_PUSH=1"
  exit 0
fi

run_check() {
  local label="$1"
  shift
  echo
  echo "==> ${label}"
  if "$@"; then
    echo "✔ ${label}"
  else
    FAILURES=$((FAILURES + 1))
    echo "✘ ${label}"
  fi
}

run_check_in_dir() {
  local label="$1"
  local dir="$2"
  shift 2
  echo
  echo "==> ${label}"
  if (
    cd "$dir"
    "$@"
  ); then
    echo "✔ ${label}"
  else
    FAILURES=$((FAILURES + 1))
    echo "✘ ${label}"
  fi
}

echo "Knoxx pre-push: lint + typecheck"
echo "Repo: $ROOT_DIR"

run_check "repo size lint" node "$ROOT_DIR/scripts/lint-file-sizes.mjs"
run_check_in_dir "backend clj-kondo" "$ROOT_DIR/backend" pnpm run lint
run_check_in_dir "backend shadow-cljs compile" "$ROOT_DIR/backend" pnpm run typecheck
run_check_in_dir "ingestion clj-kondo" "$ROOT_DIR/ingestion" clj-kondo --lint src test
run_check_in_dir "frontend size lint" "$ROOT_DIR/frontend" pnpm run lint:size
run_check_in_dir "frontend typecheck" "$ROOT_DIR/frontend" pnpm run typecheck
run_check_in_dir "discord bot size lint" "$ROOT_DIR/discord-bot" pnpm run lint:size
run_check_in_dir "discord bot typecheck" "$ROOT_DIR/discord-bot" pnpm run typecheck

echo
if (( FAILURES > 0 )); then
  echo "Knoxx pre-push checks failed: ${FAILURES} check(s) failed."
  exit 1
fi

echo "Knoxx pre-push checks passed."
