#!/usr/bin/env bash
# Lightweight smoke checks for the current Knoxx CLJS/Fastify backend.
#
# Defaults avoid model-generation requirements. Set KNOXX_SMOKE_CHAT=1 to send a
# small OpenAI-compatible chat request through /v1/chat/completions.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8000}"
STRICT="${KNOXX_SMOKE_STRICT:-0}"
CHAT="${KNOXX_SMOKE_CHAT:-0}"
MODEL="${KNOXX_SMOKE_MODEL:-${PROXX_DEFAULT_MODEL:-glm-5}}"
API_KEY="${KNOXX_API_KEY:-}"
TMP_DIR="$(mktemp -d)"

cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required" >&2
  exit 1
fi

pretty_body() {
  local file="$1"
  if [[ ! -s "$file" ]]; then
    echo "<empty body>"
    return
  fi

  if command -v jq >/dev/null 2>&1; then
    jq . "$file" 2>/dev/null || cat "$file"
  else
    cat "$file"
  fi
}

request() {
  local label="$1"
  local method="$2"
  local path="$3"
  local required="$4"
  local body="${5:-}"
  local out="$TMP_DIR/$(echo "$label" | tr -cs '[:alnum:]' '_').json"
  local err="$TMP_DIR/$(echo "$label" | tr -cs '[:alnum:]' '_').err"
  local url="${BASE_URL%/}${path}"
  local -a args=(-sS -o "$out" -w '%{http_code}' -X "$method" "$url" -H 'Accept: application/json')

  if [[ -n "$API_KEY" ]]; then
    args+=(-H "X-API-Key: $API_KEY")
  fi

  if [[ -n "$body" ]]; then
    args+=(-H 'Content-Type: application/json' -d "$body")
  fi

  echo
  echo "[$label] $method $url"
  local http_code
  http_code="$(curl "${args[@]}" 2>"$err" || true)"

  if [[ -s "$err" ]]; then
    sed 's/^/[curl] /' "$err" >&2
  fi

  echo "HTTP ${http_code:-000}"
  pretty_body "$out"

  if [[ "$required" == "1" && ! "${http_code:-000}" =~ ^2[0-9][0-9]$ ]]; then
    echo "Required smoke check failed: $label" >&2
    exit 1
  fi
}

json_from_node() {
  node --input-type=module -e "$1"
}

request "config" "GET" "/api/config" "1"

# /health includes dependency reachability and may legitimately return 503 when
# Proxx/OpenPlanner are down. Make it required only in strict mode.
request "health" "GET" "/health" "$STRICT"

if [[ -n "$API_KEY" ]]; then
  request "auth-context" "GET" "/api/auth/context" "$STRICT"
else
  echo
  echo "[auth-context] skipped because KNOXX_API_KEY is not set"
fi

request "proxx-health" "GET" "/api/proxx/health" "$STRICT"
request "openai-models" "GET" "/v1/models" "$STRICT"

if [[ "$CHAT" == "1" ]]; then
  chat_body="$(KNOXX_SMOKE_MODEL="$MODEL" json_from_node 'console.log(JSON.stringify({model: process.env.KNOXX_SMOKE_MODEL || "glm-5", messages: [{role: "user", content: "Reply with one short Knoxx backend smoke-test sentence."}], stream: false, max_tokens: 48}))')"
  request "chat" "POST" "/v1/chat/completions" "1" "$chat_body"
else
  echo
  echo "[chat] skipped. Set KNOXX_SMOKE_CHAT=1 to exercise /v1/chat/completions with model '$MODEL'."
fi

echo
if [[ "$STRICT" == "1" ]]; then
  echo "Strict smoke checks completed."
else
  echo "Smoke checks completed. Dependency failures above are informational unless KNOXX_SMOKE_STRICT=1."
fi
