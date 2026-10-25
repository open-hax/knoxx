#!/usr/bin/env bash
# Guided real login tour. Requires a human at the visible browser; never types
# passwords, exports storage, manufactures sessions or captures callback URLs.
# Intentionally skips automated authority/root-session mutation: the operator
# signs out there manually. Device flow/federation and native retirement are out
# of scope. Screenshot artifacts contain UI data and are regenerated per run.
set -Eeuo pipefail
printf 'WARN this guided tour skips device flow, federation, native retirement, collision, wrong audience, expiry, outage and local logout; qualify these separately with isolated tests and real host/client registrations\n'
step="preconditions"
trap 'status=$?; printf "FAIL tour step %s exited %s\n" "$step" "$status" >&2; exit "$status"' ERR
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
"$repo_root/scripts/verify-axxium-oidc.sh"
command -v agent-browser >/dev/null
session_name="axxium-oidc-tour-$$"
mkdir -p "$repo_root/docs/verification/screenshots"
shot_dir="$(mktemp -d "$repo_root/docs/verification/screenshots/axxium-oidc-XXXXXXXXXX")"
ab() { agent-browser --session "$session_name" "$@"; }
cleanup() {
  # Navigation restores the Knoxx origin even if interrupted at the authority.
  # The logout request also consumes this browser's pending transaction.
  ab open "$KNOXX_VERIFY_ORIGIN/login" >/dev/null 2>&1 || true
  ab eval 'fetch("/api/auth/axxium/logout", {method:"POST", credentials:"include"}).then(r => r.status)' >/dev/null 2>&1 || true
  ab close >/dev/null 2>&1 || true
  rm -rf -- "$shot_dir"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
step="open-login"
ab --headed open "$KNOXX_VERIFY_ORIGIN/login" >/dev/null
ab set media dark >/dev/null
ab wait 'a[href="/api/auth/axxium/start"]' >/dev/null
step="capture-login"
ab screenshot "$shot_dir/01-knoxx-login.png" >/dev/null
ab click 'a[href="/api/auth/axxium/start"]' >/dev/null
printf 'Complete the REAL Axxium login/consent in the visible browser. Do not enter credentials into this terminal.\n'
step="await-user-consent"
read -r -p 'Press Enter after returning to the Knoxx workspace: ' _answer
step="verify-protected-context"
ab wait --url "$KNOXX_VERIFY_ORIGIN/" >/dev/null
# Return only an HTTP status from the browser, never auth data or storage.
status="$(ab eval 'fetch("/api/auth/context", {credentials:"include"}).then(r => r.status)')"
[[ "$status" == *200* ]] || { echo 'FAIL real shared login did not obtain protected auth context'; exit 1; }
ab screenshot "$shot_dir/02-knoxx-authorized.png" >/dev/null
printf 'PASS the real redirect login acquired a protected Knoxx context\n'
printf 'Sign out at the Axxium authority in another browser tab (or have the operator revoke this grant).\n'
step="await-authority-revocation"
read -r -p 'Press Enter after root-session logout or revocation: ' _answer
step="verify-revocation"
status="$(ab eval 'fetch("/api/auth/context", {credentials:"include"}).then(r => r.status)')"
[[ "$status" == *401* || "$status" == *403* ]] || { echo 'FAIL authority revocation did not refuse protected auth context'; exit 1; }
ab open "$KNOXX_VERIFY_ORIGIN/login" >/dev/null
ab screenshot "$shot_dir/03-revoked-login.png" >/dev/null
printf 'PASS authority logout/revocation refused the next protected request\n'
printf 'Screenshots were captured for this run; the dedicated directory is removed on exit\n'
