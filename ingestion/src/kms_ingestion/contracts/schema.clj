(ns kms-ingestion.contracts.schema
  "Schema for contract-driven ingestion configuration."
  (:require
   [malli.core :as m]))

(def NonBlankString
  [:and :string [:fn (fn [s] (not (clojure.string/blank? s)))]] )

(def KeywordOrString
  [:or :keyword NonBlankString])

(def SecretRef
  [:or
   [:keyword]
   [:and :string [:fn (fn [s]
                        (or (.startsWith s ":env/")
                            (.startsWith s "env:")))]]])

(def ScheduleMode
  [:enum :poll :watch :manual :hybrid])

(def SinkType
  [:enum :openplanner :ragussy :event-log :qdrant])

(def BackpressureStrategy
  [:enum :exponential :fixed :none])

(def SemanticBuildMode
  [:enum :incremental :off])

(def HiddenPolicy
  [:enum :skip :include :explicit-only])

(def DriverType
  [:or :keyword NonBlankString])

(def Discovery
  [:map {:closed true}
   [:file-types {:optional true} [:vector NonBlankString]]
   [:include-patterns {:optional true} [:vector NonBlankString]]
   [:exclude-patterns {:optional true} [:vector NonBlankString]]
   [:text-extensions {:optional true} [:set NonBlankString]]
   [:text-filenames {:optional true} [:set NonBlankString]]
   [:skip-dirs {:optional true} [:set NonBlankString]]
   [:skip-files {:optional true} [:set NonBlankString]]
   [:skip-extensions {:optional true} [:set NonBlankString]]
   [:hidden-policy {:optional true} HiddenPolicy]
   [:follow-symlinks? {:optional true} :boolean]])

(def Schedule
  [:map {:closed true}
   [:mode ScheduleMode]
   [:sync-interval-minutes {:optional true} pos-int?]
   [:scheduler-poll-ms {:optional true} pos-int?]
   [:passive-watch-enabled? {:optional true} :boolean]
   [:passive-watch-poll-ms {:optional true} pos-int?]
   [:passive-watch-debounce-ms {:optional true} pos-int?]
   [:bootstrap? {:optional true} :boolean]])

(def Ingest
  [:map {:closed true}
   [:batch-size {:optional true} pos-int?]
   [:batch-parallelism {:optional true} pos-int?]
   [:batch-delay-ms {:optional true} nat-int?]
   [:throttle-enabled? {:optional true} :boolean]
   [:max-load-per-core {:optional true} pos?]
   [:throttle-sleep-ms {:optional true} nat-int?]
   [:retry-failed? {:optional true} :boolean]])

(def Sink
  [:map {:closed true}
   [:type SinkType]
   [:target-ref {:optional true} KeywordOrString]
   [:collections {:optional true} [:vector NonBlankString]]
   [:lake {:optional true} NonBlankString]
   [:visibility {:optional true} NonBlankString]
   [:source-label {:optional true} NonBlankString]
   [:created-by {:optional true} NonBlankString]
   [:language {:optional true} NonBlankString]
   [:model {:optional true} NonBlankString]])

(def Semantic
  [:map {:closed true}
   [:enabled? {:optional true} :boolean]
   [:build-mode {:optional true} SemanticBuildMode]
   [:k {:optional true} pos-int?]
   [:min-similarity {:optional true} number?]
   [:emit-graph-events? {:optional true} :boolean]])

(def Translation
  [:map {:closed true}
   [:enabled? {:optional true} :boolean]
   [:model {:optional true} NonBlankString]
   [:poll-ms {:optional true} pos-int?]])

(def Projection
  [:map {:closed true}
   [:domain-strategy {:optional true} KeywordOrString]
   [:document-kind-strategy {:optional true} KeywordOrString]
   [:visibility {:optional true} NonBlankString]
   [:language {:optional true} NonBlankString]
   [:created-by {:optional true} NonBlankString]
   [:source {:optional true} NonBlankString]
   [:metadata-template {:optional true} :map]])

(def Backpressure
  [:map {:closed true}
   [:strategy {:optional true} BackpressureStrategy]
   [:base-delay-ms {:optional true} nat-int?]
   [:max-delay-ms {:optional true} nat-int?]
   [:failure-window {:optional true} pos-int?]
   [:respect-remote? {:optional true} :boolean]])

(def SourceConfig
  [:map-of :keyword any?])

(def IngestSourceContract
  [:map {:closed true}
   [:contract/id KeywordOrString]
   [:contract/type [:= :ingest/source]]
   [:contract/version {:optional true} pos-int?]
   [:tenant/id NonBlankString]
   [:source/id KeywordOrString]
   [:source/name NonBlankString]
   [:source/enabled? {:optional true} :boolean]
   [:source/driver DriverType]
   [:source/config {:optional true} SourceConfig]
   [:source/discovery {:optional true} Discovery]
   [:source/schedule {:optional true} Schedule]
   [:source/ingest {:optional true} Ingest]
   [:source/sink {:optional true} Sink]
   [:source/semantic {:optional true} Semantic]
   [:source/translation {:optional true} Translation]
   [:source/projection {:optional true} Projection]
   [:source/backpressure {:optional true} Backpressure]])

(defn valid-ingest-source-contract?
  [x]
  (m/validate IngestSourceContract x))

(defn explain-ingest-source-contract
  [x]
  (m/explain IngestSourceContract x))
