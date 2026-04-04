# Knoxx knowledge-ops roadmap status

Date: 2026-04-04

## Purpose

Map the moved `knowledge-ops-*.md` corpus against the **current Knoxx application** so the specs can be read as:

- what is already landed
- what is partially landed
- what is the next logical implementation slice
- what is still exploratory or legacy-donor material

This is a roadmap/status document, not a replacement for the individual specs.

## Status taxonomy

| Status | Meaning |
|------|---------|
| `landed` | The core spec intent is visible in the current Knoxx stack. |
| `partial` | Important parts are live, but key constraints or product surfaces are still missing. |
| `next` | This spec describes the most logical immediate implementation slice from current reality. |
| `backlog` | Still a real product requirement, but not the shortest path from the current stack. |
| `exploratory` | Valuable direction/research, but not a current Knoxx implementation commitment. |
| `legacy-donor` | Useful historical/donor/reference material; should not be read as the current live contract. |

## Executive summary

### Current Knoxx shape

The highest-leverage Knoxx work is no longer basic runtime bootstrap.
That part largely succeeded.

What is clearly real now:

- Shadow CLJS + Fastify + `@mariozechner/pi-coding-agent` backend runtime
- unified root workbench around **Context Bar**, **Agent Runtime**, and **Scratchpad**
- async run transport, websocket receipts, and live intervention
- OpenPlanner-backed memory search and session resume
- Postgres-backed RBAC/control-plane bootstrap with admin CRUD and e2e coverage
- direct document/database management routes in the CLJS backend

What is still not fully real:

- request-scoped tenant/org/user enforcement across runtime routes
- policy-DB-backed tool authorization at execution time
- role/lake scoping as an actual runtime boundary rather than mostly a conceptual/search preset layer
- full CMS publish/review/public-boundary workflows
- PII classification, log scrubbing, retention, and export controls

## Roadmap readout by lane

| Lane | Current status | Readout |
|------|----------------|---------|
| Runtime rewrite | `landed` | Knoxx is now primarily the CLJS/Fastify/pi-sdk runtime, not the old Python glue layer. |
| Workbench UI | `partial` | The core workbench is real and visible, but some spec ideas like gardens-as-first-class views and richer role-aware context selection remain ahead. |
| Retrieval + memory | `partial` | Search, synthesis, passive hydration, semantic tools, and memory recall are live; role/lake enforcement and better federation UX are still missing. |
| Control plane / RBAC | `partial` → `next` | Postgres control-plane foundation is in place, but runtime enforcement is the missing bridge. |
| CMS / publish boundary | `partial` | Direct document management exists, but a true public/internal review + publish workflow is not yet the product reality. |
| PII / compliance | `backlog` | Still largely spec-only. |
| Provider portability / cloud packaging | `exploratory` | Important later, but not the next best move from the current Knoxx stack. |

## Spec status matrix

### Core runtime and user workbench

| Spec | Status | Current reading |
|------|--------|-----------------|
| `knowledge-ops-architecture-migration.md` | `partial` | The major runtime migration happened, but the wider “services thin, packages canonical” cleanup is not complete across the ecosystem. |
| `knowledge-ops-clojure-backend-migration.md` | `landed` | This is the closest spec to current Knoxx backend reality. The CLJS backend is live and serving the app. |
| `knowledge-ops-workbench-ui.md` | `partial` | The Knoxx root page now matches the core workbench framing, but the full single-surface consolidation and some panel semantics remain incomplete. |
| `knowledge-ops-ui-design-system.md` | `partial` | Monokai/UXX/token direction is real and Knoxx now uses shared markdown/primitives, but the broader modal/chord/system-wide discipline is not complete. |
| `knowledge-ops-chat-ui-library.md` | `backlog` | Shared chat behavior is converging inside Knoxx, but no extracted `packages/chat-ui` library exists yet. |
| `knowledge-ops-chat-widget-layers.md` | `partial` | The layered product model is still useful, but only some of those layers are currently embodied in Knoxx. Public widget and CMS boundary are still ahead. |
| `knowledge-ops-legacy-ui-inventory.md` | `legacy-donor` | Useful archaeology only. The live Knoxx workbench has already moved beyond the inventory phase. |
| `knowledge-ops-gardens.md` | `partial` | Query/ingestion/truth-style operator surfaces exist, but gardens are not yet a first-class Knoxx navigation/product primitive. |

### Retrieval, lakes, memory, and document handling

| Spec | Status | Current reading |
|------|--------|-----------------|
| `knowledge-ops-kms-query.md` | `landed` | The `/api/query/*` retrieval surface is real, though it is no longer the primary interactive Knoxx chat path. |
| `knowledge-ops-role-scoped-lakes.md` | `partial` | Lake classification and conceptual presets exist, but role/lake scope is not yet enforced through request identity and runtime policy. |
| `knowledge-ops-federated-lakes.md` | `partial` | Federated search concepts are real and some multi-project query exists, but full lake-aware UI and policy-aware federation are still ahead. |
| `knowledge-ops-cms-data-model.md` | `partial` | The spec’s OpenPlanner-first correction is aligned with current direction, but the CMS view/mutation/publication layer is still incomplete. |
| `knowledge-ops-ingestion-pipeline.md` | `partial` | Upload and ingestion flows exist, but the spec still reflects older Qdrant/driver/queue assumptions in major sections. |
| `knowledge-ops-ingestion-throttling.md` | `backlog` | Still useful once ingest scales harder, but not currently the central Knoxx gap. |
| `knowledge-ops-the-lake.md` | `legacy-donor` | Still important conceptually, but too broad/mixed-era to read as the active Knoxx implementation contract. |
| `knowledge-ops-demo-seed.md` | `backlog` | A seed/demo story is useful later, but Knoxx is currently being proven through the live devel corpus instead. |

### Control plane, governance, and review

| Spec | Status | Current reading |
|------|--------|-----------------|
| `knowledge-ops-multi-tenant-control-plane.md` | `next` | This is the most important missing bridge between current Knoxx reality and the intended product model. Control-plane records exist; request-time enforcement does not. |
| `knowledge-ops-mvp-phase1-epics.md` | `next` | The “Tenant Foundation” epic is only partially satisfied. The missing pieces line up exactly with the current Knoxx gap: context resolution, fail-closed enforcement, and isolation tests. |
| `knowledge-ops-pii-handling-protocol.md` | `backlog` | Strong requirement, but it depends on getting tenant/org/request scope enforcement and retrieval/tool gating right first. |
| `knowledge-ops-shibboleth-lite-labeling.md` | `backlog` | Some handoff surfaces exist, but the review workflow is not yet central in current Knoxx product reality. |
| `knowledge-ops-exposure-monitor.md` | `exploratory` | Likely useful as a garden or adjacent product, but not part of the immediate Knoxx critical path. |

### Deployment, integration, providers, and platform framing

| Spec | Status | Current reading |
|------|--------|-----------------|
| `knowledge-ops-deployment.md` | `legacy-donor` | The live stack is `services/knoxx`, but this document still mostly describes older futuresight/ragussy deployment assumptions. |
| `knowledge-ops-deploy-local.md` | `legacy-donor` | Some local-dev ideas map onto the live stack, but the document is not the canonical current setup. |
| `knowledge-ops-deploy-self-hosted.md` | `exploratory` | Future packaging direction, not the immediate Knoxx task. |
| `knowledge-ops-deploy-aws.md` | `exploratory` | Cloud deployment option, not current local-product bottleneck. |
| `knowledge-ops-deploy-azure.md` | `exploratory` | Cloud deployment option, not current local-product bottleneck. |
| `knowledge-ops-integration.md` | `legacy-donor` | Mostly superseded by the CLJS backend cutover and direct Knoxx-owned routes. |
| `knowledge-ops-platform-stack-architecture.md` | `legacy-donor` | Useful strategic framing, but too mixed-era to treat as the active implementation contract. |
| `knowledge-ops-provider-abstraction.md` | `exploratory` | Valuable later once current control-plane and enforcement work settles. |
| `knowledge-ops-multi-provider-epic.md` | `exploratory` | Same: future-facing, not the shortest path now. |
| `knowledge-ops-mongodb-vector-unification.md` | `exploratory` | Design option only, not current Knoxx reality. |
| `knowledge-ops-product-line.md` | `exploratory` | Useful product strategy framing, not immediate implementation guidance. |
| `knowledge-ops-promethean-stack.md` | `legacy-donor` | Important corpus/lore/reference material, but not current Knoxx execution truth. |
| `knowledge-ops-gap-analysis-prior-art.md` | `legacy-donor` | Research/reference document, not a current implementation target. |

## What is actually landed versus still aspirational

### Landed enough to trust as current reality

- CLJS backend rewrite
- Knoxx root workbench shape
- OpenPlanner-backed memory and recent sessions
- direct document and data-lake route ownership in the backend
- async runs, receipts, websocket streaming, and live controls
- Postgres control-plane bootstrap and admin CRUD

### Partial but clearly on the main line

- role-scoped lakes
- federated lakes
- OpenPlanner-backed CMS direction
- design-system convergence around UXX
- gardens as a product concept

### Still ahead of the app

- request-time tenant resolution and fail-closed authorization
- policy-backed tool execution gating
- full CMS publish/review/public corpus lifecycle
- PII classification and compliance pipeline
- cross-cloud/provider abstraction work

## Recommended next logical step from the specs

## Recommendation

The next implementation slice should be:

**request-scoped identity, tenant/org resolution, and policy enforcement across Knoxx runtime routes**

In practical terms:

1. resolve `user/org/membership/tool-policy` context on every request
2. fail closed when no valid context exists
3. enforce that context on:
   - `/api/admin/*`
   - `/api/memory/*`
   - `/api/runs/*`
   - `/api/tools/*`
   - `/api/documents/*`
   - `/api/settings/databases*`
4. replace hardcoded role/tool allowances with policy-DB-backed checks
5. add negative e2e tests proving cross-org denial and tool denial

## Why this is the next step

Because it is the shortest path that satisfies the most specs at once:

- `knowledge-ops-multi-tenant-control-plane.md` says every request resolves a tenant and fails closed if it cannot.
- `knowledge-ops-mvp-phase1-epics.md` puts **Tenant Foundation** first: tenant context resolution, RBAC bootstrap, and isolation tests.
- `knowledge-ops-role-scoped-lakes.md` only becomes real when role/lake visibility is a runtime boundary rather than a conceptual preset.
- `knowledge-ops-pii-handling-protocol.md` cannot be meaningfully implemented until reads, prompts, logs, and exports are scoped and auditable.
- `knowledge-ops-workbench-ui.md` wants the Context Bar to show what the user can actually work with, not a process-global view.

## Why this is a better next step than the alternatives

### Not CMS first
The CMS/publication workflow matters, but without tenant/policy enforcement it would still sit on a weak boundary.

### Not PII first
PII handling without request/org scope and authorization would mostly produce metadata without enforceable protection.

### Not multi-provider/cloud portability first
Those are real future concerns, but they do not remove the current product risk.

## Suggested implementation slice

### Backend

- add request-context resolution helpers in `orgs/open-hax/knoxx/backend/src/cljs/knoxx/backend/core.cljs`
- extend `orgs/open-hax/knoxx/backend/src/policy-db.mjs` with route-usable membership/policy lookup helpers
- replace hardcoded runtime tool authorization with policy DB evaluation
- scope memory, runs, documents, and data lakes by org/membership

### Tests

Extend `orgs/open-hax/knoxx/backend/tests/admin-rbac.e2e.test.mjs` with denial-path coverage for:

- cross-org listing denial
- membership without admin permission denied on admin routes
- membership tool-policy deny blocking `bash`
- `knowledge_worker` cannot read org-wide runs/memory without explicit grant

### Frontend follow-through

Once enforcement exists, surface the resolved org/role/lake scope in the Context Bar and settings UI so the visible workbench matches the actual policy envelope.

## Exit criteria for that slice

Treat the slice as complete when:

- all protected runtime routes fail closed without resolved request context
- tool catalog and tool execution are policy-backed, not hardcoded
- org scoping affects returned runs, sessions, documents, and lakes
- negative e2e tests cover denial paths, not only happy-path CRUD
- Knoxx can honestly claim that its current memory/workbench/product surfaces are tenant-aware rather than merely tenant-shaped

## After that

Once enforcement is real, the next best follow-on slice is:

- role/lake-aware Context Bar presets and filters in the UI
- then PII classification/log scrubbing/export filtering
- then fuller CMS publish/review/public-boundary workflow
