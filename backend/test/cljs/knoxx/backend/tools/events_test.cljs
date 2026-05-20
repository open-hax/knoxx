(ns knoxx.backend.tools.events-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.actor.tools :as actor-tools]
            [knoxx.backend.domain.event.tools :as event-tools]))

(defn- assert-valid-self-headers
  [headers]
  (is (= "application/json" (aget headers "Content-Type")))
  (is (= "system-admin@open-hax.local" (aget headers "x-knoxx-user-email")))
  (is (= "test-key" (aget headers "X-API-Key")))
  (when (exists? js/Headers)
    (let [h (js/Headers. headers)]
      (is (= "application/json" (.get h "Content-Type")))
      (is (= "test-key" (.get h "X-API-Key"))))))

(deftest events-self-headers-remain-fetch-compatible-with-api-key
  (testing "events tools must pass a headers object to fetch when KNOXX_API_KEY is configured"
    (assert-valid-self-headers (#'event-tools/self-headers {:knoxx-api-key "test-key"}))))

(deftest actor-tool-self-headers-remain-fetch-compatible-with-api-key
  (testing "actor messaging tools share the same self-call auth header contract"
    (assert-valid-self-headers (#'actor-tools/self-headers {:knoxx-api-key "test-key"}))))
