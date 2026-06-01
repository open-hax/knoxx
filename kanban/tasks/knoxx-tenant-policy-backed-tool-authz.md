---
uuid: "knoxx-tenant-policy-backed-tool-authz"
title: "Tenant Policy: Replace Hardcoded Tool Authorization with Policy-DB Checks"
status: ready
priority: "P2"
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-29T00:00:00Z"
points: 3
category: "tasks"
---
# Tenant Policy: Replace Hardcoded Tool Authorization with Policy-DB Checks

> Parent epic: `knoxx-knowledge-ops-mvp-phase1-epics` (Epic 1, Story 1.3)

## Problem

Tool execution in `routes/tools.cljs` checks `(ensure-permission! ctx "agent.chat.use")` but the tool-level allowance/deny policies stored in the policy DB (`ctx-tool-effect`, `ctx-tool-policy` in `authz.cljs`) are not enforced before dispatching tool calls. A membership with `bash` in its deny list can still invoke bash.

## Goal

Before executing any tool, check `authz/ctx-tool-effect` for the resolved context. Deny with 403 if the effect is `"deny"` or if no policy exists and the tool is not in a default-allow set.

## Affected Files

- `backend/src/cljs/knoxx/backend/infra/routes/tools.cljs` — add policy-gate before tool dispatch
- `backend/src/cljs/knoxx/backend/infra/auth/authz.cljs` — may need `tool-permitted?` helper consolidating effect + default logic
- `backend/test/cljs/knoxx/backend/agents/policy_test.cljs` — add test for deny-effect blocking tool execution

## DoD

- A request to execute tool `bash` by a membership with `{:toolId "bash" :effect "deny"}` returns HTTP 403
- A request to execute a tool with no policy entry follows the configurable default (allow by default for `knowledge_worker`, deny for unknown roles)
- `pnpm -C backend test` passes
- `pnpm -C backend lint` passes

---
Triage 2026-05-29: Bounded 3sp epic subtask with a concrete security correctness gap — tool execution in routes/tools.cljs bypasses tenant policy-DB deny rules — and a clear DoD (403 on denied tool, configurable default, tests pass). No named blockers; affected files and test targets are specified. Verdict: accepted (P2). --tasks-dir orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
