---
uuid: "knoxx-kms-cms-clojure-service"
title: "kms-cms — Clojure CMS Service (Phase 3 Bootstrap)"
status: accepted
priority: P2
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 5
category: tasks
---
# kms-cms — Clojure CMS Service (Phase 3 Bootstrap)

> Parent epic: `knoxx-knowledge-ops-clojure-backend-migration`
> Points: 5

## Purpose

Bootstrap the `kms-cms` Clojure service as the replacement for the Python CMS backend (`packages/futuresight-kms/python/km_labels/routers/cms.py`), implementing document CRUD and visibility state transitions backed by OpenPlanner and PostgreSQL.

## Scope

- Scaffold a new Clojure/Ring+Jetty service at `orgs/open-hax/knoxx/cms/` following the same project structure as the existing `orgs/open-hax/knoxx/ingestion/` service
- Implement core routes mirroring the Python cms.py feature surface: `GET /api/cms/documents`, `POST /api/cms/documents`, `PUT /api/cms/documents/:id`, `DELETE /api/cms/documents/:id`
- Implement visibility state machine transitions: draft → internal → public → archived, with publication events written to OpenPlanner using the same `source: kms-ingestion` contract shape
- Wire Proxx for AI draft generation via `POST /api/cms/documents/:id/draft`
- Reference `orgs/mojomast/ragussy/frontend/src/pages/CmsPage.tsx` to ensure route parity with what the existing CMS UI expects

## Definition of done

- Service starts cleanly with `clj -M:run` and all routes return HTTP 200/201/204 responses for valid inputs against a dev OpenPlanner + PostgreSQL instance
- Visibility transitions persist correctly and corresponding OpenPlanner events are written with the correct `kind` and `source_ref` fields
- `clj -M:test` passes with unit coverage for the state machine transitions
- `clj-kondo` reports no lint errors on the new namespace tree

## Notes

Split from parent epic `knoxx-knowledge-ops-clojure-backend-migration` on 2026-05-30.
