(ns knoxx.backend.law.source-authoring
  "Contracts for durable source revisions and their filesystem projections."
  (:require [knoxx.backend.law.publication :as publication]
            [knoxx.backend.law.source-review :as review]
            [malli.core :as m]))

(def CreationManifest
  "A source document followed by its declared publication resources."
  [:map
   [:namespace :keyword]
   [:resources [:and [:vector :map]
                [:cat publication/Document [:+ publication/PublicationIntentResource]]]]])

(defn- creation-manifest-valid?
  [event]
  (or (not= :create (:source/action event))
      (let [manifest (:source/manifest event)
            resources (:resources manifest)
            document (:source/document event)]
        (and (m/validate CreationManifest manifest)
             (= document (first resources))
             (every? #(= (:document/id document) (:publication/document %)) (rest resources))))))

(def Event
  "Full immutable source bytes and each creation manifest make projection repairable."
  [:and
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
    [:source/recorded-at review/NonBlank]]
   [:fn {:error/message "a create manifest must contain its exact source document and valid publication references"}
    creation-manifest-valid?]])

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
