(ns knoxx.backend.mongo-policy-route-compatibility-e2e
  "Real preceding HTTP routes over native Mongo; no activated identity composition."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.identity-fixture :as files]
            [knoxx.backend.extern.mongo-invite-fixture :as invites]
            [knoxx.backend.extern.mongo-policy-invites :as admission]
            [knoxx.backend.extern.mongo-remote-identity-fixture :as directory]
            [knoxx.backend.extern.mongo-run-native-fixture :as native]
            [knoxx.backend.extern.policy-route-fixture :as http]
            [knoxx.backend.infra.db.policy.connection :as connection]
            [knoxx.backend.infra.db.policy.users :as users]
            [knoxx.backend.infra.mongo-client :as mongo]
            [knoxx.backend.runtime.state :as runtime]))

(defn- patch-member [body]
  {:method "PATCH" :url "/api/admin/users/existing-user"
   ;; This is the preceding runtime's admitted header context, not an assertion
   ;; that it provides the later verified-principal authentication boundary.
   :headers {"x-knoxx-user-email" "remote@example.test" "x-knoxx-org-slug" "existing-org"}
   :payload (assoc body :orgId "existing-org")})

(deftest ^:async actual-admin-patch-preserves-omission-and-applies-explicit-empty-roles
  (let [server (await (native/open!)) db (:db server) app (http/app) contracts (files/directory!)]
    (try
      (await (directory/seed! db {:issuer "https://legacy.example.test"}))
      (await (http/grant-member-update! db))
      (http/register-admin! app)
      (await
       (directory/with-bootstrap-contracts!
        contracts
        (^:async fn []
          (with-redefs [mongo/mongo-client* (atom (:client server)) mongo/mongo-db* (atom db)
                        runtime/current-policy-db (fn [] {:mongo? true})]
            (is (= 200 (:status (await (http/inject! app (patch-member {:actorId "existing-local-actor"}))))))
            (is (= ["existing-role"] (await (http/member-role-ids db))))
            (let [before (await (directory/directory-state db))]
              (let [actor (await (http/inject! app (patch-member {:actorId "foreign" :roleSlugs []})))
                    profile (await (http/inject! app (patch-member {:displayName "Unsupported" :roleSlugs []})))]
                (is (= 409 (:status actor)))
                (is (= "membership_actor_immutable" (get-in actor [:body :error_code])))
                (is (= 400 (:status profile)))
                (is (= "mongo_policy_profile_update_unsupported" (get-in profile [:body :error_code]))))
              (is (= before (await (directory/directory-state db)))))
            (is (= 200 (:status (await (http/inject! app (patch-member {:roleSlugs []}))))))
            (is (= [] (await (http/member-role-ids db))))))))
      (finally (await (http/close! app)) (await (native/close! server)) (files/remove! contracts)))))

(defn- redeem [code email]
  {:method "POST" :url "/api/auth/invite/redeem" :payload {:code code :email email}})

(deftest ^:async actual-legacy-invite-route-preserves-classified-native-admission-failures
  (let [server (await (native/open!)) db (:db server) app (http/app) calls (atom 0)]
    (try
      (http/register-redeem! app {:mongo? true})
      (doseq [code ["known-failure" "reserved" "foreign"]]
        (await (invites/seed! db code (invites/native-expiry))))
      (await (admission/reserve! db "reserved" "alice@example.test"))
      (with-redefs [connection/db! (fn [] db)
                    users/create-user! (fn [_ _ _ _]
                                         (swap! calls inc)
                                         (throw (ex-info "Provisioning failed" {:status 503 :code "provisioning_failed"})))]
        (is (= 400 (:status (await (http/inject! app (redeem "known-failure" ""))))))
        (is (= 403 (:status (await (http/inject! app (redeem "foreign" "other@example.test"))))))
        (is (= 409 (:status (await (http/inject! app (redeem "reserved" "alice@example.test"))))))
        (is (zero? @calls))
        (is (= 503 (:status (await (http/inject! app (redeem "known-failure" "alice@example.test"))))))
        (is (= 1 @calls))
        (is (= "pending" (:status (await (invites/row db "known-failure")))))
        (is (= "provisioning" (:status (await (invites/row db "reserved"))))))
      (finally (await (http/close! app)) (await (native/close! server))))))
