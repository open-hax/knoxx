#!/usr/bin/env bash
# Render local password and actor consent from the isolated MCP login fixture.
set -euo pipefail
root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
: "${CHROME_BIN:?Set CHROME_BIN to a local Chromium executable}"
test -x "$CHROME_BIN"
export KNOXX_SHOT_DIR="${KNOXX_SHOT_DIR:-$(mktemp -d -t knoxx-mcp-login-shots.XXXXXX)}"
mkdir -p "$KNOXX_SHOT_DIR"
KNOXX_SHOT_DIR=$(realpath "$KNOXX_SHOT_DIR")
KNOXX_MCP_LOGIN_TOUR_PORT=$(node -e 'const s=require("node:net").createServer();s.listen(0,"127.0.0.1",()=>{console.log(s.address().port);s.close()})')
export KNOXX_MCP_LOGIN_TOUR_PORT CONTRACTS_DIR=test/fixtures/empty-contracts
log=$(mktemp)
trap 'rm -- "$log"' EXIT
pnpm -C "$root/backend" exec shadow-cljs --config-merge '{:ns-regexp "^knoxx[.]backend[.]e2e[.]mcp-local-login-e2e$" :output-to "target/mcp-login-tour/e2e.cjs"}' compile e2e | tee "$log"
grep -q '^Ran 1 tests containing ' "$log"
grep -q '^0 failures, 0 errors\.$' "$log"
test -s "$KNOXX_SHOT_DIR/mcp-local-password.png"
test -s "$KNOXX_SHOT_DIR/mcp-actor-consent.png"
printf 'PASS  Local password sign-in returned to the actor consent page. Screenshots in %s\n' "$KNOXX_SHOT_DIR"
