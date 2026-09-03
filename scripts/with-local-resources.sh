#!/usr/bin/env bash
set -euo pipefail

KNOXX_SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC1091
source "$KNOXX_SCRIPT_DIR/lib/local-runtime-env.sh"

configure_knoxx_local_runtime

if [[ $# -eq 0 ]]; then
  printf 'Knoxx local runtime configured for:\n'
  printf '  workspace: %s\n' "$WORKSPACE_ROOT"
  printf '  contracts: %s\n' "$CONTRACTS_DIR"
  printf '  MongoDB:   127.0.0.1:27017/%s (credential redacted)\n' "$MONGODB_DB"
  printf '  Proxx:     %s (credential redacted)\n' "$PROXX_BASE_URL"
  printf '  Ollama:    %s (agents: %s; embeddings: %s/%s)\n' \
    "$OLLAMA_BASE_URL" "$OLLAMA_DEFAULT_MODEL" \
    "$EMBED_PROVIDER_MODEL" "$EMBED_PROVIDER_DIMENSIONS"
  exit 0
fi

exec "$@"
