(ns knoxx.backend.infra.stores.mongo-policy-actor-credentials-boundary-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.infra.stores.mongo-policy-actor-credentials :as creds]))

(deftest ^:async reconcile-bootstrap-date-values-cross-as-scalars-test
  (let [ensure-created-at* (atom nil)
        operations* (atom [])
        lock-handle
        #js {:updateOne
             (fn [_query update _options]
               (reset! ensure-created-at*
                       (aget (aget update "$setOnInsert") "created_at"))
               (js/Promise.resolve
                #js {:matchedCount 0 :modifiedCount 0 :upsertedCount 1}))}
        db #js {:collection
                (fn [name]
                  (if (= name "knoxx_policy_reconciliation_locks")
                    lock-handle
                    #js {}))}
        record-operation!
        (fn [kind]
          (fn [_collection query update options]
            (swap! operations* conj
                   {:kind kind
                    :query query
                    :update update
                    :options options})
            (js/Promise.resolve
             {:matched-count (if (contains? query :_id) 1 0)})))
        with-transaction!
        (fn [f]
          (f {:update-one! (record-operation! :one)
              :update-many! (record-operation! :many)}))]
    (await (creds/reconcile-bootstrap-local-password!
            db {:user-id "current-user"
                :org-id "org"
                :account-identifier "current@example.com"
                :previous-account-identifiers ["old@example.com"]
                :secret-json {:hash "current"
                              :bootstrap-system-admin true}}
            with-transaction!))
    (is (instance? js/Date @ensure-created-at*)
        "the non-transactional lock upsert receives a boundary-owned BSON Date")
    (let [instants (for [{:keys [update]} @operations*
                         [_ assignments] update
                         [field value] assignments
                         :when (#{:created_at :updated_at} field)]
                     value)]
      (is (= 4 (count instants)))
      (is (every? number? instants)
          "transaction operation maps carry only CLJS timestamp scalars")
      (is (= 1 (count (set instants)))
          "one transaction attempt uses one consistent instant"))
    (is (= [#{:updated_at}
            #{:updated_at}
            #{:created_at :updated_at}]
           (mapv #(get-in % [:options :bson-date-fields]) @operations*))
        "each operation declares exactly which scalar fields become BSON Dates")))
