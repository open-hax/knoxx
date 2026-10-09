#!/usr/bin/env bash
# Builds this checkout's CMS HTTP adapter and runs a real loopback Fastify server.
# Auth principals are seeded at the existing auth-context seam; this does not
# verify password authentication, the deployment image, or the browser UI.
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"
command -v pnpm >/dev/null
command -v clojure >/dev/null
command -v node >/dev/null
[[ -f backend/src/cljs/knoxx/backend/infra/cms_store.cljs ]]
fixture_root="$(mktemp -d)"
trap 'rm -rf -- "$fixture_root"' EXIT INT TERM
export KNOXX_CMS_VERIFY_ROOT="$fixture_root"
export CONTRACTS_DIR="$repo_root/backend/test/fixtures/empty-contracts"
export NODE_OPTIONS="$(node --input-type=module -e 'import { testEnvironment } from "./backend/scripts/shadow-test-environment.mjs"; process.stdout.write(testEnvironment().NODE_OPTIONS);')"
log="$fixture_root/results.log"
printf 'Verifying checkout %s at %s\n' "$repo_root" "$(git rev-parse HEAD)"
pnpm -C backend exec shadow-cljs compile cms-history >"$log" 2>&1 || { cat "$log"; exit 1; }
cat "$log"
# Shadow can print green counters after an unhandled rejection; use its guard.
node --input-type=module - "$log" <<'JS'
import { readFileSync } from 'node:fs';
import { testCountersExitCode } from './backend/scripts/run-shadow-tests-ci.mjs';

const output = readFileSync(process.argv[2], 'utf8');
if (testCountersExitCode(output) !== 0 || /\b[1-9][0-9]* warnings?\b/.test(output)) {
  console.error('FAIL native CMS HTTP tests did not complete with guarded green counters and zero warnings.');
  process.exit(1);
}
JS
printf '%s\n' \
  'PASS authenticated CMS routes reject anonymous callers, read-only writes and cross-organization access.' \
  'PASS stale saves preserve both bodies and authenticated actors; explicit resolution retains their history.' \
  'PASS removing snapshots rebuilds identical EDN metadata and Markdown from Clio.' \
  'PASS publication and source digest checks refuse intervening ledger edits.' \
  'PASS logical paths reopen one history and descriptive metadata survives concurrent creation.' \
  'PASS migration retains the original JSON/Markdown and existing publication intent.' \
  'WARN password login and browser interactions are covered by the separate deployed browser tour.'
