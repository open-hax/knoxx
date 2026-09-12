(ns knoxx.backend.domain.resources.presentation
  "Project resource records into the established human and agent response data."
  (:require [knoxx.backend.domain.resources.loader :as resources]))

(defn- wire-key
  [field-key]
  (if (keyword? field-key)
    (if-let [key-ns (namespace field-key)]
      (str key-ns "/" (name field-key))
      (name field-key))
    field-key))

(defn wire-value
  "Project nested resource data into the established HTTP scalar and collection shapes."
  [value]
  (cond
    (keyword? value) (wire-key value)
    (symbol? value) (wire-key value)
    (map? value) (into {} (map (fn [[k v]] [(wire-key k) (wire-value v)])) value)
    (set? value) (mapv wire-value value)
    (sequential? value) (mapv wire-value value)
    :else value))

(defn- keywordish-name
  [value]
  (cond
    (keyword? value) (wire-key value)
    (symbol? value) (wire-key value)
    (some? value) (str value)
    :else nil))

(defn- record-definition
  [record]
  (or (:resource/definition record)
      (:contract record)))

(defn- record-class
  [record]
  (or (:resource/class record)
      (:contractClass record)))

(defn- record-kind
  [record]
  (or (:resource/kind record)
      (some-> (record-class record) resources/normalize-resource-kind)))

(defn- trigger-summary
  [record]
  (let [resource (record-definition record)
        events (wire-value (:trigger/events resource))]
    {:kind   (keywordish-name (:trigger/kind resource))
     :target (or (:trigger/target resource)
                 (get-in resource [:trigger/with :agent-id])
                 (:trigger/agent resource))
     :events events
     :source {:events events}
     :filters (wire-value (get-in resource [:data :filters]))
     :context (wire-value (get-in resource [:data :context]))}))

(defn- action-summary
  [record]
  {:handler (get-in (record-definition record) [:action/handler])})

(defn resource-list-summary
  "Describe one indexed resource with its class-specific trigger or action summary."
  [record]
  (let [resource-class (record-class record)
        resource-kind (record-kind record)
        summary {:id (:resource/id record)
                 :resource/id (:resource/id record)
                 :resource/kind resource-kind
                 :resourceClass resource-class
                 :kind (some-> resource-kind name)
                 :path (str resource-class "/" (:resource/id record) ".edn")}]
    (cond-> summary
      (= :trigger resource-kind)
      (assoc :trigger (trigger-summary record))

      (= :action resource-kind)
      (assoc :action (action-summary record)))))

(defn contract-list-summary
  "Compatibility summary for old /contracts clients."
  [record]
  (assoc (resource-list-summary record)
         :contractClass (record-class record)))

(defn wire-validation
  "Encode a successful parsed resource while preserving validation diagnostics."
  [validation]
  (cond-> validation
    (:contract validation) (update :contract wire-value)))
