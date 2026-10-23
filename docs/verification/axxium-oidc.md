# Axxium redirect login consumer

This is consumer source preparation. It does not qualify an owner policy,
Rheos readiness, native-login retirement, federation permission, deployment or
a real human login. Existing local/GitHub login and resource attribution remain
under Knoxx policy.

## Operator configuration

Enable only after the authority has registered a distinct confidential client
for this Knoxx product/host. Never reuse another product's client credentials.
Supply secrets through the existing trusted runtime configuration mechanism;
do not put them in tracked files, browser storage or verification transcripts.

| Setting | Required value |
| --- | --- |
| `KNOXX_AXXIUM_OIDC_ENABLED` | Exactly `true` to enable; otherwise disabled |
| `KNOXX_AXXIUM_OIDC_ISSUER` | Default `https://axxium.promethean.rest/oidc`; exact operator-configured HTTPS issuer |
| `KNOXX_PUBLIC_BASE_URL` | Exact public HTTPS origin, without trailing slash or path |
| `KNOXX_AXXIUM_OIDC_CLIENT_ID` | This host's Knoxx client ID |
| `KNOXX_AXXIUM_OIDC_CLIENT_SECRET` | This host's confidential client secret |
| `KNOXX_AXXIUM_OIDC_LINKS` | JSON array of explicitly approved links; defaults to an empty array, admitting nobody |

The **only** callback URI to register is
`<KNOXX_PUBLIC_BASE_URL>/api/auth/axxium/callback`. The authority should register
`authorization_code`, response type `code`, `client_secret_post`, and
`openid profile`. Set the authority client's `refresh-enabled?` option for
root-bound rotating refresh tokens retained only in the product backend.
Optional device initiation requires `KNOXX_AXXIUM_DEVICE_ENABLED=true` and an
RFC8628-enabled authority client. No offline access, email, roles, administration,
or federation permission is requested. See `renewal-device-source.md`.

Each approved link is shaped like this **fictional example**, not an account
to create:

```json
[{"issuer":"https://axxium.promethean.rest/oidc",
  "subject":"authority-issued-subject",
  "user-id":"existing-knoxx-user-id",
  "membership-id":"existing-knoxx-membership-id",
  "org-id":"existing-knoxx-org-id"}]
```

The linked local membership must already have an explicitly stored actor
binding. A role-derived default actor is insufficient. Knoxx resolves the
membership's current user, organization, actor and policy on each admission,
then compares the three approved local IDs. Missing or conflicting links refuse
with an approval-required message. No email/name matching, provisioning,
resource reassignment or remote role import occurs. Link approval and local
directory administration remain operator work outside these login routes.

Configuration is never derived from Host, forwarded headers, `redirect`,
`issuer` or a caller-supplied callback. Discovery uses the exact issuer; the
authorization, token, JWKS, introspection and revocation endpoints must share
its HTTPS origin. `openid-client` **6.8.8** owns state/nonce/PKCE protocol checks
and ID-token issuer, audience, expiry and RS256 signature validation. The
adapter explicitly enables its JWKS signature checks in code flow. See the
[pinned relying-party API source](https://github.com/panva/openid-client/blob/v6.8.8/src/index.ts).

## Runtime behavior and boundaries

- `GET /api/auth/axxium/start` redirects to the authority. Five-minute state,
  nonce and PKCE verifier stay server-side. An independent secure, HttpOnly,
  host-only transaction cookie binds the callback to the initiating browser.
- `GET /api/auth/axxium/callback` consumes state before any asynchronous work,
  validates the protocol response, checks the approved local link, introspects
  the opaque access token, and issues a random HttpOnly session handle. Success
  always redirects to the configured Knoxx origin's `/`. It clears the native
  browser cookie so revocation cannot silently fall back to an old native
  session.
- `POST /api/auth/axxium/logout` and the existing `POST /api/auth/logout`
  remove OIDC admission before attempting token revocation. OIDC logout needs
  an exact configured `Origin`. Failed remote revocation returns 503 with a
  safe message while keeping local admission deleted. Local Knoxx logout does
  not log out the Axxium root session on its separate host.
- Protected HTTP requests with an OIDC cookie require current client-scoped
  introspection, even if native identity headers, API keys, native cookies or
  a cached request context are present. Inactive, wrong-subject/client,
  expired or unavailable evidence refuses. Optional introspection `iss`/`aud`
  must match when supplied; `active`, `client_id`, `sub` and `exp` are mandatory.
- HTTP admission caches positive evidence for zero seconds. Introspection and
  local policy resolution together must take less than five seconds. OIDC
  WebSocket transports close after 25 seconds; reconnection must reauthenticate.
  This bounds retained evidence to 30 seconds. The bootstrap registers the
  parent admission hook before the WebSocket child scope. When OIDC is enabled,
  a WebSocket reconnect without an OIDC cookie must pass existing native
  authentication; a cleared/revoked cookie cannot become anonymous access.
  Qualify frontend
  reconnect behavior before cutover; periodic OIDC socket reconnects are an
  explicit tradeoff of this conservative source implementation.

`extern.axxium-oidc` owns openid-client, trusted environment/URI decoding,
browser cookies, request/header conversion, Fastify registration and the
upgraded HTTP transport lease. `law.oidc` contains portable pure admission
decisions. `infra.auth.oidc` orchestrates bounded in-memory transactions and
sessions. No new generic JS extern imports or TypeScript UI were added.

Tokens and RP handles live only in this backend process's heap. There is no
new persistent token store or independent local session minted for an OIDC
login. Restart/reload loses these sessions and requires redirect login again.
Multiple backend workers need sticky routing for start/callback/session traffic
or a separately reviewed shared server-side store. Pending transactions are
capped at 1,000 and sessions at 10,000, with expired entries pruned on insertion.
With a code-only authority registration, users reenter the
redirect flow at least that often; their authority-host root cookie provides
SSO while valid. No root cookie is copied between machines.

## Automated verification

Install dependencies and regenerate the backend lockfiles using a supported
Node version (24 recommended) and the normal parent-owned package workflow.
The dependency is pinned in `backend/package.json`; this worker could not
resolve it from npm because registry DNS failed. **Lockfile resolution and
runtime tests are outstanding; do not deploy this source from stale locks.**

```sh
pnpm -C backend add --save-exact openid-client@6.8.8
pnpm -C backend typecheck
pnpm -C backend exec shadow-cljs compile test
pnpm -C backend exec shadow-cljs compile server
pnpm -C backend boundary:check
pnpm -C frontend exec shadow-cljs compile test
pnpm -C frontend run build
```

Treat runtime import errors or test failures as failures even if shadow exits 0.
The added tests cover browser-bound and one-use state, state expiry, S256
exchange, wrong nonce/issuer/audience, discovery substitution, fixed callback,
qualified links, local actor/tenant collisions, protected context with spoofed
headers, current grant checks, expiry/revocation/outage, logout races and Origin.
Protocol tests use the actual pinned RP against a disposable in-memory signing
and discovery fixture. They open no sockets and create no real accounts.

Worker observations: portable JVM laws ran green (4 tests, 32 assertions);
backend compilation emitted
zero warnings; CLJS test execution failed loading the unavailable
`openid-client` module. Frontend test compilation hit unchanged
`frontend/src/cljs/knoxx/frontend/auth/api.cljs:9`, where the available compiler
reports undeclared `await`. `pnpm -C frontend run build` also cannot find Vite
because this worktree has no frontend `node_modules`. Neither full suite nor
the real browser tour is qualified by those compiler results.

Strict lint passes with zero warnings on the new OIDC files and frontend
changes. Comparing the original HEAD files to this diff confirms ten existing
legacy warnings: `infra/auth/authz.cljs` (`principal-match?` size),
`infra/auth/session.cljs` (two unused private helpers, three unused session-id
arguments and file size), and `infra/routes/auth.cljs` (signup, local login and
invite redemption function size). This source does not claim a globally
warning-free repository or suppress that baseline. The default pnpm executable
also reports Node 22.18.0 below the backend's declared minimum of 22.19.0.

## Live human verification

Start this checkout through the existing authorized workflow. The worker does
not start/restart a service or use credentials. To guard against testing another
worktree, obtain the serving backend PID and its startup `bootstrap-hmr-*`
token from the operator. The script verifies `/proc/<pid>/cwd` and matches that
token through the requested public origin before doing any feature checks.

```sh
export KNOXX_VERIFY_ORIGIN=https://your-knoxx-host.example
export KNOXX_VERIFY_BACKEND_PID=<serving-pid>
export KNOXX_VERIFY_HMR_TOKEN=<startup-bootstrap-hmr-token>
scripts/verify-axxium-oidc.sh
scripts/verify-axxium-oidc-tour.sh
```

The first script creates its own private temporary cookie jar and browser
transaction, checks public entry/callback/logout and anonymous/spoofed protected
access, then consumes its transaction and removes the dedicated directory on
exit or interruption. It never exchanges a valid code or creates an account.
Each PASS explains the admission it proves; failures exit nonzero. WARN lines
identify the real-human and authority tests it cannot perform.

The tour uses the existing `agent-browser` CLI in a fresh visible session and
captures login, authorized Knoxx and post-revocation login screenshots in
`docs/verification/screenshots/`. The operator enters credentials/consent in the
authority-host browser UI, then explicitly signs out at the authority or revokes
the grant. It does not export cookies/storage, fabricate consent, or capture
callback URLs. The tour closes its browser session on exit. Screenshot files
are ignored and contain UI data; inspect locally before sharing.

Before production qualification, also run separate real tests for an unlinked
identity/collision, wrong registered client/audience, authority unavailable,
access-token expiry, authority actor disable, local Knoxx logout, existing native
login and live WebSocket reconnect behavior on each approved host. Do not infer
federation enrollment or provider access from a successful shared login.
