# Knoxx Backend Lint — Async Workflow Refactor for Source Namespaces

Date: 2026-05-27
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Replace raw `.then` and `.catch` Promise chains in backend source namespaces with named async workflows that match Knoxx's current CLJS style: `^:async` functions, `await`, plain context maps, and small stage functions.

## Problem

The current lint run reports 1502 Promise-chain warnings. The old lint message and older async notes pointed developers toward `promesa.core/p/let`; that guidance is now too narrow and can pull new code away from the house style documented in AGENTS and route macro notes.

The target is not syntactic churn. Promise-chain cleanup should make workflows easier to test and should expose domain/shape/law/infra boundaries that are currently hidden inside callback bodies.

## Goals

1. Convert raw Promise chains in `backend/src/cljs/**` to named `^:async` workflow functions with `await`.
2. Prefer context-map stages for route and request workflows.
3. Preserve return shapes, thenable compatibility, and rejection behavior.
4. Keep JS interop within `extern.*` adapters or documented low-level wrappers.
5. Add characterization tests before changing error handling, ordering, retry, fallback, or side-effect semantics.
6. Leave existing Promesa code alone unless it is in the touched slice; do not introduce Promesa only to satisfy the linter.

## Non-goals

1. Migrating test namespaces; that is covered by the test-boundaries task.
2. Rewriting business behavior beyond async style and extraction boundaries.
3. Changing external API response bodies.
4. Touching generated files or vendor code.
5. Blanket replacing Promesa in working namespaces without a coverage-backed reason.

## Priority slices

Use lint count and coverage risk together. Start where promise-chain warning density overlaps low test coverage or active route risk:

1. Low-coverage route and policy hotspots:
   - `infra/db/policy.cljs`
   - `infra/routes/app.cljs`
   - `infra/routes/memory.cljs`
   - `infra/routes/studio.cljs`
   - `infra/routes/tools.cljs`
   - `infra/routes/studio/discord_scan.cljs`
2. External integration domains with low branch/function coverage:
   - `domain/bluesky/*.cljs`
   - `domain/discord/*.cljs`
   - `domain/actor/tools.cljs`
   - `domain/session_mycology.cljs`
3. Runtime and agent orchestration:
   - `bootstrap.cljs`
   - `infra/core.cljs`
   - `infra/agent/*.cljs`
   - `domain/action/*.cljs`
4. Stores and persistence:
   - `infra/stores/*.cljs`
   - `infra/db/*.cljs`
5. OpenPlanner and workspace surfaces:
   - `infra/openplanner/*.cljs`
   - `infra/routes/workspace_media.cljs`

## Migration guidance

Preferred shape for new workflow functions:

```clojure
(defn ^:async load-and-render! [ctx]
  (let [record (await (load-record! ctx))
        result (render-domain-result record)]
    (assoc ctx :result result)))
```

Preferred shape for route/request flows:

```clojure
(defn ^:async handle-route! [ctx]
  (let [ctx (validate-request ctx)
        ctx (await (load-session! ctx))
        ctx (await (perform-domain-step! ctx))]
    (await (send-response! ctx))))
```

Rules:

- Extract named stages before converting syntax when a callback body does more than one conceptual action.
- Use `try`/`catch` around `await` when the existing `.catch` changes the returned value rather than only logging.
- Preserve intentional fail-open behavior explicitly with tests.
- Keep tiny `.then(success failure)` only at documented interop boundaries where it is clearer than forcing a workflow function.
- Avoid nested anonymous async functions inside route bodies; name the stage instead.

## Risk notes

- Promise chains often encode error fallback behavior; preserve whether errors are swallowed, transformed, logged, retried, or rethrown.
- Some route handlers rely on returning a native thenable to Fastify; verify the compiled/runtime return shape.
- Async cleanup can change concurrency if sequential `await` replaces intentional `Promise.all`; keep parallel operations parallel.
- Large route handlers should be split along guard/parse/work/respond stages rather than converted in-place.

## Verification

Scoped during migration:

```bash
pnpm -C backend exec clj-kondo --lint <touched-source-files>
pnpm -C backend exec shadow-cljs compile test
```

Production-path gate when route/server startup code is changed:

```bash
pnpm -C backend exec shadow-cljs compile server
```

Targeted behavior checks:

```bash
pnpm -C backend run test:coverage
cd backend && pnpm run mutation:smoke
```

## Definition of done

- No raw Promise-chain warnings remain in `backend/src/cljs/**`, or remaining instances are documented as approved interop boundaries.
- Lint messages steer future contributors toward `^:async` / `await`, not Promesa-by-default.
- Error handling and concurrency behavior are preserved by tests.
- Relevant compile/test/coverage gates pass or blockers are recorded exactly.
