#!/usr/bin/env bash
# Builds this checkout's CMS HTTP adapter and runs a real loopback Fastify server.
# Auth principals are seeded at the existing auth-context seam; this does not
# verify password authentication, the deployment image, or the browser UI.
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"
command -v pnpm >/dev/null
command -v clojure >/dev/null
[[ -f backend/src/cljs/knoxx/backend/infra/cms_store.cljs ]]
fixture_root="$(mktemp -d)"
trap 'rm -rf -- "$fixture_root"' EXIT INT TERM
export KNOXX_CMS_VERIFY_ROOT="$fixture_root"
export CONTRACTS_DIR="$repo_root/backend/test/fixtures/empty-contracts"
log="$fixture_root/results.log"
printf 'Verifying checkout %s at %s\n' "$repo_root" "$(git rev-parse HEAD)"
pnpm -C backend exec shadow-cljs compile cms-history >"$log" 2>&1 || { cat "$log"; exit 1; }
cat "$log"
# Shadow can exit zero when an autorun test failed; counters are mandatory.
rg -q '^0 failures, 0 errors\.' "$log"
if rg -q '^FAIL|^ERROR|[1-9][0-9]* warnings' "$log"; then exit 1; fi
printf '%s\n' \
  'PASS authenticated CMS routes reject anonymous callers, read-only writes and cross-organization access.' \
  'PASS stale saves preserve both bodies and authenticated actors; explicit resolution retains their history.' \
  'PASS removing snapshots rebuilds identical EDN metadata and Markdown from Clio.' \
  'PASS publication and source digest checks refuse intervening ledger edits.' \
  'PASS logical paths reopen one history and descriptive metadata survives concurrent creation.' \
  'PASS migration retains the original JSON/Markdown and existing publication intent.' \
  'WARN password login and browser interactions are covered by the separate deployed browser tour.'
