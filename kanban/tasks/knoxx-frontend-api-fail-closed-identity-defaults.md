---
uuid: knoxx-frontend-api-fail-closed-identity-defaults
title: Fail closed instead of injecting frontend API identity defaults
status: incoming
priority: P1
points: 3
labels: tasks, 3sp, frontend, security, auth
created_at: 2026-08-30T01:19:13.727Z
category: tasks
---

# Fail closed instead of injecting frontend API identity defaults

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

## Scope

- Inventory both central request families and every direct `buildKnoxxAuthHeaders` consumer:
  `frontend/src/lib/api/core.ts`, `frontend/src/cljs/knoxx/frontend/lib/api.cljs`, their TypeScript
  and CLJS callers, and the generated bridge rebuilt from source.
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
- When no verified authentication is available, send no fabricated identity headers, surface the
  server's canonical 401/reauthentication state, and perform no privileged retry or fallback.
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
4. Mutable storage values and caller-supplied identity headers cannot change the principal
   observed by protected real-server probes through one TypeScript surface and one CLJS surface.
5. A missing/expired session reaches one canonical 401/reauthentication UI path; it never retries
   as a default administrator.
6. An authorized credential-derived session still performs GET/mutation requests from both helper
   families with the same payload, non-identity headers, credential inclusion, and error behavior.
7. TypeScript tests, the full frontend CLJS suite, production build, and strict changed-surface
   lint pass with zero warnings; real-server negative probes confirm zero protected effect.

## Non-goals

- Making raw browser identity headers trustworthy.
- Replacing server-side session/API-key authentication.
- Solving every protected-route nil-context bypass; #175 owns the reusable fail-closed route guard.
- Treating the generated bridge as source or hand-editing compiled assets.

## Done when

Neither production request family nor the admin identity UI can synthesize or switch a privileged
Knoxx principal from defaults, caller headers, or mutable browser storage; generated assets are
rebuilt from those repaired sources, unauthenticated requests fail closed through the canonical
server/UI path, and authorized credential-derived requests remain compatible.
