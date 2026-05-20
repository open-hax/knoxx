(ns knoxx.backend.infra.stores.composite-session-store
  "Writes to both Redis (live) and OpenPlanner (archive).
   On complete-run!, verifies both agree on the final run shape."
  (:require [knoxx.backend.shape.session-persistence :refer [ISessionStore get-run put-run! patch-run! complete-run! delete-run!]]))

(defn- divergence-error [run-id redis-run op-run]
  (ex-info "Session persistence divergence detected"
           {:run-id run-id
            :redis-status (:status redis-run)
            :op-status (:status op-run)
            :redis-answer? (some? (:answer redis-run))
            :op-answer? (some? (:answer op-run))
            :redis-tool-count (count (:tool_receipts redis-run))
            :op-tool-count (count (:tool_receipts op-run))}))

(defn- agree? [redis-run op-run]
  (and (= (:status redis-run) (:status op-run))
       (= (count (:tool_receipts redis-run))
          (count (:tool_receipts op-run)))
       (= (some? (:answer redis-run))
          (some? (:answer op-run)))))

(defrecord CompositeSessionStore [redis-store op-store]
  ISessionStore

  (put-run! [_ run]
    (-> (put-run! redis-store run)
        (.then (fn [_] (put-run! op-store run)))
        (.then (fn [_] run))))

  (get-run [_ run-id]
    (-> (get-run redis-store run-id)
        (.then (fn [r]
                 (if r r (get-run op-store run-id))))))

  (patch-run! [_ run-id patch]
    (-> (patch-run! redis-store run-id patch)
        (.then (fn [updated]
                 (patch-run! op-store run-id patch)
                 updated))))

  (list-active-runs [_ session-id]
    (list-active-runs redis-store session-id))

  (complete-run! [_ run-id opts]
    (-> (complete-run! redis-store run-id opts)
        (.then (fn [redis-final]
                 (-> (complete-run! op-store run-id opts)
                     (.then (fn [op-final]
                              (when-not (agree? redis-final op-final)
                                (js/console.error "CompositeSessionStore divergence:"
                                                  (clj->js (ex-data
                                                             (divergence-error run-id
                                                                               redis-final
                                                                               op-final)))))
                              redis-final)))))))

  (delete-run! [_ run-id]
    (-> (delete-run! redis-store run-id)
        (.then (fn [_] (delete-run! op-store run-id)))
        (.then (fn [_] true)))))
