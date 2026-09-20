(ns knoxx.backend.infra.source-projection-health
  "A durable source revision must match its filesystem projection before approval or publishing."
  (:require [knoxx.backend.infra.source-authoring-store :as source]))
(defn ^:async assert-current!
  "Fail closed on a pending projection; an old accepted file cannot override newer facts."
  [provider scope snapshot]
  (when provider
    (let [latest (peek (await (source/source-events! provider scope)))]
      (when (and latest (not= (:source/revision latest) (:revision snapshot)))
        (throw (ex-info "Source projection requires repair by retrying its source command"
                        {:status 503 :code "source_projection_repair_required"
                         :document (:document scope) :expected-revision (:source/revision latest)
                         :observed-revision (:revision snapshot)}))))) snapshot)
