(ns knoxx.backend.policy-db-sync-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as contracts-loader]
            [knoxx.backend.infra.db.policy :as db-policy]))

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
