(ns knoxx.backend.mongo-thread-atomicity-e2e
  "Explicit native Mongo concurrent thread mutation and restart proof."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.mongo-run-native-fixture :as native]
            [knoxx.backend.extern.provider-recovery-fixture :as concurrent]
            [knoxx.backend.infra.stores.mongo-thread-store :as store]
            [knoxx.backend.shape.thread-store :as protocol]
            [knoxx.backend.thread-identity-proof :as identity-proof]))

(deftest ^:async native-concurrent-patches-and-rewinds-survive-restart
  (let [fixture (atom (await (native/open!)))
        messages [{:role "system" :content "Keep"}
                  {:role "user" :content "First"} {:role "assistant" :content "Reply"}
                  {:role "user" :content "Second"}]
        patches (conj (mapv #(hash-map (keyword (str "field-" %)) %) (range 20))
                      {:has_active_stream true} {:messages messages :run_id "new"})]
    (try
      (await (store/setup-indexes! (:db @fixture)))
      (let [provider (store/create-store (:db @fixture))]
        (await (protocol/put-thread! provider {:session_id "native-thread" :status "running"
                                             :messages [] :has_active_stream false :run_id "old"}))
        (let [results (await (concurrent/settled
                             (mapv #(protocol/patch-thread! provider "native-thread" %) patches)))
              current (await (protocol/read-thread provider "native-thread"))]
          (is (every? #(= :fulfilled (:status %)) results))
          (doseq [patch patches] (is (= patch (select-keys current (keys patch))))))
        (let [results (await (concurrent/settled
                             [(protocol/rewind-thread! provider "native-thread" 1)
                              (protocol/rewind-thread! provider "native-thread" 1)]))]
          (is (every? #(= :fulfilled (:status %)) results))))
      (reset! fixture (await (native/restart! @fixture)))
      (let [current (await (protocol/read-thread (store/create-store (:db @fixture)) "native-thread"))]
        (is (= [(first messages)] (:messages current)))
        (is (= "new" (:run_id current)))
        (is (= "waiting_input" (:status current)))
        (is (false? (:has_active_stream current)))
        (doseq [patch (take 20 patches)] (is (= patch (select-keys current (keys patch))))))
      (finally (await (native/close! @fixture))))))

(deftest ^:async native-thread-identity-guards-both-write-entry-points
  (let [fixture (await (native/open!))]
    (try
      (await (store/setup-indexes! (:db fixture)))
      (let [provider (store/create-store (:db fixture))]
        (await (identity-proof/check-rebinding! provider))
        (await (identity-proof/check-initial-assignment! provider))
        (await (identity-proof/check-invalid-identity! provider))
        (await (identity-proof/check-compatible-and-unique! provider))
        (await (identity-proof/check-recreated-owner! provider (:db fixture))))
      (finally (await (native/close! fixture))))))
