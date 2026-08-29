(ns knoxx.backend.law.bootstrap-credentials-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.law.bootstrap-credentials :as bootstrap-law]))

(deftest global-bootstrap-credential-law-test
  (testing "the global lock is not derived from the current organization"
    (is (= "bootstrap-system-admin-local-password"
           bootstrap-law/global-reconciliation-lock-id)))

  (testing "current and prior identities are canonical and idempotent"
    (is (= ["old@example.com" "current@example.com"]
           (bootstrap-law/managed-account-identifiers
            "Current@Example.com"
            [" OLD@example.com " "old@example.com" ""]))))

  (testing "retirement is deliberately not organization-scoped"
    (let [query (bootstrap-law/managed-active-password-query
                 ["old@example.com" "current@example.com"])]
      (is (= {:provider "local"
              :kind "password"
              :status "active"
              :$or [{:secret_json.bootstrap-system-admin true}
                    {:account_identifier
                     {:$in ["old@example.com" "current@example.com"]}}]}
             query))
      (is (not (contains? query :org_id))))))
