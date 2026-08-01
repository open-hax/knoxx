(ns knoxx.backend.extern.openplanner-sdk
  "JS boundary for the OpenPlanner SDK (@open-hax/openplanner-sdk).

   The SDK is OpenPlanner's data plane as a library: direct MongoDB plus
   self-sourced embeddings (EMBED_PROVIDER_*), no REST hop. This adapter owns
   the singleton SDK instance and all interop; callers pass and receive CLJS
   data shaped exactly like the corresponding /v1 REST response bodies."
  (:require ["@open-hax/openplanner-sdk" :as sdk-mod]
            [knoxx.backend.extern.json :as xjson]
            [knoxx.backend.law.openplanner-translation :as translation-contract]))

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

(defn- request-map
  [value]
  (xjson/to-cljs (or value {})))

;; Translation is an SDK-owned Mongo projection. Keep this boundary here so
;; callers cannot accidentally fall back to the OpenPlanner HTTP API.
(defn- ^:async translation-store!
  []
  (.-translation (await (get-sdk!))))

(defn ^:async translation-segments!
  [opts]
  (let [request (translation-contract/assert-valid!
                 :translation-segments/request
                 translation-contract/TranslationSegmentsRequest
                 (request-map opts))
        response (xjson/to-cljs
                  (await (.listSegments (await (translation-store!)) (clj->js request))))]
    (translation-contract/assert-valid!
     :translation-segments/response
     translation-contract/TranslationSegmentsResponse
     response)))

(defn ^:async translation-segment!
  [segment-id]
  (let [request (translation-contract/assert-valid!
                 :translation-segment/request
                 translation-contract/NonBlankString
                 (str segment-id))
        response (xjson/to-cljs
                  (await (.segment (await (translation-store!)) request)))]
    (translation-contract/assert-valid!
     :translation-segment/response
     translation-contract/TranslationSegmentResponse
     response)))

(defn ^:async create-translation-segment!
  [segment]
  (let [request (translation-contract/assert-valid!
                 :create-translation-segment/request
                 translation-contract/CreateTranslationSegmentRequest
                 (request-map segment))
        response (xjson/to-cljs
                  (await (.createSegment (await (translation-store!)) (clj->js request))))]
    (translation-contract/assert-valid!
     :create-translation-segment/response
     translation-contract/CreateTranslationSegmentResponse
     response)))

(defn ^:async label-translation-segment!
  [segment-id payload]
  (let [validated-id (translation-contract/assert-valid!
                      :label-translation-segment/id
                      translation-contract/NonBlankString
                      (str segment-id))
        request (translation-contract/assert-valid!
                 :label-translation-segment/request
                 translation-contract/LabelTranslationSegmentRequest
                 (request-map payload))
        response (xjson/to-cljs
                  (await (.labelSegment (await (translation-store!))
                                        validated-id
                                        (clj->js request))))]
    (translation-contract/assert-valid!
     :label-translation-segment/response
     translation-contract/LabelTranslationSegmentResponse
     response)))

(defn ^:async translation-export-manifest!
  [input]
  (let [request-value (if (or (map? input) (object? input))
                        (request-map input)
                        (str input))
        request (translation-contract/assert-valid!
                 :translation-export-manifest/request
                 translation-contract/TranslationManifestRequest
                 request-value)
        sdk-request (if (map? request) (clj->js request) request)
        response (xjson/to-cljs
                  (await (.manifest (await (translation-store!)) sdk-request)))]
    (translation-contract/assert-valid!
     :translation-export-manifest/response
     translation-contract/TranslationManifestResponse
     response)))

(defn ^:async translation-export-sft!
  [opts]
  (let [request (translation-contract/assert-valid!
                 :translation-export-sft/request
                 translation-contract/TranslationSftRequest
                 (request-map opts))
        response (await (.exportSft (await (translation-store!)) (clj->js request)))]
    (translation-contract/assert-valid!
     :translation-export-sft/response
     translation-contract/TranslationSftResponse
     response)))

(defn ^:async create-translation-segments-batch!
  [payload]
  (let [request (translation-contract/assert-valid!
                 :create-translation-segments-batch/request
                 translation-contract/CreateTranslationSegmentsBatchRequest
                 (request-map payload))
        response (xjson/to-cljs
                  (await (.createSegmentsBatch (await (translation-store!)) (clj->js request))))]
    (translation-contract/assert-valid!
     :create-translation-segments-batch/response
     translation-contract/CreateTranslationSegmentsBatchResponse
     response)))

(defn ^:async translation-documents!
  [opts]
  (let [request (translation-contract/assert-valid!
                 :translation-documents/request
                 translation-contract/TranslationDocumentsRequest
                 (request-map opts))
        response (xjson/to-cljs
                  (await (.documents (await (translation-store!)) (clj->js request))))]
    (translation-contract/assert-valid!
     :translation-documents/response
     translation-contract/TranslationDocumentsResponse
     response)))

(defn ^:async translation-document!
  [document-id target-lang]
  (let [validated-document-id (translation-contract/assert-valid!
                               :translation-document/document-id
                               translation-contract/NonBlankString
                               (str document-id))
        validated-target-lang (translation-contract/assert-valid!
                               :translation-document/target-lang
                               translation-contract/NonBlankString
                               (str target-lang))
        response (xjson/to-cljs
                  (await (.document (await (translation-store!))
                                    validated-document-id
                                    validated-target-lang)))]
    (translation-contract/assert-valid!
     :translation-document/response
     translation-contract/TranslationDocumentResponse
     response)))

(defn ^:async review-translation-document!
  [document-id target-lang payload]
  (let [validated-document-id (translation-contract/assert-valid!
                               :review-translation-document/document-id
                               translation-contract/NonBlankString
                               (str document-id))
        validated-target-lang (translation-contract/assert-valid!
                               :review-translation-document/target-lang
                               translation-contract/NonBlankString
                               (str target-lang))
        request (translation-contract/assert-valid!
                 :review-translation-document/request
                 translation-contract/ReviewTranslationDocumentRequest
                 (request-map payload))
        response (xjson/to-cljs
                  (await (.reviewDocument (await (translation-store!))
                                          validated-document-id
                                          validated-target-lang
                                          (clj->js request))))]
    (translation-contract/assert-valid!
     :review-translation-document/response
     translation-contract/ReviewTranslationDocumentResponse
     response)))

(defn ^:async create-translation-batch!
  [payload]
  (let [request (translation-contract/assert-valid!
                 :create-translation-batch/request
                 translation-contract/CreateTranslationBatchRequest
                 (request-map payload))
        response (xjson/to-cljs
                  (await (.createBatch (await (translation-store!)) (clj->js request))))]
    (translation-contract/assert-valid!
     :create-translation-batch/response
     translation-contract/CreateTranslationBatchResponse
     response)))

(defn ^:async translation-batches!
  [opts]
  (let [request (translation-contract/assert-valid!
                 :translation-batches/request
                 translation-contract/TranslationBatchesRequest
                 (request-map opts))
        response (xjson/to-cljs
                  (await (.listBatches (await (translation-store!)) (clj->js request))))]
    (translation-contract/assert-valid!
     :translation-batches/response
     translation-contract/TranslationBatchesResponse
     response)))

(defn ^:async next-translation-batch!
  []
  (let [response (xjson/to-cljs
                  (await (.nextBatch (await (translation-store!)))))]
    (translation-contract/assert-valid!
     :next-translation-batch/response
     translation-contract/NextTranslationBatchResponse
     response)))

(defn ^:async translation-batch!
  [batch-id]
  (let [request (translation-contract/assert-valid!
                 :translation-batch/request
                 translation-contract/NonBlankString
                 (str batch-id))
        response (xjson/to-cljs
                  (await (.batch (await (translation-store!)) request)))]
    (translation-contract/assert-valid!
     :translation-batch/response
     translation-contract/TranslationBatchResponse
     response)))

(defn ^:async update-translation-batch-status!
  [batch-id payload]
  (let [validated-id (translation-contract/assert-valid!
                      :update-translation-batch/id
                      translation-contract/NonBlankString
                      (str batch-id))
        request (translation-contract/assert-valid!
                 :update-translation-batch/request
                 translation-contract/UpdateTranslationBatchRequest
                 (request-map payload))
        response (xjson/to-cljs
                  (await (.updateBatch (await (translation-store!))
                                       validated-id
                                       (clj->js request))))]
    (translation-contract/assert-valid!
     :update-translation-batch/response
     translation-contract/UpdateTranslationBatchResponse
     response)))
