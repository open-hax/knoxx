(ns knoxx.backend.domain.social.api-schema
  "Compact, bounded request guidance derived from pinned API definitions."
  (:require [knoxx.backend.law.platform-payload :as payload]))

(def ^:private constraint-keys
  ["type" "description" "required" "enum" "const" "minimum" "maximum"
   "minLength" "maxLength" "minItems" "maxItems" "format" "additionalProperties"])

(defn- preview
  [asset document-id raw-schema remaining seen]
  (let [reference (or (get raw-schema "$ref") (get raw-schema "ref"))
        [document-id schema] (payload/resolve-schema asset document-id raw-schema)
        selected (merge (select-keys schema constraint-keys) (select-keys raw-schema ["description"]))
        selected (cond-> selected reference (assoc "reference" reference))
        descend #(preview asset document-id % (dec remaining) (conj seen [document-id reference]))]
    (if (or (zero? remaining) (and reference (contains? seen [document-id reference])))
      (assoc selected "viewLimited" true)
      (cond-> selected
        (get schema "properties") (assoc "properties" (into (sorted-map) (map (fn [[k v]] [k (descend v)]))
                                                                            (get schema "properties")))
        (get schema "items") (assoc "items" (descend (get schema "items")))
        (get schema "oneOf") (assoc "oneOf" (mapv descend (get schema "oneOf")))
        (get schema "anyOf") (assoc "anyOf" (mapv descend (get schema "anyOf")))
        (get schema "allOf") (assoc "allOf" (mapv descend (get schema "allOf")))))))

(defn- limit-view
  [value budget]
  (cond
    (and (map? value) (<= budget 0)) [(assoc (select-keys value constraint-keys) "viewLimited" true
                                           "reference" (get value "reference")) 0]
    (map? value) (reduce (fn [[out left] [key item]]
                          (let [[item left] (limit-view item left)] [(assoc out key item) left]))
                        [{} (dec budget)] (sort-by key value))
    (vector? value) (reduce (fn [[out left] item]
                             (let [[item left] (limit-view item left)] [(conj out item) left]))
                           [[] budget] value)
    :else [value budget]))

(defn request-guide
  "Expand request references without a response-schema detour. Each top-level
   field gets its own bounded view so a large components family cannot hide a
   later poll field. Complete definitions stay available through reference."
  [asset document-id operation-schema]
  (when-let [schema (or (get-in operation-schema ["requestBody" "content" "application/json" "schema"])
                        (get-in operation-schema ["input" "schema"])
                        (when (= "record" (get operation-schema "type")) (get operation-schema "record")))]
    (let [expanded (preview asset document-id schema 7 #{})
          fields (get expanded "properties")]
      {:schema (if fields
                 (assoc expanded "properties" (into (sorted-map) (map (fn [[key view]] [key (first (limit-view view 24))])) fields))
                 (first (limit-view expanded 96)))
       :guidance "Send actual nested JSON. Descriptions supply units. Required fields apply inside each supplied object. A text or emoji imitation does not establish a native platform feature. For viewLimited references, call social.describe with reference. The platform validates remaining semantics."})))
