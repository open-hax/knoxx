(ns knoxx.backend.policy-db-credentials-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.db.policy :as policy-db]))

(deftest list-actor-credentials-is-defined
  ;; Regression: discord/source called (.listActorCredentials policy-db "discord_bot")
  ;; but policy-db is a raw pg-pool, not the old JS facade object that had this
  ;; method. The function was missing entirely. Verify it exists.
  (testing "list-actor-credentials! is a public function"
    (is (fn? policy-db/list-actor-credentials!))))

(deftest ^:async list-actor-credentials-rejects-blank-provider
  (testing "rejects with an error when provider is blank"
    (let [mock-pool #js {:query (fn [_s _p] (js/Promise.resolve #js {:rows #js [] :rowCount 0}))}]
      (try
        (await (policy-db/list-actor-credentials! mock-pool ""))
        (is false "should have rejected on blank provider")
        (catch :default _err
          (is true "rejected as expected for blank provider"))))))

(deftest ^:async list-actor-credentials-returns-credentials-map
  (testing "returns {:credentials [...]} with rows keywordized"
    (let [mock-row #js {:actor_id "actor1" :provider "discord_bot"
                        :bot_token "tok" :user_id "u1" :org_id "o1" :org_slug "org"}
          mock-pool #js {:query (fn [_s _p]
                                  (js/Promise.resolve #js {:rows #js [mock-row] :rowCount 1}))}
          result (await (policy-db/list-actor-credentials! mock-pool "discord_bot"))]
      (is (map? result) "result is a CLJS map")
      (is (vector? (:credentials result)) "credentials is a vector")
      (is (= 1 (count (:credentials result))) "one credential returned"))))
