#!/usr/bin/env bash
# Live, unauthenticated OIDC boundary proof. Never supplies a human identity.
# Seeds only one short-lived browser transaction and consumes it on teardown.
# Requires the operator's serving PID and startup HMR probe to reject the wrong
# checkout. Does not prove real login, signing, authority logout or actor disable;
# those require the human tour and the isolated protocol tests described in docs.
set -euo pipefail
trap 'printf "FAIL live OIDC verification stopped at script line %s\n" "$LINENO" >&2' ERR
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
: "${KNOXX_VERIFY_ORIGIN:?Set the exact configured HTTPS Knoxx origin}"
: "${KNOXX_VERIFY_BACKEND_PID:?Set the PID serving this checkout}"
: "${KNOXX_VERIFY_HMR_TOKEN:?Set the serving process startup bootstrap-hmr token}"
for command in curl python3 readlink; do command -v "$command" >/dev/null; done
[[ "$KNOXX_VERIFY_BACKEND_PID" =~ ^[0-9]+$ ]] || { echo 'FAIL invalid PID'; exit 1; }
[[ "$(readlink -f "/proc/$KNOXX_VERIFY_BACKEND_PID/cwd")" == "$repo_root/backend" ]] || {
  echo 'FAIL serving process must run from this checkout/backend'; exit 1;
}
python3 - "$KNOXX_VERIFY_ORIGIN" <<'PY'
import sys, urllib.parse
u = urllib.parse.urlsplit(sys.argv[1])
assert u.scheme == 'https' and u.netloc and not u.username and not u.password
assert not u.path and not u.query and not u.fragment, 'Use the exact HTTPS origin without trailing slash'
PY
fixture_dir="$(mktemp -d "$repo_root/.verify-axxium-oidc.XXXXXX")"
chmod 700 "$fixture_dir"
cleanup() {
  if [[ -f "$fixture_dir/state" ]]; then
    # No code: consumes the pending transaction without exchanging a grant.
    curl --silent --max-time 10 -b "$fixture_dir/cookies" \
      "$KNOXX_VERIFY_ORIGIN/api/auth/axxium/callback?state=$(cat "$fixture_dir/state")" >/dev/null || true
  fi
  rm -rf -- "$fixture_dir"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
failures=0
expect() {
  local status
  status="$(curl --silent --show-error --max-time 15 -o "$fixture_dir/body" -w '%{http_code}' "$@")" || status=000
  if [[ " $expected " == *" $status "* ]]; then
    printf 'PASS %s (%s)\n' "$label" "$status"
  else
    printf 'FAIL %s — expected %s, got %s\n' "$label" "$expected" "$status"
    failures=$((failures + 1))
  fi
}
curl --silent --show-error --max-time 15 -D "$fixture_dir/probe" -o /dev/null "$KNOXX_VERIFY_ORIGIN/api/dev/hmr"
python3 - "$fixture_dir/probe" "$KNOXX_VERIFY_HMR_TOKEN" <<'PY'
import sys
headers = open(sys.argv[1]).read().splitlines()
assert any(line.lower().startswith('x-knoxx-hmr-probe:') and line.split(':', 1)[1].strip() == sys.argv[2]
           for line in headers), 'FAIL public route is not the specified serving process'
print('PASS checkout PID and public HMR probe identify the intended serving process')
PY
curl --fail --silent --show-error --max-time 15 "$KNOXX_VERIFY_ORIGIN/api/auth/config" > "$fixture_dir/config"
python3 - "$fixture_dir/config" "$KNOXX_VERIFY_ORIGIN" <<'PY'
import json, sys
c = json.load(open(sys.argv[1]))
assert c['axxiumOidcEnabled'] is True and c['axxiumOidcLoginUrl'] == '/api/auth/axxium/start'
assert c['publicBaseUrl'] == sys.argv[2], 'FAIL unexpected configured origin'
print('PASS redirect SSO is enabled at the fixed browser entry')
PY
expected='401 403'; label='anonymous auth context cannot acquire a Knoxx identity'
expect "$KNOXX_VERIFY_ORIGIN/api/auth/context"
expected='401 403'; label='anonymous WebSocket reconnect cannot bypass a cleared OIDC cookie'
expect -H 'Upgrade: websocket' "$KNOXX_VERIFY_ORIGIN/ws/stream"
expected=401; label='unknown OIDC session cannot fall back to supplied native identity headers'
expect -H 'Cookie: __Host-knoxx_axxium_session=not-a-session' \
  -H 'X-Knoxx-Membership-Id: untrusted-admin' "$KNOXX_VERIFY_ORIGIN/api/auth/context"
expected=400; label='callback without browser transaction is refused'
expect "$KNOXX_VERIFY_ORIGIN/api/auth/axxium/callback"
expected=403; label='cross-origin logout is refused'
expect -X POST -H 'Origin: https://attacker.invalid' "$KNOXX_VERIFY_ORIGIN/api/auth/axxium/logout"
expected=200; label='same-origin anonymous logout is idempotent'
expect -X POST -H "Origin: $KNOXX_VERIFY_ORIGIN" "$KNOXX_VERIFY_ORIGIN/api/auth/axxium/logout"
curl --silent --show-error --max-time 15 -D "$fixture_dir/start" -o /dev/null -c "$fixture_dir/cookies" \
  "$KNOXX_VERIFY_ORIGIN/api/auth/axxium/start?redirect=https://attacker.invalid&issuer=https://attacker.invalid"
python3 - "$fixture_dir/start" "$fixture_dir/config" "$fixture_dir/state" <<'PY'
import json, sys, urllib.parse
lines = open(sys.argv[1]).read().splitlines()
assert any(line.startswith('HTTP/') and '302' in line for line in lines), 'FAIL begin did not redirect'
location = next(line.split(':', 1)[1].strip() for line in lines if line.lower().startswith('location:'))
u = urllib.parse.urlsplit(location); q = urllib.parse.parse_qs(u.query)
c = json.load(open(sys.argv[2]))
assert u.scheme == 'https' and u.hostname != 'attacker.invalid'
assert q['redirect_uri'] == [c['publicBaseUrl'] + '/api/auth/axxium/callback']
assert q['code_challenge_method'] == ['S256'] and q.get('code_challenge') and q.get('state') and q.get('nonce')
assert not any(k in q for k in ('access_token', 'client_secret', 'code_verifier'))
assert any('HttpOnly' in line and 'Secure' in line and 'SameSite=Lax' in line
           for line in lines if line.lower().startswith('set-cookie:'))
open(sys.argv[3], 'w').write(q['state'][0])
print('PASS begin uses fixed callback, state, nonce, S256 and a secure HttpOnly browser binding')
PY
expected=400; label='missing code consumes this browser transaction without creating a session'
expect -b "$fixture_dir/cookies" "$KNOXX_VERIFY_ORIGIN/api/auth/axxium/callback?state=$(cat "$fixture_dir/state")"
expected=400; label='consumed state cannot be replayed'
expect -b "$fixture_dir/cookies" "$KNOXX_VERIFY_ORIGIN/api/auth/axxium/callback?state=$(cat "$fixture_dir/state")&code=invalid"
printf 'WARN real login, approved/colliding links, authority outage/revocation/expiry and actor disable need operator qualification; no human session was fabricated\n'
printf '%s failed checks\n' "$failures"
(( failures == 0 ))
