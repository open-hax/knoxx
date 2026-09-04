---
uuid: "knoxx-translation-config-trusted-auth-context"
title: "Bind translation-config scope to trusted authentication context"
status: incoming
priority: P1
labels: tasks, 5sp, has-parent, translations, config, security, auth
created_at: "2026-08-29T19:24:51Z"
points: 5
category: tasks
---
# Bind translation-config scope to trusted authentication context

> Parent epic: `knoxx-transduction-provider-pipeline`
> GitHub issue: [#283](https://github.com/open-hax/knoxx/issues/283)
> Blocks: `knoxx-versioned-resolved-translation-config` (#275)
> Complements: `knoxx-tenant-fail-closed-route-guard` and
> `knoxx-ingestion-scoped-service-identity-handoff` (#287)

## Purpose

`authz/resolve-request-context!` currently takes a header-first path when external requests
contain `x-knoxx-user-email` or `x-knoxx-membership-id`. The config GET then resolves values
for that selected membership. Rejecting a nil context does not help when caller identity
headers can manufacture a non-nil context for another real tenant.

Bind translation-config scope to one typed, verified authority before permission checks or
repository access. Ordinary membership-bearing API keys retain their server-configured member
principal. The ingestion credential from #287 is instead a broker-only, membershipless service
principal: it cannot read the repository directly. After carrier/source-manifest verification, a
typed `ProposalSelectionCapability` may authorize exactly one model-selection observation; after
translation-manifest admission, the distinct bound delegated capability may authorize its read-only
translation-config GET.

## Contract

- Resolve principal, membership, roles, and organization from a verified session token or a typed
  server-configured membership-bearing API-key identity. Those ordinary member API keys retain
  their configured membership and existing admitted GET/PATCH permissions.
- Type the #287 ingestion API key separately as a broker-only, membershipless service principal.
  The raw ingestion API key can call only the named delegation broker; it cannot reach the config
  repository, select a membership, perform translation-config GET, or perform PATCH.
- For a batch capability, the broker verifies active membership, explicit delegation permission,
  matching organization, the current batch manifest, and its authority epoch. For a legacy-job
  capability, membership is absent: the broker instead verifies service-principal job permission,
  matching organization, the current job manifest, and its authority epoch. A job request or
  capability carrying membership fails as the wrong carrier type rather than borrowing batch
  authority. After that carrier-specific admission and `SourceManifest` install, the broker may mint
  a sender-constrained `ProposalSelectionCapability` bound to the exact carrier tuple, source
  manifest id/digest, authority epoch, audience, expiry, and workload key. Its closed config
  operation is only `translation-config:model-select`: server preflight uses it for one repository
  observation and exposes only immutable config resource/version plus catalog model id to the
  launcher, never the config payload or reusable GET authority. After translation-manifest
  admission, the distinct bound capability becomes the sole request authority. Its read-only config
  scope exact-matches the admitted organization,
  project, garden, source/target language, and manifest; it may authorize translation-config GET
  and never PATCH. API key plus capability is a collision, not an alternate path.
- External email, membership, organization-id, and organization-slug headers are never
  authentication authority. Session hydration carries trusted context out of band on the
  request; it does not rewrite caller-visible headers and then trust them.
- Cache only the verified context and its authentication method/credential identity.
- Missing, expired, forged, revoked, or unconfigured credentials fail closed before permission
  checks, config resolution, mutation, or existence/value disclosure.
- A valid membership-bearing API key binds to its server-configured membership only when no public
  identity header is present. Any public identity header combined with session or API-key material
  on the protected config routes is rejected before context; there is no strip-and-continue case.
- A valid broker-only ingestion key binds only its membershipless service principal. It never
  inherits the ordinary member-key contract, and a capability-derived delegated config context
  never acquires mutation permission.
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
3. A configured membership-bearing API key with no public identity header resolves its configured
   member identity. Adding any public identity header rejects it before context or repository I/O;
   invalid/unconfigured keys fail closed too.
4. Missing, expired, revoked, and forged sessions return the canonical non-enumerating
   authentication result and neither read nor rotate config authority.
5. A raw #287 ingestion API key can reach only the broker and performs zero config repository I/O.
   Before carrier and `SourceManifest` admission, no proposal-selection authority exists. A valid
   sender-constrained `ProposalSelectionCapability` requires the verified carrier tuple, source
   manifest, epoch, audience, expiry, and workload key, and can perform exactly one
   `translation-config:model-select` observation that returns no config payload. Swap each fact,
   replay it, request general GET/PATCH, or invoke it after selection drift and prove failure before
   repository I/O or disclosure. A valid sender-constrained batch capability requires the current
   active membership, delegation
   permission, organization, manifest, and epoch. Swap each fact and prove failure before I/O.
6. A valid sender-constrained legacy-job capability requires membership absent plus the service
   principal's explicit job permission, organization, current job manifest, and epoch. Adding a
   membership claim or substituting permission/organization/manifest/epoch fails before I/O. Each
   admitted carrier can GET exactly its delegated read-only view as sole authority;
   API-key-plus-capability fails too.
7. Capability-authorized PATCH always fails before repository mutation, including a capability
   issued to a service principal whose deployment credential could call the broker. Ordinary
   authorized platform PATCH with a membership-bearing credential still works; an organization
   administrator remains denied.
8. Authorized GET still returns `EffectiveConfigView` without an attempt/write, and the delegated
   GET preserves the same response/config precedence while binding its operation receipt to the
   capability and manifest scope.
9. Unit/route integration tests and real-server E2E prove the boundary; backend compile,
   coverage, and the complete auth suite pass.

## Non-goals

- Blessing external identity headers.
- Replacing session/API-key storage or OAuth.
- Turning the broker-only ingestion key into a membership-bearing key or reusable config token.
- Allowing a delegated capability to authorize config mutation.
- Changing config values, precedence, snapshot semantics, or provider admission.
- Fixing every protected route in this card; the reusable trusted-context seam may be adopted
  elsewhere through separately reviewed work.

## Done when

- Translation-config repository access always has one typed verified authority: a credential-derived
  member scope, #287's proposal-selection scope after carrier/source-manifest admission, or #287's
  distinct bound delegated read scope after translation-manifest admission.
- Cross-tenant header selection is impossible and non-enumerating.
- Session/API-key/capability provenance remains attributable through permission and operation
  receipts, without treating the raw ingestion key as repository authority.
- GET/PATCH behavior and permissions remain compatible for authorized callers.
- Exact unit, integration, E2E, coverage, and auth regression evidence is retained.
