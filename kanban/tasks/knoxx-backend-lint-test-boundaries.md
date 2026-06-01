---
uuid: "knoxx-knoxx-backend-lint-test-boundaries"
title: "Knoxx Backend Lint — Test Boundaries and Async Cleanup"
status: todo
priority: P1
labels: ["tasks", "5sp"]
created_at: "2026-05-27T00:00:00Z"
source: "specs/tasks/knoxx-backend-lint-test-boundaries.md"
points: 5
category: tasks
---

# Knoxx Backend Lint — Test Boundaries and Async Cleanup

> Source: `specs/tasks/knoxx-backend-lint-test-boundaries.md`
> Points: 5

Date: 2026-05-27
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Make backend tests respect public API boundaries, modern async style, required protocol methods, and namespace requirements while preserving regression coverage.

## Problem

The lint run reports test-specific blockers and warnings:

- private-var access in `pipeline_runner_test.cljs`, `policy_actor_test.cljs`, and `tools/temp_memory_test.cljs`;
- raw `.then`/`.catch` Promise chains across many test files;
