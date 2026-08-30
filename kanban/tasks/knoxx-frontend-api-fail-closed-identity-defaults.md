---
uuid: knoxx-frontend-api-fail-closed-identity-defaults
title: Fail closed instead of injecting frontend API identity defaults
status: incoming
priority: P1
points: 2
labels: tasks, 2sp, frontend, security, auth
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
The helper has since moved to `frontend/src/lib/api/core.ts` and now combines mutable storage with
optional `VITE_KNOXX_DEV_*` defaults. This card reconciles the live boundary: a production request
must never acquire authentication authority from a hard-coded, build-time, or mutable-browser
identity fallback.

## Scope

- Remove implicit privileged identity defaults from the production shared request path.
- Stop treating `knoxx_user_email`, `knoxx_org_slug`, or caller-set identity headers as proof of
  authentication for protected APIs. Credential/session cookies or an explicitly authenticated
  API-key mechanism remain the authority.
- If a deliberately supported local-development identity seam remains, isolate it behind an
  unmistakable development-only build/runtime guard that cannot enter a production bundle or
  production request.
- When no verified authentication is available, send no fabricated identity headers, surface the
  server's canonical 401/reauthentication state, and perform no privileged retry or fallback.
- Preserve caller-supplied non-identity headers, request bodies, credential inclusion, and normal
  authorized-session behavior.

## TDD / proof

1. Empty, unavailable, throwing, and malformed browser storage cannot produce user/organization
   identity headers or a privileged request fallback.
2. Production builds ignore or reject configured `VITE_KNOXX_DEV_*` identity defaults.
3. Mutable storage values and caller-supplied identity headers cannot change the authenticated
   principal observed by a protected real-server route.
4. A missing/expired session reaches one canonical 401/reauthentication UI path; it never retries
   as a default administrator.
5. An authorized credential-derived session still performs GET/mutation requests with the same
   payload and error behavior.
6. Frontend unit/integration tests, production build, and strict changed-surface lint pass with
   zero warnings; a real-server negative probe confirms no protected effect.

## Non-goals

- Making raw browser identity headers trustworthy.
- Replacing server-side session/API-key authentication.
- Solving every protected-route nil-context bypass; #175 owns the reusable fail-closed route guard.

## Done when

No production frontend request can synthesize a privileged Knoxx identity from defaults or
mutable browser storage, unauthenticated requests fail closed through the canonical server/UI
path, and authorized credential-derived requests remain compatible.
