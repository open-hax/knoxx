#!/usr/bin/env bash
# Native engine regression proof, or authenticated live Docker proof.
# Reads no .env files. Live mode needs an operator-supplied KNOXX_MCP_TOKEN.
set -euo pipefail
task_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec node "$task_root/scripts/verify-native-music.mjs" "$@"
