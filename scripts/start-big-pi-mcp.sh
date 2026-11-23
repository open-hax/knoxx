#!/usr/bin/env bash
set -euo pipefail
big_pi_repo="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
exec bash "$big_pi_repo/scripts/start-big-pi-local.sh" --stdio
