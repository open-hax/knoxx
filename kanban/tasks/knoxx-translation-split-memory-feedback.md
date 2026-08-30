---
uuid: "knoxx-translation-split-memory-feedback"
title: "Persist revision-bound translation splits and close the correction-memory loop"
status: breakdown
priority: P0
labels: ["tasks", "8sp", "has-parent", "regression", "translations", "segments", "memory", "evaluation", "publication"]
created_at: "2026-08-30T00:00:00Z"
points: 8
category: tasks
---

# Persist revision-bound translation splits and close the correction-memory loop

> Parent epic: `knoxx-evaluation-review-system`
> Product contract: `knowledge-ops-translation-document-review-v2`
> Coordinates with: `knoxx-translations-event-sourced`

## Purpose

Bridge resource-derived publication work to the restored translation workspace without
collapsing an agent candidate into one read-only file. Persist the ordered split set for each
concrete source/candidate revision, retain immutable candidate and correction history, compose
the reviewed document deterministically, and make approved corrected pairs available to later
translation attempts.

The old review data shape is retained. The repair adds the revision, attempt, tenant, and
composition identity the newer CMS/publication laws require.

## Ownership

- Resource/CMS contracts own desired documents, gardens, locales, source revisions, and
  publication intent.
- This P0 slice owns the minimal immutable manifest, pre-provider claim, candidate-member, and
  complete candidate-set history required to restore the product safely, plus deterministic
  composition, the candidate-bound translation review receipt required by this vertical slice,
  and the translation-specific projection into retrievable memory.
- `knoxx-translations-event-sourced` later generalizes that history across the transduction
  pipeline; its incoming P2 status does not block this vertical slice and it must consume these
  identities rather than introduce a second attempt model.
- The later generic evaluation contract adapts this minimal canonical translation receipt; it is
  not a predecessor for restoring the P0 workflow or a second source of review truth.
- Publication owns the separate approval and materialization receipt for the composed effective
  target revision.

## Canonical identities

Before provider invocation, one atomic translation turn claim binds:

- server-derived organization/project;
- document, garden, source locale, target locale, and exact source revision;
- one split-manifest identity and ordered member vector;
- stable split identity, ordinal/source span or reconstruction data, source digest, and
  workflow-stable attempt id for every member;
- provider/configuration snapshot and the exact retrieved translation-memory references.

The split manifest is immutable. A source change or re-segmentation produces a new manifest;
old candidates, reviews, and corrections remain history and cannot attach to the new members.

## Save and completion laws

- `save_translation` for a contract-backed turn names one pre-admitted split member and its
  stable attempt id; the tool cannot mint or reinterpret either at save time.
- Members may arrive out of order. Saving one member persists a canonical candidate event but
  does not settle the document dispatch.
- Equal retries are idempotent. Reusing an attempt id with changed canonical content conflicts
  and preserves the first event.
- Only a complete admitted candidate set can be composed, digested, and passed through the
  existing source-drift/completion path.
- The completed translation receipt names the split manifest, complete candidate-set identity,
  count, digest, and composed target revision.
- Current Mongo rows and UI status are disposable projections; immutable candidate/review
  evidence is semantic authority.

## Review, correction, and publication laws

- Review reads the exact candidate set named by the completed receipt, never mutable "current
  segments" selected only by document and locale.
- A correction appends version-bound evidence and never overwrites the original candidate.
- Effective split target = approved correction when present, otherwise approved candidate.
- Publication composition follows split-manifest order and includes only the exact effective
  split revisions admitted by review.
- Publication approval binds organization, document, garden, locales, source revision, split
  manifest, candidate/effective-set digest, and composed target revision.
- Changing source, split manifest, correction set, or composed target invalidates prior
  publication approval mechanically.

## Translation-memory feedback law

- An approved effective split projects one positive translation example containing organization,
  project, garden/domain, language pair, source/split digest, source text, effective target text,
  candidate/correction refs, evaluation receipt, and provenance.
- Corrected approval uses corrected target text. Rejected, pending, stale, or superseded
  candidates never enter positive memory.
- Before a later provider session starts, applicable examples are retrieved deterministically,
  tenant-scoped, filtered by language pair, ranked by garden/domain/terminology relevance, and
  pinned by id/version/digest in the turn snapshot and prompt.
- Memory absence or failure is explicit in the attempt receipt. Knoxx never claims prior
  corrections influenced a translation when retrieval did not run.
- Restore the historical feedback loop, not its unscoped regex lookup: cross-tenant,
  wrong-language, stale, and superseded examples are excluded by tests.

## RED-first vertical slice

First preserve resource cardinality: 18 desired resource-backed document/locale pairs with only
one completed translation still project to 18 inventory rows. Candidate storage is evidence about
work; it is never the source of which work exists.

Seed one resource-backed document with two stable splits.

1. Dispatch pins both members and their stable attempt ids before the agent session starts.
2. Save split 1: one durable candidate event exists; no completed document receipt exists.
3. Save split 0 out of order: one complete candidate-set receipt exists with the admitted order
   and digest.
4. Equal retry returns the original event; changed reuse conflicts.
5. Review GET returns those two real split ids and candidate texts.
6. Approve split 0 and correct+approve split 1; refresh retains candidate and correction history.
7. A later matching translation turn receives split 1's corrected pair as pinned memory while
   cross-tenant and wrong-language fixtures receive none.
8. Publication composition and approval name the effective candidate-set digest and publish the
   corrected bytes in manifest order.

Start by making the current tests that require `segment_index 0` fail in favor of the two-member
claim, then extend translation evidence, review route, memory retrieval, and publication runtime
tests. A route-availability test or frontend synthetic split does not satisfy this proof.

## Done when

- Resource-backed agent translations create real persisted split candidates and no client-side
  blank-line split is treated as review data.
- Candidate, review, correction, memory, publication approval, and rendered bytes all bind to
  the same immutable source/split/candidate coordinates.
- Correction-to-memory-to-later-prompt behavior is covered end to end.
- The restored three-pane UI can review the same splits through its existing field/action shape.
- A human verification script and browser tour demonstrate the complete workflow.
