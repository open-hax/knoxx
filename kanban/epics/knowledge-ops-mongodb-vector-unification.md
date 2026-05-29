---
uuid: "knoxx-knowledge-ops-mongodb-vector-unification"
title: "The Lake — MongoDB Vector Unification Spec"
status: accepted
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.389Z"
source: "specs/epics/knowledge-ops-mongodb-vector-unification.md"
points: null
category: epics
---
# The Lake — MongoDB Vector Unification Spec

> Source: `specs/epics/knowledge-ops-mongodb-vector-unification.md`

> *One database. Structured storage + vector search + full-text search. No sidecar vector stores.*

---

## What Happened

In September 2025, MongoDB shipped `$vectorSearch` and `$search` for Community Edition and Enterprise Server. Not Atlas-only. Self-managed. Free under SSPL. Same APIs as Atlas. Public preview as of 8.2.

This means: **MongoDB can now be the only database.** Structured storage, full-text search, AND vector similarity search, all in one query language, one connection string, one transaction boundary.

The current OpenPlanner stack is three databases duct-taped together:

| Current | What it does | Replace with |
|---------|-------------|-------------|
| DuckDB | Events table, compacted_memories table, FTS | MongoDB collections + `$search` |
