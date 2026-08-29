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

(deftest bootstrap-login-selection-law-test
  (testing "the active marker query is canonical and user-scoped"
    (is (= {:user_id "user-admin"
            :provider "local"
            :kind "password"
            :account_identifier "admin@example.com"
            :status "active"
            :secret_json.bootstrap-system-admin true}
           (bootstrap-law/active-bootstrap-login-query
            "user-admin" " ADMIN@EXAMPLE.COM "))))

  (testing "a marked credential owns the membership organization"
    (is (= {:user-email "admin@example.com"
            :active-only true
            :org-id "org-a"}
           (bootstrap-law/local-password-membership-query
            "admin@example.com" {:org_id "org-a"})))
    (is (= {:user-email "worker@example.com" :active-only true}
           (bootstrap-law/local-password-membership-query
            "worker@example.com" nil))))

  (testing "zero and one markers are deterministic; duplicates fail closed"
    (is (nil? (bootstrap-law/sole-active-bootstrap-credential [])))
    (is (= {:org_id "org-a"}
           (bootstrap-law/sole-active-bootstrap-credential [{:org_id "org-a"}])))
    (is (thrown? js/Error
                 (bootstrap-law/sole-active-bootstrap-credential
                  [{:org_id "org-a"} {:org_id "org-b"}])))))
