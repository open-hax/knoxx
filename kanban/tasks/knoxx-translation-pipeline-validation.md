---
uuid: "knoxx-translation-pipeline-validation"
title: "Validate the translation transduction pipeline boundary by boundary"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "translations", "transduction", "validation"]
created_at: "2026-08-04T00:00:00Z"
points: 5
category: tasks
---
# Validate the translation transduction pipeline boundary by boundary

> Parent epic: `knoxx-transduction-provider-pipeline`
> Blocked by: `knoxx-translation-config-publication-dependency-removal` ([#273](https://github.com/open-hax/knoxx/issues/273))
> Blocked by: `knoxx-versioned-resolved-translation-config` ([#275](https://github.com/open-hax/knoxx/issues/275))
> Blocked by: `knoxx-translation-config-trusted-auth-context` ([#283](https://github.com/open-hax/knoxx/issues/283))

## Purpose

The translation path is production-critical but its old validation card mixed four
independent concerns: candidate generation, persistence/history, SME review, and final
rendering. Validate **translation as transduction** here and let the evaluation and
representation systems prove their own contracts independently.

The goal is attributable failures at each transformation boundary, not one long test that
only says "translation is broken".

## Scope

Walk the machine-translation path only through production of a durable candidate and its
provenance:

- source document/segment ingestion into a typed source artifact;
- immutable segment/revision identity and tenant scoping;
- translation operation/provider selection;
- provider invocation and result decoding;
- translation-specific validation such as `assert-translated!`;
- candidate artifact + transduction provenance/receipt;
- append-only persistence/current projection boundary (`knoxx-translations-event-sourced`);
- read-back of the candidate through the translation data boundary used by consumers.

Explicitly **out of scope here**:

- `pending` / `in_review` / `approved` / `rejected` review workflow semantics — owned by
  `knoxx-evaluation-review-system`;
- dedicated translation review UI behavior;
- publication admission — owned by publication law consuming receipts;
- HTML/React/static rendering — owned by representation providers.

## Approach

- Prefer contract tests at each boundary over one monolithic E2E script.
- Use a fake transduction provider so transport/model behavior can be tested separately
  from the semantic operation contract.
- Use immutable source/candidate identities in fixtures so stale evidence and accidental
  overwrite bugs are observable.
- Extend/reuse current translation laws where they still describe the right semantics;
  do not preserve OpenPlanner-specific vocabulary merely because it exists today.
- Complete blocking card `knoxx-translation-config-publication-dependency-removal` (#273)
  before proof 2. It owns the namespace-graph repair, behavior/error parity, and the existing
  adapter's publication-absent integration proof; this card consumes that evidence rather than
  hiding an implementation prerequisite inside its approach notes.
- Complete `knoxx-versioned-resolved-translation-config` (#275) before proof 2. It evolves the
  same adapter to emit one authenticated artifact naming the exact global and optional
  organization-override resource revisions; this card consumes that artifact rather than
  reconstructing provenance after provider invocation.
- Complete `knoxx-translation-config-trusted-auth-context` (#283) before proof 2. It prevents
  externally supplied identity headers from selecting config scope and supplies trusted
  session/API-key context.

## Required proofs

1. A source artifact with a concrete revision reaches the provider unchanged in identity.
2. Provider selection/config resolves through Knoxx-owned
   `knoxx.backend.infra.routes.translation-config/admit-resolved-config-for-attempt!`, the
   attempt operation added to the existing facade by #275, or an injected artifact produced by
   that operation, without loading publication code. The read-only `EffectiveConfigView` from
   the GET route is explicitly rejected here. After all three prerequisites above, the proof
   succeeds with every publication-owned law, runtime/orchestration, route, store, and resource
   namespace absent while still exercising the active configuration authority. The resolved artifact is
   bound to the same server-derived effective organization and immutable global plus optional
   organization override config/policy revisions as the source. Ordinary actors cannot target
   another organization; authorized system-admin delegation binds the target plus actor
   evidence end to end. Cross-tenant, client-header-selected, or fabricated config artifacts
   fail before provider invocation without an existence/value leak.
3. A successful provider result becomes a candidate bound to that exact source revision.
4. A provider failure produces no successful candidate.
5. A retry/retranslation preserves earlier attempt evidence instead of overwriting it.
6. Current-state read-back selects a projection from history without becoming the history
   authority.
7. A deliberate break at each hop fails the test for that hop with enough context to
   identify the boundary.
8. Provider-returned tenant/source identity drift is rejected, while the successful
   candidate receipt preserves the exact config/policy revision, canonical request
   parameters, provider/model identity, and raw-result evidence digest.
9. The real `save_translation` MCP schema and handler carry a caller-stable `attempt_id`, while
   server-derived organization/document/segment/target coordinates form the identical composite
   identity at config and event admission. A commit-then-timeout retry of one composite is
   idempotent; changed same-composite reuse conflicts; one raw id reused across segments/targets
   remains independent; and distinct ids with equal content create distinct attempts. Store-only
   tests are insufficient for this proof. Inject config-admission crashes after observation,
   after attestation minting, before complete install, and after install/before response:
   pre-install crashes leave no reservation or provider call, post-install retry returns the
   installed artifact, and equal concurrent losers cannot return a different resolved snapshot.
10. Real GET/PATCH config routes derive scope only from a verified session or configured API
    key. A valid organization-A session plus headers naming an existing organization-B
    membership cannot read B, and a header-only request reaches no repository operation.

## Done when

- Each transduction hop has an independently attributable test.
- The full translation candidate path runs with publication, review UI, and final renderer
  absent.
- A fake provider can prove the contract without live model infrastructure.
- Provider selection remains independently provable through the resolved translation
  configuration boundary when the publication subsystem is absent; transitive namespace
  closure checks fail if the law, domain, or existing adapter regains any publication-owned
  dependency.
- The resolved-config artifact carries both contributing resource revisions and its
  deterministic identity through provider invocation and durable attempt evidence; a later
  config change cannot rewrite which policy selected the candidate.
- The real boundary proves complete config admission and event history agree on one composite attempt
  identity, including cross-segment/target raw-id reuse and same-composite conflict behavior.
- The config boundary atomically installs a complete attempt/artifact record; injected crashes
  and equal races cannot strand a reservation or let provider/event admission consume a losing
  candidate artifact.
- Candidate history/read projection semantics agree with `knoxx-translations-event-sourced`.
- The public save boundary preserves caller idempotency identity end to end instead of minting
  per-call ids or collapsing intentional equal-content attempts.
- The history proof injects append-success/projection-failure and out-of-order delivery,
  verifies monotonic per-key ordinals, then proves idempotent byte-equivalent replay and
  migration restart.
- Failures distinguish source shaping, provider invocation, candidate validation,
  persistence, and read projection rather than collapsing into one pipeline failure.

## Prior art

- `knowledge-ops-translation-mt-pipeline` (done) built the original MT worker.
- `knoxx-translation-transduction-boundary` defines the semantic operation this card
  validates.
- `knoxx-evaluation-case-contracts` and `knoxx-evaluation-mcp-review-flow` deliberately
  own the review half that used to be bundled into this validation card.
