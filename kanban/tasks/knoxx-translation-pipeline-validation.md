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

## Required proofs

1. A source artifact with a concrete revision reaches the provider unchanged in identity.
2. Provider selection/config resolves through Knoxx-owned
   `knoxx.backend.infra.routes.translation-config/resolved-config!`, or an injected
   resolved-config artifact produced by that existing boundary, without loading
   publication code. The proof succeeds with publication namespaces and resources absent
   while still exercising the active configuration authority.
3. A successful provider result becomes a candidate bound to that exact source revision.
4. A provider failure produces no successful candidate.
5. A retry/retranslation preserves earlier attempt evidence instead of overwriting it.
6. Current-state read-back selects a projection from history without becoming the history
   authority.
7. A deliberate break at each hop fails the test for that hop with enough context to
   identify the boundary.

## Done when

- Each transduction hop has an independently attributable test.
- The full translation candidate path runs with publication, review UI, and final renderer
  absent.
- A fake provider can prove the contract without live model infrastructure.
- Provider selection remains independently provable through the resolved translation
  configuration boundary when the publication subsystem is absent.
- Candidate history/read projection semantics agree with `knoxx-translations-event-sourced`.
- Failures distinguish source shaping, provider invocation, candidate validation,
  persistence, and read projection rather than collapsing into one pipeline failure.

## Prior art

- `knowledge-ops-translation-mt-pipeline` (done) built the original MT worker.
- `knoxx-translation-transduction-boundary` defines the semantic operation this card
  validates.
- `knoxx-evaluation-case-contracts` and `knoxx-evaluation-mcp-review-flow` deliberately
  own the review half that used to be bundled into this validation card.
