#!/usr/bin/env bash
#
# Live verification for deployment-triggered publication document admission.
#
# The script owns one unique anchored Document, its source, one Garden, and two
# draft publication relations. It proves that the authenticated anchor sweep
# indexes the source through OpenPlanner, dispatches every declared target
# locale, collapses an unchanged replay onto the same event and claim
# identities, and leaves both localized relations review-blocked and
# unpublished. With KNOXX_VERIFY_GENERATED_DRAFTS=true it then starts exactly
# one post-drafter for this exact document and follows the generated document
# through recursive admission, translation, and the same review gate.
#
# It deliberately never calls POST /api/publications/reconcile. Its only CMS
# PATCH is the idempotent state {"state":"draft"}, proving that the draft wire
# value is accepted without requesting publication. Unless explicit keep-demo
# mode is green, filesystem and database fixtures are removed by the
# EXIT/INT/TERM trap. The trap fences the run's resource anchors, drains the
# serialized admission tail and every reconstructed event owner, and requires
# two identical durable snapshots before it removes any database or filesystem
# state. A teardown failure exits nonzero and keeps the fenced bytes at one
# named evidence path outside every resource root.
#
# Usage:
#   KNOXX_API_KEY=... scripts/verify-deployment-content-admission.sh
#   KNOXX_API_KEY=... KNOXX_VERIFY_GENERATED_DRAFTS=true \
#     KNOXX_GENERATED_CONTRACTS_DIR=/absolute/path \
#     KNOXX_VERIFY_PUBLICATION_CONTENT_ROOT=/absolute/path \
#     scripts/verify-deployment-content-admission.sh
#   KNOXX_API_KEY=... KNOXX_VERIFY_KEEP_REVIEW_DEMO=true \
#     KNOXX_GENERATED_CONTRACTS_DIR=/absolute/path \
#     KNOXX_VERIFY_PUBLICATION_CONTENT_ROOT=/absolute/path \
#     scripts/verify-deployment-content-admission.sh
#
# Exit code is zero only when every required assertion passed and the owned
# filesystem and durable database fixtures were removed, or explicit keep-demo
# mode retained them.

# jq receives its `$name` variables through `--arg`; single quotes are required
# to keep Bash from expanding them first.
# shellcheck disable=SC2016,SC2329

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_URL="${KNOXX_BASE_URL:-http://localhost:8000}"
API_KEY="${KNOXX_API_KEY:-}"
HTTP_TIMEOUT_SECONDS="${KNOXX_VERIFY_HTTP_TIMEOUT_SECONDS:-120}"
AGENT_WAIT_SECONDS="${KNOXX_VERIFY_AGENT_WAIT_SECONDS:-360}"
VERIFY_GENERATED_DRAFTS="${KNOXX_VERIFY_GENERATED_DRAFTS:-false}"
KEEP_REVIEW_DEMO="${KNOXX_VERIFY_KEEP_REVIEW_DEMO:-false}"
CONTRACTS_DIR="${KNOXX_CONTRACTS_DIR:-${REPO_ROOT}/contracts}"
GENERATED_CONTRACTS_DIR="${KNOXX_GENERATED_CONTRACTS_DIR:-}"
PUBLICATION_CONTENT_ROOT="${KNOXX_VERIFY_PUBLICATION_CONTENT_ROOT:-}"
FIXTURE_DIR="${CONTRACTS_DIR}/_verify_deployment_content_admission"
RUN_ID="$(date -u +%Y%m%d%H%M%S)$$"
NS="knoxx.verifyadmission"
DOC_LOCAL="probe${RUN_ID}"
DOC_ID="${NS}/${DOC_LOCAL}"
GARDEN_LOCAL="probe-garden-${RUN_ID}"
GARDEN_ID="${NS}/${GARDEN_LOCAL}"
PUB_ES_ID="${NS}/${DOC_LOCAL}-es"
PUB_FR_ID="${NS}/${DOC_LOCAL}-fr"
CONTRACTS_PARENT="$(cd "$(dirname "${CONTRACTS_DIR}")" && pwd)"
CONTRACTS_NAME="$(basename "${CONTRACTS_DIR}")"
SOURCE_REL="${CONTRACTS_NAME}/_verify_deployment_content_admission/source-${RUN_ID}.md"
SOURCE_FILE="${CONTRACTS_PARENT}/${SOURCE_REL}"
EVENTS_COLLECTION="${MONGODB_EVENTS_COLLECTION:-events}"
VECTOR_COLLECTION="${MONGODB_VECTOR_HOT_COLLECTION:-event_chunks}"
GRAPH_NODE_EMBEDDING_COLLECTION="${MONGODB_GRAPH_NODE_EMBEDDING_COLLECTION:-graph_node_embeddings}"
ADMISSION_URL="/api/publications/documents/admit"
ADMISSION_BARRIER_URL="/api/publications/documents/admission-barrier"
REVIEWS_URL="/api/publications/translations/reviews"
EVENT_TURN_STATUS_URL="/api/publications/translations/event-turn-status"
MONGO_QUERY_URL="/api/data/mongo/query"
CMS_URL="/api/cms/publications/documents/${NS}%2F${DOC_LOCAL}"
CMS_INTENT_ES_URL="/api/cms/publications/intents/${NS}%2F${DOC_LOCAL}-es"
RECEIPTS_URL="/api/publications/receipts"
LOOPBACK_HTTP=0
RUN_GENERATED_DRAFTS=0
RETAIN_REVIEW_DEMO=0

# shellcheck source=scripts/lib/credential-transport.sh
# shellcheck disable=SC1091
. "${REPO_ROOT}/scripts/lib/credential-transport.sh"

PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0
FAILURES=()
FIXTURE_OWNED=0
SOURCE_EVENT_ID=""
INDEX_EVENT_ID=""
CANDIDATE_EVENT_IDS='[]'
CANDIDATE_EVENT_ROW_COUNT=0
GENERATED_DOC_ID=""
GENERATED_REVISION=""
GENERATION_REQUEST_SOURCE_EVENT_ID=""
GENERATION_REQUEST_INDEX_EVENT_ID=""
GENERATED_SOURCE_EVENT_ID=""
GENERATED_INDEX_EVENT_ID=""
GENERATED_CANDIDATE_EVENT_IDS='[]'
GENERATED_MANIFEST_FILE=""
GENERATED_SOURCE_FILE=""
GENERATED_COMPLETION_FILE=""
GENERATED_ARTIFACTS_SETTLED=0
REVIEW_DEMO_READY=0
REVIEW_DEMO_SOURCE_DIR=""
ADMISSION_ORG_ID=""
ADMISSION_PROJECT=""
SOURCE_TRANSLATION_CONTENT_SETTLED=0
GENERATED_TRANSLATION_CONTENT_SETTLED=0
GRAPH_NODE_EMBEDDINGS_SETTLED=0
EVENT_TURNS_SETTLED=0
TRANSLATION_CONTENT_FILES=()
CLEANUP_EVENT_IDS='[]'
CLEANUP_QUARANTINE_DIR=""
CLEANUP_GENERATED_FILES_QUARANTINED=0
CLEANUP_LAST_ERROR=""

if [ -t 1 ]; then
  C_RESET=$'\033[0m'; C_DIM=$'\033[2m'; C_BOLD=$'\033[1m'
  C_GREEN=$'\033[32m'; C_RED=$'\033[31m'; C_YELLOW=$'\033[33m'; C_CYAN=$'\033[36m'
else
  C_RESET=""; C_DIM=""; C_BOLD=""; C_GREEN=""; C_RED=""; C_YELLOW=""; C_CYAN=""
fi

step() { printf '\n%s── %s%s\n' "$C_BOLD$C_CYAN" "$1" "$C_RESET"; }
note() { printf '%s   %s%s\n' "$C_DIM" "$1" "$C_RESET"; }
pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  printf '%s   PASS%s  %s\n' "$C_GREEN" "$C_RESET" "$1"
}
warn() {
  WARN_COUNT=$((WARN_COUNT + 1))
  printf '%s   WARN%s  %s\n' "$C_YELLOW" "$C_RESET" "$1"
}
fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  FAILURES+=("$1")
  printf '%s   FAIL%s  %s\n' "$C_RED" "$C_RESET" "$1"
  [ -n "${2:-}" ] && printf '%s         got: %s%s\n' "$C_DIM" "$2" "$C_RESET"
  return 0
}
die() {
  printf '\n%sABORT%s %s\n\n' "$C_RED$C_BOLD" "$C_RESET" "$1" >&2
  exit 2
}

# Emits "<status>\n<body>". `-q` prevents a curlrc from enabling redirects or
# otherwise changing credential transport after BASE_URL passed admission.
http() {
  local method="$1" path="$2" authorized="$3" body="${4:-}" raw
  local args=(-q -sS -o /dev/stdout -w $'\n%{http_code}' -X "$method" --max-time "$HTTP_TIMEOUT_SECONDS")
  [ "$LOOPBACK_HTTP" -eq 1 ] && args+=(--noproxy '*')
  [ "$authorized" = "auth" ] && args+=(-H "X-API-Key: ${API_KEY}")
  [ -n "$body" ] && args+=(-H "Content-Type: application/json" -d "$body")
  raw="$(curl "${args[@]}" "${BASE_URL}${path}" 2>/dev/null)" || raw=$'\n000'
  printf '%s\n%s' "${raw##*$'\n'}" "${raw%$'\n'*}"
}

status_of() { printf '%s' "${1%%$'\n'*}"; }
body_of() { printf '%s' "${1#*$'\n'}"; }

expect_status() {
  local label="$1" expected="$2" response="$3" status
  status="$(status_of "$response")"
  if [[ " $expected " == *" $status "* ]]; then
    pass "$label ${C_DIM}(${status})${C_RESET}"
    return 0
  fi
  fail "$label — expected ${expected// /|}, got ${status}" \
    "$(body_of "$response" | head -c 500)"
  return 1
}

expect_jq() {
  local label="$1" filter="$2" response="$3"
  shift 3
  if printf '%s' "$(body_of "$response")" | jq -e "$@" "$filter" >/dev/null 2>&1; then
    pass "$label"
    return 0
  fi
  fail "$label — jq filter did not hold: ${filter}" \
    "$(body_of "$response" | jq -c . 2>/dev/null | head -c 500)"
  return 1
}

expect_json() {
  local label="$1" filter="$2" payload="$3"
  shift 3
  if printf '%s' "$payload" | jq -e "$@" "$filter" >/dev/null 2>&1; then
    pass "$label"
    return 0
  fi
  fail "$label — jq filter did not hold: ${filter}" \
    "$(printf '%s' "$payload" | jq -c . 2>/dev/null | head -c 500)"
  return 1
}

print_translation_content_cleanup() {
  local file
  [ "${#TRANSLATION_CONTENT_FILES[@]}" -gt 0 ] || return 0
  printf '   exact translation-content cleanup: rm -f --'
  for file in "${TRANSLATION_CONTENT_FILES[@]}"; do
    printf ' %q' "$file"
  done
  printf '\n'
}

track_translation_content() {
  local label="$1" pairs="$2" revision digest file missing=0
  while IFS= read -r revision; do
    [ -n "$revision" ] || continue
    digest="$(printf '%s' "$revision" | sha256sum | awk '{print $1}')"
    file="${PUBLICATION_CONTENT_ROOT}/.translations/${digest}.edn"
    case "$file" in
      "${PUBLICATION_CONTENT_ROOT}/.translations/"[0-9a-f][0-9a-f][0-9a-f][0-9a-f]*) ;;
      *) die "derived translation-content path escaped its exact store: ${file}" ;;
    esac
    if [ -s "$file" ]; then
      TRANSLATION_CONTENT_FILES+=("$file")
    else
      fail "${label} translation content is absent from the configured store" "$file"
      missing=1
    fi
  done < <(printf '%s' "$pairs" | jq -r '.[].output_revision')
  if [ "$missing" -eq 0 ]; then
    pass "${label} authenticated output revisions map to exact stored translation files"
    return 0
  fi
  return 1
}

durable_agent_work_settled() {
  [ "$SOURCE_TRANSLATION_CONTENT_SETTLED" -eq 1 ] \
    && [ "$GRAPH_NODE_EMBEDDINGS_SETTLED" -eq 1 ] \
    && [ "$EVENT_TURNS_SETTLED" -eq 1 ] \
    && { [ "$RUN_GENERATED_DRAFTS" -eq 0 ] \
         || { [ "$GENERATED_ARTIFACTS_SETTLED" -eq 1 ] \
              && [ "$GENERATED_TRANSLATION_CONTENT_SETTLED" -eq 1 ]; }; }
}

cleanup_documents_json() {
  jq -cn \
    --arg source "$DOC_ID" \
    --arg generated "$GENERATED_DOC_ID" \
    '[$source,$generated] | map(select(length > 0))'
}

# The verifier's source manifest is itself an admission capability. Move it
# outside every resource root before asking the process to drain so that a new
# anchor sweep cannot create work behind the cleanup barrier.
ensure_cleanup_quarantine() {
  local quarantine contracts_root generated_root=""
  [ -n "$CLEANUP_QUARANTINE_DIR" ] && return 0
  quarantine="$(mktemp -d "/tmp/knoxx-deployment-admission-${RUN_ID}.XXXXXX")" \
    || return 1
  quarantine="$(realpath -m "$quarantine")" || return 1
  contracts_root="$(realpath -m "$CONTRACTS_DIR")" || return 1
  if [ -n "$GENERATED_CONTRACTS_DIR" ]; then
    generated_root="$(realpath -m "$GENERATED_CONTRACTS_DIR")" || return 1
  fi
  case "$quarantine" in
    "$contracts_root"|"$contracts_root"/*)
      rmdir -- "$quarantine" 2>/dev/null || true
      return 1
      ;;
  esac
  if [ -n "$generated_root" ]; then
    case "$quarantine" in
      "$generated_root"|"$generated_root"/*)
        rmdir -- "$quarantine" 2>/dev/null || true
        return 1
        ;;
    esac
  fi
  CLEANUP_QUARANTINE_DIR="$quarantine"
}

quarantine_source_resources() {
  ensure_cleanup_quarantine || return 1
  if [ "$FIXTURE_OWNED" -eq 1 ] && [ -d "$FIXTURE_DIR" ]; then
    mv -- "$FIXTURE_DIR" "$CLEANUP_QUARANTINE_DIR/source-fixture" || return 1
  fi
  if [ -n "$REVIEW_DEMO_SOURCE_DIR" ] && [ -d "$REVIEW_DEMO_SOURCE_DIR" ]; then
    mv -- "$REVIEW_DEMO_SOURCE_DIR" \
      "$CLEANUP_QUARANTINE_DIR/review-demo-source" || return 1
  fi
}

# Generated manifests are anchors too. They cannot be fenced until the source
# document's post-draft owner has released, because that owner may still be
# writing the files. The first barrier/owner drain establishes that point.
quarantine_generated_files() {
  [ "$CLEANUP_GENERATED_FILES_QUARANTINED" -eq 0 ] || return 0
  ensure_cleanup_quarantine || return 1
  mkdir -p "$CLEANUP_QUARANTINE_DIR/generated" || return 1
  if [ -n "$GENERATED_MANIFEST_FILE" ] && [ -e "$GENERATED_MANIFEST_FILE" ]; then
    mv -- "$GENERATED_MANIFEST_FILE" \
      "$CLEANUP_QUARANTINE_DIR/generated/manifest.edn" || return 1
  fi
  if [ -n "$GENERATED_SOURCE_FILE" ] && [ -e "$GENERATED_SOURCE_FILE" ]; then
    mv -- "$GENERATED_SOURCE_FILE" \
      "$CLEANUP_QUARANTINE_DIR/generated/source.md" || return 1
  fi
  if [ -n "$GENERATED_COMPLETION_FILE" ] \
     && [ -e "$GENERATED_COMPLETION_FILE" ]; then
    mv -- "$GENERATED_COMPLETION_FILE" \
      "$CLEANUP_QUARANTINE_DIR/generated/completion.edn" || return 1
  fi
  CLEANUP_GENERATED_FILES_QUARANTINED=1
}

inspect_durable_fixtures() {
  local documents_json="$1"
  KNOXX_VERIFY_CLEANUP_MODE=inspect \
  KNOXX_VERIFY_CLEANUP_DOCUMENTS_JSON="$documents_json" \
  KNOXX_VERIFY_CLEANUP_EVENT_IDS_JSON='[]' \
  MONGODB_EVENTS_COLLECTION="$EVENTS_COLLECTION" \
  MONGODB_VECTOR_HOT_COLLECTION="$VECTOR_COLLECTION" \
  MONGODB_GRAPH_NODE_EMBEDDING_COLLECTION="$GRAPH_NODE_EMBEDDING_COLLECTION" \
    node "${REPO_ROOT}/backend/scripts/cleanup-deployment-content-admission.mjs"
}

valid_cleanup_inspection() {
  local inspection="$1" documents_json="$2"
  printf '%s' "$inspection" | jq -e --argjson documents "$documents_json" '
    type == "object" and .documents == $documents and
    (.eventIds | type) == "array" and
    (.eventIds | all(type == "string" and length > 0)) and
    (.eventIds | length) == (.eventIds | unique | length) and
    (.ownerEventIds | type) == "array" and
    (.ownerEventIds |
      all(type == "string" and
          test("^(translation-needed-translation-run|knoxx-publication-document-indexed)-[0-9a-f]{64}$"))) and
    (.ownerEventIds | length) == (.ownerEventIds | unique | length) and
    (.dispatches | type) == "array" and
    (.dispatches |
      all((keys | sort) == ["batchId","document","outcome"] and
          (.document as $document | $documents | index($document) != null) and
          (.outcome | type) == "string" and
          ((.batchId == null) or ((.batchId | type) == "string" and
                                  (.batchId | length) > 0)))) and
    (.turnIds | type) == "array" and
    (.turnIds | all(type == "string" and length > 0)) and
    (.candidateSetIds | type) == "array" and
    (.candidateSetIds | all(type == "string" and length > 0)) and
    (.candidateRevisions | type) == "array" and
    (.candidateRevisions | all(type == "string" and length > 0)) and
    (if (.eventIds | length) == 0 then
       ((.ownerEventIds + .dispatches + .turnIds + .candidateSetIds +
         .candidateRevisions) | length) == 0
     else true end)' >/dev/null 2>&1
}

cleanup_dispatches_terminal() {
  local inspection="$1"
  printf '%s' "$inspection" | jq -e '
    .dispatches |
    all(.outcome == "dispatch/completed" or
        .outcome == "dispatch/failed" or
        .outcome == "dispatch/unreachable")' >/dev/null 2>&1
}

# Return 0 when every supplied process-local owner is released, 1 for a
# transient in-flight/unreachable state, and 2 for a malformed API response.
cleanup_owners_released_once() {
  local event_ids="$1" payload response status body
  CLEANUP_LAST_ERROR=""
  if ! printf '%s' "$event_ids" | jq -e '
       type == "array" and length <= 16 and
       length == (unique | length) and
       all(type == "string" and
           test("^(translation-needed-translation-run|knoxx-publication-document-indexed)-[0-9a-f]{64}$"))' \
       >/dev/null 2>&1; then
    CLEANUP_LAST_ERROR="cleanup reconstructed an invalid event-owner set"
    return 2
  fi
  if printf '%s' "$event_ids" | jq -e 'length == 0' >/dev/null 2>&1; then
    return 0
  fi
  payload="$(jq -cn --argjson event_ids "$event_ids" \
    '{event_ids:$event_ids}')" || return 2
  response="$(http POST "$EVENT_TURN_STATUS_URL" auth "$payload")"
  status="$(status_of "$response")"
  body="$(body_of "$response")"
  if [ "$status" != "200" ]; then
    CLEANUP_LAST_ERROR="event-owner status returned HTTP ${status}"
    return 1
  fi
  if ! printf '%s' "$body" | jq -e --argjson ids "$event_ids" '
       type == "object" and (.settled | type) == "boolean" and
       (.events | type) == "array" and
       ([.events[].event_id] == $ids) and
       (.events |
         all((keys | sort) == ["event_id","state"] and
             (.state == "in_flight" or
              .state == "redelivery_pending" or
              .state == "released"))) and
       (.settled == (.events | all(.state == "released")))' \
       >/dev/null 2>&1; then
    CLEANUP_LAST_ERROR="event-owner status returned a malformed identity/state set"
    return 2
  fi
  if printf '%s' "$body" | jq -e \
       '.settled == true and (.events | all(.state == "released"))' \
       >/dev/null 2>&1; then
    return 0
  fi
  CLEANUP_LAST_ERROR="one or more event owners remain active"
  return 1
}

# Return 0 when the process-wide admission tail has drained, 1 for a transient
# transport/server failure, and 2 for a malformed success response.
await_admission_barrier_once() {
  local response status body
  CLEANUP_LAST_ERROR=""
  response="$(http POST "$ADMISSION_BARRIER_URL" auth '{}')"
  status="$(status_of "$response")"
  body="$(body_of "$response")"
  if [ "$status" != "200" ]; then
    CLEANUP_LAST_ERROR="document-admission barrier returned HTTP ${status}"
    return 1
  fi
  if ! printf '%s' "$body" | jq -e \
       'type == "object" and (keys == ["settled"]) and .settled == true' \
       >/dev/null 2>&1; then
    CLEANUP_LAST_ERROR="document-admission barrier returned malformed success"
    return 2
  fi
  return 0
}

track_cleanup_translation_content() {
  local inspection="$1" revision digest file tracked existing
  while IFS= read -r revision; do
    [ -n "$revision" ] || continue
    digest="$(printf '%s' "$revision" | sha256sum | awk '{print $1}')" \
      || return 1
    file="${PUBLICATION_CONTENT_ROOT}/.translations/${digest}.edn"
    case "$file" in
      "${PUBLICATION_CONTENT_ROOT}/.translations/"[0-9a-f][0-9a-f]*) ;;
      *) return 1 ;;
    esac
    [ -e "$file" ] || continue
    tracked=0
    for existing in "${TRANSLATION_CONTENT_FILES[@]}"; do
      if [ "$existing" = "$file" ]; then
        tracked=1
        break
      fi
    done
    [ "$tracked" -eq 1 ] || TRANSLATION_CONTENT_FILES+=("$file")
  done < <(printf '%s' "$inspection" | jq -r '.candidateRevisions[]')
}

# Establish a quiescent exact scope even when the main assertions were killed
# before they learned event/run ids. Source anchors are already fenced. The
# first barrier closes the owner-registration window, inspection reconstructs
# every run-owned owner from Mongo, and the second barrier catches recursive
# admission queued by a post-draft owner. Identical snapshots prove that no
# accepted dispatch or durable identity appeared between the two barriers.
await_cleanup_quiescence() {
  local documents_json deadline first second first_canonical second_canonical
  local owner_ids barrier_status owner_status
  documents_json="$(cleanup_documents_json)" || return 1
  deadline=$((SECONDS + AGENT_WAIT_SECONDS))
  while [ "$SECONDS" -lt "$deadline" ]; do
    await_admission_barrier_once
    barrier_status=$?
    [ "$barrier_status" -eq 2 ] && return 1
    if [ "$barrier_status" -ne 0 ]; then
      sleep 2
      continue
    fi

    first="$(inspect_durable_fixtures "$documents_json")" || {
      CLEANUP_LAST_ERROR="durable cleanup inspection failed"
      sleep 2
      continue
    }
    if ! valid_cleanup_inspection "$first" "$documents_json"; then
      CLEANUP_LAST_ERROR="durable cleanup inspection returned malformed scope"
      return 1
    fi
    if ! cleanup_dispatches_terminal "$first"; then
      CLEANUP_LAST_ERROR="one or more translation dispatches remain accepted"
      sleep 2
      continue
    fi
    owner_ids="$(printf '%s' "$first" | jq -c '.ownerEventIds')" || return 1
    cleanup_owners_released_once "$owner_ids"
    owner_status=$?
    [ "$owner_status" -eq 2 ] && return 1
    if [ "$owner_status" -ne 0 ]; then
      sleep 2
      continue
    fi

    # No run-owned producer remains able to create these generated files. Move
    # their anchor and immutable companions out of the resource roots before
    # the second barrier closes any admission already queued by another caller.
    quarantine_generated_files || {
      CLEANUP_LAST_ERROR="could not quarantine generated run files"
      return 1
    }

    await_admission_barrier_once
    barrier_status=$?
    [ "$barrier_status" -eq 2 ] && return 1
    if [ "$barrier_status" -ne 0 ]; then
      sleep 2
      continue
    fi
    second="$(inspect_durable_fixtures "$documents_json")" || {
      CLEANUP_LAST_ERROR="second durable cleanup inspection failed"
      sleep 2
      continue
    }
    if ! valid_cleanup_inspection "$second" "$documents_json"; then
      CLEANUP_LAST_ERROR="second durable cleanup inspection returned malformed scope"
      return 1
    fi
    if ! cleanup_dispatches_terminal "$second"; then
      CLEANUP_LAST_ERROR="a translation dispatch appeared after the first barrier"
      sleep 2
      continue
    fi
    owner_ids="$(printf '%s' "$second" | jq -c '.ownerEventIds')" || return 1
    cleanup_owners_released_once "$owner_ids"
    owner_status=$?
    [ "$owner_status" -eq 2 ] && return 1
    if [ "$owner_status" -ne 0 ]; then
      sleep 2
      continue
    fi

    first_canonical="$(printf '%s' "$first" | jq -S -c .)" || return 1
    second_canonical="$(printf '%s' "$second" | jq -S -c .)" || return 1
    if [ "$first_canonical" = "$second_canonical" ]; then
      CLEANUP_EVENT_IDS="$(printf '%s' "$second" | jq -c '.eventIds')" \
        || return 1
      track_cleanup_translation_content "$second" || {
        CLEANUP_LAST_ERROR="could not derive exact translation-content paths"
        return 1
      }
      return 0
    fi
    CLEANUP_LAST_ERROR="run-owned durable scope changed across cleanup barriers"
    sleep 2
  done
  [ -n "$CLEANUP_LAST_ERROR" ] \
    || CLEANUP_LAST_ERROR="cleanup quiescence deadline expired"
  return 1
}

# Called from the EXIT/INT/TERM cleanup path.
# shellcheck disable=SC2329
cleanup_durable_fixtures() {
  local documents_json
  documents_json="$(cleanup_documents_json)" || return 1

  KNOXX_VERIFY_CLEANUP_MODE=delete \
  KNOXX_VERIFY_CLEANUP_DOCUMENTS_JSON="$documents_json" \
  KNOXX_VERIFY_CLEANUP_EVENT_IDS_JSON="$CLEANUP_EVENT_IDS" \
  MONGODB_EVENTS_COLLECTION="$EVENTS_COLLECTION" \
  MONGODB_VECTOR_HOT_COLLECTION="$VECTOR_COLLECTION" \
  MONGODB_GRAPH_NODE_EMBEDDING_COLLECTION="$GRAPH_NODE_EMBEDDING_COLLECTION" \
    node "${REPO_ROOT}/backend/scripts/cleanup-deployment-content-admission.mjs"
}

mongo_query() {
  http POST "$MONGO_QUERY_URL" auth "$1"
}

wait_for_graph_node_embeddings() {
  local event_ids="$1" deadline id query response missing_json='[]'
  if ! printf '%s' "$event_ids" | jq -e \
       'type == "array" and length > 0 and all(type == "string" and length > 0)' \
       >/dev/null 2>&1; then
    fail "graph-node embedding settlement requires exact event ids" "$event_ids"
    return 1
  fi

  deadline=$((SECONDS + AGENT_WAIT_SECONDS))
  while [ "$SECONDS" -lt "$deadline" ]; do
    missing_json='[]'
    while IFS= read -r id; do
      query="$(jq -cn \
        --arg collection "$GRAPH_NODE_EMBEDDING_COLLECTION" \
        --arg event_id "$id" \
        '{collection:$collection,filter:{source_event_id:$event_id},
          projection:{_id:0,source_event_id:1},limit:1}')"
      response="$(mongo_query "$query")"
      if [ "$(status_of "$response")" != "200" ] \
         || ! body_of "$response" | jq -e --arg id "$id" \
              '.ok == true and .total >= 1 and
               (.rows | any(.source_event_id == $id))' >/dev/null 2>&1; then
        missing_json="$(jq -cn --argjson missing "$missing_json" \
          --arg id "$id" '$missing + [$id]')"
      fi
    done < <(printf '%s' "$event_ids" | jq -r '.[]')

    if printf '%s' "$missing_json" | jq -e 'length == 0' >/dev/null 2>&1; then
      pass "every verifier event has completed its graph-node embedding projection"
      return 0
    fi
    sleep 2
  done

  fail "graph-node embeddings did not settle before cleanup" "$missing_json"
  return 1
}

wait_for_event_turn_release() {
  local event_ids="$1" deadline payload response status last_body=''
  if ! printf '%s' "$event_ids" | jq -e \
       'type == "array" and length > 0 and
        all(type == "string" and length > 0) and
        length == (unique | length)' >/dev/null 2>&1; then
    fail "event-turn settlement requires unique exact owner ids" "$event_ids"
    return 1
  fi

  payload="$(jq -cn --argjson event_ids "$event_ids" '{event_ids:$event_ids}')"
  deadline=$((SECONDS + AGENT_WAIT_SECONDS))
  while [ "$SECONDS" -lt "$deadline" ]; do
    response="$(http POST "$EVENT_TURN_STATUS_URL" auth "$payload")"
    status="$(status_of "$response")"
    last_body="$(body_of "$response")"
    if [ "$status" != "200" ]; then
      fail "event-turn settlement status is queryable — expected 200, got ${status}" \
        "$(printf '%s' "$last_body" | head -c 500)"
      return 1
    fi
    if ! printf '%s' "$last_body" | jq -e --argjson ids "$event_ids" \
         'type == "object" and (.settled | type) == "boolean" and
          (.events | type) == "array" and
          ([.events[].event_id] | length) == ($ids | length) and
          ([.events[].event_id] | unique | sort) == ($ids | sort) and
          (.events | all(.state == "in_flight" or
                         .state == "redelivery_pending" or
                         .state == "released"))' >/dev/null 2>&1; then
      fail "event-turn settlement returned a malformed identity or state set" \
        "$(printf '%s' "$last_body" | jq -c . 2>/dev/null | head -c 500)"
      return 1
    fi
    if printf '%s' "$last_body" | jq -e \
         '.settled == true and (.events | all(.state == "released"))' \
         >/dev/null 2>&1; then
      pass "every verifier-owned agent turn released its settlement callback"
      return 0
    fi
    sleep 2
  done

  fail "agent turns did not release their settlement callbacks before cleanup" \
    "$(printf '%s' "$last_body" | jq -c . 2>/dev/null | head -c 500)"
  return 1
}

admission_row() {
  body_of "$1" | jq -c --arg document "$DOC_ID" \
    '.results[]? | select((.id // .["document/id"]) == $document)' | head -1
}

fixture_write_manifest() {
  local document_title="Deployment Content Admission Probe ${RUN_ID}"
  local garden_title="Deployment Admission Verification Garden"
  if [ "$RETAIN_REVIEW_DEMO" -eq 1 ]; then
    document_title="Local Translation Review Demo ${RUN_ID}"
    garden_title="Local Translation Review Demo Garden"
  fi
  mkdir -p "$FIXTURE_DIR"
  cat > "${FIXTURE_DIR}/resources.edn" <<EDN
;; Throwaway fixture written by scripts/verify-deployment-content-admission.sh.
{:namespace :${NS}
 :resources
 [{:document/id :${DOC_LOCAL}
   :document/title "${document_title}"
   :document/source-locale :en
   :document/visibility :public
   :document/source {:path "${SOURCE_REL}"}
   :document/anchor? true
   :document/generate-drafts? false}

  {:garden/id :${GARDEN_LOCAL}
   :garden/title "${garden_title}"
   :garden/status :active
   :garden/locales [:en :es :fr]}

  {:publication/id :${DOC_LOCAL}-es
   :publication/document :${DOC_LOCAL}
   :publication/garden :${GARDEN_LOCAL}
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :draft
   :publication/path "/verify-admission/${DOC_LOCAL}/es"
   :translation/review :required}

  {:publication/id :${DOC_LOCAL}-fr
   :publication/document :${DOC_LOCAL}
   :publication/garden :${GARDEN_LOCAL}
   :publication/locale :fr
   :publication/revision :source/current
   :publication/state :draft
   :publication/path "/verify-admission/${DOC_LOCAL}/fr"
   :translation/review :required}]}
EDN
}

# ShellCheck cannot see invocations made through EXIT/INT/TERM traps.
# shellcheck disable=SC2329
cleanup() {
  local code=$? signalled="${1:-}" cleanup_failed=0 translation_file
  local generated_file cleanup_summary deleted_total event_count
  trap - EXIT INT TERM
  [ -n "$signalled" ] && code="$signalled"

  if [ "$REVIEW_DEMO_READY" -eq 1 ] && [ "$code" -eq 0 ]; then
    note "retained the exact run-scoped database fixtures for review"
  elif [ "$FIXTURE_OWNED" -eq 1 ]; then
    if ! quarantine_source_resources; then
      CLEANUP_LAST_ERROR="could not fence the source fixture outside resource roots"
      cleanup_failed=1
    elif ! await_cleanup_quiescence; then
      cleanup_failed=1
    else
      event_count="$(printf '%s' "$CLEANUP_EVENT_IDS" | jq -r 'length')"
      if [ "$event_count" -gt 0 ]; then
        if cleanup_summary="$(cleanup_durable_fixtures)"; then
          deleted_total="$(printf '%s' "$cleanup_summary" | jq -r \
            '.deletedTotal // 0')"
          note "torn down the exact run-scoped database fixtures (${deleted_total} rows)"
        else
          CLEANUP_LAST_ERROR="could not remove the exact run-scoped database fixtures"
          cleanup_failed=1
        fi
      else
        note "the fenced run wrote no durable database fixtures"
      fi
    fi
  fi

  if [ "$cleanup_failed" -eq 1 ]; then
    printf '%s   FAIL%s  cleanup could not prove and remove the exact run scope: %s\n' \
      "$C_RED" "$C_RESET" "${CLEANUP_LAST_ERROR:-unknown cleanup failure}" >&2
    if [ -n "$CLEANUP_QUARANTINE_DIR" ] \
       && [ -d "$CLEANUP_QUARANTINE_DIR" ]; then
      printf '%s   WARN%s  fenced evidence remains outside admission scope: %s\n' \
        "$C_YELLOW" "$C_RESET" "$CLEANUP_QUARANTINE_DIR" >&2
    fi
    print_translation_content_cleanup >&2
    [ "$code" -eq 0 ] && code=1
    exit "$code"
  fi

  if [ "${#TRANSLATION_CONTENT_FILES[@]}" -gt 0 ]; then
    if [ "$REVIEW_DEMO_READY" -eq 1 ] && [ "$code" -eq 0 ]; then
      note "retained the exact agent translation entries for review"
    else
      for translation_file in "${TRANSLATION_CONTENT_FILES[@]}"; do
        if [ -e "$translation_file" ] && ! rm -f -- "$translation_file"; then
          printf '%s   FAIL%s  could not remove owned translation entry %s\n' \
            "$C_RED" "$C_RESET" "$translation_file" >&2
          cleanup_failed=1
        fi
      done
      [ "$cleanup_failed" -eq 0 ] \
        && note "torn down the exact agent translation entries"
    fi
  fi

  if [ -n "$GENERATED_MANIFEST_FILE" ] || [ -n "$GENERATED_SOURCE_FILE" ] \
     || [ -n "$GENERATED_COMPLETION_FILE" ]; then
    if [ "$REVIEW_DEMO_READY" -eq 1 ] && [ "$code" -eq 0 ]; then
      note "retained the generated post bytes and admission-completion marker for review"
    else
      for generated_file in "$GENERATED_MANIFEST_FILE" "$GENERATED_SOURCE_FILE" \
                            "$GENERATED_COMPLETION_FILE"; do
        [ -n "$generated_file" ] || continue
        if [ -e "$generated_file" ] && ! rm -f -- "$generated_file"; then
          printf '%s   FAIL%s  could not remove owned generated file %s\n' \
            "$C_RED" "$C_RESET" "$generated_file" >&2
          cleanup_failed=1
        fi
      done
      [ "$cleanup_failed" -eq 0 ] \
        && note "torn down the three exact generated draft files"
    fi
  fi

  if [ "$FIXTURE_OWNED" -eq 1 ] && [ -d "$FIXTURE_DIR" ]; then
    if [ "$REVIEW_DEMO_READY" -eq 1 ] && [ "$code" -eq 0 ]; then
      note "retained the exact external source/garden fixture for review"
    elif rm -rf -- "$FIXTURE_DIR"; then
      note "torn down ${FIXTURE_DIR#"$REPO_ROOT"/}"
    else
      printf '%s   FAIL%s  could not remove owned fixture %s\n' \
        "$C_RED" "$C_RESET" "$FIXTURE_DIR" >&2
      cleanup_failed=1
    fi
  fi
  if { [ "$REVIEW_DEMO_READY" -eq 0 ] || [ "$code" -ne 0 ]; } \
     && [ -n "$REVIEW_DEMO_SOURCE_DIR" ] && [ -d "$REVIEW_DEMO_SOURCE_DIR" ]; then
    if ! rm -rf -- "$REVIEW_DEMO_SOURCE_DIR"; then
      printf '%s   FAIL%s  could not remove owned review source directory %s\n' \
        "$C_RED" "$C_RESET" "$REVIEW_DEMO_SOURCE_DIR" >&2
      cleanup_failed=1
    fi
  fi
  if [ -n "$CLEANUP_QUARANTINE_DIR" ] && [ -d "$CLEANUP_QUARANTINE_DIR" ]; then
    case "$CLEANUP_QUARANTINE_DIR" in
      /tmp/knoxx-deployment-admission-"${RUN_ID}".*)
        if rm -rf -- "$CLEANUP_QUARANTINE_DIR"; then
          note "removed the fenced cleanup quarantine"
        else
          printf '%s   FAIL%s  could not remove cleanup quarantine %s\n' \
            "$C_RED" "$C_RESET" "$CLEANUP_QUARANTINE_DIR" >&2
          cleanup_failed=1
        fi
        ;;
      *)
        printf '%s   FAIL%s  refused unexpected cleanup quarantine path %s\n' \
          "$C_RED" "$C_RESET" "$CLEANUP_QUARANTINE_DIR" >&2
        cleanup_failed=1
        ;;
    esac
  fi
  if [ "$cleanup_failed" -eq 1 ] && [ "$code" -eq 0 ]; then code=1; fi
  exit "$code"
}
trap cleanup EXIT
trap 'trap - EXIT; cleanup 130' INT
trap 'trap - EXIT; cleanup 143' TERM

printf '%s\n' "${C_BOLD}Knoxx deployment content admission — live verification${C_RESET}"
note "base url       ${BASE_URL}"
note "run id         ${RUN_ID}"
note "contracts dir  ${CONTRACTS_DIR}"

for tool in curl jq node sha256sum realpath; do
  command -v "$tool" >/dev/null 2>&1 || die "missing required tool: ${tool}"
done
[ -r "${REPO_ROOT}/backend/scripts/cleanup-deployment-content-admission.mjs" ] \
  || die "missing durable fixture cleanup helper"

case "$VERIFY_GENERATED_DRAFTS" in
  1|true|TRUE|yes|YES) RUN_GENERATED_DRAFTS=1 ;;
  0|false|FALSE|no|NO) RUN_GENERATED_DRAFTS=0 ;;
  *) die "KNOXX_VERIFY_GENERATED_DRAFTS must be true or false" ;;
esac

case "$KEEP_REVIEW_DEMO" in
  1|true|TRUE|yes|YES)
    RETAIN_REVIEW_DEMO=1
    RUN_GENERATED_DRAFTS=1
    ;;
  0|false|FALSE|no|NO) RETAIN_REVIEW_DEMO=0 ;;
  *) die "KNOXX_VERIFY_KEEP_REVIEW_DEMO must be true or false" ;;
esac

if [ "$RUN_GENERATED_DRAFTS" -eq 1 ]; then
  [ -n "$GENERATED_CONTRACTS_DIR" ] \
    || die "KNOXX_GENERATED_CONTRACTS_DIR is required when generated-draft verification is enabled"
  GENERATED_CONTRACTS_DIR="$(cd "$REPO_ROOT" && realpath -m "$GENERATED_CONTRACTS_DIR")"
  case "$GENERATED_CONTRACTS_DIR" in
    "$REPO_ROOT"|"$REPO_ROOT"/*)
      die "generated contracts must live outside the Knoxx repository: ${GENERATED_CONTRACTS_DIR}"
      ;;
  esac
  if [ "$RETAIN_REVIEW_DEMO" -eq 1 ]; then
    generated_root="$(dirname "$GENERATED_CONTRACTS_DIR")"
    FIXTURE_DIR="${GENERATED_CONTRACTS_DIR}/namespaces/knoxx-review-demo-${RUN_ID}"
    REVIEW_DEMO_SOURCE_DIR="${generated_root}/review-demos/${RUN_ID}"
    SOURCE_REL="review-demos/${RUN_ID}/source-${RUN_ID}.md"
    SOURCE_FILE="${generated_root}/${SOURCE_REL}"
  fi
  note "generated root ${GENERATED_CONTRACTS_DIR}"
  [ "$RETAIN_REVIEW_DEMO" -eq 1 ] \
    && note "review demo    enabled; a green run retains only its exact external files"
else
  note "generated path disabled (set KNOXX_VERIFY_GENERATED_DRAFTS=true for the isolated post-drafter proof)"
fi

transport_kind="$(knoxx_credential_transport_kind "$BASE_URL" 2>/dev/null)" \
  || die "KNOXX_BASE_URL must use HTTPS or exact loopback HTTP and must not contain userinfo"
[ "$transport_kind" = "loopback-http" ] && LOOPBACK_HTTP=1

[ -n "$API_KEY" ] \
  || die "KNOXX_API_KEY is required and must match the running backend"
[[ "$HTTP_TIMEOUT_SECONDS" =~ ^[1-9][0-9]*$ ]] \
  || die "KNOXX_VERIFY_HTTP_TIMEOUT_SECONDS must be a positive integer"
[[ "$AGENT_WAIT_SECONDS" =~ ^[1-9][0-9]*$ ]] \
  || die "KNOXX_VERIFY_AGENT_WAIT_SECONDS must be a positive integer"
[ -n "$PUBLICATION_CONTENT_ROOT" ] \
  || die "KNOXX_VERIFY_PUBLICATION_CONTENT_ROOT must equal the running backend's content root"
PUBLICATION_CONTENT_ROOT="$(cd "$REPO_ROOT" && realpath -m "$PUBLICATION_CONTENT_ROOT")"
case "$PUBLICATION_CONTENT_ROOT" in
  /|"$REPO_ROOT"|"$REPO_ROOT"/*)
    die "verification translation content must live outside the Knoxx repository: ${PUBLICATION_CONTENT_ROOT}"
    ;;
esac
[ -d "$PUBLICATION_CONTENT_ROOT" ] \
  || die "publication content root does not exist: ${PUBLICATION_CONTENT_ROOT}"
[[ "$EVENTS_COLLECTION" =~ ^[A-Za-z][A-Za-z0-9._-]{0,127}$ ]] \
  || die "MONGODB_EVENTS_COLLECTION is not a safe Mongo collection name"
[[ "$VECTOR_COLLECTION" =~ ^[A-Za-z][A-Za-z0-9._-]{0,127}$ ]] \
  || die "MONGODB_VECTOR_HOT_COLLECTION is not a safe Mongo collection name"
[[ "$GRAPH_NODE_EMBEDDING_COLLECTION" =~ ^[A-Za-z][A-Za-z0-9._-]{0,127}$ ]] \
  || die "MONGODB_GRAPH_NODE_EMBEDDING_COLLECTION is not a safe Mongo collection name"
[ -d "$CONTRACTS_DIR" ] || die "contracts directory not found: ${CONTRACTS_DIR}"
[ ! -e "$FIXTURE_DIR" ] \
  || die "fixture directory already exists: ${FIXTURE_DIR}; remove the stale verifier fixture first"

health_args=(-q -sS -o /dev/null --max-time 5)
[ "$LOOPBACK_HTTP" -eq 1 ] && health_args+=(--noproxy '*')
curl "${health_args[@]}" "${BASE_URL}/health" 2>/dev/null \
  || die "no Knoxx backend answers ${BASE_URL}/health"
note "backend is reachable"

step "0. the running backend serves the selected resource root"
FIXTURE_OWNED=1
fixture_write_manifest

probe=""
for _attempt in {1..20}; do
  probe="$(http GET "/api/publications/documents" auth)"
  if [ "$(status_of "$probe")" = "200" ] \
     && body_of "$probe" | jq -e --arg id "$DOC_ID" \
          '[.documents[].document.id] | index($id) != null' >/dev/null 2>&1; then
    break
  fi
  sleep 0.25
done

if [ "$(status_of "$probe")" = "200" ] \
   && body_of "$probe" | jq -e --arg id "$DOC_ID" \
        '[.documents[].document.id] | index($id) != null' >/dev/null 2>&1; then
  pass "the unique anchored fixture is visible over the live resource projection"
else
  note "The backend may be serving another checkout/root. Check: pm2 describe knoxx-backend | grep cwd"
  note "Response: $(body_of "$probe" | head -c 500)"
  die "cannot verify a backend that does not serve this fixture"
fi

step "1. anonymous admission is refused before filesystem work"
anonymous_body="$(jq -cn --arg document "$DOC_ID" \
  '{document:$document,generateDrafts:false}')"
expect_status "POST ${ADMISSION_URL} refuses an anonymous caller" "401 403" \
  "$(http POST "$ADMISSION_URL" anon "$anonymous_body")"

step "2. a missing anchored source fails atomically"
missing="$(http POST "$ADMISSION_URL" auth "$anonymous_body")"
expect_status "the exact anchored document with no source is refused" "409" "$missing"
expect_jq "the refusal names document_source_missing" \
  '.error.code == "document_source_missing"' "$missing"

no_event_query="$(jq -cn --arg collection "$EVENTS_COLLECTION" --arg document "$DOC_ID" \
  '{collection:$collection,filter:{"extra.document_id":$document},
    projection:{_id:0,id:1,kind:1},limit:20}')"
no_events="$(mongo_query "$no_event_query")"
expect_status "the durable event collection is queryable" "200" "$no_events"
expect_jq "missing-source preflight wrote no event prefix" \
  '.ok == true and .total == 0' "$no_events"

mkdir -p "$(dirname "$SOURCE_FILE")"
cat > "$SOURCE_FILE" <<EOF
# Deployment admission probe

This source belongs to verification run ${RUN_ID}.
Every declared target locale must receive one revision-bound translation claim.
Human review remains mandatory and no draft may be published by admission.
EOF
EXPECTED_REVISION="sha256-$(sha256sum "$SOURCE_FILE" | awk '{print $1}')"

step "3. the deployment-shaped anchor sweep indexes and dispatches"
sweep_body='{"anchors":true,"generateDrafts":false}'
first="$(http POST "$ADMISSION_URL" auth "$sweep_body")"
expect_status "POST ${ADMISSION_URL} anchor sweep" "200" "$first"
expect_jq "the deployment gate response is explicitly green" \
  '.ok == true and (.admitted | type) == "number" and .admitted >= 1 and
   (.failed | type) == "number" and .failed == 0 and
   (.results | type) == "array" and
   ((.["indexed-events"] // .indexedEvents) | type) == "number" and
   (.["indexed-events"] // .indexedEvents) >= 2' \
  "$first"

first_row="$(admission_row "$first")"
expect_json "the anchored fixture, not merely some other anchor, was admitted" \
  'type == "object" and ((.id // .["document/id"]) == $document)' \
  "$first_row" --arg document "$DOC_ID"
expect_json "admission is pinned to the exact source digest" \
  '(.["source-revision"] // .["document/source-revision"]) == $revision' \
  "$first_row" --arg revision "$EXPECTED_REVISION"
expect_json "both immutable index events were newly recorded" \
  '(.["source-event-status"] // .["index/source-event-status"]) == "recorded" and
   (.["event-status"] // .["index/event-status"]) == "recorded"' \
  "$first_row"
expect_json "every declared cross-locale relation reached the agent dispatcher" \
  '.translation.runner == "agent" and .translation.considered == 2 and
   .translation.admissible == 2 and (.translation.dispatched | length) == 2 and
   ([.translation.dispatched[].outcome] | all(. == "dispatch/accepted")) and
   (.translation.dispatched |
      all(((.["run-id"] // .["translation/run-id"] // "") | length) > 0 and
          (((.detail // .["dispatch/detail"] // "") |
             contains("no enabled trigger")) | not)))' \
  "$first_row"
expect_json "the per-document dispatch reports no hidden failure" \
  '.ok == true and .failed == 0' "$first_row"

SOURCE_EVENT_ID="$(printf '%s' "$first_row" | jq -r \
  '.["source-event-id"] // .["index/source-event-id"] // empty')"
INDEX_EVENT_ID="$(printf '%s' "$first_row" | jq -r \
  '.["event-id"] // .["index/event-id"] // empty')"
if [ -n "$SOURCE_EVENT_ID" ] && [ -n "$INDEX_EVENT_ID" ]; then
  pass "the response exposes both content-addressed event identities"
else
  fail "the response exposes both content-addressed event identities" "$first_row"
fi

step "4. the source and indexed signal are searchable with embeddings"
if [ -n "$SOURCE_EVENT_ID" ] && [ -n "$INDEX_EVENT_ID" ]; then
  event_query="$(jq -cn --arg collection "$EVENTS_COLLECTION" \
    --arg source "$SOURCE_EVENT_ID" --arg indexed "$INDEX_EVENT_ID" \
    '{collection:$collection,filter:{id:{"$in":[$source,$indexed]}},
      projection:{_id:0,id:1,kind:1,text:1,extra:1},limit:10}')"
  events="$(mongo_query "$event_query")"
  expect_status "the exact admission events are queryable" "200" "$events"
  expect_jq "one docs snapshot and one document-indexed signal exist" \
    '.ok == true and .total == 2 and
     ([.rows[].kind] | sort) == (["docs","publication.document.indexed"] | sort)' \
    "$events"
  expect_jq "the indexed facts retain the anchored document and source revision" \
    '.rows | length == 2 and
     all(.extra.document_id == $document and .extra.source_revision == $revision)' \
    "$events" --arg document "$DOC_ID" --arg revision "$EXPECTED_REVISION"
  expect_jq "the indexed facts retain one nonblank organization and project scope" \
    '.rows | length == 2 and
     ([.[].extra.org_id | select(type == "string" and length > 0)] | unique | length) == 1 and
     ([.[].extra.project | select(type == "string" and length > 0)] | unique | length) == 1' \
    "$events"
  ADMISSION_ORG_ID="$(body_of "$events" | jq -r \
    '[.rows[].extra.org_id | select(type == "string" and length > 0)]
     | unique | if length == 1 then .[0] else empty end')"
  ADMISSION_PROJECT="$(body_of "$events" | jq -r \
    '[.rows[].extra.project | select(type == "string" and length > 0)]
     | unique | if length == 1 then .[0] else empty end')"

  vector_query="$(jq -cn --arg collection "$VECTOR_COLLECTION" \
    --arg source "$SOURCE_EVENT_ID" --arg indexed "$INDEX_EVENT_ID" \
    '{collection:$collection,filter:{parent_id:{"$in":[$source,$indexed]}},
      projection:{_id:0,parent_id:1,embedding_model:1,embedding_dimensions:1,embedding:1},limit:10}')"
  vectors="$(mongo_query "$vector_query")"
  expect_status "the exact admission vectors are queryable" "200" "$vectors"
  expect_jq "both events have exact nomic 768-dimensional embedding vectors" \
    '.ok == true and .total == 2 and
     (.rows | all(.embedding_model == "nomic-embed-text" and
                  .embedding_dimensions == 768 and
                  (.embedding | type) == "array" and
                  (.embedding | length) == 768))' \
    "$vectors"
else
  fail "event/vector checks require the two admission ids" "ids were absent"
fi

step "5. both local translation agents finish and save review events"
candidate_query="$(jq -cn --arg collection "$EVENTS_COLLECTION" \
  --arg document "$DOC_ID" --arg revision "$EXPECTED_REVISION" \
  '{collection:$collection,
    filter:{kind:"translation.segment","extra.document_id":$document,
            "extra.source_revision":$revision},
    projection:{_id:0,id:1,text:1,meta:1,extra:1},limit:100}')"
candidate_events=""
deadline=$((SECONDS + AGENT_WAIT_SECONDS))
while [ "$SECONDS" -lt "$deadline" ]; do
  candidate_events="$(mongo_query "$candidate_query")"
  if [ "$(status_of "$candidate_events")" = "200" ] \
     && body_of "$candidate_events" | jq -e \
          '(.rows | type) == "array" and
           ([.rows[] | (.extra.target_lang // .meta.target_lang)] | unique | sort) ==
             (["es","fr"] | sort)' \
          >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
expect_status "candidate translation events are queryable after agent completion" \
  "200" "$candidate_events"
expect_jq "gemma4:e2b completed both locale translations as durable events" \
  '([.rows[] | (.extra.target_lang // .meta.target_lang)] | unique | sort) ==
     (["es","fr"] | sort) and
   (.rows | all(.extra.source_lang == "en" and
                (.extra.target_lang == "es" or .extra.target_lang == "fr") and
                .extra.mt_model == "gemma4:e2b" and
                .extra.status == "in_review" and
                .extra.producer == "knoxx-contract-agent" and
                .extra.document_id == $document and .extra.source_revision == $revision and
                ((.text // "") | length) > 0 and
                ((.extra.source_text // "") | length) > 0)) and
   ([.rows[] | select(.text != .extra.source_text) | .extra.target_lang] |
      unique | sort) == (["es","fr"] | sort)' \
  "$candidate_events" --arg document "$DOC_ID" --arg revision "$EXPECTED_REVISION"

CANDIDATE_EVENT_RAW_IDS="$(body_of "$candidate_events" | jq -c '[.rows[]?.id]')"
CANDIDATE_EVENT_ROW_COUNT="$(body_of "$candidate_events" | jq -r '.total // (.rows | length) // 0')"
if ! [[ "$CANDIDATE_EVENT_ROW_COUNT" =~ ^[0-9]+$ ]]; then
  fail "candidate translation response exposes a numeric row count" \
    "$CANDIDATE_EVENT_ROW_COUNT"
  CANDIDATE_EVENT_ROW_COUNT=0
fi
if printf '%s' "$CANDIDATE_EVENT_RAW_IDS" | jq -e \
     'type == "array" and length > 0 and
      all(type == "string" and length > 0) and
      length == (unique | length)' \
     >/dev/null 2>&1; then
  pass "raw candidate translation rows expose unique nonblank event ids"
  CANDIDATE_EVENT_IDS="$(printf '%s' "$CANDIDATE_EVENT_RAW_IDS" | jq -c 'unique')"
else
  fail "raw candidate translation rows expose unique nonblank event ids" \
    "$CANDIDATE_EVENT_RAW_IDS"
  CANDIDATE_EVENT_IDS='[]'
fi

candidate_vector_query="$(jq -cn --arg collection "$VECTOR_COLLECTION" \
  --argjson ids "$CANDIDATE_EVENT_IDS" \
  '{collection:$collection,filter:{parent_id:{"$in":$ids}},
    projection:{_id:0,parent_id:1,embedding_model:1,embedding_dimensions:1,embedding:1},limit:500}')"
candidate_vectors=""
candidate_vector_deadline=$((deadline + 30))
while [ "$SECONDS" -lt "$candidate_vector_deadline" ]; do
  candidate_vectors="$(mongo_query "$candidate_vector_query")"
  if [ "$(status_of "$candidate_vectors")" = "200" ] \
     && body_of "$candidate_vectors" | jq -e --argjson ids "$CANDIDATE_EVENT_IDS" \
          '($ids | length) > 0 and
           ([.rows[].parent_id] | unique | sort) == ($ids | sort) and
           (.rows | all(.embedding_model == "nomic-embed-text" and
                        .embedding_dimensions == 768 and
                        (.embedding | type) == "array" and
                        (.embedding | length) == 768))' >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
expect_status "candidate translation vectors are queryable by stable event id" \
  "200" "$candidate_vectors"
expect_jq "every candidate event has an exact nomic 768-dimensional vector" \
  '.ok == true and ($ids | length) > 0 and
   ([.rows[].parent_id] | unique | sort) == ($ids | sort) and
   (.rows | all(.embedding_model == "nomic-embed-text" and
                .embedding_dimensions == 768 and
                (.embedding | type) == "array" and
                (.embedding | length) == 768))' \
  "$candidate_vectors" --argjson ids "$CANDIDATE_EVENT_IDS"

step "6. unchanged redeployment reuses events and translation claims"
replay="$(http POST "$ADMISSION_URL" auth "$sweep_body")"
expect_status "the unchanged anchor sweep succeeds" "200" "$replay"
expect_jq "the replay remains an explicitly green deployment result" \
  '.ok == true and .failed == 0 and .admitted >= 1' "$replay"
replay_row="$(admission_row "$replay")"
expect_json "the replay returns the same source and index event ids" \
  '((.["source-event-id"] // .["index/source-event-id"]) == $source) and
   ((.["event-id"] // .["index/event-id"]) == $indexed)' \
  "$replay_row" --arg source "$SOURCE_EVENT_ID" --arg indexed "$INDEX_EVENT_ID"
expect_json "both immutable events resolve as existing facts" \
  '(.["source-event-status"] // .["index/source-event-status"]) == "existing" and
   (.["event-status"] // .["index/event-status"]) == "existing"' \
  "$replay_row"
expect_json "the replay accepts no new translation run (empty or duplicate-only)" \
  '(.translation.considered == 2) and
   (.translation.dispatched | length) <= 2 and
   (.translation.dispatched | all(.outcome == "dispatch/duplicate")) and
   ([.translation.dispatched[].outcome] | index("dispatch/accepted") == null)' \
  "$replay_row"

if [ -n "$SOURCE_EVENT_ID" ] && [ -n "$INDEX_EVENT_ID" ]; then
  replay_events="$(mongo_query "$event_query")"
  expect_jq "event replay did not append duplicate durable rows" \
    '.total == 2' "$replay_events"
fi
claims_query="$(jq -cn --arg document "$DOC_ID" \
  '{collection:"knoxx_translation_dispatches",filter:{document_wire_id:$document},
    projection:{_id:0,dispatch_key:1,document_wire_id:1,outcome:1,batch_id:1,binding_edn:1},limit:10}')"
claims="$(mongo_query "$claims_query")"
expect_status "translation claims are queryable" "200" "$claims"
expect_jq "exactly one claim exists per declared target locale" \
  '.ok == true and .total == 2 and
   (.rows | all(.document_wire_id == $document)) and
   ([.rows[].binding_edn | capture(":dispatch/locale :(?<locale>[^, }]+)").locale] | sort) ==
     (["es","fr"] | sort)' \
  "$claims" --arg document "$DOC_ID"

replay_candidates="$(mongo_query "$candidate_query")"
expect_status "candidate events remain queryable after replay" "200" "$replay_candidates"
expect_jq "replay preserves the exact candidate event identity set" \
  '.total == $count and (.rows | length) == $count and
   ([.rows[].id] | unique | sort) == ($ids | sort)' \
  "$replay_candidates" --argjson ids "$CANDIDATE_EVENT_IDS" \
  --argjson count "$CANDIDATE_EVENT_ROW_COUNT"

step "7. translation remains human-review blocked and unpublished"
reviews="$(http GET "$REVIEWS_URL" auth)"
expect_status "the review inventory is readable" "200" "$reviews"
expect_jq "both locale relations are visible and neither is approved" \
  '[.reviews[] | select(.document == $document)] as $rows |
   ($rows | length) == 2 and
   ([$rows[].publication] | sort) == ([$publication_es,$publication_fr] | sort) and
   ($rows | all(.garden == $garden)) and
   ([$rows[].locale] | sort) == (["es","fr"] | sort) and
   ($rows | all(.approved == false and .work_state != "approved"))' \
  "$reviews" --arg document "$DOC_ID" --arg garden "$GARDEN_ID" \
  --arg publication_es "$PUB_ES_ID" --arg publication_fr "$PUB_FR_ID"

source_translation_pairs="$(body_of "$candidate_events" | jq -c \
  '[.rows[] | {document:.extra.document_id,
                locale:.extra.target_lang,
                source_revision:.extra.source_revision,
                output_revision:.extra.candidate_revision}] |
   unique | sort_by(.locale)')"
source_pairs_valid=1
expect_json "candidate events expose one exact output revision per source locale relation" \
  'length == 2 and
   ([.[].locale] | sort) == (["es","fr"] | sort) and
   all(.document == $document and .source_revision == $revision and
       (.output_revision | type) == "string" and
       (.output_revision | length) > 0) and
   ([.[].output_revision] | unique | length) == 2' \
  "$source_translation_pairs" --arg document "$DOC_ID" \
  --arg revision "$EXPECTED_REVISION" || source_pairs_valid=0
source_reviews_valid=1
expect_jq "review hydration authenticates the same two stored agent outputs" \
  '[.reviews[] | select(.document == $document)] as $rows |
   ($rows | length) == 2 and
   ($rows | all(.candidate_present == true and .reviewable == true and
                .hydration_state == "displayable" and
                .content_source == "agent" and .approved == false and
                .work_state == "ready")) and
   (($rows | map({document,locale,source_revision:.revision,
                  output_revision:.translation_revision}) |
             sort_by(.locale)) == $pairs)' \
  "$reviews" --arg document "$DOC_ID" \
  --argjson pairs "$source_translation_pairs" || source_reviews_valid=0
if [ "$source_pairs_valid" -eq 1 ] && [ "$source_reviews_valid" -eq 1 ] \
   && track_translation_content "source" "$source_translation_pairs"; then
  SOURCE_TRANSLATION_CONTENT_SETTLED=1
fi

expect_status "CMS refuses an anonymous draft-state PATCH" "401 403" \
  "$(http PATCH "$CMS_INTENT_ES_URL" anon '{"state":"draft"}')"
draft_patch="$(http PATCH "$CMS_INTENT_ES_URL" auth '{"state":"draft"}')"
expect_status "CMS accepts an idempotent draft-state PATCH" "200" "$draft_patch"
expect_jq "the PATCH returns draft desired state with no materialization" \
  '.id == $publication and .desired == "draft" and .observed == null' \
  "$draft_patch" --arg publication "$PUB_ES_ID"

cms="$(http GET "$CMS_URL" auth)"
expect_status "the fixture CMS projection is readable" "200" "$cms"
expect_jq "both locale relations remain unpublished drafts" \
  '.document.id == $document and (.publications | length) == 2 and
   (.publications | all(.desired == "draft" and .observed == null))' \
  "$cms" --arg document "$DOC_ID"

receipts="$(http GET "$RECEIPTS_URL" auth)"
case "$(status_of "$receipts")" in
  200)
    pass "the publication receipt journal is readable ${C_DIM}(200)${C_RESET}"
    expect_jq "admission emitted no materialization receipt for the fixture" \
      '[.receipts[]? | select([.. | strings] | index($document))] | length == 0' \
      "$receipts" --arg document "$DOC_ID"
    ;;
  503)
    pass "publication reconciliation is unconfigured, so no materializer exists ${C_DIM}(503)${C_RESET}"
    ;;
  *)
    fail "publication receipt journal must answer 200 or explicit unconfigured 503" \
      "$(status_of "$receipts"): $(body_of "$receipts" | head -c 300)"
    ;;
esac
note "No reconcile request or published-state PATCH is issued anywhere in this script."

if [ "$RUN_GENERATED_DRAFTS" -eq 1 ]; then
  step "8. one bounded post-drafter creates a terminal, translated draft"

  [ -n "$ADMISSION_ORG_ID" ] \
    || die "generated-draft identity requires the admitted organization scope"
  [ -n "$ADMISSION_PROJECT" ] \
    || die "generated-draft identity requires the admitted project scope"
  revision_edn="$(jq -rn --arg value "$EXPECTED_REVISION" '$value | tojson')"
  org_edn="$(jq -rn --arg value "$ADMISSION_ORG_ID" '$value | tojson')"
  project_edn="$(jq -rn --arg value "$ADMISSION_PROJECT" '$value | tojson')"
  canonical_draft_policy="[1 :${DOC_ID} ${revision_edn} :en ${org_edn} ${project_edn} [[:${GARDEN_ID} [:en :es :fr]]]]"
  generated_fingerprint="$(printf '%s' "$canonical_draft_policy" \
    | sha256sum | awk '{print $1}')"
  generated_token="${generated_fingerprint:0:24}"
  GENERATED_DOC_ID="knoxx.generated/post-${generated_token}"
  generated_local="post-${generated_token}"
  generated_root="$(dirname "$GENERATED_CONTRACTS_DIR")"
  GENERATED_MANIFEST_FILE="${GENERATED_CONTRACTS_DIR}/namespaces/${generated_local}.edn"
  GENERATED_SOURCE_FILE="${generated_root}/drafts/${generated_local}.md"
  GENERATED_COMPLETION_FILE="${generated_root}/.knoxx/draft-admission-completions/${generated_local}.edn"
  generated_projection_url="/api/publications/documents/knoxx.generated%2F${generated_local}"
  generated_cms_url="/api/cms/publications/documents/knoxx.generated%2F${generated_local}"

  case "$GENERATED_MANIFEST_FILE" in
    "$REPO_ROOT"|"$REPO_ROOT"/*)
      die "derived manifest resolved inside the repository: ${GENERATED_MANIFEST_FILE}"
      ;;
  esac
  case "$GENERATED_SOURCE_FILE" in
    "$REPO_ROOT"|"$REPO_ROOT"/*)
      die "derived source resolved inside the repository: ${GENERATED_SOURCE_FILE}"
      ;;
  esac
  case "$GENERATED_COMPLETION_FILE" in
    "$REPO_ROOT"|"$REPO_ROOT"/*)
      die "derived completion marker resolved inside the repository: ${GENERATED_COMPLETION_FILE}"
      ;;
  esac
  [ ! -e "$GENERATED_MANIFEST_FILE" ] \
    || die "unique generated manifest already exists: ${GENERATED_MANIFEST_FILE}"
  [ ! -e "$GENERATED_SOURCE_FILE" ] \
    || die "unique generated source already exists: ${GENERATED_SOURCE_FILE}"
  [ ! -e "$GENERATED_COMPLETION_FILE" ] \
    || die "unique generated completion marker already exists: ${GENERATED_COMPLETION_FILE}"
  pass "the topology-addressed generated bytes and completion marker are outside the repository and initially absent"

  generation_body="$(jq -cn --arg document "$DOC_ID" \
    '{document:$document,generateDrafts:true}')"
  generation="$(http POST "$ADMISSION_URL" auth "$generation_body")"
  expect_status "one exact-document admission requests post drafting" "200" "$generation"
  generation_row="$(admission_row "$generation")"
  expect_json "the source revision requests exactly one still-needed generated draft" \
    '(.ok == true) and
     ((.["generate-drafts?"] // .["document/generate-drafts?"]) == true) and
     ((.["draft-generation-needed?"] // .["document/draft-generation-needed?"]) == true) and
     ((if has("draft-generation-complete?")
       then .["draft-generation-complete?"]
       elif has("document/draft-generation-complete?")
       then .["document/draft-generation-complete?"]
       else null
       end) == false)' \
    "$generation_row"
  GENERATION_REQUEST_SOURCE_EVENT_ID="$(printf '%s' "$generation_row" | jq -r \
    '.["source-event-id"] // .["index/source-event-id"] // empty')"
  GENERATION_REQUEST_INDEX_EVENT_ID="$(printf '%s' "$generation_row" | jq -r \
    '.["event-id"] // .["index/event-id"] // empty')"
  generation_request_ids="$(jq -cn \
    --arg source "$GENERATION_REQUEST_SOURCE_EVENT_ID" \
    --arg indexed "$GENERATION_REQUEST_INDEX_EVENT_ID" '[$source,$indexed]')"
  expect_json "the generation policy is recorded under two nonblank event identities" \
    'length == 2 and all(type == "string" and length > 0)' \
    "$generation_request_ids"

  generation_request_vector_query="$(jq -cn --arg collection "$VECTOR_COLLECTION" \
    --argjson ids "$generation_request_ids" \
    '{collection:$collection,filter:{parent_id:{"$in":$ids}},
      projection:{_id:0,parent_id:1,embedding_model:1,embedding_dimensions:1,embedding:1},limit:20}')"
  generation_request_vectors="$(mongo_query "$generation_request_vector_query")"
  expect_status "generation-request admission vectors are queryable" \
    "200" "$generation_request_vectors"
  expect_jq "both generation-request events have exact nomic 768-dimensional vectors" \
    '($ids | length) == 2 and
     ([.rows[].parent_id] | unique | sort) == ($ids | sort) and
     (.rows | all(.embedding_model == "nomic-embed-text" and
                  .embedding_dimensions == 768 and
                  (.embedding | type) == "array" and
                  (.embedding | length) == 768))' \
    "$generation_request_vectors" --argjson ids "$generation_request_ids"

  generated_deadline=$((SECONDS + AGENT_WAIT_SECONDS))
  while [ "$SECONDS" -lt "$generated_deadline" ]; do
    if [ -s "$GENERATED_MANIFEST_FILE" ] && [ -s "$GENERATED_SOURCE_FILE" ] \
       && [ -s "$GENERATED_COMPLETION_FILE" ]; then
      break
    fi
    sleep 2
  done
  if [ -s "$GENERATED_MANIFEST_FILE" ] && [ -s "$GENERATED_SOURCE_FILE" ] \
     && [ -s "$GENERATED_COMPLETION_FILE" ]; then
    pass "gemma4:e2b saved immutable draft bytes and recursive admission wrote its completion marker"
    expected_completion_marker="{:draft/id :${GENERATED_DOC_ID}, :draft/policy-fingerprint \"${generated_fingerprint}\", :draft/admission-complete? true}"
    actual_completion_marker="$(sed -n '1p' "$GENERATED_COMPLETION_FILE")"
    if [ "$actual_completion_marker" = "$expected_completion_marker" ]; then
      pass "the completion marker is bound to the exact canonical topology fingerprint"
    else
      fail "the completion marker must match the expected draft identity and topology" \
        "$actual_completion_marker"
    fi
  else
    fail "the post-drafter must save both generated files and complete recursive admission before timeout" \
      "manifest=${GENERATED_MANIFEST_FILE}; source=${GENERATED_SOURCE_FILE}; completion=${GENERATED_COMPLETION_FILE}"
  fi

  if [ -s "$GENERATED_SOURCE_FILE" ]; then
    GENERATED_REVISION="sha256-$(sha256sum "$GENERATED_SOURCE_FILE" | awk '{print $1}')"
  fi

  generated_projection=""
  generated_projection_deadline=$((generated_deadline + 30))
  while [ "$SECONDS" -lt "$generated_projection_deadline" ]; do
    generated_projection="$(http GET "$generated_projection_url" auth)"
    if [ "$(status_of "$generated_projection")" = "200" ]; then
      break
    fi
    sleep 2
  done
  expect_status "the generated manifest is admitted into the live resource projection" \
    "200" "$generated_projection"
  expect_jq "the generated document retains source lineage and cannot generate descendants" \
    '.document.id == $generated and
     .document["derived-from"] == $source and
     .document["derived-source-revision"] == $source_revision and
     .document["generate-drafts?"] == false' \
    "$generated_projection" --arg generated "$GENERATED_DOC_ID" \
    --arg source "$DOC_ID" --arg source_revision "$EXPECTED_REVISION"
  expect_jq "source and translated locale intents are drafts pinned to the static-site target" \
    '(.publications | length) == 3 and
     (.publications | all(.state == "draft" and
                          .target == "open-hax.publication/static-site")) and
     ([.publications[].locale] | sort) == (["en","es","fr"] | sort) and
     ([.publications[] | select(.locale == "en") | .review] == ["none"]) and
     ([.publications[] | select(.locale != "en") | .review] | all(. == "required"))' \
    "$generated_projection"

  generated_event_query="$(jq -cn --arg collection "$EVENTS_COLLECTION" \
    --arg document "$GENERATED_DOC_ID" --arg revision "$GENERATED_REVISION" \
    '{collection:$collection,
      filter:{"extra.document_id":$document,"extra.source_revision":$revision,
              kind:{"$in":["docs","publication.document.indexed"]}},
      projection:{_id:0,id:1,kind:1,extra:1},limit:20}')"
  generated_events=""
  generated_event_deadline=$((generated_deadline + 30))
  while [ "$SECONDS" -lt "$generated_event_deadline" ]; do
    generated_events="$(mongo_query "$generated_event_query")"
    if [ "$(status_of "$generated_events")" = "200" ] \
       && body_of "$generated_events" | jq -e \
            '.total == 2 and
             ([.rows[].kind] | sort) == (["docs","publication.document.indexed"] | sort)' \
            >/dev/null 2>&1; then
      break
    fi
    sleep 2
  done
  expect_status "generated source and index events are queryable" "200" "$generated_events"
  expect_jq "the generated document has one source snapshot and one indexed signal" \
    '.ok == true and .total == 2 and
     ([.rows[].kind] | sort) == (["docs","publication.document.indexed"] | sort)' \
    "$generated_events"
  GENERATED_SOURCE_EVENT_ID="$(body_of "$generated_events" | jq -r \
    '.rows[]? | select(.kind == "docs") | .id' | head -1)"
  GENERATED_INDEX_EVENT_ID="$(body_of "$generated_events" | jq -r \
    '.rows[]? | select(.kind == "publication.document.indexed") | .id' | head -1)"
  generated_admission_ids="$(jq -cn --arg source "$GENERATED_SOURCE_EVENT_ID" \
    --arg indexed "$GENERATED_INDEX_EVENT_ID" '[$source,$indexed]')"
  generated_vector_query="$(jq -cn --arg collection "$VECTOR_COLLECTION" \
    --argjson ids "$generated_admission_ids" \
    '{collection:$collection,filter:{parent_id:{"$in":$ids}},
      projection:{_id:0,parent_id:1,embedding_model:1,embedding_dimensions:1,embedding:1},limit:20}')"
  generated_vectors=""
  generated_vector_deadline=$((generated_deadline + 30))
  while [ "$SECONDS" -lt "$generated_vector_deadline" ]; do
    generated_vectors="$(mongo_query "$generated_vector_query")"
    if [ "$(status_of "$generated_vectors")" = "200" ] \
       && body_of "$generated_vectors" | jq -e --argjson ids "$generated_admission_ids" \
            '($ids | length) == 2 and
             ([.rows[].parent_id] | unique | sort) == ($ids | sort) and
             (.rows | all(.embedding_model == "nomic-embed-text" and
                          .embedding_dimensions == 768 and
                          (.embedding | type) == "array" and
                          (.embedding | length) == 768))' >/dev/null 2>&1; then
      break
    fi
    sleep 2
  done
  expect_status "generated admission vectors are queryable" "200" "$generated_vectors"
  expect_jq "both generated admission events have exact nomic 768-dimensional vectors" \
    '.ok == true and ($ids | length) == 2 and
     ([.rows[].parent_id] | unique | sort) == ($ids | sort) and
     (.rows | all(.embedding_model == "nomic-embed-text" and
                  .embedding_dimensions == 768 and
                  (.embedding | type) == "array" and
                  (.embedding | length) == 768))' \
    "$generated_vectors" --argjson ids "$generated_admission_ids"

  generated_candidate_query="$(jq -cn --arg collection "$EVENTS_COLLECTION" \
    --arg document "$GENERATED_DOC_ID" --arg revision "$GENERATED_REVISION" \
    '{collection:$collection,
      filter:{kind:"translation.segment","extra.document_id":$document,
              "extra.source_revision":$revision},
      projection:{_id:0,id:1,text:1,meta:1,extra:1},limit:100}')"
  generated_candidates=""
  generated_translation_deadline=$((SECONDS + AGENT_WAIT_SECONDS))
  while [ "$SECONDS" -lt "$generated_translation_deadline" ]; do
    generated_candidates="$(mongo_query "$generated_candidate_query")"
    if [ "$(status_of "$generated_candidates")" = "200" ] \
       && body_of "$generated_candidates" | jq -e \
            '([.rows[] | (.extra.target_lang // .meta.target_lang)] | unique | sort) ==
               (["es","fr"] | sort)' >/dev/null 2>&1; then
      break
    fi
    sleep 2
  done
  expect_status "generated translation events are queryable after agent completion" \
    "200" "$generated_candidates"
  expect_jq "the generated post is automatically translated to every non-source locale" \
    '([.rows[] | (.extra.target_lang // .meta.target_lang)] | unique | sort) ==
       (["es","fr"] | sort) and
     (.rows | all(.extra.source_lang == "en" and
                  (.extra.target_lang == "es" or .extra.target_lang == "fr") and
                  .extra.mt_model == "gemma4:e2b" and
                  .extra.status == "in_review" and
                  .extra.document_id == $document and
                  .extra.source_revision == $revision and
                  ((.text // "") | length) > 0 and
                  ((.extra.source_text // "") | length) > 0)) and
     ([.rows[] | select(.text != .extra.source_text) | .extra.target_lang] |
        unique | sort) == (["es","fr"] | sort)' \
    "$generated_candidates" --arg document "$GENERATED_DOC_ID" \
    --arg revision "$GENERATED_REVISION"

  GENERATED_CANDIDATE_EVENT_RAW_IDS="$(body_of "$generated_candidates" \
    | jq -c '[.rows[]?.id]')"
  if ! printf '%s' "$GENERATED_CANDIDATE_EVENT_RAW_IDS" | jq -e \
       'type == "array" and length > 0 and
        all(type == "string" and length > 0) and
        length == (unique | length)' \
       >/dev/null 2>&1; then
    fail "raw generated translation rows expose unique nonblank event ids" \
      "$GENERATED_CANDIDATE_EVENT_RAW_IDS"
    GENERATED_CANDIDATE_EVENT_IDS='[]'
  else
    pass "raw generated translation rows expose unique nonblank event ids"
    GENERATED_CANDIDATE_EVENT_IDS="$(printf '%s' \
      "$GENERATED_CANDIDATE_EVENT_RAW_IDS" | jq -c 'unique')"
  fi
  generated_candidate_vector_query="$(jq -cn --arg collection "$VECTOR_COLLECTION" \
    --argjson ids "$GENERATED_CANDIDATE_EVENT_IDS" \
    '{collection:$collection,filter:{parent_id:{"$in":$ids}},
      projection:{_id:0,parent_id:1,embedding_model:1,embedding_dimensions:1,embedding:1},limit:500}')"
  generated_candidate_vectors=""
  generated_candidate_vector_deadline=$((generated_translation_deadline + 30))
  while [ "$SECONDS" -lt "$generated_candidate_vector_deadline" ]; do
    generated_candidate_vectors="$(mongo_query "$generated_candidate_vector_query")"
    if [ "$(status_of "$generated_candidate_vectors")" = "200" ] \
       && body_of "$generated_candidate_vectors" | jq -e \
            --argjson ids "$GENERATED_CANDIDATE_EVENT_IDS" \
            '($ids | length) > 0 and
             ([.rows[].parent_id] | unique | sort) == ($ids | sort) and
             (.rows | all(.embedding_model == "nomic-embed-text" and
                          .embedding_dimensions == 768 and
                          (.embedding | type) == "array" and
                          (.embedding | length) == 768))' >/dev/null 2>&1; then
      GENERATED_ARTIFACTS_SETTLED=1
      break
    fi
    sleep 2
  done
  expect_status "generated candidate vectors are queryable by stable event id" \
    "200" "$generated_candidate_vectors"
  expect_jq "every generated candidate event has an exact nomic 768-dimensional vector" \
    '.ok == true and ($ids | length) > 0 and
     ([.rows[].parent_id] | unique | sort) == ($ids | sort) and
     (.rows | all(.embedding_model == "nomic-embed-text" and
                  .embedding_dimensions == 768 and
                  (.embedding | type) == "array" and
                  (.embedding | length) == 768))' \
    "$generated_candidate_vectors" --argjson ids "$GENERATED_CANDIDATE_EVENT_IDS"

  generated_reviews="$(http GET "$REVIEWS_URL" auth)"
  expect_status "generated translation review inventory is readable" "200" "$generated_reviews"
  expect_jq "both translated generated intents remain unapproved" \
    '[.reviews[] | select(.document == $document and (.locale == "es" or .locale == "fr"))] as $rows |
     ($rows | length) == 2 and
     ([$rows[].locale] | sort) == (["es","fr"] | sort) and
     ($rows | all(.approved == false and .work_state != "approved"))' \
    "$generated_reviews" --arg document "$GENERATED_DOC_ID"

  generated_translation_pairs="$(body_of "$generated_candidates" | jq -c \
    '[.rows[] | {document:.extra.document_id,
                  locale:.extra.target_lang,
                  source_revision:.extra.source_revision,
                  output_revision:.extra.candidate_revision}] |
     unique | sort_by(.locale)')"
  generated_pairs_valid=1
  expect_json "generated events expose one exact output revision per translated relation" \
    'length == 2 and
     ([.[].locale] | sort) == (["es","fr"] | sort) and
     all(.document == $document and .source_revision == $revision and
         (.output_revision | type) == "string" and
         (.output_revision | length) > 0) and
     ([.[].output_revision] | unique | length) == 2' \
    "$generated_translation_pairs" --arg document "$GENERATED_DOC_ID" \
    --arg revision "$GENERATED_REVISION" || generated_pairs_valid=0
  generated_reviews_valid=1
  expect_jq "generated review hydration authenticates the same two stored agent outputs" \
    '[.reviews[] | select(.document == $document and
                          (.locale == "es" or .locale == "fr"))] as $rows |
     ($rows | length) == 2 and
     ($rows | all(.candidate_present == true and .reviewable == true and
                  .hydration_state == "displayable" and
                  .content_source == "agent" and .approved == false and
                  .work_state == "ready")) and
     (($rows | map({document,locale,source_revision:.revision,
                    output_revision:.translation_revision}) |
               sort_by(.locale)) == $pairs)' \
    "$generated_reviews" --arg document "$GENERATED_DOC_ID" \
    --argjson pairs "$generated_translation_pairs" || generated_reviews_valid=0
  if [ "$generated_pairs_valid" -eq 1 ] \
     && [ "$generated_reviews_valid" -eq 1 ] \
     && track_translation_content "generated" "$generated_translation_pairs"; then
    GENERATED_TRANSLATION_CONTENT_SETTLED=1
  fi

  generated_cms="$(http GET "$generated_cms_url" auth)"
  expect_status "the generated post CMS projection is readable" "200" "$generated_cms"
  expect_jq "source and translated generated relations remain unpublished drafts" \
    '.document.id == $document and (.publications | length) == 3 and
     (.publications | all(.desired == "draft" and .observed == null))' \
    "$generated_cms" --arg document "$GENERATED_DOC_ID"

  generated_receipts="$(http GET "$RECEIPTS_URL" auth)"
  case "$(status_of "$generated_receipts")" in
    200)
      expect_jq "post generation and translation emitted no materialization receipt" \
        '[.receipts[]? | select([.. | strings] | index($document))] | length == 0' \
        "$generated_receipts" --arg document "$GENERATED_DOC_ID"
      ;;
    503)
      pass "publication reconciliation remains unconfigured for the generated post ${C_DIM}(503)${C_RESET}"
      ;;
    *)
      fail "generated receipt journal must answer 200 or explicit unconfigured 503" \
        "$(status_of "$generated_receipts"): $(body_of "$generated_receipts" | head -c 300)"
      ;;
  esac
  note "The generated path uses one exact-document generation request; generated resources disable further generation."
else
  step "8. generated-post verification is explicitly optional"
  warn "post-drafter and recursive translation were not invoked in this run"
  note "Set KNOXX_VERIFY_GENERATED_DRAFTS=true and KNOXX_GENERATED_CONTRACTS_DIR to enable the isolated bounded proof."
fi

step "9. run-scoped cleanup is bounded and automatic"
settlement_documents="$(jq -cn \
  --arg source "$DOC_ID" --arg generated "$GENERATED_DOC_ID" \
  '[$source,$generated] | map(select(length > 0))')"
settlement_claim_count=2
[ "$RUN_GENERATED_DRAFTS" -eq 1 ] && settlement_claim_count=4
settlement_claim_query="$(jq -cn \
  --argjson documents "$settlement_documents" \
  '{collection:"knoxx_translation_dispatches",
    filter:{document_wire_id:{"$in":$documents}},
    projection:{_id:0,document_wire_id:1,outcome:1,batch_id:1},limit:20}')"
settlement_claims="$(mongo_query "$settlement_claim_query")"
settlement_claims_valid=1
expect_status "settlement-bound translation claims are queryable" "200" \
  "$settlement_claims" || settlement_claims_valid=0
if [ "$settlement_claims_valid" -eq 1 ]; then
  expect_jq "every expected translation claim completed with an exact agent run" \
    '.ok == true and .total == $count and (.rows | length) == $count and
     (.rows | all(.outcome == "dispatch/completed" and
                  (.batch_id | type) == "string" and
                  (.batch_id | length) > 0))' \
    "$settlement_claims" --argjson count "$settlement_claim_count" \
    || settlement_claims_valid=0
fi
if [ "$settlement_claims_valid" -eq 1 ]; then
  translation_owner_event_ids="$(body_of "$settlement_claims" | jq -c \
    '[.rows[].batch_id | "translation-needed-" + .] | unique')"
  if [ "$RUN_GENERATED_DRAFTS" -eq 1 ]; then
    settlement_owner_event_ids="$(jq -cn \
      --argjson translations "$translation_owner_event_ids" \
      --arg post_drafter "$GENERATION_REQUEST_INDEX_EVENT_ID" \
      '$translations + [$post_drafter] | unique')"
  else
    settlement_owner_event_ids="$translation_owner_event_ids"
  fi
  if wait_for_event_turn_release "$settlement_owner_event_ids"; then
    EVENT_TURNS_SETTLED=1
  fi
fi
settlement_event_ids="$(jq -cn \
  --arg source "$SOURCE_EVENT_ID" \
  --arg indexed "$INDEX_EVENT_ID" \
  --arg generation_source "$GENERATION_REQUEST_SOURCE_EVENT_ID" \
  --arg generation_indexed "$GENERATION_REQUEST_INDEX_EVENT_ID" \
  --arg generated_source "$GENERATED_SOURCE_EVENT_ID" \
  --arg generated_indexed "$GENERATED_INDEX_EVENT_ID" \
  --argjson candidates "$CANDIDATE_EVENT_IDS" \
  --argjson generated_candidates "$GENERATED_CANDIDATE_EVENT_IDS" \
  '([$source,$indexed,$generation_source,$generation_indexed,
     $generated_source,$generated_indexed] + $candidates +
     $generated_candidates) |
   map(select(type == "string" and length > 0)) | unique')"
if wait_for_graph_node_embeddings "$settlement_event_ids"; then
  GRAPH_NODE_EMBEDDINGS_SETTLED=1
fi
expected_translation_content_count=2
[ "$RUN_GENERATED_DRAFTS" -eq 1 ] && expected_translation_content_count=4
if [ "${#TRANSLATION_CONTENT_FILES[@]}" -eq "$expected_translation_content_count" ]; then
  pass "every authenticated translation output has one exact tracked content file"
else
  fail "translation-content tracking must cover every expected locale output" \
    "expected=${expected_translation_content_count}; actual=${#TRANSLATION_CONTENT_FILES[@]}"
fi
if [ "$RETAIN_REVIEW_DEMO" -eq 1 ] && [ "$FAIL_COUNT" -eq 0 ] \
   && [ "$GENERATED_ARTIFACTS_SETTLED" -eq 1 ] \
   && [ "$SOURCE_TRANSLATION_CONTENT_SETTLED" -eq 1 ] \
   && [ "$GENERATED_TRANSLATION_CONTENT_SETTLED" -eq 1 ]; then
  pass "the opt-in review demo is green and will remain visible to the local review surfaces"
  note "source document  ${DOC_ID}"
  note "generated post   ${GENERATED_DOC_ID}"
  note "review inventory ${BASE_URL}${REVIEWS_URL}"
  printf '   exact file cleanup: rm -rf -- %q %q; rm -f -- %q %q %q\n' \
    "$FIXTURE_DIR" "$REVIEW_DEMO_SOURCE_DIR" \
    "$GENERATED_MANIFEST_FILE" "$GENERATED_SOURCE_FILE" \
    "$GENERATED_COMPLETION_FILE"
  print_translation_content_cleanup
else
  if durable_agent_work_settled; then
    pass "the exit trap will fence both anchors and remove the run's exact durable and filesystem scope"
  else
    warn "the exit trap will reconstruct and drain partial agent work before removing it"
  fi
fi
note "The trap requires two identical post-barrier snapshots before deleting durable facts."

printf '\n%s====================================================================%s\n' "$C_BOLD" "$C_RESET"
printf '%s%s passed%s' "$C_GREEN$C_BOLD" "$PASS_COUNT" "$C_RESET"
[ "$WARN_COUNT" -gt 0 ] && printf '%s, %s explicit warning(s)%s' "$C_YELLOW" "$WARN_COUNT" "$C_RESET"
if [ "$FAIL_COUNT" -gt 0 ]; then
  printf '%s, %s FAILED%s\n' "$C_RED$C_BOLD" "$FAIL_COUNT" "$C_RESET"
  for failure in "${FAILURES[@]}"; do
    printf '%s    - %s%s\n' "$C_RED" "$failure" "$C_RESET"
  done
  printf '\n'
  exit 1
fi
if [ "$RETAIN_REVIEW_DEMO" -eq 1 ] && [ "$GENERATED_ARTIFACTS_SETTLED" -eq 1 ] \
   && [ "$SOURCE_TRANSLATION_CONTENT_SETTLED" -eq 1 ] \
   && [ "$GENERATED_TRANSLATION_CONTENT_SETTLED" -eq 1 ]; then
  REVIEW_DEMO_READY=1
fi
printf '\n\n'
exit 0
