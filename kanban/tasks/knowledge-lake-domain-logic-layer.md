---
uuid: "knoxx-knowledge-lake-domain-logic-layer"
title: "knowledge-lake: Domain logic layer (documents, tenants, compaction, labeling, PII)"
status: incoming
priority: P2
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 5
category: tasks
---

# knowledge-lake: Domain logic layer (documents, tenants, compaction, labeling, PII)

> Parent epic: `knoxx-knowledge-ops-provider-abstraction`
> Points: 5

## Purpose

Implement the provider-independent business logic in `src/domain/` — the layer that sits above provider abstractions and encodes document lifecycle, tenant isolation, semantic compaction, HITL labeling, and PII detection without any direct infrastructure dependencies.

## Scope

- `src/domain/documents.ts` — document lifecycle state machine: `draft → review → public → archive`; enforces transitions, records `updatedAt` and `status` metadata, delegates persistence to an injected StorageProvider
- `src/domain/tenants.ts` — tenant isolation helpers: namespace all collection queries with `tenant_id`, enforce that cross-tenant reads are rejected, expose `withTenant(ctx, fn)` wrapper
- `src/domain/compaction.ts` — semantic compaction pipeline: given a document array, use EmbeddingProvider to cluster near-duplicates, select canonical entries, return a compacted set; configurable similarity threshold
- `src/domain/labeling.ts` — HITL labeling primitives: attach label candidates to documents (via StorageProvider), record human decisions, export labeled dataset as JSONL for training
- `src/domain/pii.ts` — PII detection: regex + heuristic scanner for emails, phone numbers, national IDs; returns detected spans and optionally redacts them; does not call any external provider

Each domain module accepts provider instances as constructor/function arguments (dependency injection) — no global singletons.

## Definition of done

- All five domain modules typecheck cleanly and have no direct imports from `src/providers/`
- `documents.ts` unit tests cover all four lifecycle transitions including invalid transition rejection
- `tenants.ts` `withTenant` wrapper is verified to inject `tenant_id` into every StorageProvider call via a mock provider in tests
- `compaction.ts` accepts a configurable `similarityThreshold` (0–1) and is tested with a mock EmbeddingProvider returning fixed vectors
- `pii.ts` detects at minimum: RFC-5322 email addresses, E.164 phone numbers, and runs without any network calls; covered by unit tests with known fixtures

## Notes

Split from parent epic `knoxx-knowledge-ops-provider-abstraction` on 2026-05-30.
