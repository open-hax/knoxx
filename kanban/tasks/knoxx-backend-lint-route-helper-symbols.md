---
uuid: "knoxx-knoxx-backend-lint-route-helper-symbols"
title: "Knoxx Backend Lint — Route Helper and Macro Symbol Repair"
status: todo
priority: P1
labels: ["tasks", "5sp"]
created_at: "2026-05-27T00:00:00Z"
source: "specs/tasks/knoxx-backend-lint-route-helper-symbols.md"
points: 5
category: tasks
---

# Knoxx Backend Lint — Route Helper and Macro Symbol Repair

> Source: `specs/tasks/knoxx-backend-lint-route-helper-symbols.md`
> Points: 5

Date: 2026-05-27
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Stop route namespaces from generating unresolved-symbol cascades around `defroute`, request context bindings, guards, helper functions, and route registration forms.

## Problem

The latest lint run no longer shows the earlier broad route unresolved-symbol cascade, but route namespaces still carry route-specific hard errors, function-length errors, and many Promise-chain warnings. The `defroute` hook and route helper model still need explicit verification before route refactors, especially because route bodies may use `await` and must be analyzed under an actual `^:async` function context.

Current route risks suggest one or more of:

