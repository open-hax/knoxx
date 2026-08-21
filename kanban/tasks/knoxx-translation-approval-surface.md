---
uuid: knoxx-translation-approval-surface
title: Translation — a real surface for recording review approval
status: ready
priority: P1
points: 3
labels:
  - tasks
  - translations
  - publication
  - review
  - has-parent
---

# Translation — a real surface for recording review approval

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

The gate requires a revision-specific approval receipt before a translated
document may publish, and approval receipts exist only as test fixtures. There is
no route, no tool, and no UI through which a person can approve anything. Under a
review policy of `required`, nothing can ever publish.

## Dependencies

`knoxx-publication-stack-relink`. Related to
`knoxx-translation-review-chat-panel`, which owns the review *experience*; this
card owns only the recorded decision.

## Work

- Add an authorized route that records an approval or a rejection for a document
  × locale × **concrete revision**, with the deciding actor on the receipt.
- Approval is revision-specific by construction. Approving "the document" must be
  inexpressible — the parent epic's gate work already proves a stale translation
  leaves the old approval receipt intact while it stops satisfying the new
  revision, and this surface must not offer a way around that.
- Authorize with a distinct permission from publication management. Approving a
  translation is not authority to change what is published, matching the read /
  manage split already used across the publication surface.
- Approval of an identity with no translation receipt is a conflict, not a
  pre-approval. There is nothing yet to have reviewed.
- Rejection is recorded, not silent. A rejected revision must be distinguishable
  from an unreviewed one, or the reconciler cannot tell "waiting" from "refused".
- Re-approving an already-approved identity is idempotent and does not append a
  second receipt.
- Add the route to `law.publication-surface/required-surfaces` and update
  `surface-count`, so deploy verification and the E2E both cover it — the list is
  asserted against its count precisely so a silently shortened list fails.

## Definition of Done

- A person with the review permission can approve a specific translated revision
  through an authorized route, and the gate's blocker clears.
- The same actor without the publication-manage permission still cannot change
  publication intent.
- Approving an unreviewable identity is refused with the reason.
- A rejection is queryable and distinct from absence.
- A new source revision leaves the old approval recorded and no longer
  satisfying.
- The surface list and its count both grow, and deploy verification covers it.
