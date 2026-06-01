---
uuid: "knoxx-canvas-tool-backend-handler"
title: "Canvas Tool Backend Handler"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Canvas Tool Backend Handler

> Parent epic: `knoxx-unified-workplace-pattern`
> Points: 3

## Purpose

Implement the `canvas` tool on the agent backend so the agent can read, append, replace, insert, and query selections in the canvas across all workplaces (Chat and Editor).

## Scope

- Add a `canvas` tool namespace under `backend/src/cljs/knoxx/backend/tools/` implementing the `CanvasTool` interface (`read`, `append`, `replace`, `insert`, `getSelection`)
- Register the tool in the agent tool vector in `infra/agent/runner`
- Connect the tool to the frontend canvas state via the existing WebSocket bridge so mutations are reflected live in the UI
- Write unit tests covering each operation

## Definition of done

- The agent can call `canvas/read` and receive the current editor content in a running session
- The agent can call `canvas/append`, `canvas/replace`, and `canvas/insert` and the frontend canvas updates in real time without a page reload
- All four operations have passing unit tests

## Notes

Split from parent epic `knoxx-unified-workplace-pattern` on 2026-05-30.
