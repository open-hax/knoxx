#!/usr/bin/env bash
set -euo pipefail

KNOXX_SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
KNOXX_DIR="$(cd "$KNOXX_SCRIPT_DIR/.." && pwd)"
# shellcheck disable=SC1091
source "$KNOXX_SCRIPT_DIR/lib/local-runtime-env.sh"

configure_knoxx_local_runtime

failures=0

pass() {
  printf 'PASS  %s\n' "$*"
}

fail() {
  printf 'FAIL  %s\n' "$*" >&2
  failures=$((failures + 1))
}

if [[ -d "$WORKSPACE_ROOT" && -r "$WORKSPACE_ROOT" && -d "$CONTRACTS_DIR" ]]; then
  pass "local resources are readable at $WORKSPACE_ROOT and Knoxx contracts resolve at $CONTRACTS_DIR"
else
  fail "workspace/contracts paths are not readable"
fi

if (
  cd "$KNOXX_DIR/backend"
  node --input-type=module -e '
    import { MongoClient } from "mongodb";
    const client = new MongoClient(process.env.MONGODB_URI, { serverSelectionTimeoutMS: 5000 });
    try {
      await client.connect();
      const result = await client.db(process.env.MONGODB_DB).admin().ping();
      if (result.ok !== 1) process.exit(1);
    } finally {
      await client.close();
    }
  '
); then
  pass "MongoDB accepted the local application credential and answered ping for $MONGODB_DB"
else
  fail "MongoDB did not accept the configured local connection"
fi

proxx_health_status="$(curl -sS --max-time 10 -o /dev/null -w '%{http_code}' \
  "$PROXX_BASE_URL/health" || true)"
if [[ "$proxx_health_status" == "200" ]]; then
  pass "Proxx answered its health contract at $PROXX_BASE_URL"
else
  fail "Proxx health returned HTTP ${proxx_health_status:-unreachable}"
fi

proxx_models_status="$(curl -sS --max-time 60 -o /dev/null -w '%{http_code}' \
  -H "Authorization: Bearer $PROXX_AUTH_TOKEN" \
  "$PROXX_BASE_URL/v1/models" || true)"
if [[ "$proxx_models_status" == "200" ]]; then
  pass "Proxx accepted Knoxx's bearer credential on /v1/models"
else
  fail "Proxx authenticated model discovery returned HTTP ${proxx_models_status:-unreachable}"
fi

ollama_version_status="$(curl -sS --max-time 10 -o /dev/null -w '%{http_code}' \
  "$OLLAMA_BASE_URL/api/version" || true)"
if [[ "$ollama_version_status" == "200" ]]; then
  pass "Ollama answered its version contract at $OLLAMA_BASE_URL"
else
  fail "Ollama version returned HTTP ${ollama_version_status:-unreachable}"
fi

ollama_tags="$(curl -fsS --max-time 10 "$OLLAMA_BASE_URL/api/tags" || true)"
if printf '%s' "$ollama_tags" \
  | jq -e --arg model "$OLLAMA_DEFAULT_MODEL" \
      '.models | any(.name == $model or .model == $model)' >/dev/null; then
  pass "Ollama has the configured Knoxx model $OLLAMA_DEFAULT_MODEL"
else
  fail "Ollama does not list the configured Knoxx model $OLLAMA_DEFAULT_MODEL"
fi

if printf '%s' "$ollama_tags" \
  | jq -e --arg model "$EMBED_PROVIDER_MODEL" '
      .models | any(
        .name == $model or .model == $model or
        (.name | startswith($model + ":")) or
        (.model | startswith($model + ":")))' >/dev/null; then
  pass "Ollama has the configured embedding model $EMBED_PROVIDER_MODEL"
else
  fail "Ollama does not list the configured embedding model $EMBED_PROVIDER_MODEL"
fi

embedding_payload="$(jq -cn --arg model "$EMBED_PROVIDER_MODEL" \
  '{model:$model,input:"Knoxx local embedding verification"}')"
embedding_response="$(curl -fsS --max-time 120 \
  -H 'Content-Type: application/json' \
  -d "$embedding_payload" \
  "$EMBED_PROVIDER_BASE_URL/v1/embeddings" || true)"
if printf '%s' "$embedding_response" \
  | jq -e --argjson dimensions "$EMBED_PROVIDER_DIMENSIONS" '
      (.data | type) == "array" and (.data | length) == 1 and
      (.data[0].embedding | type) == "array" and
      (.data[0].embedding | length) == $dimensions and
      (.data[0].embedding | all(type == "number" and isfinite))' >/dev/null; then
  pass "Ollama produced one finite ${EMBED_PROVIDER_DIMENSIONS}-dimensional embedding"
else
  fail "Ollama did not produce the configured ${EMBED_PROVIDER_DIMENSIONS}-dimensional embedding"
fi

if (( failures > 0 )); then
  printf '\n%d local connection check(s) failed.\n' "$failures" >&2
  exit 1
fi

printf '\nAll local Knoxx resource connections are ready.\n'
