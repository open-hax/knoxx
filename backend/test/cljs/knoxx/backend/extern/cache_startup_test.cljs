(ns knoxx.backend.extern.cache-startup-test
  "Cache facades retain async error delivery, and startup waits for their indexes."
  (:require [cljs.test :as test]
            [knoxx.backend.bootstrap :as bootstrap]
            [knoxx.backend.extern.provider-recovery-fixture :as fixture]
            [knoxx.backend.infra.cache-services :as cache]
            [knoxx.backend.infra.mongo-client :as mongo]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as oauth]
            [knoxx.backend.infra.stores.mongo-memory-sessions :as memory-sessions]
            [knoxx.backend.infra.stores.mongo-policy-store :as policy]
            [knoxx.backend.infra.stores.mongo-rate-limits :as limits]
            [knoxx.backend.infra.stores.mongo-session-store :as sessions]
            [knoxx.backend.infra.stores.mongo-session-titles :as titles]
            [knoxx.backend.infra.stores.mongo-temp-memory :as memory]
            [knoxx.backend.infra.stores.mongo-translation-evidence :as evidence]
            [knoxx.backend.infra.stores.mongo-translation-split :as splits]
            [knoxx.backend.infra.stores.run-provider-startup :as runs]
            [knoxx.backend.infra.stores.translation-evidence-registry :as evidence-registry]
            [knoxx.backend.infra.stores.translation-split-registry :as split-registry]))

(defn- deferred []
  (let [resolve* (atom nil) reject* (atom nil)]
    {:promise (js/Promise. (fn [resolve reject] (reset! resolve* resolve) (reset! reject* reject)))
     :resolve! (fn [] (@resolve* nil)) :reject! (fn [error] (@reject* error))}))

(defn- ^:async startup-outcome []
  (try (await (bootstrap/start-required-persistence! #js {:warn (fn [& _])}))
       (catch :default error error)))

(defn- ^:async assert-index-barrier! [blocked-cache reject?]
  (let [gate (deferred) installed* (atom false) settled* (atom false)
        failure (ex-info "cache index refused" {})
        setup! (fn [bucket] (fn [_] (when (= blocked-cache bucket) (:promise gate))))]
    (with-redefs [mongo/init-mongo! (fn [] :owned-db) sessions/setup-indexes! (fn [_])
                  titles/setup-indexes! (setup! :titles) memory/setup-indexes! (setup! :memory)
                  memory-sessions/setup-indexes! (fn [_]) oauth/setup-indexes! (fn [_])
                  limits/setup-indexes! (fn [_]) policy/ensure-indexes! (fn [_])
                  evidence/setup-indexes! (fn [_]) evidence/create-store (fn [_] nil)
                  splits/setup-indexes! (fn [_]) splits/create-store (fn [_ _] nil)
                  evidence-registry/store* (atom nil) split-registry/store* (atom nil)
                  runs/install-mongo! (fn [_] (reset! installed* true))]
      (let [pending ((^:async fn [] (let [result (await (startup-outcome))] (reset! settled* true) result)))
            observed (fixture/settled [(:promise gate)])]
        (await (fixture/drain!))
        (test/is (false? @settled*))
        (test/is (false? @installed*))
        (if reject? ((:reject! gate) failure) ((:resolve! gate)))
        (await observed)
        (test/is (= (if reject? failure :owned-db) (await pending)))
        (test/is (= (not reject?) @installed*))))))

(test/deftest ^:async both-cache-indexes-must-complete-before-provider-publication
  (doseq [bucket [:titles :memory]] (await (assert-index-barrier! bucket false))))

(test/deftest ^:async either-cache-index-rejection-refuses-startup
  (doseq [bucket [:titles :memory]] (await (assert-index-barrier! bucket true))))

(defn- capture-call [read!]
  (try {:result (read!)} (catch :default error {:synchronous-error error})))

(test/deftest ^:async reader-facades-deliver-provider-refusal-as-a-promise
  (let [failure (ex-info "cache provider unavailable" {:status 503})]
    (with-redefs [cache/read! (fn [_ _ _] (throw failure))]
      (doseq [read! [#(titles/get-title! "session") #(titles/get-title! :db "session")
                    #(memory/get-memory! "key") #(memory/get-memory! :db "key")]]
        (let [{:keys [result synchronous-error]} (capture-call read!)]
          (test/is (nil? synchronous-error))
          (when result
            (try (await result) (test/is false "provider rejection must propagate")
                 (catch :default error (test/is (identical? failure error))))))))))
