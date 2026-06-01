---
uuid: "knoxx-knowledge-lake-package-scaffold-and-core-interfaces"
title: "knowledge-lake: Package scaffold and core provider interfaces"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# knowledge-lake: Package scaffold and core provider interfaces

> Parent epic: `knoxx-knowledge-ops-provider-abstraction`
> Points: 3

## Purpose

Create the `packages/knowledge-lake` package with its directory structure, `package.json`, `tsconfig.json`, and the canonical TypeScript interface definitions for all six provider contracts (SearchProvider, EmbeddingProvider, StorageProvider, BlobProvider, QueueProvider, AuthProvider) plus shared types.

## Scope

- `packages/knowledge-lake/package.json` — name, scripts, deps skeleton
- `packages/knowledge-lake/tsconfig.json` — strict TypeScript config
- `packages/knowledge-lake/src/core/interfaces.ts` — all six provider interfaces and their supporting option/result types as specified in the epic
- `packages/knowledge-lake/src/core/types.ts` — shared types (IndexDefinition, SearchDocument, SearchResult, QueryOptions, JobPayload, JobStatus, TenantContext, etc.)
- `packages/knowledge-lake/src/core/config.ts` — env-driven provider name resolution (SEARCH_PROVIDER, EMBEDDING_PROVIDER, STORAGE_PROVIDER, BLOB_PROVIDER, QUEUE_PROVIDER, AUTH_PROVIDER) with self-hosted defaults
- Create stub `packages/knowledge-lake/src/index.ts` (empty export, filled in a later task)
- Create placeholder `packages/knowledge-lake/src/providers/` and `packages/knowledge-lake/src/domain/` directories (`.gitkeep` or empty index files)

## Definition of done

- `packages/knowledge-lake` can be compiled with `tsc --noEmit` without errors
- All six provider interfaces are exported from `src/core/interfaces.ts` and match the signatures in the epic spec exactly
- `config.ts` reads each `*_PROVIDER` env var and returns one of the recognised provider names with a self-hosted default; this is covered by at least one unit test
- `package.json` lists the package as `@knoxx/knowledge-lake` and includes `build`, `typecheck`, and `test` script entries

## Notes

Split from parent epic `knoxx-knowledge-ops-provider-abstraction` on 2026-05-30.
