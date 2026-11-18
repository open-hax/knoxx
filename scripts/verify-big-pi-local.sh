#!/usr/bin/env bash
# The integration suite creates isolated data directories and owns its test servers.
# This verifies the local compatibility profile, not a customer's Optimizely tenant.
set -euo pipefail
big_pi_repo="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
big_pi_package="$big_pi_repo/packages/big-pi-optimizely"
if [[ ! -x "$big_pi_package/node_modules/.bin/nbb" ]]; then
  printf 'Missing dependencies. Run: npm ci --ignore-scripts --prefix %q\n' "$big_pi_package" >&2
  exit 1
fi
printf 'Verify: pure CMS laws, source provenance and draft-only bridge behavior.\n'
npm --prefix "$big_pi_package" test
printf 'Verify: provenance round-trips through the actual current Knoxx HTTP codec and body law.\n'
npm --prefix "$big_pi_package" run test:knoxx-contract
printf 'Verify: live HTTP, real MCP SDK clients, persistence and failure behavior.\n'
npm --prefix "$big_pi_package" run test:integration
printf 'PASS: local Big Pi compatibility tests completed.\n'
printf 'WARN: browser tour is separate: npm run test:browser --prefix packages/big-pi-optimizely.\n'
printf 'WARN: customer tenant, complete vendor API, Graph, publication and live Knoxx qualification are separate gates.\n'
