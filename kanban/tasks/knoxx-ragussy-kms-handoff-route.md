---
uuid: "knoxx-ragussy-kms-handoff-route"
title: "Implement Ragussy to futuresight-kms labeled-data handoff route"
status: incoming
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---

# Implement Ragussy to futuresight-kms labeled-data handoff route

> Parent epic: `knoxx-knowledge-ops-shibboleth-lite-labeling`
> Points: 2

## Purpose

Build the missing export/handoff path that takes reviewed and labeled knowledge items from the Ragussy pipeline and delivers them to the futuresight-kms store, closing the gap identified in the parent epic's gap analysis.

## Scope

- Add a backend route (Fastify handler) or ingestion script that pulls completed-label records from Ragussy and POSTs/writes them to the futuresight-kms handoff endpoint
- Include provenance metadata (labeler ID, timestamp, label schema version) in the exported payload
- Write an integration test or manual smoke-test script verifying a round-trip from a labeled item in Ragussy to a persisted record in futuresight-kms

## Definition of done

- A documented route or script exists that performs the Ragussy → futuresight-kms export
- Provenance fields (labeler, timestamp, schema version) are present in every exported record
- The handoff path is exercised by at least one automated test or a documented manual test procedure

## Notes

Split from parent epic `knoxx-knowledge-ops-shibboleth-lite-labeling` on 2026-05-30.
