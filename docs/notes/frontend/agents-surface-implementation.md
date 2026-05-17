---
title: "Agents Surface Implementation Notes"
category: frontend
created: 2026-04-24
original: 2026.04.24.14.21.23.md
status: note
---

 Signal

 (己, p=0.9) Implemented the unified Agents surface at /agents by merging the old /event-agents control plane with the old /ops/agents observability view as an “Agent Audit Logs” sub-tab.
 (己, p=0.9) Event Agents is now just Agents in navigation; legacy /event-agents redirects to /agents.
 (己, p=0.85) /ops/agents now redirects to /agents?tab=audit (so ops “Agents” becomes the audit sub-tab).
 (己, p=0.8) The audit log view now only renders OpenPlanner session events (via /api/openplanner/v1/sessions...), and the Active/History/Pause refresh controls live inside the left Sessions panel.
 (己, p=0.8) Added keyword search + semantic search inside Agent Audit Logs (semantic uses searchMemory({sessionId})).
 (己, p=0.75) Updated the event-agent control plane details layout so Runtime snapshot / Live runtime / Quick reference render as a 3-column grid at the top of the selected job pane.

 Evidence

 (己, p=0.95) Unified route + legacy redirect:
 - frontend/src/lib/app-routes.ts (added AGENTS_ROUTE='/agents', LEGACY_EVENT_AGENTS_ROUTE='/event-agents', EVENT_AGENTS_ROUTE=AGENTS_ROUTE)
 - frontend/src/App.tsx (nav label → “Agents”, /agents route, legacy /event-agents redirect)

 (己, p=0.95) Ops redirect into audit sub-tab:
 - frontend/src/pages/OpsRoot.tsx (/ops/agents → <Navigate to="/agents?tab=audit" />)

 (己, p=0.9) New unified page:
 - frontend/src/pages/AgentsPage.tsx (tabs: “Runtime jobs” + “Agent Audit Logs”; passes selected job context into audit as built-in filter seed)

 (己, p=0.9) Agent Audit Logs component (OpenPlanner-only events + controls moved left + keyword+semantic search):
 - frontend/src/components/agent-audit/AgentAuditLogs.tsx

 (己, p=0.9) Control plane tweaks (callback + 3-column top grid):
 - frontend/src/components/admin-page/DiscordSection.tsx (onSelectedJobChange + new layout)

 (己, p=0.8) Rename surface label in actor contract:
 - contracts/actors/event_agents_page.edn (label → “Agents”)

 (己, p=0.95) Verification:
 - pnpm -C frontend typecheck ✅
 - pnpm -C frontend test ✅ (all tests passed)

 (己, p=0.95) Commit:
 - Knoxx: unify Agents control + audit logs at /agents (SHA 7addf2f0)

 Frames

 (己, p=0.6) This is primarily a routing + IA consolidation: users now have one “Agents” place, with control-plane and observability separated by tabs rather than separate top-level pages.
 (己, p=0.55) This is an observability correctness move: “Agent Audit Logs” now enforces “OpenPlanner events only” by construction (no mixing in ad-hoc runtime event feeds).
 (己, p=0.5) This is a componentization seed: AgentAuditLogs is now a re-usable two-panel surface with page-provided built-in filters (builtInSessionQuery, builtInRowQuery), but it’s not yet a fully generic “panel system” beyond this one component.

 Countermoves

 (己, p=0.65) If the intention was to keep live run detail (run settings, prompt transcript, tool receipts) in the audit tab, this change intentionally drops that in favor of OpenPlanner session truth—confirm whether we still want a third “Live Run
 Inspector” sub-tab.
 (己, p=0.6) The “context-aware” filtering from selected event-agent job → sessions is currently best-effort keyword seeding (role/name/id), because OpenPlanner sessions don’t expose a first-class agent_role field in the list API—if you want hard
 linkage, we should extend session metadata and filter server-side.
 (己, p=0.6) Multimodal rendering in audit logs currently depends on row.extra.contentParts / row.extra.receipt.contentParts existing; if OpenPlanner rows encode media differently, we should normalize that upstream or expand the extractor.

 Next

 (己, p=0.75) Tell me what the truncated requirement was (“…it has a …”) for the filtered list of running jobs on the selected agent, and I’ll implement that linkage (including the exact “running job” definition and where it should render).


 agent role filter is wrong. It's suposed to be by agent contract. Right now these are totally empty, probably because openplanner doesn't container role meta data, nor would that be the desired behavior if it did. The desired behavior is that each
 session is tracking the actor responsible for it, and the agent contract that actor is running under in that session.
