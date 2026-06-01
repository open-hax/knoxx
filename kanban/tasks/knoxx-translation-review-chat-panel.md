---
uuid: "knoxx-translation-review-chat-panel"
title: "Translation Review Chat Panel"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Translation Review Chat Panel

> Parent epic: `knoxx-unified-workplace-pattern`
> Points: 3

## Purpose

Create the `TranslationReviewPage` that places a translation comparison view in the centre pane and an always-visible `AgentChatPanel` on the right, following the unified 3-pane workplace pattern.

## Scope

- Add `frontend/src/pages/TranslationReviewPage.tsx` wiring together `ContextBar` (left), `TranslationComparison` (centre), and `AgentChatPanel` (right, always visible)
- Register the `/translations` route in the frontend router
- Auto-pin source and target translations to the agent context on page load
- Surface "explain difference", "suggest revision", and "check terminology" quick-action commands in the chat panel

## Definition of done

- Navigating to `/translations` renders the 3-pane layout without errors
- The `AgentChatPanel` is always visible and auto-pins the active source/target translation pair to agent context
- Quick-action commands appear in the panel and round-trip to the agent backend successfully

## Notes

Split from parent epic `knoxx-unified-workplace-pattern` on 2026-05-30.
