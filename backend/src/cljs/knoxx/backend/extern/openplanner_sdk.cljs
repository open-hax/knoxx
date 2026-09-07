(ns knoxx.backend.extern.openplanner-sdk
  "JS boundary for the OpenPlanner SDK (@open-hax/openplanner-sdk).

   The SDK is OpenPlanner's data plane as a library: direct MongoDB plus
   self-sourced embeddings (EMBED_PROVIDER_*), no REST hop. This adapter owns
   the singleton SDK instance and all interop; callers pass and receive CLJS
   data shaped exactly like the corresponding /v1 REST response bodies."
  (:require ["@open-hax/openplanner-sdk" :as sdk-mod]
            ["@open-hax/openplanner-sdk/mongo-vectors" :as mongo-vectors]
            [knoxx.backend.extern.json :as xjson]))

(defonce ^:private sdk-promise* (atom nil))

(defn- resolve-mongo-uri
  "Resolve the MongoDB connection string from the same env lineage the rest of
   the backend uses (see knoxx.backend.infra.mongo-client)."
  []
  (or (aget js/process.env "MONGODB_URI")
      (aget js/process.env "OPENPLANNER_MONGODB_URI")
      "mongodb://localhost:27017"))

(defn- resolve-mongo-db-name
  []
  (or (aget js/process.env "MONGODB_DB")
      (aget js/process.env "OPENPLANNER_MONGODB_DB")
      "openplanner"))

(defn- create-sdk!
  []
  (sdk-mod/createOpenPlannerSdk
   #js {:config #js {:mongodb #js {:uri (resolve-mongo-uri)
                                   :dbName (resolve-mongo-db-name)}}
        :log #js {:warn (fn [obj msg]
                          (js/console.warn "[openplanner-sdk]" msg
                                           (or (some-> obj (aget "err") (aget "message")) "")))}}))

(defn- get-sdk!
  "Shared SDK instance as a js Promise; created on first use."
  ^js []
  (or @sdk-promise*
      (let [created (create-sdk!)]
        (reset! sdk-promise* created)
        created)))

(defn ^:async close-sdk!
  "Close the shared SDK instance (mongo connection). Safe to call when unused."
  []
  (when-let [pending @sdk-promise*]
    (reset! sdk-promise* nil)
    (let [sdk (await pending)]
      (await (.close sdk)))))

(defn- mongo-ctx
  ^js [^js sdk]
  #js {:mongo (.-mongo sdk)})

(defn- configured-embedding-dimensions!
  []
  (let [raw (aget js/process.env "EMBED_PROVIDER_DIMENSIONS")
        trimmed (when (string? raw) (.trim raw))
        parsed (js/Number trimmed)]
    (when-not (and (string? raw)
                   (re-matches #"[1-9][0-9]*" trimmed)
                   (js/Number.isInteger parsed)
                   (pos? parsed))
      (throw (ex-info
              "EMBED_PROVIDER_DIMENSIONS must be configured as a positive integer"
              {:status 503
               :code "openplanner_embedding_dimensions_invalid"
               :configured-value raw})))
    parsed))

(defn- finite-number?
  [value]
  (and (number? value) (js/Number.isFinite value)))

(defn- valid-vector-row?
  [expected-dimensions row]
  (let [embedding (:embedding row)]
    (and (vector? embedding)
         (pos? (count embedding))
         (= expected-dimensions (count embedding))
         (= expected-dimensions (:embedding_dimensions row))
         (every? finite-number? embedding))))

(defn- checked-event-ids!
  [event-ids]
  (let [ids (vec (distinct event-ids))]
    (when-let [invalid-input-ids
               (seq (filterv #(not (and (string? %) (seq (.trim %)))) ids))]
      (throw (ex-info "OpenPlanner vector verification requires event ids"
                      {:status 500
                       :code "openplanner_event_ids_invalid"
                       :invalid-event-ids (vec invalid-input-ids)})))
    ids))

(defn- ^:async hot-vector-rows!
  [^js sdk ids]
  (let [hot-vectors (some-> sdk (.-mongo) (.-hotVectors))]
    (when-not (and hot-vectors (fn? (.-find hot-vectors)))
      (throw (ex-info "OpenPlanner hot-vector collection is unavailable"
                      {:status 503
                       :code "openplanner_hot_vectors_unavailable"
                       :event-ids ids})))
    (let [cursor (.find hot-vectors
                        (clj->js {:parent_id {:$in ids}})
                        (clj->js {:projection {:_id 1
                                               :parent_id 1
                                               :embedding 1
                                               :embedding_dimensions 1}}))]
      (xjson/to-cljs (await (.toArray cursor))))))

(defn- vector-coverage
  [ids expected-dimensions rows]
  (let [rows-by-event (group-by :parent_id rows)
        missing (filterv #(empty? (get rows-by-event %)) ids)
        invalid (filterv (fn [id]
                           (some #(not (valid-vector-row? expected-dimensions %))
                                 (get rows-by-event id)))
                         ids)]
    {:missing-event-ids missing
     :invalid-event-ids invalid}))

(defn- assert-vector-coverage!
  [ids expected-dimensions rows]
  (let [{:keys [missing-event-ids invalid-event-ids]}
        (vector-coverage ids expected-dimensions rows)]
    (when (or (seq missing-event-ids) (seq invalid-event-ids))
      (throw (ex-info
              "OpenPlanner event vector indexing did not materialize the configured embedding shape"
              {:status 503
               :code "openplanner_event_vector_verification_failed"
               :event-ids ids
               :expected-dimensions expected-dimensions
               :missing-event-ids missing-event-ids
               :invalid-event-ids invalid-event-ids})))
    {:ok true
     :event-ids ids
     :event-count (count ids)
     :vector-count (count rows)
     :embedding-dimensions expected-dimensions}))

(defn- ^:async verify-event-vectors-with-sdk!
  [^js sdk event-ids]
  (let [ids (checked-event-ids! event-ids)]
    (if (empty? ids)
      {:ok true :event-ids [] :event-count 0 :vector-count 0}
      (let [expected-dimensions (configured-embedding-dimensions!)
            rows (await (hot-vector-rows! sdk ids))]
        (assert-vector-coverage! ids expected-dimensions rows)))))

(defn ^:async verify-event-vectors!
  "Fail closed unless every named event has materialized hot-vector chunks.

  This is public so an idempotent caller that finds an already-durable base
  event can verify its detached projection too; replay must not equate an
  event row with a usable embedding. Every chunk must carry a finite, nonempty
  vector whose length and recorded dimension equal EMBED_PROVIDER_DIMENSIONS."
  [event-ids]
  (let [sdk (await (get-sdk!))]
    (await (verify-event-vectors-with-sdk! sdk event-ids))))

(defn- events-collection!
  [^js sdk event-id]
  (let [events (some-> sdk (.-mongo) (.-events))]
    (when-not (and events
                   (fn? (.-find events))
                   (fn? (.-findOne events))
                   (fn? (.-updateOne events)))
      (throw (ex-info "OpenPlanner event collection is unavailable"
                      {:status 503
                       :code "openplanner_events_unavailable"
                       :event-id event-id})))
    events))

(defn- ^:async event-row!
  [^js events event-id]
  (xjson/to-cljs
   (await (.findOne events
                    (clj->js {:id event-id})
                    (clj->js {:projection {:id 1 :extra 1}})))))

(defn- extra-state!
  [event-id row required-extra]
  (when-not row
    (throw (ex-info "OpenPlanner event disappeared during metadata repair"
                    {:status 503
                     :code "openplanner_event_missing"
                     :event-id event-id})))
  (let [extra (:extra row)
        conflicts (into {}
                        (keep (fn [[key expected]]
                                (let [actual (get extra key)]
                                  (when (and (some? actual)
                                             (not= expected actual))
                                    [key {:expected expected
                                          :actual actual}]))))
                        required-extra)]
    (when (seq conflicts)
      (throw (ex-info "OpenPlanner event metadata conflicts with immutable translation evidence"
                      {:status 409
                       :code "openplanner_event_extra_conflict"
                       :event-id event-id
                       :conflicts conflicts})))
    {:extra extra
     :missing (into {}
                    (filter (fn [[key _expected]] (nil? (get extra key))))
                    required-extra)}))

(defn- dotted-extra-set
  [fields]
  (into {} (map (fn [[key value]] [(str "extra." (name key)) value])) fields))

(defn- missing-field-guard
  [[key expected]]
  (let [path (str "extra." (name key))]
    {:$or [{path {:$exists false}}
           {path nil}
           {path expected}]}))

(defn- ^:async write-missing-extra!
  [^js events event-id extra missing]
  (if (map? extra)
    (await (.updateOne events
                       (clj->js {:id event-id
                                 :$and (mapv missing-field-guard missing)})
                       (clj->js {:$set (dotted-extra-set missing)})))
    (await (.updateOne events
                       (clj->js {:id event-id
                                 :$or [{:extra nil}
                                       {:extra {:$exists false}}]})
                       (clj->js {:$set {:extra missing}})))))

(defn ^:async ensure-event-extra-fields!
  "Backfill deterministic missing fields on one stable existing event.

  A conflicting non-nil value is immutable evidence drift and is refused. The
  guarded update fills only absent/nil fields, then re-reads the row so a lost
  race or an adapter that ignored the update still fails closed."
  [event-id required-extra]
  (when-not (map? required-extra)
    (throw (ex-info "OpenPlanner event extra repair requires a field map"
                    {:status 500 :code "openplanner_event_extra_invalid"})))
  (let [sdk (await (get-sdk!))
        events (events-collection! sdk event-id)
        before (extra-state! event-id (await (event-row! events event-id))
                             required-extra)]
    (when (seq (:missing before))
      (await (write-missing-extra! events event-id (:extra before)
                                   (:missing before))))
    (let [after (extra-state! event-id (await (event-row! events event-id))
                              required-extra)]
      (when (seq (:missing after))
        (throw (ex-info "OpenPlanner event metadata repair was not persisted"
                        {:status 503
                         :code "openplanner_event_extra_repair_failed"
                         :event-id event-id
                         :missing-fields (vec (keys (:missing after)))})))
      {:ok true
       :event-id event-id
       :updated-fields (vec (keys (:missing before)))})))

(defn- ^:async raw-event-rows!
  [^js events ids]
  (let [cursor (.find events
                      (clj->js {:id {:$in ids}})
                      (clj->js
                       {:projection {:id 1 :ts 1 :source 1 :kind 1
                                     :project 1 :session 1 :message 1
                                     :role 1 :author 1 :model 1 :text 1
                                     :extra 1}}))]
    (await (.toArray cursor))))

(defn- raw-events-in-id-order!
  [ids raw-rows]
  (let [by-id (into {} (map (fn [^js row] [(aget row "id") row]))
                    (array-seq raw-rows))
        missing (filterv #(nil? (get by-id %)) ids)]
    (when (seq missing)
      (throw (ex-info "OpenPlanner base event is missing during vector repair"
                      {:status 503
                       :code "openplanner_event_missing"
                       :missing-event-ids missing})))
    (mapv by-id ids)))

(defn- unindexable-event-reason
  [^js event]
  (let [text (aget event "text")
        kind (aget event "kind")]
    (cond
      (not (string? text)) :text-not-string
      (empty? (.trim text)) :blank-text
      (contains? #{"graph.node" "graph.edge"} kind) :excluded-kind
      (not (sdk-mod/shouldIndexEventHotVectors event)) :sdk-declined
      :else nil)))

(defn- raw-event-labels
  [^js extra]
  (let [labels (some-> extra (aget "openplanner_labels") (aget "labels"))]
    (if (js/Array.isArray labels)
      (->> (array-seq labels)
           (map #(-> (or % "") str .trim))
           (remove empty?)
           distinct
           clj->js)
      #js [])))

(defn- nullish-default
  [value fallback]
  (if (nil? value) fallback value))

(defn- embedding-scope
  [^js event]
  (let [scope #js {:source (aget event "source")
                   :kind (aget event "kind")}
        project (aget event "project")]
    (when-not (nil? project)
      (aset scope "project" project))
    scope))

(defn- index-metadata
  [^js event ^js extra embedding-model]
  (let [labels-root (some-> extra (aget "openplanner_labels"))]
    #js {:ts (aget event "ts")
         :source (aget event "source")
         :kind (aget event "kind")
         :project (aget event "project")
         :session (aget event "session")
         :author (nullish-default (aget event "author") "")
         :role (nullish-default (aget event "role") "")
         :model (nullish-default (aget event "model") "")
         :embedding_model (nullish-default embedding-model "")
         :search_tier "hot"
         :visibility (nullish-default (some-> extra (aget "visibility"))
                                      "internal")
         :quality_label (nullish-default
                         (some-> labels-root (aget "quality")) "")
         :labels (raw-event-labels extra)
         :title (nullish-default
                 (some-> extra (aget "title"))
                 (nullish-default (aget event "message")
                                  (aget event "id")))}))

(defn- assert-indexable-event!
  [^js event]
  (when-let [reason (unindexable-event-reason event)]
    (throw (ex-info "OpenPlanner base event cannot produce a hot vector"
                    {:status 422
                     :code "openplanner_event_not_vector_indexable"
                     :event-id (aget event "id")
                     :kind (aget event "kind")
                     :reason reason}))))

(defn- ^:async index-event-vector!
  [^js sdk ^js event]
  (assert-indexable-event! event)
  (let [scope (embedding-scope event)
        hot (some-> sdk (.-embeddingRuntime) (.-hot))
        extra (or (aget event "extra") #js {})
        embedding-model (.getModel hot scope)
        embedding-function (.getBackgroundEmbeddingFunction hot scope)]
    (try
      (await
       (mongo-vectors/indexTextInMongoVectors
        #js {:mongo (.-mongo sdk)
             :tier "hot"
             :parentId (aget event "id")
             :text (aget event "text")
             :extra extra
             :metadata (index-metadata event extra embedding-model)
             :embeddingFunction embedding-function}))
      (catch :default err
        (throw (ex-info "OpenPlanner event vector repair failed"
                        {:status 503
                         :code "openplanner_event_vector_repair_failed"
                         :event-id (aget event "id")
                         :detail (or (.-message err) (str err))}
                        err))))))

(defn- ^:async repair-event-vectors!
  [^js sdk raw-events]
  (loop [remaining raw-events]
    (when-let [event (first remaining)]
      (await (index-event-vector! sdk event))
      (recur (next remaining)))))

(defn- ^:async ensure-event-vectors-with-sdk!
  [^js sdk event-ids]
  (let [ids (checked-event-ids! event-ids)]
    (if (empty? ids)
      {:ok true :event-ids [] :event-count 0 :vector-count 0
       :repaired-event-ids []}
      (let [dimensions (configured-embedding-dimensions!)
            rows (await (hot-vector-rows! sdk ids))
            coverage (vector-coverage ids dimensions rows)
            repair-set (set (concat (:missing-event-ids coverage)
                                    (:invalid-event-ids coverage)))
            repair-ids (filterv repair-set ids)]
        (when (seq repair-ids)
          (let [events (events-collection! sdk (first repair-ids))
                raw-rows (await (raw-event-rows! events repair-ids))]
            (await (repair-event-vectors!
                    sdk (raw-events-in-id-order! repair-ids raw-rows)))))
        (assoc (await (verify-event-vectors-with-sdk! sdk ids))
               :repaired-event-ids repair-ids)))))

(defn ^:async ensure-event-vectors!
  "Materialize missing/invalid hot vectors from immutable base event rows.

  No base event is reinserted. Re-indexing uses the SDK's current scoped model,
  background embedding function, event metadata rules and transactional vector
  replacement, then performs the same strict dimensional verification as an
  awaited ingest. Blank text and SDK-excluded event kinds fail explicitly."
  [event-ids]
  (let [sdk (await (get-sdk!))]
    (await (ensure-event-vectors-with-sdk! sdk event-ids))))

(defn ^:async events!
  "Ingest event envelopes. Same response shape as POST /v1/events.

  Embedded callers that need read-your-writes may pass
  `{:await-index? true}`. This awaits the SDK's detached embedding work before
  removing its transport-only promise from the returned response."
  ([events]
   (await (events! events {})))
  ([events {:keys [await-index?]}]
   (let [sdk (await (get-sdk!))
         result (await (.ingestEvents sdk (clj->js events)))
         background-indexing (aget result "backgroundIndexing")]
     (when (and await-index? background-indexing)
       (await background-indexing))
     (when await-index?
       (await (ensure-event-vectors-with-sdk! sdk (mapv :id events))))
     (js-delete result "acceptedEvents")
     (js-delete result "backgroundIndexing")
     (xjson/to-cljs result))))

(defn ^:async vector-search!
  "Vector search. Same response shape as POST /v1/search/vector."
  [payload]
  (let [sdk (await (get-sdk!))]
    (xjson/to-cljs (await (.searchVector sdk (clj->js payload))))))

(defn ^:async session
  "Session detail. Same response shape as GET /v1/sessions/:id."
  [session-id opts]
  (let [sdk (await (get-sdk!))]
    (xjson/to-cljs (await (sdk-mod/getSessionResponse (mongo-ctx sdk) (str session-id) (clj->js (or opts {})))))))

(defn ^:async sessions
  "Session list. Same response shape as GET /v1/sessions."
  [opts]
  (let [sdk (await (get-sdk!))]
    (xjson/to-cljs (await (sdk-mod/listSessionsResponse (mongo-ctx sdk) (clj->js (or opts {})))))))

(defn ^:async mongo-collections
  "Collection inventory. Same response shape as GET /v1/mongo/collections."
  []
  (let [sdk (await (get-sdk!))]
    (xjson/to-cljs (await (sdk-mod/listCollectionsResponse (mongo-ctx sdk))))))

(defn ^:async mongo-query
  "Raw collection query. Same response shape as POST /v1/mongo/query."
  [payload]
  (let [sdk (await (get-sdk!))]
    (xjson/to-cljs (await (sdk-mod/queryCollectionResponse (mongo-ctx sdk) (clj->js (or payload {})))))))

(defn ^:async health
  "Mongo-mode health probe shaped like the REST client's health! result:
   {:ok bool :status int :body map}."
  []
  (try
    (let [sdk (await (get-sdk!))
          db (.-db (.-mongo sdk))]
      (await (.command (.admin db) #js {:ping 1}))
      {:ok true
       :status 200
       :body {:ok true :storageBackend "mongodb" :transport "sdk"}})
    (catch :default err
      {:ok false
       :status 503
       :body {:detail (str "OpenPlanner SDK mongo ping failed: " (.-message err))}})))
