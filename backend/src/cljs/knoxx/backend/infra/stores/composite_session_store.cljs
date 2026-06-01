(ns knoxx.backend.infra.stores.composite-session-store
  "Composite session store: Redis-primary for live sessions, OpenPlanner for archives.

   Read path: Redis first, OpenPlanner fallback.
   Write path:
     - live run (running/queued/waiting_input) → Redis only
     - terminal run (completed/failed/cancelled) → Redis + OpenPlanner
     - complete-run! → Redis patch, then put full result to OpenPlanner"
  (:require [knoxx.backend.shape.session-persistence :refer [ISessionStore get-run put-run! patch-run! list-active-runs complete-run! delete-run!]]))

(def ^:private live-statuses #{"running" "queued" "waiting_input"})

(defn ^:async put-composite-run!
  [redis-store op-store run]
  (if (contains? live-statuses (:status run))
    (await (put-run! redis-store run))
    (do
      (await (put-run! redis-store run))
      (await (put-run! op-store run))
      run)))

(defn ^:async get-composite-run
  [redis-store op-store run-id]
  (or (await (get-run redis-store run-id))
      (await (get-run op-store run-id))))

(defn ^:async patch-composite-run!
  [redis-store op-store run-id patch]
  (let [updated (await (patch-run! redis-store run-id patch))]
    (when-not (contains? live-statuses (:status updated))
      (await (put-run! op-store updated)))
    updated))

(defn ^:async complete-composite-run!
  [redis-store op-store run-id opts]
  (let [redis-final (await (complete-run! redis-store run-id opts))]
    (await (put-run! op-store redis-final))
    redis-final))

(defn ^:async delete-composite-run!
  [redis-store op-store run-id]
  (await (delete-run! redis-store run-id))
  (await (delete-run! op-store run-id))
  true)

(defrecord CompositeSessionStore [redis-store op-store]
  ISessionStore

  (put-run! [_ run]
    (put-composite-run! redis-store op-store run))

  (get-run [_ run-id]
    (get-composite-run redis-store op-store run-id))

  (patch-run! [_ run-id patch]
    (patch-composite-run! redis-store op-store run-id patch))

  (list-active-runs [_ session-id]
    (list-active-runs redis-store session-id))

  (complete-run! [_ run-id opts]
    (complete-composite-run! redis-store op-store run-id opts))

  (delete-run! [_ run-id]
    (delete-composite-run! redis-store op-store run-id)))
