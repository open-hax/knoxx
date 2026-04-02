#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "Starting backend on http://0.0.0.0:8000"
(
  cd "$ROOT_DIR/backend"
  uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
) &
BACK_PID=$!

trap 'kill $BACK_PID 2>/dev/null || true' EXIT

echo "Starting frontend on http://0.0.0.0:5173"
cd "$ROOT_DIR/frontend"
npm run dev
