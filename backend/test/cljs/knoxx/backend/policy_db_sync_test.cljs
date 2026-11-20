(ns knoxx.backend.policy-db-sync-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as contracts-loader]
            [knoxx.backend.domain.policy.protocol :as policy]
            [knoxx.backend.infra.db.policy :as db-policy]
            [knoxx.backend.infra.db.policy.connection :as connection]
            [knoxx.backend.infra.db.policy.projection :as projection]
            [knoxx.backend.infra.stores.mongo-policy-directory :as directory]))

(deftest ^:async unknown-explicit-actor-org-does-not-create-a-user
  (let [created? (atom false)]
    (with-redefs [policy/validate-actor! identity
                  connection/db! (fn [] (js/Promise.resolve :db))
                  directory/find-org-by-slug (fn [_ _] (js/Promise.resolve nil))
                  directory/create-user! (fn [& _] (reset! created? true))]
      (try
        (await (projection/project-actor-via-store! nil {:id "primary"}
                                                     {:actor/id "a" :actor/org "missing"}))
        (is false "an unknown explicit organization must fail")
        (catch :default error
          (is (= "actor organization was not found" (.-message error)))))
      (is (false? @created?) "failure occurs before creating a user"))))

(deftest ^:async sync-contract-role-projections-resolves-to-nil-with-empty-contracts
  ;; Regression: a missing `)` caused (.then (fn [_] nil)) to be passed as the
  ;; second arg (rejection handler) to the outer .then instead of being chained.
  ;; This produced "TypeError: (intermediate value).then is not a function" at
  ;; runtime. The test verifies the full chain resolves rather than throws.
  (testing "returns Promise<nil> when contracts list is empty"
    (with-redefs [contracts-loader/load-all-contracts!
                  (fn [_] (js/Promise.resolve []))]
      (let [result (await (db-policy/sync-contract-role-projections! #js {}))]
        (is (nil? result))))))

(deftest ^:async sync-contract-role-projections-skips-non-role-contracts
  (testing "ignores capability and other contracts, processes only role kind"
    (with-redefs [contracts-loader/load-all-contracts!
                  (fn [_]
                    (js/Promise.resolve
                     [{:contractClass "capabilities" :id "cap1"
                       :contract {:cap/tools ["tool:foo"]}}
                      {:contractClass "permissions" :id "perm1"
                       :contract {}}]))]
      (let [result (await (db-policy/sync-contract-role-projections! #js {}))]
        (is (nil? result))))))
