#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || $1 != /* || ! -x $1 ]]; then
  printf 'Usage: %s /absolute/path/to/mongod\n' "$0" >&2
  exit 2
fi
command -v setsid >/dev/null || { printf 'The verifier requires setsid for process cleanup.\n' >&2; exit 2; }

proof_pid=
cleanup_proof() {
  local status=$? attempt
  trap - EXIT
  trap '' INT TERM
  if [[ -n $proof_pid ]]; then
    kill -TERM -- "-$proof_pid" 2>/dev/null || kill -TERM "$proof_pid" 2>/dev/null || true
    for ((attempt = 0; attempt < 150; attempt++)); do
      kill -0 -- "-$proof_pid" 2>/dev/null || break
      sleep 0.1
    done
    kill -KILL -- "-$proof_pid" 2>/dev/null || true
    wait "$proof_pid" 2>/dev/null || true
  fi
  exit "$status"
}
trap cleanup_proof EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
printf 'Testing Knoxx checkout %s at %s\n' "$repo_root" "$(git -C "$repo_root" rev-parse HEAD)"
export KNOXX_VERIFY_NATIVE_INVITES=1
export KNOXX_TEST_MONGOD="$1"
setsid pnpm -C "$repo_root/backend" test:e2e &
proof_pid=$!
wait "$proof_pid"
proof_pid=
printf '%s\n' 'PASS Native Knoxx invite creation and redemption over HTTP against real MongoDB' \
  'PASS Both stored expiry formats work; expired, malformed and replayed invites are refused' \
  'PASS Only one concurrent redemption receives a session, and the invited email must match'
