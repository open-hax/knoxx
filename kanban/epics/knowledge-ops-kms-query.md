---
uuid: "knoxx-knowledge-ops-kms-query"
title: "Knowledge Ops — KMS Query Service"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.388Z"
source: "specs/epics/knowledge-ops-kms-query.md"
points: null
category: epics
---

# Knowledge Ops — KMS Query Service

> Source: `specs/epics/knowledge-ops-kms-query.md`

## Current Canonical Reading

- state: current retrieval-oriented query slice
- canonical implementation path: `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/api/routes.clj`
- canonical nginx exposure path: `services/knoxx/config/conf.d/default.conf`
- note: Knoxx's primary interactive chat/runtime surface is now `/api/knoxx/*`; this spec describes the still-useful `/api/query/*` retrieval and synthesis surface

> *One Clojure query surface over many lakes.*

---

## Purpose

Define the `kms-query` surface currently implemented inside the `kms-ingestion` Clojure service. This is the first provider-independent query layer over:
- `devel-*` lakes
