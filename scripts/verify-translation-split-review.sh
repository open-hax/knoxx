#!/usr/bin/env bash
#
# Live verification for resource-owned translation split review.
#
# The run creates exactly eighteen publication work rows, seeds one
# production-shaped three-split candidate through Knoxx's real laws and Mongo
# adapters, drives granular + document review over HTTP, verifies corrected
# text is future translation memory, records whole-output approval, then proves
# a later rejection revokes that approval. Every filesystem and Mongo fact is
# run-scoped and removed from the EXIT/INT/TERM trap.
#
# This deliberately does not call a model. It proves the review and memory seam,
# not provider quality or agent orchestration; the direct seed is called out as
# a WARN in the summary and in docs/verification/translation-split-review.md.
#
# Usage:
#   KNOXX_API_KEY=... \
#   VERIFY_ORG_ID=... \
#   KNOXX_PUBLICATION_CONTENT_ROOT=/absolute/path \
#   scripts/verify-translation-split-review.sh

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_URL="${KNOXX_BASE_URL:-http://localhost:8000}"
API_KEY="${KNOXX_API_KEY:-}"
CONTRACTS_DIR="${KNOXX_CONTRACTS_DIR:-${REPO_ROOT}/contracts}"
FIXTURE_DIR="${CONTRACTS_DIR}/_verify_translation_split_review"
RUN_ID="$(date -u +%Y%m%d%H%M%S)$$"
VERIFY_TMP_DIR="$(mktemp -d)"
VERIFY_ORG_ID="${VERIFY_ORG_ID:-${KNOXX_VERIFY_ORG_ID:-}}"
KNOXX_PUBLICATION_CONTENT_ROOT="${KNOXX_PUBLICATION_CONTENT_ROOT:-}"
LOOPBACK_HTTP=0

# shellcheck source=lib/translation-split-review-fixture.sh
. "${REPO_ROOT}/scripts/lib/translation-split-review-fixture.sh"
# shellcheck source=lib/credential-transport.sh
. "${REPO_ROOT}/scripts/lib/credential-transport.sh"

REVIEWS_URL="/api/publications/translations/reviews"
BULK_REVIEWS_URL="${REVIEWS_URL}/bulk"
APPROVALS_URL="/api/publications/translations/approvals"
RECONCILE_URL="/api/publications/reconcile"
CORRECTION_A="Reviewer correction A retained as immutable history."
CORRECTION_B="Reviewer correction B used by publication and future memory."

PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0
FAILURES=()
FIXTURE_OWNED=0
DURABLE_SEED_ATTEMPTED=0

if [ -t 1 ]; then
  C_RESET=$'\033[0m'; C_DIM=$'\033[2m'; C_BOLD=$'\033[1m'
  C_GREEN=$'\033[32m'; C_RED=$'\033[31m'; C_YELLOW=$'\033[33m'; C_CYAN=$'\033[36m'
else
  C_RESET=""; C_DIM=""; C_BOLD=""; C_GREEN=""; C_RED=""; C_YELLOW=""; C_CYAN=""
fi

step() { printf '\n%s── %s%s\n' "$C_BOLD$C_CYAN" "$1" "$C_RESET"; }
note() { printf '%s   %s%s\n' "$C_DIM" "$1" "$C_RESET"; }
pass() { PASS_COUNT=$((PASS_COUNT + 1)); printf '%s   PASS%s  %s\n' "$C_GREEN" "$C_RESET" "$1"; }
warn() { WARN_COUNT=$((WARN_COUNT + 1)); printf '%s   WARN%s  %s\n' "$C_YELLOW" "$C_RESET" "$1"; }
fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1)); FAILURES+=("$1")
  printf '%s   FAIL%s  %s\n' "$C_RED" "$C_RESET" "$1"
  [ -n "${2:-}" ] && printf '%s         got: %s%s\n' "$C_DIM" "$2" "$C_RESET"
  return 0
}
die() { printf '\n%sABORT%s %s\n\n' "$C_RED$C_BOLD" "$C_RESET" "$1" >&2; exit 2; }

http() {
  local method="$1" path="$2" authorized="$3" body="${4:-}"
  # `-q` must be first: no curlrc may enable redirects or otherwise alter how
  # the API key is transported after the URL admission check below.
  local args=(-q -sS -o /dev/stdout -w $'\n%{http_code}' -X "$method" --max-time 60)
  [ "$LOOPBACK_HTTP" -eq 1 ] && args+=(--noproxy '*')
  [ "$authorized" = "auth" ] && args+=(-H "X-API-Key: ${API_KEY}")
  [ -n "$body" ] && args+=(-H "Content-Type: application/json" -d "$body")
  local raw
  raw="$(curl "${args[@]}" "${BASE_URL}${path}" 2>/dev/null)"
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

fixture_rows_filter='[.reviews[] | select(.publication | startswith($prefix))]'

fixture_row() {
  body_of "$1" | jq -c --arg publication "$TRANSLATION_FIXTURE_PUBLICATION_ID" \
    '.reviews[] | select(.publication == $publication)'
}

cleanup() {
  local code=$? signalled="${1:-}" cleanup_failed=0 durable_cleanup_succeeded=0
  [ -n "$signalled" ] && code="$signalled"

  if [ "$DURABLE_SEED_ATTEMPTED" -eq 1 ]; then
    if translation_fixture_helper cleanup >/dev/null 2>&1; then
      note "torn down this run's split turns, candidates, reviews, receipts, approvals and translation entries"
      durable_cleanup_succeeded=1
    else
      printf '%s   WARN%s  durable fixture cleanup failed for run %s\n' \
        "$C_YELLOW" "$C_RESET" "$RUN_ID" >&2
      cleanup_failed=1
    fi
  fi
  if [ "$FIXTURE_OWNED" -eq 1 ]; then
    translation_fixture_remove_files
    note "torn down ${FIXTURE_DIR#$REPO_ROOT/}"
  fi
  if [ "$durable_cleanup_succeeded" -eq 1 ]; then
    rm -rf -- "${KNOXX_PUBLICATION_CONTENT_ROOT}/artifacts/${TRANSLATION_FIXTURE_NS}"
  fi
  rm -rf -- "$VERIFY_TMP_DIR"

  if [ "$cleanup_failed" -eq 1 ] && [ "$code" -eq 0 ]; then code=1; fi
  exit "$code"
}
trap cleanup EXIT
trap 'trap - EXIT; cleanup 130' INT
trap 'trap - EXIT; cleanup 143' TERM

for tool in curl jq clojure unzip node; do
  command -v "$tool" >/dev/null 2>&1 || die "missing required tool: ${tool}"
done
transport_kind="$(knoxx_credential_transport_kind "$BASE_URL" 2>/dev/null)" \
  || die "KNOXX_BASE_URL must use HTTPS or exact loopback HTTP and must not contain userinfo"
[ "$transport_kind" = "loopback-http" ] && LOOPBACK_HTTP=1

printf '%s\n' "${C_BOLD}Knoxx resource translation split review — live verification${C_RESET}"
note "base url      ${BASE_URL}"
note "run id        ${RUN_ID}"
note "contracts dir ${CONTRACTS_DIR}"

[ -x "${REPO_ROOT}/backend/node_modules/.bin/nbb" ] \
  || die "backend dependencies are absent; run pnpm install before verification"
[ -n "$API_KEY" ] \
  || die "KNOXX_API_KEY is required and must match the running backend"
[ -n "$VERIFY_ORG_ID" ] \
  || die "VERIFY_ORG_ID (or KNOXX_VERIFY_ORG_ID) must name the API key's organization"
[ -n "$KNOXX_PUBLICATION_CONTENT_ROOT" ] \
  || die "KNOXX_PUBLICATION_CONTENT_ROOT must equal the running backend's content root"
[ -d "$CONTRACTS_DIR" ] || die "contracts directory not found: ${CONTRACTS_DIR}"
[ ! -e "$FIXTURE_DIR" ] \
  || die "fixture directory already exists: ${FIXTURE_DIR}; remove the stale verifier fixture first"
health_args=(-q -sS -o /dev/null --max-time 5)
[ "$LOOPBACK_HTTP" -eq 1 ] && health_args+=(--noproxy '*')
curl "${health_args[@]}" "${BASE_URL}/health" 2>/dev/null \
  || die "the admitted Knoxx backend does not answer its health route"

step "0. the live backend serves this checkout and all 18 resource rows"
FIXTURE_OWNED=1
translation_fixture_write
sleep 1

topology="$(http GET "/api/publications/documents" auth)"
expect_status "resource topology is readable" "200" "$topology"
if body_of "$topology" | jq -e --arg id "$TRANSLATION_FIXTURE_DOC_ID" \
  '[.documents[].document.id] | index($id) != null' >/dev/null 2>&1; then
  pass "the backend can see the run-scoped resource fixture"
else
  note "The PM2 process probably serves another checkout. Check its cwd and CONTRACTS_DIR."
  die "the running backend cannot see ${FIXTURE_DIR#$REPO_ROOT/}"
fi

inventory="$(http GET "$REVIEWS_URL" auth)"
expect_status "resource translation inventory is readable" "200" "$inventory"
expect_jq "exactly eighteen desired fixture relations are present before any receipt" \
  "(${fixture_rows_filter} | length) == 18" "$inventory" \
  --arg prefix "${TRANSLATION_FIXTURE_NS}/"
expect_jq "all eighteen rows stay visible while candidate evidence is absent" \
  "(${fixture_rows_filter} | all(.candidate_present == false and .work_state == \"missing\"))" \
  "$inventory" --arg prefix "${TRANSLATION_FIXTURE_NS}/"

project="$(body_of "$inventory" | jq -r '.project // "__NONE__"')"
if [ "$project" = "__NONE__" ]; then
  VERIFY_PROJECT=""
  note "review scope project <none> (taken from the live response)"
else
  VERIFY_PROJECT="$project"
  note "review scope project ${VERIFY_PROJECT} (taken from the live response)"
fi

step "1. every review mutation refuses an unauthenticated caller"
placeholder='{"candidate_set_id":"not-present","split_id":"not-present","status":"approved"}'
expect_status "GET split inventory is private" "401 403" \
  "$(http GET "$REVIEWS_URL" anon)"
expect_status "POST one split review is private" "401 403" \
  "$(http POST "$REVIEWS_URL" anon "$placeholder")"
expect_status "POST document review is private" "401 403" \
  "$(http POST "$BULK_REVIEWS_URL" anon '{"candidate_set_id":"not-present","status":"approved"}')"
expect_status "POST whole-output approval is private" "401 403" \
  "$(http POST "$APPROVALS_URL" anon '{"document":"not/present","garden":"not/present","locale":"es","revision":"sha256-absent","translation_revision":"sha256-absent"}')"
expect_status "POST publication reconciliation is private" "401 403" \
  "$(http POST "$RECONCILE_URL" anon '{"publicationId":"not/present"}')"

step "2. closed request contracts reject forgery, omission and impossible candidates"
smuggled="$(printf '%s' "$placeholder" | jq -c '. + {principal: "forged", org_id: "another-org"}')"
expect_status "caller-supplied principal and tenant are refused" "400" \
  "$(http POST "$REVIEWS_URL" auth "$smuggled")"
expect_status "a split review missing split_id is refused" "400" \
  "$(http POST "$REVIEWS_URL" auth '{"candidate_set_id":"not-present","status":"approved"}')"
expect_status "bulk review refuses a client-supplied correction" "400" \
  "$(http POST "$BULK_REVIEWS_URL" auth '{"candidate_set_id":"not-present","status":"approved","corrected_text":"smuggled"}')"
expect_status "the closed risk vocabulary refuses the historical unsafe spelling" "400" \
  "$(http POST "$REVIEWS_URL" auth \
    '{"candidate_set_id":"not-present","split_id":"not-present","status":"rejected","adequacy":"adequate","fluency":"adequate","terminology":"minor_errors","risk":"unsafe"}')"
expect_status "a syntactically valid but absent candidate set is a conflict" "409" \
  "$(http POST "$REVIEWS_URL" auth "$placeholder")"

step "3. one production-shaped candidate joins without changing cardinality"
DURABLE_SEED_ATTEMPTED=1
seed_result="$(translation_fixture_helper seed)" \
  || die "the production-law fixture helper could not seed Mongo/content evidence"
candidate_set_id="$(printf '%s' "$seed_result" | jq -r '.candidate_set_id // empty')"
[ -n "$candidate_set_id" ] || die "fixture helper returned no candidate_set_id: ${seed_result}"
if [ "$(printf '%s' "$seed_result" | jq -r '.split_count')" = "3" ]; then
  pass "production split laws admitted exactly three canonical source parts"
else
  fail "fixture helper did not seed three canonical splits" "$seed_result"
fi

inventory="$(http GET "$REVIEWS_URL" auth)"
expect_status "the hydrated inventory is readable" "200" "$inventory"
expect_jq "one receipt still projects onto exactly eighteen resource-owned rows" \
  "(${fixture_rows_filter} | length) == 18" "$inventory" \
  --arg prefix "${TRANSLATION_FIXTURE_NS}/"
expect_jq "seventeen uncompleted resource rows remain visible and actionable" \
  "(${fixture_rows_filter} | map(select(.work_state == \"missing\")) | length) == 17" \
  "$inventory" --arg prefix "${TRANSLATION_FIXTURE_NS}/"
expect_jq "the candidate row is exact-byte hydrated from the agent content store" \
  '.reviews[] | select(.publication == $publication) |
   .candidate_present == true and .reviewable == true and
   .contract_candidate == true and .content_source == "agent"' \
  "$inventory" --arg publication "$TRANSLATION_FIXTURE_PUBLICATION_ID"
expect_jq "the candidate exposes its real persisted three-split set in order" \
  '.reviews[] | select(.publication == $publication) |
   .split_review.candidate_set_id == $candidate and
   (.split_review.splits | length) == 3 and
   [.split_review.splits[].split_index] == [0,1,2]' \
  "$inventory" --arg publication "$TRANSLATION_FIXTURE_PUBLICATION_ID" \
  --arg candidate "$candidate_set_id"

row="$(fixture_row "$inventory")"
first_split_id="$(printf '%s' "$row" | jq -r '.split_review.splits[0].split_id')"
second_split_id="$(printf '%s' "$row" | jq -r '.split_review.splits[1].split_id')"

step "4. granular review persists scores, notes, correction and retry identity"
in_review_payload="$(jq -cn \
  --arg set "$candidate_set_id" --arg split "$first_split_id" --arg correction "$CORRECTION_A" \
  '{candidate_set_id:$set, split_id:$split, status:"in-review",
    adequacy:"excellent", fluency:"good", terminology:"correct", risk:"safe",
    corrected_text:$correction, editor_notes:"Keep the product term Knoxx."}')"
response="$(http POST "$REVIEWS_URL" auth "$in_review_payload")"
expect_status "Submit review appends the first immutable judgment" "201" "$response"
expect_jq "the server reports the split is still in review" \
  '.review_status != "ready" and .review.status == "in-review"' "$response"

retry="$(http POST "$REVIEWS_URL" auth "$in_review_payload")"
expect_status "an exact double-submit is idempotent" "200" "$retry"
expect_jq "the replay resolves to the existing review fact" '.status == "existing"' "$retry"

inventory="$(http GET "$REVIEWS_URL" auth)"
expect_jq "the refreshed card recalls every reviewer-controlled field" \
  '.reviews[] | select(.publication == $publication) |
   .split_review.splits[0] |
   .review_status == "in-review" and .adequacy == "excellent" and
   .fluency == "good" and .terminology == "correct" and .risk == "safe" and
   .corrected_text == $correction and
   .editor_notes == "Keep the product term Knoxx."' \
  "$inventory" --arg publication "$TRANSLATION_FIXTURE_PUBLICATION_ID" \
  --arg correction "$CORRECTION_A"

approve_first="$(printf '%s' "$in_review_payload" | jq -c \
  --arg correction "$CORRECTION_B" \
  '.status = "approved" |
   .corrected_text = $correction |
   .editor_notes = "Correction B supersedes A without erasing its label."')"
response="$(http POST "$REVIEWS_URL" auth "$approve_first")"
expect_status "Approve split appends correction B after correction A" "201" "$response"

inventory="$(http GET "$REVIEWS_URL" auth)"
expect_jq "the refreshed card exposes newest-first immutable A→B label history" \
  '.reviews[] | select(.publication == $publication) |
   .split_review.splits[0] |
   .review_status == "approved" and .corrected_text == $correction_b and
   .label_count == 2 and (.labels | length) == 2 and
   .labels[0].review_status == "approved" and
   .labels[0].corrected_text == $correction_b and
   .labels[1].review_status == "in-review" and
   .labels[1].corrected_text == $correction_a and
   ([.labels[].labeler_email] | all(type == "string" and length > 0))' \
  "$inventory" --arg publication "$TRANSLATION_FIXTURE_PUBLICATION_ID" \
  --arg correction_a "$CORRECTION_A" --arg correction_b "$CORRECTION_B"

memory="$(translation_fixture_helper memory)" \
  || die "the production split store could not project future translation memory"
if printf '%s' "$memory" | jq -e \
  --arg candidate "$candidate_set_id" --arg correction "$CORRECTION_B" \
  '.current_candidate_set_ids == [$candidate] and
   (.examples | any(.target_text == $correction))' >/dev/null 2>&1; then
  pass "the attempt-visible current set projects correction B as future memory"
else
  fail "approved correction was not returned by the production memory projection" "$memory"
fi

step "5. incomplete and rejected split sets cannot authorize whole output"
inventory="$(http GET "$REVIEWS_URL" auth)"
row="$(fixture_row "$inventory")"
approval_body="$(printf '%s' "$row" | jq -c \
  '{document,garden,locale,revision,translation_revision}')"
expect_status "whole-output approval is blocked while two splits are pending" "409" \
  "$(http POST "$APPROVALS_URL" auth "$approval_body")"

reject_second="$(jq -cn --arg set "$candidate_set_id" --arg split "$second_split_id" \
  '{candidate_set_id:$set,split_id:$split,status:"rejected",
    adequacy:"adequate",fluency:"adequate",terminology:"minor_errors",risk:"sensitive",
    editor_notes:"Deliberate rejection from the live verifier."}')"
expect_status "Reject split appends an explicit negative judgment" "201" \
  "$(http POST "$REVIEWS_URL" auth "$reject_second")"
inventory="$(http GET "$REVIEWS_URL" auth)"
expect_jq "the rejection is visible beside the still-approved corrected split" \
  '.reviews[] | select(.publication == $publication) |
   [.split_review.splits[].review_status] |
   index("approved") != null and index("rejected") != null' \
  "$inventory" --arg publication "$TRANSLATION_FIXTURE_PUBLICATION_ID"

step "6. document fast paths operate on the server-owned persisted set"
bulk_base="$(jq -cn --arg set "$candidate_set_id" \
  '{candidate_set_id:$set,adequacy:"good",fluency:"good",
    terminology:"correct",risk:"safe",editor_notes:"Document fast path."}')"
needs_edit="$(printf '%s' "$bulk_base" | jq -c '.status = "in-review"')"
reject_all="$(printf '%s' "$bulk_base" | jq -c '.status = "rejected"')"
approve_all="$(printf '%s' "$bulk_base" | jq -c '.status = "approved"')"

expect_status "Needs Edit marks every persisted split in review" "201" \
  "$(http POST "$BULK_REVIEWS_URL" auth "$needs_edit")"
inventory="$(http GET "$REVIEWS_URL" auth)"
expect_jq "Needs Edit reached all three splits without client-enumerated ids" \
  '.reviews[] | select(.publication == $publication) |
   [.split_review.splits[].review_status] | all(. == "in-review")' \
  "$inventory" --arg publication "$TRANSLATION_FIXTURE_PUBLICATION_ID"

expect_status "Reject All marks every persisted split rejected" "201" \
  "$(http POST "$BULK_REVIEWS_URL" auth "$reject_all")"
inventory="$(http GET "$REVIEWS_URL" auth)"
expect_jq "Reject All reached the same exact three-split set" \
  '.reviews[] | select(.publication == $publication) |
   [.split_review.splits[].review_status] | all(. == "rejected")' \
  "$inventory" --arg publication "$TRANSLATION_FIXTURE_PUBLICATION_ID"

expect_status "Approve All marks every persisted split approved" "201" \
  "$(http POST "$BULK_REVIEWS_URL" auth "$approve_all")"
inventory="$(http GET "$REVIEWS_URL" auth)"
expect_jq "Approve All makes the exact persisted set fully approved" \
  '.reviews[] | select(.publication == $publication) |
   .split_review.status == "fully-approved" and
   ([.split_review.splits[].review_status] | all(. == "approved"))' \
  "$inventory" --arg publication "$TRANSLATION_FIXTURE_PUBLICATION_ID"
expect_jq "a document fast path does not erase the accepted split correction" \
  '.reviews[] | select(.publication == $publication) |
   .split_review.splits[0].corrected_text == $correction' \
  "$inventory" --arg publication "$TRANSLATION_FIXTURE_PUBLICATION_ID" \
  --arg correction "$CORRECTION_B"

memory="$(translation_fixture_helper memory)" \
  || die "memory projection failed after document review"
if printf '%s' "$memory" | jq -e --arg correction "$CORRECTION_B" \
  '.examples | any(.target_text == $correction)' >/dev/null 2>&1; then
  pass "Approve All preserved the correction used by future translation memory"
else
  fail "document review erased the corrected future-memory target" "$memory"
fi

bulk_retry="$(http POST "$BULK_REVIEWS_URL" auth "$approve_all")"
expect_status "an exact Approve All replay is idempotent" "200" "$bulk_retry"
expect_jq "the bulk replay reports existing review facts" '.status == "existing"' "$bulk_retry"

step "7. whole approval succeeds only for the current fully-reviewed output"
inventory="$(http GET "$REVIEWS_URL" auth)"
row="$(fixture_row "$inventory")"
approval_body="$(printf '%s' "$row" | jq -c \
  '{document,garden,locale,revision,translation_revision}')"
approval="$(http POST "$APPROVALS_URL" auth "$approval_body")"
expect_status "Approve whole output records revision-bound approval" "201" "$approval"
expect_jq "whole approval is attributed and tied to the reviewed output" \
  '.approved == true and .status == "recorded"' "$approval"
expect_status "an exact whole-approval replay is idempotent" "200" \
  "$(http POST "$APPROVALS_URL" auth "$approval_body")"

inventory="$(http GET "$REVIEWS_URL" auth)"
expect_jq "the inventory now reports the exact resource work approved" \
  '.reviews[] | select(.publication == $publication) |
   .approved == true and .work_state == "approved"' \
  "$inventory" --arg publication "$TRANSLATION_FIXTURE_PUBLICATION_ID"

reconcile_body="$(jq -cn --arg publication "$TRANSLATION_FIXTURE_PUBLICATION_ID" \
  '{publicationId:$publication}')"
reconciliation="$(http POST "$RECONCILE_URL" auth "$reconcile_body")"
expect_status "the approved resource reconciles through the production target" "200" \
  "$reconciliation"
expect_jq "reconciliation reports a real materialization receipt" \
  '[.. | strings] | index("publication/materialized") != null' "$reconciliation"

materialization="$(translation_fixture_helper materialization)" \
  || die "the static-site target could not be inspected after reconciliation"
if printf '%s' "$materialization" | jq -e \
  --arg path "$TRANSLATION_FIXTURE_PUBLICATION_PATH" \
  --arg correction_a "$CORRECTION_A" --arg correction_b "$CORRECTION_B" \
  '.materialized == true and .path == $path and
   (.content | contains($correction_b)) and
   (.content | contains($correction_a) | not)' >/dev/null 2>&1; then
  pass "the committed static artifact contains correction B, not superseded A"
else
  fail "reconciliation did not materialize the corrected reviewed bytes" "$materialization"
fi

revoke_payload="$(jq -cn --arg set "$candidate_set_id" --arg split "$first_split_id" \
  '{candidate_set_id:$set,split_id:$split,status:"rejected",
    adequacy:"adequate",fluency:"adequate",terminology:"major_errors",risk:"policy_violation",
    editor_notes:"A later review must revoke stale whole approval."}')"
expect_status "a later split rejection is recorded" "201" \
  "$(http POST "$REVIEWS_URL" auth "$revoke_payload")"
inventory="$(http GET "$REVIEWS_URL" auth)"
expect_jq "the later rejection revokes the stale whole-output approval" \
  '.reviews[] | select(.publication == $publication) |
   .approved == false and
   (.split_review.splits | any(.review_status == "rejected"))' \
  "$inventory" --arg publication "$TRANSLATION_FIXTURE_PUBLICATION_ID"

row="$(fixture_row "$inventory")"
revoked_body="$(printf '%s' "$row" | jq -c \
  '{document,garden,locale,revision,translation_revision}')"
expect_status "the rejected current projection cannot be whole-approved" "409" \
  "$(http POST "$APPROVALS_URL" auth "$revoked_body")"

step "8. known live-service boundary"
warn "the verifier seeds a production-shaped candidate directly; it does not prove model/provider quality"
note "Run the dispatch verifier separately to watch agent dispatch and worker availability."
note "This run does prove the 18-row inventory, canonical persisted splits, granular"
note "review with label history, reject/edit/approve fast paths, corrected future memory,"
note "whole approval, corrected static materialization and later revocation through Knoxx."

printf '\n%s%s%s\n' "$C_BOLD" "$(printf '═%.0s' $(seq 1 72))" "$C_RESET"
printf '%s%s passed%s' "$C_GREEN" "$PASS_COUNT" "$C_RESET"
[ "$WARN_COUNT" -gt 0 ] && printf '%s, %s known boundary%s' "$C_YELLOW" "$WARN_COUNT" "$C_RESET"
if [ "$FAIL_COUNT" -gt 0 ]; then
  printf '%s, %s FAILED%s\n' "$C_RED" "$FAIL_COUNT" "$C_RESET"
  for failure in "${FAILURES[@]}"; do
    printf '%s    - %s%s\n' "$C_RED" "$failure" "$C_RESET"
  done
  printf '\n'
  exit 1
fi
printf '\n\n'
exit 0
