---
uuid: "knoxx-chat-ui-core-components"
title: "Chat UI — Core React components (MessageBubble, Composer, ChatPanel)"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Chat UI — Core React components (MessageBubble, Composer, ChatPanel)

> Parent epic: `knoxx-knowledge-ops-chat-ui-library`
> Points: 3

## Purpose

Deliver the shared presentational components that replace the duplicated message bubbles, composer inputs, and error-handling UI currently copy-pasted across Ragussy ChatPage, Ragussy ChatLabPage, and Shibboleth ChatLab.

## Scope

- `packages/chat-ui/src/components/MessageBubble.tsx` — renders a single message with role-aware styling via `@open-hax/uxx` tokens
- `packages/chat-ui/src/components/Composer.tsx` — textarea + send button, controlled, emits `onSubmit(text)`
- `packages/chat-ui/src/components/ChatPanel.tsx` — composition root: accepts `useChat` result props, renders message list + Composer + error banner
- `packages/chat-ui/src/components/SourceCard.tsx` — collapsible source citation card (migrated from Ragussy ChatPage pattern)
- All components exported from `packages/chat-ui/src/index.ts`

## Definition of done

- All components render without runtime errors in a Storybook or standalone React app against the `@open-hax/uxx` token set
- `ChatPanel` accepts a `transport` prop and wires through `useChat` with no prop-drilling of raw state
- No component imports implementation-specific modules from Ragussy, Shibboleth, or any consuming app

## Notes

Split from parent epic `knoxx-knowledge-ops-chat-ui-library` on 2026-05-30.
