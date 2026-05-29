---
uuid: "knoxx-knowledge-ops-cms-data-model"
title: "Knowledge Ops — CMS Data Model Spec"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.383Z"
source: "specs/epics/knowledge-ops-cms-data-model.md"
points: null
category: epics
---

# Knowledge Ops — CMS Data Model Spec

> Source: `specs/epics/knowledge-ops-cms-data-model.md`

> *The document is the atom. Visibility is the gate. Publish is the sync.*

---

## Purpose

Define the data model, state machine, and API contract for the agent-aware CMS layer that controls the boundary between internal knowledge and public-facing content.

## Canonical Status

This spec originally modeled the CMS as a separate Postgres document store that syncs into a vector store.

That model is now considered **transitional and architecturally broken**.

### Broken assumption
