(ns knoxx.backend.infra.db.policy.sessions
  "Legacy Mongo session persistence operations."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.stores.mongo-policy-store :as mongo-policy]
            [knoxx.backend.infra.db.policy.connection :as policy-connection]))

(defn ^:async touch-session-best-effort!
  [_pool session-id]
  (await (mongo-policy/touch-session-best-effort! session-id)))

(defn ^:async create-session!
  [_pool {:keys [token] :as opts}]
  (if (str/blank? token)
    (throw (js/Error. "token is required"))
    (await (mongo-policy/create-session! (await (policy-connection/db!)) opts))))

(defn ^:async get-session-by-token!
  [_pool token]
  (when-let [db (await (policy-connection/ensure-mongo-policy-db!))]
    (await (mongo-policy/get-session-by-token! db token))))

(defn ^:async delete-session-by-token!
  [_pool token]
  (when-let [db (await (policy-connection/ensure-mongo-policy-db!))]
    (await (mongo-policy/delete-session-by-token! db token))))

(defn ^:async cleanup-expired-sessions!
  [_pool]
  (if-let [db (await (policy-connection/ensure-mongo-policy-db!))]
    (await (mongo-policy/cleanup-expired-sessions! db))
    0))

(defn ^:async recover-session-secret!
  "Load the session secret from the Mongo config collection, generating and
   persisting one if absent. Returns Promise<string>."
  [_pool]
  (if-let [db (await (policy-connection/ensure-mongo-policy-db!))]
    (await (mongo-policy/recover-session-secret! db nil))
    (throw (js/Error. "Mongo policy store unavailable"))))
