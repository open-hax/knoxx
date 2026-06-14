---
uuid: "knoxx-frontend-chat-workspace-decomposition"
title: "Frontend: chat-workspace subtree decomposition (the migration keystone)"
status: accepted
priority: P1
labels: ["tasks", "frontend", "helix", "chat", "vite-retirement", "has-parent"]
created_at: "2026-06-11T00:00:00Z"
points: 13
category: tasks
---
# Frontend: chat-workspace subtree decomposition

> Parent epic: `knoxx-frontend-helix-migration-vite-retirement`

## Why this is the keystone

ChatPage, ContractsPage, CmsPage, BroadcastStudioPage and AgentAuditLogs all
hang off the chat-workspace subtree. Until it migrates, those pages cannot,
and the app-bridge cannot shrink past its current floor. Roughly half the
remaining TS by weight lives here:

- `components/chat-page/useChatWorkspaceController.ts` — the controller hook
  (state machine: conversation/session ids, streaming, memory resume).
  Consumed from CLJS (agents/events pages) via the bridge TODAY.
- `components/chat-page/ChatWorkspacePane.tsx` + ChatMainPane,
  ChatMessageList, ChatComposer, MultimodalContent/MultimodalInput,
  ToolReceiptBlock, CollapsedPanelTab(TS copy), chat-page/utils
  (parseMemoryRowExtra).
- uxx `Markdown` (via the Vite frontend-bridge) renders message bodies.

## Decomposition order (leaf-up, each its own TDD slice)

1. **chat-page/utils → CLJS** (parseMemoryRowExtra etc. — pure, has vitest
   coverage to port).
2. **ToolReceiptBlock → Helix** (self-contained, has vitest test;
   render+interaction testable; needed by message list and AgentAuditLogs).
3. **MultimodalContent → Helix** (pure-ish renderer over ContentParts).
4. **ChatMessageList → Helix** (depends on 1–3 + Markdown via bridge).
5. **ChatComposer + MultimodalInput → Helix** (attachment handling).
6. **useChatWorkspaceController → CLJS hook** (the big one — port with the
   interaction harness; expose a JS-shaped controller for remaining TS
   consumers, mirroring the auth-context instance pattern).
7. **ChatWorkspacePane/ChatMainPane → Helix**; flip agents/events pages to
   native; bridge exports `ChatWorkspacePane`/`useChatWorkspaceController`
   die.
8. ChatPage itself → native route; then AgentAuditLogs unblocks.

## Constraints

- uxx Markdown stays on the frontend-bridge until uxx-helix-native lands.
- The controller's JS shape must stay stable while TS consumers remain
  (ContractsPage/CmsPage) — same shared-instance discipline as auth.
- Each step: tests first (port existing vitest where present), gates green,
  TS deleted with its consumers.

## Definition of done

- No `components/chat-page/*.tsx` remain; bridge no longer exports
  ChatWorkspacePane/useChatWorkspaceController; ChatPage natively routed.
