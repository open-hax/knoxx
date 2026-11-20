#!/usr/bin/env bash
# Opt-in live Claude Code canary on isolated local MCP and Clio fixtures.
set -euo pipefail
root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
command -v claude >/dev/null
: "${KNOXX_CLAUDE_HOME:?Set KNOXX_CLAUDE_HOME to an enrolled, private Claude profile}"
export KNOXX_NATIVE_CLAUDE_CANARY=1
printf 'Verifying Claude Code through isolated Knoxx MCP from %s\n' "$root"
pnpm -C "$root/backend" test:e2e
printf '%s\n' 'PASS  Claude accepted an MCP steer and Knoxx persisted the exact reply and usage.'
