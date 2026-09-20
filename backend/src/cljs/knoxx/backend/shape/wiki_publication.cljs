(ns knoxx.backend.shape.wiki-publication
  "Explicit publication receipt JSON projection without qualified-key collisions."
  (:require [knoxx.backend.shape.resource-identity :as identity]))

(defn- fields
  [receipt mapping]
  (reduce-kv (fn [wire source target]
               (if (contains? receipt source)
                 (assoc wire target (get receipt source))
                 wire))
             {} mapping))

(defn- conflict-wire
  "Conflict evidence retains fully qualified keys because it is extensible."
  [value]
  (cond
    (map? value) (reduce-kv (fn [out k v]
                             (assoc out (if (keyword? k) (identity/encode-keyword k) k)
                                    (conflict-wire v))) {} value)
    (sequential? value) (mapv conflict-wire value)
    :else value))

(defn receipt->wire
  "Keep resource identities distinct and materialization separate from intent."
  [receipt]
  (identity/encode-wire-values
   (cond-> (merge (fields receipt {:receipt/type :type :publication/id :publication
                            :adapter/id :adapter :document/id :document
                            :idempotency/key :key :target :target :locale :locale
                            :revision :revision :path :path :reason :reason
                            :blockers :blockers})
                   (fields receipt {:correlation/publication :publication
                                    :correlation/revision :revision
                                    :correlation/trigger :trigger :correlation/origin :origin}))
     (contains? receipt :materialized/revision)
     (assoc :materialized (fields receipt {:materialized/revision :revision
                                           :materialized/path :path
                                           :materialized/title :title}))
     (contains? receipt :removed/path)
     (assoc :removed {:path (:removed/path receipt)})
     (contains? receipt :failure/reason)
     (assoc :failure (cond-> (fields receipt {:failure/reason :reason :failure/drift? :drift})
                       (contains? receipt :failure/conflict)
                       (assoc :conflict (conflict-wire (:failure/conflict receipt))))))))
