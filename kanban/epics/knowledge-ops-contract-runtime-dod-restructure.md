---
uuid: "knoxx-knowledge-ops-contract-runtime-dod-restructure"
title: "Contract Runtime: Data-Oriented Restructure"
status: ready
priority: "P2"
labels: ["epics"]
created_at: "2026-05-28T22:40:14.383Z"
source: "specs/epics/knowledge-ops-contract-runtime-dod-restructure.md"
points: null
category: "epics"
---
# Contract Runtime: Data-Oriented Restructure

> Source: `specs/epics/knowledge-ops-contract-runtime-dod-restructure.md`

**Date:** 2026-04-19  
**Status:** Accepted  
**Supersedes:** `docs/notes/2026.04.17.10.11.17.md`

---
## Framing: Why "Actor"

The system needs a single term for any decision-making entity — human or AI. "User" is human-only by convention. "Agent" is AI-only by convention. "Actor" is semantically neutral: it denotes any entity that has agency and takes actions in the system. The term appears in the actor model of computation, in theatre (a role that acts), and in legal contexts (a party who acts). None of these connotations conflict with the intended meaning here.

- `:actor/kind :human` — a person operating through the UI or API
- `:actor/kind :ai` — an AI agent operating under a contract
- All actors have an id, roles, and capabilities. No other distinction at the data layer.

Triage 2026-05-29: Spec is thorough and internally consistent — defines a data-oriented separation of the Knoxx contract runtime (EDN data layer vs. ClojureScript interpreter layer), with a clear 9-step migration order, dependency graph with no cycles, deletion table, and a named supersession of the prior plan. No external blockers are identified; the migration is fully self-contained within the knoxx backend. Purpose is clear (eliminate god-objects, dissolve policy_db.cljs, replace inlined role/tool data with EDN files), scope is large but broken into safe sequential steps, and DoD is implied per step. Suitable for breakdown next. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
