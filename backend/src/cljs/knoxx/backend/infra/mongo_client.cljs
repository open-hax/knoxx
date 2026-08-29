(ns knoxx.backend.infra.mongo-client
  "MongoDB client for Knoxx session/run persistence.
   Replaces Redis as the primary state store."
  (:require ["mongodb" :refer [MongoClient]]))

(defonce mongo-client* (atom nil))
(defonce mongo-db* (atom nil))
(defonce mongo-init-promise* (atom nil))

(defn- mongo-url
  "Resolve MongoDB connection string from environment."
  []
  (or (aget js/process.env "MONGODB_URI")
      (aget js/process.env "OPENPLANNER_MONGODB_URI")
      "mongodb://localhost:27017"))

(defn- mongo-db-name
  "Resolve MongoDB database name from environment."
  []
  (or (aget js/process.env "MONGODB_DB")
      (aget js/process.env "OPENPLANNER_MONGODB_DB")
      "openplanner"))

(defn ^:async connect-mongo!
  "Connect to MongoDB and cache client + db. Returns the Db instance."
  []
  (try
    (let [url (mongo-url)
          client (MongoClient. url #js {:serverSelectionTimeoutMS 5000})
          _ (await (.connect client))
          db (.db client (mongo-db-name))]
      (reset! mongo-client* client)
      (reset! mongo-db* db)
      (js/console.log "[mongo-client] Connected to MongoDB:" url "/" (mongo-db-name))
      db)
    (catch :default err
      (js/console.error "[mongo-client] FATAL: failed to connect to MongoDB at" (mongo-url))
      (js/console.error "[mongo-client] Error:" (.-message err))
      (when (.-stack err)
        (js/console.error "[mongo-client] Stack:" (.-stack err)))
      (reset! mongo-client* nil)
      (reset! mongo-db* nil)
      nil)
    (finally
      (reset! mongo-init-promise* nil))))

(defn ^:async init-mongo!
  "Initialize MongoDB connection if not already connected."
  []
  (cond
    @mongo-db*
    @mongo-db*

    @mongo-init-promise*
    (await @mongo-init-promise*)

    :else
    (let [connect-promise (connect-mongo!)]
      (reset! mongo-init-promise* connect-promise)
      (await connect-promise))))

(defn get-db
  "Get the current MongoDB Db instance, or nil if not connected."
  []
  @mongo-db*)

(defn get-client
  "Get the current MongoDB Client instance, or nil if not connected."
  []
  @mongo-client*)

(defn ^:async close-mongo!
  "Close MongoDB connection."
  []
  (when-let [client @mongo-client*]
    (try
      (await (.close client))
      (catch :default err
        (js/console.error "[mongo-client] Error closing connection:" err)))
    (reset! mongo-client* nil)
    (reset! mongo-db* nil)))
