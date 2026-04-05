# Knoxx knowledge-ops full roadmap

Date: 2026-04-05
Status: active program roadmap

## Purpose

Provide one roadmap that accounts for **the full `knowledge-ops-*.md` corpus**:

- active specs
- active epic wrappers
- board-sized child specs
- exploratory future work
- legacy-donor/reference docs
- meta roadmap/review docs

This document is the master roadmap for the Knoxx knowledge-ops corpus.
It does **not** replace the individual specs.
It orders them, classifies them, and shows how they relate.

## How to read this roadmap

1. **Board-sized child specs** are executable work items.
2. **Epic wrappers** are roadmap containers and should be split before direct execution.
3. **Partial** means real intent is already visible in the current Knoxx stack, but the product contract is not complete yet.
4. **Legacy-donor** means the doc is useful as reference material, not as current execution truth.
5. **Exploratory** means important later, but not on the current critical path.

## Program shape

The roadmap has two immediate near-term tracks and then a set of dependent follow-on waves.

### Immediate Track A — tenant foundation and runtime enforcement

This is the highest product-risk reduction path.

Exit gate:

- every protected request resolves org/user/membership context
- routes fail closed without valid context
- tool execution is policy-backed
- cross-org denial is covered by negative tests

### Immediate Track B — graph-memory runtime coherence

This is the highest architecture-truth path.

Exit gate:

- producers can write into canonical graph truth
- OpenPlanner graph endpoints are populated and queryable
- Graph-Weaver reflects canonical truth or loudly declares degraded fallback
- Knoxx graph contract is frozen against real behavior

### Follow-on waves

After Tracks A and B, the roadmap proceeds through:

1. retrieval/lake convergence
2. CMS/review/public boundary
3. compliance/audit/safety
4. operator UX/productization
5. deployment/provider portability
6. advanced graph intelligence

## Program order summary

| Slot | Focus | Depends on | Exit signal |
|------|-------|------------|-------------|
| P0 | landed baseline / keep green | none | current Knoxx runtime truth stays healthy while new work lands |
| P1A | tenant foundation + policy enforcement | P0 | request-scoped tenant/org/user enforcement is real |
| P1B | graph-memory runtime coherence | P0 | canonical graph path is coherent end to end |
| P2 | retrieval, lake, and ingestion convergence | P1A + P1B | scoped retrieval and lake model match runtime truth |
| P3 | CMS, review, and public/internal boundary | P1A + P2 | content lifecycle and review boundary are product-real |
| P4 | PII, audit, and exposure controls | P1A + P2 + P3 | sensitive-content handling becomes enforceable and auditable |
| P5 | workbench/UI/productization | P1A + P2 + P3 | UI reflects actual scope, review, and graph/retrieval semantics |
| P6 | deployment, provider portability, product line | P3 + P4 + P5 | packaging/portability work builds on stable product contracts |
| P7 | advanced graph intelligence | P1B + P2 | derived views and adaptive expansion land behind stable contracts |
| R | reference / donor archive | none | docs remain available without being mistaken for current execution truth |

## Pmeta — coordination and canonical reading docs

| Spec / epic | Status | Slot | Note |
|-------------|--------|------|------|
| `knowledge-ops-consistency-review.md` | active reference | Pmeta | canonical naming/path/era normalization across the corpus |
| `knowledge-ops-roadmap-status.md` | active reference | Pmeta | status matrix for the moved corpus against the current Knoxx app |
| `knowledge-ops-knoxx-opinionated-distribution.md` | active doctrine | Pmeta | Knoxx as packaged opinionated distribution over reusable subsystem organs |
| `knowledge-ops-graph-memory-roadmap.md` | active roadmap | P1B support | detailed execution order for the graph-memory tranche |
| `knowledge-ops-full-roadmap.md` | active roadmap | Pmeta | master roadmap across all specs and epics |

## P0 — landed baseline / keep green

| Spec / epic | Status | Slot | Note |
|-------------|--------|------|------|
| `knowledge-ops-clojure-backend-migration.md` | landed | P0 | closest spec to current backend reality; preserve as baseline truth |
| `knowledge-ops-kms-query.md` | landed | P0 | current Knoxx retrieval/query surface already exists and must stay working |
| `knowledge-ops-architecture-migration.md` | partial | P0 support | services-thin / packages-canonical cleanup remains relevant but is no longer the primary blocker |

## P1A — tenant foundation and runtime enforcement

| Spec / epic | Status | Slot | Note |
|-------------|--------|------|------|
| `knowledge-ops-multi-tenant-control-plane.md` | next epic | P1A | most important missing bridge between current Knoxx and intended product model |
| `knowledge-ops-mvp-phase1-epics.md` | next epic | P1A | Tenant Foundation goes first; later Secure Ingestion, PII, Retrieval, and Audit epics feed P3/P4 |

### P1A interpretation

Treat these as the primary **product-fidelity** epics.
Before later product claims, Knoxx needs request-scoped identity, fail-closed authorization, and policy-backed runtime enforcement.

## P1B — graph-memory runtime coherence

| Spec / epic | Status | Slot | Note |
|-------------|--------|------|------|
| `knowledge-ops-graph-memory-reconciliation.md` | active epic wrapper | P1B | canonical current-state bridge for graph-memory direction |
| `knowledge-ops-kms-openplanner-ingest-arity-fix.md` | next | P1B / M0 | restore KMS ingestion writes into OpenPlanner |
| `knowledge-ops-knoxx-health-route-coherence.md` | next | P1B / M0 | make Knoxx health truthful and stable in local dev |
| `knowledge-ops-myrmex-openplanner-write-recovery.md` | next | P1B / M0 | restore Myrmex → OpenPlanner write path |
| `knowledge-ops-openplanner-graph-population-smoke.md` | next | P1B / M1 | prove canonical graph truth is populated in live Mongo-backed OpenPlanner |
| `knowledge-ops-graph-weaver-live-sync-truth.md` | next | P1B / M2 | align Graph-Weaver display truth with current canonical OpenPlanner state |
| `knowledge-ops-graph-memory-runtime-smoke-e2e.md` | next | P1B / M3 | add one producer → lake → workbench → Knoxx smoke path |
| `knowledge-ops-knoxx-graph-query-contract-v1.md` | next | P1B / M4 | freeze bounded graph query semantics around actual runtime behavior |
| `knowledge-ops-docs-source-of-truth-normalization.md` | next | P1B / M4 | stop README/spec drift from fighting current graph-memory reality |

### P1B interpretation

This is the primary **architecture-truth** tranche.
Do not jump to adaptive traversal or derived graph cleverness until P1B is green.

## P2 — retrieval, lake, and ingestion convergence

| Spec / epic | Status | Slot | Note |
|-------------|--------|------|------|
| `knowledge-ops-role-scoped-lakes.md` | partial | P2 | becomes truly real only after P1A enforcement exists |
| `knowledge-ops-federated-lakes.md` | partial | P2 | multi-lake retrieval/UI follow once role/lake scope is enforced |
| `knowledge-ops-source-lakes-cross-lake-graph.md` | partial | P2 | canonical one-lake-per-source graph model for `devel`/`web`/`bluesky` |
| `knowledge-ops-ingestion-pipeline.md` | partial | P2 | ingestion exists but needs convergence away from older storage assumptions |
| `knowledge-ops-ingestion-throttling.md` | backlog | P2 later | useful after ingest pressure returns as a real bottleneck |

### P2 interpretation

P2 is where retrieval and graph/lake semantics stop being conceptual presets and become an actual scoped product contract.

## P3 — CMS, review, and public/internal boundary

| Spec / epic | Status | Slot | Note |
|-------------|--------|------|------|
| `knowledge-ops-cms-data-model.md` | partial | P3 | OpenPlanner-first CMS direction is right, but lifecycle semantics are incomplete |
| `knowledge-ops-chat-widget-layers.md` | partial | P3 | still useful for internal/public boundary and CMS/product layering |
| `knowledge-ops-shibboleth-lite-labeling.md` | backlog | P3 later | review/handoff/labeling layer after scoped content lifecycle is real |

### P3 interpretation

This wave makes document lifecycle, review, and publication boundaries into a product surface instead of a loose collection of routes.

## P4 — PII, audit, and exposure controls

| Spec / epic | Status | Slot | Note |
|-------------|--------|------|------|
| `knowledge-ops-pii-handling-protocol.md` | backlog | P4 | only becomes meaningful once access scope and content lifecycle are enforceable |
| `knowledge-ops-exposure-monitor.md` | exploratory | P4 later | useful later as monitoring/audit surface, not immediate critical path |

### P4 interpretation

This wave is intentionally after P1A and P3, because sensitive-data promises without enforceable scope and review boundaries are fake safety.

## P5 — workbench UX and productization

| Spec / epic | Status | Slot | Note |
|-------------|--------|------|------|
| `knowledge-ops-workbench-ui.md` | partial | P5 | current workbench is real, but richer scoped context/review/graph surfaces remain ahead |
| `knowledge-ops-ui-design-system.md` | partial | P5 | token/primitives direction is real, but broader system-wide discipline remains incomplete |
| `knowledge-ops-gardens.md` | partial | P5 | useful operator/product concept once retrieval and truth boundaries are stable |
| `knowledge-ops-chat-ui-library.md` | backlog | P5 later | shared UI library extraction should follow product-surface stabilization |
| `knowledge-ops-demo-seed.md` | backlog | P5 later | demo/seed story is best once the real product surfaces are stable enough to showcase |

### P5 interpretation

Do not over-polish the UI before the enforcement, retrieval, and CMS boundaries it is supposed to represent are real.

## P6 — deployment, provider portability, and product line

| Spec / epic | Status | Slot | Note |
|-------------|--------|------|------|
| `knowledge-ops-provider-abstraction.md` | exploratory | P6 | useful after the current product contract settles |
| `knowledge-ops-multi-provider-epic.md` | exploratory epic | P6 | provider work is real later, but not the shortest path now |
| `knowledge-ops-deploy-self-hosted.md` | exploratory | P6 | packaging direction after core product behavior is stable |
| `knowledge-ops-deploy-aws.md` | exploratory | P6 | cloud option, not current bottleneck |
| `knowledge-ops-deploy-azure.md` | exploratory | P6 | cloud option, not current bottleneck |
| `knowledge-ops-mongodb-vector-unification.md` | exploratory | P6 | design option, not current Knoxx reality |
| `knowledge-ops-product-line.md` | exploratory | P6 | product strategy framing after current product contract settles |

### P6 interpretation

Portability and packaging should ride on top of a stable product, not substitute for finishing the product.

## P7 — advanced graph intelligence and frontier work

| Spec / epic | Status | Slot | Note |
|-------------|--------|------|------|
| `knowledge-ops-openplanner-derived-edge-projections-slice.md` | later | P7 | first derived graph view after canonical graph truth is trustworthy |
| `knowledge-ops-adaptive-expand-policy-hook.md` | later epic wrapper | P7 | wrapper for future adaptive expansion behind stable contracts |
| `knowledge-ops-adaptive-expand-policy-seam.md` | later | P7 step 1 | explicit policy seam behind bounded graph operations |
| `knowledge-ops-adaptive-expand-policy-telemetry.md` | later | P7 step 2 | telemetry for comparing future adaptive policies to baseline |
| `knowledge-ops-adaptive-web-frontier-and-multiscale-backbone.md` | exploratory | P7 later | advanced frontier/backbone research after graph-memory runtime coherence and contract stability |

### P7 interpretation

This is where the clever graph work goes **after** runtime coherence and bounded contract stability, not before.

## R — reference, legacy-donor, and archive lane

| Spec / epic | Status | Slot | Note |
|-------------|--------|------|------|
| `knowledge-ops-legacy-ui-inventory.md` | legacy-donor | R | useful archaeology only |
| `knowledge-ops-deployment.md` | legacy-donor | R | older deployment framing, not current stack truth |
| `knowledge-ops-deploy-local.md` | legacy-donor | R | some ideas still useful, but not the canonical local setup |
| `knowledge-ops-integration.md` | legacy-donor | R | mostly superseded by the CLJS backend cutover |
| `knowledge-ops-platform-stack-architecture.md` | legacy-donor | R | strategic framing, not active implementation contract |
| `knowledge-ops-the-lake.md` | legacy-donor | R | important lineage, too mixed-era for direct execution truth |
| `knowledge-ops-promethean-stack.md` | legacy-donor | R | useful historical/product lineage material |
| `knowledge-ops-gap-analysis-prior-art.md` | legacy-donor | R | research/reference, not current implementation target |

## Immediate execution guidance

If the goal is **shortest path to real product truth**, prioritize in this order:

1. **P1A** tenant foundation and runtime enforcement
2. **P1B** graph-memory runtime coherence
3. **P2** retrieval/lake convergence
4. **P3** CMS/review/public boundary
5. **P4** PII/audit/exposure controls
6. **P5** workbench/product polish
7. **P6** deployment/provider portability
8. **P7** advanced graph intelligence

## What this roadmap says explicitly

1. The backend rewrite is no longer the main problem.
2. The next two real problems are:
   - tenant-aware enforcement
   - graph-memory runtime coherence
3. Retrieval/lake semantics only become product-real once those are in place.
4. CMS/public boundary and PII work depend on that foundation.
5. Provider abstraction and cloud portability are later than product truth.
6. Advanced graph intelligence is later than graph-memory coherence.

## Definition of done for the current program era

Treat the current Knoxx knowledge-ops era as having crossed the main threshold when:

- request-scoped tenancy and policy enforcement are real
- graph-memory runtime coherence is real
- scoped retrieval and lake semantics are real
- CMS/review/public boundaries are real
- PII and audit guarantees are enforceable

At that point, UI polish, portability, and graph intelligence become compounding improvements instead of compensation for unfinished foundations.
