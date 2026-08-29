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
  (testing "returns {:credentials [...]} shape regardless of backend"
    (let [result (await (policy-db/list-actor-credentials! #js {} "discord_bot"))]
      (is (map? result) "result is a CLJS map")
      (is (vector? (:credentials result)) "credentials is a vector"))))

(deftest ^:async bootstrap-local-password-projection-test
  (let [captured* (atom nil)
        bootstrap {:user {:id "user-admin" :email "admin@example.com"}}
        org {:id "org-open-hax"}]
    (await (policy-db/ensure-bootstrap-local-password!
            #js {} org bootstrap {:bootstrapSystemAdminPassword "dev-password"}
            {:encode-password (fn [value] {:encoded value})
             :reconcile-bootstrap-credential!
             (fn [_db payload]
               (reset! captured* payload)
               (js/Promise.resolve nil))}))
    (is (= {:user-id "user-admin"
            :org-id "org-open-hax"
            :account-identifier "admin@example.com"
            :previous-account-identifiers ["system-admin@open-hax.local"]
            :secret-json {:encoded "dev-password" :bootstrap-system-admin true}}
           @captured*))))

(deftest ^:async blank-bootstrap-local-password-revokes-credential-test
  (let [captured* (atom nil)]
    (await (policy-db/ensure-bootstrap-local-password!
            #js {} {:id "org"} {:user {:id "user" :email "admin@example.com"}}
            {:bootstrapSystemAdminPassword ""}
            {:encode-password (fn [_]
                                (is false "blank password must not be encoded"))
             :reconcile-bootstrap-credential!
             (fn [_db payload]
               (reset! captured* payload)
               (js/Promise.resolve nil))}))
    (is (= {:user-id "user"
            :org-id "org"
            :account-identifier "admin@example.com"
            :previous-account-identifiers ["system-admin@open-hax.local"]
            :secret-json nil}
           @captured*))))

(deftest ^:async bootstrap-local-password-reconciles-previous-identity-test
  (let [captured* (atom nil)]
    (await (policy-db/ensure-bootstrap-local-password!
            #js {} {:id "org"} {:user {:id "current-user" :email "new@example.com"}}
            {:bootstrapSystemAdminPassword "new-password"
             :bootstrapSystemAdminPreviousEmails "Pi@Open-Hax.Local, old@example.com"}
            {:encode-password (fn [_] {:hash "encoded"})
             :reconcile-bootstrap-credential!
             (fn [_db payload]
               (reset! captured* payload)
               (js/Promise.resolve nil))}))
    (is (= {:user-id "current-user"
            :org-id "org"
            :account-identifier "new@example.com"
            :previous-account-identifiers
            ["system-admin@open-hax.local" "pi@open-hax.local" "old@example.com"]
            :secret-json {:hash "encoded" :bootstrap-system-admin true}}
           @captured*))))
