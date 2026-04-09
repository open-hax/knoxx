# The Lake — Self-Hosted Deployment Spec

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
| Database (labels) | **PostgreSQL** | — |

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                  Docker Compose                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │ CMS API  │  │ Label API│  │ Widget   │               │
│  │ (Node)   │  │ (Python) │  │ Proxy    │               │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘               │
│       │              │              │                     │
│       └──────────────┼──────────────┘                     │
│                      │                                    │
│  ┌───────────────────▼────────────────────┐              │
│  │         Provider Abstraction Layer      │              │
│  └──┬──────────┬──────────┬──────────┬────┘              │
│     │          │          │          │                    │
└─────┼──────────┼──────────┼──────────┼────────────────────┘
      │          │          │          │
      ▼          ▼          ▼          ▼
 MongoDB    Ollama     MongoDB    Filesystem
 + mongot   (embed     (docs,     (blobs,
 (vectors   + chat)    metadata)  SHA-256)
  + FTS)
      │
      ▼
 PostgreSQL
 (labels, tenants)
```

---

## Embedding Tier Configuration

| Tier | Ollama Model | Dimensions | VRAM |
|------|-------------|------------|------|
| Hot | qwen3-embedding:0.6b | variable | ~2GB |
| Warm | qwen3-embedding:4b | variable | ~8GB |
| Compact | qwen3-embedding:8b | variable | ~16GB |

**GPU required** for warm and compact tiers. See [provider hardware spec](knowledge-ops-provider-hardware.md) for GPU recommendations.

---

## Docker Compose

```yaml
services:
  mongodb:
    image: mongodb/mongodb-community-server:8.2.0-ubi9
    command: mongod --replSet rs0
    ports:
      - "27017:27017"
    volumes:
      - mongodb-data:/data/db

  mongot:
    image: mongodb/mongodb-community-search:0.53.1
    ports:
      - "27028:27028"
    depends_on:
      mongodb:
        condition: service_healthy

  mongo-init:
    image: mongodb/mongodb-community-server:8.2.0-ubi9
    depends_on:
      mongodb:
        condition: service_healthy
    entrypoint: |
      mongosh --host mongodb --eval '
        rs.initiate({ _id: "rs0", members: [{ _id: 0, host: "mongodb:27017" }] })
      ' || true

  ollama:
    image: ollama/ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama-data:/root/.ollama
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]

  postgres:
    image: postgres:16
    environment:
      POSTGRES_USER: kms
      POSTGRES_PASSWORD: kms
      POSTGRES_DB: knowledge_lake
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  app:
    build: .
    depends_on:
      mongodb:
        condition: service_healthy
      postgres:
        condition: service_healthy
    environment:
      SEARCH_PROVIDER: mongodb
      EMBEDDING_PROVIDER: ollama
      STORAGE_PROVIDER: mongodb
      BLOB_PROVIDER: filesystem
      MONGO_URL: mongodb://mongodb:27017/knowledge-lake?replicaSet=rs0
      OLLAMA_BASE_URL: http://ollama:11434
      DATABASE_URL: postgresql://kms:kms@postgres:5432/knowledge_lake
    ports:
      - "3000:3000"

volumes:
  mongodb-data:
  ollama-data:
  postgres-data:
```

---

## Cost Estimate

| Component | Cost |
|-----------|------|
| VPS with GPU (Vast.ai RTX 3090) | ~$50-180/month |
| VPS without GPU (for demo/hot-tier only) | ~$10-30/month |
| Your own hardware | $0/month (electricity only) |

**Total: $10-180/month** depending on GPU requirements.

---

## Environment Variables

```env
SEARCH_PROVIDER=mongodb
EMBEDDING_PROVIDER=ollama
STORAGE_PROVIDER=mongodb
BLOB_PROVIDER=filesystem
QUEUE_PROVIDER=jsonl
AUTH_PROVIDER=bearer-token

MONGO_URL=mongodb://localhost:27017/knowledge-lake?replicaSet=rs0
OLLAMA_BASE_URL=http://localhost:11434
DATABASE_URL=postgresql://kms:kms@localhost:5432/knowledge_lake
KNOWLEDGE_LAKE_API_KEY=change-me
BLOB_PATH=./blobs
```

---

## Status

Specified 2026-04-01. Self-hosted provider implementation.
