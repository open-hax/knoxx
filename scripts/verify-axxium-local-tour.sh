#!/usr/bin/env bash
# Capture the local Axxium-delegated Knoxx login screen. Authenticated password
# entry is covered by verify-axxium-local.mjs without passing secrets via argv.
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
session="knoxx-axxium-local-tour"
shots="$repo/docs/verification/screenshots/axxium-local"
cleanup() { agent-browser --session "$session" close >/dev/null 2>&1 || true; }
trap cleanup EXIT INT TERM
command -v agent-browser >/dev/null || { echo 'FAIL agent-browser unavailable' >&2; exit 1; }
node "$repo/scripts/verify-axxium-local.mjs"
browser_launches() {
  agent-browser --session "$session" open 'about:blank' >/dev/null 2>&1
}
if ! browser_launches; then
  found=0
  for candidate in "$HOME"/.cache/ms-playwright/chromium-*/chrome-linux64/chrome \
                   /usr/bin/google-chrome /usr/bin/chromium /snap/bin/chromium; do
    [ -x "$candidate" ] || continue
    export AGENT_BROWSER_EXECUTABLE_PATH="$candidate"
    if browser_launches; then found=1; break; fi
  done
  [ "$found" -eq 1 ] || { echo 'FAIL no compatible Chromium browser launches' >&2; exit 1; }
fi
mkdir -p "$shots"
agent-browser --session "$session" open 'http://127.0.0.1:5176/' >/dev/null
agent-browser --session "$session" wait 'text=Axxium password' >/dev/null
agent-browser --session "$session" screenshot "$shots/01-login.png" >/dev/null
snapshot="$(agent-browser --session "$session" snapshot)"
printf '%s\n' "$snapshot"
if [[ "$snapshot" != *'Axxium password'* ]]; then
  echo 'FAIL Knoxx login screen does not name Axxium' >&2
  exit 1
fi
echo "PASS login screen names its Axxium authority; screenshot: $shots/01-login.png"
echo 'WARN Tour does not complete external Google account consent'
