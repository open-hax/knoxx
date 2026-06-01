---
uuid: "knoxx-lake-provider-factory"
title: "Implement the provider factory and abstract interfaces"
status: incoming
priority: P2
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 5
category: tasks
---

# Implement the provider factory and abstract interfaces

> Parent epic: `knoxx-knowledge-ops-multi-provider-epic`
> Points: 5

## Purpose

Define the TypeScript abstract interfaces for every provider role (Search, Embedding, Storage, Blob, Queue, Auth) and implement the env-driven factory that selects and wires the correct concrete implementation at deploy time — this is Epic 0 of the parent epic and gates all provider subtasks.

## Scope

- Write all six provider interfaces in `packages/knowledge-lake/src/core/interfaces.ts`: `SearchProvider`, `EmbeddingProvider`, `StorageProvider`, `BlobProvider`, `QueueProvider`, `AuthProvider`
- Write `packages/knowledge-lake/src/core/config.ts`: reads `LAKE_PROVIDER` env var (`local` | `self-hosted` | `azure` | `aws`), lazily imports the matching provider bundle, and returns a fully-typed `LakeProviders` record
- Export a `createLake(config?)` function from `src/index.ts` that returns the assembled provider set
- Add Zod (or TypeBox) runtime validation for the config object

## Definition of done

- All six interfaces are exported from `src/core/interfaces.ts` with at least one method signature each
- `createLake({ provider: 'local' })` resolves without throwing when the local provider bundle exists
- TypeScript strict-mode compile passes with zero errors (`pnpm --filter @open-hax/knowledge-lake typecheck`)

## Notes

Split from parent epic `knoxx-knowledge-ops-multi-provider-epic` on 2026-05-30.
