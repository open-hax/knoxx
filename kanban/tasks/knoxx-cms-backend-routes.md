---
uuid: "knoxx-cms-backend-routes"
title: "CMS Backend Routes — Document CRUD and Publish Endpoints"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# CMS Backend Routes — Document CRUD and Publish Endpoints

> Parent epic: `knoxx-knowledge-ops-cms-data-model`
> Points: 3

## Purpose

Implement the Fastify HTTP routes that expose the CMS document lifecycle — create, read, update, delete, and publish state transitions — forming the API contract defined by the parent epic.

## Scope

- Add route handlers under `backend/src/cljs/knoxx/backend/domain/cms/` (or equivalent vertical slice)
- Routes to implement: `POST /cms/documents`, `GET /cms/documents/:id`, `PATCH /cms/documents/:id`, `DELETE /cms/documents/:id`, `POST /cms/documents/:id/publish`, `POST /cms/documents/:id/unpublish`
- Wire routes into the existing Fastify server registration in `backend/src/cljs/knoxx/backend/infra/http.cljs` (or equivalent)
- Apply existing policy/contract authZ guard (`knoxx.backend.law`) to each route

## Definition of done

- All six routes respond with correct HTTP status codes and JSON bodies under test
- Auth guard rejects unauthenticated and unauthorized requests on every route
- No new lint warnings (`pnpm lint` from `backend/`) and shadow-cljs compiles cleanly

## Notes

Split from parent epic `knoxx-knowledge-ops-cms-data-model` on 2026-05-30.
