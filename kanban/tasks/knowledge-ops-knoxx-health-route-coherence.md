---
uuid: "knoxx-knowledge-ops-knoxx-health-route-coherence"
title: "Knowledge Ops — Knoxx Health Route Coherence"
status: done
priority: P1
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/tasks/knowledge-ops-knoxx-health-route-coherence.md"
points: 3
category: tasks
---

# Knowledge Ops — Knoxx Health Route Coherence

> Source: `specs/tasks/knowledge-ops-knoxx-health-route-coherence.md`
> Parent: `knowledge-ops-graph-memory-reconciliation.md`
> Points: 3

Date: 2026-04-05
Status: done
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 3

## Purpose

Make Knoxx health reporting truthful, stable, and useful in the local stack.

## Problem

In the current local deploy:

- `knoxx-backend` is running but unhealthy
- `GET /health/knoxx` through nginx returns `503`

---

**Triage 2026-05-28 (verified — stays incoming, NOT done):** Still open. Code read of `backend/.../infra/routes/app.cljs`: `/health` (≈726–740) and `/api/data/health` (≈990–1025) DO real dependency fan-out (proxx, openplanner, ingestion, graph-weaver, etc.). But `/api/knoxx/health` (≈1150–1160) is a **hardcoded stub** that always returns 200 `status: "ok"` regardless of subsystem state — the incoherence this card is about. Fix lives there. Leaving as incoming.

---

**Done 2026-05-29:** Replaced the hardcoded `api-knoxx-health!` stub in `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` with a real implementation. The route now fans out to `proxx-client/health!` and `openplanner-client/health!` exactly like the `/health` route, reflecting actual dependency states.

- New private handlers `knoxx-health-ok` and `knoxx-health-err` were added next to the existing `health-deps-ok`/`health-deps-err` helpers.
- Response is truthful: HTTP **200** only when every configured dependency is reachable (and at least one dependency is configured), **503** otherwise. The `:reachable`, `:status_code`, and `:details.status` fields now mirror the real outcome instead of always `true`/`200`/`"ok"`.
- The original response shape (`reachable`, `configured`, `base_url`, `status_code`, `details{mode,status,project,collection}`) is preserved for the existing frontend consumer (`frontend/src/lib/api/runtime.ts knoxxHealth`); a `details.dependencies` block (per-dep `configured`/`reachable`/`status_code`/`detail`) was added for diagnostics.
- Unconfigured deps no longer falsely flip the route healthy — a configured-but-unreachable dep forces 503.

Verification:
- `pnpm -C backend typecheck` (shadow-cljs compile server): **Build completed. 0 warnings**.
- `pnpm -C backend lint` (clj-kondo): **errors: 0** (only pre-existing repo-wide `.then/.catch` interop-boundary and test-file warnings remain; the new code uses the same documented promise-chain pattern as the neighboring `health!` route).
- Logic smoke test confirmed the 200/503 decision matrix across both-healthy, single-dep-down, none-configured, and single-dep-only cases (all PASS).
- Live `curl http://localhost:8000/api/knoxx/health` could NOT be run: the `knoxx-backend` PM2 process is in a pre-existing crash loop (`knoxx.backend.infra.routes.studio.studio_list_playlists_BANG_ is not a function` during `register-http-routes!`, plus shadow-cljs dev server at :9630 not connected) — unrelated to this card. Per CLAUDE.md, PM2 was not restarted. The freshly recompiled `dist-dev` output contains the new `knoxx_health_ok`/`knoxx_health_err` functions.
