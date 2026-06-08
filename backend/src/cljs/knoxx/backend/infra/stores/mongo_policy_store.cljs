(ns knoxx.backend.infra.stores.mongo-policy-store
  "Mongo-backed policy storage — slice 1 of the PG policy DB migration
   (kanban 14-04): auth sessions + singleton config (session secret).

   Response shapes mirror infra.db.policy exactly so the flag-dispatch
   layer there can route per OPENPLANNER_KNOXX_POLICY_STORE=mongo without
   touching any of the thirteen consumer namespaces.

   Documents store expires_at as a real js/Date so the TTL index reaps
   expired sessions server-side; the PG layer stored ISO strings."
  (:require
    [clojure.string :as str]
    [knoxx.backend.infra.auth.token-hash :as token-hash]
    [knoxx.backend.infra.mongo-client :as mongo-client]
    [knoxx.backend.infra.stores.mongo-policy-directory :as mongo-directory]
    [knoxx.backend.infra.stores.mongo-policy-roles :as mongo-roles]
    [knoxx.backend.infra.stores.mongo-policy-tools :as mongo-tools]
    [knoxx.backend.infra.stores.mongo-policy-actor-credentials :as mongo-actor-credentials]
    [knoxx.backend.infra.stores.mongo-policy-audit-events :as mongo-audit]
    [knoxx.backend.infra.stores.mongo-policy-data-lakes :as mongo-data-lakes]
    [knoxx.backend.infra.stores.mongo-policy-invites :as mongo-invites]
    [knoxx.backend.infra.stores.mongo-policy-studio :as mongo-studio]
    [knoxx.backend.infra.system-instance :as system-instance]))

(def SESSIONS_COLLECTION "knoxx_policy_sessions")
(def CONFIG_COLLECTION "knoxx_config")

(defn- session-ttl-seconds
  []
  (js/parseInt (or (aget js/process.env "KNOXX_SESSION_TTL_SECONDS") "86400") 10))

(defn- sessions-coll [db] (.collection db SESSIONS_COLLECTION))
(defn- config-coll [db] (.collection db CONFIG_COLLECTION))

(defn- session-row-response
  "Same public shape as infra.db.policy/session-row-response."
  [row]
  {:session {:id            (:session_id row)
             :user-id       (:user_id row)
             :membership-id (:membership_id row)
             :org-id        (:org_id row)
             :email         (:email row)
             :display-name  (:display_name row)
             :auth-provider (:auth_provider row)
             :expires-at    (:expires_at row)
             :created-at    (:created_at row)}})

(defn ^:async setup-indexes!
  "Create session lookup + TTL and config uniqueness indexes. Idempotent."
  [db]
  (let [sessions (sessions-coll db)
        config (config-coll db)]
    (await (.createIndex sessions #js {"session_id" 1} #js {"unique" true}))
    (await (.createIndex sessions #js {"token_prefix" 1}))
    (await (.createIndex sessions #js {"expires_at" 1} #js {"expireAfterSeconds" 0}))
    (await (.createIndex config #js {"key" 1} #js {"unique" true}))
    (await (mongo-directory/setup-indexes! db))
    (await (mongo-roles/setup-indexes! db))
    (await (mongo-tools/setup-indexes! db))
    (await (mongo-actor-credentials/setup-indexes! db))
    (await (mongo-audit/setup-indexes! db))
    (await (mongo-data-lakes/setup-indexes! db))
    (await (mongo-invites/setup-indexes! db))
    (await (mongo-studio/setup-state-indexes! db))
    (await (mongo-studio/setup-assets-indexes! db))
    true))

(defonce ^:private indexes-init-promise* (atom nil))

(defn ^:async ensure-indexes!
  "Single-flight setup-indexes! per process. Called from the policy-flag
   dispatch path so the TTL and uniqueness indexes exist even when the
   composite-store flag (which gates bootstrap's mongo path) is off."
  [db]
  (when db
    (when-not @indexes-init-promise*
      (reset! indexes-init-promise* (setup-indexes! db)))
    (try
      (await @indexes-init-promise*)
      (catch :default err
        (reset! indexes-init-promise* nil)
        (js/console.warn "[mongo-policy-store] index setup failed:" err)))))

(defn ^:async create-session!
  "Persist a new auth session. Mirrors infra.db.policy/create-session!."
  ([opts] (create-session! (mongo-client/get-db) opts))
  ([db {:keys [token user-id membership-id org-id email display-name
               auth-provider external-subject ip-address user-agent]}]
   (if (str/blank? token)
     (throw (js/Error. "token is required"))
     (let [salt (token-hash/generate-salt)
           now (js/Date.)
           doc {:session_id (str (random-uuid))
                :user_id user-id
                :membership_id membership-id
                :org_id org-id
                :token_hash (token-hash/hash-token token salt)
                :token_prefix (token-hash/token-prefix token)
                :salt salt
                :email email
                :display_name display-name
                :auth_provider (or auth-provider "github")
                :external_subject external-subject
                :ip_address ip-address
                :user_agent user-agent
                :system_instance_id (system-instance/current-id)
                :expires_at (js/Date. (+ (js/Date.now) (* (session-ttl-seconds) 1000)))
                :created_at now
                :last_seen_at now}]
       (await (.insertOne (sessions-coll db) (clj->js doc)))
       (session-row-response doc)))))

(defn ^:async touch-session-best-effort!
  "Update last_seen_at; failures are swallowed like the PG variant."
  ([session-id] (touch-session-best-effort! (mongo-client/get-db) session-id))
  ([db session-id]
   (try
     (await (.updateOne (sessions-coll db)
                        #js {"session_id" (str session-id)}
                        #js {"$set" #js {"last_seen_at" (js/Date.)}}))
     nil
     (catch :default _ nil))))

(defn- ^:async find-session-by-token
  "Find the candidate row whose salted hash matches the presented token."
  [db token]
  (let [cursor (.find (sessions-coll db)
                      #js {"token_prefix" (token-hash/token-prefix token)
                           "expires_at" #js {"$gt" (js/Date.)}})
        rows (js->clj (await (.toArray cursor)) :keywordize-keys true)]
    (first (filter #(= (:token_hash %) (token-hash/hash-token token (:salt %))) rows))))

(defn ^:async get-session-by-token!
  "Resolve a session by bearer token, touching last_seen_at on hit."
  ([token] (get-session-by-token! (mongo-client/get-db) token))
  ([db token]
   (when-not (str/blank? token)
     (try
       (when-let [row (await (find-session-by-token db token))]
         ;; Fire-and-forget like the PG path: don't tax every authenticated
         ;; request with the last_seen_at write.
         (touch-session-best-effort! db (:session_id row))
         (session-row-response row))
       (catch :default _ nil)))))

(defn ^:async delete-session-by-token!
  "Delete the session matching token; returns the deleted session response."
  ([token] (delete-session-by-token! (mongo-client/get-db) token))
  ([db token]
   (let [result (await (get-session-by-token! db token))]
     (when-let [sid (get-in result [:session :id])]
       (try
         (await (.deleteOne (sessions-coll db) #js {"session_id" (str sid)}))
         (catch :default _ nil)))
     result)))

(defn ^:async cleanup-expired-sessions!
  "Delete expired sessions. The TTL index already reaps server-side; this
   keeps API parity with the PG layer and returns the deleted count."
  ([] (cleanup-expired-sessions! (mongo-client/get-db)))
  ([db]
   (try
     (let [result (await (.deleteMany (sessions-coll db)
                                      #js {"expires_at" #js {"$lt" (js/Date.)}}))
           n (or (.-deletedCount result) 0)]
       (when (> n 0)
         (.log js/console "[mongo-policy-store] Cleaned up" n "expired sessions"))
       n)
     (catch :default _ 0))))

(defn ^:async get-config-value!
  "Read a singleton config value by key, or nil."
  ([key] (get-config-value! (mongo-client/get-db) key))
  ([db key]
   (let [row (await (.findOne (config-coll db) #js {"key" (str key)}))]
     (when row (aget row "value")))))

(defn ^:async set-config-value!
  "Upsert a singleton config value by key. Returns the value."
  ([key value] (set-config-value! (mongo-client/get-db) key value))
  ([db key value]
   (await (.updateOne (config-coll db)
                      #js {"key" (str key)}
                      #js {"$set" #js {"value" value "updated_at" (js/Date.)}}
                      #js {"upsert" true}))
   value))

(defn ^:async init-config-value!
  "Atomically set key to value only if absent ($setOnInsert), returning the
   stored (winning) value. First writer wins: concurrent initializers all
   observe the same value, unlike set-config-value! which clobbers."
  ([key value] (init-config-value! (mongo-client/get-db) key value))
  ([db key value]
   (let [result (await (.findOneAndUpdate
                        (config-coll db)
                        #js {"key" (str key)}
                        #js {"$setOnInsert" #js {"value" value "updated_at" (js/Date.)}}
                        #js {"upsert" true "returnDocument" "after"}))]
     (or (when result (aget result "value")) value))))

(defn ^:async recover-session-secret!
  "Load the session secret from knoxx_config, generating and persisting one
   if absent. An optional fallback-secret (e.g. read from the PG config
   table during cutover) is adopted before generating a fresh one, so
   existing cookies survive the storage migration. The write is
   first-writer-wins, so concurrent recovery converges on one secret."
  ([] (recover-session-secret! (mongo-client/get-db) nil))
  ([fallback-secret] (recover-session-secret! (mongo-client/get-db) fallback-secret))
  ([db fallback-secret]
   (if-let [stored (await (get-config-value! db "session_secret"))]
     (do
       (.log js/console "[mongo-policy-store] Recovered session secret from Mongo")
       stored)
     (let [adopted? (not (str/blank? (str (or fallback-secret ""))))
           candidate (if adopted? fallback-secret (token-hash/generate-secret))
           secret (await (init-config-value! db "session_secret" candidate))]
       (.log js/console
             (cond
               (not= secret candidate) "[mongo-policy-store] Session secret already initialized by a concurrent writer"
               adopted? "[mongo-policy-store] Adopted session secret from fallback store"
               :else "[mongo-policy-store] Generated and persisted session secret"))
       secret))))
