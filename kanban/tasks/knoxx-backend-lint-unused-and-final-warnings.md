---
uuid: "knoxx-knoxx-backend-lint-unused-and-final-warnings"
title: "Knoxx Backend Lint — Final Warning Cleanup"
status: todo
priority: P1
labels: ["tasks", "5sp"]
created_at: "2026-05-27T00:00:00Z"
source: "specs/tasks/knoxx-backend-lint-unused-and-final-warnings.md"
points: 5
category: tasks
---

# Knoxx Backend Lint — Final Warning Cleanup

> Source: `specs/tasks/knoxx-backend-lint-unused-and-final-warnings.md`
> Points: 5

Date: 2026-05-27
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Eliminate the remaining lint warnings after hard errors, coverage-backed refactors, route hook checks, function-length extraction, async workflow cleanup, and test-boundary work have reduced the noisy categories.

## Problem

The current lint output includes smaller warning classes that should run late because many will disappear as earlier refactors remove or move code:

- 38 private-var access warnings;
- 7 redundant `let` warnings;
