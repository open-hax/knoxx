(ns knoxx.backend.infra.stores.mongo-thread-store
  "Explicit Mongo implementation of the finite conversation persistence port."
  (:require [knoxx.backend.domain.thread-store :as domain]
            [knoxx.backend.domain.thread-recovery :as recovery-domain]
            [knoxx.backend.extern.mongo-thread :as native]
            [knoxx.backend.extern.thread-store :as clock]
            [knoxx.backend.infra.system-instance :as instance]
            [knoxx.backend.law.thread-store :as law]
            [knoxx.backend.domain.startup-admission :as startup-domain]
            [knoxx.backend.shape.thread-recovery :as recovery]
            [knoxx.backend.shape.thread-store :as protocol]
            [knoxx.backend.shape.startup-admission :as startup]))

(defn- ^:async write-fields! [db fields observed]
  (try {:written (await (native/upsert-session! db fields observed))}
       (catch :default error
         (if (native/duplicate-key? error)
           (do
             (when (native/duplicate-conversation? error)
               (when-let [bound (await (native/find-session-by-conversation
                                       db (or (:conversation_id fields) (:conversation_id observed))))]
                 (when-not (= (:session_id fields) (:session_id bound))
                   (throw (ex-info "Conversation already has a thread"
                                   {:status 409 :code "thread_store_conversation_conflict"})))))
             {:retry? true})
           (throw error)))))

(defn- ^:async put! [db thread]
  (law/assert-valid! :thread/value law/Thread thread)
  (let [fields (assoc thread :system_instance_id (instance/current-id))
        id (:session_id fields)]
    (loop [attempt 0 initial nil]
      (let [current (await (native/find-session db id))
            origin (if (zero? attempt) current initial)
            proposed (merge current fields)]
        (domain/assert-identity! origin (assoc current :session_id id) id)
        (law/assert-valid! :thread/value law/Thread proposed)
        (domain/assert-identity! current proposed id)
        (let [{:keys [written retry?]} (await (write-fields! db fields current))]
          (if-not retry?
            (law/assert-valid! :thread/value law/Thread written)
            (if (< attempt 31) (recur (inc attempt) origin)
              (throw (ex-info "Thread changed repeatedly during identity admission"
                              {:status 503 :code "thread_store_contention"})))))))))

(defn- ^:async patch! [db id patch]
  ;; put! validates the merged view, then writes only these atomic partial fields.
  (law/assert-valid! :thread/patch law/DataMap patch)
  (await (put! db (assoc patch :session_id id :updated_at (clock/now-ms)))))

(defn- ^:async rewind! [db id turns]
  (loop [attempt 0 initial nil]
    (when-let [current (await (native/find-session db id))]
      (let [origin (if (zero? attempt) current initial)
            messages (domain/rewind-messages (:messages current) turns)
            fields {:messages messages :status "waiting_input" :has_active_stream false
                    :answer nil :error nil :updated_at (clock/now-ms)
                    :system_instance_id (instance/current-id)}]
        ;; Both directions preserve the rewind's exact identity, even when the
        ;; first observation was unbound and a concurrent writer assigns it.
        (domain/assert-identity! origin current id)
        (domain/assert-identity! current origin id)
        (if (= messages (vec (or (:messages current) []))) current
          (do
            (law/assert-valid! :thread/value law/Thread (merge current fields))
            (if-let [written (await (native/patch-if-messages! db current fields))]
              (law/assert-valid! :thread/value law/Thread written)
              (if (< attempt 31) (recur (inc attempt) origin)
                (throw (ex-info "Thread changed repeatedly during rewind"
                                {:status 503 :code "thread_store_contention"}))))))))))

(defn- ^:async startup! [db phase record expected]
  (loop [attempt 0]
    (let [view (await (native/startup-view! db (:session_id record)))
          current (native/startup-value view)
          proposed (startup-domain/decide :thread phase current (some? current)
                                       (native/same-startup-view? view expected) record)]
      (if-not proposed {:settled? false :reason :superseded}
        (do
          (domain/assert-identity! current proposed (:session_id record))
          (law/assert-valid! :thread/value law/Thread proposed)
          (if-let [written (await (native/startup-cas! db view
                                  (assoc proposed :system_instance_id (instance/current-id))))]
            written
            (if (< attempt 31) (recur (inc attempt))
              (throw (ex-info "Thread changed repeatedly during startup settlement"
                              {:status 503 :code "thread_store_contention"})))))))))

(defn- ^:async release-recovery! [db observed]
  (if-let [expected (get (meta observed) recovery/view-key)]
    (let [id (:session_id observed)
          current (native/startup-value expected)
          stamp (clock/stamp id (clock/now-ms) (instance/current-id))
          proposed (recovery-domain/release current observed true stamp)]
      (or (await (native/startup-cas! db expected (assoc proposed :system_instance_id (instance/current-id))))
          (throw (ex-info "Thread changed during recovery release"
                          {:status 409 :code "thread_recovery_conflict"}))))
    (throw (ex-info "Recovery requires an original provider read receipt"
                    {:status 409 :code "thread_recovery_conflict"}))))

(defrecord MongoThreadStore [db]
  startup/IStartupAdmission
  (startup-view [_ id] (native/startup-view! db id))
  (claim-startup! [_ record view] (startup! db :claim record view))
  (settle-startup! [_ record view] (startup! db :settle record view))
  recovery/IThreadRecovery
  (release-recovery! [_ observed] (release-recovery! db observed))
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
