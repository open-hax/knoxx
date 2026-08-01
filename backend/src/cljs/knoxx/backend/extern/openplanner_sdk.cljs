(ns knoxx.backend.extern.openplanner-sdk
  "JS boundary for the OpenPlanner SDK (@open-hax/openplanner-sdk).

   The SDK is OpenPlanner's data plane as a library: direct MongoDB plus
   self-sourced embeddings (EMBED_PROVIDER_*), no REST hop. This adapter owns
   the singleton SDK instance and all interop; callers pass and receive CLJS
   data shaped exactly like the corresponding /v1 REST response bodies."
  (:require ["@open-hax/openplanner-sdk" :as sdk-mod]
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

(defn ^:async events!
  "Ingest event envelopes. Same response shape as POST /v1/events."
  [events]
  (let [sdk (await (get-sdk!))
        result (await (.ingestEvents sdk (clj->js events)))]
    (js-delete result "acceptedEvents")
    (js-delete result "backgroundIndexing")
    (xjson/to-cljs result)))

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

;; Translation is an SDK-owned Mongo projection.  Keep this boundary here so
;; callers cannot accidentally fall back to the OpenPlanner HTTP API.
(defn- ^:async translation-store!
  []
  (.-translation (await (get-sdk!))))

(defn ^:async translation-segments!
  [opts]
  (xjson/to-cljs (await (.listSegments (await (translation-store!)) (clj->js (or opts {}))))))

(defn ^:async translation-segment!
  [segment-id]
  (xjson/to-cljs (await (.segment (await (translation-store!)) (str segment-id)))))

(defn ^:async create-translation-segment!
  [segment]
  (xjson/to-cljs (await (.createSegment (await (translation-store!)) (clj->js (or segment {}))))))

(defn ^:async label-translation-segment!
  [segment-id payload]
  (xjson/to-cljs (await (.labelSegment (await (translation-store!)) (str segment-id) (clj->js (or payload {}))))))

(defn ^:async translation-export-manifest!
  [project]
  (xjson/to-cljs (await (.manifest (await (translation-store!)) project))))

(defn ^:async translation-export-sft!
  [opts]
  (await (.exportSft (await (translation-store!)) (clj->js (or opts {})))))

(defn ^:async create-translation-segments-batch!
  [payload]
  (xjson/to-cljs (await (.createSegmentsBatch (await (translation-store!)) (clj->js (or payload {}))))))

(defn ^:async translation-documents!
  [opts]
  (xjson/to-cljs (await (.documents (await (translation-store!)) (clj->js (or opts {}))))))

(defn ^:async translation-document!
  [document-id target-lang]
  (xjson/to-cljs (await (.document (await (translation-store!)) (str document-id) (str target-lang)))))

(defn ^:async review-translation-document!
  [document-id target-lang payload]
  (xjson/to-cljs (await (.reviewDocument (await (translation-store!)) (str document-id) (str target-lang) (clj->js (or payload {}))))))

(defn ^:async create-translation-batch!
  [payload]
  (xjson/to-cljs (await (.createBatch (await (translation-store!)) (clj->js (or payload {}))))))

(defn ^:async translation-batches!
  [opts]
  (xjson/to-cljs (await (.listBatches (await (translation-store!)) (clj->js (or opts {}))))))

(defn ^:async next-translation-batch!
  []
  (xjson/to-cljs (await (.nextBatch (await (translation-store!))))))

(defn ^:async translation-batch!
  [batch-id]
  (xjson/to-cljs (await (.batch (await (translation-store!)) (str batch-id)))))

(defn ^:async update-translation-batch-status!
  [batch-id payload]
  (xjson/to-cljs (await (.updateBatch (await (translation-store!)) (str batch-id) (clj->js (or payload {}))))))
