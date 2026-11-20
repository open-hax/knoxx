---
uuid: "knoxx-353-persistence-readiness"
title: "Order persistence before startup readiness"
status: "incoming"
type: "task"
priority: "P1"
labels: "backend, persistence, startup"
parent: "knoxx-knowledge-ops-architecture-migration"
dependency: []
category: "tasks"
write-id: "1793663433530-0.wqq5xiw9dqvd6ut1c4"
created_at: "2026-11-02T23:50:33.531Z"
---

# Order selected persistence before readiness

## Outcome

Knoxx must not bind HTTP or report readiness while selected persistence is unavailable.

## Scope

Destination: backend startup composition and its ClojureScript tests. Parent epic: knowledge-ops architecture migration.

## Acceptance criteria

- The configured persistence installation completes before listen and readiness.
- A failed installation propagates and prevents the listener and readiness signal.
- Failed startup does not leave newly started recovery or flush tasks running.
- Hot-reload recomposition and live recovery are verified or explicitly reported as separate gaps.

## Verification

Execute the focused startup tests and relevant backend suite with real dependencies; report native service gaps accurately.
