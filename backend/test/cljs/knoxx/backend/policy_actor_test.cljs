(ns knoxx.backend.policy-actor-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.db.policy :as policy-db]))

(deftest default-membership-actor-id-follows-role-shape
  (testing "system admins map to the system_admin user actor"
    (is (= "system_admin" (policy-db/default-membership-actor-id ["system_admin"]))))
  (testing "hyphenated contract role ids normalize to the same system admin actor"
    (is (= "system_admin" (policy-db/default-membership-actor-id ["system-admin"]))))
  (testing "non-admin users map to the default workspace user actor"
    (is (= "workspace_user" (policy-db/default-membership-actor-id ["knowledge_worker"]))))
  (testing "empty role sets still resolve to a safe user actor"
    (is (= "workspace_user" (policy-db/default-membership-actor-id [])))))
