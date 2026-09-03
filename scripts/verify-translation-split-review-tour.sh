#!/usr/bin/env bash
#
# Guided browser tour for the resource-owned translation review workflow.
# Seeds its own 18-row/three-split fixture, captures every interactive state,
# and removes its filesystem/Mongo/content facts from EXIT/INT/TERM.
#
# It deliberately seeds candidate bytes through production laws rather than
# calling a model. This verifies review behavior, not provider quality.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PATH="${REPO_ROOT}/node_modules/.bin:${PATH}"
export PATH
FRONTEND_URL="${KNOXX_FRONTEND_URL:-http://localhost:5173}"
CONTRACTS_DIR="${KNOXX_CONTRACTS_DIR:-${REPO_ROOT}/contracts}"
FIXTURE_DIR="${CONTRACTS_DIR}/_verify_translation_split_review_tour"
RUN_ID="$(date -u +%Y%m%d%H%M%S)$$"
VERIFY_TMP_DIR="$(mktemp -d)"
VERIFY_ORG_ID="${VERIFY_ORG_ID:-${KNOXX_VERIFY_ORG_ID:-}}"
KNOXX_PUBLICATION_CONTENT_ROOT="${KNOXX_PUBLICATION_CONTENT_ROOT:-}"
USER_EMAIL="${KNOXX_USER_EMAIL:-pi@open-hax.local}"
ORG_SLUG="${KNOXX_ORG_SLUG:-open-hax}"
SHOT_DIR="${KNOXX_SHOT_DIR:-${REPO_ROOT}/docs/verification/screenshots/translation-split-review-${RUN_ID}}"
SESSION="knoxx-translation-split-review-tour-${RUN_ID}"
CORRECTION_A="Browser-tour correction A retained in label history."
CORRECTION_B="Browser-tour correction B rendered by publication."
LOOPBACK_HTTP=0
API_KEY_SESSION=0

# shellcheck source=lib/translation-split-review-fixture.sh
. "${REPO_ROOT}/scripts/lib/translation-split-review-fixture.sh"
# shellcheck source=lib/credential-transport.sh
. "${REPO_ROOT}/scripts/lib/credential-transport.sh"

STEP_NO=0
FAIL_COUNT=0
FIXTURE_OWNED=0
DURABLE_SEED_ATTEMPTED=0

note() { printf '   %s\n' "$1"; }
step() { printf '\n── %s\n' "$1"; }
pass() { printf '   PASS  %s\n' "$1"; }
warn() { printf '   WARN  %s\n' "$1"; }
fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  printf '   FAIL  %s\n' "$1"
  [ -n "${2:-}" ] && printf '         %s\n' "$2"
  return 0
}
die() { printf '\nABORT %s\n\n' "$1" >&2; exit 2; }
ab() {
  if [ "$LOOPBACK_HTTP" -eq 1 ]; then
    # The browser daemon inherits this on first launch. Loopback credentials
    # must never be handed to an HTTP_PROXY/ALL_PROXY intermediary.
    NO_PROXY='*' no_proxy='*' HTTP_PROXY='' HTTPS_PROXY='' ALL_PROXY='' \
      agent-browser --session "$SESSION" "$@" 2>&1
  else
    agent-browser --session "$SESSION" "$@" 2>&1
  fi
}

shot() {
  STEP_NO=$((STEP_NO + 1))
  local file
  file="${SHOT_DIR}/$(printf '%02d-%s' "$STEP_NO" "$1").png"
  ab screenshot "$file" >/dev/null
  [ -f "$file" ] && note "shot  ${file#$REPO_ROOT/}" \
    || fail "screenshot was not written" "$file"
}

click_button() {
  local label="$1" match="${2:-exact}" encoded predicate result
  encoded="$(jq -Rn --arg value "$label" '$value')"
  [ "$match" = contains ] \
    && predicate='node.textContent.includes(label)' \
    || predicate='node.textContent.trim() === label'
  result="$(ab eval "(() => { const label=$encoded; const button=[...document.querySelectorAll('button')].find((node)=>$predicate); if(!button)return 'missing'; if(button.disabled)return 'disabled'; button.click(); return 'clicked'; })()")"
  printf '%s' "$result" | grep -q clicked && return 0
  fail "button '$label' could not be clicked" "$result"
  return 1
}

fill_review_text() {
  local label="$1" value="$2" encoded_label encoded_value result
  encoded_label="$(jq -Rn --arg value "$label" '$value')"
  encoded_value="$(jq -Rn --arg value "$value" '$value')"
  result="$(ab eval "(() => { const label=$encoded_label,value=$encoded_value; const field=[...document.querySelectorAll('textarea')].find((node)=>node.closest('label')?.textContent.includes(label)); if(!field)return 'missing'; Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,'value').set.call(field,value); field.dispatchEvent(new Event('input',{bubbles:true})); field.dispatchEvent(new Event('change',{bubbles:true})); return field.value; })()")"
  printf '%s' "$result" | grep -qF "$value" && return 0
  fail "textarea '$label' could not be filled" "$result"
  return 1
}

cleanup() {
  local code=$? signalled="${1:-}" cleanup_failed=0 durable_cleanup_succeeded=0
  [ -n "$signalled" ] && code="$signalled"
  ab close >/dev/null 2>&1
  if [ "$DURABLE_SEED_ATTEMPTED" -eq 1 ]; then
    if translation_fixture_helper cleanup >/dev/null 2>&1; then
      note "torn down durable split-review evidence and any materialized route"
      durable_cleanup_succeeded=1
    else
      cleanup_failed=1
    fi
  fi
  [ "$FIXTURE_OWNED" -eq 1 ] && translation_fixture_remove_files
  if [ "$durable_cleanup_succeeded" -eq 1 ]; then
    rm -rf -- "${KNOXX_PUBLICATION_CONTENT_ROOT}/artifacts/${TRANSLATION_FIXTURE_NS}"
  fi
  rm -rf -- "$VERIFY_TMP_DIR"
  [ "$cleanup_failed" -eq 1 ] && [ "$code" -eq 0 ] && code=1
  exit "$code"
}
trap cleanup EXIT
trap 'trap - EXIT; cleanup 130' INT
trap 'trap - EXIT; cleanup 143' TERM

for tool in agent-browser jq clojure unzip curl node; do
  command -v "$tool" >/dev/null 2>&1 || die "missing required tool: $tool"
done
transport_kind="$(knoxx_credential_transport_kind "$FRONTEND_URL" 2>/dev/null)" \
  || die "KNOXX_FRONTEND_URL must use HTTPS or exact loopback HTTP and must not contain userinfo"
[ "$transport_kind" = "loopback-http" ] && LOOPBACK_HTTP=1

printf 'Knoxx resource translation split review — browser tour\n'
note "frontend $FRONTEND_URL"
note "identity $USER_EMAIL / $ORG_SLUG"
note "shots    ${SHOT_DIR#$REPO_ROOT/}"

[ -x "${REPO_ROOT}/backend/node_modules/.bin/nbb" ] \
  || die "backend dependencies are absent; run pnpm install"
[ -n "$VERIFY_ORG_ID" ] || die "VERIFY_ORG_ID must name the browser identity's organization"
[ -n "$KNOXX_PUBLICATION_CONTENT_ROOT" ] \
  || die "KNOXX_PUBLICATION_CONTENT_ROOT must equal the backend's content root"
[ -d "$CONTRACTS_DIR" ] || die "contracts directory not found: $CONTRACTS_DIR"
[ ! -e "$FIXTURE_DIR" ] || die "stale fixture exists: $FIXTURE_DIR"
health_args=(-q -sS -H 'Accept: text/html' -o /dev/null --max-time 5)
[ "$LOOPBACK_HTTP" -eq 1 ] && health_args+=(--noproxy '*')
curl "${health_args[@]}" "$FRONTEND_URL" 2>/dev/null \
  || die "the admitted frontend does not answer"
mkdir -p "$SHOT_DIR"

browser_launches() {
  local output
  output="$(ab open about:blank)"
  [[ "$output" != *"Executable doesn't exist"* && "$output" != *"browserType.launch"* ]]
}
if ! browser_launches; then
  found=""
  for candidate in "$HOME"/.cache/ms-playwright/chromium-*/chrome-linux64/chrome \
                   /usr/bin/google-chrome /usr/bin/chromium /snap/bin/chromium; do
    [ -x "$candidate" ] || continue
    AGENT_BROWSER_EXECUTABLE_PATH="$candidate"; export AGENT_BROWSER_EXECUTABLE_PATH
    if browser_launches; then found="$candidate"; break; fi
  done
  [ -n "$found" ] || die "agent-browser cannot launch Chromium; install its browser"
fi

FIXTURE_OWNED=1
translation_fixture_write

step "1. establish the app session and prove checkout identity"
ab set viewport 1600 1000 >/dev/null
ab set media dark reduced-motion >/dev/null
identity_js="$(jq -rn --arg email "$USER_EMAIL" --arg org "$ORG_SLUG" \
  '"localStorage.setItem(\"knoxx_user_email\", \($email|tojson)); localStorage.setItem(\"knoxx_org_slug\", \($org|tojson)); \"seeded\""')"
ab open "$FRONTEND_URL" >/dev/null
ab eval "$identity_js" >/dev/null
if [ -n "${KNOXX_API_KEY:-}" ]; then
  api_headers="$(jq -cn --arg key "$KNOXX_API_KEY" '{"x-api-key":$key}')"
  ab set headers "$api_headers" >/dev/null
  ab reload >/dev/null
  ab wait 1000 >/dev/null
  auth_status="$(ab eval '(async()=> (await fetch("/api/auth/context")).status)()')"
  printf '%s' "$auth_status" | grep -q 200 \
    || die "API-key browser authentication failed: $auth_status"
  API_KEY_SESSION=1
  pass "the app shell session is authenticated with an API key"
elif [ -n "${KNOXX_DEV_EMAIL:-}" ] && [ -n "${KNOXX_DEV_PASSWORD:-}" ]; then
  login_body="$(jq -cn --arg email "$KNOXX_DEV_EMAIL" --arg password "$KNOXX_DEV_PASSWORD" \
    '{email:$email,password:$password}')"
  login="$(ab eval "(async()=> (await fetch('/api/auth/local/login',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify($login_body)})).status)()")"
  printf '%s' "$login" | grep -q 200 || die "local password login failed: $login"
  pass "the app shell session is authenticated with a local password session"
else
  die "set KNOXX_API_KEY or both KNOXX_DEV_EMAIL and KNOXX_DEV_PASSWORD"
fi

sleep 1
user_email_json="$(jq -Rn --arg value "$USER_EMAIL" '$value')"
org_slug_json="$(jq -Rn --arg value "$ORG_SLUG" '$value')"
probe="$(ab eval "(async()=>{const h={\"x-knoxx-user-email\":$user_email_json,\"x-knoxx-org-slug\":$org_slug_json};const r=await fetch(\"/api/publications/translations/reviews\",{headers:h});return {status:r.status,body:await r.json()};})()")"
printf '%s' "$probe" | grep -qF "$TRANSLATION_FIXTURE_PUBLICATION_ID" \
  || die "the proxy backend cannot see ${FIXTURE_DIR#$REPO_ROOT/}: $(printf '%s' "$probe" | jq -c '{status, error:.body.error, row_count:(.body.rows // .body.items // [] | length)}' 2>/dev/null || printf 'unparseable response: %.300s' "$probe")"
pass "the proxy backend serves this checkout's run-scoped fixture"
project="$(printf '%s' "$probe" | jq -r '.body.project // "__NONE__"' 2>/dev/null)"
if [ -z "$project" ] || [ "$project" = "__NONE__" ]; then
  VERIFY_PROJECT=""
else
  VERIFY_PROJECT="$project"
fi
DURABLE_SEED_ATTEMPTED=1
translation_fixture_helper seed >/dev/null || die "could not seed the production-shaped candidate"

step "2. see eighteen rows and one real three-split candidate"
[ "$API_KEY_SESSION" -eq 1 ] && ab set headers "$api_headers" >/dev/null
ab eval 'history.pushState({}, "", "/translations"); window.dispatchEvent(new PopStateEvent("popstate")); "navigated"' >/dev/null
ab wait 3000 >/dev/null
page_text="$(ab get text body)"
title_prefix_json="$(jq -Rn --arg value "$TRANSLATION_FIXTURE_TITLE_PREFIX" '$value')"
inventory_shape="$(ab eval "(() => { const prefix=$title_prefix_json; const pane=[...document.querySelectorAll('aside')].find((node)=>node.textContent.includes(prefix)); const rows=[...(pane?.querySelectorAll('button') ?? [])].filter((node)=>node.textContent.includes(prefix)); return {rows:rows.length,first:rows.some((node)=>node.textContent.includes(prefix+' 01')),last:rows.some((node)=>node.textContent.includes(prefix+' 18'))}; })()")"
if printf '%s' "$inventory_shape" | jq -e \
     '.rows == 18 and .first == true and .last == true' >/dev/null 2>&1 \
   && printf '%s' "$page_text" | grep -qF "${TRANSLATION_FIXTURE_TITLE_PREFIX} 18"; then
  pass "the browser owns exactly eighteen fixture rows, including row 18"
else
  fail "one receipt collapsed or duplicated the 18-row inventory" "$inventory_shape"
fi
shot "translation-inventory-top"
ab eval "(() => { const prefix=$title_prefix_json; const pane=[...document.querySelectorAll('aside')].find((node)=>node.textContent.includes(prefix)); if(!pane)return 'missing'; pane.scrollTop=pane.scrollHeight; return pane.scrollTop; })()" >/dev/null
ab wait 200 >/dev/null
shot "translation-inventory-bottom-row-18"
click_button "${TRANSLATION_FIXTURE_TITLE_PREFIX} 01" contains
ab wait 1200 >/dev/null
shot "three-persisted-splits"
page_text="$(ab get text body)"
printf '%s' "$page_text" | grep -q '3 segments' \
  && pass "the selected candidate exposes three persisted splits" \
  || fail "the candidate does not report three segments"

step "3. use correction, notes, Submit review, Approve, Reject and Skip"
click_button "seg 0" contains
ab wait 300 >/dev/null
shot "granular-review-card"
page_text="$(ab get text body)"
for control in adequacy fluency terminology risk "Corrected translation" "Editor notes" \
               "Approve split" "Submit review" "Reject split" "Skip"; do
  printf '%s' "$page_text" | grep -qiF "$control" \
    && pass "granular card exposes $control" \
    || fail "granular card is missing $control"
done
fill_review_text "Corrected translation" "$CORRECTION_A"
fill_review_text "Editor notes" "Browser tour reviewer note."
click_button "Submit review"; ab wait 1500 >/dev/null; shot "submitted-in-review"
click_button "seg 0" contains; ab wait 200 >/dev/null
fill_review_text "Corrected translation" "$CORRECTION_B"
click_button "Approve split"; ab wait 1500 >/dev/null
click_button "seg 0" contains; ab wait 200 >/dev/null
page_text="$(ab get text body)"
correction_a_json="$(jq -Rn --arg value "$CORRECTION_A" '$value')"
correction_b_json="$(jq -Rn --arg value "$CORRECTION_B" '$value')"
history_shape="$(ab eval "(() => { const a=$correction_a_json,b=$correction_b_json; const heading=[...document.querySelectorAll('h5')].find((node)=>node.textContent.trim()==='Existing labels'); const card=heading?.parentElement; const entries=card?.querySelector('.space-y-2')?.children.length ?? 0; const text=card?.textContent ?? ''; return {entries,newestFirst:text.indexOf(b)>=0 && text.indexOf(a)>text.indexOf(b)}; })()")"
if printf '%s' "$page_text" | grep -qF "Existing labels" \
   && printf '%s' "$page_text" | grep -qF "2 reviews" \
   && printf '%s' "$page_text" | grep -qF "$CORRECTION_A" \
   && printf '%s' "$page_text" | grep -qF "$CORRECTION_B" \
   && printf '%s' "$history_shape" | jq -e \
        '.entries == 2 and .newestFirst == true' >/dev/null 2>&1; then
  pass "the card renders both immutable A→B labels newest first"
else
  fail "the two-review correction history is absent or out of order" "$history_shape"
fi
shot "immutable-label-history-a-to-b"
click_button "Skip"; ab wait 250 >/dev/null; shot "skip-to-next-split"
page_text="$(ab get text body)"
printf '%s' "$page_text" | grep -qF "Split 1" \
  && pass "Skip advances to the next persisted split without a verdict" \
  || fail "Skip did not advance to split 1"
click_button "Reject split"; ab wait 1500 >/dev/null; shot "granular-rejection"

step "4. drive Needs Edit, Reject All and Approve All"
click_button "Needs Edit"; ab wait 1500 >/dev/null; shot "document-needs-edit"
click_button "Reject All"; ab wait 1500 >/dev/null; shot "document-rejected"
click_button "Approve All"; ab wait 1800 >/dev/null; shot "document-approved"
page_text="$(ab get text body)"
printf '%s' "$page_text" | grep -qF "All splits approved." \
  && pass "Approve All reaches every server-owned split" \
  || fail "Approve All feedback is missing"

step "5. whole approval is visible, and a later rejection revokes it"
click_button "Approve whole output"; ab wait 2500 >/dev/null; shot "whole-output-approved"
page_text="$(ab get text body)"
printf '%s' "$page_text" | grep -qE 'Whole output approved|Translation approved' \
  && pass "the UI completed revision-bound whole-output approval" \
  || fail "whole-output approval did not become visible" "reconciliation may be absent"
publication_id_json="$(jq -Rn --arg value "$TRANSLATION_FIXTURE_PUBLICATION_ID" '$value')"
reconciliation="$(ab eval "(async()=>{const r=await fetch('/api/publications/reconcile',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({publicationId:$publication_id_json})});return {status:r.status,body:await r.json()};})()")"
materialization="$(translation_fixture_helper materialization)" \
  || die "could not inspect the production static-site target"
if printf '%s' "$materialization" | jq -e \
  --arg path "$TRANSLATION_FIXTURE_PUBLICATION_PATH" \
  --arg correction_a "$CORRECTION_A" --arg correction_b "$CORRECTION_B" \
  '.materialized == true and .path == $path and
   (.content | contains($correction_b)) and
   (.content | contains($correction_a) | not)' >/dev/null 2>&1; then
  pass "the browser workflow materialized correction B, not superseded A"
else
  fail "whole approval did not commit the corrected artifact" "$materialization; reconciliation=$reconciliation"
fi
click_button "seg 0" contains; ab wait 200 >/dev/null
click_button "Reject split"; ab wait 1800 >/dev/null; shot "later-rejection-revokes-approval"
page_text="$(ab get text body)"
printf '%s' "$page_text" | grep -qF "Approve whole output" \
  && pass "the later rejection removes stale whole-output approval" \
  || fail "the stale whole-output approval remained current"

step "6. the same browser origin refuses an anonymous inventory read"
[ "$API_KEY_SESSION" -eq 1 ] && ab set headers '{}' >/dev/null
anonymous="$(ab eval '(async()=> (await fetch("/api/publications/translations/reviews",{credentials:"omit",headers:{"x-knoxx-user-email":"","x-knoxx-org-slug":""}})).status)()')"
printf '%s' "$anonymous" | grep -qE '401|403' \
  && pass "anonymous inventory is refused (401/403)" \
  || fail "anonymous inventory was not refused" "$anonymous"
shot "final-revoked-state"

warn "candidate seeding bypasses model execution; this tour evaluates review behavior, not translation quality"
printf '\nscreenshots: %s\n' "${SHOT_DIR#$REPO_ROOT/}"
[ "$FAIL_COUNT" -eq 0 ] && printf 'tour completed with no failures\n\n' && exit 0
printf '%d step(s) FAILED\n\n' "$FAIL_COUNT"
exit 1
