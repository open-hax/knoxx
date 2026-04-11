# Knowledge Ops — Deployment Spec

## Current Canonical Reading

- state: legacy deployment sketch, partially superseded
- current canonical live stack for Knoxx on this machine is `services/knoxx`
- interpret this document as a historical futuresight-kms / ragussy deployment plan unless a future cleanup rewrites it against the live Knoxx stack
- current runtime naming to prefer elsewhere:
  - `services/knoxx`
  - `orgs/open-hax/knoxx/backend`
  - `orgs/open-hax/knoxx/frontend`
  - `orgs/open-hax/knoxx/ingestion`

> *Nginx routes, Docker Compose services, env vars, health checks. Everything you need to go from code to running stack.*

---

## Purpose

Define the deployment configuration for the full five-layer stack: Nginx routing, Docker Compose services, environment variables, and health checks.

---

## Nginx Configuration

### New location blocks to add to the nginx config

Historical target in this spec:
- `services/futuresight-kms/config/nginx.conf`

Current Knoxx analog:
- `services/knoxx/config/conf.d/default.conf`

```nginx
# ── Widget API (Layer 1) ──────────────────────────────
location /api/widget/ {
    proxy_pass         http://km-labels:3002/api/widget/;
    proxy_set_header   Host $host;
    proxy_set_header   X-Real-IP $remote_addr;
    proxy_set_header   X-Tenant-Id $http_x_tenant_id;
    proxy_read_timeout 60s;
}

# ── CMS API (Layer 2) ─────────────────────────────────
location /api/cms/ {
    proxy_pass         http://km-labels:3002/api/cms/;
    proxy_set_header   Host $host;
    proxy_set_header   X-Real-IP $remote_addr;
    proxy_set_header   X-Tenant-Id $http_x_tenant_id;
    proxy_read_timeout 60s;
}

# ── Widget static assets ──────────────────────────────
location /widget/ {
    alias /usr/share/nginx/html/widget/;
    try_files $uri $uri/ =404;
}

# ── Existing routes (unchanged) ───────────────────────
# /api/km-labels/ → km-labels:3002
# /api/export/    → km-labels:3002
# /api/           → ragussy-backend:8000
# /v1/            → ragussy-backend:8000
# /health/*       → respective services
# /               → ragussy-frontend
# /stack/         → landing page
```

---

## Docker Compose Changes

### New services (none — km_labels already exists)

No new Docker services needed. The CMS and widget proxy are new routers inside the existing km_labels Python service.

### Modified services

**km_labels** service needs these additions:

```yaml
km-labels:
  # ... existing config ...
  environment:
    # Existing:
    DATABASE_URL: postgresql://kms:kms@postgres:5432/knoxx
    REDIS_URL: redis://redis:6379
    KM_LABELS_API_KEY: ${KM_LABELS_API_KEY:-change-me-km-labels}
    # New for CMS:
    RAGUSSY_BASE_URL: ${RAGUSSY_BASE_URL:-http://ragussy-backend:8000}
    RAGUSSY_API_KEY: ${RAGUSSY_API_KEY:-change-me-ragussy}
    QDRANT_URL: ${QDRANT_URL:-http://qdrant:6333}
    EMBED_MODEL: ${EMBED_MODEL:-BAAI/bge-m3}
    EMBED_DIM: ${EMBED_DIM:-1024}
```

**nginx** service needs widget static assets baked into the image:

```dockerfile
# In Dockerfile.nginx, add:
COPY widget/ /usr/share/nginx/html/widget/
```

### Network topology (unchanged)

```
kms-internal (bridge): nginx, ragussy-backend, ragussy-frontend, shibboleth-backend, km-labels, qdrant, postgres, redis
ai-infra (external): ragussy-backend, shibboleth-backend
```

---

## Environment Variables

### Complete `.env` for futuresight-kms stack

```env
# ── Ports ─────────────────────────────────────────────
NGINX_HTTP_PORT=80
SHIBBOLETH_HTTP_PORT=8097

# ── API Keys ──────────────────────────────────────────
RAGUSSY_API_KEY=change-me-ragussy
KM_LABELS_API_KEY=change-me-km-labels
PROXY_AUTH_TOKEN=${OPEN_HAX_OPENAI_PROXY_AUTH_TOKEN:-}

# ── Database ──────────────────────────────────────────
POSTGRES_USER=kms
POSTGRES_PASSWORD=kms
POSTGRES_DB=knoxx

# ── Embedding ─────────────────────────────────────────
EMBED_MODE=local
EMBED_MODEL=BAAI/bge-m3
EMBED_DIM=1024

# ── Models ────────────────────────────────────────────
PROXX_BASE_URL=http://proxx:8789
PROXX_DEFAULT_MODEL=glm-4.7-flash
PROXX_AUTH_TOKEN=${OPEN_HAX_OPENAI_PROXY_AUTH_TOKEN:-}

# ── Internal service URLs ─────────────────────────────
RAGUSSY_BASE_URL=http://ragussy-backend:8000
SHIBBOLETH_BASE_URL=http://shibboleth-backend:8788
QDRANT_URL=http://qdrant:6333

# ── Path overrides (local dev) ────────────────────────
# RAGUSSY_BACKEND_PATH=../../orgs/mojomast/ragussy/backend
# RAGUSSY_FRONTEND_PATH=../../orgs/mojomast/ragussy/frontend
# SHIBBOLETH_BACKEND_PATH=../../orgs/octave-commons/shibboleth
# KM_LABELS_PATH=../../packages/futuresight-kms
```

---

## Health Checks

| Service | Endpoint | Check | Interval |
|---------|----------|-------|----------|
| nginx | `nginx -t` | Config validity | 30s |
| ragussy-backend | `GET /health` | HTTP 200 | 30s |
| ragussy-frontend | `test -f /usr/share/nginx/html/index.html` | File exists | 30s |
| shibboleth-backend | `echo > /dev/tcp/127.0.0.1/8788` | Port open | 30s |
| km-labels | `GET /health` | HTTP 200 + DB reachable | 30s |
| qdrant | `echo > /dev/tcp/127.0.0.1/6333` | Port open | 10s |
| postgres | `pg_isready` | Ready for connections | 10s |
| redis | `redis-cli ping` | PONG | 10s |

### km_labels health endpoint (enhanced)

```python
@router.get("/health")
async def health():
    """Check all dependencies."""
    checks = {
        "postgres": await check_postgres(),
        "ragussy": await check_ragussy(),
        "qdrant": await check_qdrant(),
    }
    all_ok = all(checks.values())
    return JSONResponse(
        status_code=200 if all_ok else 503,
        content={"status": "ok" if all_ok else "degraded", "checks": checks}
    )
```

---

## Build Order

```bash
# 1. Start infrastructure
docker compose up -d postgres redis qdrant

# 2. Wait for health
docker compose exec postgres pg_isready
docker compose exec qdrant curl -s http://localhost:6333/healthz

# 3. Start application services
docker compose up -d ragussy-backend ragussy-frontend shibboleth-backend km-labels

# 4. Start proxy
docker compose up -d nginx

# 5. Seed
docker compose exec km-labels python /app/scripts/seed_devel_tenant.py

# 6. Verify
curl http://localhost/health/ragussy
curl http://localhost/health/km-labels
curl http://localhost/health/qdrant
curl http://localhost/api/cms/documents?tenant_id=devel
```

---

## Development Mode

For local development without Docker:

```bash
# Terminal 1: Infrastructure
docker compose up postgres redis qdrant

# Terminal 2: Ragussy backend
cd orgs/mojomast/ragussy/backend
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000

# Terminal 3: km_labels (includes CMS + widget proxy)
cd packages/futuresight-kms/python
pip install -r requirements.txt
DATABASE_URL=postgresql://kms:kms@localhost:5432/knoxx \
  RAGUSSY_BASE_URL=http://localhost:8000 \
  QDRANT_URL=http://localhost:6333 \
  uvicorn km_labels.app:create_app --reload --port 3002

# Terminal 4: Seed
cd services/futuresight-kms
python scripts/seed_devel_tenant.py

# Test
curl http://localhost:3002/api/cms/documents?tenant_id=devel
curl -X POST http://localhost:3002/api/widget/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: devel" \
  -d '{"message": "What is Promethean?"}'
```

---

## Files to Create/Modify

| File | Action | Purpose |
|------|--------|---------|
| `services/futuresight-kms/config/nginx.conf` | Modify | Add `/api/widget/`, `/api/cms/`, `/widget/` locations |
| `services/futuresight-kms/Dockerfile.nginx` | Modify | Copy widget assets into nginx image |
| `services/futuresight-kms/docker-compose.yml` | Modify | Add env vars to km_labels service |
| `services/futuresight-kms/.env.example` | Modify | Add new env vars |
| `services/futuresight-kms/config/html/widget/` | Create | Widget JS bundle directory |

## Files to Reference

| File | Why |
|------|-----|
| `services/futuresight-kms/config/nginx.conf` | Existing routing config |
| `services/futuresight-kms/docker-compose.yml` | Existing service definitions |
| `services/futuresight-kms/Dockerfile.nginx` | Existing nginx image build |
| `packages/futuresight-kms/python/km_labels/app.py` | Existing FastAPI factory — add cms + widget routers |
| `packages/futuresight-kms/Dockerfile` | Existing km_labels image — add scripts/ copy |

---

## Status

Specified 2026-04-01. Blocking questions resolved.
