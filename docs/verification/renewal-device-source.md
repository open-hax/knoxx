# Renewal and cross-device login source verification

This worker changed only owned source checkouts. No live service, credentials,
real actor, database, commit, board, or canonical ledger was used or changed.

Use the existing explicit issuer+subject link to the exact existing user,
membership, org and actor. Enable the authority confidential client's
`refresh-enabled?` option for root-bound refresh. Enable
`KNOXX_AXXIUM_DEVICE_ENABLED=true` only for an RFC8628-enabled client.
The login UI then offers **Sign in using another device** alongside native login.

The random `__Host-knoxx_axxium_session` cookie contains only a handle. Access
and refresh tokens, PKCE verifiers, and device codes remain server-side. Browser
expiry is bounded by 24 hours and the authority's current stored parent expiry.
Normal responses reissue the cookie with the remaining deadline; renewal cannot
move that deadline. Every admission introspects live and rereads the exact local
actor/membership context. The maintained RP validates signatures and issuer/audience.
Renewal requires subject continuity and original nonce continuity when a nonce is
present. Standard nonce omission on refresh is permitted.

Concurrent requests share one rotation. Logout removes admission before revocation,
revokes both token types, and late rotated results are revoked. An outage or failed
revocation cannot restore admission. Upgraded transports retain their short lease,
additionally bounded by current token/root expiry. Product heap state is ephemeral;
restart requires sign-in. Singleflight is per process, so multi-worker qualification
requires sticky ownership or a shared transactional product session store.

Device start/poll/cancel are fixed same-origin POST endpoints. Start returns only
user code, authority-pinned verification link, expiry and interval. A separate
host-only HttpOnly Secure cookie selects the transaction. The maintained library
handles OAuth polling/backoff and signatures; the browser polls only the product.
Transactions expire within ten minutes. Cancel aborts polling; completion is
one-use; cancel/logout wins local-context lookup. Identity resolution is shared
with code login and never creates a root actor. No browser login grants federation.

```sh
npm --prefix backend run typecheck
npm --prefix backend run test:oidc
npm --prefix backend run build
npm --prefix frontend run build
npm --prefix frontend test
npm --prefix frontend run test:oidc
npm --prefix frontend run test:cljs
```

The OIDC target uses synthetic effects and a maintained RP with signed protocol
fixtures; no live account or DB is needed. New async helpers catch rejection and
fail an assertion before signalling completion. Full backend `npm test` and
`npm run test:shadow` were attempted: JS harness environment tests fail, and the
CLJS suite stops at a pre-existing history test opening a forbidden loopback socket.
Those runs are not a green full backend suite.

After parent integration, use the existing `scripts/verify-axxium-oidc-tour.sh`
with its checkout/runtime preconditions. Verify a protected request after the
original token TTL and logout during renewal. On a second browser request a code,
approve it from the existing real Axxium root session, and verify matching code,
local actor attribution, cancel/denial/expiry, and refusal after root logout.
The worker did not run a live tour or capture user data. Synthetic tests do not
replace multi-host integration and human device qualification.

Historical preparation branch only: the full frontend CLJS target was also attempted: 462 tests, 2 failures and 7
errors in the migration Git adapter tests, all reporting `spawnSync git EPERM`.
The focused OIDC UI target passes separately. Frontend Vitest reports 44 files
passing (231 tests, 41 todo), and the production frontend build compiles with zero
warnings. Re-run the full frontend CLJS target in the parent's process-capable
harness; do not call the full frontend suite green based on the focused run.

Parent integration correction: the temporary focused Shadow targets were removed.
Their duplicate backend module entry and additional frontend raw-JavaScript text
input are unnecessary; the existing whole-suite targets cover the new tests and
retain the checked-in migration inventory. Use backend `pnpm test` and frontend
`pnpm test:cljs`; no migration guard was relaxed. Parent native backend ran1871
 tests/9251assertions with zero failures/errors.


## Current-main parent results

The earlier preparation counts and migration-inventory failure above are
historical. Parent removed the unnecessary focused shadow targets and retained
the inventory guard. The current main-based source passes1876 backend tests/9302 assertions and457
frontend tests/2049 assertions, with zero failures, errors and compiler warnings.
Review follow-up adds renewal-window straddle, capacity and rollback logout tests
and preserves live sessions on origin/policy refusal. The changed login namespace now passes the actual
hosted changed-file linter with zero errors and warnings. Deployment, browser
sign-in, device approval and cross-host acceptance remain distinct requirements.
