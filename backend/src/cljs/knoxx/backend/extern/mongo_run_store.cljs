(ns knoxx.backend.extern.mongo-run-store
  "Native Mongo run document boundary. EDN preserves namespaced event values exactly."
  (:require ["mongodb" :refer [BSON]]
            ["node:crypto" :as crypto]
            [cljs.tools.reader.edn :as reader]
            [cljs.tools.reader.reader-types :as readers]
            [knoxx.backend.law.mongo-run-events :as event-law]
            [knoxx.backend.law.run-directory :as directory-law]
            [knoxx.backend.law.run-store :as law]))

(def collection-name "knoxx_runs")

(defn read-snapshot
  "Decode exactly one portable EDN authority value or refuse corrupt storage."
  [encoded]
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
            (let [state (read-snapshot (:run_state_edn doc))]
              (when (or (contains? doc :persistence_token) (contains? state :format))
                (event-law/require! event-law/Uuid (:persistence_token doc)))
              (merge {:revision (:persistence_revision doc) :encoded-state (:run_state_edn doc)
                      :token (:persistence_token doc)}
                     (if (and (map? state) (contains? state :format))
                       (event-law/unpack state) {:state state}))))
        {:revision 0 :legacy-token native
         :expires-ms (some-> (:expiresAt doc) .getTime)
         :legacy-run (dissoc doc :_id :expiresAt :createdAt :updatedAt)}))))

(defn ^:async read!
  "Read a single canonical snapshot without hidden cache or fallback."
  [db run-id]
  (law/require! law/NonBlank run-id)
  (decode (await (.findOne (.collection db collection-name) #js {:run_id run-id} #js {:readPreference "primary"}))))

(defn- document [run-id revision state chain]
  (let [binding (get-in state [:bindings run-id])
        entry (get-in state [:runs run-id])]
    (clj->js {:run_id run-id :session_id (:session_id binding)
              :status (or (get-in entry [:run :status]) "deleted")
              :org_id (:org_id binding) :user_id (:user_id binding)
              :persistence_revision revision :persistence_token (crypto/randomUUID) :run_state_edn (pr-str (event-law/envelope state chain))
              :run_expires_ms (:expires-ms entry)})))

(defn ^:async fence-canonical!
  "Stamp an exact old canonical snapshot before replacing large metadata. Older canonical
   writers replace the document and remove this token, so their writes invalidate adoption."
  [db run-id previous]
  (let [token (crypto/randomUUID)
        result (await (.updateOne (.collection db collection-name)
                                   #js {:run_id run-id :persistence_revision (:revision previous)
                                        :run_state_edn (:encoded-state previous)}
                                   #js {"$set" #js {:persistence_token token}
                                        "$inc" #js {:persistence_revision 1}}
                                   #js {:writeConcern #js {:w "majority" :j true}}))]
    (when (= 1 (.-matchedCount result))
      (assoc previous :token token :revision (inc (:revision previous))))))

(defn- ^:async replace-head! [collection run-id previous next-document]
  (let [revision (:revision previous)
        query (if (zero? revision)
                #js {:run_id run-id :persistence_revision #js {"$exists" false}
                     "$expr" #js {"$eq" #js ["$$ROOT" #js {"$literal" (:legacy-token previous)}]}}
                #js {:run_id run-id :persistence_revision revision :persistence_token (:token previous)})]
    (when (> (.calculateObjectSize BSON #js {:q query :u next-document}) event-law/run-record-bytes)
      (throw (ex-info "Legacy run metadata requires bounded explicit recovery before adoption"
                      {:status 413 :code "run_store_legacy_record_too_large"})))
    (= 1 (.-matchedCount (await (.replaceOne collection query next-document
                                             #js {:writeConcern #js {:w "majority" :j true}}))))))

(defn ^:async compare-and-swap!
  "Publish a bounded versioned head; immutable event preparations become facts only here.
   Tokens prevent revision ABA without copying large metadata into replacement selectors.
   Revisionless legacy adoption retains full BSON equality and refuses oversized commands."
  [db run-id previous state chain]
  (let [collection (.collection db collection-name)
        next-document (document run-id (inc (or (:revision previous) 0)) state chain)]
    (when (> (.calculateObjectSize BSON
                                    #js {:q #js {:run_id run-id :persistence_revision (.-persistence_revision next-document)
                                                 :persistence_token (.-persistence_token next-document)}
                                         :u next-document}) event-law/run-record-bytes)
      (throw (ex-info "Run metadata exceeds Mongo's bounded head capacity"
                      {:status 413 :code "run_store_record_too_large"})))
    (if (nil? previous)
      (try (await (.insertOne collection next-document
                                 #js {:writeConcern #js {:w "majority" :j true}})) true
           (catch :default cause (if (= 11000 (.-code cause)) false (throw cause))))
      (let [fenced (if (and (pos? (:revision previous)) (nil? (:token previous)))
                     (await (fence-canonical! db run-id previous)) previous)]
        (if (nil? fenced) false
            (do (set! (.-persistence_revision next-document) (inc (:revision fenced)))
                (await (replace-head! collection run-id fenced next-document))))))))

(defn ^:async active!
  "Read selected session snapshots; the domain rechecks wall-clock expiry."
  [db session-id]
  (law/require! law/NonBlank session-id)
  (let [cursor (.find (.collection db collection-name)
                       #js {:session_id session-id
                            :status #js {"$in" #js ["running" "queued" "waiting_input"]}} #js {:readPreference "primary"})]
    (mapv (fn [native] {:run-id (.-run_id native) :record (decode native)})
          (array-seq (await (.toArray cursor))))))

(defn ^:async directory!
  "Read exact scoped snapshots; never use approximate search for run authority."
  [db scope]
  (directory-law/scope! scope)
  (let [query (if (:all? scope) #js {} #js {:org_id (:org-id scope)})
        cursor (.find (.collection db collection-name) query #js {:readPreference "primary"})]
    (mapv (fn [native] {:run-id (.-run_id native) :record (decode native)})
          (array-seq (await (.toArray cursor))))))

(defn ^:async setup-indexes!
  "Require unique run identity and indexed active-session reads before selection."
  [db]
  (let [collection (.collection db collection-name)]
    (await (.createIndex collection #js {:run_id 1} #js {:unique true}))
    (await (.createIndex collection #js {:session_id 1 :status 1}))
    (await (.createIndex collection #js {:org_id 1}))))
