---
uuid: "knoxx-trigger-runner-facade-delete"
title: "Delete infra/trigger-runner deprecated facade after confirming zero callers"
status: review
priority: "P3"
labels: ["tasks", "1sp"]
created_at: "2026-05-29T00:00:00Z"
source: "epics/events-agent-runtime-separation.md"
points: 1
category: "tasks"
---
# Delete infra/trigger-runner deprecated facade after confirming zero callers

> Parent epic: `knoxx-events-agent-runtime-separation`

## Context

`infra/trigger_runner.cljs` (39 lines) is an explicitly deprecated compatibility facade that delegates every function to `infra/event_runtime`. No runtime code imports it — the only reference is a stale docstring in `domain/discord/discord_io.cljs` line 2. The facade should be deleted to prevent future confusion.

## Work

1. Confirm zero callers:
   ```bash
   grep -rn "trigger-runner\|trigger_runner" backend/src --include="*.cljs" | grep -v trigger_runner.cljs
   ```
   Expected: only the discord_io.cljs docstring line, no `:require` entries.

2. Delete `backend/src/cljs/knoxx/backend/infra/trigger_runner.cljs`.

3. Update the `domain/discord/discord_io.cljs` docstring (line 2) to remove the reference to trigger-runner:
   Change: `"Discord I/O helpers. Pure API wrappers consumed by trigger-runner,`
   To: `"Discord I/O helpers. Pure API wrappers consumed by event-runtime,`

4. Run `pnpm -C backend run typecheck` and `pnpm -C backend lint` to confirm no broken requires.

## Affected files

- `backend/src/cljs/knoxx/backend/infra/trigger_runner.cljs` (delete)
- `backend/src/cljs/knoxx/backend/domain/discord/discord_io.cljs` (docstring line 2 only)

## Definition of Done

- `infra/trigger_runner.cljs` no longer exists in the repo
- `grep -rn trigger-runner backend/src` returns zero results
- `pnpm -C backend run typecheck` exits 0
- `pnpm -C backend lint` exits 0

---
Triage 2026-05-29: 1sp dead-code deletion with all steps fully specified — grep to confirm zero callers, delete `infra/trigger_runner.cljs`, patch a single docstring line in `discord_io.cljs`, then typecheck and lint to confirm clean. No dependencies or blockers. Verdict: accepted (P3). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
