(ns knoxx.backend.domain.resources.loader
  "Resource loader facade over the legacy EDN contract loader.

   EDN files describe resources. They still must satisfy boundary contracts
   (Malli schemas), but callers should talk about resource kinds and resource
   registries rather than treating every file as a contract entity."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.contracts.loader :as contract-loader]))

(def resource-kind->class
  {:action "actions"
   :actor "actors"
   :agent "agents"
   :capability "capabilities"
   :cms "cms"
   :generator "generators"
   :ingest-source "ingest_sources"
   :model "models"
   :model-family "model_families"
   :pipeline "pipelines"
   :policy "policies"
   :role "roles"
   :rule "rules"
   :runtime-feature "runtime_features"
   :schedule "schedules"
   :source "sources"
   :source-mode "source_modes"
   :sub-agent "sub_agents"
   :trigger "triggers"
   :user "users"
   :workflow "workflows"})

(def class->resource-kind
  (reduce-kv (fn [acc kind class-name]
               (assoc acc class-name kind))
             {}
             resource-kind->class))

(defn- token
  [value]
  (some-> value
          (cond-> (keyword? value) name
                  (not (keyword? value)) str)
          str/trim
          str/lower-case))

(defn normalize-resource-kind
  [value]
  (case (token value)
    (nil "") :agent
    ("action" "actions") :action
    ("actor" "actors") :actor
    ("agent" "agents" "agent-spec" "agent-specs" "contract" "contracts") :agent
    ("cap" "caps" "capability" "capabilities") :capability
    ("cms" "cms-config" "cms-configs" "cms-block-registry" "cms-template-registry" "cms-templates") :cms
    ("generator" "generators") :generator
    ("ingest-source" "ingest-sources" "ingest_source" "ingest_sources") :ingest-source
    ("model" "models") :model
    ("model-family" "model-families" "model_family" "model_families") :model-family
    ("pipeline" "pipelines") :pipeline
    ("policy" "policies") :policy
    ("role" "roles") :role
    ("rule" "rules") :rule
    ("runtime-feature" "runtime-features" "runtime_feature" "runtime_features" "runtime") :runtime-feature
    ("schedule" "schedules") :schedule
    ("source" "sources" "runtime-source" "runtime-sources" "runtime_source" "runtime_sources") :source
    ("source-mode" "source-modes" "source_mode" "source_modes") :source-mode
    ("sub-agent" "sub-agents" "sub_agent" "sub_agents") :sub-agent
    ("trigger" "triggers") :trigger
    ("user" "users" "human" "humans") :user
    ("workflow" "workflows") :workflow
    (throw (js/Error. (str "Unknown resource kind: " value)))))

(defn resource-class
  [resource-kind]
  (get resource-kind->class (normalize-resource-kind resource-kind)))

(defn- resource-kind-from-class
  [class-name]
  (or (get class->resource-kind class-name)
      (keyword (str/replace (str class-name) #"_" "-"))))

(defn resource-record
  [record]
  {:resource/id (:id record)
   :resource/kind (resource-kind-from-class (:contractClass record))
   :resource/class (:contractClass record)
   :resource/definition (:contract record)
   :resource/file-path (:file-path record)
   :resource/edn-text (:edn-text record)
   :id (:id record)
   :resourceClass (:contractClass record)
   ;; Transitional compatibility keys for old callers.
   :contractClass (:contractClass record)
   :contract (:contract record)
   :file-path (:file-path record)
   :edn-text (:edn-text record)
   :ok? (:ok? record)})

(defn load-all-resources-sync
  [config]
  (mapv resource-record (contract-loader/load-all-contracts-sync config)))

(defn ^:async load-all-resources!
  [config]
  (mapv resource-record (await (contract-loader/load-all-contracts! config))))

(defn resource-record-sync
  [config resource-kind resource-id]
  (let [class-name (resource-class resource-kind)
        wanted-id (some-> resource-id str str/trim not-empty)]
    (some (fn [record]
            (when (and (= class-name (:resource/class record))
                       (= wanted-id (:resource/id record)))
              record))
          (load-all-resources-sync config))))

(defn resource-sync
  [config resource-kind resource-id]
  (some-> (resource-record-sync config resource-kind resource-id)
          :resource/definition))

(defn list-resource-ids-sync
  [config resource-kind]
  (let [class-name (resource-class resource-kind)]
    (->> (load-all-resources-sync config)
         (filter #(= class-name (:resource/class %)))
         (map :resource/id)
         distinct
         sort
         vec)))

(defn ^:async list-resource-ids!
  [config resource-kind]
  (let [class-name (resource-class resource-kind)
        records (await (load-all-resources! config))]
    (->> records
         (filter #(= class-name (:resource/class %)))
         (map :resource/id)
         distinct
         sort
         vec)))

(defn safe-resource-id!
  [resource-id]
  (contract-loader/safe-path-segment! resource-id "resource-id"))

(defn resource-root-paths
  [config]
  (contract-loader/contract-root-paths config))

(defn resource-file-path
  ([config resource-id]
   (resource-file-path config :agent resource-id))
  ([config resource-kind resource-id]
   (contract-loader/contract-file-path config (resource-class resource-kind) resource-id)))

(defn write-edn-file!
  [file-path edn-text]
  (contract-loader/write-edn-file! file-path edn-text))

(defn read-edn-file!
  [file-path]
  (contract-loader/read-edn-file! file-path))

(defn invalidate-sync-resource-cache!
  []
  (contract-loader/invalidate-sync-contract-cache!))
