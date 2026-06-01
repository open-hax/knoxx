---
uuid: "knoxx-chat-ui-package-scaffold-and-types"
title: "Chat UI Library — Package Scaffold and Unified Types"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-29T00:00:00Z"
points: 3
category: tasks
---

# Chat UI Library — Package Scaffold and Unified Types

> Parent epic: `knoxx-knowledge-ops-chat-ui-library`

Create the `@workspace/chat-ui` package directory with build config and the full unified type definitions that all five chat layers will share.

## Goal

Stand up `packages/chat-ui/` as a buildable TypeScript workspace package. No components yet — just the package wiring and the canonical types.

## Affected Files

| File | Action |
|------|--------|
| `packages/chat-ui/package.json` | Create — name `@workspace/chat-ui`, peer deps react ^18, dep @open-hax/uxx |
| `packages/chat-ui/tsconfig.json` | Create — extends workspace root, targets ESNext, strict |
| `packages/chat-ui/src/index.ts` | Create — barrel (empty re-exports for now) |
| `packages/chat-ui/src/types/index.ts` | Create — ChatMessage, ChatConfig, SourceChunk, ToolCall, ChatTransport interfaces |

## DoD

- `packages/chat-ui/package.json` exists with `name: "@workspace/chat-ui"` and `peerDependencies: { react: "^18.0.0", "react-dom": "^18.0.0" }`
- `packages/chat-ui/src/types/index.ts` exports `ChatMessage`, `ChatConfig`, `SourceChunk`, `ToolCall`, `ChatTransport` — matching the spec's interface definitions exactly
- `ChatConfig.transport` is typed as `"rest" | "websocket" | "polling"`
- Running `cd packages/chat-ui && npx tsc --noEmit` exits 0 (types compile clean)
- Root workspace `pnpm install` resolves `@workspace/chat-ui` without error
