---
uuid: "knoxx-knowledge-labeler-ui-queue-wiring"
title: "Wire KnowledgeLabeler UI to real task queue data"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Wire KnowledgeLabeler UI to real task queue data

> Parent epic: `knoxx-knowledge-ops-shibboleth-lite-labeling`
> Points: 3

## Purpose

Replace stub/mock data in the existing KnowledgeLabeler frontend component with live fetches from the label API, enabling reviewers to work through a real queue of knowledge items awaiting QA labeling.

## Scope

- Identify the existing `KnowledgeLabeler` UI component in `frontend/`
- Replace static fixture data with API calls to the label task queue endpoint (as defined in the parent epic's API contract section)
- Handle loading, empty-queue, and error states in the UI
- Ensure label submission writes back through the API and advances the queue cursor

## Definition of done

- `KnowledgeLabeler` fetches the task queue from the live label API (not a local fixture)
- Submitting a label via the UI persists to the backend and removes the item from the queue
- Frontend TypeScript typechecks pass (`pnpm typecheck` from `frontend/`)

## Notes

Split from parent epic `knoxx-knowledge-ops-shibboleth-lite-labeling` on 2026-05-30.
