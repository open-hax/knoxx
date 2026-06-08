---
uuid: "knoxx-knowledge-ops-docker-compose-self-hosted"
title: "Author Docker Compose stack for self-hosted knowledge ops deployment"
status: breakdown
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---
# Author Docker Compose stack for self-hosted knowledge ops deployment

> Parent epic: `knoxx-knowledge-ops-deploy-self-hosted`
> Points: 3

## Purpose

Deliver a production-ready `docker-compose.yml` (and supporting env template) that brings up the full self-hosted knowledge ops stack — MongoDB 8.2 + mongot, Ollama, and the Knoxx backend — with a single `docker compose up`.

## Scope

- Write `deploy/self-hosted/docker-compose.yml` defining services: `mongodb` (8.2), `mongot` (latest compatible), `ollama`, `knoxx-backend`
- Write `deploy/self-hosted/.env.example` covering all required env vars (`MONGODB_URI`, `OLLAMA_BASE_URL`, `OLLAMA_EMBEDDING_MODEL`, `BLOB_ROOT`, `QUEUE_DIR`, `STORAGE_PROVIDER`, `EMBEDDING_PROVIDER`, `BLOB_PROVIDER`, `QUEUE_PROVIDER`, `AUTH_BEARER_TOKEN`)
- Configure named volumes for MongoDB data, blob store, and queue directory
- Include a health-check for each service; `knoxx-backend` must depend on healthy `mongodb` and `ollama`
- Document the bring-up sequence in `deploy/self-hosted/README.md` (one-page quick-start only)

## Definition of done

- `docker compose up` in `deploy/self-hosted/` starts all services and the backend reaches `/health` within 60 s
- mongot connects to MongoDB and `$vectorSearch` resolves on the knowledge collection
- `.env.example` covers every required variable with a descriptive comment

## Notes

Split from parent epic `knoxx-knowledge-ops-deploy-self-hosted` on 2026-05-30.
