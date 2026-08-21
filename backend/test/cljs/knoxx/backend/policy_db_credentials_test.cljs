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
            {:deactivate-credential! (fn [& _]
                                       (js/Promise.resolve nil))
             :deactivate-other-bootstrap-credentials! (fn [& _]
                                                        (js/Promise.resolve nil))
             :encode-password (fn [value] {:encoded value})
             :upsert-credential! (fn [_db user-id org-id provider payload]
                                   (reset! captured* [user-id org-id provider payload])
                                   (js/Promise.resolve nil))}))
    (is (= ["user-admin" "org-open-hax" "local"
            {:kind "password"
             :account-identifier "admin@example.com"
             :secret-json {:encoded "dev-password" :bootstrap-system-admin true}
             :status "active"}]
           @captured*))))

(deftest ^:async blank-bootstrap-local-password-revokes-credential-test
  (let [captured* (atom nil)]
    (await (policy-db/ensure-bootstrap-local-password!
            #js {} {:id "org"} {:user {:id "user"}} {:bootstrapSystemAdminPassword ""}
            {:deactivate-credential! (fn [_db user-id org-id provider kind]
                                       (reset! captured* [user-id org-id provider kind])
                                       (js/Promise.resolve nil))
             :deactivate-other-bootstrap-credentials! (fn [& _]
                                                        (js/Promise.resolve nil))
             :encode-password identity
             :upsert-credential! (fn [& _]
                                   (is false "blank password must not be upserted")
                                   (js/Promise.resolve nil))}))
    (is (= ["user" "org" "local" "password"] @captured*))))

(deftest ^:async bootstrap-local-password-reconciles-previous-identity-test
  (let [captured* (atom nil)]
    (await (policy-db/ensure-bootstrap-local-password!
            #js {} {:id "org"} {:user {:id "current-user" :email "new@example.com"}}
            {:bootstrapSystemAdminPassword "new-password"}
            {:deactivate-credential! (fn [& _] (js/Promise.resolve nil))
             :deactivate-other-bootstrap-credentials!
             (fn [_db current-user-id]
               (reset! captured* current-user-id)
               (js/Promise.resolve nil))
             :encode-password (fn [_] {:hash "encoded"})
             :upsert-credential! (fn [& _] (js/Promise.resolve nil))}))
    (is (= "current-user" @captured*))))
