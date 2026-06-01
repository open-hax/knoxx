---
uuid: "knoxx-chat-ui-test-and-typecheck-gate"
title: "Chat UI — TypeScript build and test gate for @workspace/chat-ui"
status: incoming
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---

# Chat UI — TypeScript build and test gate for @workspace/chat-ui

> Parent epic: `knoxx-knowledge-ops-chat-ui-library`
> Points: 2

## Purpose

Establish the CI-ready build and test gate for the `@workspace/chat-ui` package so that downstream consumers (Ragussy, Shibboleth, futuresight-kms) can depend on it with confidence and regressions are caught before merge.

## Scope

- `packages/chat-ui/tsconfig.json` — strict mode, composite build, emits to `dist/`
- `packages/chat-ui/package.json` — `build`, `typecheck`, and `test` scripts wired to `tsc --build`, `vitest run`
- `packages/chat-ui/src/__tests__/useChat.test.ts` — covers REST transport happy path and error state
- `packages/chat-ui/src/__tests__/ChatPanel.test.tsx` — smoke render test using React Testing Library
- Nx/pnpm workspace integration: `chat-ui` appears in `pnpm-workspace.yaml` and nx project graph with `build` and `test` targets

## Definition of done

- `pnpm --filter @workspace/chat-ui build` exits 0 with zero TypeScript errors under strict mode
- `pnpm --filter @workspace/chat-ui test` exits 0 with all tests passing
- Running `nx affected --target=typecheck` from workspace root includes `chat-ui` when its source files change

## Notes

Split from parent epic `knoxx-knowledge-ops-chat-ui-library` on 2026-05-30.
