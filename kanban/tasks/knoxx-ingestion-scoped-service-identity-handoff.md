---
uuid: knoxx-ingestion-scoped-service-identity-handoff
title: Replace ingestion identity headers with scoped service delegation
status: incoming
priority: P1
points: 8
labels: tasks, 8sp, has-parent, ingestion, auth, security, boundaries
created_at: 2026-08-30T03:58:03.000Z
category: tasks
---

# Replace ingestion identity headers with scoped service delegation

> GitHub issue: [#287](https://github.com/open-hax/knoxx/issues/287)
> Required child of: `knoxx-frontend-api-fail-closed-identity-defaults` (#2)

## Signal

The fail-closed identity repair in #2 rejects any request that combines an API key with asserted
public identity headers. Two production JVM callers still depend on exactly that combination.

`ingestion/src/kms_ingestion/translation/worker.clj` function `knoxx-headers` sends the configured
Knoxx API key with `x-knoxx-user-email` and can add a batch-specific
`x-knoxx-membership-id`. It uses those headers for translation config, segment reads, agent start,
and polling. The worker's principal resolution deliberately distinguishes explicit batch
membership, a configured system administrator, and same-organization membership, so merely
dropping the headers would erase existing per-batch authority semantics.

`ingestion/src/kms_ingestion/drivers/audio.clj` has a separate `knoxx-headers` producer. It sends
the same API-key/user-email pair for audio agent start and run polling. In addition,
`kms-ingestion.config` defaults `KNOXX_USER_EMAIL` to `system-admin@open-hax.local`, which is an
ambient identity fallback rather than authentication.

Issue #2 must close the public-wire collision, but its implementation cannot complete until these
workers have one server-verifiable authority each. This child owns that independently
implementable JVM/service migration. Board PR #286 only plans and projects the dependency; it does
not deliver the broader delegation runtime.

## Scope

1. Inventory both production `knoxx-headers` functions, their configuration readers, and every
   translation/audio Knoxx call site. Include translation config, segment reads, direct agent
   starts, run polling, legacy jobs, batch jobs, and audio analysis.
2. Remove every worker-emitted `x-knoxx-*` identity header. Remove the `KNOXX_USER_EMAIL` production
   identity input and its default `system-admin@open-hax.local`; do not rename or move that ambient
   fallback.
3. Give each ingestion deployment a dedicated machine credential whose API key authenticates only
   the server-mapped ingestion service principal. The key cannot select a membership, accept a
   companion user email, or inherit a default administrator. Missing, malformed, expired, or
   unmapped production credentials fail before any protected request.
4. Audio operations that need no delegated user run only as the admitted service principal, under
   explicit audio-analysis permissions. They send the API key as the sole credential and no
   identity assertion.
5. Treat batch membership from a row or job as a delegation request, not an asserted identity.
   Under the service API key alone, a named server broker verifies explicit delegation permission,
   resolves one active membership in the requested organization, confirms the current batch tuple
   through an authoritative batch adapter, and either refuses or mints a short-lived capability.
6. The delegation capability binds the service principal, batch id, membership id, organization id,
   allowed routes or operations, expiry, and nonce. It is audience-bound to Knoxx and cannot be
   widened by request body, query, or header data.
7. Subsequent batch work uses that capability as its sole credential: never API key plus capability
   and never capability plus identity header. The capability resolver rechecks expiry, audience,
   operation, batch state, and active membership before deriving request context.
8. Define explicit completion and replay behavior. A capability becomes unusable after expiry,
   batch completion, revocation, or its permitted call budget; a captured nonce cannot be replayed
   for another batch, membership, organization, route, or worker.
9. Preserve legacy batches only when the broker can resolve their configured service principal to a
   single explicitly delegable active membership. Ambiguous, cross-organization, or unresolved
   legacy work stays queued or fails with a typed non-effect result; it never falls back to system
   administrator.
10. Run a whole-repository identity-emitter scan over production sources, tests, generated assets,
    scripts, and operator docs. Update `scripts/verify-publication-tour.sh` and other fixtures that
    currently authenticate with public identity headers; retain such headers only in clearly named
    malicious-header probes.

## Contract / invariants

- An API key authenticates only the server-mapped ingestion service principal and cannot select a
  membership.
- A batch membership is a delegation request, not an asserted identity. Possessing a batch id or
  membership id confers no authority.
- Delegation requires explicit delegation permission, one active membership, the matching
  organization, and a current batch confirmed by the authoritative adapter.
- No request contains more than one credential authority. Broker requests use only the API key;
  delegated work uses only the issued capability.
- Public `x-knoxx-*` headers are always untrusted and never distinguish an internal worker.
- A capability grants only its bound tuple and operations. Unknown fields, blank identifiers,
  malformed claims, tuple changes, and missing issuer/audience metadata fail closed.
- Translation and audio retain their existing domain behavior, ordering, payloads, polling, and
  error handling after the authority transport changes.
- No fixed service identity, API key, or capability becomes ambient system-administrator authority.

## TDD / proof

1. RED-prove that the exact current translation and audio `knoxx-headers` producers send API key
   plus identity and that the translation producer accepts arbitrary batch membership.
2. GREEN-prove API-key-only audio and non-delegated service operations resolve exactly the configured
   service principal; identity headers, a second credential, and unmapped keys fail before policy
   lookup or downstream work.
3. Exercise capability issuance with an authorized service principal and a current active
   batch/membership/organization tuple. Capture the credential and prove its decoded/validated
   claims bind the complete tuple, audience, operations, expiry, and nonce.
4. Send an arbitrary membership with a valid service key; a swapped batch, membership, or
   organization; and an expired, replayed, forged, or cross-route capability. Every variant fails
   before policy lookup, segment read, agent start, or audio effect, with no administrator fallback.
5. Prove the valid translation path retains translation config, segment read, agent start, and
   polling behavior across explicit-membership, same-organization, and safely admitted legacy
   batches. Batch completion/revocation invalidates its capability.
6. Prove the valid audio path retains audio start and run polling under the service principal,
   including concurrency limits, timeouts, payloads, and error behavior.
7. Add a cross-process fake Knoxx server that captures every request. Valid worker traffic contains
   exactly one credential and no public identity; collision and delegation negatives produce zero
   protected effects.
8. Make the whole-repository identity-emitter scan a regression assertion. Production worker,
   frontend, self-client, proxy, and media paths contain no unauthorized public identity emitter;
   malicious-header probes remain explicit and cannot be used as helper code.
9. Run focused ingestion translation/audio tests, focused backend credential/capability tests, both
   relevant full suites, real-server probes, compile/typecheck, and strict changed-surface lint with
   zero warnings.

## Non-goals

- Treating batch, membership, organization, email, or identity headers as credentials.
- Reusing the generic API key as both service authentication and delegated user authority.
- Building a generic delegation framework for unrelated services.
- Changing translation selection, segment semantics, audio analysis behavior, or queue policy.
- Claiming that board PR #286 delivers this runtime; it records the required child work only.

## Done when

Translation and audio ingestion send no public identity headers, each protected request has exactly
one server-verifiable authority, audio runs as its service principal, translation batch authority
comes only from a narrowly bound policy-admitted capability, all confused-deputy and replay probes
produce zero protected effects, the required happy paths remain compatible, and #2 can reject the
legacy collision without breaking either worker.
