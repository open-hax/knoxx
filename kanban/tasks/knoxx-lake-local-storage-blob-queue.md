---
uuid: "knoxx-lake-local-storage-blob-queue"
title: "Implement local/dev DuckDB storage, filesystem blob, and in-memory queue providers"
status: icebox
priority: P2
labels: ["tasks", "4sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 4
category: tasks
---
# Implement local/dev DuckDB storage, filesystem blob, and in-memory queue providers

> Parent epic: `knoxx-knowledge-ops-multi-provider-epic`
> Points: 4

## Purpose

Complete the remaining three local/dev provider roles (Storage, Blob, Queue) so the local deployment target is fully self-contained — a developer can run the entire knowledge platform with only DuckDB, a local filesystem, and no external queue service.

## Scope

- Implement `StorageProvider` in `providers/local/storage.ts` using `duckdb-async`: create/read/update/delete document records in a persistent `.lake.duckdb` file, with a schema migration helper for first-run
- Implement `BlobProvider` in `providers/local/blob.ts`: write binary blobs to a configurable directory using SHA-256-derived paths (`<root>/<first2>/<rest>`), read by content-hash, and delete by hash
- Implement `QueueProvider` in `providers/local/queue.ts`: in-memory FIFO queue with `enqueue`, `dequeue`, and `peek` — no persistence, suitable for dev/demo only; document the limitation in a JSDoc comment
- All three must implement their corresponding interfaces from `src/core/interfaces.ts`
- Write unit tests in `tests/providers/local.storage.test.ts`, `local.blob.test.ts`, and `local.queue.test.ts`

## Definition of done

- `DuckDBStorageProvider`, `FilesystemBlobProvider`, and `MemoryQueueProvider` classes compile under strict TypeScript with no errors
- Unit tests for all three providers pass with `pnpm --filter @open-hax/knowledge-lake test`
- A seed script at `scripts/seed-demo.ts` can successfully enqueue, store, and retrieve a sample document using all three providers wired through `createLake({ provider: 'local' })`

## Notes

Split from parent epic `knoxx-knowledge-ops-multi-provider-epic` on 2026-05-30.
