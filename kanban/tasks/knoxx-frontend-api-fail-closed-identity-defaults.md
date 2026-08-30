---
uuid: knoxx-frontend-api-fail-closed-identity-defaults
title: Fail closed instead of trusting browser-supplied API identity
status: incoming
priority: P1
points: 8
labels: tasks, 8sp, frontend, backend, security, auth
created_at: 2026-08-30T01:19:13.727Z
category: tasks
---

# Fail closed instead of trusting browser-supplied API identity

> GitHub issue: [#2](https://github.com/open-hax/knoxx/issues/2)
> Complements: `knoxx-tenant-fail-closed-route-guard` (#175; duplicate intake #9 is closed) and
> `knoxx-translation-config-trusted-auth-context` (#283)
> Required child: `knoxx-ingestion-scoped-service-identity-handoff` (#287)

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

Internal Knoxx self-calls expose the same collision from the other direction.
`infra.clients.knoxx-control/headers-for` attaches the hard-coded
`system-admin@open-hax.local` identity and, when configured, an `X-API-Key`; its event, actor, and
Discord voice callers use that client for protected control-plane and ingestion operations. Once
the server admits exactly one credential authority, the current pair is either rejected as an
API-key/identity collision or preserves a fixed administrator escape hatch. A repaired self-client
must authenticate with one server-verifiable credential and let the server derive its configured
principal.

`extern.agent-turn-media/auth-header-map` presents a second internal handoff: it converts an
already-resolved auth context back into ordinary `x-knoxx-*` wire headers for local Knoxx media
fetches made by `fetch-data-url-with-fetch!` through `infra.agent.turn/fetch-media-data-url!`.
Rejecting public identity headers without addressing that path breaks legitimate authenticated
self-fetches; exempting indistinguishable headers reopens spoofing. The internal handoff therefore
needs a named server-owned boundary rather than a special case for the public header names.

A whole-repository emitter scan also finds two JVM machine clients outside the backend tree.
`ingestion/src/kms_ingestion/translation/worker.clj` and
`ingestion/src/kms_ingestion/drivers/audio.clj` both send an API key with
`x-knoxx-user-email`; the translation worker can add batch-specific
`x-knoxx-membership-id`. Rejecting the collision immediately would break both workers, while
dropping the batch header without a replacement would erase legitimate per-batch membership
semantics. Required child #287 owns the scoped service/delegation migration. This card owns the
eventual rejection and cannot complete until that projected child is green.

## Scope

- Inventory both central request families and every direct `buildKnoxxAuthHeaders` consumer:
  `frontend/src/lib/api/core.ts`, `frontend/src/cljs/knoxx/frontend/lib/api.cljs`, their TypeScript
  and CLJS callers, and the generated bridge rebuilt from source.
- Repair the server authentication boundary in `backend/src/cljs/knoxx/backend/infra/auth/`:
  inventory `authz/resolve-request-context!`, `session/create-session-hook`,
  `session/resolve-auth-context`, and every path that converts request headers into a policy
  context. On every protected or credential-required route, any public `x-knoxx-*` identity header
  combined with session-cookie or API-key material is a credential collision and is rejected before
  context resolution, policy lookup, downstream I/O, or another protected effect. There is no
  strip-and-continue outcome, even when the header happens to name the credential-derived principal.
  Identity-header-only protected requests remain unauthenticated. A deliberately credentialless
  public read may ignore or remove those bytes only as non-authorizing input; it cannot derive a
  principal, membership, organization, or privileged downstream request from them. Any internal
  identity handoff must be distinguishable from untrusted wire input and derived from an
  already-verified session, API key, or typed bootstrap credential. Inventory every internal
  header-shaped `policy-db/resolve-context!` caller, including `infra/routes/auth.cljs`
  `signup-handler!`, `local-login-handler!`, and `invite-redeem-handler!`, plus
  `infra/auth/session.cljs` OAuth membership synchronization. Replace them with one named internal
  adapter over closed typed variants for the newly created membership, verified local-password
  record, verified invite redemption, and synchronized OAuth membership. Invite redemption never
  sources its email from `x-knoxx-user-email`; its untrusted code/body selectors must be verified
  together before the typed result exists. The adapter cannot consume request identity headers,
  and invalid bootstrap evidence cannot resolve context or create a session.
- Inventory `infra.clients.knoxx-control/headers-for` and its event, actor, and Discord voice
  callers. Protected control requests send the configured API key without any `x-knoxx-*` identity
  header; the server derives that credential's principal only from `KNOXX_API_KEY_USER_EMAIL` and
  the canonical membership resolver. Remove the hard-coded self-client administrator identity.
- Missing, malformed, or identity-less production API-key configuration fails before any protected
  self-request or effect. Deliberately public internal reads may remain credentialless only under
  the same non-authorizing public-read rule as an external caller; no fixed identity is a fallback.
- Inventory `extern.agent-turn-media/auth-header-map`, `fetch-data-url-with-fetch!`, and
  `infra.agent.turn/fetch-media-data-url!`. A local authenticated media self-fetch must not
  serialize verified context back into indistinguishable public `x-knoxx-*` headers. Perform the
  operation directly under the admitted context or use a named server-owned internal fetch adapter
  whose credential or capability is bound to the already-verified context and accepted only on the
  loopback/internal boundary. External media URLs receive no Knoxx credential, capability, or
  identity header.
- Treat `knoxx-ingestion-scoped-service-identity-handoff` (#287) as a blocking implementation
  dependency. #287 inventories both JVM `knoxx-headers` producers, removes the ambient
  `KNOXX_USER_EMAIL` identity default, moves audio to its server-mapped service principal, and
  replaces translation's batch-membership header with a policy-admitted scoped capability. This
  card rejects the legacy API-key/identity combination only with that compatibility path in place.
- Complete a credentialed service proxy inventory, beginning with
  `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` functions
  `openplanner-proxy-handler!` and `register-ingestion-service-proxy-route!`, plus
  `register-openplanner-proxy-routes!` and its dedicated `GET /api/openplanner/v1/sessions` call to
  `openplanner-client/sessions!`, plus
  the separately registered `infra/routes/app.cljs` handlers under `/api/data/op/*`,
  `/api/data/mongo/collections`, `/api/data/mongo/list`, `/api/data/mongo/query`, and
  `/api/data/jobs/build-semantic-edges`, plus
  `infra/routes/voice.cljs` `GET /api/voice/tts/health` and `POST /api/voice/tts`, plus
  every bearer-backed `infra/routes/translation.cljs` `/api/translations/*` registration, plus
  every `infra/routes/memory.cljs` `/api/memory/*` registration that can read or mutate
  OpenPlanner-backed session rows, including session list/detail and title status/backfill/import,
  plus
  `infra/routes/models.cljs` `GET /api/proxx/models`, `POST /api/proxx/chat`, `GET /api/models`,
  and every other Proxx bearer-backed health/observability registration, plus
  `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs`. Include every Knoxx route that
  accepts inbound data or headers and then calls a downstream service with Knoxx-held credentials;
  record which headers are constructed locally, copied, or forwarded.
- At every such proxy boundary, construct outbound headers from an explicit non-authority
  allowlist; never clone the inbound header object. Drop `cookie`, `authorization`, `x-api-key`,
  every `x-knoxx-*` identity header, and every caller-supplied credential or capability before
  constructing the downstream request. Add a required downstream service credential only from
  Knoxx server configuration after caller authorization. If the downstream protocol genuinely
  requires identity, rederive it from the verified server request context and pass it through a
  named internal service adapter; never reuse client bytes or let an absent context become a
  system actor.
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
- Define credential precedence at the server: a valid session or API-key credential derives the
  principal only when no public identity header is present. On a protected or credential-required
  route, adding any public identity header makes that otherwise valid request an invalid collision;
  the server rejects it rather than preserving the credential-derived principal. Requests with only
  identity headers are unauthenticated, and credentialless deliberately public reads treat the
  headers as non-authorizing input.
- Valid session and API-key credentials cannot be overridden by conflicting client identity headers:
  the no-header credential path derives its configured principal, while every identity-header
  collision is rejected before context rather than selecting either value.
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
5. Send each public identity header, both singly and in combinations, alongside a valid session and
   alongside a valid API key on protected routes. Every collision fails before context resolution,
   policy lookup, downstream request, or protected effect; there is no strip-and-continue case even
   when the header value equals the credential-derived principal. Removing every identity header
   restores the one credential's normal principal. Header-only protected requests remain
   unauthenticated, while a credentialless deliberately public read proves the same bytes are
   non-authorizing and cannot select downstream identity.
6. Add simultaneous-credential probes for the same principal, different principals, and one
   malformed credential. Every session-cookie/API-key collision fails before context resolution,
   policy lookup, or protected effect; removing one credential restores that credential's normal
   authenticated behavior without fallback from a rejected collision.
7. Add self-control and local-media negative probes for absent/malformed credentials, missing API-key
   identity configuration, fabricated identity headers, and an unverified context. Downstream
   request and protected-effect counts remain zero; no fixed administrator fallback is attempted.
8. Valid self-control calls contain one configured API key and no identity header, resolve the
   `KNOXX_API_KEY_USER_EMAIL` principal at the server, and preserve normal event, actor, Discord
   voice, and admitted ingestion behavior without a credential collision.
9. Valid local media self-fetches preserve the already-verified principal through the named
   internal boundary without public identity headers. External media URLs receive no Knoxx
   credential, capability, or identity header, and an unverified local protected fetch fails before
   network or file disclosure.
   End-to-end signup, local-password login, invite redemption, and OAuth callback probes each pass
   only their closed typed bootstrap variant to the named context adapter and create the expected
   session. Captured public identity headers cannot alter any resulting context. Invite fixtures
   prove a header-only email is rejected and code/body mismatch cannot create typed evidence.
   Invalid password, invite, OAuth state, or malformed bootstrap evidence produces zero
   context-resolution and session-creation calls.
10. Direct unauthenticated OpenPlanner wildcard and `/api/ingestion/*` probes send malicious identity
   headers through representative read and protected mutation routes. Before authorization, the
   downstream call count remains zero and no protected mutation occurs; where an unauthenticated
   read is deliberately public, its captured outbound request contains no spoofed identity. For
   every such public read, also capture all outbound credential headers and the returned response
   scope: the request carries no Knoxx-held downstream credential unless the adapter names and tests
   an explicitly public downstream operation whose response is limited to that public scope. A
   server bearer credential, API key, capability, or privileged response scope fails the probe even
   when no spoofed identity header is present.
   For `/api/ingestion/*`, an authenticated caller probe captures the downstream headers and proves
   `cookie`, `authorization`, `x-api-key`, every `x-knoxx-*` header, and any caller capability are
   absent. Only explicit non-authority headers plus the server-selected, operation-scoped KMS
   service credential may cross after caller authorization; the proxy never clones inbound
   headers.
   Run the same no-context and identity-header-only probes directly against every active
   `/api/data/op/*`, `/api/data/mongo/{collections,list,query}`, and
   `/api/data/jobs/build-semantic-edges` registration in `infra/routes/app.cljs`; each must reject
   before `openplanner-client` can attach the server bearer credential, with zero downstream reads,
   queries, jobs, or mutations.
   Direct no-context probes also cover `GET /api/voice/tts/health`, `POST /api/voice/tts`, and every
   `/api/translations/*` registration. A nil context—including when the policy database is
   disabled—must reject before the Voice Gateway API key or OpenPlanner bearer credential is
   attached, with zero downstream voice, translation read, or translation mutation calls.
   Apply the same generated route-table probe to every credential-reachable `/api/memory/*`
   registration, including session list/detail and title status/backfill/import; nil context has
   zero OpenPlanner session-row reads, title jobs, imports, or mutations. The gate derives this
   inventory from registered handlers and their server-credential call graph, then fails if any
   credential-reachable route lacks a direct negative probe.
   Direct no-context probes cover every Proxx bearer-backed registration, including
   `GET /api/proxx/models`, `POST /api/proxx/chat`, and `GET /api/models`; each rejects before
   `proxx-client/headers-for` can attach the server token, with zero paid chat, model, health, or
   observability calls.
11. A direct `GET /api/openplanner/v1/sessions` negative probe has no valid Knoxx credential and may
   include conflicting identity headers. The `openplanner-client/sessions!` call count remains zero,
   no session rows or enrichment data are disclosed, and the canonical protected-request failure is
   returned before the server bearer credential can be used.
12. Add direct unauthenticated and identity-header-only probes across all six admin routes in
   `/api/admin/eta-mu-sessions{,/status,/ingest}` and
   `/api/admin/opencode-sessions{,/status,/ingest}`. Local list/status and downstream fetch/job call
   counts remain zero; no session data, source/job data, or mutation result is disclosed.
13. Valid administrator session and API-key probes with no public identity headers can perform each
    admitted admin operation; authenticated non-admin is denied before local/downstream work, and
    conflicting client identity headers cannot override the credential-derived administrator because
    adding any such header rejects the otherwise valid request before context. Capture the KMS request
    to prove any internal system identity is added only after that authorization fence.
14. Valid session and API-key probes for the dedicated sessions route and the wildcard/ingestion
    proxy routes derive any required downstream identity from verified context only when no public
    identity header is present. Conflicting client identity headers cannot override that principal:
    adding any such header rejects the protected request before the proxy, and the fake downstream
    captures neither the conflicting value nor a fabricated system actor. Exercise OpenPlanner's
    server-credential attachment and ingestion's former clone-all-headers path.
15. Mutable storage values and caller-supplied identity headers cannot change the principal
    observed by protected real-server probes through one TypeScript surface and one CLJS surface.
16. A missing/expired session on a protected browser request reaches one canonical
    401/reauthentication UI path; it never retries as a default administrator.
17. An authorized credential-derived session still performs GET/mutation requests from both helper
    families with the same payload, non-identity headers, credential inclusion, and error behavior.
18. TypeScript tests, the full frontend CLJS suite, focused backend auth and proxy tests, production build,
    server compile, and strict changed-surface lint pass with zero warnings; real-server negative
    probes confirm zero protected effect across both the direct auth and service-proxy boundaries.
19. Before this card is done, run #287's real cross-process worker contract. Legacy ingestion
    API-key/identity combinations fail before context or effect, while the replacement translation
    and audio paths preserve their admitted authority and behavior with one credential per request.

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
internal self-calls neither collide credentials nor round-trip verified context into public identity
headers, required child #287 has removed the ingestion collision without losing batch membership
semantics, and authorized credential-derived requests remain compatible.
