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
9. Every agent context that exposes `save_translation` begins through a provider-neutral turn
   initiator that persists an immutable, canonically ordered non-empty collection of attempt
   members: one stable `attempt_id` and exact server-derived
   organization/document/segment/target/source request per composite the bound turn may save.
   All members share one server-derived effective organization, and the durable turn-admission
   slot is keyed only by that organization plus `turn_id`, never by a claim, member-set, or
   execution digest. The same raw `turn_id` under two organizations creates independent slots and
   records; the same organization and turn id with changed membership conflicts.
   One atomic turn admission authorizes and installs the claim, execution snapshot, and complete
   ordered member-admission map before the real provider/model session starts. Publication
   dispatch and ordinary chat use distinct provider-neutral initiator variants; ordinary chat
   requires no publication claim. Retry equality includes the exact claim variant and its stable
   variant-specific initiator facts: durable dispatch-claim identity/workflow idempotency key for
   publication, or interactive translation-start claim identity for ordinary chat. A same-tenant
   dispatch/ordinary-chat collision on one `turn_id` conflicts and never returns the wrong claim
   type; changing only the variant-specific initiator facts also conflicts. The authenticated
   session pins the claim identity/digest, one immutable `TranslationTurnExecutionSnapshot`, and
   the complete member map. Every member artifact names that same semantic
   provider/model/config/provider-policy snapshot and `provider-session-config-digest`;
   member-specific request facts and authorization-operation receipts cannot change the model
   session configuration. A denial at atomic turn admission yields no turn or session. Each real
   `save_translation` invocation selects, echoes, and
   validates its member rather than minting or adding one after provider work. One bound session
   pre-admits at least two segments and saves them in either order; both receive their own member
   artifacts/events but truthful provenance for the same configured model session. Omitting a
   member, mutating the set, mixing execution digests, or using a missing/mismatched echo appends
   no candidate. A commit-then-timeout retry of one composite is idempotent; changed same-composite
   reuse conflicts; one raw id reused across segments/targets remains independent; and distinct
   ids with equal content create distinct attempts. Store-only tests are insufficient for this
   proof. Inject failures after observation, after attestation minting, while staging each member,
   immediately before atomic commit, and after commit/before response. Every precommit failure
   exposes no claim, snapshot, member admission, or session; retry reuses only stable initiator ids
   and must perform a fresh authorized whole-turn observation. The earlier receipt is audit
   evidence, never retry authority. A postcommit retry returns the exact complete turn, and equal
   concurrent losers cannot return a different candidate. A config-straddling equal loser with the
   same claim variant and variant-specific initiator facts excludes its server-derived candidate
   execution digest from retry equality, discards it, and returns the installed winner;
   substituting the installed digest after admission conflicts. Advance current
   provider/model config and rotate authorization policy allow-to-allow or allow-to-deny at each
   barrier: outcomes are a
   complete authorized old turn, a freshly observed complete new turn, or no turn on denial—never
   mixed members or an old-receipt install. When preflight determines that member groups genuinely
   require different execution digests, it partitions them into separately identified turn claims
   before admission; that is not a same-slot retry outcome. A complete ordinary chat request follows the same
   law and saves without a dispatch claim; an incomplete target withholds the tool from that turn
   and starts no translation-bound provider/model session.
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
- Publication dispatch and ordinary-chat preflight each persist an immutable turn claim and pin
  every allowed composite attempt identity plus one coherent provider-session config snapshot as
  one authorized atomic admission before provider/session start; `save_translation` validates one
  pre-admitted member per call but cannot create, consume, reinterpret, reconfigure, or reauthorize
  another, and interactive multi-segment saves do not require a publication claim.
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
