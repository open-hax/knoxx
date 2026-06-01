---
uuid: "knoxx-contract-runtime-merge-proposal"
title: "Contract Runtime Unification Proposal"
status: "icebox"
priority: "P3"
labels: ["epics"]
created_at: "2026-05-28T22:40:14.378Z"
source: "specs/epics/contract-runtime-merge-proposal.md"
points: null
category: "epics"
---

# Contract Runtime Unification Proposal

> Source: `specs/epics/contract-runtime-merge-proposal.md`

**Status:** draft
**Created:** 2026-05-06
**Scope:** Merge proxx, knoxx, and eta-mu extension/contract runtimes into a single canonical system

---
## 1. Architecture Analysis

### 1.1 Proxx — Provider Policy Engine

| Dimension | Detail |
|---|---|
| **Language** | ClojureScript (compiled to ESM via shadow-cljs), TypeScript host wrapper |
| **Contract format** | EDN maps with `:contract/id` (keyword), `:contract/kind` |
| **Schema system** | Malli schemas in `proxx.schema` |

Triage 2026-05-29: Epic-level proposal to unify proxx, knoxx, and eta-mu contract/extension runtimes into a single canonical system. The source spec (specs/epics/contract-runtime-merge-proposal.md) does not exist anywhere in the workspace — the kanban task body is a truncated import (32 lines, cuts off mid-table). Full scope, DoD, and sub-tasks cannot be determined. Additionally, the active P0 epic knoxx-knoxx-backend-law-shape-domain-epic is currently in_progress and covers the closely related law/shape domain restructure; runtime unification is a logical sequel but cannot be meaningfully scoped until that foundational work stabilises. Downgrading priority to P3 — this is a good architectural direction but premature. Verdict: icebox (P3). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
