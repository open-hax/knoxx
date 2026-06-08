---
uuid: "knoxx-knowledge-ops-ollama-embedding-provider"
title: "Implement Ollama embedding provider for knowledge ops"
status: rejected
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---
# Implement Ollama embedding provider for knowledge ops

> Parent epic: `knoxx-knowledge-ops-deploy-self-hosted`
> Points: 3

## Purpose

Deliver a self-hosted embedding provider backed by Ollama (qwen3-embedding:0.6b / :4b / :8b) so the knowledge pipeline can generate vector embeddings without any cloud API dependency, activated via `EMBEDDING_PROVIDER=ollama`.

## Scope

- Implement an Ollama embedding adapter that calls the local Ollama HTTP API (`OLLAMA_BASE_URL`, default `http://localhost:11434`) using the model specified by `OLLAMA_EMBEDDING_MODEL` (default `qwen3-embedding:0.6b`)
- Adapter must satisfy the shared embedding provider protocol (batch input → float array output)
- Expose model selection and batch size as config keys
- Write unit tests with a stub Ollama server or recorded HTTP fixture

## Definition of done

- `EMBEDDING_PROVIDER=ollama` produces valid float-vector embeddings when Ollama is running locally
- Adapter handles Ollama unavailability with a clear error (not a generic crash)
- Unit tests pass (`pnpm test` in `backend/`)

## Notes

Split from parent epic `knoxx-knowledge-ops-deploy-self-hosted` on 2026-05-30.
