---
uuid: "knoxx-knoxx-backend-cljs-lint-remediation"
title: "Knoxx Backend CLJS Lint, Coverage, and Architecture Remediation Epic"
status: in_progress
priority: P0
labels: ["epics", "40sp"]
created_at: "2026-05-27T00:00:00Z"
source: "specs/epics/knoxx-backend-cljs-lint-remediation.md"
points: 40
category: epics
---
# Knoxx Backend CLJS Lint, Coverage, and Architecture Remediation Epic

> Source: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
> Points: 40

Date: 2026-05-27
Status: next
Source command: `pnpm -C backend lint`
Source log: `/tmp/knoxx-backend-lint-after-agent-template-extraction.log`
Test command: `pnpm -C backend test`
Story points: 40

## Purpose

Restore Knoxx backend lint to the zero-warning contract while improving test coverage, async architecture, and domain/shape/law boundaries. This is not a suppression campaign; it is a quality campaign that turns lint pressure into safer, better-factored backend code.

## Problem

The current backend command chain `pnpm -C backend lint && pnpm -C backend test` short-circuits at lint. Running `pnpm -C backend test` separately still passes, so the immediate blocker is lint quality, not the Shadow test runner.

