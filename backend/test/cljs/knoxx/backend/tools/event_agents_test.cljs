(ns knoxx.backend.tools.event-agents-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.tools.actors :as actor-tools]
            [knoxx.backend.tools.event-agents :as event-tools]))

(defn- assert-valid-self-headers
  [headers]
  (is (= "application/json" (aget headers "Content-Type")))
  (is (= "system-admin@open-hax.local" (aget headers "x-knoxx-user-email")))
  (is (= "test-key" (aget headers "X-API-Key")))
  (when (exists? js/Headers)
    (let [h (js/Headers. headers)]
      (is (= "application/json" (.get h "Content-Type")))
      (is (= "test-key" (.get h "X-API-Key"))))))

(deftest event-agent-self-headers-remain-fetch-compatible-with-api-key
  (testing "agent-management tools must pass a headers object to fetch when KNOXX_API_KEY is configured"
    (assert-valid-self-headers (#'event-tools/self-headers {:knoxx-api-key "test-key"}))))

(deftest actor-tool-self-headers-remain-fetch-compatible-with-api-key
  (testing "actor messaging tools share the same self-call auth header contract"
    (assert-valid-self-headers (#'actor-tools/self-headers {:knoxx-api-key "test-key"}))))
