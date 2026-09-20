(ns knoxx.backend.infra.stores.mongo-session-store
  "Compatibility conversation facade with explicit provider selection and disposable cache."
  (:require [knoxx.backend.domain.thread-store :as domain]
            [knoxx.backend.extern.thread-store :as clock]
            [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.infra.stores.mongo-thread-store :as mongo]
            [knoxx.backend.law.thread-store :as law]
            [knoxx.backend.shape.thread-store :as protocol]))

(def SESSION_TTL_SECONDS 3600)
(def STICKY_SESSION_TTL_SECONDS 86400)
(def COLLECTION_NAME "knoxx_threads")
(def ACTIVE_STATUS domain/active-statuses)
(defonce session-cache* (atom {}))
(defonce ^:private cache-owners (atom {}))
(defonce provider* (atom nil))

(defn available?
  "Report only an installed provider or an already initialized Mongo handle; never probe a service."
  []
  (boolean (or @provider* (mongo-client/get-db))))

(defn install!
  "Install a finite provider and discard cache authority from every prior provider."
  [provider]
  (when (and provider (not (satisfies? protocol/IThreadStore provider)))
    (throw (ex-info "Invalid thread provider" {:status 400 :code "thread_provider_invalid"})))
  (reset! session-cache* {})
  (reset! cache-owners {})
  (reset! provider* provider))

(defn- selected [db]
  (cond db {:provider (mongo/create-store db) :owner db}
        @provider* {:provider @provider* :owner @provider*}
        :else (if-let [handle (mongo-client/get-db)]
                {:provider (mongo/create-store handle) :owner handle}
                (throw (ex-info "Thread persistence is not initialized" {:status 503 :code "thread_provider_unavailable"})))))

(defn- remember! [owner id value]
  (when (and value (identical? owner (or @provider* (mongo-client/get-db))))
    (swap! cache-owners assoc id owner)
    (swap! session-cache* assoc id (assoc value :cached-at (clock/now-ms))))
  value)

(defn- forget! [owner id]
  (when (identical? owner (get @cache-owners id))
    (swap! cache-owners dissoc id)
    (swap! session-cache* dissoc id)))

(defn ^:async get-session
  "Read current provider state; stale heap state never overrides durable expiry or revocation."
  ([id] (await (get-session nil id)))
  ([db id]
   (let [{:keys [provider owner]} (selected db)
         value (await (protocol/read-thread provider id))]
     (if value (remember! owner id value) (do (forget! owner id) nil)))))

(defn get-session-sync
  "Best-effort synchronous view of only the installed provider's nonexpired cache."
  [id]
  (let [value (get @session-cache* id)
        owner (or @provider* (mongo-client/get-db))]
    (when (and (identical? owner (get @cache-owners id)) value
               (clock/cache-live? value (clock/now-ms) (law/ttl-ms id))) value)))

(defn ^:async get-conversation-active-session
  "Resolve a conversation through its selected durable provider."
  ([id] (await (get-conversation-active-session nil id)))
  ([db id] (:session_id (await (protocol/conversation-thread (:provider (selected db)) id)))))

(defn- ^:async mutate! [db id operation]
  (let [{:keys [provider owner]} (selected db)
        result (await (operation provider))]
    (if (map? result) (remember! owner id result) (forget! owner id))
    result))

(defn put-session!
  "Await durable admission before publishing any cache entry."
  ([session] (put-session! nil session))
  ([db session] (mutate! db (:session_id session) #(protocol/put-thread! % session))))

(defn update-session!
  "Merge under the selected provider's admission contract."
  ([id updates] (update-session! nil id updates))
  ([db id updates] (mutate! db id #(protocol/patch-thread! % id updates))))

(defn remove-session!
  "Delete durably before removing only this provider's cached copy."
  ([id conversation] (remove-session! nil id conversation))
  ([db id _conversation] (mutate! db id #(protocol/delete-thread! % id))))

(defn list-active-sessions
  "Read visible active conversations from the selected provider."
  ([] (list-active-sessions nil))
  ([db] (protocol/active-threads (:provider (selected db)))))

(defn ^:async list-active-session-ids
  "Read only the IDs of current active conversations."
  ([] (await (list-active-session-ids nil)))
  ([db] (mapv :session_id (await (list-active-sessions db)))))

(defn ^:async recover-sessions!
  "Rebuild this provider's cache from current durable state and return running conversations."
  ([] (await (recover-sessions! nil)))
  ([db]
   (let [{:keys [provider owner]} (selected db)
         sessions (await (protocol/active-threads provider))]
     (doseq [session sessions] (remember! owner (:session_id session) session))
     (filterv #(= "running" (:status %)) sessions))))

(defn mark-session-streaming!
  "Durably update stream state."
  ([id streaming?] (mark-session-streaming! nil id streaming?))
  ([db id streaming?] (update-session! db id {:has_active_stream streaming?})))

(defn complete-session!
  "Durably finalize one conversation."
  ([id conversation opts] (complete-session! nil id conversation opts))
  ([db id _conversation opts]
   (update-session! db id (merge {:status "completed" :has_active_stream false}
                                 (select-keys opts [:status :answer :error :messages])))))

(defn session-can-send? "Pure conversation send readiness." [session] (domain/session-can-send? session))
(defn rewind-messages "Pure transcript rewind." [messages turns] (domain/rewind-messages messages turns))

(defn undo-session-turns!
  "Rewind through a durable provider; an absent transcript produces no event."
  ([id turns] (undo-session-turns! nil id turns))
  ([db id turns] (mutate! db id #(protocol/rewind-thread! % id turns))))

(defn active-session-snapshots
  "Return current-provider cached active snapshots for diagnostics only."
  []
  (->> (keys @session-cache*) sort (keep get-session-sync)
       (filter #(ACTIVE_STATUS (:status %))) vec))

(defn setup-indexes! "Retain the explicit Mongo initialization entry point." [db] (mongo/setup-indexes! db))
