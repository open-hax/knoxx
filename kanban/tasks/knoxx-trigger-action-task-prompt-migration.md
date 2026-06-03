---
uuid: "knoxx-trigger-action-task-prompt-migration"
title: "Move triggered-run task prompts from agent contracts to trigger/action inputs"
status: "in_progress"
priority: "P1"
labels: ["tasks", "5sp", "events-agent-runtime-separation", "prompting", "audit"]
created_at: "2026-06-02T00:00:00Z"
source: "docs/design/event-trigger-action-runtime.md"
points: 5
category: "tasks"
---

# Move triggered-run task prompts from agent contracts to trigger/action inputs

> Parent epic: `knoxx-events-agent-runtime-separation`

## Context

The design docs already state the target architecture:

- Agents are executables that specify prompting, capabilities, roles, ownership, and policy; they are not generators, triggers, actions, or schedules.
- Agent resources do not define triggers, actions, schedules, generators, event filters, or source/source-mode fields.
- Triggers subscribe to event types and request actions after a predicate passes.
- Actions are registered functions invoked against actor context plus an optional event.
- Agent session lifecycle is represented as actions.

Current triggered social agents still carry old-shape `:prompts :task` fields on agent resources, and `domain/action/start_agent_session.cljs` appends the agent task prompt into the rendered user message. That keeps legacy event-agent semantics alive and makes audit confusing: a run can look like an agent prompt was missing even though the runtime synthesized a user message from a mixed agent/trigger shape.

## Problem

Long-lived Knoxx resources have drifted between old and new architecture. Agent contracts such as `ussyverse_social_replies` and `ussyverse_social_creative` still mix durable agent identity/system instructions with trigger-specific task instructions and event/filter data.

This causes three concrete failures:

1. The source of the task prompt is ambiguous: agent contract vs trigger/action invocation.
2. Audit surfaces do not clearly show the rendered trigger/action user task that was sent to the model.
3. Empty/no-op triggered completions are hard to distinguish from prompt-construction failures unless the run ledger records the action input explicitly.

## Intended Model

- Agent resources may keep durable system prompts and runtime defaults.
- Trigger resources decide whether an event should request an action.
- Action input owns the task prompt/user message template for the action.
- `:actions/start-agent-session` renders the user message from trigger/action data plus event context.
- Agent `:prompts :task` is deprecated for triggered runs and should only be used as a temporary fallback with an audit warning.

## Work

1. Add an action-input normalization helper for `:actions/start-agent-session`.
   - Accept task/message fields from trigger/action data, e.g. `:action/with`, `:trigger/task`, `:trigger/message-template`, or `:data :task` during migration.
   - Preserve current event fields: event type, trigger id, reason, channel id, author, content, event id, and event scope.

2. Refactor `domain/action/start_agent_session.cljs`.
   - Stop reading `(:task-prompt resolved)` as the primary triggered-run task.
   - Render the user message from trigger/action input.
   - If only the agent contract has `:prompts :task`, use it as a deprecated fallback and emit a structured audit/runtime warning.
   - Include a `task_source` / `taskSource` marker in the direct-start payload metadata.

3. Make audit explicit.
   - Ensure initial run/session state records the rendered user task message.
   - Ensure run events include a compact `action_task_rendered` or equivalent system event with task source and trigger/action ids, without logging secrets.
   - In the audit UI, the rendered user task must be visible even when there is no assistant output or tool call.

4. Migrate the two Ussyverse trigger resources.
   - Move the triggered task text out of `contracts/agents/ussyverse_social_replies.edn` into `contracts/triggers/ussyverse_social_replies_event.edn` action/trigger input.
   - Move the scheduled/social creative task text out of `contracts/agents/ussyverse_social_creative.edn` into its trigger/schedule/action invocation resource.
   - Leave agent resources with system prompt, model, actors, roles/capabilities, memory/source policy, and durable identity only.

5. Add characterization tests or smoke checks.
   - Fire `ussyverse_social_replies_event` manually.
   - Assert the run/session has a user message containing the trigger/action-rendered task.
   - Assert agent `:prompts :task` fallback produces a deprecation warning, not silent behavior.
   - Assert empty model completions remain failed via the existing `empty_output` path.

## Acceptance Criteria

- Triggered agent sessions no longer depend on agent-contract `:prompts :task` for their user task.
- Audit logs show the rendered trigger/action task message before any assistant response is expected.
- Agent resources can omit `:prompts :task` and still run correctly when invoked by a trigger/action with task input.
- Deprecated agent task-prompt fallback is visible in logs/run events and can be removed later.
- `ussyverse_social_replies` and `ussyverse_social_creative` are migrated without losing current behavior.

## Definition of Done

- `backend/src/cljs/knoxx/backend/domain/action/start_agent_session.cljs` uses trigger/action task input as the primary user-message source.
- `contracts/agents/ussyverse_social_replies.edn` no longer carries trigger-specific task text.
- `contracts/agents/ussyverse_social_creative.edn` no longer carries trigger/schedule-specific task text.
- Relevant trigger/action resources carry the invocation task text.
- Manual trigger smoke shows the rendered user task in the audit panel.
- Empty/no-op triggered completion is logged and marked failed, not completed.
- `pnpm -C backend typecheck` exits 0.
- `pnpm -C backend error-boundaries:check` exits 0.

## Risks

- There are legacy resource shapes that still use `:data` for filters/context; do not mass-delete them during this task unless the runtime path is migrated.
- Changing the prompt source may alter social-agent behavior; use the current task text as migration input first, then tune prompts separately.
- Do not turn trigger task prompts into system prompts. They are user/task messages by design.

---
2026-06-02 triage: Created from live triggered-run debugging and design-doc review. Scope is intentionally 5sp: migrate task ownership and audit visibility for triggered runs, but do not complete the entire event/trigger/action resource-language cleanup. --tasks-dir .

2026-06-02 TDD implementation slice: added failing characterization tests for action-owned task selection/rendering, then implemented trigger/action task normalization, migrated Ussyverse reply/creative task text from agent resources to trigger resources, recorded task source in agent_spec, and emitted action_task_rendered run events. Verified backend test:shadow 454 tests / 1333 assertions pass, backend typecheck pass, server-dev compile pass, error-boundaries check pass, targeted clj-kondo for new start-agent-session test/code pass. PM2 knoxx-backend restarted and /api/auth/config returned 200. Manual trigger audit remains pending because it needs an authenticated admin session/click path. --tasks-dir kanban

2026-06-02 pressure-test follow-up: user-triggered run trigger-ussyverse_social_replies_event-1780443290768 proved the prompt migration worked: Redis run_events contain run_started, action_task_rendered with task_source=action/task, turn_end/agent_end, then run_failed reason=empty_output. Session status is failed and request messages include the rendered Action task prompt. Added TDD regression for error_observatory/safe-json preserving namespaced context keys, fixed JSON key preservation, and flattened empty-output context keys so future logs include run_id/conversation_id/session_id/contract_id/actor_id/trigger_id/task_source instead of collapsing to id/model. Verification reran test:shadow 455 tests/1338 assertions, typecheck, server-dev compile, error-boundaries check; PM2 backend restarted. --tasks-dir kanban
---

2026-06-03 continued pressure-test: added TDD coverage for JSON admin dispatch dotted event types preserving EDN keywords (`discord.message` -> `:discord.message`), fixed event normalization so `/api/admin/config/events/dispatch` can match Ussyverse trigger events, and added live-contract policy coverage keeping `ussyverse_social_replies` on `:thinking :off` after proving Proxx/Gemma `:xhigh` could return hidden reasoning with empty visible content. Also characterized Gemma tool-call incompatibility and disabled provider tools for `gemma4:31b`, so triggered runs now produce visible assistant output instead of `empty_output` when the event asks for no tool use. Verified event dispatch matched `ussyverse_social_replies_event`; run `trigger-ussyverse_social_replies_event-1780446180889` completed with `action_task_rendered`, `assistant_first_token`, and a non-empty answer. Separately posted a Discord smoke-proof message via actor-owned Discord bot credentials without logging secrets: message `1511524299230548048` in channel `1444189585373663417`. Full backend test/typecheck/server-dev/error-boundaries checks passed. Remaining separate issue: Gemma still reasons about Discord send rather than calling `discord.send`, so true model-driven Discord send needs a follow-up design/fix. --tasks-dir kanban
