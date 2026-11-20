(ns knoxx.backend.law.graph-query
  "Contracts for bounded, read-only structural graph queries."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(def registry
  "Named graph query boundary shapes."
  {::request [:map
              [:q [:string {:min 1 :max 512}]]
              [:lakes {:optional true} [:vector {:max 32} [:string {:min 1 :max 128}]]]
              [:nodeTypes {:optional true} [:vector {:max 32} [:string {:min 1 :max 128}]]]
              [:k {:optional true} [:int {:min 1 :max 60}]]
              [:edgeLimit {:optional true} [:int {:min 0 :max 60}]]
              [:includeText {:optional true} :boolean]]
   ::result [:map
             [:query :string]
             [:mode [:= "structural"]]
             [:nodes [:vector :map]]
             [:edges [:vector :map]]
             [:clusters [:vector :map]]
             [:stats :map]]})

(def request-valid?
  "Executable request validator."
  (m/validator ::request {:registry (merge (m/default-schemas) registry)}))

(def result-valid?
  "Executable result validator."
  (m/validator ::result {:registry (merge (m/default-schemas) registry)}))

(defn request!
  "Validate before building Mongo selectors; reject blank text as a bad request."
  [payload]
  (when-not (and (request-valid? payload) (not (str/blank? (:q payload))))
    (throw (ex-info "Invalid structural graph query" {:status 400 :code :graph-query-invalid})))
  payload)

(defn result!
  "Reject a malformed graph result at the domain boundary."
  [result]
  (when-not (result-valid? result)
    (throw (ex-info "Invalid structural graph response" {:status 500 :code :graph-query-result-invalid})))
  result)
