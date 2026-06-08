---
uuid: "knoxx-lake-local-embedding-search"
title: "Implement local/dev ChromaDB search and Ollama embedding providers"
status: rejected
priority: P2
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 5
category: tasks
---
# Implement local/dev ChromaDB search and Ollama embedding providers

> Parent epic: `knoxx-knowledge-ops-multi-provider-epic`
> Points: 5

## Purpose

Deliver the search and embedding halves of the local/dev provider (Epic 1 of the parent), enabling zero-cost semantic search on a developer machine via ChromaDB and Ollama — the demo path that ships first before any cloud provider is needed.

## Scope

- Implement `SearchProvider` in `providers/local/search.ts` using the ChromaDB JS client: `upsert`, `search` (cosine similarity, top-k), and `delete` operations against a named collection
- Implement `EmbeddingProvider` in `providers/local/embedding.ts` calling the Ollama `/api/embeddings` endpoint with configurable model (default `nomic-embed-text`; also support `qwen3:0.6b`)
- Both implementations must satisfy the interfaces defined in `src/core/interfaces.ts`
- Add a `docker-compose.dev.yml` service entry for ChromaDB (port 8000) if the file does not yet exist
- Write unit tests in `tests/providers/local.search.test.ts` and `tests/providers/local.embedding.test.ts` using mocked HTTP responses

## Definition of done

- `ChromaSearchProvider` and `OllamaEmbeddingProvider` classes implement their respective interfaces with no TypeScript errors
- Unit tests pass with `pnpm --filter @open-hax/knowledge-lake test`
- A round-trip integration test (`upsert` → `search`) succeeds against a live ChromaDB container started via docker-compose

## Notes

Split from parent epic `knoxx-knowledge-ops-multi-provider-epic` on 2026-05-30.
