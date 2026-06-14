---
uuid: "knoxx-system-prompt-audit-visibility"
title: "Emit rendered system prompts into the run event ledger and audit UI"
status: "completed"
priority: "P2"
labels: ["tasks", "2sp", "events-agent-runtime-separation", "prompting", "audit", "frontend"]
created_at: "2026-06-09T00:00:00Z"
source: "session-review-triggered-agent-prompts"
points: 2
category: "tasks"
---

# Emit rendered system prompts into the run event ledger and audit UI

## Context

Triggered agents receive a combined system prompt assembled from:

1. Role contracts (`:prompts :system`)
2. Actor contracts (`:prompts :system`)
3. Agent contracts (`:prompts :system`)

This composition already happens in `domain/contracts/resolve.cljs`, but there was no
characterization test verifying that role prompts survive into the final resolved
`:system-prompt`. Additionally, the rendered prompt was not written into the run event
ledger, making it impossible for operators to confirm what instructions were actually
sent to the model when investigating triggered runs in the audit panel.

## Changes

### Backend

- `backend/test/cljs/knoxx/backend/contracts/resolve_test.cljs`
  - Added `resolve-agent-contract-composes-role-actor-and-agent-system-prompts`
    characterization test. Mocks `roles/role-system-prompt` to return a real value
    and asserts the combined `:system-prompt` contains role, actor, and agent
    contributions.

- `backend/src/cljs/knoxx/backend/infra/agent/turn.cljs`
  - Added `emit-system-prompt-rendered-event!` (next to the existing
    `emit-action-task-rendered-event!`).
  - Emits a `system_prompt_rendered` run event when the agent spec carries a
    non-empty rendered `:system-prompt`.
  - Preview is capped at 2000 characters with a truncation marker so the event
    payload stays bounded while remaining useful for audit.
  - Event extra carries `contract_id`, `actor_id`, `role`, and `trigger_id` when
    available, mirroring the metadata shape used by `action_task_rendered`.
  - Called at the end of `create-initial-run!` so the event is persisted and
    broadcast alongside `run_started` and `action_task_rendered`.

### Frontend

- `frontend/src/components/agent-audit/AgentAuditLogs.tsx`
  - Added `eventTypeLabel` helper that maps event types to human-readable titles,
    including `system_prompt_rendered` -> "System prompt".
  - Applied the label helper in both `runDetailToAuditItems` and
    `eventsToAuditItems` so the audit timeline shows a friendly title instead of
    the raw snake-case type.
  - Added a `default` case to the timeline `switch` to satisfy the exhaustive-return
    linter rule (pre-existing issue in the same file).

## Verification

- `pnpm -C backend exec shadow-cljs compile test` → 934 tests, 2367 assertions,
  0 failures, 0 errors.
- `pnpm -C backend typecheck` → 0 warnings.
- `NODE_ENV=test pnpm -C frontend typecheck` → clean.
- `NODE_ENV=test pnpm -C frontend exec vitest run --config vitest.config.ts
  src/components/agent-audit/AgentAuditLogs.test.tsx
  src/components/agent-audit/AgentAuditSessionList.test.tsx
  src/components/agent-audit/AgentAuditLogs.test.ts` → 14 passed.

## Result

Operators can now open a triggered run in the audit panel and see a "System prompt"
system note containing the rendered prompt that was actually sent to the LLM,
alongside the existing "Action task" note.
