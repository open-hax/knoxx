---
category: "tasks"
labels: ["tasks", "has-parent", "translation", "approval", "authorization", "publication", "wave-2"]
write-id: "1787011200004-0.528396"
points: "3"
title: "Translation — revision-specific approval surface"
priority: "P1"
status: "review"
uuid: "knoxx-translation-approval-surface"
created_at: "2026-08-22T00:00:00Z"
---

# Translation — revision-specific approval surface

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

Give an authorized human a real route to record approval of one translated
revision. Approval is evidence for the publication gate, not a mutable blanket
permission for a document or locale.

## Dependencies

The existing translation receipt and publication gate contracts.
`knoxx-publication-reconciler-runtime` consumes the approval evidence.

## Work

- Provide an authenticated, authorized API and/or UI action that records an
  approval against translation identity, target locale, and concrete translated
  revision.
- Validate the requested approval against the corresponding completed
  translation receipt before persisting it; reject selector revisions, unknown
  translations, and revision or locale mismatches.
- Attribute each approval to the acting principal and timestamp it as immutable
  review evidence usable by the gate.
- Return a validated approval receipt to the caller and ensure unauthorized and
  malformed requests fail before review state changes.
- Keep approval recording separate from publication effects: an approval may
  make a plan admissible but must not itself materialize content.

## Definition of Done

- An authorized principal can approve one completed translation revision through
  the supported route and receives a revision-specific receipt.
- An unauthenticated or unauthorized request cannot create approval evidence.
- Tests prove an approval for one revision, locale, or document cannot satisfy
  the gate for another.
- Tests prove approval alone invokes no publication adapter effect.

## Card premise corrections

1. **"records an approval against translation identity, target locale, and
   concrete translated revision."** Taken literally this produces approvals the
   gate can never match: `domain.publication-gate` asks `approved?` with the
   concrete *source* revision, because that is what a publication intent's
   revision selector resolves to. The approval is therefore keyed by the source
   revision, with the translated output recorded alongside as
   `:review/translation-revision`. Both matter — see
   `law.translation-evidence/approval-current?` — and keying by the output alone
   would have made every approval invisible to the gate.

2. **"The existing translation receipt and publication gate contracts."** The
   gate contract existed; a translation receipt contract did not. It was
   introduced by `knoxx-translation-work-dispatch`, which is why that card was
   done first even though both are wave-2: the consumer cannot validate against a
   contract the producer has not declared.

3. **An unstated requirement.** Approvals are tenant- and project-scoped, like
   the receipts they attest to. The card does not mention scope at all, and the
   dispatch card needed two review rounds to establish that translation evidence
   without a tenant is admissible everywhere. `:review/org-id` is required and
   `:review/project` is carried, both inherited from the receipt rather than from
   the request so a reviewer cannot file into another scope.

4. **A second unstated requirement, learned the same way.** The garden is a
   *coordinate* of an approval, not scope that happens to travel with it.
   `knoxx-translation-work-dispatch` made the completed-translation receipt
   garden-specific under review, because the ingestion worker builds its prompt
   with `garden_id` and translated-document reads filter by it — one document
   translated into one locale for two gardens is two different outputs. The
   approval half had to follow or the same leak reappeared one step later, with
   a reviewer who read garden A's bytes admitting publication of garden B's.
   `:review/garden` is therefore required on both `Approval` and
   `ApprovalRequest`, `approved?` and `index-approvals` key by it, and it is
   taken from the receipt rather than from the request.

## What this card does NOT deliver

The approval *surface*, not the approval *control*. Nothing in the frontend
calls `POST /api/publications/translations/approvals`; the existing translation
review UI still posts to `/api/translations/documents/:id/:locale/review`, which
is the older document-review model and is not what the publication gate reads.

So today a reviewer clicking "Approve All" does not produce evidence
`domain.publication-gate` recognizes. That is a real gap and it is deliberately
not papered over here: making the legacy review status count as a publication
approval is exactly the shortcut that would let unreviewed bytes publish.

Closing it needs one thing this card cannot assume — a read surface exposing
which `[document garden locale]` currently has a completed translation and at
which pair of revisions. The UI has `garden_id` but no notion of either
revision, and the approval contract requires both. That belongs to the UI card
together with the endpoint it reads.
