#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
log_file=$(mktemp)
trap 'rm -f "$log_file"' EXIT
VITE_API_BASE=https://fixture.invalid \
VITE_KNOXX_DEV_USER_EMAIL=fixture@example.invalid \
VITE_KNOXX_DEV_ORG_SLUG=fixture-org \
  pnpm exec vite build --config vite.bridge.config.ts
pnpm exec shadow-cljs compile event-agent-proof >"$log_file" 2>&1
cat "$log_file"
if ! grep -q 'Ran 1 tests containing 8 assertions\.' "$log_file" ||
   ! grep -q '^0 failures, 0 errors\.' "$log_file"; then
  echo 'Event-agent bridge proof did not complete successfully' >&2
  exit 1
fi
