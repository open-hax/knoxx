---
uuid: "knoxx-knowledge-ops-mvp-phase1-epics"
title: "Knowledge Ops — MVP Phase 1 Epic Spec"
status: "icebox"
priority: "P2"
labels: "["epics"]"
created_at: "2026-05-28T22:40:14.390Z"
source: "specs/epics/knowledge-ops-mvp-phase1-epics.md"
points: null
category: "epics"
---

# Knowledge Ops — MVP Phase 1 Epic Spec

> Source: `specs/epics/knowledge-ops-mvp-phase1-epics.md`

> *Tenant onboarding → secure ingestion → tenant-scoped retrieval → expert review → labeled export → audit trail.*

---
## Purpose

Define the MVP scope for the domain-aware knowledge ops platform as a set of epics, user stories, and acceptance criteria. No timelines — sized by relative complexity and level of effort using Fibonacci story points.
---

## Product Statement

A multi-tenant system that ingests client knowledge, detects and controls PII, translates content, routes uncertain outputs to expert review, and produces training-grade feedback data for model improvement.

## Triage Notes (2026-05-28)

- **Status**: Keep as breakdown. 271-line MVP scope definition.
- **Action needed**: Cross-reference with current implementation state, identify which epics are landed vs still needed.
- **Assessment**: This is a roadmap document. Many of its epics may already be partially implemented. Before splitting into child tasks, audit against `knowledge-ops-roadmap-status.md` to identify what's actually remaining.

---
Breakdown 2026-05-29: Full source spec at `kanban/epics/knowledge-ops-mvp-phase1-epics.md` is a 271-line, 5-epic roadmap totalling 121 story points (Epic 1 Tenant Foundation 21sp, Epic 2 Secure Ingestion 31sp, Epic 3 PII Controls 26sp, Epic 4 Retrieval v1 24sp, Epic 5 Audit Trail 19sp). Cross-reference against `knowledge-ops-roadmap-status.md` confirms only Epic 1 ("Tenant Foundation") is the `next` implementation priority — everything else is backlog or exploratory.

Code inspection: `infra/auth/authz.cljs` has `resolve-request-context!` and `with-request-context!` functions, but 85 route handlers across `routes/documents.cljs`, `routes/memory.cljs`, `routes/tools.cljs`, etc. use `(when ctx ...)` which is fail-open — unauthenticated requests proceed when ctx is nil. Story 1.2 (context resolution / fail-closed) is the critical missing bridge. Story 1.3 (RBAC bootstrap) is partially done — `shape/db/roles.cljs`, `shape/db/orgs.cljs` exist but no membership/role enforcement middleware. Story 1.5 (isolation test suite) — `test/cljs/knoxx/backend/authz_test.cljs` covers ctx accessor unit tests only; no cross-org denial e2e tests exist.

Story points: 13sp (epic-level). Verdict: epic. Splitting into 4 bounded tasks covering the `next` slice identified by the roadmap: fail-closed route guard, org-scope enforcement on runs/memory, policy-backed tool authorization, and cross-org denial e2e tests. --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
