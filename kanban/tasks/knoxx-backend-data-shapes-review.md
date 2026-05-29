---
uuid: "knoxx-knoxx-backend-data-shapes-review"
title: "Knoxx Backend Data Shapes Review"
status: review
priority: P2
labels: ["tasks"]
created_at: "2026-05-21T00:00:00Z"
source: "specs/tasks/knoxx-backend-data-shapes-review.md"
points: null
category: tasks
---

# Knoxx Backend Data Shapes Review

> Source: `specs/tasks/knoxx-backend-data-shapes-review.md`

Date: 2026-05-21
Status: review
Scope: `backend/src/cljs/knoxx/backend/**`
Follow-up epic: `specs/epics/knoxx-backend-law-shape-domain-epic.md`

## Purpose

Identify explicit, implied, and drift-prone data shapes used across the Knoxx ClojureScript backend, then define the target consolidation into `knoxx.backend.law.*` and `knoxx.backend.shape.*` domain namespaces.

## Review method

This review inspected:

- all 192 files under `backend/src/cljs/knoxx/backend/`
- existing explicit shape/law namespaces under `backend/src/cljs/knoxx/backend/{law,shape}/`
