#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

NODE_PID=""

cleanup() {
  local status=$?
  trap - EXIT INT TERM

  if [[ -n "$NODE_PID" ]]; then
    kill "$NODE_PID" 2>/dev/null || true
    wait "$NODE_PID" 2>/dev/null || true
  fi

  exit "$status"
}
trap cleanup EXIT INT TERM

source_fingerprint() {
  find src/cljs src/server.mjs src/policy-db.mjs shadow-cljs.edn package.json \
    -type f -printf '%P %T@\n' 2>/dev/null \
    | sort \
    | sha256sum \
    | awk '{print $1}'
}

build_backend() {
  echo "[knoxx-backend-dev] building shadow-cljs release bundle"
  pnpm build
}

start_server() {
  echo "[knoxx-backend-dev] starting backend runtime"
  node src/server.mjs &
  NODE_PID="$!"
}

stop_server() {
  if [[ -n "$NODE_PID" ]]; then
    kill "$NODE_PID" 2>/dev/null || true
    wait "$NODE_PID" 2>/dev/null || true
    NODE_PID=""
  fi
}

echo "[knoxx-backend-dev] preparing pnpm toolchain"
corepack enable >/dev/null 2>&1 || true
corepack prepare pnpm@10.14.0 --activate >/dev/null 2>&1 || true

if ! command -v java >/dev/null 2>&1; then
  echo "[knoxx-backend-dev] java is required for shadow-cljs builds" >&2
  exit 1
fi

echo "[knoxx-backend-dev] installing backend dependencies"
pnpm install --ignore-workspace --no-frozen-lockfile

build_backend
start_server

current_fingerprint="$(source_fingerprint)"

while true; do
  sleep 1

  if [[ -n "$NODE_PID" ]] && ! kill -0 "$NODE_PID" 2>/dev/null; then
    echo "[knoxx-backend-dev] backend exited; restarting from current build"
    start_server
  fi

  next_fingerprint="$(source_fingerprint)"
  if [[ "$next_fingerprint" != "$current_fingerprint" ]]; then
    echo "[knoxx-backend-dev] source change detected; rebuilding"
    if build_backend; then
      stop_server
      start_server
    else
      echo "[knoxx-backend-dev] build failed; keeping last successful server if available" >&2
    fi
    current_fingerprint="$next_fingerprint"
  fi
done
