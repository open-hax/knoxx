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
   [:source/driver {:optional true} KeywordOrString]
   [:source/enabled? {:optional true} :boolean]
   [:tenant/id {:optional true} :string]
   [:source/discovery {:optional true}
    [:map {:closed false}
     [:hidden-policy {:optional true} HiddenPolicy]
     [:skip-dirs {:optional true} [:set :string]]
     [:skip-files {:optional true} [:set :string]]
     [:skip-extensions {:optional true} [:set :string]]
     [:text-filenames {:optional true} [:set :string]]
     [:follow-symlinks? {:optional true} :boolean]]]
   [:source/schedule {:optional true}
    [:map {:closed false}
     [:mode {:optional true} ScheduleMode]
     [:sync-interval-minutes {:optional true} pos-int?]
     [:bootstrap? {:optional true} :boolean]]]
   [:source/semantic {:optional true}
    [:map {:closed false}
     [:enabled? {:optional true} :boolean]
     [:build-index? {:optional true} :boolean]
     [:chunk-size {:optional true} pos-int?]
     [:chunk-overlap {:optional true} nat-int?]]]
   [:source/sink {:optional true}
    [:map {:closed false}
     [:type {:optional true} SinkType]]]])
