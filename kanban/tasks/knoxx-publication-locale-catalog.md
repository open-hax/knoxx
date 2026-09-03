---
category: "tasks"
labels: ["tasks", "has-parent", "publication", "locales", "resources", "adapters", "wave-2"]
write-id: "1787011200005-0.793154"
points: "2"
title: "Publication — target locale catalog"
priority: "P2"
status: "ready"
uuid: "knoxx-publication-locale-catalog"
created_at: "2026-08-22T00:00:00Z"
---

# Publication — target locale catalog

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

Declare the locales each publication target accepts in resources and enforce the
locale identity that reaches the static-site adapter. Without this guard, an
artifact path derived from `:artifact/locale` can place wrong-language bytes
behind a manifest route derived from `:publication/locale`.

## Dependencies

`knoxx-publication-artifact-contract`. It may land after that contract and must
be wired before static-site publication is allowed to materialize locale routes.

## Work

- Extend target resource declarations with an explicit locale catalog or policy
  that states the locales accepted by that target.
- Validate `:publication/locale` against the selected target catalog during
  admissibility and validate it equals `:artifact/locale` before adapter effects.
- Define a clear error/receipt shape for unsupported and disagreeing locales so
  reconciliation reports a blocker rather than a misleading materialization.
- Keep default locale and route-prefix behavior owned by the website reader; this
  card governs writer target admissibility only.

## Definition of Done

- A target resource declares its accepted locales and an accepted locale reaches
  the adapter unchanged.
- Unsupported publication locales are blocked before artifact or manifest writes.
- Tests prove a disagreement between `:publication/locale` and
  `:artifact/locale` cannot create a route or expose bytes.
- Tests prove locale catalog validation does not change the website's reader-side
  default-locale routing rules.
