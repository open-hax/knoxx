# Knowledge Ops — Knoxx Health Route Coherence

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
- backend `/health` requests repeatedly fail with `fetch failed`

This makes the stack look dead even when only some dependencies are degraded, and it blocks `depends_on: service_healthy` semantics across the compose stack.

## Goals

1. Define the intended meaning of Knoxx backend health.
2. Make `/health` and `/health/knoxx` reflect that meaning consistently.
3. Distinguish:
   - process liveness
   - core readiness
   - optional dependency degradation
4. Stop backend health from flapping due to slow or transient upstream fetches unless those upstreams are required for the service contract.

## Non-Goals

1. Redesigning all service health endpoints.
2. Solving all OpenPlanner or Proxx issues in this spec.
3. Reworking frontend behavior.

## Affected files / surfaces

- `orgs/open-hax/knoxx/backend/src/cljs/knoxx/backend/core.cljs`
- `services/knoxx/config/conf.d/default.conf`
- `services/knoxx/config/conf.d/dev.conf.template`
- `services/knoxx/docker-compose.yml`

## Verification

1. `docker compose ps` shows `knoxx-backend` healthy.
2. `curl http://127.0.0.1/health/knoxx` returns `200` when the backend is operational.
3. Health payload names degraded dependencies explicitly instead of collapsing into opaque failure.
4. Compose health checks stop oscillating under normal local dev conditions.

## Definition of done

- Knoxx backend health semantics are documented in code/comments or adjacent docs.
- nginx and backend health surfaces agree.
- Health no longer fails for reasons outside the intended readiness contract.
