(ns knoxx.backend.extern.mongo-run-store
  "Native Mongo run document boundary. EDN preserves namespaced event values exactly."
  (:require [cljs.tools.reader.edn :as reader]
            [cljs.tools.reader.reader-types :as readers]
            [knoxx.backend.law.run-directory :as directory-law]
            [knoxx.backend.law.run-store :as law]))

(def collection-name "knoxx_runs")

(defn- read-snapshot [encoded]
  (try
    (law/require! :string encoded)
    (let [input (readers/string-push-back-reader encoded)
          eof (js-obj)
          snapshot (reader/read {:eof eof} input)]
      (when (or (identical? eof snapshot)
                (not (identical? eof (reader/read {:eof eof} input))))
        (throw (ex-info "Expected exactly one run snapshot" {})))
      snapshot)
    (catch :default _
      (throw (ex-info "Invalid durable Mongo run state"
                      {:status 503 :code "run_store_corrupt"})))))

(defn- decode [native]
  (when native
    (let [doc (js->clj native :keywordize-keys true)]
      (if (contains? doc :run_state_edn)
        (do (law/require! [:int {:min 1}] (:persistence_revision doc))
            {:revision (:persistence_revision doc)
             :state (read-snapshot (:run_state_edn doc))})
        {:revision 0
         :expires-ms (some-> (:expiresAt doc) .getTime)
         :legacy-run (dissoc doc :_id :expiresAt :createdAt :updatedAt)}))))

(defn ^:async read!
  "Read a single canonical snapshot without hidden cache or fallback."
  [db run-id]
  (law/require! law/NonBlank run-id)
  (decode (await (.findOne (.collection db collection-name) #js {:run_id run-id}))))

(defn- document [run-id revision state]
  (let [binding (get-in state [:bindings run-id])
        entry (get-in state [:runs run-id])]
    (clj->js {:run_id run-id :session_id (:session_id binding)
              :status (or (get-in entry [:run :status]) "deleted")
              :org_id (:org_id binding) :user_id (:user_id binding)
              :persistence_revision revision :run_state_edn (pr-str state)
              :run_expires_ms (:expires-ms entry)})))

(defn ^:async compare-and-swap!
  "Atomically install one whole run state. Duplicate creation and stale revisions return false.
   No TTL Date is stored: expiry hides views while retaining identity and event history."
  [db run-id previous state]
  (let [collection (.collection db collection-name)
        revision (or (:revision previous) 0)
        next-document (document run-id (inc revision) state)]
    (if (nil? previous)
      (try (await (.insertOne collection next-document
                                 #js {:writeConcern #js {:w "majority" :j true}})) true
           (catch :default cause (if (= 11000 (.-code cause)) false (throw cause))))
      (let [query #js {:run_id run-id :persistence_revision
                      (if (zero? revision) #js {"$exists" false} revision)}
            result (await (.replaceOne collection query next-document
                                        #js {:writeConcern #js {:w "majority" :j true}}))]
        (= 1 (.-matchedCount result))))))

(defn ^:async active!
  "Read selected session snapshots; the domain rechecks wall-clock expiry."
  [db session-id]
  (law/require! law/NonBlank session-id)
  (let [cursor (.find (.collection db collection-name)
                       #js {:session_id session-id
                            :status #js {"$in" #js ["running" "queued" "waiting_input"]}})]
    (mapv (fn [native] {:run-id (.-run_id native) :record (decode native)})
          (array-seq (await (.toArray cursor))))))

(defn ^:async directory!
  "Read exact scoped snapshots; never use approximate search for run authority."
  [db scope]
  (directory-law/scope! scope)
  (let [query (if (:all? scope) #js {} #js {:org_id (:org-id scope)})
        cursor (.find (.collection db collection-name) query)]
    (mapv (fn [native] {:run-id (.-run_id native) :record (decode native)})
          (array-seq (await (.toArray cursor))))))

(defn ^:async setup-indexes!
  "Require unique run identity and indexed active-session reads before selection."
  [db]
  (let [collection (.collection db collection-name)]
    (await (.createIndex collection #js {:run_id 1} #js {:unique true}))
    (await (.createIndex collection #js {:session_id 1 :status 1}))
    (await (.createIndex collection #js {:org_id 1}))))
