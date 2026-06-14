---
uuid: "knoxx-knowledge-ops-pass4-ui-vocabulary"
title: "Knowledge-ops consistency pass 4: UI vocabulary normalization"
status: breakdown
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---
# Knowledge-ops consistency pass 4: UI vocabulary normalization

> Parent epic: `knoxx-knowledge-ops-consistency-review`
> Points: 2

## Purpose

Standardize UI and UX terminology across all knowledge-ops specs so that component names, route paths, and user-facing labels are consistent and match the terms used in the frontend codebase.

## Scope

- Identify divergent UI terms across specs (e.g. "workbench" vs "workspace", "garden" vs "lake", "panel" vs "drawer")
- Choose the canonical term for each concept based on the frontend source under `frontend/src/`
- Update spec prose, headings, and examples to use canonical terms throughout

## Definition of done

- A short vocabulary reference list (inline in a spec or comment) documents the chosen canonical UI terms
- All 34 specs use the canonical term for each UI concept with no in-spec synonyms left unresolved

## Notes

Split from parent epic `knoxx-knowledge-ops-consistency-review` on 2026-05-30.
