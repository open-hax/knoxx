#!/usr/bin/env bash
#
# Live verification for the shipped POST /mcp authentication contract.
#
# This surface has no domain fixture to seed. The script writes response
# evidence only to a private temporary directory and removes it on every exit.
# It refuses to run unless the listener's process cwd is this checkout, because
# a green probe against another Knoxx tree is not evidence for this one.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_URL="${KNOXX_BASE_URL:-http://127.0.0.1:8000}"
TOKEN="${KNOXX_MCP_LOOPBACK_TOKEN:-}"
EXPECTED_NODE_ENV="${KNOXX_EXPECT_NODE_ENV:-production}"
SERVER_PID="${KNOXX_SERVER_PID:-}"
SELFTEST="${KNOXX_MCP_VERIFY_SELFTEST:-false}"
EVIDENCE_DIR="$(mktemp -d "${TMPDIR:-/tmp}/knoxx-mcp-auth.XXXXXX")"

PASS_COUNT=0
FAIL_COUNT=0
FAILURES=()

cleanup() {
  local code=$?
  local signalled="${1:-}"
  if [ -n "$signalled" ]; then code="$signalled"; fi
  if [ -d "$EVIDENCE_DIR" ]; then rm -rf "$EVIDENCE_DIR"; fi
  exit "$code"
}

trap cleanup EXIT
trap 'trap - EXIT; cleanup 130' INT
trap 'trap - EXIT; cleanup 143' TERM

pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  printf 'PASS  %s\n' "$1"
}

fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  FAILURES+=("$1")
  printf 'FAIL  %s\n' "$1"
  if [ -n "${2:-}" ]; then printf '      got: %s\n' "$2"; fi
}

die() {
  printf 'ABORT %s\n' "$1" >&2
  exit 2
}

listener_pid() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null | head -n 1
    return
  fi
  if command -v ss >/dev/null 2>&1; then
    ss -ltnp "sport = :$port" 2>/dev/null \
      | sed -n 's/.*pid=\([0-9][0-9]*\).*/\1/p' \
      | head -n 1
    return
  fi
  return 1
}

request() {
  local label="$1" id="$2" token="$3" method="$4" params="$5"
  local headers="$EVIDENCE_DIR/${label}.headers"
  local body="$EVIDENCE_DIR/${label}.body"
  local payload
  payload="$(jq -cn --argjson id "$id" --arg method "$method" --argjson params "$params" \
    '{jsonrpc:"2.0", id:$id, method:$method, params:$params}')"
  local args=(-sS -D "$headers" -o "$body" -w '%{http_code}' --max-time 20
              -X POST -H 'content-type: application/json'
              -H 'accept: application/json, text/event-stream')
  if [ -n "$token" ]; then args+=(-H "authorization: Bearer ${token}"); fi
  curl "${args[@]}" --data "$payload" "${BASE_URL}/mcp" 2>/dev/null || printf '000'
}

rpc_reply() {
  local label="$1" id="$2"
  local headers="$EVIDENCE_DIR/${label}.headers"
  local body="$EVIDENCE_DIR/${label}.body"
  if grep -qi '^content-type:.*text/event-stream' "$headers"; then
    sed -n 's/^data:[[:space:]]*//p' "$body" \
      | jq -cs --argjson id "$id" 'map(select(.id == $id)) | first'
  else
    jq -c --argjson id "$id" 'select(.id == $id)' "$body"
  fi
}

expect_status() {
  local label="$1" expected="$2" actual="$3"
  if [ "$actual" = "$expected" ]; then
    pass "$label — HTTP ${actual}"
  else
    fail "$label — expected HTTP ${expected}" "$actual"
  fi
}

printf 'Knoxx MCP authentication — live verification\n'
printf 'base url: %s\n' "$BASE_URL"

for tool in curl jq readlink; do
  command -v "$tool" >/dev/null 2>&1 || die "missing required tool: $tool"
done
[ -n "$TOKEN" ] || die "KNOXX_MCP_LOOPBACK_TOKEN is not set"
[ "${#TOKEN}" -ge 16 ] || die "KNOXX_MCP_LOOPBACK_TOKEN is shorter than the shipped 16-character floor"

authority="${BASE_URL#*://}"
authority="${authority%%/*}"
host="${authority%%:*}"
port="${authority##*:}"
if [ "$host" = "$port" ]; then
  case "$BASE_URL" in https://*) port=443 ;; *) port=80 ;; esac
fi
case "$host" in
  127.0.0.1|localhost) ;;
  *) die "KNOXX_BASE_URL must name a loopback listener; got $host" ;;
esac

if [ "$SELFTEST" = true ]; then
  pass "self-test uses a controlled listener; live checkout and NODE_ENV checks are intentionally skipped"
else
  if [ -z "$SERVER_PID" ]; then SERVER_PID="$(listener_pid "$port" || true)"; fi
  [ -n "$SERVER_PID" ] || die "cannot identify the listener on port $port; set KNOXX_SERVER_PID explicitly"
  [ -r "/proc/${SERVER_PID}/cwd" ] || die "cannot inspect /proc/${SERVER_PID}/cwd"

  server_cwd="$(readlink -f "/proc/${SERVER_PID}/cwd")"
  case "$server_cwd" in
    "$REPO_ROOT"|"$REPO_ROOT"/*)
      pass "listener process ${SERVER_PID} serves this checkout (${server_cwd})"
      ;;
    *)
      die "listener process ${SERVER_PID} serves ${server_cwd}, not ${REPO_ROOT}"
      ;;
  esac

  [ -r "/proc/${SERVER_PID}/environ" ] || die "cannot inspect /proc/${SERVER_PID}/environ"
  actual_node_env="$(tr '\0' '\n' < "/proc/${SERVER_PID}/environ" \
    | sed -n 's/^NODE_ENV=//p' | tail -n 1)"
  if [ "$actual_node_env" = "$EXPECTED_NODE_ENV" ]; then
    pass "listener NODE_ENV is ${EXPECTED_NODE_ENV}"
  else
    die "listener NODE_ENV is ${actual_node_env:-<unset>}; expected ${EXPECTED_NODE_ENV}"
  fi
fi

if curl -sS -o /dev/null --max-time 5 "${BASE_URL}/health"; then
  pass "live Knoxx answers /health"
else
  die "no Knoxx backend answers ${BASE_URL}/health"
fi

unauth_status="$(request unauthenticated 1 "" initialize \
  '{"protocolVersion":"2025-06-18","clientInfo":{"name":"knoxx-live-verifier","version":"1.0.0"},"capabilities":{}}')"
expect_status "POST /mcp refuses an unauthenticated initialize" 401 "$unauth_status"

invalid_status="$(request invalid-bearer 2 "knoxx-invalid-live-verifier-token" initialize \
  '{"protocolVersion":"2025-06-18","clientInfo":{"name":"knoxx-live-verifier","version":"1.0.0"},"capabilities":{}}')"
expect_status "POST /mcp refuses an unknown bearer" 401 "$invalid_status"

valid_status="$(request valid-loopback 3 "$TOKEN" initialize \
  '{"protocolVersion":"2025-06-18","clientInfo":{"name":"knoxx-live-verifier","version":"1.0.0"},"capabilities":{}}')"
expect_status "the configured token crosses the production loopback gate" 200 "$valid_status"
valid_reply="$(rpc_reply valid-loopback 3 2>/dev/null || true)"
if printf '%s' "$valid_reply" \
  | jq -e '.jsonrpc == "2.0" and .id == 3 and (.result.protocolVersion | type == "string")' \
    >/dev/null 2>&1; then
  pass "initialize returns a JSON-RPC result, not merely HTTP 200"
else
  fail "initialize did not return a valid JSON-RPC result" "$(head -c 300 "$EVIDENCE_DIR/valid-loopback.body")"
fi

tools_status="$(request tools-list 4 "$TOKEN" tools/list '{}')"
expect_status "the authenticated principal reaches tools/list" 200 "$tools_status"
tools_reply="$(rpc_reply tools-list 4 2>/dev/null || true)"
if printf '%s' "$tools_reply" \
  | jq -e '.jsonrpc == "2.0" and .id == 4 and (.result.tools | type == "array" and length > 0)' \
    >/dev/null 2>&1; then
  tool_count="$(printf '%s' "$tools_reply" | jq '.result.tools | length')"
  duplicate_count="$(printf '%s' "$tools_reply" \
    | jq '[.result.tools[].name] as $names | ($names | length) - ($names | unique | length)')"
  if [ "$duplicate_count" -eq 0 ]; then
    pass "tools/list exposes ${tool_count} uniquely named tools under the contract grant"
  else
    fail "tools/list contains duplicate names" "$duplicate_count duplicates"
  fi
else
  fail "tools/list did not return a non-empty tool catalog" "$(head -c 300 "$EVIDENCE_DIR/tools-list.body")"
fi

printf '\nSummary: %s passed, %s failed\n' "$PASS_COUNT" "$FAIL_COUNT"
if [ "$FAIL_COUNT" -ne 0 ]; then
  printf 'Failed checks:\n'
  printf '  - %s\n' "${FAILURES[@]}"
  exit 1
fi
