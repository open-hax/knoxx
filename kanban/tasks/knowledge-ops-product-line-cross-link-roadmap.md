---
uuid: "knoxx-knowledge-ops-product-line-cross-link-roadmap"
title: "Document cross-product integration flows and update roadmap status"
status: icebox
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---
# Document cross-product integration flows and update roadmap status

> Parent epic: `knoxx-knowledge-ops-product-line`
> Points: 2

## Purpose

Translate the four cross-product integration flows defined in the parent epic (KnowledgeOps↔ExposureMonitor, KnowledgeOps↔TheLake, KnowledgeOps↔Shibboleth, ExposureMonitor↔TheLake) into kanban-linked specs and update `knowledge-ops-roadmap-status.md` and `knowledge-ops-full-roadmap.md` to reflect current product-line progress.

## Scope

- Write `specs/knowledge-ops-product-line-integration-flows.md` describing the four integration flows with data-flow diagrams (ASCII) and the shared infrastructure touchpoints for each
- Update `orgs/open-hax/openplanner/packages/agents/knoxx/kanban/epics/knowledge-ops-roadmap-status.md` to include the product-line epic and its subtasks
- Update `orgs/open-hax/openplanner/packages/agents/knoxx/kanban/epics/knowledge-ops-full-roadmap.md` to position all four products in the overall roadmap timeline
- Cross-link the new integration-flows spec from the relevant existing epics (exposure-monitor, ingestion-pipeline, shibboleth-lite-labeling)

## Definition of done

- `specs/knowledge-ops-product-line-integration-flows.md` is written with all four flows documented
- `knowledge-ops-roadmap-status.md` lists the product-line epic with links to its child tasks
- `knowledge-ops-full-roadmap.md` has an updated product-line section with status markers
- At least three existing epic files contain a backlink to the integration-flows spec

## Notes

Split from parent epic `knoxx-knowledge-ops-product-line` on 2026-05-30.
