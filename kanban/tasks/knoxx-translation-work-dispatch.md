---
category: "tasks"
labels: ["tasks", "has-parent", "translation", "publication", "ingestion", "receipts", "wave-2"]
write-id: "1787011200003-0.176845"
points: "5"
title: "Translation — dispatch gated work to ingestion"
priority: "P1"
status: "review"
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

## Card premise corrections

Written before the code existed, three of this card's premises turned out to be
stale. Annotated rather than silently implemented around.

1. **"Map each derived work item into the ingestion worker's input contract,
   carrying ... concrete source revision, and a stable dispatch/idempotency
   identity."** The worker's contract cannot carry either.
   `law.openplanner-translation/CreateTranslationBatchRequest` is
   `{garden_id, target_lang, document_ids, source_lang, project, org_id,
   membership_id}` — no revision field, no idempotency field — and the batch
   collection belongs to another repository. The binding therefore stays
   Knoxx-side as a `DispatchRecord` keyed to the batch id the worker returns,
   and the worker is sent only what its contract admits. Rationale and the
   rejected alternative are in `law.translation-dispatch`.

2. **"Decode and validate the worker result into a translation receipt."** The
   worker never reports a translated revision, because it has no such concept.
   The output revision is therefore minted by Knoxx from the source revision,
   the target locale, and the producing batch id — which is also what makes it
   change on re-translation, so later review evidence cannot be transplanted.
   See `law.translation-dispatch/output-revision`.

3. **An unstated precondition.** `domain.publication-gate`'s
   `:current-source-revision` fact had no production provider — only test
   stubs — so an intent declaring `:source/current` resolved to nil and derived
   no work at all. Without it this card's first DoD line is unreachable, so
   `infra.publication-source-revision` supplies it as a content digest of the
   document's source file.

## Known gap left open

`:source-revision-superseded?` is implemented but unreachable in the current gate
flow, and the `:translation-stale` blocker it feeds is therefore inert. The
blocker is really about the revision an existing *translation* was made from,
and the gate's fact signature `[intent revision]` does not carry that. Closing
it needs a gate-level change and its own card; the reasoning is recorded on
`infra.publication-source-revision/revision-facts` rather than papered over with
a policy nobody asked for.
