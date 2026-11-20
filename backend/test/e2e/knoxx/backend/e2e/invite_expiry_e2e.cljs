(ns knoxx.backend.e2e.invite-expiry-e2e
  "Exercise the native invite endpoints over HTTP against an owned Mongo process."
  (:require [clojure.string :as str]
            [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.fetch :as fetch]
            [knoxx.backend.extern.mongo-run-native-fixture :as mongo-fixture]
            [knoxx.backend.infra.auth.session :as session]
            [knoxx.backend.infra.db.policy :as policy]
            [knoxx.backend.infra.http-server :as http]
            [knoxx.backend.infra.mongo-client :as mongo]
            [knoxx.backend.infra.routes.app :as app-routes]
            [knoxx.backend.infra.routes.auth :as auth]
            [knoxx.backend.infra.stores.mongo-policy-invites :as invites]
            [knoxx.backend.runtime.state :as runtime-state]
            ["node:fs" :as fs]
            ["node:path" :as path]))

(def ^:private member-email "invite-member@example.invalid")

(defn- request!
  [origin endpoint payload cookie]
  (fetch/json! fetch/default-client
               {:url (str origin "/api/auth/" endpoint)
                :method "POST"
                :headers (when cookie {"cookie" cookie})
                :json payload}))

(defn- ^:async prepare-admin!
  [app]
  (let [org (await (policy/ensure-primary-org! nil {}))]
    (doseq [role ["system-admin" "basic-user"]]
      (await (policy/ensure-role! nil {:org-id (:id org) :slug role
                                       :name role :scope-kind "org"})))
    (let [bootstrap (await (policy/ensure-bootstrap-user! nil org {}))
          admin (await (policy/resolve-request-context!
                        nil {"x-knoxx-membership-id" (get-in bootstrap [:membership :id])}))
          cookie (atom nil)
          reply #js {:setCookie (fn [name value _options]
                                  (reset! cookie (str name "=" (js/encodeURIComponent value))))}]
      (auth/register-auth-routes app {:policy-context {:mongo? true}})
      (await (session/set-db-session-store! {:mongo? true}))
      (await (session/create-session-from-context! reply "http://127.0.0.1" admin {}))
      @cookie)))

(defn- ^:async prove-member-session!
  [origin success]
  (let [cookie (first (str/split (get-in success [:headers "set-cookie"] "") #";" 2))
        url (str origin "/api/auth/context")
        anon (await (fetch/json! fetch/default-client {:url url}))
        {:keys [status body]} (await (fetch/json! fetch/default-client
                                                 {:url url :headers {"cookie" cookie}}))]
    (is (= 401 (:status anon)))
    (is (= 200 status) "The issued cookie authenticates a subsequent request")
    (is (= member-email (get-in body [:user :email])))
    (is (= (get-in success [:body :membership :id]) (get-in body [:membership :id])))
    (is (= (get-in success [:body :org :id]) (get-in body [:org :id])))
    (is (= "active" (get-in body [:membership :status])))
    (is (= ["basic-user"] (:roleSlugs body)))
    (is (false? (:isSystemAdmin body)))))

(defn- ^:async prove-current-invite!
  [origin db admin-cookie]
  (let [create-payload {:email member-email :send-email false}
        refused (await (request! origin "invite" create-payload nil))
        created (await (request! origin "invite" create-payload admin-cookie))
        code (get-in created [:body :invite :code])
        payload {:code code :email member-email}]
    (is (= 401 (:status refused)) "Invite creation requires a session")
    (is (= 200 (:status created))
        (str "The normal invite endpoint issued an invite: " (get-in created [:body :error])))
    (is (string? (:expires_at (await (invites/pending-by-code! db code))))
        "The current writer's ISO-string expiry is usable")
    (is (= 403 (:status (await (request! origin "invite/redeem"
                                        (assoc payload :email "other@example.invalid") nil))))
        "An email mismatch cannot consume the invite")
    (let [first-redeem (request! origin "invite/redeem" payload nil)
          second-redeem (request! origin "invite/redeem" payload nil)
          attempts [(await first-redeem) (await second-redeem)]
          success (first (filter #(= 200 (:status %)) attempts))
          refused (first (filter #(= 400 (:status %)) attempts))
          replay (await (request! origin "invite/redeem" payload nil))]
      (is (= [200 400] (sort (map :status attempts))) "One concurrent redemption wins")
      (is (= "redeemed" (get-in success [:body :invite :status])))
      (is (= member-email (get-in success [:body :user :email])))
      (is (str/starts-with? (get-in success [:headers "set-cookie"] "") "knoxx_session=")
          "Knoxx issued the normal session cookie")
      (is (nil? (get-in refused [:headers "set-cookie"])))
      (is (= 400 (:status replay)) "A replay cannot mint another session")
      (is (nil? (get-in replay [:headers "set-cookie"])))
      (await (prove-member-session! origin success)))))

(defn- ^:async prove-stored-expiries!
  [origin db org-id]
  (let [future (js/Date. (+ (js/Date.now) 86400000))
        past (js/Date. (- (js/Date.now) 86400000))]
    (doseq [[code expiry status] [["future-date" future 200]
                                 ["expired-date" past 400]
                                 ["expired-string" (.toISOString past) 400]
                                 ["malformed" "invalid expiry" 400]
                                 ["null" nil 400]
                                 ["missing" nil 400]
                                 ["number" (.getTime future) 400]]]
      (let [email (str code "@example.invalid")]
        (await (invites/insert-invite! db {:org-id org-id :code code :email email
                                           :expires-at expiry}))
        (when (= code "missing")
          (await (.updateOne (.collection db invites/INVITES_COLLECTION)
                             #js {:code code} #js {"$unset" #js {"expires_at" ""}})))
        (is (= status (:status (await (request! origin "invite/redeem" {:code code :email email} nil))))
            (str "Stored expiry case: " code))))))

(defn- ^:async close-native-fixture!
  [app {:keys [db] :as fixture} previous-contracts previous-policy]
  (try
    (await (http/close! app))
    (finally
      (reset! runtime-state/policy-context* previous-policy)
      (when (identical? (mongo/get-db) db)
        (await (session/set-db-session-store! previous-policy))
        (reset! mongo/mongo-db* nil))
      (if (some? previous-contracts)
        (aset js/process.env "CONTRACTS_DIR" previous-contracts)
        (js-delete js/process.env "CONTRACTS_DIR"))
      (await (mongo-fixture/close! fixture)))))

(defn- ^:async prove-native-invites!
  []
  (let [{:keys [db directory] :as fixture} (await (mongo-fixture/open!))
        env js/process.env
        previous-contracts (aget env "CONTRACTS_DIR")
        previous-policy @runtime-state/policy-context*
        contracts (path/join directory "contracts")
        app (http/create-app!)]
    (try
      (when (mongo/get-db)
        (throw (ex-info "Native invite proof requires an isolated test process" {})))
      (fs/mkdirSync contracts)
      (aset env "CONTRACTS_DIR" contracts)
      (reset! mongo/mongo-db* db)
      (reset! runtime-state/policy-context* {:mongo? true})
      (await (http/register-default-plugins! app))
      (app-routes/api-auth-context! app nil nil app-routes/deps)
      (let [admin-cookie (await (prepare-admin! app))
            org (await (policy/find-org-by-slug nil "open-hax"))
            origin (await (http/listen! app "127.0.0.1" 0))]
        (await (prove-current-invite! origin db admin-cookie))
        (await (prove-stored-expiries! origin db (:id org))))
      (finally
        (await (close-native-fixture! app fixture previous-contracts previous-policy))))))

(deftest ^:async native-invite-expiry-test
  (when (aget js/process.env "KNOXX_VERIFY_NATIVE_INVITES")
    (testing "Native invite authorization, expiry, session creation and replay"
      (await (prove-native-invites!)))))
