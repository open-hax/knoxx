(ns knoxx.backend.policy-db-credentials-test
  (:require [cljs.test :refer [async deftest is testing]]
            [knoxx.backend.infra.db.policy :as policy-db]))

(deftest list-actor-credentials-is-defined
  ;; Regression: discord/source called (.listActorCredentials policy-db "discord_bot")
  ;; but policy-db is a raw pg-pool, not the old JS facade object that had this
  ;; method. The function was missing entirely. Verify it exists.
  (testing "list-actor-credentials! is a public function"
    (is (fn? policy-db/list-actor-credentials!))))

(deftest list-actor-credentials-rejects-blank-provider
  (async done
    (testing "rejects with an error when provider is blank"
      (let [mock-pool #js {:query (fn [_s _p] (js/Promise.resolve #js {:rows #js [] :rowCount 0}))}]
        (-> (policy-db/list-actor-credentials! mock-pool "")
            (.then (fn [_]
                     (is false "should have rejected on blank provider")
                     (done)))
            (.catch (fn [_err]
                      (is true "rejected as expected for blank provider")
                      (done))))))))

(deftest list-actor-credentials-returns-credentials-map
  (async done
    (testing "returns {:credentials [...]} with rows keywordized"
      (let [mock-row #js {:actor_id "actor1" :provider "discord_bot"
                          :bot_token "tok" :user_id "u1" :org_id "o1" :org_slug "org"}
            mock-pool #js {:query (fn [_s _p]
                                    (js/Promise.resolve #js {:rows #js [mock-row] :rowCount 1}))}]
        (-> (policy-db/list-actor-credentials! mock-pool "discord_bot")
            (.then (fn [result]
                     (is (map? result) "result is a CLJS map")
                     (is (vector? (:credentials result)) "credentials is a vector")
                     (is (= 1 (count (:credentials result))) "one credential returned")
                     (done)))
            (.catch (fn [err]
                      (is false (str "unexpected error: " err))
                      (done))))))))
