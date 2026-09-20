(ns knoxx.backend.infra.stores.local-openplanner-reference
  "Disposable reference projection for the finite local OpenPlanner vocabulary."
  (:require [knoxx.backend.domain.local-openplanner :as domain]))

(defn read!
  "Read named plain-data query against a reference projection."
  [state query]
  (domain/query @state query))

(defn write!
  "Apply and expose one lawful reference transition."
  [state operation]
  (let [[next-state result] (domain/transition @state operation)]
    (reset! state next-state)
    result))

(defn projection
  "Build a fresh replay target with no persistent cache authority."
  []
  (let [state (atom {})] {:store state :snapshot #(deref state)}))
