(ns knoxx.backend.law.run-store
  "Finite run persistence contracts, independent from any database provider."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(def NonBlank [:and :string [:fn #(not (str/blank? %))]])
(def Milliseconds [:int {:min 0 :max 8640000000000000}])
(def Instant [:re #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$"])
(def Event [:map [:run_id NonBlank] [:session_id NonBlank] [:conversation_id NonBlank]
            [:type NonBlank] [:at Instant]])
(def Stamp [:map {:closed true} [:at Instant] [:at-ms Milliseconds]
            [:expires-ms Milliseconds] [:instance-id NonBlank]])
(def Operation [:map {:closed true} [:kind [:enum :put :patch :delete :event]]
                [:run-id NonBlank] [:stamp Stamp]
                [:run {:optional true} :map] [:patch {:optional true} :map]
                [:event {:optional true} Event] [:event-id {:optional true} NonBlank]])
(def ttl-ms (* 2 60 60 1000))

(defn require!
  "Reject invalid run persistence data at admission and replay boundaries."
  [schema value]
  (when-not (m/validate schema value)
    (throw (ex-info "Invalid run persistence value" {:status 400 :code "run_store_invalid"})))
  value)

(defn conflict!
  "Refuse mutation of an already accepted run or event identity."
  [message]
  (throw (ex-info message {:status 409 :code "run_store_identity_conflict"})))
