(ns kms-ingestion.contracts.schema
  "Schema for contract-driven ingestion configuration."
  (:require
   [clojure.string :as str]))

(def NonBlankString
  [:and :string [:fn (fn [s] (not (str/blank? s)))]] )

(def KeywordOrString
  [:or :keyword NonBlankString])

(def SecretRef
  [:or
   [:keyword]
   [:and :string [:fn (fn [s]
                        (or (str/starts-with? s ":env/")
                            (str/starts-with? s "env:")))]]]
)

(def ScheduleMode
  [:enum :poll :watch :manual :hybrid :passive])

(def SinkType
  [:enum :openplanner :ragussy :none])

(def HiddenPolicy
  [:enum :skip :include])

(def IngestSourceContract
  [:map
   [:contract/id   {:optional true} KeywordOrString]
   [:contract/type {:optional true} KeywordOrString]
   [:contract/version {:optional true} pos-int?]
   [:source/id {:optional true} KeywordOrString]
   [:source/name {:optional true} NonBlankString]
   [:source/driver {:optional true} KeywordOrString]
   [:source/enabled? {:optional true} :boolean]
   [:tenant/id {:optional true} :string]
   [:source/config {:optional true}
    [:map {:closed false}
     [:root-path {:optional true} NonBlankString]
     [:root_path {:optional true} NonBlankString]]]
   [:source/discovery {:optional true}
    [:map {:closed false}
     [:hidden-policy {:optional true} HiddenPolicy]
     [:skip-dirs {:optional true} [:set :string]]
     [:skip-files {:optional true} [:set :string]]
     [:skip-extensions {:optional true} [:set :string]]
     [:text-extensions {:optional true} [:set :string]]
     [:text-filenames {:optional true} [:set :string]]
     [:follow-symlinks? {:optional true} :boolean]
     [:file-types {:optional true} [:vector :string]]
     [:include-patterns {:optional true} [:vector :string]]
     [:exclude-patterns {:optional true} [:vector :string]]]]
   [:source/schedule {:optional true}
    [:map {:closed false}
     [:mode {:optional true} ScheduleMode]
     [:sync-interval-minutes {:optional true} pos-int?]
     [:passive-watch-enabled? {:optional true} :boolean]
     [:bootstrap? {:optional true} :boolean]]]
   [:source/semantic {:optional true}
    [:map {:closed false}
     [:enabled? {:optional true} :boolean]
     [:build-index? {:optional true} :boolean]
     [:chunk-size {:optional true} pos-int?]
     [:chunk-overlap {:optional true} nat-int?]]]
   [:source/sink {:optional true}
    [:map {:closed false}
     [:type {:optional true} SinkType]
     [:collections {:optional true} [:vector :string]]]]])
