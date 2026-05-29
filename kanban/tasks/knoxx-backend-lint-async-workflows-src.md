---
uuid: "knoxx-knoxx-backend-lint-async-workflows-src"
title: "Knoxx Backend Lint — Async Workflow Refactor for Source Namespaces"
status: todo
priority: P1
labels: ["tasks", "5sp"]
created_at: "2026-05-27T00:00:00Z"
source: "specs/tasks/knoxx-backend-lint-async-workflows-src.md"
points: 5
category: tasks
---

# Knoxx Backend Lint — Async Workflow Refactor for Source Namespaces

> Source: `specs/tasks/knoxx-backend-lint-async-workflows-src.md`
> Points: 5

Date: 2026-05-27
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Replace raw `.then` and `.catch` Promise chains in backend source namespaces with named async workflows that match Knoxx's current CLJS style: `^:async` functions, `await`, plain context maps, and small stage functions.

## Problem

The current lint run reports 1502 Promise-chain warnings. The old lint message and older async notes pointed developers toward `promesa.core/p/let`; that guidance is now too narrow and can pull new code away from the house style documented in AGENTS and route macro notes.

The target is not syntactic churn. Promise-chain cleanup should make workflows easier to test and should expose domain/shape/law/infra boundaries that are currently hid
