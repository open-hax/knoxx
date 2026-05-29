---
uuid: "knoxx-knowledge-ops-deploy-self-hosted"
title: "The Lake — Self-Hosted Deployment Spec"
status: accepted
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.384Z"
source: "specs/epics/knowledge-ops-deploy-self-hosted.md"
points: null
category: epics
---
# The Lake — Self-Hosted Deployment Spec

> Source: `specs/epics/knowledge-ops-deploy-self-hosted.md`

> *MongoDB + mongot + Ollama. No cloud provider. Your servers, your rules.*

---

## Provider Mapping

| Logical Component | Implementation | Config Key |
|-------------------|---------------|------------|
| Search (vector + FTS + hybrid) | **MongoDB 8.2 + mongot** ($vectorSearch, $search) | `SEARCH_PROVIDER=mongodb` |
| Embeddings | **Ollama** (qwen3-embedding:0.6b / :4b / :8b) | `EMBEDDING_PROVIDER=ollama` |
| Structured storage | **MongoDB 8.2** | `STORAGE_PROVIDER=mongodb` |
| Blob storage | **Filesystem** (SHA-256 sharded) | `BLOB_PROVIDER=filesystem` |
| Job queue | **JSONL job queue** | `QUEUE_PROVIDER=jsonl` |
| Auth | **Bearer token** | `AUTH_PROVIDER=bearer-token` |
| App hosting | **Docker Compose** | — |
