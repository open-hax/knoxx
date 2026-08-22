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

## Known gaps left open

`:source-revision-superseded?` is implemented but unreachable in the current gate
flow, and the `:translation-stale` blocker it feeds is therefore inert. The
blocker is really about the revision an existing *translation* was made from,
and the gate's fact signature `[intent revision]` does not carry that. Closing
it needs a gate-level change and its own card; the reasoning is recorded on
`infra.publication-source-revision/revision-facts` rather than papered over with
a policy nobody asked for.

### An ambiguous send whose batch landed can strand its claim

If `create-translation-batch!` throws *after* the worker committed the batch,
Knoxx cannot tell which batch is its own: the batch record carries no dispatch
identifier, and garden, target locale, document, project, source language and
creation time together still do not identify the request that created it. One
unrelated actor creating a matching batch after the claim produces exactly one
candidate, and adopting it would let `recover-settled-batch!` mint a receipt for
a source revision that batch never carried.

So observation is used only to *refute* "the send did not land". It never binds.
The consequence, stated rather than hidden: such a claim stays in flight, no
later pass can bind or retry it, and that revision needs an operator. The record's
detail says so.

That is the deliberate side of the trade — fabricated evidence is worse than a
visible stranded claim — but it is a real operational gap. Closing it needs a
dispatch correlation value carried on the batch, which is a contract change in
another repository and therefore its own cross-repo card.

### The drift check cannot see what the worker read

`source-drift-refusal` compares digests of the *repository* source. The worker
fetches its input from OpenPlanner's document store, so a document already
divergent over there is translated while both observations agree. The check still
catches every case where the local source moved, which is a receipt that is
definitely wrong — but it does not establish what was translated. Closing that
needs the batch contract to return a digest of the bytes the worker read: again
another repository, again its own card.
