---
uuid: "knoxx-cms-typebox-shapes"
title: "CMS TypeBox Shapes — Document and Visibility Gate Type Definitions"
status: incoming
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---

# CMS TypeBox Shapes — Document and Visibility Gate Type Definitions

> Parent epic: `knoxx-knowledge-ops-cms-data-model`
> Points: 2

## Purpose

Define the canonical TypeBox schemas for CMS document records and visibility/publish state so all routes, agents, and ingestion drivers share a single source of truth for shape validation.

## Scope

- Create or extend shape definitions in `backend/src/cljs/knoxx/backend/shape/` (e.g. `cms.cljs`)
- Shapes required: `CmsDocument` (id, tenant, content, visibility enum, publish state machine fields, timestamps), `CmsVisibility` enum (`internal` | `public`), `CmsPublishState` enum (`draft` | `pending` | `published` | `archived`)
- Export compiled TypeBox validators for use in Fastify route schema validation and agent tool inputs
- Ensure shapes follow existing `knoxx.backend.shape.*` conventions reviewed in `knoxx-backend-data-shapes-review`

## Definition of done

- Shape file(s) present under `backend/src/cljs/knoxx/backend/shape/` with all three TypeBox definitions
- `pnpm typecheck` (shadow-cljs compile) passes with zero errors
- Shapes are importable and used by at least the stub route handlers from `knoxx-cms-backend-routes`

## Notes

Split from parent epic `knoxx-knowledge-ops-cms-data-model` on 2026-05-30.
