(ns knoxx.backend.infra.stores.mongo-thread-store
  "Explicit Mongo implementation of the finite conversation persistence port."
  (:require [knoxx.backend.domain.thread-store :as domain]
            [knoxx.backend.extern.mongo-thread :as native]
            [knoxx.backend.infra.system-instance :as instance]
            [knoxx.backend.law.thread-store :as law]
            [knoxx.backend.shape.thread-store :as protocol]))

(defn- ^:async put! [db thread]
  (law/assert-valid! :thread/value law/Thread thread)
  (let [thread (assoc thread :system_instance_id (instance/current-id))]
    (await (native/upsert-session! db thread))
    thread))

(defn- ^:async patch! [db id patch]
  (let [current (await (native/find-session db id))]
    (await (put! db (merge current patch {:session_id id})))))

(defn- ^:async rewind! [db id turns]
  (when-let [current (await (native/find-session db id))]
    (let [messages (domain/rewind-messages (:messages current) turns)]
      (if (= messages (vec (or (:messages current) []))) current
        (await (put! db (assoc current :messages messages :status "waiting_input"
                                :has_active_stream false :answer nil :error nil)))))))

(defrecord MongoThreadStore [db]
  protocol/IThreadStore
  (read-thread [_ id] (native/find-session db id))
  (conversation-thread [_ id] (native/find-session-by-conversation db id))
  (put-thread! [_ thread] (put! db thread))
  (patch-thread! [_ id patch] (patch! db id patch))
  (rewind-thread! [_ id turns] (rewind! db id turns))
  (delete-thread! [_ id] (native/delete-session! db id))
  (active-threads [_] (native/fetch-active-sessions db)))

(defn create-store "Wrap an explicitly initialized Mongo handle." [db]
  (when-not db (throw (ex-info "Mongo thread handle is required" {:status 503})))
  (->MongoThreadStore db))

(defn setup-indexes! "Create Mongo's declared conversation indexes." [db] (native/setup-indexes! db))
