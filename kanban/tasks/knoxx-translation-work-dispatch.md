---
category: "tasks"
labels: ["tasks", "has-parent", "translation", "publication", "ingestion", "receipts", "wave-2"]
write-id: "1787011200003-0.176845"
points: "5"
title: "Translation — dispatch gated work to ingestion"
priority: "P1"
status: "ready"
uuid: "knoxx-translation-work-dispatch"
created_at: "2026-08-22T00:00:00Z"
---

# Translation — dispatch gated work to ingestion

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

Connect the publication gate's derived translation work to the ingestion worker
and return its result as evidence. A derived work item must be dispatchable,
revision-specific, and receipt-backed instead of being a plan that remains only
in memory.

## Dependencies

The existing translation/publication gate and translation ingestion worker.
`knoxx-publication-reconciler-runtime` consumes the resulting translation facts.

## Work

- Map each admissible derived translation work item into the ingestion worker's
  input contract, carrying document identity, source and target locale, concrete
  source revision, and a stable dispatch/idempotency identity.
- Dispatch through the established worker boundary; do not reimplement
  translation or make a Knoxx publication adapter call a provider directly.
- Decode and validate the worker result into a translation receipt tied to the
  exact derived-work and source revision that produced it.
- Record failed, rejected, duplicate, and completed dispatches distinctly so the
  gate can distinguish missing work from an attempted-but-unsuccessful run.
- Reject selector revisions and stale or mismatched worker responses rather than
  allowing them to satisfy a publication gate for a moving source.

## Definition of Done

- A gated translation work item reaches the ingestion worker with a concrete
  revision and expected locale pair.
- A successful worker result returns a validated, revision-specific receipt that
  the publication gate recognizes.
- Duplicate dispatch reuses its idempotency identity and does not enqueue or
  translate twice.
- Tests prove failed, stale, selector, and mismatched-revision results cannot
  satisfy the gate or cause publication.
