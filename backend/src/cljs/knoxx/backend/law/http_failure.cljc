(ns knoxx.backend.law.http-failure
  "Portable data contract for a classified HTTP transport failure."
  (:require [malli.core :as m]))

(def HttpFailure
  "The status, stable code and message supplied to a native HTTP error."
  [:map {:closed true}
   [:status [:int {:min 400 :max 599}]]
   [:code [:string {:min 1}]]
   [:message :string]])

(defn validate!
  "Refuse malformed transport failure data before constructing a host error."
  [failure]
  (when-not (m/validate HttpFailure failure)
    (throw (ex-info "Invalid HTTP failure" {:type :http/invalid-failure})))
  failure)
