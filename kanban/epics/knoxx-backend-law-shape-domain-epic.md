---
uuid: "knoxx-knoxx-backend-law-shape-domain-epic"
title: "Knoxx Backend Law/Shape Domain Epic"
status: in_progress
priority: P0
labels: ["epics", "34sp"]
created_at: "2026-05-21T00:00:00Z"
source: "specs/epics/knoxx-backend-law-shape-domain-epic.md"
points: 34
category: epics
---
# Knoxx Backend Law/Shape Domain Epic

> Source: `specs/epics/knoxx-backend-law-shape-domain-epic.md`
> Points: 34

Date: 2026-05-21
Status: next
Parent review: `specs/tasks/knoxx-backend-data-shapes-review.md`
Story points: 34

## Purpose

Turn the implicit data contracts spread across `backend/src/cljs/knoxx/backend/**` into explicit, reusable domain namespaces under:

- `backend/src/cljs/knoxx/backend/shape/`
- `backend/src/cljs/knoxx/backend/law/`

## Problem

Knoxx backend data currently crosses many boundaries as plain maps with locally implied shape:
