(ns knoxx.backend.admin-credential-routes-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.routes.users.admin :as admin-routes]
            [knoxx.backend.infra.db.policy :as policy-db]))

;; ---------------------------------------------------------------------------
;; credential-payload validation
;; ---------------------------------------------------------------------------

(deftest credential-payload-rejects-missing-secret-json
  (testing "throws when secretJson is missing from body"
    (let [body {:orgId "org-1" :kind "bot-token"}]
      (try
        (#'admin-routes/credential-payload body "discord_bot")
        (is false "should have thrown for missing secretJson")
        (catch :default err
          (is (re-find #"secretJson is required" (.-message err))))))))

(deftest credential-payload-rejects-empty-secret-json
  (testing "throws when secretJson is an empty map"
    (let [body {:orgId "org-1" :kind "bot-token" :secretJson {}}]
      (try
        (#'admin-routes/credential-payload body "discord_bot")
        (is false "should have thrown for empty secretJson")
        (catch :default err
          (is (re-find #"secretJson is required" (.-message err))))))))

(deftest credential-payload-rejects-nil-secret-json
  (testing "throws when secretJson is nil"
    (let [body {:orgId "org-1" :kind "bot-token" :secretJson nil}]
      (try
        (#'admin-routes/credential-payload body "discord_bot")
        (is false "should have thrown for nil secretJson")
        (catch :default err
          (is (re-find #"secretJson is required" (.-message err))))))))

(deftest credential-payload-accepts-valid-secret-json
  (testing "returns payload map with secretJson when valid"
    (let [body {:orgId "org-1"
                :kind "bot-token"
                :accountIdentifier "app-123"
                :secretJson {:botToken "tok" :applicationId "123"}}
          result (#'admin-routes/credential-payload body "discord_bot")]
      (is (= "org-1" (:org-id result)))
      (is (= "discord_bot" (:provider result)))
      (is (= "bot-token" (:kind result)))
      (is (= "app-123" (:account-identifier result)))
      (is (= {:botToken "tok" :applicationId "123"} (:secret-json result)))
      (is (= "active" (:status result))))))

(deftest credential-payload-defaults-kind-and-status
  (testing "defaults kind to credential and status to active"
    (let [body {:orgId "org-1" :secretJson {:key "val"}}
          result (#'admin-routes/credential-payload body "discord_bot")]
      (is (= "credential" (:kind result)))
      (is (= "active" (:status result))))))

;; ---------------------------------------------------------------------------
;; actor-credential-response shape
;; ---------------------------------------------------------------------------

(deftest actor-credential-response-includes-secret-json
  (testing "response includes secretJson parsed from DB row"
    (let [row {:id "cred-1"
               :user_id "user-1"
               :org_id "org-1"
               :provider "discord_bot"
               :kind "bot-token"
               :account_identifier "app-123"
               :secret_json #js {:botToken "tok" :applicationId "123" :publicKey "pub"}
               :status "active"
               :created_at "2024-01-01"
               :updated_at "2024-01-01"}
          result (#'policy-db/actor-credential-response row)
          credential (:credential result)]
      (is (map? credential))
      (is (= "cred-1" (:id credential)))
      (is (= "discord_bot" (:provider credential)))
      (is (= "bot-token" (:kind credential)))
      (is (= "app-123" (:accountIdentifier credential)))
      (is (= "active" (:status credential)))
      (is (= {:botToken "tok" :applicationId "123" :publicKey "pub"}
             (:secretJson credential)))
      (is (= #{"applicationId" "botToken" "publicKey"}
             (set (:configuredFields credential)))))))

(deftest actor-credential-response-handles-nil-row
  (testing "returns nil credential when row is nil"
    (let [result (#'policy-db/actor-credential-response nil)]
      (is (nil? (:credential result))))))

(deftest actor-credential-response-handles-empty-secret-json
  (testing "returns empty configuredFields when secret_json is empty"
    (let [row {:id "cred-1"
               :user_id "user-1"
               :org_id "org-1"
               :provider "discord_bot"
               :kind "credential"
               :account_identifier nil
               :secret_json #js {}
               :status "active"
               :created_at "2024-01-01"
               :updated_at "2024-01-01"}
          result (#'policy-db/actor-credential-response row)
          credential (:credential result)]
      (is (= {} (:secretJson credential)))
      (is (= [] (:configuredFields credential))))))
