---
uuid: "knoxx-knowledge-ops-source-lakes-cross-lake-graph"
title: "Knowledge Ops — Source Lakes + Cross-Lake Graph Spec"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.393Z"
source: "specs/epics/knowledge-ops-source-lakes-cross-lake-graph.md"
points: null
category: epics
---

# Knowledge Ops — Source Lakes + Cross-Lake Graph Spec

> Source: `specs/epics/knowledge-ops-source-lakes-cross-lake-graph.md`

## Current Canonical Reading

- state: proposed canonical replacement for the split `devel-*` lake model
- canonical lake backend: `orgs/open-hax/openplanner`
- canonical devel producer: `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/jobs/worker.clj`
- canonical web producer: `orgs/octave-commons/myrmex`
- future canonical Bluesky producer: Sintel / Bluesky firehose ingestion
- primary graph workbench: `orgs/octave-commons/graph-weaver`

This spec supersedes the `devel-docs` / `devel-code` / `devel-config` / `devel-data` split as the **lake boundary model**.
Those values remain useful, but only as **node/edge metadata and query filters**, not as separate lakes.

> *One lake per source. Rich labels inside the lake. One graph spanning the lakes.*

---
