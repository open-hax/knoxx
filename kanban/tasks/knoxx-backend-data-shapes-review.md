---
uuid: "knoxx-knoxx-backend-data-shapes-review"
title: "Knoxx Backend Data Shapes Review"
status: review
priority: P2
labels: ["tasks", "5sp"]
created_at: "2026-05-21T00:00:00Z"
source: "specs/tasks/knoxx-backend-data-shapes-review.md"
points: 5
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

## Definition of done

- All explicit shapes identified and catalogued by namespace.
- All implied/inline shapes (ad-hoc maps, destructuring patterns) flagged.
- Target `law.*` / `shape.*` consolidation plan documented.
- Follow-up epic (`knoxx-backend-law-shape-domain-epic.md`) seeded with findings.

## Acceptance criteria

- [ ] Findings document exists with shape inventory and consolidation plan.
- [ ] Follow-up epic references concrete namespaces to create or migrate.
- [ ] No new code — review output is documentation only.

---

**Triage 2026-05-29 (scored, status confirmed review):** Was unscored (`null` points). Scored **5sp** — full 192-file backend read plus consolidation plan output. Status confirmed `review`: the task is in review because the findings document is the deliverable and needs a human pass before the follow-up epic can be seeded. Next step: confirm findings are captured and move to Document, or return to In Progress if the findings write-up is incomplete.
