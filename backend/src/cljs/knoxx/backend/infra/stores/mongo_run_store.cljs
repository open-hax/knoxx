(ns knoxx.backend.infra.stores.mongo-run-store
  "MongoDB-backed Knoxx run store.
   Replaces Redis run storage with a TTL-indexed Mongo collection."
  (:require
    [knoxx.backend.shape.session-persistence :refer [ISessionStore assert-run!]]
    [knoxx.backend.domain.time :as time]
    [knoxx.backend.infra.system-instance :as system-instance]))

;; ── Constants ─────────────────────────────────────────────────────────

(def COLLECTION_NAME "knoxx_runs")
(def DEFAULT_RUN_TTL_SECONDS (* 2 60 60)) ; 2 hours
(def MAX_RUN_EVENTS 1000)
(def ACTIVE_STATUSES #{"running" "queued" "waiting_input"})

;; ── MongoDB Helpers ───────────────────────────────────────────────────

(defn- ^:async find-run [db run-id]
  (let [coll (.collection db COLLECTION_NAME)
        result (await (.findOne coll #js {"run_id" run-id}))]
    (when result (js->clj result :keywordize-keys true))))

(defn- ^:async insert-run! [db run]
  (let [coll (.collection db COLLECTION_NAME)
        doc (assoc run
                   :run_events []
                   :system_instance_id (system-instance/current-id)
                   :expiresAt (js/Date. (+ (.now js/Date) (* DEFAULT_RUN_TTL_SECONDS 1000)))
                   :createdAt (js/Date.)
                   :updatedAt (js/Date.))]
    (await (.insertOne coll (clj->js doc)))
    doc))

(defn- ^:async update-run! [db run-id patch]
  (let [coll (.collection db COLLECTION_NAME)
        ;; Re-stamp on every write: ownership tracks the last live writer,
        ;; so a run adopted by a resuming instance becomes owned by it.
        set-doc (assoc patch
                       :system_instance_id (system-instance/current-id)
                       :updatedAt (js/Date.)
                       :expiresAt (js/Date. (+ (.now js/Date) (* DEFAULT_RUN_TTL_SECONDS 1000))))]
    (await (.findOneAndUpdate
             coll
             #js {"run_id" run-id}
             #js {"$set" (clj->js set-doc)}
             #js {"returnDocument" "after"}))))

(defn- ^:async delete-run-doc! [db run-id]
  (let [coll (.collection db COLLECTION_NAME)]
    (await (.deleteOne coll #js {"run_id" run-id}))
    true))

(defn- ^:async list-runs-for-session [db session-id]
  (let [coll (.collection db COLLECTION_NAME)
        cursor (.find coll #js {"session_id" session-id
                               "status" #js {"$in" (clj->js (vec ACTIVE_STATUSES))}})
        results (await (.toArray cursor))]
    (js->clj results :keywordize-keys true)))

;; ── Public API ────────────────────────────────────────────────────────

(defn ^:async append-run-event!
  "Append a run event to the run_events array. Keeps max 1000 events."
  [db run-id event]
  (let [coll (.collection db COLLECTION_NAME)
        event-with-ts (assoc event :at (or (:at event) (time/now-iso)))]
    (await (.findOneAndUpdate
             coll
             #js {"run_id" run-id}
             #js {"$push" #js {"run_events" #js {"$each" (clj->js [event-with-ts])
                                                "$slice" (- MAX_RUN_EVENTS)}}
                  "$set" #js {"updatedAt" (js/Date.)
                              "expiresAt" (js/Date. (+ (.now js/Date) (* DEFAULT_RUN_TTL_SECONDS 1000)))}}
             #js {"returnDocument" "after"}))))

(defn ^:async setup-indexes!
  "Create required indexes on knoxx_runs collection."
  [db]
  (let [coll (.collection db COLLECTION_NAME)]
    (await (.createIndex coll #js {"run_id" 1} #js {"unique" true}))
    (await (.createIndex coll #js {"session_id" 1}))
    (await (.createIndex coll #js {"status" 1}))
    (await (.createIndex coll #js {"expiresAt" 1} #js {"expireAfterSeconds" 0}))))

;; ── ISessionStore Implementation ──────────────────────────────────────

(defn- ^:async put-run-impl! [db run]
  (assert-run! run "MongoRunStore/put-run!")
  (let [run-id (:run_id run)
        existing (await (find-run db run-id))]
    (if existing
      (js->clj (await (update-run! db run-id run)) :keywordize-keys true)
      (js->clj (await (insert-run! db run)) :keywordize-keys true))))

(defn- ^:async patch-run-impl! [db run-id patch]
  (let [current (await (find-run db run-id))]
    (when-not current
      (throw (ex-info "patch-run! on unknown run"
                      {:run-id run-id :patch-keys (keys patch)})))
    (let [updated (merge current patch {:updated_at (time/now-iso)})]
      (await (put-run-impl! db updated)))))

(defrecord MongoRunStore [db]
  ISessionStore

  (put-run! [_ run]
    (put-run-impl! db run))

  (get-run [_ run-id]
    (find-run db run-id))

  (patch-run! [_ run-id patch]
    (patch-run-impl! db run-id patch))

  (list-active-runs [_ session-id]
    (list-runs-for-session db session-id))

  (complete-run! [_ run-id opts]
    (patch-run-impl! db run-id
                     (merge {:status "completed"
                             :has_active_stream false
                             :updated_at (time/now-iso)}
                            (select-keys opts [:status :answer :error
                                               :trace_blocks :messages]))))

  (delete-run! [_ run-id]
    (delete-run-doc! db run-id)))

(defn create-mongo-run-store
  "Factory for MongoRunStore."
  [db]
  (->MongoRunStore db))
