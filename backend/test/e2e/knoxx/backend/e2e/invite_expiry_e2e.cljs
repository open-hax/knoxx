(ns knoxx.backend.e2e.invite-expiry-e2e
  "Exercise the native invite endpoints over HTTP against an owned Mongo process."
  (:require [clojure.string :as str]
            [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.mongo-run-native-fixture :as mongo-fixture]
            [knoxx.backend.infra.auth.session :as session]
            [knoxx.backend.infra.db.policy :as policy]
            [knoxx.backend.infra.http-server :as http]
            [knoxx.backend.infra.mongo-client :as mongo]
            [knoxx.backend.infra.routes.auth :as auth]
            [knoxx.backend.infra.stores.mongo-policy-invites :as invites]
            ["node:fs" :as fs]
            ["node:path" :as path]))

(def ^:private member-email "invite-member@example.invalid")

(defn- ^:async request!
  [origin endpoint payload cookie]
  (let [response (await (js/fetch (str origin "/api/auth/" endpoint)
                                  (clj->js {:method "POST"
                                            :headers (cond-> {"content-type" "application/json"}
                                                       cookie (assoc "cookie" cookie))
                                            :body (js/JSON.stringify (clj->js payload))})))]
    {:status (.-status response)
     :cookie (.get (.-headers response) "set-cookie")
     :body (js->clj (await (.json response)) :keywordize-keys true)}))

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
          success (first (filter #(= 200 (:status %)) attempts))]
      (is (= [200 400] (sort (map :status attempts))) "One concurrent redemption wins")
      (is (= "redeemed" (get-in success [:body :invite :status])))
      (is (= member-email (get-in success [:body :user :email])))
      (is (str/starts-with? (or (:cookie success) "") "knoxx_session=")
          "Knoxx issued the normal session cookie")
      (is (= 400 (:status (await (request! origin "invite/redeem" payload nil))))
          "A replay cannot mint another session"))))

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

(defn- ^:async prove-native-invites!
  []
  (let [{:keys [db directory] :as fixture} (await (mongo-fixture/open!))
        env js/process.env
        previous-contracts (aget env "CONTRACTS_DIR")
        contracts (path/join directory "contracts")
        app (http/create-app!)]
    (try
      (when (mongo/get-db)
        (throw (ex-info "Native invite proof requires an isolated test process" {})))
      (fs/mkdirSync contracts)
      (aset env "CONTRACTS_DIR" contracts)
      (reset! mongo/mongo-db* db)
      (await (http/register-default-plugins! app))
      (let [admin-cookie (await (prepare-admin! app))
            org (await (policy/find-org-by-slug nil "open-hax"))
            origin (await (http/listen! app "127.0.0.1" 0))]
        (await (prove-current-invite! origin db admin-cookie))
        (await (prove-stored-expiries! origin db (:id org))))
      (finally
        (await (http/close! app))
        (when (identical? (mongo/get-db) db)
          (reset! mongo/mongo-db* nil))
        (if (some? previous-contracts)
          (aset env "CONTRACTS_DIR" previous-contracts)
          (js-delete env "CONTRACTS_DIR"))
        (await (mongo-fixture/close! fixture))))))

(deftest ^:async native-invite-expiry-test
  (when (aget js/process.env "KNOXX_VERIFY_NATIVE_INVITES")
    (testing "Native invite authorization, expiry, session creation and replay"
      (await (prove-native-invites!)))))
