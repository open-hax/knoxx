---
uuid: "knoxx-knoxx-backend-lint-hard-error-first-pass"
title: "Knoxx Backend Lint — Hard Error First Pass"
status: done
priority: P1
labels: ["tasks", "5sp"]
created_at: "2026-05-27T00:00:00Z"
source: "specs/tasks/knoxx-backend-lint-hard-error-first-pass.md"
points: 5
category: tasks
---

# Knoxx Backend Lint — Hard Error First Pass

> Source: `specs/tasks/knoxx-backend-lint-hard-error-first-pass.md`
> Points: 5

Date: 2026-05-27
Status: done
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Reduce the lint failure from a noisy backlog to a trustworthy queue by fixing diagnostics that can hide real compile failures or make later refactors unsafe.

## Problem

`pnpm -C backend lint` initially reported 52 errors. Most were function-length errors, but the highest leverage first pass was the non-style error set: unresolved symbols, analyzer/type mismatches, and tests reaching into private vars.

After this pass, lint reports 33 errors, all currently function-length errors owned by `knoxx-backend-lint-function-length-extractions.md`.

---

**Triage 2026-05-28 (status reconciliation → done):** `pnpm -C backend lint` now reports **0 errors** (down from 52). All non-style hard errors — unresolved symbols, analyzer/type mismatches, private-var test access — are resolved, and the function-length errors that were handed off are also at zero. This pass's scope is fully complete; moving review → done. Remaining work is warning-level only and tracked by the other lint cards.

