#!/usr/bin/env bash
# Additive provider proof. Uses owned local ledgers, never a running Knoxx or Mongo.
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root/backend"
command -v clojure >/dev/null
command -v node >/dev/null
command -v rg >/dev/null
node -e 'if (Number(process.versions.node.split(".")[0]) < 24) { console.error("Identity foundation proof requires Node >=24; found " + process.version); process.exit(1); }'
export CONTRACTS_DIR="$repo_root/backend/test/fixtures/empty-contracts"
fixture_root="$(mktemp -d)"
trap 'rm -rf -- "$fixture_root"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
export TMPDIR="$fixture_root"
log="$fixture_root/results.log"
printf 'Identity foundation proof: %s based on %s\n' "$repo_root" "$(git rev-parse HEAD)"
if [[ -n "$(git status --porcelain)" ]]; then
  printf '%s\n' 'WARN working tree has local changes; this run compiles those changes.'
fi
node --version
# A failed compile must never reuse an earlier successful proof artifact.
rm -rf -- target/identity-foundation-proof
clojure -M:cljs scripts/compile-identity-foundation-proof.clj 2>&1 | tee "$log"
node --require ./scripts/shadow-test-error-guard.cjs target/identity-foundation-proof/tests.cjs 2>&1 | tee -a "$log"
rg -q '^Ran [1-9][0-9]* tests containing [1-9][0-9]* assertions\.' "$log"
rg -q '^0 failures, 0 errors\.' "$log"
if rg -q '^FAIL|^ERROR|\[shadow-test-guard\] FATAL|[1-9][0-9]* warnings' "$log"; then exit 1; fi
printf '%s\n' \
  'PASS real Clio directory initialization, observation, authorized reads and commands survive reopening.' \
  'PASS timestamp-only command retries return the first result; conflicting retries append no facts.' \
  'PASS role revocation survives initialization and observation; unauthorized reads and retries are refused.' \
  'PASS real Axxium logout invalidates the next local provider call; cached and delegated authority cannot bypass checks.' \
  'PASS canonical binding facts remain unchanged beside owned membership additions; foreign, conflicting and ambiguous selectors refuse.' \
  'PASS scoped profile changes preserve other tenants and require platform authority for shared user fields.' \
  'WARN this additive layer does not activate production identity, sessions, HTTP routes, Mongo policy selection or browser flows.'
