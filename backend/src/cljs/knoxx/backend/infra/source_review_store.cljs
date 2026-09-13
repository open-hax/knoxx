(ns knoxx.backend.infra.source-review-store
  "Reference semantics for the named revision-bound review persistence port."
  (:require [knoxx.backend.domain.source-review :as review]
            [knoxx.backend.law.source-review :as law]))

(defprotocol ISourceReviewStore
  (read-source-review-events! [store scope] "Read the complete validated scoped history.")
  (admit-source-review! [store scope expected-head event] "Atomically admit or return the exact existing operation."))

(defn admit-in-state
  "Validate identity and causal predecessor inside the provider's atomic state change."
  [state scope expected-head event]
  (law/assert-valid! :source-review/scope law/Scope scope)
  (law/assert-valid! :source-review/event law/Event event)
  (when-not (= expected-head (:review/previous event))
    (throw (ex-info "Source review predecessor differs from requested CAS"
                    {:status 409 :code "source_review_stale_head"})))
  (let [events (review/validated-history scope (get state scope []))
        existing (some #(when (= (:review/id event) (:review/id %)) %) events)]
    (if existing
      (if (review/same-operation? existing event)
        [state {:existing? true :event existing}]
        (throw (ex-info "source review operation identity has different content"
                        {:status 409 :code "source_review_operation_conflict"})))
      [(assoc state scope (review/append-event events scope event)) {:existing? false :event event}])))

(defrecord MemorySourceReviewStore [state]
  ISourceReviewStore
  (read-source-review-events! [_ scope] (review/validated-history scope (get @state scope [])))
  (admit-source-review! [_ scope expected-head event]
    (let [[next-state result] (admit-in-state @state scope expected-head event)]
      (reset! state next-state) result)))

(defn memory-store
  "A disposable reference projection; production selects a durable provider explicitly."
  ([] (->MemorySourceReviewStore (atom {})))
  ([initial] (->MemorySourceReviewStore (atom initial))))
