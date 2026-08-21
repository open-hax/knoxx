---
uuid: knoxx-publication-reconciler-runtime
title: Publication — something that actually reconciles
status: ready
priority: P1
points: 3
labels:
  - tasks
  - publication
  - runtime
  - has-parent
---

# Publication — something that actually reconciles

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

`infra/publication_effects.cljs` is required by two files: the memory target and
its tests. Nothing in the running backend calls plan → effects. Desired state is
served, observed state is never produced, and drift is therefore always the whole
of desired state.

## Dependencies

`knoxx-publication-static-site-target`, `knoxx-publication-target-registry`. It
can be built against the memory target and switched over.

## Work

- Add a reconcile entry point: resolve desired intents, compute the plan, execute
  it through the effect boundary, and append receipts.
- Triggers, in increasing order of ambition — deliver the first two:
  explicit invocation through an authorized route, and after a publication intent
  changes. A periodic sweep is optional and must be separately disableable.
- One reconcile at a time per target. Two concurrent runs against one content
  root is the case the idempotency store exists for, and it should not be the
  normal path.
- A failing publish surfaces as drift and must not mutate desired state, matching
  what `memory-target`'s `:fail?` mode already proves at the boundary.
- One intent's failure does not abandon the rest of the run. Report per-intent
  outcomes.
- Bound the run. A hung adapter must fail the reconcile, not hold it open — the
  same rule the deploy gates already apply to every probe.
- Emit a run summary: attempted, materialized, noop, removed, blocked, failed.
  Blocked is not failed, and a run that is all-blocked is a success.
- Respect `knoxx-http-event-runtime-lifecycle-separation`: HTTP-only startup must
  not implicitly begin reconciling.

## Definition of Done

- A publication intent reaches the target without a human calling a REPL.
- Running twice with no change materializes nothing and reports all `:noop`.
- An adapter failure appears as drift, leaves desired state untouched, and does
  not abort the run.
- Blocked intents are reported as blocked and are not retried as failures.
- Concurrent runs against one target cannot double-publish.
- An HTTP-only process performs no reconciliation until asked.
