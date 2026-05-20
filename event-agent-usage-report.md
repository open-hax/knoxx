# `event-agent` Usage Report — Backend Source

## Summary
- **130 matches** in `backend/src/**/*.cljs` (source code)
- **14 matches** in `backend/test/**/*.cljs` (test code)
- Found in **12 source files** and **1 test file**
- The term appears as: `event-agent`, `event_agents`, `event-agents` (log prefixes), `event-agent-control`

---

## 1. Core Configuration & Control (`triggers/control_config.cljs`)
The primary engine for event-agent scheduling and contract translation.

| Line | Symbol / Context |
|------|------------------|
| 23 | `(declare … event-agent-trigger-kinds)` |
| 219 | `(defn- contract->event-agent-job …)` — Converts agent contracts into event-agent job maps |
| 224 | `(contains? event-agent-trigger-kinds trigger-kind)` — Only `"cron"` and `"event"` kinds become jobs |
| 256 | Calls `contract->event-agent-job` while building the default job list |
| 276 | `(def ^:private event-agent-control-redis-key "event-agent:control-config")` |
| 277 | `(def ^:private event-agent-trigger-kinds #{"cron" "event"})` |
| 318–343 | `persist-event-agent-control!` / `load-event-agent-control` — Redis persistence |
| 477 | `event-agent-role-options` — Returns valid roles for event-agent jobs |
| 481 | `event-agent-source-kind-options` — `"discord"`, `"github"`, `"manual"`, `"cron"` |
| 485 | `event-agent-trigger-kind-options` — `"cron"`, `"event"` |
| 547 | `default-event-agent-control` — Generates default job list from contracts |
| 562 | `normalize-event-agent-job` — Validates & normalizes a job map |
| 676 | `event-agent-control-config` — Merges defaults + saved overrides into final config |
| 714 | Default description: `"Custom scheduled event-agent job"` |

---

## 2. HTTP Routes (`infra/routes/tools.cljs`)
Admin REST API surface for managing event-agents.

| Line | Route / Symbol |
|------|----------------|
| 47 | `(defn- event-agents-control-response …)` — Builds the JSON config response |
| 68 | `(defn- event-agent-result-summary …)` — Summarises job run results |
| 84 | `(defn- event-agent-job-run-response! …)` — HTTP response helper |
| 341–346 | `GET /api/admin/config/event-agents` |
| 348–361 | `PUT /api/admin/config/event-agents` — Update control config |
| 365–375 | `POST /api/admin/config/event-agents/jobs/:jobId/run` |
| 380–396 | `POST /api/admin/config/event-agents/events/dispatch` |
| 396–402 | `POST /api/admin/config/event-agents/runtime/stop` |
| 404–410 | `POST /api/admin/config/event-agents/runtime/start` |
| 412–421 | `POST /api/admin/config/event-agents/runtime/reset` |
| 635–641 | Registration of all seven routes in `register-tool-routes!` |

---

## 3. LLM Tool Surface (`tools/events.cljs`)
Five deprecated* tool factories still expose `event-agent` naming to LLMs.

| Line | Tool Name | Deprecation Note |
|------|-----------|------------------|
| 256 | `event_agent_status_tool` | "Deprecated: use `events.status` instead." |
| 266 | `event_agent_dispatch_tool` | "Deprecated: use `events.dispatch` instead." |
| 276 | `event_agent_run_job_tool` | "Deprecated: use `events.run_job` or `agents.spawn` instead." |
| 286 | `event_agent_upsert_job_tool` | "Deprecated: use `events.upsert_job` instead." |
| 296 | `schedule_event_agent_tool` | "Deprecated: use `schedule_trigger` instead." |

*All five are still returned by `create-events-custom-tools` (lines 318–336).

---

## 4. Tool Registry (`tools/registry.cljs`)
Static metadata for the five legacy tool IDs.

| Line | Tool ID | Risk Level |
|------|---------|------------|
| 27 | `event_agents.status` | low |
| 28 | `event_agents.dispatch` | low |
| 29 | `event_agents.run_job` | low |
| 30 | `event_agents.upsert_job` | high |
| 31 | `schedule_event_agent` | high |

---

## 5. Discord Integration (`domain/discord/source.cljs`)
Runtime log prefixes and source adapter for event-agent runs.

| Line | Context |
|------|---------|
| 2 | Namespace doc: `"Discord source adapter for event-agent runtime."` |
| 124 | `console.warn "[event-agents.discord] OpenPlanner label lookup failed …"` |
| 234 | `println "[event-agents.discord] patrol failed for …"` |
| 317 | `println "[event-agents] bound … Discord actor gateway(s)"` |
| 323 | `println "[event-agents] discord actor gateway bind failed: …"` |
| 325 | `println "[event-agents] policy DB unavailable; Discord actor gateways not bound"` |

---

## 6. Discord Voice Tools (`domain/discord/voice_tools.cljs`)
Two contract-only voice tools that emit raw audio as event-agent events.

| Line | Tool | Description snippet |
|------|------|---------------------|
| 678–681 | `discord.voice.agent_event.connect` | `"Join voice and emit raw Discord audio as real Knoxx event-agent events."` |
| 689–692 | `discord.voice.agent_event.listen` | `"Emit raw Discord audio as real Knoxx event-agent events for an existing voice connection."` |

---

## 7. Agent Runtime & Session Resume

| File | Line | Context |
|------|------|---------|
| `domain/agent/agent_resume.cljs` | 93 | Comment: `"sessions so that in-flight runs (e.g. those orphaned by event-agents/reload!)"` |
| `domain/agent/agent_templates.cljs` | 7 | Comment: `"the old event-agent job helper surface only as compatibility plumbing."` |
| `domain/agent/agent_templates.cljs` | 555 | Docstring: `"Create a concrete event-agent job from a legacy template."` |
| `domain/agent/turn.cljs` | 506 | Comment: `"LLM request payload — especially in sticky event-agent sessions"` |
| `domain/sessions/session_store.cljs` | 284 | Comment: `"Sticky event-agent sessions are long-lived across scheduled runs, so keep them"` |

---

## 8. Cron & Event Scheduling (`domain/event/cron.cljs`)

| Line | Context |
|------|---------|
| 5 | Comment: `"the legacy event-agents namespace can delegate to them while internals are"` |

---

## 9. Application Bootstrap (`infra/core.cljs`)

| Line | Context |
|------|---------|
| 172 | Comment: `"Bring Redis up before any event-agent scheduling so we can"` |

---

## 10. MCP Gateway (`infra/mcp/mcp_expose.cljs`)

| Line | Context |
|------|---------|
| 100 | Docstring fragment: `"Use these ids when constructing Knoxx sub-agent/event-agent payloads."` |

---

## 11. Contract Documentation (`law/contracts.cljs`)

| Line | Context |
|------|---------|
| 186 | Comment: `"Source-mode contracts document how event-agent source modes transform upstream"` |

---

## 12. Test Coverage (`test/cljs/knoxx/backend/triggers/control_config_test.cljs`)

| Line | Test Name |
|------|-----------|
| 38 | `event-agent-control-config-uses-saved-overrides-when-contract-hash-matches` |
| 63 | `event-agent-control-config-resets-contract-job-when-hash-drifts` |
| 84 | `event-agent-control-config-resets-legacy-contract-job-without-hash` |
| 103 | `event-agent-control-config-keeps-custom-jobs` |
| 164 | Private var test: `contract->event-agent-job` |

---

## Observations
1. **Deprecation drift** — Five LLM tools in `tools/events.cljs` are marked deprecated but still returned by the factory function. The newer `events.*` / `agents.spawn` / `schedule_trigger` tools exist alongside them.
2. **Naming inconsistency** — Kebab-case (`event-agent`), snake_case (`event_agents.*`), and camelCase (`eventAgents`) all appear in different layers (Clojure source, tool IDs, API paths).
3. **Redis persistence** — Only `event-agent-control` is stored in Redis; the key is `event-agent:control-config`.
4. **Contract-driven jobs** — The `contract->event-agent-job` translation layer means every `agents/*.edn` contract with `:trigger-kind "cron"` or `"event"` auto-generates an event-agent job.
