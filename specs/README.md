# Knoxx specs

Reviewed and reorganized: 2026-05-21.

## Layout

- `specs/epics/` — active epic wrappers, roadmaps, architecture/doctrine docs, and large future designs that are not directly executable as one small task.
- `specs/tasks/` — active executable work items, bugfix specs, review tasks, and child specs intended to stay at or below 5 story points.
- `specs/tasks/workbench/` — active Workbench child tasks.
- `specs/archived/` — completed, superseded, or legacy/reference specs that should not be mistaken for current execution truth.
  - `specs/archived/epics/` — completed epic wrappers.
  - `specs/archived/tasks/` — completed task specs, including completed Workbench child tasks under `workbench/`.
  - `specs/archived/reference/` — historical inventories, old research, superseded designs, and legacy-donor docs.

## Placement rules

- Keep executable specs in `specs/tasks/` only while they still represent unfinished or reviewable work.
- Move any spec over 5 story points into `specs/epics/` and split it into child tasks.
- Archive tasks once they are explicitly `done`, `complete`, `implemented`, superseded, or old enough that they are no longer active board work.
- Archive legacy/reference docs when they are useful archaeology but no longer current implementation truth.
- Keep `README.md` at the root; all other spec documents belong in one of the three buckets above.

## Counts

- Active epics: 46
- Active tasks: 32
- Archived epics: 2
- Archived tasks: 29
- Archived reference docs: 12

## Active epics

- `specs/epics/broadcast-studio-playlist-publication-and-block-cms.md`
- `specs/epics/contract-runtime-merge-proposal.md`
- `specs/epics/events-agent-runtime-separation.md`
- `specs/epics/folder-backed-visual-cms-design-spec.md`
- `specs/epics/garden-cms-playlist-chat-and-label-provenance.md`
- `specs/epics/knowledge-ops-adaptive-expand-policy-hook.md`
- `specs/epics/knowledge-ops-adaptive-web-frontier-and-multiscale-backbone.md`
- `specs/epics/knowledge-ops-architecture-migration.md`
- `specs/epics/knowledge-ops-chat-ui-library.md`
- `specs/epics/knowledge-ops-chat-widget-layers.md`
- `specs/epics/knowledge-ops-clojure-backend-migration.md`
- `specs/epics/knowledge-ops-cms-data-model.md`
- `specs/epics/knowledge-ops-consistency-review.md`
- `specs/epics/knowledge-ops-contract-runtime-dod-restructure.md`
- `specs/epics/knowledge-ops-deploy-aws.md`
- `specs/epics/knowledge-ops-deploy-azure.md`
- `specs/epics/knowledge-ops-deploy-self-hosted.md`
- `specs/epics/knowledge-ops-exposure-monitor.md`
- `specs/epics/knowledge-ops-federated-lakes.md`
- `specs/epics/knowledge-ops-full-roadmap.md`
- `specs/epics/knowledge-ops-gardens.md`
- `specs/epics/knowledge-ops-graph-memory-reconciliation.md`
- `specs/epics/knowledge-ops-graph-memory-roadmap.md`
- `specs/epics/knowledge-ops-ingestion-architecture.md`
- `specs/epics/knowledge-ops-ingestion-pipeline.md`
- `specs/epics/knowledge-ops-kms-query.md`
- `specs/epics/knowledge-ops-knoxx-opinionated-distribution.md`
- `specs/epics/knowledge-ops-mongodb-vector-unification.md`
- `specs/epics/knowledge-ops-multi-provider-epic.md`
- `specs/epics/knowledge-ops-multi-tenant-control-plane.md`
- `specs/epics/knowledge-ops-mvp-phase1-epics.md`
- `specs/epics/knowledge-ops-pii-handling-protocol.md`
- `specs/epics/knowledge-ops-product-line.md`
- `specs/epics/knowledge-ops-provider-abstraction.md`
- `specs/epics/knowledge-ops-roadmap-status.md`
- `specs/epics/knowledge-ops-role-scoped-lakes.md`
- `specs/epics/knowledge-ops-shibboleth-lite-labeling.md`
- `specs/epics/knowledge-ops-source-lakes-cross-lake-graph.md`
- `specs/epics/knowledge-ops-translation-review-epic.md`
- `specs/epics/knowledge-ops-ui-design-system.md`
- `specs/epics/knowledge-ops-workbench-ui.md`
- `specs/epics/knowledge-ops-workbench-ux-breakdown.md`
- `specs/epics/knowledge-ops-workbench-ux-v1.md`
- `specs/epics/knoxx-backend-law-shape-domain-epic.md`
- `specs/epics/knoxx-session-lake-graph-and-memory.md`
- `specs/epics/unified-workplace-pattern.md`

## Active tasks

- `specs/tasks/knowledge-ops-adaptive-expand-policy-seam.md`
- `specs/tasks/knowledge-ops-adaptive-expand-policy-telemetry.md`
- `specs/tasks/knowledge-ops-demo-seed.md`
- `specs/tasks/knowledge-ops-docs-source-of-truth-normalization.md`
- `specs/tasks/knowledge-ops-gardens-page-bugfixes.md`
- `specs/tasks/knowledge-ops-graph-memory-runtime-smoke-e2e.md`
- `specs/tasks/knowledge-ops-graph-weaver-live-sync-truth.md`
- `specs/tasks/knowledge-ops-kms-openplanner-ingest-arity-fix.md`
- `specs/tasks/knowledge-ops-knoxx-graph-query-contract-v1.md`
- `specs/tasks/knowledge-ops-knoxx-health-route-coherence.md`
- `specs/tasks/knowledge-ops-myrmex-openplanner-write-recovery.md`
- `specs/tasks/knowledge-ops-openplanner-derived-edge-projections-slice.md`
- `specs/tasks/knowledge-ops-openplanner-gardens-backend-fixes.md`
- `specs/tasks/knowledge-ops-openplanner-graph-population-smoke.md`
- `specs/tasks/knowledge-ops-translation-document-review-v2.md`
- `specs/tasks/knowledge-ops-translation-export.md`
- `specs/tasks/knowledge-ops-translation-mt-pipeline.md`
- `specs/tasks/knowledge-ops-translation-routes.md`
- `specs/tasks/knoxx-backend-data-shapes-review.md`
- `specs/tasks/knoxx-openplanner-mcp-integration.md`
- `specs/tasks/openplanner-mcp-server.md`
- `specs/tasks/workbench/1.2-dashboard-agent-runs.md`
- `specs/tasks/workbench/1.3-dashboard-memory-activity.md`
- `specs/tasks/workbench/2.2-content-editor-ai-suggestions.md`
- `specs/tasks/workbench/2.3-content-editor-provenance.md`
- `specs/tasks/workbench/3.3-review-correction-writeback.md`
- `specs/tasks/workbench/4.1-memory-search.md`
- `specs/tasks/workbench/4.2-memory-focal-node.md`
- `specs/tasks/workbench/4.3-memory-history-slider.md`
- `specs/tasks/workbench/5.1-agent-run-list.md`
- `specs/tasks/workbench/5.2-agent-run-detail.md`
- `specs/tasks/workbench/5.3-agent-scratchpad.md`

## Archived epics

- `specs/archived/epics/knoxx-agent-service-protocol-split-epic.md`
- `specs/archived/epics/knoxx-js-cljs-boundary-hardening-epic.md`

## Archived task groups

- Completed task specs: `specs/archived/tasks/*.md`
- Completed Workbench child tasks: `specs/archived/tasks/workbench/*.md`

## Archived reference docs

- `specs/archived/reference/ingestion-surface-inventory.md`
- `specs/archived/reference/knowledge-ops-deploy-local.md`
- `specs/archived/reference/knowledge-ops-deployment.md`
- `specs/archived/reference/knowledge-ops-gap-analysis-prior-art.md`
- `specs/archived/reference/knowledge-ops-integration.md`
- `specs/archived/reference/knowledge-ops-legacy-ui-inventory.md`
- `specs/archived/reference/knowledge-ops-platform-stack-architecture.md`
- `specs/archived/reference/knowledge-ops-promethean-stack.md`
- `specs/archived/reference/knowledge-ops-the-lake.md`
- `specs/archived/reference/knowledge-ops-translation-review-ui.md`
- `specs/archived/reference/knowledge-ops-translation-triage-2026-04-12.md`
- `specs/archived/reference/simple-markdown-gardens.md`
