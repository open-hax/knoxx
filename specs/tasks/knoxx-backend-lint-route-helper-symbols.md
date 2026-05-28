# Knoxx Backend Lint — Route Helper and Macro Symbol Repair

Date: 2026-05-27
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Stop route namespaces from generating unresolved-symbol cascades around `defroute`, request context bindings, guards, helper functions, and route registration forms.

## Problem

The latest lint run no longer shows the earlier broad route unresolved-symbol cascade, but route namespaces still carry route-specific hard errors, function-length errors, and many Promise-chain warnings. The `defroute` hook and route helper model still need explicit verification before route refactors, especially because route bodies may use `await` and must be analyzed under an actual `^:async` function context.

Current route risks suggest one or more of:

1. the `defroute` macro/hook may still not model current async call-site shape;
2. some route helpers moved without updating requires;
3. route-local lexical bindings may escape their intended scope;
4. large route-registration functions hide real binding drift behind length and Promise-chain noise.

## Goals

1. Identify whether the route errors are macro-hook false positives, real macro expansion bugs, missing requires, or lexical-scope leaks.
2. Repair the route helper import/binding model without broad lint suppression.
3. Make affected route namespaces clj-kondo clean for unresolved route-context symbols.
4. Keep route handlers small enough to support the function-length task.
5. Add or update tests only if route behavior changes.

## Non-goals

1. Rewriting all route modules into a new framework.
2. Changing HTTP response shapes except where current code is provably broken.
3. Solving all promise-chain warnings in route files.
4. Restarting the Knoxx backend process.

## Affected namespaces

Route namespaces to keep in scope while validating the hook and helper model:

- `src/cljs/knoxx/backend/infra/routes/actors.cljs`
- `src/cljs/knoxx/backend/infra/routes/documents.cljs`
- `src/cljs/knoxx/backend/infra/routes/mcp.cljs`
- `src/cljs/knoxx/backend/infra/routes/memory.cljs`
- `src/cljs/knoxx/backend/infra/routes/studio.cljs`
- `src/cljs/knoxx/backend/infra/routes/studio/discord_scan.cljs`
- `src/cljs/knoxx/backend/infra/routes/tools.cljs`
- `src/cljs/knoxx/backend/infra/routes/translation.cljs`
- `src/cljs/knoxx/backend/infra/routes/app.cljs`

## Investigation checklist

1. Inspect `backend/.clj-kondo/config.edn` and the hook for `knoxx.backend.macros/defroute`.
2. Inspect `backend/src/cljs/knoxx/backend/macros.cljc` and representative route call sites.
3. Confirm whether clj-kondo sees route handler bindings after macro expansion.
4. Fix macro hook drift if the runtime code is valid but lint cannot see it.
5. Fix real missing requires or leaked lexical names at route call sites.
6. Rerun clj-kondo on one representative namespace before sweeping the full route set.

## Async `defroute` handler shape

User investigation against the upstream ClojureScript async/await test suite found the canonical anonymous async function shape:

```clojure
(^:async fn [ctx]
  (await some-promise))
```

This matters for `knoxx.backend.macros/defroute` because `(await ...)` inside route bodies must expand inside an actual `^:async fn` context, not merely inside a surrounding route-registration `defn` or a lint-only wrapper. The current runtime macro shape should therefore keep the body under `(^:async fn [ctx] ...)` for classic mode, and any pre-handler-mode route body that uses `await` must also be emitted under an async handler/context.

The clj-kondo hook likely needs to model this more faithfully. Current hook risks to inspect/fix:

1. It models the route body as a plain `let`, so lint hooks may not see the same async context as the compiler.
2. It drops a fixed five children and may accidentally include the pre-handler vector as body in pre-handler mode instead of skipping it.
3. It should inject all macro-provided symbols that are always destructured by the macro, including `session-guard` and `optional-session-guard`.
4. It should preserve lexical bindings for `request`, `reply`, `ctx`, `runtime`, `config`, and all `extra-deps` while avoiding broad unresolved-symbol ignores.

A representative lint-hook model should expand route bodies into a small `defn` containing a handler-like form with the body inside `(^:async fn [ctx] ...)` or an equivalent analyzer shape that tells clj-kondo `await` is legal in route bodies.

## Verification

Targeted checks while iterating:

```bash
pnpm -C backend exec clj-kondo --lint src/cljs/knoxx/backend/macros.cljc src/cljs/knoxx/backend/infra/routes/tools.cljs
pnpm -C backend exec clj-kondo --lint src/cljs/knoxx/backend/infra/routes/actors.cljs src/cljs/knoxx/backend/infra/routes/documents.cljs src/cljs/knoxx/backend/infra/routes/mcp.cljs src/cljs/knoxx/backend/infra/routes/memory.cljs src/cljs/knoxx/backend/infra/routes/studio.cljs src/cljs/knoxx/backend/infra/routes/studio/discord_scan.cljs
```

Final check for this task:

```bash
pnpm -C backend lint
```

## Definition of done

- Route-context unresolved-symbol cascades are gone.
- Any macro hook change is covered by at least one representative route form.
- Real missing route helpers are imported or moved to an appropriate boundary namespace.
- Remaining route diagnostics are either function-length or style cleanup owned by later tasks.
