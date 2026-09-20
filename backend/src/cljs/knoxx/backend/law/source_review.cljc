(ns knoxx.backend.law.source-review
  "Closed contracts for revision-bound source review and reusable writing lessons."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(def NonBlank
  "Text with at least one visible character."
  [:and :string [:fn #(not (str/blank? %))]])

(def Scope
  "Review authority belongs to one organization, project and resource document."
  [:map {:closed true}
   [:org-id NonBlank]
   [:project [:maybe NonBlank]]
   [:document qualified-keyword?]])

(def Actor
  "An authenticated principal supplied by application composition, never the wire."
  [:map {:closed true}
   [:id NonBlank]
   [:kind [:enum :human :agent :service :automation]]])

(def Snapshot
  "One observed resource source, including its declared language."
  [:map {:closed true}
   [:document qualified-keyword?]
   [:revision NonBlank]
   [:source-locale :keyword]
   [:title :string]
   [:content NonBlank]])

(def Correction
  "A review proposal. Applying it requires a separate source revision edit."
  [:map {:closed true}
   [:before NonBlank]
   [:after :string]
   [:reason NonBlank]])

(def Action
  "Review actions shared by human controls and agent tools."
  [:enum :comment :submit :request-changes :accept])

(def Command
  "Caller-stable command; authority is supplied separately."
  [:map {:closed true}
   [:operation-id NonBlank]
   [:revision NonBlank]
   [:source-locale :keyword]
   [:expected-head [:maybe NonBlank]]
   [:action Action]
   [:notes {:optional true} :string]
   [:corrections {:optional true} [:vector {:max 100} Correction]]
   [:lessons {:optional true} [:vector {:max 100} NonBlank]]])

(def Event
  "One immutable accepted review operation; causal order is explicit."
  [:map {:closed true}
   [:review/id NonBlank]
   [:review/scope Scope]
   [:review/actor Actor]
   [:review/revision NonBlank]
   [:review/source-locale :keyword]
   [:review/previous [:maybe NonBlank]]
   [:review/action Action]
   [:review/notes :string]
   [:review/corrections [:vector {:max 100} Correction]]
   [:review/lessons [:vector {:max 100} NonBlank]]
   [:review/recorded-at NonBlank]])

(defn assert-valid!
  "Validate a named boundary and return its value, or a classified refusal."
  [contract schema value]
  (when-not (m/validate schema value)
    (throw (ex-info "invalid source review contract"
                    {:status 400 :code "source_review_invalid" :contract contract})))
  value)
