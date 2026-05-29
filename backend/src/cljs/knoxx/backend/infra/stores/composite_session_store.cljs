(ns knoxx.backend.infra.stores.composite-session-store
  "Composite session store: Redis-primary for live sessions, OpenPlanner for archives.

   Read path: Redis first, OpenPlanner fallback.
   Write path:
     - live run (running/queued/waiting_input) → Redis only
     - terminal run (completed/failed/cancelled) → Redis + OpenPlanner
     - complete-run! → Redis patch, then put full result to OpenPlanner"
  (:require [knoxx.backend.shape.session-persistence :refer [ISessionStore get-run put-run! patch-run! list-active-runs complete-run! delete-run!]]))

(def ^:private live-statuses #{"running" "queued" "waiting_input"})

(defrecord CompositeSessionStore [redis-store op-store]
  ISessionStore

  (put-run! [_ run]
    (if (contains? live-statuses (:status run))
      (put-run! redis-store run)
      (-> (put-run! redis-store run)
          (.then (fn [_] (put-run! op-store run)))
          (.then (fn [_] run)))))

  (get-run [_ run-id]
    (-> (get-run redis-store run-id)
        (.then (fn [r]
                 (if r r (get-run op-store run-id))))))

  (patch-run! [_ run-id patch]
    (-> (patch-run! redis-store run-id patch)
        (.then (fn [updated]
                 (when-not (contains? live-statuses (:status updated))
                   (put-run! op-store updated))
                 updated))))

  (list-active-runs [_ session-id]
    (list-active-runs redis-store session-id))

  (complete-run! [_ run-id opts]
    (-> (complete-run! redis-store run-id opts)
        (.then (fn [redis-final]
                 (-> (put-run! op-store redis-final)
                     (.then (fn [_] redis-final)))))))

  (delete-run! [_ run-id]
    (-> (delete-run! redis-store run-id)
        (.then (fn [_] (delete-run! op-store run-id)))
        (.then (fn [_] true)))))
