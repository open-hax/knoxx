---
uuid: "knoxx-cms-ai-draft-route"
title: "CMS AI Draft Route — Agent-Assisted Document Draft Generation Endpoint"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# CMS AI Draft Route — Agent-Assisted Document Draft Generation Endpoint

> Parent epic: `knoxx-knowledge-ops-cms-data-model`
> Points: 3

## Purpose

Expose a route that accepts a topic or seed prompt and returns an agent-generated document draft, materialising the "agent-aware" dimension of the CMS layer described by the parent epic.

## Scope

- Add `POST /cms/documents/draft` route within the CMS vertical slice
- Route accepts `{ topic: string, context?: string }` body (validated via TypeBox shapes from `knoxx-cms-typebox-shapes`)
- Delegates generation to the existing agent runner (`knoxx.backend.infra.agent.runner`) with an appropriate system prompt and tool set
- Returns a `CmsDocument` stub with `publishState: "draft"` and `visibility: "internal"` populated from the agent output
- Apply authZ guard; honour tenant scope so generated content is scoped to the requesting tenant

## Definition of done

- `POST /cms/documents/draft` returns HTTP 200 with a valid `CmsDocument` JSON body (matching TypeBox schema) when called with a non-empty `topic`
- Unauthenticated requests receive HTTP 401; missing `topic` receives HTTP 422
- `pnpm lint` and shadow-cljs compile pass with no new warnings or errors

## Notes

Split from parent epic `knoxx-knowledge-ops-cms-data-model` on 2026-05-30.
