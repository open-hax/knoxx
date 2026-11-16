#!/usr/bin/env bash
# Run the local MCP and control-route wire tests with deterministic fixtures.
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
printf 'Verifying Knoxx MCP source at %s\n' "$root"
pnpm -C "$root/backend" test:e2e
printf '%s\n' 'The local MCP test checks caller ownership, acknowledged event consumption, exact run observation, and rejection before stale or foreign steering.'
printf '%s\n' 'WARN  Subscription inference, browser OAuth, and production deployment require separate live verification.'
