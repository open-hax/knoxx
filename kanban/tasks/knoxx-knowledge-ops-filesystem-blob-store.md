---
uuid: "knoxx-knowledge-ops-filesystem-blob-store"
title: "Implement SHA-256 sharded filesystem blob store for knowledge ops"
status: incoming
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---

# Implement SHA-256 sharded filesystem blob store for knowledge ops

> Parent epic: `knoxx-knowledge-ops-deploy-self-hosted`
> Points: 2

## Purpose

Provide a local filesystem blob store (SHA-256 content-addressable, two-level shard directories) so binary assets — documents, audio, images — are stored and retrieved without any cloud object-storage dependency, activated via `BLOB_PROVIDER=filesystem`.

## Scope

- Implement a filesystem blob adapter under the appropriate infra namespace
- Shard layout: `<BLOB_ROOT>/<first-2-hex>/<next-2-hex>/<full-sha256>` (content-addressed, dedup by hash)
- Expose `BLOB_ROOT` env var (default `./data/blobs`)
- Adapter must satisfy the blob protocol: `put(stream) → sha256`, `get(sha256) → stream`, `delete(sha256)`, `exists(sha256)`
- Write unit tests covering round-trip put/get and dedup behaviour

## Definition of done

- `BLOB_PROVIDER=filesystem` stores and retrieves a binary fixture without corruption
- Shard directories are created automatically on first write
- Unit tests pass (`pnpm test` in `backend/`)

## Notes

Split from parent epic `knoxx-knowledge-ops-deploy-self-hosted` on 2026-05-30.
