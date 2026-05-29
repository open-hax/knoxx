---
uuid: "knoxx-knowledge-ops-adaptive-expand-policy-telemetry"
title: "Knowledge Ops — Adaptive Expand Policy Telemetry"
status: incoming
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
Status: later
Parent: `knowledge-ops-adaptive-expand-policy-hook.md`
Story points: 2

## Purpose

Add structured telemetry for bounded graph expansion so future adaptive policies can be compared against the baseline using evidence rather than intuition.

## Problem

Even with a policy seam, future adaptive traversal will be guesswork unless the system records which policy ran, what bounds were applied, and what result shape came back.

## Goals

