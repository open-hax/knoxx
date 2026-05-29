---
uuid: "knoxx-contract-runtime-merge-proposal"
title: "Contract Runtime Unification Proposal"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.378Z"
source: "specs/epics/contract-runtime-merge-proposal.md"
points: null
category: epics
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
