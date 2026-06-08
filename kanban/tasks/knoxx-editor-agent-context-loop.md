---
uuid: "knoxx-editor-agent-context-loop"
title: "Editor Agent Context Loop"
status: breakdown
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---
# Editor Agent Context Loop

> Parent epic: `knoxx-unified-workplace-pattern`
> Points: 2

## Purpose

Wire the Editor workplace's `AgentChatPanel` so that it automatically pins the currently open document to the agent context and keeps it updated as the user edits, enabling commands like "edit this" and "rewrite section" to operate on live content.

## Scope

- Update `frontend/src/pages/CMSPage.tsx` (or its Editor workplace equivalent) to push the active document's content into the shared pinned-context state whenever the editor selection changes or the document is saved
- Ensure the `AgentChatPanel` on the right receives the updated context and passes it to each agent message without requiring the user to re-pin manually
- Remove any stale "pin document" manual step from the Editor UX

## Definition of done

- Opening a document in the Editor workplace automatically pins it to the agent context visible in the chat panel
- Editing the document body updates the pinned context so the agent always receives the latest content without user intervention
- The "edit this" agent command successfully modifies the canvas using the live document content

## Notes

Split from parent epic `knoxx-unified-workplace-pattern` on 2026-05-30.
