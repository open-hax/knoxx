#!/usr/bin/env bash
set -euo pipefail

# Run the shadow-cljs compiler/watch server and the Node runtime as one PM2 app.
# The Node runtime connects to shadow-cljs over its dev WebSocket so CLJS
# namespace changes are pushed into the live process without PM2 restarts.

cd "$(dirname "$0")/../backend"

export NODE_ENV="${NODE_ENV:-development}"
export HOST="${HOST:-0.0.0.0}"
export PORT="${PORT:-8000}"
export NODE_OPTIONS="${NODE_OPTIONS:-} --enable-source-maps"

shadow_pid=""
node_pid=""

cleanup() {
  if [[ -n "$node_pid" ]] && kill -0 "$node_pid" 2>/dev/null; then
    kill "$node_pid" 2>/dev/null || true
  fi
  if [[ -n "$shadow_pid" ]] && kill -0 "$shadow_pid" 2>/dev/null; then
    kill "$shadow_pid" 2>/dev/null || true
  fi
  wait 2>/dev/null || true
}
trap cleanup EXIT INT TERM

rm -f dist/server.js

pnpm exec shadow-cljs --source-maps watch server &
shadow_pid="$!"

for _ in $(seq 1 120); do
  if [[ -s dist/server.js ]] && [[ -s .shadow-cljs/nrepl.port ]] && ss -ltn | grep -q ':9630'; then
    break
  fi
  if ! kill -0 "$shadow_pid" 2>/dev/null; then
    wait "$shadow_pid"
  fi
  sleep 1
done

node dist/server.js &
node_pid="$!"

wait -n "$shadow_pid" "$node_pid"
