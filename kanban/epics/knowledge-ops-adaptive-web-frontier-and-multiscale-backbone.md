---
labels: "["epics"]"
---

---
uuid: "knoxx-knowledge-ops-adaptive-web-frontier-and-multiscale-backbone"
title: "Knowledge Ops — Adaptive Web Frontier + Multiscale Backbone Spec"
status: icebox
priority: "P2"
labels: "["epics"]"
created_at: "2026-05-28T22:40:14.381Z"
source: "specs/epics/knowledge-ops-adaptive-web-frontier-and-multiscale-backbone.md"
points: null
category: "epics"
---

# Knowledge Ops — Adaptive Web Frontier + Multiscale Backbone Spec

> Source: `specs/epics/knowledge-ops-adaptive-web-frontier-and-multiscale-backbone.md`

## Status
Draft

## Current canonical reading

- canonical lake backend: `orgs/open-hax/openplanner`
- canonical web producer: `orgs/octave-commons/myrmex`
- traversal brain: `orgs/octave-commons/graph-weaver-aco`
- fetch/render/extraction backend: `orgs/shuv/shuvcrawl`
- primary graph workbench: `orgs/octave-commons/graph-weaver`
- canonical lake model: `knowledge-ops-source-lakes-cross-lake-graph.md`
- implementation companion specs:
  - `orgs/octave-commons/myrmex/specs/adaptive-frontier-salience-and-template-aware-pruning.md`
  - `orgs/open-hax/openplanner/specs/openplanner-web-edge-salience-and-backbone-projections.md`

---
Triage 2026-05-29: Epic-level spec umbrella for the Adaptive Web Frontier + Multiscale Backbone system. Purpose is clear — coordinate the adaptive frontier salience, template-aware pruning (Myrmex), and multiscale backbone projection (OpenPlanner) work across canonical repos. Companion implementation specs exist (openplanner-web-edge-salience-and-backbone-projections, adaptive-frontier-salience-and-template-aware-pruning) and a child task (knoxx-knowledge-ops-openplanner-derived-edge-projections-slice, currently blocked on upstream P1 graph write/smoke tasks) is already in the kanban. The parent spec file at orgs/open-hax/knoxx/specs/ does not yet exist — authoring it is part of the scope. No hard external blockers at the epic level; the epic can be accepted and broken down while child tasks sequence through their own gates. P2 is appropriate — this is a significant multi-system design spec that should progress this sprint but is not a production incident. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban

Breakdown 2026-05-29: The epic spans four canonical repos (OpenPlanner, Myrmex, graph-weaver-aco, shuvcrawl) with zero derived projection infrastructure in place — no web_edge_salience_v1/web_backbone_membership_v1 collections, no edgeView query param in graph.ts (5169 lines), and the parent spec file path (orgs/open-hax/knoxx/specs/) does not exist. Three child tasks already in the kanban cover the full scope: umbrella spec authoring (2sp, incoming → ready gate passed), Phase 1 projection table --tasks-dir orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
