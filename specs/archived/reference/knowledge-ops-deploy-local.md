# The Lake — Local/Dev Deployment Spec

> *Everything on localhost. No GPU required. For demos, development, and testing.*

---

## Provider Mapping

| Logical Component | Implementation | Config Key |
|-------------------|---------------|------------|
| Search (vector + FTS) | **ChromaDB** (in-process or Docker) | `SEARCH_PROVIDER=chromadb` |
| Embeddings | **Ollama local** (nomic-embed-text or qwen3-embedding:0.6b) | `EMBEDDING_PROVIDER=ollama` |
| Structured storage | **DuckDB** (file-based) or **SQLite** | `STORAGE_PROVIDER=duckdb` |
| Blob storage | **Local filesystem** | `BLOB_PROVIDER=filesystem` |
| Job queue | **In-process queue** (no persistence) | `QUEUE_PROVIDER=memory` |
| Auth | **None** (dev mode) | `AUTH_PROVIDER=none` |
| App hosting | **Local process** (tsx / uvicorn) | — |
| Database (labels) | **SQLite** (file-based) | — |

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                  Local Process                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │ CMS API  │  │ Label API│  │ Widget   │               │
│  │ :3000    │  │ :3001    │  │ :3002    │               │
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
 ChromaDB   Ollama     DuckDB     Filesystem
 (local)    (local)    (file)     (./blobs/)
      │
      ▼
 SQLite
 (labels)
```

---

## Embedding Configuration (no GPU)

| Tier | Model | Dimensions | RAM | Speed |
|------|-------|------------|-----|-------|
| Hot | nomic-embed-text:latest | 768 | ~1GB | Fast (CPU OK) |
| Warm | qwen3-embedding:0.6b | variable | ~2GB | Moderate (CPU OK) |
| Compact | qwen3-embedding:0.6b | variable | ~2GB | Same as warm for local dev |

**No GPU required.** The 0.6b model runs fine on CPU. Slower than GPU, but adequate for development and demos with small corpora.

---

## Quick Start

```bash
# 1. Start infrastructure
docker compose -f docker-compose.dev.yml up -d chromadb ollama

# 2. Pull embedding model
ollama pull nomic-embed-text:latest

# 3. Start app
cd packages/knowledge-lake
SEARCH_PROVIDER=chromadb EMBEDDING_PROVIDER=ollama STORAGE_PROVIDER=duckdb \
  npx tsx src/index.ts

# 4. Seed demo data
npx tsx scripts/seed-demo.ts
```

---

## Docker Compose (dev)

```yaml
services:
  chromadb:
    image: chromadb/chroma
    ports:
      - "8000:8000"
    volumes:
      - chromadb-data:/chroma/chroma

  ollama:
    image: ollama/ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama-data:/root/.ollama

volumes:
  chromadb-data:
  ollama-data:
```

---

## Cost

$0. Runs on your existing machine.

---

## Environment Variables

```env
SEARCH_PROVIDER=chromadb
EMBEDDING_PROVIDER=ollama
STORAGE_PROVIDER=duckdb
BLOB_PROVIDER=filesystem
QUEUE_PROVIDER=memory
AUTH_PROVIDER=none

CHROMA_URL=http://localhost:8000
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_EMBED_MODEL=nomic-embed-text:latest
DATABASE_URL=sqlite://./data/labels.db
BLOB_PATH=./blobs
```

---

## Status

Specified 2026-04-01. Local/dev provider implementation.
