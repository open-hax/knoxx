# Knoxx knowledge-ops specs

This directory is now the canonical home for the Knoxx / knowledge-ops spec set that previously lived in `specs/drafts/`.

## Spec Size Rules

- No executable spec may exceed **5 story points**
- Specs over 5 points must be split into an **epic wrapper** + **child specs**
- Each child spec is independently executable

## Active Epics

### Graph Memory (P1B)

Epic: `knowledge-ops-graph-memory-reconciliation.md`

Child specs:
- `knowledge-ops-knoxx-health-route-coherence.md` (3 pts)
- `knowledge-ops-kms-openplanner-ingest-arity-fix.md` (2 pts)
- `knowledge-ops-openplanner-graph-population-smoke.md` (5 pts)
- `knowledge-ops-myrmex-openplanner-write-recovery.md` (3 pts)
- `knowledge-ops-graph-weaver-live-sync-truth.md` (5 pts)
- `knowledge-ops-graph-memory-runtime-smoke-e2e.md` (3 pts)
- `knowledge-ops-knoxx-graph-query-contract-v1.md` (3 pts)
- `knowledge-ops-docs-source-of-truth-normalization.md` (2 pts)
- `knowledge-ops-openplanner-derived-edge-projections-slice.md` (5 pts)
- `knowledge-ops-adaptive-expand-policy-seam.md` (2 pts)
- `knowledge-ops-adaptive-expand-policy-telemetry.md` (2 pts)

### Translation Review (Client Priority)

Epic: `knowledge-ops-translation-review-epic.md` (15 pts total)

Child specs:
- `knowledge-ops-translation-routes.md` (5 pts) — critical path
- `knowledge-ops-translation-export.md` (2 pts)
- `knowledge-ops-translation-review-ui.md` (5 pts)
- `knowledge-ops-translation-mt-pipeline.md` (3 pts) — deferrable

## Review notes

See:

- `orgs/open-hax/knoxx/specs/knowledge-ops-consistency-review.md`
- `orgs/open-hax/knoxx/specs/knowledge-ops-roadmap-status.md`

The consistency review captures the current cross-spec inconsistencies and recommends the canonical Knoxx-aligned interpretation.
The roadmap status maps the spec corpus onto the current Knoxx implementation and identifies the next logical implementation slice.
The full roadmap accounts for the entire `knowledge-ops-*.md` corpus across active, later, exploratory, and legacy-donor lanes.
The Knoxx distribution doctrine names Knoxx as the opinionated packaged product over more generic subsystem repos.
The graph-memory reconciliation spec grounds the GraphRAG / graph-memory direction in the current Knoxx + OpenPlanner + Graph-Weaver source/runtime reality.
The graph-memory roadmap turns that reconciliation epic into an ordered execution plan.

- `orgs/open-hax/knoxx/specs/knowledge-ops-full-roadmap.md`
- `orgs/open-hax/knoxx/specs/knowledge-ops-knoxx-opinionated-distribution.md`
- `orgs/open-hax/knoxx/specs/knowledge-ops-graph-memory-roadmap.md`

The reconciliation epic is now split into board-sized child specs:

- `knowledge-ops-knoxx-health-route-coherence.md`
- `knowledge-ops-kms-openplanner-ingest-arity-fix.md`
- `knowledge-ops-openplanner-graph-population-smoke.md`
- `knowledge-ops-myrmex-openplanner-write-recovery.md`
- `knowledge-ops-graph-weaver-live-sync-truth.md`
- `knowledge-ops-graph-memory-runtime-smoke-e2e.md`
- `knowledge-ops-knoxx-graph-query-contract-v1.md`
- `knowledge-ops-docs-source-of-truth-normalization.md`
- `knowledge-ops-openplanner-derived-edge-projections-slice.md`
- `knowledge-ops-adaptive-expand-policy-seam.md`
- `knowledge-ops-adaptive-expand-policy-telemetry.md`

First-pass canonicalization has started in these high-value specs:

- `knowledge-ops-architecture-migration.md`
- `knowledge-ops-clojure-backend-migration.md`
- `knowledge-ops-kms-query.md`
- `knowledge-ops-role-scoped-lakes.md`
- `knowledge-ops-workbench-ui.md`
- `knowledge-ops-deployment.md`
- `knowledge-ops-adaptive-web-frontier-and-multiscale-backbone.md`
- `knowledge-ops-graph-memory-reconciliation.md`

## Spec inventory

- `knowledge-ops-adaptive-web-frontier-and-multiscale-backbone.md`
- `knowledge-ops-architecture-migration.md`
- `knowledge-ops-chat-ui-library.md`
- `knowledge-ops-chat-widget-layers.md`
- `knowledge-ops-clojure-backend-migration.md`
- `knowledge-ops-cms-data-model.md`
- `knowledge-ops-demo-seed.md`
- `knowledge-ops-deploy-aws.md`
- `knowledge-ops-deploy-azure.md`
- `knowledge-ops-deploy-local.md`
- `knowledge-ops-deploy-self-hosted.md`
- `knowledge-ops-deployment.md`
- `knowledge-ops-exposure-monitor.md`
- `knowledge-ops-federated-lakes.md`
- `knowledge-ops-gap-analysis-prior-art.md`
- `knowledge-ops-gardens.md`
- `knowledge-ops-graph-memory-reconciliation.md`
- `knowledge-ops-ingestion-pipeline.md`
- `knowledge-ops-ingestion-throttling.md`
- `knowledge-ops-integration.md`
- `knowledge-ops-kms-query.md`
- `knowledge-ops-legacy-ui-inventory.md`
- `knowledge-ops-mongodb-vector-unification.md`
- `knowledge-ops-multi-provider-epic.md`
- `knowledge-ops-multi-tenant-control-plane.md`
- `knowledge-ops-mvp-phase1-epics.md`
- `knowledge-ops-pii-handling-protocol.md`
- `knowledge-ops-platform-stack-architecture.md`
- `knowledge-ops-product-line.md`
- `knowledge-ops-promethean-stack.md`
- `knowledge-ops-provider-abstraction.md`
- `knowledge-ops-role-scoped-lakes.md`
- `knowledge-ops-shibboleth-lite-labeling.md`
- `knowledge-ops-the-lake.md`
- `knowledge-ops-ui-design-system.md`
- `knowledge-ops-workbench-ui.md`
