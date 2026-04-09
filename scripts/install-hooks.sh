#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

git -C "$ROOT_DIR" config core.hooksPath .githooks

echo "Configured Knoxx git hooks: core.hooksPath=.githooks"
