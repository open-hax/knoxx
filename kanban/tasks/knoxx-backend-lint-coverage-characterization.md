---
uuid: "knoxx-knoxx-backend-lint-coverage-characterization"
title: "Knoxx Backend Lint — Coverage-First Characterization"
status: todo
priority: P1
labels: ["tasks", "5sp"]
created_at: "2026-05-27T00:00:00Z"
source: "specs/tasks/knoxx-backend-lint-coverage-characterization.md"
points: 5
category: tasks
---

# Knoxx Backend Lint — Coverage-First Characterization

> Source: `specs/tasks/knoxx-backend-lint-coverage-characterization.md`
> Points: 5

Date: 2026-05-27
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Before refactoring lint hotspots, add characterization tests around low-coverage units so lint cleanup improves design without erasing unknown behavior.

## Problem

`pnpm -C backend lint` is blocked by 52 errors and 1763 warnings, while the latest coverage run reports only 57.35% line coverage, 42.10% branch coverage, and 31.90% function coverage. Many of the warning-heavy files are also low-coverage route, policy, Discord, Bluesky, session, and persistence surfaces.

A lint-only campaign could make the code prettier while moving untested behavior. The work needs a coverage gate that answers: what behavior are we protecting before extraction?

