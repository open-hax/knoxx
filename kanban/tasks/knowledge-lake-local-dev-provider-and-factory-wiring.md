---
uuid: "knoxx-knowledge-lake-local-dev-provider-and-factory-wiring"
title: "knowledge-lake: Local/dev provider implementations and provider factory wiring"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# knowledge-lake: Local/dev provider implementations and provider factory wiring

> Parent epic: `knoxx-knowledge-ops-provider-abstraction`
> Points: 3

## Purpose

Implement the six lightweight local/dev providers (ChromaDB or in-memory search, Ollama local embedding, DuckDB/SQLite storage, filesystem blob, in-process queue, no-auth) and wire the provider factory in `src/index.ts` so that `*_PROVIDER` env vars select the correct implementation at startup.

## Scope

- `src/providers/local/search.ts` — in-memory vector store (cosine similarity over a plain array); no external service required
- `src/providers/local/embedding.ts` — Ollama local client defaulting to `nomic-embed-text` or `qwen3:0.6b`; reuses Ollama HTTP client pattern from self-hosted embedding
- `src/providers/local/storage.ts` — DuckDB (preferred) or SQLite via `better-sqlite3` as fallback; single-file DB at `LOCAL_STORAGE_PATH` (default `./data/local.db`)
- `src/providers/local/blob.ts` — plain `fs` read/write to `LOCAL_BLOB_DIR` (default `./data/local-blobs`); same SHA-256 key contract as self-hosted blob
- `src/providers/local/queue.ts` — in-process `Map`-backed queue; jobs are lost on process restart (acceptable for dev)
- `src/providers/local/auth.ts` — no-auth pass-through: always resolves tenant as `{ tenantId: "dev", roles: ["admin"], scopes: ["*"] }`
- `src/index.ts` — provider factory: reads `config.ts` values, instantiates the correct provider class for each slot, calls `initialize()` on lifecycle-bearing providers, and exports a fully assembled `Lake` object with typed provider slots and domain module instances

## Definition of done

- All six local providers typecheck against `src/core/interfaces.ts`
- `src/index.ts` factory selects `local` providers when all `*_PROVIDER` env vars are unset (or set to `"local"`), and `self-hosted` when set to `"self-hosted"`
- Running `node -e "require('./dist/index.js')"` with no env vars set completes without error (local providers initialise successfully)
- `local/auth.ts` `health()` always returns `{ status: "ok" }` synchronously

## Notes

Split from parent epic `knoxx-knowledge-ops-provider-abstraction` on 2026-05-30.
