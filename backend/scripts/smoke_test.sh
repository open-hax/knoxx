#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8000}"
MODEL_PATH="${1:-}"

if [[ -z "$MODEL_PATH" ]]; then
  echo "Usage: $0 /absolute/path/to/model.gguf"
  exit 1
fi

echo "[1] Health"
curl -fsS "$BASE_URL/health" | jq

echo "[2] Models"
curl -fsS "$BASE_URL/api/models" | jq '.models | length'

echo "[3] Start server"
curl -fsS -X POST "$BASE_URL/api/server/start" \
  -H "Content-Type: application/json" \
  -d "{\"model_path\":\"$MODEL_PATH\",\"ctx_size\":4096,\"gpu_layers\":99,\"threads\":8,\"batch_size\":512}" | jq '.ok'

echo "[4] Warmup"
curl -fsS -X POST "$BASE_URL/api/server/warmup" -H "Content-Type: application/json" -d '{"prompt":"Say hello in one sentence.","max_tokens":16}' | jq '.ok'

echo "[5] Queue chat run"
RUN_ID=$(curl -fsS -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"messages":[{"role":"user","content":"Write one sentence about local LLM testing."}],"temperature":0.3,"max_tokens":64}' | jq -r '.run_id')

echo "Run queued: $RUN_ID"
sleep 2

echo "[6] Check run detail"
curl -fsS "$BASE_URL/api/runs/$RUN_ID" | jq '.status, .ttft_ms, .tokens_per_s'

echo "[7] Stop server"
curl -fsS -X POST "$BASE_URL/api/server/stop" | jq '.ok'

echo "Smoke test complete"
