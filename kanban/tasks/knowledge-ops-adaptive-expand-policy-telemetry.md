---
uuid: "knoxx-knowledge-ops-adaptive-expand-policy-telemetry"
title: "Knowledge Ops — Adaptive Expand Policy Telemetry"
status: ready
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/tasks/knowledge-ops-adaptive-expand-policy-telemetry.md"
points: 2
category: tasks
---

# Knowledge Ops — Adaptive Expand Policy Telemetry

> Source: `specs/tasks/knowledge-ops-adaptive-expand-policy-telemetry.md`
> Parent: `knowledge-ops-adaptive-expand-policy-hook.md`
> Points: 2

Date: 2026-04-05
Status: ready
Parent: `knowledge-ops-adaptive-expand-policy-hook.md`
Story points: 2

## Purpose

Add structured telemetry for bounded graph expansion so future adaptive policies can be compared against the baseline using evidence rather than intuition.

## Problem

Even with a policy seam, future adaptive traversal will be guesswork unless the system records which policy ran, what bounds were applied, and what result shape came back.

## Goals

1. Emit structured telemetry for expansion requests and outcomes.
2. Record enough context to compare default and future policies.
3. Keep telemetry out of the public agent-facing contract.

## Non-Goals

1. Real-time policy optimization.
2. Building a full observability dashboard.
3. Exposing internal scoring details directly to agents.

## Telemetry minimums

At minimum, record:

- operation type
- active policy name
- applied bounds / limits
- result counts or summary shape
- duration / failure class

## Affected files / surfaces

- `orgs/open-hax/knoxx/backend/src/cljs/knoxx/backend/core.cljs`
- any structured log / metric / receipt surface used by Knoxx graph operations
- adjacent docs/specs describing graph query behavior

## Verification

1. Expansion operations emit structured telemetry under the default policy.
2. Telemetry can distinguish policy choice, bounds, and outcome shape.
3. Agent-facing tool semantics remain unchanged.

## Definition of done

- Future adaptive traversal work has an evidence surface for judging policy quality without redesigning the public graph contract.

## Breakdown

Implementation is a single-file change in `knoxx/backend/core.cljs`: wrap the graph expansion call(s) with a structured log/receipt emit capturing operation type, active policy name, applied bounds, result shape, and duration or failure class. No new routes, no public contract changes, no new dependencies required. Verification is a manual smoke check confirming telemetry fields appear under the default policy without altering agent-facing tool semantics.

---

**Triage 2026-05-29 (incoming → accepted):** P2. Parent `knowledge-ops-adaptive-expand-policy-seam` is Done — seam exists. 2sp score confirmed: scoped to adding structured telemetry at the graph-op call site, no new routes or public contract changes. Accepted; deprioritised below P1 graph recovery tasks.

---

**Triage 2026-05-29 (accepted → ready):** All Ready gate criteria met. Score is 2sp. Blocking dependency `knowledge-ops-adaptive-expand-policy-seam` is Done — the policy seam call-site exists. DoD and telemetry minimums are unambiguously specified. Scope is bounded to `backend/core.cljs` and adjacent log surfaces with no public contract changes. Promoted to Ready.
