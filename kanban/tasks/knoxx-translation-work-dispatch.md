---
uuid: knoxx-translation-work-dispatch
title: Translation — dispatch the gate's derived work and record its receipt
status: ready
priority: P1
points: 5
labels:
  - tasks
  - translations
  - publication
  - ingestion
  - has-parent
---

# Translation — dispatch the gate's derived work and record its receipt

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

`domain.publication-gate` computes translation work for a resolved concrete
revision, and nothing consumes it. The JVM ingestion worker at
`ingestion/src/kms_ingestion/translation/worker.clj` has no publication
awareness — a repo-wide search for "publication" under `ingestion/src` returns
nothing. So a publication blocks on translation evidence that no process is
producing, forever.

## Dependencies

`knoxx-publication-stack-relink` (the gate is in the stranded chain). Independent
of the target adapter — a translated document with nowhere to publish is still
progress.

## Work

- Carry the concrete revision through dispatch untouched. The gate resolves
  `:source/current` exactly once and the resolved revision is the identity every
  downstream step must use; re-resolving anywhere below is the defect
  `one-evidence-result-supplies-every-consumer` was written to catch.
- Dispatch to the worker over the existing seam. The worker already reads the
  authoritative model from the translation-config resource after
  `knoxx-translation-pipeline-config-resource`; do not introduce a second
  authority for model selection.
- Record the outcome as a receipt keyed by document × locale × concrete revision,
  the same identity the gate looks evidence up by. A receipt the gate cannot find
  is not evidence.
- Dispatch is idempotent per identity: re-running with work already in flight or
  already complete must not queue a second translation.
- Model failure explicitly. A worker error is a typed failure receipt, not an
  absent success — absence is indistinguishable from never-dispatched, and the
  reconciler will re-dispatch forever.
- Keep operational state out of resources. `:translating`, `:worker-failed`, job
  ids, and timestamps are receipt facts. This is a stated non-goal of the parent
  publication epic and stays one.
- A superseding source revision does not cancel in-flight work; it makes the
  result stop satisfying the new revision. Do not build cancellation.

## Definition of Done

- A blocked publication produces dispatched translation work for the concrete
  revision the gate resolved.
- A completed translation writes a receipt the gate finds, and the blocker clears.
- Re-dispatch for the same identity is a no-op.
- A worker failure is a typed receipt, visible, and retryable without duplicating.
- No workflow state appears in any resource.
- The ingestion worker still has exactly one model authority.
