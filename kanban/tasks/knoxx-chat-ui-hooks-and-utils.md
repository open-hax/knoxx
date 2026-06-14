---
uuid: "knoxx-chat-ui-hooks-and-utils"
title: "Chat UI — useChat hook and shared transport utilities"
status: accepted
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---
# Chat UI — useChat hook and shared transport utilities

> Parent epic: `knoxx-knowledge-ops-chat-ui-library`
> Points: 3

## Purpose

Implement the core `useChat` React hook and pluggable transport layer (REST, WebSocket, polling) that unifies the divergent streaming approaches currently spread across Ragussy, Shibboleth, and futuresight-kms implementations.

## Scope

- Create `packages/chat-ui/src/hooks/useChat.ts` — unified hook accepting a transport config, returning messages, send, and status
- Create `packages/chat-ui/src/transports/rest.ts`, `websocket.ts`, `polling.ts` — adapters implementing a common `ChatTransport` interface
- Create `packages/chat-ui/src/types.ts` — shared `Message`, `ChatTransport`, `UseChatOptions`, and `UseChatResult` types
- Export all from `packages/chat-ui/src/index.ts`

## Definition of done

- `useChat` hook compiles under strict TypeScript with zero errors
- All three transport adapters implement the shared `ChatTransport` interface and are importable from `@workspace/chat-ui`
- Unit tests cover the hook's message-append and error-state transitions for at least the REST transport

## Notes

Split from parent epic `knoxx-knowledge-ops-chat-ui-library` on 2026-05-30.
