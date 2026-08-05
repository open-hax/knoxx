(ns knoxx.backend.extern.async-local-storage
  "Extern adapter owning Node's AsyncLocalStorage boundary.

   All raw interop with node:async_hooks is born and dies here; callers upstream
   speak CLJS values only.

   Why this exists rather than a global atom: a store created here keeps a
   separate value per asynchronous execution context, and that value survives
   `await`. A plain atom does not. Two concurrent requests interleaving at any
   await point would read each other's value, which for request-scoped identity
   means one caller acting as another. The distinction only shows up under
   concurrency, so it cannot be found by testing one request at a time."
  (:require ["node:async_hooks" :as async-hooks]))

(defn create-store
  "A new AsyncLocalStorage instance."
  []
  (new (.-AsyncLocalStorage async-hooks)))

(defn run-with
  "Call f with no arguments inside a context where this store holds value.

   Returns whatever f returns, including a promise: the context follows the
   promise chain, so an async f still sees value after every await."
  [^js store value f]
  (.run store value f))

(defn current
  "The value this store holds in the calling context, or nil outside one."
  [^js store]
  (.getStore store))
