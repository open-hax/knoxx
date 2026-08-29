(ns knoxx.backend.infra.mongo-client-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.infra.mongo-client :as mongo-client]))

(deftest ^:async with-transaction-closes-session-test
  (let [events* (atom [])
        session #js {:withTransaction
                     (fn [callback opts]
                       (swap! events* conj [:transaction-options
                                            (js->clj opts :keywordize-keys true)])
                       (callback))
                     :endSession
                     (fn []
                       (swap! events* conj [:end-session])
                       (js/Promise.resolve nil))}
        client #js {:startSession
                    (fn []
                      (swap! events* conj [:start-session])
                      session)}
        result (await (mongo-client/with-transaction!
                       client
                       (fn [actual-session]
                         (swap! events* conj [:callback (= session actual-session)])
                         (js/Promise.resolve :done))))]
    (is (= :done result))
    (is (= [[:start-session]
            [:transaction-options {:readConcern {:level "snapshot"}
                                   :writeConcern {:w "majority"}}]
            [:callback true]
            [:end-session]]
           @events*))))

(deftest ^:async with-transaction-closes-session-after-error-test
  (let [closed?* (atom false)
        session #js {:withTransaction (fn [callback _opts] (callback))
                     :endSession (fn []
                                   (reset! closed?* true)
                                   (js/Promise.resolve nil))}
        client #js {:startSession (fn [] session)}]
    (try
      (await (mongo-client/with-transaction!
              client
              (fn [_]
                (js/Promise.reject (js/Error. "transaction failed")))))
      (is false "transaction error must propagate")
      (catch js/Error err
        (is (= "transaction failed" (.-message err)))))
    (is @closed?* "session is closed after an aborted transaction")))
