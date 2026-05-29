---
uuid: "knoxx-knowledge-ops-adaptive-expand-policy-seam"
title: "Knowledge Ops — Adaptive Expand Policy Seam"
status: incoming
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-04-05T00:00:00Z"
source: "specs/tasks/knowledge-ops-adaptive-expand-policy-seam.md"
points: 2
category: tasks
---

# Knowledge Ops — Adaptive Expand Policy Seam

> Source: `specs/tasks/knowledge-ops-adaptive-expand-policy-seam.md`
> Parent: `knowledge-ops-adaptive-expand-policy-hook.md`
> Points: 2

Date: 2026-04-05
Status: later
Parent: `knowledge-ops-adaptive-expand-policy-hook.md`
Story points: 2

## Purpose

Introduce the smallest internal seam needed to swap graph expansion policy without changing the agent-facing graph query contract.

## Problem

Future adaptive traversal needs a place to plug in, but today that policy seam is not explicit. If adaptive behavior lands directly in query handlers, later experimentation will create contract churn and hidden coupling.

## Goals

