#!/usr/bin/env bash
#
# Guided browser tour of the contract-owned publication surface.
#
# Drives a real browser against a running Knoxx frontend and captures a
# screenshot at every step, so the epic can be reviewed by looking at it rather
# than by reading assertions. Uses the same throwaway fixture as
# scripts/verify-publication-epic.sh and tears it down the same way.
#
# What this tour proves, in order:
#   1. the CMS page loads with no OpenPlanner REST service running
#   2. the garden list on that page comes from the resource graph
#   3. the page's own fetch returns desired and observed as separate fields
#   4. an authorized PATCH flips desired, and a reload shows the new state
#   5. an unauthenticated caller is refused from the same origin
#
# What it deliberately does NOT do: click the CMS publish toggle. That control
# is still wired through the legacy OpenPlanner document flow
# (`/api/openplanner/v1/cms/publish/...`) and only calls setPublicationState
# afterwards, so with no OpenPlanner running the click cannot complete and would
# prove nothing about the resource-backed path. See
# docs/verification/publication-epic.md.
#
# Usage:
#   scripts/verify-publication-tour.sh
#   KNOXX_FRONTEND_URL=http://localhost:5173 scripts/verify-publication-tour.sh

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

FRONTEND_URL="${KNOXX_FRONTEND_URL:-http://localhost:5173}"
USER_EMAIL="${KNOXX_USER_EMAIL:-pi@open-hax.local}"
ORG_SLUG="${KNOXX_ORG_SLUG:-open-hax}"
CONTRACTS_DIR="${KNOXX_CONTRACTS_DIR:-${REPO_ROOT}/contracts}"
FIXTURE_DIR="${CONTRACTS_DIR}/_verify"
SHOT_DIR="${KNOXX_SHOT_DIR:-${REPO_ROOT}/docs/verification/screenshots}"
SESSION="knoxx-publication-tour"

# shellcheck source=lib/publication-fixture.sh
. "${REPO_ROOT}/scripts/lib/publication-fixture.sh"

STEP_NO=0
FAIL_COUNT=0

if [ -t 1 ]; then
  C_RESET=$'\033[0m'; C_DIM=$'\033[2m'; C_BOLD=$'\033[1m'
  C_GREEN=$'\033[32m'; C_RED=$'\033[31m'; C_CYAN=$'\033[36m'
else
  C_RESET=""; C_DIM=""; C_BOLD=""; C_GREEN=""; C_RED=""; C_CYAN=""
fi

note() { printf '%s   %s%s\n' "$C_DIM" "$1" "$C_RESET"; }
pass() { printf '%s   PASS%s  %s\n' "$C_GREEN" "$C_RESET" "$1"; }
fail() { FAIL_COUNT=$((FAIL_COUNT + 1)); printf '%s   FAIL%s  %s\n' "$C_RED" "$C_RESET" "$1"; [ -n "${2:-}" ] && printf '%s         %s%s\n' "$C_DIM" "$2" "$C_RESET"; return 0; }
die()  { printf '\n%sABORT%s %s\n\n' "$C_RED$C_BOLD" "$C_RESET" "$1" >&2; exit 2; }

ab() { agent-browser --session "$SESSION" "$@" 2>&1; }

# Capture a screenshot and announce where it landed.
shot() {
  STEP_NO=$((STEP_NO + 1))
  local name; name="$(printf '%02d-%s' "$STEP_NO" "$1")"
  local file="${SHOT_DIR}/${name}.png"
  ab screenshot "$file" >/dev/null
  if [ -f "$file" ]; then
    printf '%s   shot%s  %s\n' "$C_CYAN" "$C_RESET" "${file#$REPO_ROOT/}"
  else
    fail "screenshot ${name} was not written"
  fi
}

step() { printf '\n%s── %s%s\n' "$C_BOLD$C_CYAN" "$1" "$C_RESET"; }

# Only ever removes a fixture this run created — see the note in
# verify-publication-epic.sh. An early exit (no agent-browser, no frontend) must
# not delete a directory another run owns.
FIXTURE_OWNED=0

cleanup() {
  local code=$?
  ab close >/dev/null 2>&1
  if [ "$FIXTURE_OWNED" -eq 1 ] && [ -d "$FIXTURE_DIR" ]; then
    fixture_remove
    note "torn down ${FIXTURE_DIR#$REPO_ROOT/}"
  fi
  exit $code
}
trap cleanup EXIT INT TERM

# ── Preflight ──────────────────────────────────────────────────────────────

printf '%s\n' "${C_BOLD}Knoxx contract-owned publication — guided browser tour${C_RESET}"
note "frontend   ${FRONTEND_URL}"
note "identity   ${USER_EMAIL} / ${ORG_SLUG}"
note "shots      ${SHOT_DIR#$REPO_ROOT/}"

command -v agent-browser >/dev/null 2>&1 || die "agent-browser is not installed or not on PATH"
command -v jq >/dev/null 2>&1 || die "missing required tool: jq"

# Resolve a browser before anything else, so a launch problem surfaces here
# rather than as a dozen unrelated-looking step failures further down.
#
# agent-browser pins a playwright-core build number and refuses any other, so
# `agent-browser install` can report success while leaving a version it will not
# use (1234 installed, 1208 expected). Rather than make that the reader's
# problem, fall back to any chromium on the machine via the documented
# AGENT_BROWSER_EXECUTABLE_PATH escape hatch.
# Tested as a string, not as `... | grep -q`: agent-browser exits non-zero on a
# launch failure, and under `set -o pipefail` that non-zero becomes the whole
# pipeline's status — which would inverted-negate into "the browser is fine".
browser_launches() {
  local out
  out="$(ab open "about:blank")"
  [[ "$out" != *"Executable doesn't exist"* && "$out" != *"browserType.launch"* ]]
}

if ! browser_launches; then
  found=""
  for candidate in \
    "$HOME"/.cache/ms-playwright/chromium-*/chrome-linux64/chrome \
    /usr/bin/google-chrome /usr/bin/chromium /snap/bin/chromium
  do
    [ -x "$candidate" ] || continue
    AGENT_BROWSER_EXECUTABLE_PATH="$candidate"
    export AGENT_BROWSER_EXECUTABLE_PATH
    if browser_launches; then found="$candidate"; break; fi
  done
  if [ -z "$found" ]; then
    unset AGENT_BROWSER_EXECUTABLE_PATH
    die "agent-browser cannot launch a browser, and no fallback chromium was found.
       Try: agent-browser install --with-deps
       Or:  export AGENT_BROWSER_EXECUTABLE_PATH=/path/to/chrome"
  fi
  note "using browser ${found}"
fi
[ -d "$CONTRACTS_DIR" ] || die "contracts directory not found: ${CONTRACTS_DIR}"
[ -e "$FIXTURE_DIR" ] && die "fixture directory already exists: ${FIXTURE_DIR} — remove it and retry."

curl -s -o /dev/null --max-time 5 "$FRONTEND_URL" 2>/dev/null \
  || die "no frontend answering at ${FRONTEND_URL}. Run: pnpm -C frontend dev"

mkdir -p "$SHOT_DIR"
FIXTURE_OWNED=1
fixture_write_valid
note "seeded ${FIXTURE_DIR#$REPO_ROOT/}"

# ── 1. Identity ────────────────────────────────────────────────────────────
#
# There are TWO gates and they do not accept the same credential.
#
#   * The API client (lib/api/core.ts) reads localStorage and sends
#     x-knoxx-user-email, which resolve-auth-context prefers over everything
#     else. Seeding localStorage satisfies this.
#   * The app SHELL guards its routes on a session cookie. Seeding localStorage
#     does NOT satisfy this, and the SPA renders its login screen instead of
#     the CMS.
#
# So localStorage alone gets the API assertions below green while the page a
# human would look at shows a 401 login form. Set KNOXX_DEV_EMAIL and
# KNOXX_DEV_PASSWORD to establish a real session and see the actual CMS.

step "1. open the app and establish an identity"

ab set viewport 1600 1000 >/dev/null
ab open "$FRONTEND_URL" >/dev/null
ab eval "localStorage.setItem('knoxx_user_email', '${USER_EMAIL}'); localStorage.setItem('knoxx_org_slug', '${ORG_SLUG}'); 'seeded'" >/dev/null

if [ -n "${KNOXX_DEV_EMAIL:-}" ] && [ -n "${KNOXX_DEV_PASSWORD:-}" ]; then
  login="$(ab eval "(async () => (await fetch('/api/auth/local/login', {method:'POST', headers:{'content-type':'application/json'}, body: JSON.stringify({email:'${KNOXX_DEV_EMAIL}', password:'${KNOXX_DEV_PASSWORD}'})})).status)()")"
  if printf '%s' "$login" | grep -q "200"; then
    pass "signed in as ${KNOXX_DEV_EMAIL} — the shell will render the real CMS"
  else
    fail "local password login failed" "$(printf '%s' "$login" | head -c 120)"
  fi
else
  note "KNOXX_DEV_EMAIL / KNOXX_DEV_PASSWORD not set — API checks will still run,"
  note "but the app shell will show its login screen rather than the CMS."
fi

ab reload >/dev/null
ab wait 1500 >/dev/null
shot "app-loaded"

# ── 2. The CMS page with no OpenPlanner ────────────────────────────────────

step "2. the CMS page renders with no OpenPlanner REST service running"

ab open "${FRONTEND_URL}/cms" >/dev/null
ab wait 3000 >/dev/null
shot "cms-page"

page_text="$(ab get text body)"
if printf '%s' "$page_text" | grep -qi "$FIXTURE_GARDEN_TITLE"; then
  pass "the seeded garden \"${FIXTURE_GARDEN_TITLE}\" is on the page — it came from the resource graph"
elif printf '%s' "$page_text" | grep -qiE "Sign in with password|Knowledge operations platform|Redeem invite"; then
  # Name the actual wall rather than reporting a vague absence. The API checks
  # below will still pass, which is exactly the confusing part worth spelling
  # out: the data is being served correctly, the shell just will not show it.
  fail "the app shell rendered its LOGIN screen, not the CMS" \
       "seeded localStorage satisfies the API client but not the route guard. Set KNOXX_DEV_EMAIL and KNOXX_DEV_PASSWORD to sign in."
else
  fail "the seeded garden is not visible on the CMS page" \
       "the topology fetch may have failed; check: agent-browser --session ${SESSION} console"
fi

errors="$(ab errors)"
if printf '%s' "$errors" | grep -qi "openplanner"; then
  fail "the page logged an OpenPlanner error while loading the publication surface" \
       "$(printf '%s' "$errors" | grep -i openplanner | head -2)"
else
  pass "no OpenPlanner error was raised while loading the publication surface"
fi

# ── 3. Desired vs observed ─────────────────────────────────────────────────
#
# Fetched from inside the page so it carries the page's own origin and auth
# headers — the same request the CMS itself makes, not a curl that bypasses it.

step "3. the wire keeps desired intent and observed evidence apart"

topology="$(ab eval "(async () => { const h = {'x-knoxx-user-email': localStorage.getItem('knoxx_user_email') || '', 'x-knoxx-org-slug': localStorage.getItem('knoxx_org_slug') || ''}; return (await fetch('/api/cms/publications/documents', {headers: h})).text(); })()")"
# agent-browser prints an eval result as JSON, so a returned string arrives
# quoted and escaped. Unwrap it once before parsing the payload itself.
payload="$(printf '%s' "$topology" | jq -r . 2>/dev/null)"

desired="$(printf '%s' "$payload" | jq -r "[.documents[].publications[] | select(.id == \"${FIXTURE_PUB_ID}\")] | .[0].desired" 2>/dev/null)"
observed="$(printf '%s' "$payload" | jq -r "[.documents[].publications[] | select(.id == \"${FIXTURE_PUB_ID}\")] | .[0].observed" 2>/dev/null)"

if [ "$desired" = "withheld" ]; then
  pass "desired = withheld, straight from the EDN resource"
else
  fail "desired should be withheld, got: ${desired:-<none>}" "$(printf '%s' "$payload" | head -c 300)"
fi

if [ "$observed" = "null" ]; then
  pass "observed = null — nothing has been materialized, and the wire says so"
else
  fail "observed should be null, got: ${observed}"
fi

if printf '%s' "$payload" | jq -e "[.documents[].document.id] | any(startswith(\"${FIXTURE_NS}/\"))" >/dev/null 2>&1; then
  pass "qualified identity survived JSON — namespace intact, no leading colon"
else
  fail "identity lost its namespace crossing the wire"
fi

# ── 4. The write ───────────────────────────────────────────────────────────

step "4. publishing through the CMS surface rewrites the resource"

patch="$(ab eval "(async () => { const h = {'x-knoxx-user-email': localStorage.getItem('knoxx_user_email') || '', 'x-knoxx-org-slug': localStorage.getItem('knoxx_org_slug') || ''}; h['content-type'] = 'application/json'; return (await fetch('/api/cms/publications/intents/${FIXTURE_NS}%2Fprobe-es', {method:'PATCH', headers: h, body: JSON.stringify({state:'published'})})).status; })()")"

if printf '%s' "$patch" | grep -q "200"; then
  pass "PATCH returned 200 from the page's own origin"
else
  fail "PATCH did not return 200" "$(printf '%s' "$patch" | head -c 200)"
fi

sleep 1
on_disk="$(fixture_state_on_disk)"
if [ "$on_disk" = "published" ]; then
  pass "the EDN file on disk now says :published — the resource graph is the authority"
else
  fail "the resource file was not rewritten, it says: ${on_disk:-<unreadable>}"
fi

ab reload >/dev/null
ab wait 3000 >/dev/null
shot "cms-after-publish"

after="$(ab eval "(async () => { const h = {'x-knoxx-user-email': localStorage.getItem('knoxx_user_email') || '', 'x-knoxx-org-slug': localStorage.getItem('knoxx_org_slug') || ''}; return (await fetch('/api/cms/publications/documents', {headers: h})).text(); })()")"
after_payload="$(printf '%s' "$after" | jq -r . 2>/dev/null)"
after_desired="$(printf '%s' "$after_payload" | jq -r "[.documents[].publications[] | select(.id == \"${FIXTURE_PUB_ID}\")] | .[0].desired" 2>/dev/null)"

if [ "$after_desired" = "published" ]; then
  pass "re-reading the topology shows desired = published"
else
  fail "after the write the topology still says: ${after_desired:-<none>}"
fi

# ── 5. Authorization from the same origin ──────────────────────────────────

step "5. the same surface refuses an unauthenticated caller"

# Strip the identity and re-issue the request from the same page. A route that
# answers 200 here is an enumeration leak: the projection exposes document
# titles, garden membership, and publication paths.
anon="$(ab eval "(async () => (await fetch('/api/cms/publications/documents', {headers:{'x-knoxx-user-email':'', 'x-knoxx-org-slug':''}})).status)()")"
if printf '%s' "$anon" | grep -qE "401|403"; then
  pass "an anonymous request is refused (401/403)"
else
  fail "an anonymous request was not refused" "$(printf '%s' "$anon" | head -c 200)"
fi

shot "final-state"

# ── Summary ────────────────────────────────────────────────────────────────

printf '\n%s%s%s\n' "$C_BOLD" "$(printf '═%.0s' $(seq 1 64))" "$C_RESET"
printf 'screenshots: %s\n' "${SHOT_DIR#$REPO_ROOT/}"
if [ "$FAIL_COUNT" -eq 0 ]; then
  printf '%stour completed with no failures%s\n\n' "$C_GREEN$C_BOLD" "$C_RESET"
  exit 0
fi
printf '%s%d step(s) FAILED%s\n\n' "$C_RED$C_BOLD" "$FAIL_COUNT" "$C_RESET"
exit 1
