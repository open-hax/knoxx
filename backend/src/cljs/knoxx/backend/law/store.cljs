(ns knoxx.backend.law.store
  "Contracts for store resources: document schema guards.

   Stores declare a Malli :store/schema. Every inserted document must satisfy
   it — the guard throws on violation so bad documents never reach a backend."
  (:require [malli.core :as m]
            [malli.error :as me]))

(defn compile-schema-guard
  "Compile a Malli schema into a document guard.
   The guard returns the document unchanged when valid and throws ex-info with
   humanized :errors when invalid. A nil schema yields a pass-through guard."
  [schema]
  (if (nil? schema)
    identity
    (let [validator (m/validator schema)
          explainer (m/explainer schema)]
      (fn guard [doc]
        (if (validator doc)
          doc
          (throw (ex-info "Store document failed schema validation"
                          {:errors (me/humanize (explainer doc))
                           :doc doc})))))))
