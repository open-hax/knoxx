---
uuid: "knoxx-knowledge-lake-self-hosted-provider-implementations"
title: "knowledge-lake: Self-hosted provider implementations"
status: "incoming"
priority: "P2"
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 5
category: "tasks"
---

# knowledge-lake: Self-hosted provider implementations

> Parent epic: `knoxx-knowledge-ops-provider-abstraction`
> Points: 5

## Purpose

Implement the six self-hosted provider classes under `src/providers/self-hosted/`, giving the platform a fully functional default backend that requires no cloud accounts — MongoDB for search/storage, Ollama for embeddings, filesystem blob store, JSONL queue, and bearer-token auth.

## Scope

- `src/providers/self-hosted/search.ts` — MongoDB `$vectorSearch` + full-text (`mongot`) implementation of SearchProvider
- `src/providers/self-hosted/embedding.ts` — Ollama HTTP client wrapping the `qwen3-embedding` model; implements `embed`, `embedBatch`, `dimensions`, `availableModels`, and `health`
- `src/providers/self-hosted/storage.ts` — MongoDB CRUD + query implementation of StorageProvider
- `src/providers/self-hosted/blob.ts` — SHA-256-sharded local filesystem implementation of BlobProvider (`put` returns the hash key, `get` reconstructs from path)
- `src/providers/self-hosted/queue.ts` — append-only JSONL file queue with `enqueue`/`dequeue`/`complete`/`fail`/`status` using a companion `.status.jsonl` sidecar
- `src/providers/self-hosted/auth.ts` — bearer-token tenant resolution: reads `X-API-Key`, looks up a JSON key store, returns TenantContext or null

Each file must implement the interface from `src/core/interfaces.ts` and include a `health()` method that performs a lightweight connectivity check.

## Definition of done

- All six files typecheck cleanly against the interfaces in `src/core/interfaces.ts`
- `search.ts`, `storage.ts` connect to MongoDB using a URI supplied via env (`MONGODB_URI`); connection is lazy (established on first `initialize()` call)
- `embedding.ts` connects to Ollama via `OLLAMA_BASE_URL` (default `http://localhost:11434`)
- `blob.ts` uses `BLOB_ROOT_DIR` env var (default `./data/blobs`) and shards paths as `<root>/<first2>/<rest>`
- `queue.ts` uses `QUEUE_DIR` env var (default `./data/queues`)
- `auth.ts` reads key store from `AUTH_KEY_STORE_PATH` env var
- Each provider's `health()` returns `{ status: "ok" }` on success and `{ status: "error", message }` on failure without throwing

## Notes

Split from parent epic `knoxx-knowledge-ops-provider-abstraction` on 2026-05-30.

---
I don't remember what this was for
---
