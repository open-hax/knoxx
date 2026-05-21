#!/usr/bin/env bash
# Run the current Knoxx backend development loop in one terminal.
#
# This script starts:
#   1. shadow-cljs watch server-dev  -> dist-dev/server.js
#   2. the nbb dev launcher          -> imports dist-dev/server.js after it is runnable
#
# shadow-cljs owns hot reload. This helper only supervises both foreground
# processes and tears them down together.
set -euo pipefail

cd "$(dirname "$0")/.."

SHADOW_PID=""
SERVER_PID=""

cleanup() {
  local status=$?
  trap - EXIT INT TERM

  for pid in "$SERVER_PID" "$SHADOW_PID"; do
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
    fi
  done

  for pid in "$SERVER_PID" "$SHADOW_PID"; do
    if [[ -n "$pid" ]]; then
      wait "$pid" 2>/dev/null || true
    fi
  done

  exit "$status"
}
trap cleanup EXIT INT TERM

if ! command -v java >/dev/null 2>&1; then
  echo "[knoxx-backend-dev] java is required for shadow-cljs" >&2
  exit 1
fi

if ! command -v pnpm >/dev/null 2>&1; then
  corepack enable >/dev/null 2>&1 || true
  corepack prepare pnpm@10.14.0 --activate >/dev/null 2>&1 || true
fi

if ! command -v pnpm >/dev/null 2>&1; then
  echo "[knoxx-backend-dev] pnpm is required" >&2
  exit 1
fi

if [[ ! -d node_modules ]]; then
  echo "[knoxx-backend-dev] backend/node_modules missing; installing from lockfile"
  pnpm install --frozen-lockfile
fi

echo "[knoxx-backend-dev] starting shadow-cljs watch server-dev"
pnpm run watch &
SHADOW_PID="$!"

echo "[knoxx-backend-dev] starting CLJS dev server launcher"
pnpm exec nbb scripts/start-server-dev.cljs &
SERVER_PID="$!"

wait -n "$SHADOW_PID" "$SERVER_PID"
