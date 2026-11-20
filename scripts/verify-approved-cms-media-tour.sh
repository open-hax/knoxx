#!/usr/bin/env bash
# Native CMS source selection plus anonymous public artwork/music screenshots.
# Backend fixture state/process is owned and trapped by the main verifier.
# Prebuilt frontend is read-only; password login/providers/remote audio excluded.
set -euo pipefail
media_tour_repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
media_tour_mode="${1:---browser}"
case "$media_tour_mode" in --browser|--serve) ;; *) printf 'Usage: %s [--browser|--serve]\n' "$0" >&2; exit 2 ;; esac
: "${KNOXX_VERIFY_FRONTEND_DIST:?Set KNOXX_VERIFY_FRONTEND_DIST to the built native frontend directory.}"
[[ -f "$KNOXX_VERIFY_FRONTEND_DIST/index.html" ]] || { printf 'FAIL native frontend build is absent\n' >&2; exit 1; }
exec bash "$media_tour_repo/scripts/verify-approved-cms-media.sh" "$media_tour_mode"
