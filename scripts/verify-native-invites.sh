#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || $1 != /* || ! -x $1 ]]; then
  printf 'Usage: %s /absolute/path/to/mongod\n' "$0" >&2
  exit 2
fi

repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
printf 'Testing Knoxx checkout %s at %s\n' "$repo_root" "$(git -C "$repo_root" rev-parse HEAD)"
export KNOXX_VERIFY_NATIVE_INVITES=1
export KNOXX_TEST_MONGOD="$1"
pnpm -C "$repo_root/backend" test:e2e
printf '%s\n' 'PASS Native Knoxx invite creation and redemption over HTTP against real MongoDB' \
  'PASS Both stored expiry formats work; expired, malformed and replayed invites are refused' \
  'PASS Only one concurrent redemption receives a session, and the invited email must match'
