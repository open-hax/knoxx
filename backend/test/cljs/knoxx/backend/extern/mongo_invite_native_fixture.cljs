(ns knoxx.backend.extern.mongo-invite-native-fixture
  "Own the native Mongo and HTTP handles used by the invitation proof."
  (:require [knoxx.backend.extern.mongo-run-native-fixture :as mongo-fixture]
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

(defn enabled?
  "Select the native fixture when requested by the verifier."
  []
  (= "1" (aget js/process.env "KNOXX_VERIFY_NATIVE_INVITES")))

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

(defn ^:async with-fixture!
  "Exercise the native routes with an ephemeral origin, org, and administrator session."
  [exercise!]
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
        (await (exercise! {:origin origin :db db :org-id (:id org) :admin-cookie admin-cookie})))
      (finally
        (await (close-native-fixture! app fixture previous-contracts previous-policy))))))

(defn ^:async stored-iso-expiry?
  "Check the storage type of an invite issued by the normal writer."
  [{:keys [db]} code]
  (string? (:expires_at (await (invites/pending-by-code! db code)))))

(defn ^:async seed-invite!
  "Seed an expiry case in the owned fixture database."
  [{:keys [db org-id]} {:keys [code email expiry]}]
  (let [future (js/Date. (+ (js/Date.now) 86400000))
        past (js/Date. (- (js/Date.now) 86400000))
        value (case expiry
                :future-date future
                :expired-date past
                :expired-string (.toISOString past)
                :invalid "invalid expiry"
                :null nil
                :missing nil
                :number (.getTime future))]
    (await (invites/insert-invite! db {:org-id org-id :code code :email email :expires-at value}))
    (when (= :missing expiry)
      (await (.updateOne (.collection db invites/INVITES_COLLECTION)
                         #js {:code code} #js {"$unset" #js {"expires_at" ""}})))))
