---
uuid: "knoxx-knowledge-ops-mongodb-storage-sink"
title: "Implement MongoDB structured storage sink for knowledge ops"
status: icebox
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---
# Implement MongoDB structured storage sink for knowledge ops

> Parent epic: `knoxx-knowledge-ops-deploy-self-hosted`
> Points: 3

## Purpose

Wire the structured storage layer to MongoDB 8.2 so that knowledge records, document metadata, and search indexes are persisted and retrievable via the `STORAGE_PROVIDER=mongodb` config key.

## Scope

- Implement a MongoDB storage adapter under `backend/src/cljs/knoxx/backend/infra/` (or the appropriate provider namespace) satisfying the existing storage protocol
- Connect using the `MONGODB_URI` env var; use the `knoxx` database by default
- Ensure collections are created with the required indexes (vector index compatible with mongot, plus standard FTS index)
- Write unit tests covering insert, find-by-id, and list operations against a mock or in-process MongoDB

## Definition of done

- `STORAGE_PROVIDER=mongodb` boots without errors and passes the storage integration smoke test
- Collection indexes exist and are validated on startup (logged at info level)
- Unit tests pass (`pnpm test` in `backend/`)

## Notes

Split from parent epic `knoxx-knowledge-ops-deploy-self-hosted` on 2026-05-30.
