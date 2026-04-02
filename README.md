# Knoxx

Secure knowledge vault and garden system. A local-first RAG + inference stack with operations UI, document ingestion workflows, and OpenAI-compatible runtime endpoints.

The name comes from Fort Knox - a secure vault for your knowledge. The garden concept draws from Quartz and the garden at the center of the Pentagon.

## Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│   Frontend  │────▶│    Ingestion     │────▶│ OpenPlanner │
│   (React)   │     │   (Clojure)      │     │ (TypeScript)│
│             │◀────│                  │◀────│             │
│   CMS Page  │     │  /api/query/*    │     │ /v1/docs    │
│   Query     │     │  /api/ingestion/*│     │ /v1/search  │
└─────────────┘     └──────────────────┘     └─────────────┘
```

**Data Flow**:
1. Frontend provides operator UI for ingestion, query, CMS, gardens
2. Ingestion worker discovers files, classifies into lakes, writes to OpenPlanner
3. OpenPlanner stores documents/events with FTS + vector search

## Repository Layout

```
knoxx/
├── frontend/       # React + Vite + TypeScript UI
│                   # - CMS, Ingestion, Query, Gardens pages
│                   # - Uses @devel/ui-react components
│
├── backend/        # FastAPI (Python)
│                   # - OpenAI-compatible endpoints
│                   # - llama.cpp control
│                   # - Knoxx RAG/Direct proxy
│
├── ingestion/      # Clojure worker
│                   # - File browser and preview
│                   # - Source and job management
│                   # - Federated FTS query
│                   # - Writes to OpenPlanner
│
└── discord-bot/    # Optional Discord integration
```

## Core Capabilities

- **Inference control**: discover GGUF models, start/stop/warmup `llama-server`
- **OpenAI compatibility**: `/v1/models`, `/v1/chat/completions`, `/v1/embeddings`
- **Document ingestion**: file browser, source management, job queue, progress tracking
- **Knowledge lakes**: files classified into docs, code, config, data
- **Query interface**: federated FTS search across multiple projects
- **Gardens**: curated knowledge spaces

## Quick Start

### 1. OpenPlanner (Data Lake)

```bash
# From devel workspace
cd orgs/open-hax/openplanner
pnpm install
docker compose up -d chroma
cp .env.example .env
pnpm dev
# Runs on :7777
```

### 2. Ingestion Worker

```bash
cd orgs/open-hax/knoxx/ingestion
clj -M:dev
# Runs on :3002
```

### 3. Backend

```bash
cd orgs/open-hax/knoxx/backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 4. Frontend

```bash
cd orgs/open-hax/knoxx/frontend
pnpm install
pnpm dev -- --host 0.0.0.0 --port 5173
```

## API Surface

### Ingestion Worker (Clojure)

| Endpoint | Description |
|----------|-------------|
| `GET /health` | Health check |
| `GET /api/ingestion/browse` | File browser |
| `GET /api/ingestion/file` | File preview |
| `GET /api/ingestion/sources` | List sources |
| `POST /api/ingestion/sources` | Create source |
| `GET /api/ingestion/jobs` | List jobs |
| `POST /api/ingestion/jobs` | Create/start job |
| `POST /api/query/search` | Federated FTS |
| `POST /api/query/answer` | Grounded summary |
| `GET /api/query/gardens` | List gardens |

### Backend (Python)

| Endpoint | Description |
|----------|-------------|
| `GET /v1/models` | OpenAI models |
| `POST /v1/chat/completions` | Chat completion |
| `POST /v1/embeddings` | Embeddings |
| `GET /api/knoxx/health` | Knoxx health |
| `POST /api/knoxx/chat` | Knoxx RAG chat |
| `POST /api/knoxx/direct` | Knoxx direct chat |

## Environment

Key environment variables:

```bash
# Ingestion
OPENPLANNER_URL=http://localhost:7777
OPENPLANNER_API_KEY=xxx
WORKSPACE_PATH=/home/err/devel

# Backend
KNOXX_BASE_URL=http://localhost:3001
KNOXX_API_KEY=xxx
MODELS_DIR=/path/to/gguf/models
```

## License

GPL-3.0-only
