(ns knoxx.backend.law.source-authoring
  "Contracts for durable source revisions and their filesystem projections."
  (:require [knoxx.backend.law.publication :as publication]
            [knoxx.backend.law.source-review :as review]))

(def Event
  "Full immutable source bytes make a failed projection repairable."
  [:map {:closed true}
   [:source/id review/NonBlank]
   [:source/scope review/Scope]
   [:source/actor review/Actor]
   [:source/action [:enum :observe :create :save]]
   [:source/previous-revision [:maybe review/NonBlank]]
   [:source/revision review/NonBlank]
   [:source/content review/NonBlank]
   [:source/document publication/Document]
   [:source/manifest {:optional true} :map]
   [:source/recorded-at review/NonBlank]])

(def SaveCommand
  "Only source text changes; resource identity and metadata cannot be replaced."
  [:map {:closed true}
   [:operation-id review/NonBlank]
   [:expected-revision review/NonBlank]
   [:content review/NonBlank]])

(def CreateCommand
  "Creation chooses declared publication destinations, never ambient defaults."
  [:map {:closed true}
   [:operation-id review/NonBlank]
   [:title review/NonBlank]
   [:content review/NonBlank]
   [:source-locale :keyword]
   [:garden qualified-keyword?]
   [:target-locales [:vector {:min 1} :keyword]]])
