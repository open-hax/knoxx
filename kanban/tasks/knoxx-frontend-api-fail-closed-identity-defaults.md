---
uuid: knoxx-frontend-api-fail-closed-identity-defaults
title: Fail closed instead of trusting browser-supplied API identity
status: incoming
priority: P1
points: 5
labels: tasks, 5sp, frontend, backend, security, auth
created_at: 2026-08-30T01:19:13.727Z
category: tasks
---

# Fail closed instead of trusting browser-supplied API identity

> GitHub issue: [#2](https://github.com/open-hax/knoxx/issues/2)
> Complements: `knoxx-tenant-fail-closed-route-guard` (#175; duplicate intake #9 is closed) and
> `knoxx-translation-config-trusted-auth-context` (#283)

## Signal

The original shared frontend request helper injected a privileged
`system-admin@open-hax.local` / `open-hax` identity when browser storage was empty or unavailable.
That TypeScript path now lives at `frontend/src/lib/api/core.ts` and combines mutable storage with
optional `VITE_KNOXX_DEV_*` defaults, but it is not the only active path.

`frontend/src/cljs/knoxx/frontend/lib/api.cljs` independently reads the same storage keys and
emits the same identity headers for documents, gardens, mail, publication, settings, and
translations. The admin surface also exposes `getKnoxxAuthIdentity` / `setKnoxxAuthIdentity` as an
"Apply actor" form, and generated bridge assets contain the TypeScript helper. A production
request must never acquire authentication authority from any hard-coded, build-time, or
mutable-browser identity fallback.

Removing those emitters is necessary but not sufficient. The current server also promotes the
same client-supplied identity headers into authority. `infra/auth/authz.cljs` function
`resolve-request-context!` selects `x-knoxx-user-email` or `x-knoxx-membership-id` before the
cookie resolver. In `infra/auth/session.cljs`, `create-session-hook` skips cookie hydration when
either header is already present, and `resolve-auth-context` sends those headers directly to
policy context resolution before checking an API key or cookie. A direct client can therefore
produce a non-nil spoofed context even after every frontend helper is repaired; #175's nil-context
guard cannot constrain authority that was synthesized before the route guard.

The same identity bytes also cross credentialed service proxies. In
`backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs`,
`openplanner-proxy-handler!` copies `x-knoxx-user-email` and `x-knoxx-org-slug` from the request,
then `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` merges those values into an
outbound request carrying Knoxx's server-held OpenPlanner bearer credential. The ingestion path in
`register-ingestion-service-proxy-route!` clones the complete inbound header object before calling
the service. Direct frontend callers exercise read and mutation routes through both boundaries, so
fixing only the three authentication resolvers would still allow browser-selected identity to
cross a Knoxx-owned service boundary with stronger server authority.

There is also a dedicated `GET /api/openplanner/v1/sessions` registration inside
`register-openplanner-proxy-routes!`. It bypasses `openplanner-proxy-handler!` and calls
`openplanner-client/sessions!`, which still attaches Knoxx's server-held OpenPlanner credential.
That session listing is a separate credential-required boundary, not a deliberately public read
that the wildcard handler's tests can cover implicitly.

The same registration table exposes the eta-mu and OpenCode session list, status, and ingest admin
routes. `register-session-status-route!` and `register-session-ingest-route!` reach KMS through
`system-kms-headers`, whose fixed `system-admin@open-hax.local` identity is stronger than caller
input; `source-ingest-request!` can create a downstream job. The two list routes expose local
session data even without a downstream credential. These six admin endpoints therefore need an
explicit caller authorization fence before either local observation or system-authorized KMS work.

## Scope

- Inventory both central request families and every direct `buildKnoxxAuthHeaders` consumer:
  `frontend/src/lib/api/core.ts`, `frontend/src/cljs/knoxx/frontend/lib/api.cljs`, their TypeScript
  and CLJS callers, and the generated bridge rebuilt from source.
- Repair the server authentication boundary in `backend/src/cljs/knoxx/backend/infra/auth/`:
  inventory `authz/resolve-request-context!`, `session/create-session-hook`,
  `session/resolve-auth-context`, and every path that converts request headers into a policy
  context. Client-supplied identity headers must be rejected or stripped before context
  resolution; any internal identity handoff must be distinguishable from untrusted wire input and
  derived from an already-verified session or API key.
- Complete a credentialed service proxy inventory, beginning with
  `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` functions
  `openplanner-proxy-handler!` and `register-ingestion-service-proxy-route!`, plus
  `register-openplanner-proxy-routes!` and its dedicated `GET /api/openplanner/v1/sessions` call to
  `openplanner-client/sessions!`, plus
  `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs`. Include every Knoxx route that
  accepts inbound data or headers and then calls a downstream service with Knoxx-held credentials;
  record which headers are constructed locally, copied, or forwarded.
- At every such proxy boundary, drop every inbound `x-knoxx-*` identity header before constructing
  the outbound request. Knoxx's server-held OpenPlanner bearer credential and any other service
  credential must not amplify untrusted identity. If the downstream protocol genuinely requires
  identity, rederive it from the verified server request context and pass it through a named
  internal service adapter; never reuse client bytes or let an absent context become a system
  actor.
- The dedicated OpenPlanner sessions listing is credential-required: require a verified Knoxx
  request context before any downstream call. The server-held bearer credential cannot make this
  session inventory anonymously readable or serve as caller authentication.
- Inventory `system-kms-headers`, `session-status-handler!`, `source-ingest-request!`,
  `register-session-status-route!`, and `register-session-ingest-route!`, together with the direct
  list responders for `/api/admin/eta-mu-sessions{,/status,/ingest}` and
  `/api/admin/opencode-sessions{,/status,/ingest}`. Require a verified administrator context and
  explicit permission before any local listing, status handler, or downstream KMS fetch/mutation.
- The fixed KMS system identity is not caller authentication. If the downstream service still
  requires that identity, it is constructed only after caller authorization inside a named service
  adapter, scoped to the admitted operation, and never exposed as the Knoxx request principal.
- Inventory direct frontend callers of `/api/openplanner/*` and `/api/ingestion/*`, including
  write-capable CMS, labels, review, garden, workspace, and ingestion flows and
  `frontend/src/test/ui-backend-surface-matrix.ts`. Cover the listed GET, POST, PUT, PATCH, and
  DELETE proxy routes rather than proving only a harmless read.
- Remove storage-derived, build-time-default, and caller-supplied identity-header injection from
  production frontend requests. `knoxx_user_email`, `knoxx_org_slug`, and `x-knoxx-*` identity
  headers are not browser authentication authority. Credential-derived session cookies remain the
  browser authority; an explicitly authenticated API-key mechanism remains a non-browser option.
- Remove or redesign the admin "Apply actor" storage form so it cannot switch the authenticated
  principal. If organization/context selection remains, the server must admit it under the
  current credential rather than trusting mutable browser identity.
- If a deliberately supported local-development identity seam remains, isolate it behind an
  authenticated server-side login/session mechanism and an unmistakable development-only guard
  that cannot enter a production bundle or production request.
- When no verified authentication is available for a protected or credential-required request,
  send no fabricated identity headers, surface the server's canonical 401/reauthentication state,
  and perform no privileged retry or fallback. A deliberately public unauthenticated read may
  proceed only without fabricated identity; a downstream server-held credential cannot grant
  caller authority or turn that read into a privileged operation.
- Define credential precedence at the server: valid session and API-key credentials derive the
  principal, cannot be overridden by any client identity header, and cannot borrow identity from
  one another. Requests with only identity headers are unauthenticated.
- Exactly one credential authority is admitted per request. When session-cookie and API-key
  material are both present, reject the ambiguous request before identity or policy context
  resolution, whether both credentials name the same principal, name different principals, or
  one is malformed. Neither credential wins, and the collision permits no fallback to the other
  credential.
- Preserve caller-supplied non-identity headers, request bodies, credential inclusion, and normal
  authorized-session behavior.
- Rebuild generated frontend assets from the repaired sources; do not hand-edit compiled bridge
  output.

## TDD / proof

1. RED-prove both the TypeScript and CLJS helpers currently turn mutable storage into identity
   headers; invert both suites so empty, populated, unavailable, throwing, and malformed storage
   can never produce those headers or a privileged fallback.
2. Production builds ignore or reject configured `VITE_KNOXX_DEV_*` identity defaults, and the
   rebuilt production assets contain no request-time storage/default identity injector.
3. The admin surface cannot change the authenticated principal by editing local storage or
   submitting the former "Apply actor" form.
4. Direct-client spoof probes submit each identity header alone and in combination across
   representative protected routes; header-only input cannot produce a non-nil request context,
   resolve policy membership, or cause a protected effect.
5. Valid session and API-key credentials determine the authenticated principal and cannot be
   overridden by conflicting client identity headers; the credential-bound principal remains
   unchanged or the request fails closed.
6. Add simultaneous-credential probes for the same principal, different principals, and one
   malformed credential. Every session-cookie/API-key collision fails before context resolution,
   policy lookup, or protected effect; removing one credential restores that credential's normal
   authenticated behavior without fallback from a rejected collision.
7. Direct unauthenticated OpenPlanner wildcard and `/api/ingestion/*` probes send malicious identity
   headers through representative read and protected mutation routes. Before authorization, the
   downstream call count remains zero and no protected mutation occurs; where an unauthenticated
   read is deliberately public, its captured outbound request contains no spoofed identity.
8. A direct `GET /api/openplanner/v1/sessions` negative probe has no valid Knoxx credential and may
   include conflicting identity headers. The `openplanner-client/sessions!` call count remains zero,
   no session rows or enrichment data are disclosed, and the canonical protected-request failure is
   returned before the server bearer credential can be used.
9. Add direct unauthenticated and identity-header-only probes across all six admin routes in
   `/api/admin/eta-mu-sessions{,/status,/ingest}` and
   `/api/admin/opencode-sessions{,/status,/ingest}`. Local list/status and downstream fetch/job call
   counts remain zero; no session data, source/job data, or mutation result is disclosed.
10. Valid administrator session and API-key probes can perform each admitted admin operation;
    authenticated non-admin is denied before local/downstream work, and conflicting client identity
    headers cannot override the credential-derived administrator. Capture the KMS request to prove
    any internal system identity is added only after that authorization fence.
11. Valid session and API-key probes for the dedicated sessions route and the wildcard/ingestion
   proxy routes derive any required downstream identity from verified context. Conflicting client
   identity headers cannot override that principal, and the fake downstream captures neither the
   conflicting value nor a fabricated system actor. Exercise OpenPlanner's server-credential
   attachment and ingestion's former clone-all-headers path.
12. Mutable storage values and caller-supplied identity headers cannot change the principal
   observed by protected real-server probes through one TypeScript surface and one CLJS surface.
13. A missing/expired session on a protected browser request reaches one canonical
    401/reauthentication UI path; it never retries as a default administrator.
14. An authorized credential-derived session still performs GET/mutation requests from both helper
    families with the same payload, non-identity headers, credential inclusion, and error behavior.
15. TypeScript tests, the full frontend CLJS suite, focused backend auth and proxy tests, production build,
    server compile, and strict changed-surface lint pass with zero warnings; real-server negative
    probes confirm zero protected effect across both the direct auth and service-proxy boundaries.

## Non-goals

- Making raw browser identity headers trustworthy.
- Replacing server-side session/API-key authentication.
- Solving every protected-route nil-context bypass; #175 owns the reusable fail-closed route guard.
- Treating the generated bridge as source or hand-editing compiled assets.

## Done when

Neither production request family, the admin identity UI, nor direct wire input can synthesize or
switch a privileged Knoxx principal from defaults, caller headers, or mutable browser storage.
The server derives context only from verified credentials, no Knoxx-owned service proxy forwards
client-selected identity or combines it with a server credential, generated assets are rebuilt
from the repaired sources, protected unauthenticated requests fail closed through the canonical
server/UI path, deliberately public reads remain compatible without acquiring identity authority,
and authorized credential-derived requests remain compatible.
