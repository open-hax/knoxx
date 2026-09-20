#!/usr/bin/env bash
# Compile this checkout's preceding policy/auth runtime with owned fixture data.
set -euo pipefail
repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
if [[ $# -gt 1 || ( $# -eq 1 && "$1" != '--native' ) ]]; then
  printf '%s\n' 'Usage: scripts/verify-policy-provider-foundation.sh [--native]' >&2
  exit 2
fi
command -v clojure >/dev/null
command -v node >/dev/null
command -v rg >/dev/null
cd "$repo_root/backend"
node -e 'if (Number(process.versions.node.split(".")[0]) < 24) { console.error("Policy provider proof requires Node >=24; found " + process.version); process.exit(1); }'
if [[ "${1:-}" == '--native' ]]; then
  : "${KNOXX_TEST_MONGOD:?Set an absolute path to a qualified local MongoDB executable}"
  [[ "$KNOXX_TEST_MONGOD" == /* && -x "$KNOXX_TEST_MONGOD" ]] || exit 2
  [[ -f test/cljs/knoxx/backend/mongo_legacy_auth_e2e.cljs ]] || { printf '%s\n' 'Missing preceding-auth compatibility proof' >&2; exit 2; }
  export KNOXX_POLICY_NATIVE_PROOF=1
else
  export KNOXX_POLICY_NATIVE_PROOF=0
  printf '%s\n' 'WARN native Mongo, actual preceding HTTP auth and provider route compatibility are not selected; use --native to qualify them.'
fi
fixture_root=$(mktemp -d "${TMPDIR:-/tmp}/knoxx-policy-provider-proof.XXXXXX")
proof_pid=
cleanup() {
  if [[ -n "$proof_pid" ]]; then kill "$proof_pid" 2>/dev/null || true; wait "$proof_pid" 2>/dev/null || true; fi
  rm -rf -- "$fixture_root"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
export TMPDIR="$fixture_root" CONTRACTS_DIR="$repo_root/backend/test/fixtures/empty-contracts" NODE_ENV=test
log="$fixture_root/results.log"
printf 'Policy provider proof: %s at %s\n' "$repo_root" "$(git rev-parse HEAD)"
if [[ -n "$(git status --porcelain)" ]]; then printf '%s\n' 'WARN this run includes working-tree changes.'; fi
node --version
rm -rf -- target/policy-proof
clojure -M:cljs scripts/compile-policy-proof.clj 2>&1 | tee "$log"
node --require ./scripts/shadow-test-error-guard.cjs target/policy-proof/tests.cjs >> "$log" 2>&1 &
proof_pid=$!
result=0
wait "$proof_pid" || result=$?
proof_pid=
cat "$log"
[[ "$result" -eq 0 ]] || exit "$result"
rg -q '^Ran [1-9][0-9]* tests containing [1-9][0-9]* assertions\.' "$log"
rg -q '^0 failures, 0 errors\.' "$log"
if rg -q '^FAIL|^ERROR|\[shadow-test-guard\] FATAL|[1-9][0-9]* warnings' "$log"; then exit 1; fi
printf '%s\n' \
  'PASS omitted roles remain unchanged; explicit empty roles clear the membership and actor contract.' \
  'PASS assigned actor coordinates refuse reassignment; conflicting projections make no user or role changes.' \
  'PASS known invitation provisioning failures release only their own reservation; uncertain claims require recovery.'
if [[ "$KNOXX_POLICY_NATIVE_PROOF" == 1 ]]; then
  printf '%s\n' \
    'PASS preceding remote and local login/context/session/logout execute through real HTTP handlers and owned Mongo.' \
    'PASS actual preceding admin/invitation routes preserve classified failures and payload presence through the new providers.'
fi
printf '%s\n' 'WARN this provider layer retains preceding authentication admission. Verified-principal runtime activation and invitation UI remain a later layer.'
