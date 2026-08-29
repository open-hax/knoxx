(ns knoxx.backend.extern-mongo-transaction-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.mongo :as extern-mongo]))

(deftest ^:async transaction-boundary-hides-native-session-test
  (let [events* (atom [])
        session
        #js {:withTransaction
             (fn [callback options]
               (swap! events* conj
                      [:transaction-options
                       (.-level (.-readConcern options))
                       (.-w (.-writeConcern options))])
               (callback))
             :endSession
             (fn []
               (swap! events* conj [:end-session])
               (js/Promise.resolve nil))}
        collection
        #js {:updateOne
             (fn [query update options]
               (swap! events* conj
                      [:update-one
                       (js->clj query :keywordize-keys true)
                       (js->clj update :keywordize-keys true)
                       {:upsert (.-upsert options)
                        :write-concern (.-w (.-writeConcern options))
                        :collation-locale (.-locale (.-collation options))
                        :collation-strength (.-strength (.-collation options))
                        :session? (= session (.-session options))}])
               (js/Promise.resolve
                #js {:matchedCount 1 :modifiedCount 1 :upsertedCount 0}))
             :updateMany
             (fn [_query _update options]
               (swap! events* conj
                      [:update-many :session? (= session (.-session options))])
               (js/Promise.resolve
                #js {:matchedCount 2 :modifiedCount 2 :upsertedCount 0}))}
        client #js {:startSession (fn [] session)}
        result
        (await
         (extern-mongo/with-transaction!
          client
          (^:async fn [{:keys [update-one! update-many!]}]
            {:one (await (update-one!
                          collection
                          {:credential_id "credential"}
                          {:$set {:status "active"}}
                          {:upsert true
                           :write-concern "majority"
                           :case-insensitive? true}))
             :many (await (update-many!
                           collection
                           {:org_id "org"}
                           {:$set {:status "inactive"}}
                           {}))})))]
    (is (= {:one {:matched-count 1 :modified-count 1 :upserted-count 0}
            :many {:matched-count 2 :modified-count 2 :upserted-count 0}}
           result))
    (is (= [:transaction-options "snapshot" "majority"] (first @events*)))
    (is (= [:update-one
            {:credential_id "credential"}
            {:$set {:status "active"}}
            {:upsert true
             :write-concern "majority"
             :collation-locale "en"
             :collation-strength 2
             :session? true}]
           (second @events*)))
    (is (= [:update-many :session? true] (nth @events* 2)))
    (is (= [:end-session] (last @events*)))))

(deftest ^:async transaction-boundary-closes-session-after-error-test
  (let [closed?* (atom false)
        session #js {:withTransaction (fn [callback _options] (callback))
                     :endSession (fn []
                                   (reset! closed?* true)
                                   (js/Promise.resolve nil))}
        client #js {:startSession (fn [] session)}]
    (try
      (await
       (extern-mongo/with-transaction!
        client
        (fn [_operations]
          (js/Promise.reject (js/Error. "transaction failed")))))
      (is false "transaction error must propagate")
      (catch js/Error err
        (is (= "transaction failed" (.-message err)))))
    (is @closed?* "session is closed after an aborted transaction")))
