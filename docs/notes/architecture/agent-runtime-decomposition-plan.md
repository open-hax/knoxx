---
original_name: "2026.05.05.13.40.07.md"
title: "Agent Runtime Decomposition Plan"
summary: "Analysis and migration plan for decomposing Knoxx agent runtime namespaces."
category: "architecture"
created: "2026-05-05"
---

This is excellent. The picture is clear. Here's the full analysis and migration plan.

***

## What we're actually looking at

13 files across 2 tiers: 

| File | Size | Core job |
|---|---|---|
| `agent_context.cljs` | 955 B | 3 functions: set/clear/get a JS context atom |
| `agent_hydration.cljs` | 12 KB | Settings, passive memory hydration, user message assembly, custom tool building |
| `agent_resume.cljs` | 15.7 KB | Process supervisor: scan Redis, classify stale/hot/resumable, run recovery ticks |
| `agent_runtime.cljs` | 41.6 KB | The giant: SDK session creation, OpenPlanner rehydration, workspace paths, stream body, tool allow-listing |
| `agent_templates.cljs` | 7.7 KB | Model profiles, job templates, tool policies, job normalization |
| `agent_turns.cljs` | 2.7 KB | **Shell/re-export file** — mostly `^:export` delegates + duplicated fns |
| `agents/content.cljs` | 11.4 KB | Content part accessors and builders (pure EDN + some aget) |
| `agents/policy.cljs` | 3.8 KB | Rate limiting and model policy enforcement |
| `agents/recovery.cljs` | 9.7 KB | Per-session rebuild: auth, agent-spec, kickoff wait, turn restart |
| `agents/stream.cljs` | 21.6 KB | Streaming event handlers, run state sync |
| `agents/tools.cljs` | 4.9 KB | Tool call preview/formatting |
| `agents/transcript.cljs` | 3.5 KB | Message transcript helpers |
| `agents/turn.cljs` | 37.3 KB | Core turn dispatch: access, session ensure, run create, prompt+await |

***

## The duplications

These are confirmed by matching `defn` names: 

### 1. `agent_turns.cljs` is a zombie re-export shell

It **re-declares every public symbol from `agents/recovery.cljs` and `agents/turn.cljs`**:
- `validate-chat-policy!` — also in `agents/policy.cljs`
- `recovered-auth-context`, `recovered-agent-spec`, `restore-recovered-conversation-access!`, `last-session-user-message`, `wait-for-recovered-turn-kickoff!`, `resume-recovered-session!`, `recover-active-agent-sessions!` — all **exactly** in `agents/recovery.cljs`
- `ensure-conversation-access!`, `remember-conversation-access!`, `ensure-session-id`, `send-agent-turn!` — all in `agents/turn.cljs`

This file exists purely to let old callers that `require [knoxx.backend.agent-turns ...]` not break. It's technical debt masquerading as a module.

### 2. Path helpers are defined twice in `agent_runtime.cljs`

From the defns list you can see `path-resolve`, `path-relative`, `path-is-absolute?` each appear **twice** inside `agent_runtime.cljs`. 

### 3. `agent_resume.cljs` + `agents/recovery.cljs` split the same concept

- `agent_resume` has `resume-session!` which calls `agent-turns/resume-recovered-session!`.
- `agents/recovery.cljs` has `resume-recovered-session!` and `recover-active-agent-sessions!`.
- `agent_resume` is the **scheduler** (timer, classification), `recovery` is the **executor** (session rebuild + turn kickoff). These belong together as `agents/recovery.cljs` with a separate thin `agents/recovery-scheduler.cljs` (or folded into an app startup ns).

### 4. `agent_hydration.cljs` owns custom tool building but so does `agent_runtime.cljs`

- `agent_hydration.cljs`: `create-knoxx-custom-tools`, `agent-custom-tool-suite`, `create-agent-custom-tools`
- `agent_runtime.cljs`: `create-runtime-tools`, `effective-tool-auth-context`, `enabled-tool-name-allowlist`, `allowed-root-records` + workspace path helpers

Two files share responsibility for "what tools does this agent get". That should live in one place.

***

## Target layout after the lift

```
agents/
  context.cljs          ← agent_context.cljs (trivial, just rename ns)
  hydration.cljs        ← agent_hydration.cljs
  model.cljs            ← EDN-only agent/message/run types (new, extracted from runtime)
  paths.cljs            ← workspace path helpers (extracted from runtime)
  policy.cljs           ← agents/policy.cljs (unchanged, already here)
  recovery.cljs         ← merge agents/recovery + supervisor fns from agent_resume
  recovery_scheduler.cljs ← startup scan + interval tick (from agent_resume)
  runtime.cljs          ← agent_runtime.cljs core: SDK session create, rehydrate, tool allowlist
  session.cljs          ← create-agent-session!, ensure-agent-session!, active/remove/queue (from runtime)
  templates.cljs        ← agent_templates.cljs
  tools.cljs            ← merge agents/tools + tool-building from hydration + allowlist from runtime
  content.cljs          ← agents/content.cljs (unchanged)
  stream.cljs           ← agents/stream.cljs (unchanged)
  transcript.cljs       ← agents/transcript.cljs (unchanged)
  turn.cljs             ← agents/turn.cljs (unchanged, core is already here)
```

The old `agent_turns.cljs` is **deleted** once callers are updated.

***

## Step-by-step migration plan

### Step 1 — Extract `agents/paths.cljs`

The duplicate `path-resolve`, `path-relative`, `path-is-absolute?`, `resolve-workspace-path`, `allowed-root-records`, `configured-extra-root-records`, `root-relative-path` in `agent_runtime.cljs` should move to their own ns. 

- Create `agents/paths.cljs` with `(ns knoxx.backend.agents.paths)`.
- Copy the **single canonical version** of each path helper.
- Update `agent_runtime.cljs` to `(:require [knoxx.backend.agents.paths :as paths])`.
- No callers outside runtime currently touch these directly, so no other changes needed.

### Step 2 — Create `agents/tools.cljs` (upgrade, not replace)

Currently `agents/tools.cljs` is only preview/formatting. Tool-building (creating the actual tool suite) is split between `agent_hydration` and `agent_runtime`. Merge them:

- Move `create-knoxx-custom-tools`, `agent-custom-tool-suite`, `create-agent-custom-tools` from `agent_hydration` into `agents/tools.cljs`.
- Move `create-runtime-tools`, `effective-tool-auth-context`, `enabled-tool-name-allowlist`, `allowed-root-records` from `agent_runtime` into `agents/tools.cljs`.
- `agent_runtime` now just calls `(agents.tools/create-runtime-tools ...)` and `(agents.tools/create-agent-custom-tools ...)`.
- `agent_hydration` drops its tool-building fns, becoming purely about message assembly and hydration.

### Step 3 — Rename `agent_context.cljs` → `agents/context.cljs`

This is just 3 tiny fns. New ns: `knoxx.backend.agents.context`. Update all callers (search `knoxx.backend.agent-context`). 

### Step 4 — Rename `agent_hydration.cljs` → `agents/hydration.cljs`

After Step 2 strips out the tool-building fns, rename the file and ns to `knoxx.backend.agents.hydration`. Update all callers.

### Step 5 — Rename `agent_templates.cljs` → `agents/templates.cljs`

Straightforward rename. New ns: `knoxx.backend.agents.templates`. Update callers.

### Step 6 — Extract `agents/session.cljs` from `agent_runtime.cljs`

`agent_runtime` is 41 KB. The session-management surface (`create-agent-session!`, `ensure-agent-session!`, `active-agent-session`, `remove-agent-session!`, `queue-agent-control!`, `agent-sessions*`) belongs in its own ns:

- Create `agents/session.cljs` with `(ns knoxx.backend.agents.session)`.
- Move the session atom and the 5 session fns there.
- `agent_runtime` becomes `agents/runtime.cljs`: SDK wiring, rehydration from OpenPlanner/Redis, image materializer interop, `visible-session-signature`, stream body.

### Step 7 — Merge `agent_resume.cljs` scheduler into `agents/recovery_scheduler.cljs`

`agents/recovery.cljs` already has the per-session execution (`resume-recovered-session!`, `recover-active-agent-sessions!`).  The supervisor belongs alongside it:

- Create `agents/recovery_scheduler.cljs` with `(ns knoxx.backend.agents.recovery-scheduler)`.
- Move `session-stale?`, `session-hot?`, `session-resumable?`, `session-last-updated-ms`, `abort-stale-session!`, `resume-session!`, `run-limited!`, `process-sessions!`, `resume-on-startup!`, `attempt-recovery!`, `start-periodic-recovery!`, `stop-periodic-recovery!`, `mark-sessions-resumable!`, `wait-for-turns-and-flush!` from `agent_resume` there.
- Keep `interval-handle*` and the logging helpers in `recovery_scheduler`.
- Update callers of `knoxx.backend.agent-resume` to `knoxx.backend.agents.recovery-scheduler`.

### Step 8 — Delete `agent_turns.cljs`

Before deleting:

1. Find all `require [knoxx.backend.agent-turns ...]` in the codebase.
2. Repoint each caller to the **canonical** namespace where the fn actually lives now:
   - Recovery fns → `agents/recovery`
   - Turn fns → `agents/turn`
   - Policy fns → `agents/policy`
3. Delete the file.

### Step 9 — Rename `agent_runtime.cljs` → `agents/runtime.cljs`

After Steps 2, 3, 5, 6: what's left in `agent_runtime` is the SDK wiring, rehydration logic, and the `setAfterToolCall` image materializer. Rename to `agents/runtime.cljs`, new ns `knoxx.backend.agents.runtime`.

As part of this rename, move the inline image materializer lambda (the gross `setAfterToolCall` blob from lines 520–571) into a dedicated function in a new `agents/image_codec.cljs` or fold it into the codec boundary plan from earlier.

***

## Summary of the final file count

Before: **13 files** across 2 directories, 6 with flat `agent_*.cljs` names.   
After: **15 focused files**, all inside `agents/`, clear responsibility per file, `agent_turns.cljs` deleted and no stale re-exports.

The extra 2 files (`paths`, `session`, `recovery_scheduler`) come from splitting the 41 KB `agent_runtime` monolith — that's a net win on comprehension and change blast radius.

Would you like me to start with Step 1 (paths extraction) or Step 8 (nuking `agent_turns.cljs` and its re-exports) as a clean first PR?
