(ns knoxx.backend.infra.db.policy.credentials
  "Explicit credential provider dispatch and local password record lookup."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.local-policy-api :as local-api]
            [knoxx.backend.infra.stores.mongo-policy-directory :as mongo-directory]
            [knoxx.backend.infra.stores.mongo-policy-actor-credentials :as mongo-actor-creds]
            [knoxx.backend.law.bootstrap-credentials :as bootstrap-law]
            [knoxx.backend.law.policy-values :as values]
            [knoxx.backend.infra.db.policy.connection :as policy-connection]))

(defn ^:async list-actor-credentials!
  "Return active actor credential rows for provider as {:credentials [...]}."
  [pool provider]
  (if (local-api/selected? pool)
    (local-api/credentials! pool provider)
    (do
      (when (str/blank? provider)
    (throw (js/Error. "provider is required")))
  (if-let [db (await (policy-connection/ensure-mongo-policy-db!))]
    {:credentials (mapv mongo-actor-creds/credential-row->response
                        (await (mongo-actor-creds/list-actor-credentials-by-provider! db provider)))}
    {:credentials []}))))

(defn- secret-json->clj
  [value]
  (cond
    (nil? value) {}
    (map? value) value
    (string? value) (js->clj (js/JSON.parse value) :keywordize-keys true)
    :else (js->clj value :keywordize-keys true)))

(defn- password-record [row credential]
  {:user-id (:user_id row)
                  :email (:email row)
                  :display-name (:display_name row)
                  :membership-id (:id row)
                  :org-id (:org_id row)
                  :org-slug (:org_slug row)
                  :actor-id (:actor_id row)
                  :secret-json (secret-json->clj (:secret_json credential))})

(defn ^:async local-password-auth-record!
  "Resolve active Mongo password credentials. Marked bootstrap credentials keep
   their original organization; ordinary users retain default-first selection."
  ([_pool email]
   (local-password-auth-record!
    _pool email
    {:get-db! policy-connection/db!
     :find-user! mongo-directory/find-user-by-email!
     :find-membership! mongo-directory/find-membership-row-by-email-and-org!
     :find-bootstrap-credential! mongo-actor-creds/find-active-bootstrap-local-password!
     :get-membership-credential! mongo-actor-creds/get-credential-by-user-org-provider-kind!}))
  ([_pool email {:keys [get-db! find-user! find-membership!
                        find-bootstrap-credential! get-membership-credential!]}]
   (when-let [normalized (values/normalize-email email)]
     (let [db (await (get-db!))
           user (await (find-user! db normalized))]
       (when (and user (= "active" (:status user)))
         (let [bootstrap-credential
               (await (find-bootstrap-credential! db (:id user) normalized))
               membership-query (bootstrap-law/local-password-membership-query
                                 normalized bootstrap-credential)]
           (when-let [row (await (find-membership! db membership-query))]
             (when (and (= "active" (:user_status row)) (= "active" (:status row)))
               (let [credential (or bootstrap-credential
                                    (await (get-membership-credential!
                                            db (:user_id row) (:org_id row)
                                            "local" "password")))]
                 (password-record row credential))))))))))

(defn- actor-credential-response [row]
  {:credential (when row
                 (let [secret (js->clj (or (:secret_json row) {}) :keywordize-keys true)]
                   {:id                 (:id row)
                    :userId             (:user_id row)
                    :orgId              (:org_id row)
                    :provider           (:provider row)
                    :kind               (:kind row)
                    :accountIdentifier  (:account_identifier row)
                    :status             (:status row)
                    :secretJson         secret
                    :configuredFields   (vec (remove str/blank? (map name (keys secret))))}))})

(defn ^:async upsert-actor-credential!
  "Upsert an actor credential by user-id + org-id + provider."
  [pool _uid _mid user-id {:keys [org-id provider kind account-identifier secret-json status] :as payload}]
  (if (local-api/selected? pool)
    (local-api/command! pool :policy/upsert-credential [user-id payload])
    (if-let [db (await (policy-connection/ensure-mongo-policy-db!))]
    (let [row (await (mongo-actor-creds/upsert-actor-credential!
                      db user-id org-id provider
                      {:kind kind
                       :account-identifier account-identifier
                       :secret-json (js->clj (or secret-json {}) :keywordize-keys true)
                       :status status}))]
      (actor-credential-response row))
    (throw (js/Error. "Mongo policy store unavailable")))))

(defn ^:async get-actor-credential!
  "An actor's active credential for a provider.

   scope narrows the membership lookup: {:org-id, :membership-id}. actor_id is
   unique nowhere — not even within an org — so an unnarrowed lookup refuses an
   ambiguous actor rather than returning some member's secret. A caller holding a
   request context should pass its membership id, which is exact.

   Dispatches through the context's :get-actor-credential! when it carries one,
   the same seam :resolve-context! and :query! already use. This is the query
   seam mongo-policy-actor-credentials names as the correct dispatch point, and
   it is what lets a harness supply credentials without a database: until it
   existed, every credential-backed tool was unreachable from a test, which is
   most of the Discord and Bluesky surface."
  ([policy-context actor-id provider] (get-actor-credential! policy-context actor-id provider nil))
  ([policy-context actor-id provider scope]
   (if (local-api/selected? policy-context)
     (await (local-api/credential! policy-context actor-id provider scope))
     (if-let [f (:get-actor-credential! policy-context)]
       (await (f actor-id provider scope))
     (when-let [db (await (policy-connection/ensure-mongo-policy-db!))]
       {:credential (mongo-actor-creds/credential-row->response
                     (await (mongo-actor-creds/get-actor-credential-by-actor-and-provider!
                             db actor-id provider scope)))})))))
