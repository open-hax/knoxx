# CLJS async and request-state style

## Status

- State: partially superseded backend style note.
- Scope: `backend/src/cljs/knoxx/backend/**/*.cljs`.
- Audience: agents and humans writing Knoxx Fastify routes, agent turns, tool calls, and OpenPlanner/Proxx adapters.
- Supersession: keep the context-map workflow model from this note, but prefer native CLJS `^:async` functions with `await` for new or refactored backend async code. The older `js-await` examples below are historical migration context, not the current default.

## Decision

Use named `^:async` workflow functions and `await` as the default async syntax for new Knoxx backend request workflows.

Use explicit `.then(success failure)` only for tiny documented interop boundaries where it is clearer than an async workflow, and avoid hand-threaded `-> ... (.then ...) (.catch ...)` route bodies.

The route-level unit of composition is still a plain immutable context map. Small functions should take a context map and return either a context map or a Promise that resolves to the next context map.

## Why this exists

Knoxx has repeatedly hit CLJS Promise-chain failure modes in backend route code:

- compiled JavaScript containing malformed `.catch()` calls
- runtime errors like `"(intermediate value)(intermediate value).catch is not a function"`
- route handlers that mix request parsing, auth, session lookup, agent turn setup, persistence, broadcast, and response rendering inside nested inline callbacks

The fix is not only syntactic. `js-await` flattens local async shape, while context-map stages make the business logic composable and testable.

## External research summary

Thomas Heller introduced `shadow.cljs.modern/js-await` as a small shadow-cljs macro available since shadow-cljs `2.19.1`. It intentionally maps to ordinary Promise `.then` and `.catch` calls rather than compiler-level JavaScript `async`/`await`.

Important constraints from the macro and discussion:

- Require it with `[shadow.cljs.modern :refer [js-await]]`.
- Form shape is `(js-await [binding thenable] ...body)`.
- It always returns a Promise.
- The awaited value must be a Promise or thenable; the macro does not validate this.
- Only one binding pair is supported.
- Destructuring works in the binding.
- A `(catch err ...)` form is recognized only when it is the last body form.
- Nested `js-await` works when the nested await is returned as the last expression of the enclosing body.
- shadow-cljs changelog includes a later `fix js-await catch`, so keep the shadow-cljs version modern before leaning on the catch form.

Alternatives like Promesa, kitchen-async, `core.async`, and `cljs-async-await` exist, but Knoxx already depends on shadow-cljs and mostly needs lightweight Promise syntax, not channel coordination or a new async abstraction stack.

## Preferred shape

```clojure
(ns knoxx.backend.routes.example
  (:require
    [shadow.cljs.modern :refer [js-await]]))

(defn- request-ctx [runtime config req reply]
  {:runtime runtime
   :config config
   :request req
   :reply reply
   :params (js->clj (.-params req) :keywordize-keys true)
   :body (js->clj (.-body req) :keywordize-keys true)
   :effects []})

(defn- require-session-id [ctx]
  (if-let [session-id (get-in ctx [:body :session_id])]
    (assoc ctx :session/id session-id)
    (js/Promise.reject (js/Error. "missing session_id"))))

(defn- load-session [ctx]
  (js-await [session (get-session (:redis-client ctx) (:session/id ctx))]
    (assoc ctx :session session)))

(defn- render-response! [{:keys [reply result] :as ctx}]
  (.send reply (clj->js {:ok true :result result}))
  ctx)

(defn handle-route! [runtime config req reply]
  (let [ctx0 (request-ctx runtime config req reply)]
    (js-await [ctx1 (js/Promise.resolve (require-session-id ctx0))]
      (js-await [ctx2 (load-session ctx1)]
        (render-response! ctx2))
      (catch err
        (.code reply 500)
        (.send reply (clj->js {:error (.-message err)}))))))
```

Use this shape for readability, but prefer extracting additional named stages before nesting more than two `js-await` forms in one function.

## Anti-patterns

Do not write large threaded Promise route bodies:

```clojure
(-> (get-session redis-client session-id)
    (.then (fn [session]
             ;; parses request, mutates run state, calls model, sends reply,
             ;; and appends events inside one callback
             ...))
    (.catch (fn [err]
              ...)))
```

Do not place `(catch ...)` anywhere except as the final body form of `js-await`.

Do not pass non-thenable values directly to `js-await`; wrap synchronous stage starts with `js/Promise.resolve` when a uniform Promise boundary is needed.

Do not let helper functions send HTTP replies as a side effect unless the function name ends in `!` and the route contract makes that terminal effect explicit.

## Request-state model

Use three state layers and keep them distinct:

1. Process runtime state: long-lived atoms, clients, and caches. This belongs under `knoxx.backend.runtime.*`.
2. Request context state: an immutable map passed through stages for one HTTP request, websocket message, or agent turn.
3. Durable application state: Redis/OpenPlanner/Postgres records and event logs.

Request context maps should be ordinary Clojure data. They are not records, classes, or mutable JS objects.

Recommended top-level keys:

| Key | Meaning |
|-----|---------|
| `:runtime` | process/runtime dependency map |
| `:config` | loaded config snapshot |
| `:request` | Fastify request object |
| `:reply` | Fastify reply object |
| `:params` | keywordized params |
| `:query` | keywordized query params |
| `:body` | keywordized request body |
| `:auth` | resolved user/session/auth context |
| `:session/id` | active session id |
| `:conversation/id` | active conversation id |
| `:run/id` | active run id |
| `:agent` | resolved agent contract/runtime selection |
| `:model` | resolved model/provider selection |
| `:result` | terminal domain result before rendering |
| `:effects` | planned or emitted side-effect receipts/events |
| `:errors` | recoverable validation errors, not thrown failures |

Use namespaced keys for domain facts (`:session/id`, `:conversation/id`, `:run/id`) when collisions are plausible.

## Stage contract

A stage function should normally satisfy one of these shapes:

```clojure
;; pure enrichment
(fn [ctx] ctx')

;; async enrichment
(fn [ctx] #js Promise<ctx'>)

;; terminal side effect
(fn [ctx] #js Promise<ctx>) ; name ends with !
```

Rules:

- A stage establishes one fact or performs one effect.
- A stage receives all inputs from `ctx`; avoid closing over request-local values hidden in outer `let`s.
- A stage returns an enriched `ctx`, not a tuple of unrelated locals.
- Throw or reject for hard failures that should hit route-level `catch`.
- Store validation accumulations under `:errors` only when the route can continue and render a structured validation response.
- The terminal response function is explicit, usually `send-*!` or `render-*!`.

## Route workflow pattern

Recommended top-level route function:

```clojure
(defn run-start-route! [runtime config req reply]
  (let [ctx0 (request-ctx runtime config req reply)]
    (js-await [ctx1 (validate-start-request ctx0)]
      (js-await [ctx2 (load-or-create-session ctx1)]
        (js-await [ctx3 (start-agent-run ctx2)]
          (send-start-response! ctx3)))
      (catch err
        (send-route-error! reply err)))))
```

If this grows beyond a readable ladder, split it into named sub-workflows:

```clojure
(defn- prepare-start-context [ctx]
  (js-await [ctx1 (validate-start-request ctx)]
    (load-or-create-session ctx1)))

(defn run-start-route! [runtime config req reply]
  (let [ctx0 (request-ctx runtime config req reply)]
    (js-await [ctx1 (prepare-start-context ctx0)]
      (js-await [ctx2 (start-agent-run ctx1)]
        (send-start-response! ctx2))
      (catch err
        (send-route-error! reply err)))))
```

## Verification after async refactors

After changing CLJS async route code:

1. Run backend typecheck/compile:

   ```bash
   cd backend && pnpm typecheck
   ```

2. Inspect compiled JavaScript for malformed empty catch callbacks:

   ```bash
   rg "\.catch\(\)" backend/dist/cljs-runtime
   ```

3. Smoke the route locally or through the public endpoint if it affects live behavior.

4. If route behavior changed, append a receipt with the command output summary and the affected files.

## Migration plan

1. Stop adding new manual `-> ... (.then ...) (.catch ...)` route chains.
2. Convert high-risk route handlers first: `app_routes.cljs`, proxy routes, auth/MCP routes, and any handler with nested callback bodies.
3. Introduce small `request-ctx` constructors per route namespace before extracting shared helpers.
4. Extract stable helpers only after two or more route namespaces use the same shape.
5. Keep process runtime state under `runtime.*`; do not turn request context into another global atom.
6. For each conversion, compile and inspect generated JS before restarting PM2.

## References

- ClojureVerse: "Promise handling in CLJS using js-await" — https://clojureverse.org/t/promise-handling-in-cljs-using-js-await/8998
- shadow-cljs `shadow.cljs.modern` source — https://raw.githubusercontent.com/thheller/shadow-cljs/master/src/main/shadow/cljs/modern.cljc
- shadow-cljs changelog entries for `add very basic js-await macro for promise interop` and `fix js-await catch` — https://raw.githubusercontent.com/thheller/shadow-cljs/master/CHANGELOG.md
