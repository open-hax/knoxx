(ns knoxx.backend.infra.stores.mongo-thread-store
  "Explicit Mongo implementation of the finite conversation persistence port."
  (:require [knoxx.backend.domain.thread-store :as domain]
            [knoxx.backend.extern.mongo-thread :as native]
            [knoxx.backend.extern.thread-store :as clock]
            [knoxx.backend.infra.system-instance :as instance]
            [knoxx.backend.law.thread-store :as law]
            [knoxx.backend.shape.thread-store :as protocol]))

(defn- ^:async put! [db thread]
  (law/assert-valid! :thread/value law/Thread thread)
  (let [thread (assoc thread :system_instance_id (instance/current-id))]
    (law/assert-valid! :thread/value law/Thread (await (native/upsert-session! db thread)))))

(defn- ^:async patch! [db id patch]
  ;; Validate the merged view, then write only supplied fields with atomic $set.
  ;; Writing the full snapshot would replay stale fields over a concurrent patch.
  (law/assert-valid! :thread/patch law/DataMap patch)
  (let [fields (assoc patch :session_id id :updated_at (clock/now-ms))
        current (await (native/find-session db id))]
    (law/assert-valid! :thread/value law/Thread (merge current fields))
    (await (put! db fields))))

(defn- ^:async rewind! [db id turns]
  (loop [attempt 0]
    (when-let [current (await (native/find-session db id))]
      (let [messages (domain/rewind-messages (:messages current) turns)
            fields {:messages messages :status "waiting_input" :has_active_stream false
                    :answer nil :error nil :updated_at (clock/now-ms)
                    :system_instance_id (instance/current-id)}]
        (if (= messages (vec (or (:messages current) []))) current
          (do
            (law/assert-valid! :thread/value law/Thread (merge current fields))
            (if-let [written (await (native/patch-if-messages! db id (:messages current) fields))]
              (law/assert-valid! :thread/value law/Thread written)
              (if (< attempt 31) (recur (inc attempt))
                (throw (ex-info "Thread changed repeatedly during rewind"
                                {:status 503 :code "thread_store_contention"}))))))))))

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
