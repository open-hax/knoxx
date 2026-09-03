(ns knoxx.backend.policy-db-credentials-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.db.policy :as policy-db]))

(deftest list-actor-credentials-is-defined
  ;; Regression: discord/source called (.listActorCredentials policy-db "discord_bot")
  ;; but policy-db is a raw pg-pool, not the old JS facade object that had this
  ;; method. The function was missing entirely. Verify it exists.
  (testing "list-actor-credentials! is a public function"
    (is (fn? policy-db/list-actor-credentials!))))

(deftest ^:async policy-db-initialization-failure-propagates-test
  (let [failure (js/Error. "fatal policy initialization")]
    (with-redefs [policy-db/initialise-policy-db!
                  (fn [_] (js/Promise.reject failure))]
      (try
        (await (policy-db/create-policy-db {}))
        (is false "security-critical policy initialization must reject")
        (catch :default err
          (is (identical? failure err)))))))

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

(deftest ^:async bootstrap-login-follows-active-credential-after-opposite-primary-completion-test
  (let [membership-query* (atom nil)
        result (await
                (policy-db/local-password-auth-record!
                 nil "ADMIN@EXAMPLE.COM"
                 {:get-db! (fn [] (js/Promise.resolve :db))
                  :find-user! (fn [_db email]
                                (is (= "admin@example.com" email))
                                (js/Promise.resolve {:id "user-admin" :status "active"}))
                  ;; Credential convergence committed org A last, while the
                  ;; independent primary-org update committed org B last.
                  :find-bootstrap-credential!
                  (fn [_db user-id account]
                    (is (= ["user-admin" "admin@example.com"] [user-id account]))
                    (js/Promise.resolve {:user_id user-id
                                         :org_id "org-a"
                                         :secret_json {:hash "active-a"
                                                       :bootstrap-system-admin true}}))
                  :find-membership!
                  (fn [_db query]
                    (reset! membership-query* query)
                    (js/Promise.resolve {:id "membership-a"
                                         :user_id "user-admin"
                                         :user_status "active"
                                         :status "active"
                                         :email "admin@example.com"
                                         :display_name "Admin"
                                         :org_id "org-a"
                                         :org_slug "former-primary-a"
                                         :actor_id "system_admin"}))
                  :get-membership-credential!
                  (fn [& _]
                    (is false "the default/primary org B credential must not be selected")
                    (js/Promise.resolve nil))}))]
    (is (= {:user-id "user-admin"
            :email "admin@example.com"
            :display-name "Admin"
            :membership-id "membership-a"
            :org-id "org-a"
            :org-slug "former-primary-a"
            :actor-id "system_admin"
            :secret-json {:hash "active-a" :bootstrap-system-admin true}}
           result))
    (is (= {:user-email "admin@example.com"
            :active-only true
            :org-id "org-a"}
           @membership-query*)
        "login scopes membership selection to the committed active credential")))

(deftest ^:async ordinary-local-login-keeps-default-membership-selection-test
  (let [membership-query* (atom nil)
        result (await
                (policy-db/local-password-auth-record!
                 nil "worker@example.com"
                 {:get-db! (fn [] (js/Promise.resolve :db))
                  :find-user! (fn [& _]
                                (js/Promise.resolve {:id "worker" :status "active"}))
                  :find-bootstrap-credential! (fn [& _] (js/Promise.resolve nil))
                  :find-membership!
                  (fn [_db query]
                    (reset! membership-query* query)
                    (js/Promise.resolve {:id "membership-primary"
                                         :user_id "worker"
                                         :user_status "active"
                                         :status "active"
                                         :email "worker@example.com"
                                         :display_name "Worker"
                                         :org_id "org-primary"
                                         :org_slug "primary"
                                         :actor_id "worker"}))
                  :get-membership-credential!
                  (fn [& _]
                    (js/Promise.resolve {:secret_json {:hash "ordinary"}}))}))]
    (is (= {:user-email "worker@example.com" :active-only true}
           @membership-query*))
    (is (= "org-primary" (:org-id result)))
    (is (= {:hash "ordinary"} (:secret-json result)))))
