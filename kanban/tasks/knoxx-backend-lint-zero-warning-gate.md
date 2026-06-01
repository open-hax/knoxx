---
uuid: "knoxx-knoxx-backend-lint-zero-warning-gate"
title: "Knoxx Backend Lint — Zero Warning Gate"
status: todo
priority: P1
labels: ["tasks", "3sp"]
created_at: "2026-05-27T00:00:00Z"
source: "specs/tasks/knoxx-backend-lint-zero-warning-gate.md"
points: 3
category: tasks
---

# Knoxx Backend Lint — Zero Warning Gate

> Source: `specs/tasks/knoxx-backend-lint-zero-warning-gate.md`
> Points: 3

Date: 2026-05-27
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 3

## Purpose

Close the lint-remediation epic with a clean, reproducible verification record that proves the backend satisfies Knoxx's zero-warning policy.

## Problem

A lint cleanup is not complete when the most visible errors disappear. Knoxx's house rules require zero warnings in CI, so the final task must rerun the canonical gates from a clean enough working state and record exact evidence.

## Goals

