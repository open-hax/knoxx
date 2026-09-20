#!/usr/bin/env bash
# Local source/review/Clio provider proof; no server, login or browser is started.
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root/backend"
command -v clojure >/dev/null
command -v node >/dev/null
command -v rg >/dev/null
export CONTRACTS_DIR="$repo_root/backend/test/fixtures/empty-contracts"
fixture_root="$(mktemp -d)"
trap 'rm -rf -- "$fixture_root"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
export TMPDIR="$fixture_root"
log="$fixture_root/results.log"
printf 'Source authority proof: %s based on %s\n' "$repo_root" "$(git rev-parse HEAD)"
if [[ -n "$(git status --porcelain)" ]]; then
  printf '%s\n' 'WARN working tree has local changes; this run compiles those changes.'
fi
clojure -M:cljs scripts/compile-source-proof.clj 2>&1 | tee "$log"
node --require ./scripts/shadow-test-error-guard.cjs target/source-authority-proof/tests.cjs 2>&1 | tee -a "$log"
rg -q '^Ran [1-9][0-9]* tests containing [1-9][0-9]* assertions\.' "$log"
rg -q '^0 failures, 0 errors\.' "$log"
if rg -q '^FAIL|^ERROR|\[shadow-test-guard\] FATAL|[1-9][0-9]* warnings' "$log"; then exit 1; fi
printf '%s\n' \
  'PASS scoped source and review history survives ledger reopening and disposable projection repair.' \
  'PASS selected acceptance facts share one resource snapshot; scoped IDs and timestamp-only retries preserve original facts.' \
  'PASS stale identities, corrupt history, conflicting retries and refused admission fail visibly.' \
  'PASS explicit-ID no-ops retain their original result; failed document writes preserve successor sequencing.' \
  'PASS stream/scope observers receive only selected stream names; observer failures cannot undo durable writes.' \
  'PASS source reads and saves use matching provenance; malformed wire locales and incomplete create facts are refused.' \
  'WARN Wiki HTTP, agent tools, provider composition, password login and browser workflows arrive in later layers.'
