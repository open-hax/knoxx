---
uuid: knoxx-publication-target-registry
title: Publication — resolve the target adapter from resources
status: ready
priority: P1
points: 5
labels:
  - tasks
  - publication
  - contracts
  - has-parent
---

# Publication — resolve the target adapter from resources

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

There is one adapter and it is constructed by whoever wants it. The moment a
second exists — the site target — something must decide which one a given
publication uses, and that decision belongs in resources, like every other
desired-state decision in this epic.

## Dependencies

`knoxx-publication-artifact-contract`.

## Work

- Add a publication-target resource kind: stable adapter id, adapter kind, and
  adapter-kind-specific configuration validated per kind rather than as an open
  map.
- Resolve the adapter for an intent from the resource graph. A publication whose
  declared target does not resolve is a blocker with the target named, not a
  silent fallback to a default adapter.
- Keep adapter identity below the semantic boundary in every direction except
  the receipt: `target-id` is already part of the idempotency key, so a
  publication that changes target republishes rather than reporting `:noop`, and
  that must be asserted rather than assumed.
- The registry constructs adapters; it holds no publication semantics and makes
  no admissibility decision.
- Register the memory target through the same registry the site target uses. A
  test double reached by a different path than production proves less than it
  appears to.
- Fail closed on an unknown adapter kind. An unrecognized kind must never fall
  through to the memory target in a production process — that would report
  success while publishing nowhere.

## Definition of Done

- Adapter selection is a resource fact, resolved by the same loader as every
  other resource.
- An unresolvable or unknown target is a typed blocker naming the target.
- The memory target is obtained through the registry in tests and the E2E.
- Changing an intent's target changes its idempotency key, asserted.
- No adapter-specific identifier appears in plan or gate output.
