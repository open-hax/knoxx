(ns knoxx.backend.infra.mailbox-changes
  "Process-local observation of accepted mailbox appends."
  (:require [knoxx.backend.domain.mailbox-changes :as domain]
            [knoxx.backend.extern.clio-store :as observers]))

(defonce ^:private listeners* (atom {}))

(defn subscribe!
  "Observe sanitized accepted mailbox changes; return an idempotent unsubscribe."
  [listener]
  (when-not (fn? listener)
    (throw (ex-info "Mailbox change listener must be callable" {:status 500})))
  (let [id (gensym "mailbox-listener-")]
    (swap! listeners* assoc id listener)
    (fn [] (swap! listeners* dissoc id) nil)))

(defn accepted!
  "Called only after Clio accepts a state-changing append, never during replay."
  [operation]
  (when-let [change (domain/accepted-change operation)]
    (doseq [listener (vals @listeners*)]
      (observers/notify-subscriber! #(listener change)))))
