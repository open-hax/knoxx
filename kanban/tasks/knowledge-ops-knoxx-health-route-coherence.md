---
uuid: "knoxx-knowledge-ops-knoxx-health-route-coherence"
title: "Knowledge Ops — Knoxx Health Route Coherence"
status: incoming
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
Status: next
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
