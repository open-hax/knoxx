---
title: defn-async / defroute Macro Design
category: async
created: 2026-04-27
status: proposed
tags: [macro, cljs, shadow-cljs, async, defn-async, defroute]
related: [async/async-refactor-plan.md]
---

# `defn-async` / `defroute` Macro Design

Proposed macro substrate to standardize async route handlers in knoxx.

## Spec

```edn
{:spec/name      "knoxx/backend/http-async-routes"
 :spec/version   "0.1.0"
 :spec/purpose   "Flatten async nesting in Knoxx MCP handlers to workflow orchestration"
 :spec/requires  ["shadow-cljs 2.28+" "malli 1.0+" "funcool/promesa 12+"]}
```

## `defn-async`

Defines a function that always returns a Promise. Lowers body to chained
`p/let` (or `js-await`) sequencing.

```clojure
(defmacro defn-async [name args & body]
  `(defn ~name ~args
     (p/let ~@body)))
```

## `defroute`

Expands to thin route registration + named workflow dispatch.

```clojure
(defmacro defroute [method path schema workflow-fn]
  `(route! ~app ~method ~path
           (fn [req# reply#]
             (if-let [deps# (u/route-deps req# reply#)]
               (u/with-request-context!
                 (:runtime deps#) req# reply#
                 (fn [ctx#]
                   (let [body# (u/decode ~schema (aget req# "body"))]
                     (~workflow-fn deps# ctx# body# req# reply#))))
               nil))))
```

## Workflow Stage Contract

```
stage :: deps → ctx → input → Promise[output]
```

Standard stage order: `:parse → :validate → :load → :resolve → :build → :handle → :respond`

## CI Checks

- `:returns-promise?` — all `defroute` handlers return Promise
- `:uses-named-stages?` — no anonymous `.then` lambdas inside handler
- `:no-nested-lambdas?` — no nesting depth > 1
- `:malli-schema?` — body has a Malli schema

## Next

- [ ] Write the minimal `defn-async` macro pair and test under shadow-cljs REPL
- [ ] Convert `mcp-post` and `oauth-token` handlers as first adopters
- [ ] Retrofit existing `.then` pyramids route-by-route
