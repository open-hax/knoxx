(ns knoxx.backend.infra.stores.composite-session-store
  "Writes to both Redis (live) and OpenPlanner (archive).
   On complete-run!, verifies both agree on the final run shape."
  (:require [shadow.cljs.modern :refer [js-await]]
            [knoxx.backend.contracts.session-persistence :refer [ISessionStore]]))

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
    (js-await [_ (.put-run! redis-store run)]
      (.put-run! op-store run)
      run))

  (get-run [_ run-id]
    (js-await [r (.get-run redis-store run-id)]
      (if r r (.get-run op-store run-id))))

  (patch-run! [_ run-id patch]
    (js-await [updated (.patch-run! redis-store run-id patch)]
      (.patch-run! op-store run-id patch)
      updated))

  (list-active-runs [_ session-id]
    (.list-active-runs redis-store session-id))

  (complete-run! [_ run-id opts]
    (js-await [redis-final (.complete-run! redis-store run-id opts)]
      (js-await [op-final (.complete-run! op-store run-id opts)]
        (when-not (agree? redis-final op-final)
          (js/console.error "CompositeSessionStore divergence:"
                            (clj->js (ex-data
                                       (divergence-error run-id
                                                         redis-final
                                                         op-final)))))
        redis-final)))

  (delete-run! [_ run-id]
    (js-await [_ (.delete-run! redis-store run-id)]
      (.delete-run! op-store run-id)
      true)))