(ns knoxx.backend.infra.clients.openplanner-clio
  "Canonical Clio OpenPlanner driver with real, repairable local embeddings."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.local-openplanner-events :as events]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.extern.local-openplanner :as host]
            [knoxx.backend.infra.clients.local-embedding :as embedding]
            [knoxx.backend.infra.clients.openplanner :as client]
            [knoxx.backend.infra.clio-application-store :as engine]
            [knoxx.backend.infra.stores.local-openplanner-reference :as reference]
            [knoxx.backend.law.local-openplanner :as law]
            [knoxx.backend.law.openplanner-translation :as translation-law]
            [openplanner.translations.core :as translation]))

(defn- unsupported [method]
  (throw (ex-info "Selected local OpenPlanner provider does not implement this operation"
                  {:status 503 :code "openplanner_local_operation_unsupported" :operation method})))

(defn- now [provider] (host/instant! ((:now! provider))))

(defn- read-query! [provider query]
  (engine/read! (:ledger provider) :openplanner/read [query]))

(defn- write-operation! [provider operation]
  (engine/write! (:ledger provider) :openplanner/write [operation]))

(defn- query! [provider kind opts]
  (read-query! provider {:kind kind :opts (host/query-options opts) :at (now provider)}))

(defn- require-embeddings! [provider]
  (when-not (:embeddings-configured? provider)
    (throw (ex-info "Local OpenPlanner embeddings are not configured"
                    {:status 503 :code "openplanner_embedding_not_configured"}))))

(defn- ^:async embed! [provider texts]
  (require-embeddings! provider)
  (law/assert-valid! [:vector {:min 1} law/NonBlank] texts)
  (let [{:keys [model dimensions vectors] :as result} (await ((:embed! provider) texts))
        config (:embedding-config provider)]
    (when-not (and (= model (:embed-provider-model config))
                   (= dimensions (:embed-provider-dimensions config))
                   (= (count texts) (count vectors)))
      (throw (ex-info "Embedding response disagrees with configured model or input count"
                      {:status 502 :code "openplanner_embedding_invalid"})))
    (doseq [vector vectors]
      (law/projection! {:id "response" :digest "response" :model model
                        :dimensions dimensions :embedding vector}))
    result))

(defn- matching-vector? [provider event row]
  (and row
       (do (law/projection! row) true)
       (= (:digest row) (events/content-digest event))
       (= (:model row) (get-in provider [:embedding-config :embed-provider-model]))
       (= (:dimensions row) (get-in provider [:embedding-config :embed-provider-dimensions]))))

(defn- ^:async ensure-vectors! [provider ids]
  (law/assert-valid! [:vector law/NonBlank] ids)
  (let [ids (vec (distinct ids))]
    (when (seq ids) (require-embeddings! provider))
    (loop [remaining ids pending []]
      (if-let [id (first remaining)]
        (let [event (await (read-query! provider {:kind :event :id id}))
              row (await (read-query! provider {:kind :vector :id id}))]
          (when-not event (throw (ex-info "Cannot index an absent event" {:status 404 :id id})))
          (recur (rest remaining) (cond-> pending (not (matching-vector? provider event row)) (conj event))))
        (do
          (when (seq pending)
            (let [{:keys [model dimensions vectors]} (await (embed! provider (mapv :text pending)))
                  projections (mapv (fn [event vector]
                                      {:id (:id event) :digest (events/content-digest event)
                                       :model model :dimensions dimensions :embedding vector}) pending vectors)]
              (await (write-operation! provider {:kind :vectors :vectors projections}))))
          {:ok true :event-ids ids :event-count (count ids) :vector-count (count ids)
           :repaired-event-ids (mapv :id pending)})))))

(defn- ^:async ingest! [provider values]
  (law/assert-valid! [:vector map?] values)
  (let [events (mapv host/normalize-event values)
        result (await (write-operation! provider {:kind :events :events events}))]
    ;; Event facts are durable before the model call. Failure leaves a visible,
    ;; repairable pending projection, never invented vectors or a false success.
    (await (ensure-vectors! provider (mapv :id (remove #(str/blank? (:text %)) events))))
    result))

(defn- ^:async search! [provider payload]
  (law/scope! payload)
  (require-embeddings! provider)
  (let [payload (host/query-options payload)
        _ (law/assert-valid! law/NonBlank (or (:q payload) (:query payload)))
        candidates (await (read-query! provider {:kind :events :opts payload}))
        relevant (filter #(and (not (str/blank? (:text %)))
                               (or (nil? (:kind payload)) (= (:kind payload) (:kind %)))
                               (or (nil? (:source payload)) (= (:source payload) (:source %)))
                               (or (nil? (:session payload)) (= (:session payload) (events/event-session %)))) candidates)
        _ (await (ensure-vectors! provider (mapv :id relevant)))
        {:keys [model dimensions vectors]} (await (embed! provider [(or (:q payload) (:query payload))]))]
    {:ok true :result (await (read-query! provider {:kind :search :opts payload
                                                   :vector (first vectors) :model model :dimensions dimensions}))}))

(defn- ^:async upsert-document! [provider document]
  (law/scope! document)
  (law/assert-valid! [:map [:id law/NonBlank] [:content law/NonBlank]] document)
  (let [id (str "document-" (crypto/sha256-hex (pr-str [(:org_id document) (:id document) (:content document)])))
        previous (await (read-query! provider {:kind :event :id id}))
        event (or previous {:id id :schema "openplanner.event.v1" :ts (now provider)
                            :source (or (:source document) "knoxx") :kind "document"
                            :text (:content document) :source_ref {:project (:project document)}
                            :extra (assoc document :document_id (:id document))})]
    (await (ingest! provider [event]))
    {:ok true :document document :indexed true}))

(defn- ^:async import-segments! [provider request]
  (translation-law/assert-valid! :local/import translation-law/CreateTranslationSegmentsBatchRequest request)
  (loop [rows (:segments request) index 0 results []]
    (if-let [row (first rows)]
      (let [input (assoc row :org_id (:org_id request) :project (:project request)
                             :segment_index (or (:segment_index row) index))
            result (await (write-operation! provider {:kind :segment :input input :at (now provider)}))]
        (recur (rest rows) (inc index) (conj results (assoc result :index index))))
      {:ok true :imported (count results) :errors 0 :results results})))

(defn- ^:async export-sft! [provider opts]
  (translation-law/assert-valid! :local/sft translation-law/TranslationSftRequest opts)
  (let [rows (await (query! provider :sft opts))]
    (host/json-lines
     (map (fn [row] (translation/sft-row
                     {:source-text (:source_text row) :translated-text (:translated_text row)
                      :source-lang (:source_lang row) :target-lang (:target_lang row)
                      :document-id (:document_id row) :segment-id (:id row)})) rows))))

(defrecord ClioOpenPlannerClient [ledger now! embedding-config embed! embeddings-configured?]
  client/IOpenPlannerClient
  (enabled? [_] true)
  (health! [_] {:ok true :status 200 :body {:ok true :provider "clio"
                                          :embeddings-configured embeddings-configured?}})
  (events! [this values] (ingest! this values))
  (session! [this id opts] (read-query! this {:kind :session :id id :opts (host/query-options opts)}))
  (sessions! [this opts] (query! this :sessions opts))
  (vector-search! [this payload] (search! this payload))
  (graph-memory! [_ _] (unsupported :graph-memory))
  (graph-export! [_ _] (unsupported :graph-export))
  (upsert-document! [this document] (upsert-document! this document))
  (documents-stats! [_] (unsupported :documents-stats))
  (graph-monitoring! [_] (unsupported :graph-monitoring))
  (mongo-collections! [_] (unsupported :mongo-collections))
  (mongo-query! [_ _] (unsupported :mongo-query))
  (build-semantic-edges! [_ _] (unsupported :build-semantic-edges))
  (record-labels! [_ _] (unsupported :record-labels))
  (record-reaction! [_ _ _] (unsupported :record-reaction))
  (translation-segments! [this opts] (query! this :segments opts))
  (translation-segment! [this id opts] (read-query! this {:kind :segment :id id :opts opts}))
  (create-translation-segment! [this input] (write-operation! this {:kind :segment :input input :at (now this)}))
  (label-translation-segment! [this id input]
    (write-operation! this {:kind :label :id (host/uuid!) :at (now this)
                             :input {:segment-id id :request input}}))
  (translation-export-manifest! [this opts] (query! this :manifest opts))
  (translation-export-sft! [this opts] (export-sft! this opts))
  (create-translation-segments-batch! [this input] (import-segments! this input))
  (translation-documents! [this opts] (query! this :documents opts))
  (translation-document! [this id language opts]
    (read-query! this {:kind :document :id id :language language :opts opts}))
  (review-translation-document! [this id language input]
    (write-operation! this {:kind :review :id (host/uuid!) :at (now this)
                             :input {:document-id id :language language :request input}}))
  (create-translation-batch! [this input]
    (write-operation! this {:kind :batch :input input :id (host/uuid!) :at (now this)}))
  (translation-batches! [this opts] (query! this :batches opts))
  (next-translation-batch! [this opts] (write-operation! this {:kind :claim :input opts :at (now this)}))
  (translation-batch! [this id opts] (read-query! this {:kind :batch :id id :opts opts}))
  (update-translation-batch-status! [this id input]
    (write-operation! this {:kind :batch-status :id id :input input :at (now this)}))
  (v1-json! [_ _ _ _] (unsupported :v1-json))
  (forward-v1! [_ _] (unsupported :forward-v1))
  client/IOpenPlannerEventLookup
  (-event-by-id! [this id] (read-query! this {:kind :event :id id}))
  client/IOpenPlannerEventProjectionRepair
  (ingest-events-awaiting-projections! [this values] (ingest! this values))
  (ensure-event-extra-fields! [this id required]
    (write-operation! this {:kind :extra :id id :required required}))
  (ensure-event-vectors! [this ids] (ensure-vectors! this ids)))

(defn open!
  "Open Clio storage without requiring a model; validate partial settings eagerly."
  [{:keys [directory now! embedding-config embed!] :as options}]
  (law/assert-valid! law/Options options)
  (let [configured? (law/embedding-configured! embedding-config)
        clock (or now! host/now!)]
    (host/instant! (clock))
    (map->ClioOpenPlannerClient
     {:ledger (engine/open! {:directory directory :stream "knoxx/openplanner"
                             :projection reference/projection
                             :reads {:openplanner/read reference/read!}
                             :writes {:openplanner/write reference/write!}})
      :now! clock :embedding-config (or embedding-config {}) :embeddings-configured? configured?
      :embed! (or embed! (partial embedding/embed! embedding-config))})))

(defonce providers* (atom {}))

(defn configured-client
  "Reuse one selected driver by its explicit ledger and embedding configuration."
  [config]
  (let [directory (or (:openplanner-directory config)
                       (when-let [wiki (:wiki-directory config)] (str wiki "/openplanner")))
        embedding-config (select-keys config [:embed-provider-base-url :embed-provider-model :embed-provider-dimensions])
        key [directory embedding-config]]
    (or (get @providers* key)
        (let [provider (open! {:directory directory :embedding-config embedding-config})]
          (swap! providers* assoc key provider)
          provider))))

(client/register-local-client-factory! configured-client)
