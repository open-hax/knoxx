---
uuid: "knoxx-translation-config-trusted-auth-context"
title: "Bind translation-config scope to trusted authentication context"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "translations", "config", "security", "auth"]
created_at: "2026-08-29T19:24:51Z"
points: 5
category: tasks
---
# Bind translation-config scope to trusted authentication context

> Parent epic: `knoxx-transduction-provider-pipeline`
> GitHub issue: [#283](https://github.com/open-hax/knoxx/issues/283)
> Blocks: `knoxx-versioned-resolved-translation-config` (#275)
> Complements: `knoxx-tenant-fail-closed-route-guard`

## Purpose

`authz/resolve-request-context!` currently takes a header-first path when external requests
contain `x-knoxx-user-email` or `x-knoxx-membership-id`. The config GET then resolves values
for that selected membership. Rejecting a nil context does not help when caller identity
headers can manufacture a non-nil context for another real tenant.

Bind translation-config GET/PATCH scope only to a verified Knoxx session or configured API-key
identity before permission checks or repository access.

## Contract

- Resolve principal, membership, roles, and organization from a verified session token or
  server-configured API-key identity.
- External email, membership, organization-id, and organization-slug headers are never
  authentication authority. Session hydration carries trusted context out of band on the
  request; it does not rewrite caller-visible headers and then trust them.
- Cache only the verified context and its authentication method/credential identity.
- Missing, expired, forged, revoked, or unconfigured credentials fail closed before permission
  checks, config resolution, mutation, or existence/value disclosure.
- A valid API key binds to its server-configured membership regardless of identity headers.
- Preserve the existing read permission, platform-only global mutation permission, wire
  response, and one config facade/resolver. This task changes authentication provenance, not
  precedence or repository semantics.
- Any future internal-gateway identity assertion is a separate cryptographically attributable
  mechanism. Raw identity headers do not become trusted merely because an internal client
  sends them.

## TDD / proof

1. A valid organization-A session plus headers naming an existing organization-B membership,
   email, id, and slug resolves A only or rejects; it never reads B.
2. Header-only requests using real membership/email values return the same authentication
   failure as invented values and invoke no repository adapter.
3. A configured API key resolves its configured identity despite conflicting caller headers;
   invalid/unconfigured keys fail closed.
4. Missing, expired, revoked, and forged sessions return the canonical non-enumerating
   authentication result and neither read nor rotate config authority.
5. Authorized GET still returns `EffectiveConfigView` without an attempt/write. Authorized
   platform PATCH still works; an organization administrator remains denied.
6. Unit/route integration tests and real-server E2E prove the boundary; backend compile,
   coverage, and the complete auth suite pass.

## Non-goals

- Blessing external identity headers.
- Replacing session/API-key storage or OAuth.
- Changing config values, precedence, snapshot semantics, or provider admission.
- Fixing every protected route in this card; the reusable trusted-context seam may be adopted
  elsewhere through separately reviewed work.

## Done when

- Translation-config repository access always has credential-derived scope.
- Cross-tenant header selection is impossible and non-enumerating.
- Session/API-key provenance remains attributable through permission and operation receipts.
- GET/PATCH behavior and permissions remain compatible for authorized callers.
- Exact unit, integration, E2E, coverage, and auth regression evidence is retained.
