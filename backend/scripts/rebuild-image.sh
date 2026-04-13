#!/usr/bin/env bash
# Knoxx Backend Image Rebuild Script
#
# This script rebuilds the knoxx-knoxx-backend Docker image.
# Run from the workspace root: ./orgs/open-hax/openplanner/packages/knoxx/backend/scripts/rebuild-image.sh
#
# Prerequisites:
#   - Java 11+ installed (for shadow-cljs)
#   - Docker installed
#   - pnpm installed
#
# What this script does:
#   1. Compiles CLJS to dist/app.js using shadow-cljs
#   2. Builds the Docker image knoxx-knoxx-backend:latest
#   3. Restarts the knoxx-backend container via docker compose
#
# Architecture:
#   - CLJS source: src/cljs/knoxx/backend/*.cljs
#   - JS bootstrap: src/server.mjs (imports from dist/app.js)
#   - Compiled output: dist/app.js
#   - Container mounts dist/ for hot-reload during dev

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"
WORKSPACE_ROOT="$(cd "$BACKEND_DIR/../../../.." && pwd)"
COMPOSE_DIR="$WORKSPACE_ROOT/services/openplanner"

echo "=== Knoxx Backend Rebuild ==="
echo "Backend dir: $BACKEND_DIR"
echo "Workspace root: $WORKSPACE_ROOT"
echo "Compose dir: $COMPOSE_DIR"
echo

# Step 1: Compile CLJS
echo ">>> Step 1: Compiling CLJS..."
cd "$BACKEND_DIR"
if command -v shadow-cljs &> /dev/null; then
    shadow-cljs release app
else
    npx shadow-cljs release app
fi
echo "CLJS compilation complete."
echo

# Step 2: Build Docker image
echo ">>> Step 2: Building Docker image..."
docker build -t knoxx-knoxx-backend:latest .
echo "Docker image built."
echo

# Step 3: Restart container
echo ">>> Step 3: Restarting knoxx-backend container..."
cd "$COMPOSE_DIR"
docker compose up -d knoxx-backend
echo "Container restarted."
echo

# Step 4: Show logs
echo ">>> Step 4: Container logs (last 20 lines)..."
docker compose logs knoxx-backend --tail=20
echo

echo "=== Rebuild complete ==="
echo "To follow logs: docker compose -f $COMPOSE_DIR/docker-compose.yml logs -f knoxx-backend"
