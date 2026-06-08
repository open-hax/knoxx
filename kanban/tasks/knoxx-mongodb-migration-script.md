---
uuid: "knoxx-mongodb-migration-script"
title: "Write one-time DuckDB + ChromaDB to MongoDB migration script"
status: rejected
priority: P2
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 5
category: tasks
---
# Write one-time DuckDB + ChromaDB to MongoDB migration script

> Parent epic: `knoxx-knowledge-ops-mongodb-vector-unification`
> Points: 5

## Purpose

Implement the one-time data migration script (`scripts/migrate-to-mongodb.ts`) that exports all existing events and compacted memories from DuckDB and all vectors from ChromaDB into the unified MongoDB collections, preserving embeddings and ensuring row counts match.

## Scope

- Create `scripts/migrate-to-mongodb.ts` implementing the Phase 3 migration path from the spec:
  1. Export DuckDB `events` and `compacted_memories` tables to JSONL
  2. Insert JSONL records into MongoDB `events` and `compacted_memories` collections with correct schema (`openplanner.event.v1`, `openplanner.compacted.v1`)
  3. Export ChromaDB hot and compact collection embeddings
  4. Attach embedding vectors to corresponding MongoDB documents by matching on `id` / `uuid`
  5. Create all required search indexes (`events_text`, `events_vector_hot`, `chunks_vector_warm`, `compact_vector`) via `src/lib/mongodb-indexes.ts` (create this module if it does not yet exist)
  6. Print a final verification report: source counts vs. MongoDB counts per collection

## Definition of done

- Running `npx ts-node scripts/migrate-to-mongodb.ts` against a populated dev environment completes without uncaught errors
- MongoDB `events` document count equals DuckDB `events` row count after migration
- MongoDB `compacted_memories` document count equals DuckDB `compacted_memories` row count after migration
- At least 95% of migrated `events` documents carry a non-null `embedding` field sourced from ChromaDB (remainder documented in script output)
- All four search indexes exist on the target collections and report status `READY` via `db.collection.getSearchIndexes()`

## Notes

Split from parent epic `knoxx-knowledge-ops-mongodb-vector-unification` on 2026-05-30.
