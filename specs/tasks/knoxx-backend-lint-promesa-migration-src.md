# Knoxx Backend Lint — Promesa Migration for Source Namespaces

Date: 2026-05-21
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Replace raw `.then` and `.catch` Promise chains in backend source namespaces with the project's preferred async style.

## Problem

The lint run reports roughly 1482 Promise-chain warnings. Source files should use `promesa.core/p/let` or `^:async` + `await` rather than hand-written Promise chains, especially in route handlers, external clients, store implementations, and event pipelines.

## Goals

1. Convert raw Promise chains in `backend/src/cljs/**` to `p/let` or `^:async` + `await`.
2. Preserve return shapes and rejection behavior.
3. Keep JS interop within `extern.*` adapters or documented low-level wrappers.
4. Avoid nested async blocks that obscure control flow.
5. Add focused tests when conversion changes error handling or sequencing.

## Non-goals

1. Migrating test namespaces; that is covered by the test-boundaries task.
2. Refactoring business behavior beyond async style.
3. Changing external API response bodies.
4. Touching generated files or vendor code.

## Priority slices

1. Route and app hotspots:
   - `infra/routes/app.cljs`
   - `infra/routes/tools.cljs`
   - `infra/routes/mcp.cljs`
   - `infra/routes/documents.cljs`
   - `infra/routes/memory.cljs`
   - `infra/routes/studio.cljs`
2. Runtime and agent orchestration:
   - `bootstrap.cljs`
   - `infra/core.cljs`
   - `infra/agent/*.cljs`
   - `domain/action/*.cljs`
3. Stores and persistence:
   - `infra/stores/*.cljs`
   - `infra/db/*.cljs`
4. External integration domains:
   - `domain/discord/*.cljs`
   - `domain/bluesky/*.cljs`
   - `domain/label/*.cljs`
   - `infra/openplanner/*.cljs`

## Migration guidance

Preferred pattern:

```clojure
(p/let [value (some-async-call args)
        result (next-async-call value)]
  (build-result result))
```

For functions that naturally target modern CLJS async/await:

```clojure
(defn ^:async handler [request]
  (let [value (await (some-async-call request))]
    (build-result value)))
```

Use `p/catch` or explicit `try`/`catch` around `await` when the existing `.catch` changes the returned value rather than only logging.

## Risk notes

- Promise chains often encode error fallback behavior; preserve whether errors are swallowed, transformed, logged, or rethrown.
- Some route handlers may rely on returning a native Promise to Fastify; confirm `p/let` returns a compatible thenable.
- Avoid moving raw JS Promise construction into non-extern namespaces unless it is already the owning boundary.

## Verification

Scoped during migration:

```bash
pnpm -C backend exec clj-kondo --lint <touched-source-files>
```

Batch gate:

```bash
pnpm -C backend lint
pnpm -C backend exec shadow-cljs compile test
```

Production-path gate when route/server startup code is changed:

```bash
pnpm -C backend exec shadow-cljs compile server
```

## Definition of done

- No raw Promise-chain warnings remain in `backend/src/cljs/**`, or remaining instances are documented as approved boundary exceptions.
- Error handling behavior is preserved.
- Relevant compile gates pass.
