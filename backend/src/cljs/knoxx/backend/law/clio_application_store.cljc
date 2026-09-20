(ns knoxx.backend.law.clio-application-store
  "Contracts for accepted application operations persisted in canonical Clio."
  (:require [clio.law.schema :as clio-schema]
            [clojure.string :as str]
            [malli.core :as m]))

(def Operation
  "An accepted method invocation and the answer witnessed before its durable append."
  [:map {:closed true}
   [:operation/id [:string {:min 1}]]
   [:operation/method :qualified-keyword]
   [:operation/args [:vector :any]]
   [:operation/result :any]])

(def catalog
  "The complete envelope is versioned with the operation contract by Clio."
  {:knoxx.application/operation-accepted
   (clio-schema/event-schema :knoxx.application/operation-accepted Operation)})

(defn assert-operation!
  "Check one accepted operation at the persistence and replay boundary."
  [operation]
  (when-not (m/validate Operation operation)
    (throw (ex-info "invalid Clio application operation"
                    {:cause :clio-application/invalid-operation})))
  operation)

(defn assert-directory!
  "Require an explicit data directory, never a temporary or Mongo fallback."
  [directory]
  (when-not (and (string? directory) (not (str/blank? directory)))
    (throw (ex-info "Clio application store requires an explicit directory"
                    {:cause :clio-application/missing-directory})))
  directory)

(defn assert-invocation!
  "Refuse methods outside the provider's declared protocol operation table."
  [operations method args]
  (when-not (and (qualified-keyword? method) (vector? args)
                 (fn? (get operations method)))
    (throw (ex-info "unsupported Clio application operation"
                    {:cause :clio-application/unsupported-operation
                     :method method})))
  args)
