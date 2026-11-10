#!/usr/bin/env bash
# GPL-3.0-or-later. Public HTTP checks and an optional signed-out browser tour.
# OAuth consent, token scope and the Drive archive worker are separate checks.
set -euo pipefail

fail() { printf 'FAIL  %s\n' "$1" >&2; exit 1; }
pass() { printf 'PASS  %s\n' "$1"; }

if [[ $# -lt 1 || $# -gt 2 || (${2:-} != '' && ${2:-} != '--browser') ]]; then
  fail 'Usage: scripts/verify-public-privacy.sh BASE_URL [--browser]'
fi
base=${1%/}
[[ $base =~ ^https?://[^@/?#[:space:]]+$ ]] || fail 'Use an HTTP(S) origin without credentials, path or query'
for tool in curl python3 cmp; do
  command -v "$tool" >/dev/null || fail "Missing required tool $tool"
done
if [[ ${2:-} == --browser ]]; then
  command -v agent-browser >/dev/null || fail 'Install agent-browser for the browser tour'
fi
repo=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
policy="$repo/frontend/public/legal/privacy.html"
[[ -s $policy ]] || fail 'This checkout does not contain frontend/public/legal/privacy.html'

scratch=$(mktemp -d "${TMPDIR:-/tmp}/knoxx-public-privacy.XXXXXXXX")
session="knoxx-public-privacy-${scratch##*.}"
browser_started=0
ab() { agent-browser --session "$session" "$@"; }
cleanup() {
  if (( browser_started )); then ab close >/dev/null 2>&1 || true; fi
  rm -r -- "$scratch"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

# -q suppresses the operator's curl config. No cookies, headers or redirects
# are used, so a login redirect cannot qualify as a public page.
fetch() {
  curl -q --silent --show-error --max-time 15 --output "$scratch/$2" \
    --write-out '%{http_code} %{content_type}' "$base$1"
}

printf 'Checking public origin %s\n' "$base"
home_result=$(fetch / home.html) || fail 'Could not reach the public homepage'
[[ $home_result == '200 text/html'* ]] || fail 'Homepage must return HTML with HTTP 200 without redirect'
policy_result=$(fetch /privacy.html policy.html) || fail 'Could not reach the privacy policy'
[[ $policy_result == '200 text/html'* ]] || fail 'Privacy policy must return HTML with HTTP 200 without redirect'
cmp -s "$policy" "$scratch/policy.html" ||
  fail 'Served policy differs from this checkout. Confirm the deployed frontend source'
if cmp -s "$scratch/home.html" "$scratch/policy.html"; then
  fail 'Homepage and policy returned the same HTML'
fi
pass 'Signed-out homepage and policy each return distinct HTML, and the served policy matches this checkout'

probe_result=$(fetch /__knoxx_public_privacy_route_probe__ probe.html) ||
  fail 'Could not check the unrelated route'
case "$probe_result" in
  '200 '*) ;;
  '404 '*) : >"$scratch/probe.html" ;;
  *) fail 'Unrelated route returned neither a fallback page nor HTTP 404' ;;
esac

python3 - "$scratch" <<'PY'
from html.parser import HTMLParser
from pathlib import Path
import re
import sys


class Page(HTMLParser):
    def __init__(self, path):
        super().__init__()
        self.title = []
        self.root_text = []
        self.privacy_link = False
        self.in_title = False
        self.div_depth = self.root_depth = 0
        self.anchor_text = None
        self.feed(path.read_text(encoding="utf-8"))

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "div":
            self.div_depth += 1
            if attrs.get("id") == "root":
                self.root_depth = self.div_depth
        elif tag == "title":
            self.in_title = True
        elif tag == "a" and self.root_depth and attrs.get("href") == "/privacy.html":
            self.anchor_text = []

    def handle_endtag(self, tag):
        if tag == "div":
            if self.root_depth == self.div_depth:
                self.root_depth = 0
            self.div_depth -= 1
        elif tag == "title":
            self.in_title = False
        elif tag == "a" and self.anchor_text is not None:
            self.privacy_link |= " ".join("".join(self.anchor_text).split()) == "Privacy Policy"
            self.anchor_text = None

    def handle_data(self, data):
        if self.in_title:
            self.title.append(data)
        if self.root_depth:
            self.root_text.append(data)
        if self.anchor_text is not None:
            self.anchor_text.append(data)


def check(ok, message):
    if not ok:
        sys.exit(f"FAIL  {message}")


scratch = Path(sys.argv[1])
home = Page(scratch / "home.html")
policy = Page(scratch / "policy.html")
probe = Page(scratch / "probe.html")
policy_title = "Knoxx Privacy Policy"
check("".join(policy.title).strip() == policy_title, "Privacy route has the wrong document title")
check("".join(probe.title).strip() != policy_title, "An unrelated route also serves the privacy document")
print("PASS  Only the dedicated route returned the Knoxx Privacy Policy document title")
check(home.privacy_link, "Initial homepage #root lacks the visible Privacy Policy link to /privacy.html")
text = " ".join(" ".join(home.root_text).lower().split())
for phrase in (r"ai[- ]assisted", r"\bchat\b", r"\btools\b", r"connected services",
               r"google sign[- ]in", r"drive.{0,60}archiv|archiv.{0,60}drive"):
    check(re.search(phrase, text), f"Homepage #root is missing a required disclosure matching {phrase}")
print("PASS  Initial homepage links to the policy and describes Knoxx, Google sign-in and Drive archiving")
PY

if [[ ${2:-} == --browser ]]; then
  unset AGENT_BROWSER_STATE AGENT_BROWSER_SESSION_NAME AGENT_BROWSER_AUTO_CONNECT
  unset AGENT_BROWSER_HEADERS AGENT_BROWSER_IGNORE_HTTPS_ERRORS AGENT_BROWSER_PROVIDER AGENT_BROWSER_JSON
  shots=${KNOXX_SHOT_DIR:-"$repo/docs/verification/screenshots"}
  mkdir -p "$shots"
  browser_started=1
  ab --profile "$scratch/browser" open "$base/" >/dev/null || fail 'Browser could not open the public homepage'
  ab set media dark >/dev/null || fail 'Browser could not set its color scheme'
  ab wait --text 'or redeem an invite' >/dev/null ||
    fail 'Browser did not reach the hydrated signed-out homepage'
  ab screenshot "$shots/public-privacy-home.png" >/dev/null ||
    fail 'Could not capture the signed-out homepage'
  ab find role link click --name 'Privacy Policy' --exact >/dev/null ||
    fail 'Browser could not follow the signed-out Privacy Policy link'
  ab wait --url "$base/privacy.html" >/dev/null ||
    fail 'Browser link did not navigate to the exact public policy URL'
  [[ $(ab get title) == 'Knoxx Privacy Policy' ]] ||
    fail 'Browser did not display the privacy policy document'
  ab screenshot "$shots/public-privacy-policy.png" >/dev/null ||
    fail 'Could not capture the public privacy policy'
  pass "Hydrated signed-out homepage links to the public policy. Screenshots in $shots"
else
  printf '%s\n' 'WARN  Browser tour skipped. Use --browser to verify the hydrated homepage and capture screenshots.'
fi
printf '%s\n' 'WARN  Google OAuth consent, token scope and archive-worker operation are outside these public-page checks.'
