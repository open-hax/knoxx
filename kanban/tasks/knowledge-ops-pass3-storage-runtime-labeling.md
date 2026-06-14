---
uuid: "knoxx-knowledge-ops-pass3-storage-runtime-labeling"
title: "Knowledge-ops consistency pass 3: storage and runtime labeling"
status: breakdown
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---
# Knowledge-ops consistency pass 3: storage and runtime labeling

> Parent epic: `knoxx-knowledge-ops-consistency-review`
> Points: 3

## Purpose

Audit the 34 knowledge-ops specs for consistent labeling of storage layer (MongoDB, Redis, in-memory graph) and runtime tier (ingestion, backend, frontend) so that downstream tooling and search can reliably filter by infrastructure concern.

## Scope

- Review each spec's body and labels for mentions of storage backends and runtime tiers
- Add or correct `labels:` entries to include at least one storage tag (`mongo`, `redis`, `graph`, `in-memory`) and one runtime tag (`ingestion`, `backend`, `frontend`) where the spec is scoped to those concerns
- Document any specs that intentionally span multiple tiers with a `cross-layer` label

## Definition of done

- Every spec that targets a specific storage backend or runtime tier carries the corresponding label
- No spec is silently missing a storage or runtime label when its body references one
- A `cross-layer` label is applied where appropriate

## Notes

Split from parent epic `knoxx-knowledge-ops-consistency-review` on 2026-05-30.
