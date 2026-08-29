---
category: "tasks"
labels: ["tasks", "has-parent", "publication", "reconciliation", "runtime", "receipts", "wave-1"]
write-id: "1787011200002-0.914627"
points: "3"
title: "Publication — reconciler runtime"
priority: "P1"
status: "ready"
uuid: "knoxx-publication-reconciler-runtime"
created_at: "2026-08-22T00:00:00Z"
---

# Publication — reconciler runtime

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

Make the proven pure plan and replaceable adapter effects run on a real trigger.
The runtime translates a trigger into plan → effects and records evidence of what
actually happened; it does not turn operational receipt state into desired state.

## Dependencies

`knoxx-publication-target-registry`, `knoxx-publication-static-site-target`,
`knoxx-translation-work-dispatch`, and `knoxx-translation-approval-surface`.

## Work

- Define the runtime entry point and trigger payload that load resources,
  publication intent, relevant translation/review facts, and a concrete artifact
  before invoking the existing pure reconciliation law.
- Resolve the declared target through the registry, execute the resulting plan,
  and preserve the planner's operation identities when effects are retried.
- Persist or emit a validated receipt for every attempted effect, including
  blocked, noop, materialized, removed, and failed outcomes, with enough
  correlation data to trace the triggering publication identity and revision.
- Ensure one failed target effect is observable and does not fabricate a
  materialized receipt; desired resource state remains unchanged by runtime
  observation.
- Provide an explicit trigger integration (event, scheduled job, or authorized
  runtime route) rather than a library function that no production path calls.

## Definition of Done

- An integration test triggers reconciliation and proves the runtime calls pure
  planning before adapter effects.
- A materialization, noop, blocker, removal, and adapter failure each produce a
  receipt with the expected outcome and correlation identity.
- A retry of the same operation preserves idempotency and yields no duplicate
  publication.
- Tests prove a receipt cannot mutate desired resource declarations or bypass a
  translation/review blocker.
