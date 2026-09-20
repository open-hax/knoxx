(ns knoxx.backend.infra.stores.mongo-thread-store
  "Explicit Mongo implementation of the finite conversation persistence port."
  (:require [knoxx.backend.domain.thread-store :as domain]
            [knoxx.backend.extern.mongo-thread :as native]
            [knoxx.backend.extern.thread-store :as clock]
            [knoxx.backend.infra.system-instance :as instance]
            [knoxx.backend.law.thread-store :as law]
            [knoxx.backend.shape.thread-store :as protocol]))

(defn- ^:async write-fields! [db fields]
  (try {:written (await (native/upsert-session! db fields))}
       (catch :default error
         (if (native/duplicate-key? error)
           (if (native/duplicate-conversation? error)
             (throw (ex-info "Conversation already has a thread"
                             {:status 409 :code "thread_store_conversation_conflict"}))
             {:retry? true})
           (throw error)))))

(defn- ^:async put! [db thread]
  (law/assert-valid! :thread/value law/Thread thread)
  (let [fields (assoc thread :system_instance_id (instance/current-id))
        id (:session_id fields)]
    (loop [attempt 0]
      (let [current (await (native/find-session db id))
            proposed (merge current fields)]
        (law/assert-valid! :thread/value law/Thread proposed)
        (domain/assert-identity! current proposed id)
        (let [{:keys [written retry?]} (await (write-fields! db fields))]
          (if-not retry?
            (law/assert-valid! :thread/value law/Thread written)
            (if (< attempt 31) (recur (inc attempt))
              (throw (ex-info "Thread changed repeatedly during identity admission"
                              {:status 503 :code "thread_store_contention"})))))))))

(defn- ^:async patch! [db id patch]
  ;; put! validates the merged view, then writes only these atomic partial fields.
  (law/assert-valid! :thread/patch law/DataMap patch)
  (await (put! db (assoc patch :session_id id :updated_at (clock/now-ms)))))

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
