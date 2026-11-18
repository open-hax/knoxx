---
uuid: "big-pi-optimizely-provider-contract"
title: "Prove a replaceable Optimizely resource provider"
status: incoming
priority: P2
labels: ["tasks", "has-parent", "cms", "optimizely"]
created_at: "2026-11-18T00:00:00Z"
points: 0
category: tasks
---
# Prove a replaceable Optimizely resource provider

> Parent epic: `big-pi-optimizely-compatibility`

As a resource consumer, I can use the same repository operations regardless of provider so revisions, references and authorization retain their meaning. The current Clio document bridge is not that provider.

**Acceptance**

- Implement the existing parent fake/file contract first, then map a qualified Optimizely provider without consumers branching on provider identity.
- Prove tenant authorization before existence disclosure, expected-version conflicts, immutable resource and provenance history, exact pinned transitive closures, cycle and missing-reference failures, and authorization receipts.
- Prove linearizable multi-resource observation including exact absence and exact versions; prove crash safety and cross-process races to the guarantees in the parent cards. Refuse operations the upstream system cannot implement truthfully.

**Evidence**: shared provider contract suite with concurrency, crash, closure and authorization cases against fake, file and the Optimizely adapter; documented unsupported provider obligations and live integration fixture.
