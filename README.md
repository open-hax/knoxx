# Knoxx Platform

Knoxx and LLM Model Lab are now treated as one platform: a local-first RAG + inference stack with operations UI, document ingestion workflows, and OpenAI-compatible runtime endpoints.

## What this repo includes

- FastAPI backend for local `llama.cpp` control, telemetry, run logging, and OpenAI-compatible APIs
- React/Vite frontend with classic lab pages and `/next/*` Knoxx operations console
- Knoxx bridge/proxy endpoints for RAG and direct chat routing
- Multi-database document management, ingestion progress, restart/resume controls, and history
- Optional Discord bot for chat and status commands in servers
- Local deployment path for new environments (models, runs, env config, build steps)

## Core capabilities

- **Inference control:** discover GGUF models, start/stop/warmup `llama-server`, tune sampling settings
- **OpenAI compatibility:** `GET /v1/models`, `POST /v1/chat/completions`, `POST /v1/embeddings`
- **Knoxx integration:** provider switching (`Local llama.cpp`, `Knoxx RAG`, `Knoxx Direct`) and Knoxx health checks
- **Ops console (`/next/*`):** dashboard telemetry, retrieval diagnostics, document workflows, and ingestion history
- **Document database profiles:** create/switch/rename/delete profiles, forum mode toggle, local/public docs links, session-private access
- **Ingestion resiliency:** progress polling, throughput + ETA, resumable forum checkpoints, stale-run force-fresh restart

## Architecture

User -> Frontend (`frontend/`) -> Backend (`backend/`) -> `llama.cpp` + embeddings -> Knoxx (optional bridge mode)

Notes:
- `llama.cpp` handles chat inference for GGUF models.
- Embeddings use the backend embedding path (`bge-m3` by default).
- Knoxx can consume Model Lab through OpenAI-compatible endpoints.

## Repository layout

- `backend/` FastAPI app, API routers, runtime manager, metrics, OpenAI endpoints
- `frontend/` React + Vite + TypeScript UI (Lab + `/next/*` operations pages)
- `models/` local GGUF model files
- `runs/` JSONL event logs and SQLite run index
- `scripts/dev.sh` quick local dev launcher
- `discord-bot/` optional Discord integration service
- `docker-compose.yml` optional containerized stack

## Quick start (local)

### 1) Backend

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 2) Frontend

```bash
cd frontend
npm install
npm run dev -- --host 0.0.0.0 --port 5173
```

Frontend: `http://localhost:5173`  
Backend: `http://localhost:8000`

### 3) Optional dependencies for full Knoxx flow

- Qdrant for retrieval storage: `docker run -p 6333:6333 qdrant/qdrant`
- Knoxx service running (if using proxy/provider switch modes)

### 4) Optional Discord bot

```bash
cd discord-bot
npm install
cp .env.example .env
npm run register
npm run dev
```

By default, the bot targets Model Lab proxy mode at `http://localhost:8000/api/knoxx`.

## Environment configuration

Copy `backend/.env.example` to `backend/.env`.

Most important keys:

- `MODELS_DIR`, `RUNS_DIR`, `RUNS_DB_PATH`
- `LLAMA_SERVER_PATH`, `LLAMA_PORT`, `DEFAULT_THREADS`, `DEFAULT_CTX`, `DEFAULT_GPU_LAYERS`
- `MODEL_LAB_OPENAI_API_KEY`
- `EMBED_MODE`, `EMBED_MODEL`, `EMBED_DIM`
- `KNOXX_BASE_URL`, `KNOXX_API_KEY`, `KNOXX_ADMIN_URL`

Example Knoxx bridge values:

```env
KNOXX_BASE_URL=http://localhost:3001
KNOXX_API_KEY=<KNOXX_API_KEY>
KNOXX_ADMIN_URL=http://localhost:5173
```

## API surface

### Core backend

- `GET /health`
- `GET /api/models`
- `POST /api/server/start`
- `POST /api/server/stop`
- `GET /api/server/status`
- `GET /api/server/health`
- `POST /api/server/warmup`
- `POST /api/chat`
- `GET /api/config`
- `GET /api/runs`
- `GET /api/runs/{run_id}`
- `GET /api/runs/{run_id}/export`

### Knoxx bridge

- `GET /api/knoxx/health`
- `POST /api/knoxx/chat`
- `POST /api/knoxx/direct`

### OpenAI-compatible

- `GET /v1/models`
- `POST /v1/chat/completions`
- `POST /v1/embeddings`

### Realtime stream

- `WS /ws/stream` channels: `tokens`, `stats`, `console`, `events`

## Fresh environment bootstrap

```bash
git clone https://github.com/mojomast/knoxx.git
cd knoxx

# backend
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env

# frontend
cd ../frontend
npm install
```

Before deploy/start, verify:

- `llama-server` exists and is reachable via `LLAMA_SERVER_PATH` (or PATH)
- `MODELS_DIR` points to real GGUF files
- Knoxx env values are set when bridge mode is enabled
- Qdrant is available for retrieval-enabled workflows

## Build and deploy

- Frontend production build: `cd frontend && npm run build`
- Backend production run: `cd backend && uvicorn app.main:app --host 0.0.0.0 --port 8000`
- Docker compose option: `docker compose up --build`
- Docker compose with Discord bot: `docker compose --profile with-discord up --build`

## Troubleshooting

- **No models listed:** check `MODELS_DIR` and `*.gguf` availability
- **Server start fails:** verify model path and file permissions
- **GPU telemetry unavailable:** install NVIDIA drivers + `pynvml`
- **No token stream:** verify `WS /ws/stream` connectivity
- **Knoxx errors in UI:** verify `KNOXX_BASE_URL` and `KNOXX_API_KEY`

## Validation checklist

1. Start backend + frontend.
2. Confirm `GET /api/models` returns models.
3. Start/warmup a model from UI.
4. Send a chat prompt and confirm streamed tokens.
5. Switch provider to Knoxx and verify bridge health/chat calls.
6. Upload docs, run ingestion, verify history rows and progress behavior.
