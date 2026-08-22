---
category: "tasks"
labels: ["tasks", "has-parent", "translation", "approval", "authorization", "publication", "wave-2"]
write-id: "1787011200004-0.528396"
points: "3"
title: "Translation — revision-specific approval surface"
priority: "P1"
status: "ready"
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
