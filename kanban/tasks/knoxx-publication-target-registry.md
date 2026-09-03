---
category: "tasks"
labels: ["tasks", "has-parent", "publication", "adapters", "resources", "wave-1"]
write-id: "1787011200000-0.381743"
points: "5"
title: "Publication — target registry from resources"
priority: "P1"
status: "ready"
uuid: "knoxx-publication-target-registry"
created_at: "2026-08-22T00:00:00Z"
---

# Publication — target registry from resources

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

Select a publication adapter from declared resources rather than constructing an
adapter at a publication call site. Desired target identity stays in resources;
the registry resolves that identity to a Knoxx-owned adapter without making the
adapter, its mutable state, or its transport part of the domain plan.

## Dependencies

`knoxx-publication-artifact-contract` and the existing publication adapter
effect/idempotency contract. `knoxx-publication-static-site-target` consumes
this registry.

## Work

- Define a resource-shaped target declaration with a stable target id, target
  kind, and adapter configuration sufficient for selection without embedding an
  adapter instance in resource data.
- Build a registry that resolves a declared target kind to its
  `IPublicationTarget` implementation and rejects unknown, duplicate, malformed,
  or disabled target declarations before effects run.
- Keep selection separate from reconciliation: the pure plan names the desired
  target; only runtime composition turns that name and validated configuration
  into an adapter.
- Pass validated publication context through the registry unchanged so the
  adapter still receives the concrete revision, publication identity, and
  idempotency operation identity established by existing laws.
- State explicitly that locale admissibility is not inferred here. The verifier
  found that `:artifact/locale` is not cross-checked with
  `:publication/locale`; accepted target locales belong to
  `knoxx-publication-locale-catalog`.

## Definition of Done

- A resource declaration selects the registered adapter without a call site
  constructing that adapter directly.
- Unknown, duplicate, disabled, and structurally invalid targets fail before
  `publish!`, `remove!`, or `observe!` is invoked.
- Tests prove target selection is deterministic and adapter configuration cannot
  alter the pure reconciliation decision.
- Tests prove locale mismatch remains unadmitted until the locale-catalog guard
  is supplied; the registry does not silently treat any locale as accepted.
