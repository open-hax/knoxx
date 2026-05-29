---
uuid: "knoxx-knoxx-backend-lint-route-helper-symbols"
title: "Knoxx Backend Lint — Route Helper and Macro Symbol Repair"
status: done
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
Status: done
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Stop route namespaces from generating unresolved-symbol cascades around `defroute`, request context bindings, guards, helper functions, and route registration forms.

## Problem

The latest lint run no longer shows the earlier broad route unresolved-symbol cascade, but route namespaces still carry route-specific hard errors, function-length errors, and many Promise-chain warnings. The `defroute` hook and route helper model still need explicit verification before route refactors, especially because route bodies may use `await` and must be analyzed under an actual `^:async` function context.

Current route risks suggest one or more of:

---

**2026-05-29 — Done (defroute clj-kondo hook now models the real `^:async` handler context).**

Changes in `backend/.clj-kondo/hooks/defroute.clj`:

- `deps-syms` already carried `session-guard` and `optional-session-guard`; confirmed both guard helpers resolve in route bodies.
- Removed `ctx` from `handler-syms` and now model it as the parameter of an emitted `^:async fn`. The runtime `defroute` macro (`backend/src/cljs/knoxx/backend/macros.cljc`) binds `ctx` from `(aget request "ctx")` inside its handler, and emits `^:async fn` when the body contains `await` — so the hook now wraps `effective-body` in a new `async-handler-node` that produces `(^:async fn [ctx] body...)` (or `[_ctx]` when `ctx` is unused, to avoid an unused-binding warning). This makes `await` and raw Promise chains in route bodies analyzed inside an async-fn boundary rather than at let-body scope.
- `request`, `reply`, `await` stay injected via the variadic-fn let pool. Pre-handler guard vectors are still spliced into the wrapped body so guard symbols stay referenced.

Verification (from `backend/`):

- `pnpm lint` → `errors: 0, warnings: 1463` — identical to pre-change baseline; no new warnings, no unresolved-symbol regressions. The two `.then`/`.catch` warnings in `defroute_async_test.cljs` are pre-existing (the test intentionally keeps a raw Promise chain).
- `pnpm test` → `Ran 449 tests containing 1319 assertions. 0 failures, 0 errors.` (`defroute_async_test.cljs` included and green).
- `pnpm typecheck` (shadow-cljs compile server) → `0 warnings`.

