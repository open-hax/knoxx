#!/usr/bin/env bash
# Read-only tour of an existing document. Uses an already authenticated browser
# session; creates no application data. Run verify-cms-history.sh for isolated
# write/replay/authorization fixtures. This tour does not test password login.
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
: "${KNOXX_FRONTEND_URL:?Set the verified deployment origin}"
: "${KNOXX_DOCUMENT_TITLE:?Set the exact existing document title}"
: "${KNOXX_BROWSER_SESSION:?Set an authenticated agent-browser session}"
shot_dir="${KNOXX_SHOT_DIR:-$repo_root/docs/verification/screenshots}"
mkdir -p "$shot_dir"
command -v agent-browser >/dev/null
ab() { agent-browser --session "$KNOXX_BROWSER_SESSION" "$@"; }
ab open "$KNOXX_FRONTEND_URL/cms"
ab find role button click --name "$KNOXX_DOCUMENT_TITLE" --exact
ab screenshot "$shot_dir/clio-cms-document.png"
ab find role button click --name History --exact
ab screenshot "$shot_dir/clio-cms-history.png"
printf '%s\n' 'PASS opened the existing CMS document and its history without modifying content.' \
  'WARN this read-only tour reuses a session; fresh login and conflict writes need the separate acceptance walkthrough.'
