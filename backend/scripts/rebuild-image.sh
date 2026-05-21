#!/usr/bin/env bash
# Build the current Knoxx backend runtime image.
#
# Current backend runtime:
#   shadow-cljs release server -> dist/server.js
#   Docker CMD                 -> node dist/server.js
#
# Optional compose restart:
#   KNOXX_BACKEND_COMPOSE_FILE=/path/to/compose.yml \
#   KNOXX_BACKEND_COMPOSE_SERVICE=knoxx-backend \
#     backend/scripts/rebuild-image.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"
IMAGE="${KNOXX_BACKEND_IMAGE:-knoxx-backend:latest}"
COMPOSE_FILE="${KNOXX_BACKEND_COMPOSE_FILE:-}"
COMPOSE_SERVICE="${KNOXX_BACKEND_COMPOSE_SERVICE:-knoxx-backend}"
SKIP_COMPILE="${KNOXX_BACKEND_SKIP_COMPILE:-0}"
SKIP_DOCKER="${KNOXX_BACKEND_SKIP_DOCKER:-0}"

cd "$BACKEND_DIR"

echo "=== Knoxx backend image helper ==="
echo "Backend dir: $BACKEND_DIR"
echo "Image:       $IMAGE"
echo

if ! command -v pnpm >/dev/null 2>&1; then
  corepack enable >/dev/null 2>&1 || true
  corepack prepare pnpm@10.14.0 --activate >/dev/null 2>&1 || true
fi

if ! command -v pnpm >/dev/null 2>&1; then
  echo "pnpm is required" >&2
  exit 1
fi

if [[ "$SKIP_COMPILE" != "1" ]]; then
  if ! command -v java >/dev/null 2>&1; then
    echo "java is required for shadow-cljs builds" >&2
    exit 1
  fi

  if [[ ! -d node_modules ]]; then
    echo ">>> Installing backend dependencies from lockfile"
    pnpm install --frozen-lockfile
    echo
  fi

  echo ">>> Compiling backend (:server)"
  pnpm run build
  echo
else
  echo ">>> Skipping compile because KNOXX_BACKEND_SKIP_COMPILE=1"
  echo
fi

if [[ ! -f dist/server.js ]]; then
  echo "Expected dist/server.js to exist. Run: pnpm -C backend run build" >&2
  exit 1
fi

if [[ "$SKIP_DOCKER" != "1" ]]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo "docker is required unless KNOXX_BACKEND_SKIP_DOCKER=1" >&2
    exit 1
  fi

  echo ">>> Building Docker image"
  docker build -t "$IMAGE" .
  echo
else
  echo ">>> Skipping Docker build because KNOXX_BACKEND_SKIP_DOCKER=1"
  echo
fi

if [[ -n "$COMPOSE_FILE" ]]; then
  echo ">>> Restarting compose service"
  docker compose -f "$COMPOSE_FILE" up -d "$COMPOSE_SERVICE"
  docker compose -f "$COMPOSE_FILE" logs "$COMPOSE_SERVICE" --tail=20
else
  cat <<EOF
No compose restart requested.

To restart a compose-managed backend, rerun with:
  KNOXX_BACKEND_COMPOSE_FILE=/path/to/compose.yml \\
  KNOXX_BACKEND_COMPOSE_SERVICE=$COMPOSE_SERVICE \\
    $SCRIPT_DIR/rebuild-image.sh
EOF
fi

echo
echo "=== Knoxx backend image helper complete ==="
