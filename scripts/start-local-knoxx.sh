#!/usr/bin/env bash
set -euo pipefail

KNOXX_SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
KNOXX_DIR="$(cd "$KNOXX_SCRIPT_DIR/.." && pwd)"
# shellcheck disable=SC1091
source "$KNOXX_SCRIPT_DIR/lib/local-runtime-env.sh"

configure_knoxx_local_runtime

cd "$KNOXX_DIR"
KNOXX_BUILD_MARKER="$(mktemp)"
pnpm -C backend run watch &
KNOXX_SHADOW_PID=$!

cleanup() {
  kill "$KNOXX_SHADOW_PID" 2>/dev/null || true
  wait "$KNOXX_SHADOW_PID" 2>/dev/null || true
  rm -f "$KNOXX_BUILD_MARKER"
}
trap cleanup EXIT INT TERM

KNOXX_BUILD_DEADLINE=$((SECONDS + 120))
KNOXX_SERVER_ENTRY="$KNOXX_DIR/backend/dist-dev/server.js"
KNOXX_CLJS_CORE="$KNOXX_DIR/backend/dist-dev/cljs-runtime/cljs.core.js"

while [[ ! -f "$KNOXX_SERVER_ENTRY" || ! -f "$KNOXX_CLJS_CORE" \
         || ! "$KNOXX_SERVER_ENTRY" -nt "$KNOXX_BUILD_MARKER" \
         || ! "$KNOXX_CLJS_CORE" -nt "$KNOXX_BUILD_MARKER" ]]; do
  if (( SECONDS >= KNOXX_BUILD_DEADLINE )); then
    printf 'ERROR  timed out waiting for a complete fresh server-dev build\n' >&2
    exit 1
  fi
  if ! kill -0 "$KNOXX_SHADOW_PID" 2>/dev/null; then
    printf 'ERROR  shadow-cljs exited before server-dev was complete\n' >&2
    exit 1
  fi
  sleep 0.5
done

pnpm -C backend run start:dev
