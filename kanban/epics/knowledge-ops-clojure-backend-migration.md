---
uuid: "knoxx-knowledge-ops-clojure-backend-migration"
title: "Knowledge Ops — Clojure Backend Migration"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.382Z"
source: "specs/epics/knowledge-ops-clojure-backend-migration.md"
points: null
category: epics
---

# Knowledge Ops — Clojure Backend Migration

> Source: `specs/epics/knowledge-ops-clojure-backend-migration.md`

## Current Canonical Reading

- state: current migration spec with partially landed implementation
- canonical backend target today: `orgs/open-hax/knoxx/backend` (shadow-cljs + Fastify)
- canonical ingestion/query donor now lives at: `orgs/open-hax/knoxx/ingestion`
- `ragussy` references remain useful as donor context only

> *Keep the frontend. Replace the spine.*

---

## Purpose

Define the migration from the current Python/Ragussy-centered backend shape to a Clojure/OpenPlanner-centered backend shape.

