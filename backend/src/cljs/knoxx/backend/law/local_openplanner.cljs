(ns knoxx.backend.law.local-openplanner
  "Contracts for finite local OpenPlanner facts and scoped queries."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]))

(def NonBlank [:and string? [:fn #(not (str/blank? %))]])
(def Finite [:and number? [:fn #(and (= % %) (< ##-Inf % ##Inf))]])
(def Scope [:map [:org_id NonBlank] [:project {:optional true} [:maybe string?]]])
(def Event
  [:map [:id NonBlank] [:text string?] [:kind NonBlank] [:source NonBlank]
   [:ts NonBlank] [:extra [:map [:org_id NonBlank]]]
   [:source_ref {:optional true} map?] [:meta {:optional true} map?]])
(def Projection
  [:map {:closed true} [:id NonBlank] [:digest NonBlank] [:model NonBlank]
   [:dimensions [:int {:min 1}]] [:embedding [:vector {:min 1} Finite]]])
(def Options
  [:map [:directory NonBlank] [:now! {:optional true} fn?]
   [:embed! {:optional true} fn?] [:embedding-config {:optional true} map?]])

(defn assert-valid!
  "Validate a local boundary before performing I/O or accepting a fact."
  [schema value]
  (when-not (m/validate schema value)
    (throw (ex-info "Invalid local OpenPlanner data"
                    {:status 400 :code "openplanner_local_invalid"
                     :errors (me/humanize (m/explain schema value))})))
  value)

(defn scope!
  "Require explicit organization scope; never infer it from a query or default."
  [value]
  (when-not (m/validate Scope value)
    (throw (ex-info "OpenPlanner organization scope is required"
                    {:status 403 :code "openplanner_scope_required"})))
  value)

(defn embedding-configured!
  "All embedding settings may be absent; partial configuration is invalid."
  [config]
  (let [url (:embed-provider-base-url config)
        model (:embed-provider-model config)
        dimensions (:embed-provider-dimensions config)
        present? #(and (some? %) (not= "" %))]
    (if-not (some present? [url model dimensions])
      false
      (do
        (assert-valid! [:map [:url NonBlank] [:model NonBlank]
                        [:dimensions [:int {:min 1}]]]
                       {:url url :model model :dimensions dimensions})
        true))))

(defn projection!
  "Reject empty, nonfinite or incorrectly dimensioned persisted vectors."
  [value]
  (assert-valid! Projection value)
  (when-not (= (:dimensions value) (count (:embedding value)))
    (throw (ex-info "OpenPlanner vector has the wrong dimensions"
                    {:status 500 :code "openplanner_vector_invalid"})))
  value)
