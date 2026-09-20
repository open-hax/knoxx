(ns knoxx.backend.domain.resources.editing
  "Normalize and safely rewrite resource identities without changing surrounding EDN."
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [knoxx.backend.domain.resources.loader :as resources]))

(defn normalize-resource-class
  "Normalize a resource kind or class through the canonical resource grammar."
  [raw]
  (resources/resource-class raw))

(defn normalize-contract-class
  "Compatibility alias for old contract route clients."
  [raw]
  (normalize-resource-class raw))

(defn- model-id->slug
  [model-id]
  (some-> model-id
          str
          (str/replace #"[^A-Za-z0-9._-]+" "_")
          (str/replace #"_+" "_")))

(defn- qualified-id-with-local-slug
  [value new-id]
  (let [existing-ns (when (keyword? value) (namespace value))
        local-name (str/replace (str new-id) #"_" "-")]
    (keyword existing-ns local-name)))

(defn- replace-qualified-resource-id
  [edn-text id-key pattern new-id]
  (let [parsed (reader/read-string edn-text)
        value (qualified-id-with-local-slug (get parsed id-key) new-id)]
    (str/replace edn-text pattern (str id-key " " value))))

(defn parsed-resource-id
  "Read the route-facing identity for resource classes with string or slug IDs."
  [resource-class value]
  (case (normalize-resource-class resource-class)
    "agents" (some-> (:contract/id value) str)
    "policies" (some-> (:contract/id value) str)
    "sources" (some-> (:contract/id value) str)
    "actions" (some-> (:contract/id value) str)
    "triggers" (some-> (:contract/id value) str)
    "sub_agents" (some-> (:contract/id value) str)
    "actors" (some-> (:actor/id value) str)
    "roles" (some-> (:role/id value) name)
    "capabilities" (some-> (:cap/id value) name)
    "model_families" (some-> (:model-family/id value) str)
    "models" (some-> (:model/id value) model-id->slug)
    nil))

(defn safe-resource-id
  "Validate an ID for safe resource paths and return a data refusal on failure."
  [raw-id]
  (try
    {:ok true
     :id (resources/safe-resource-id! raw-id)}
    (catch :default err
      {:ok false
       :error (or (ex-message err) (str err))})))

(defn safe-contract-id
  "Compatibility alias for old contract route clients."
  [raw-id]
  (safe-resource-id raw-id))

(defn safe-resource-class
  "Validate a resource class and return its canonical spelling or refusal."
  [raw-class]
  (try
    {:ok true
     :class (normalize-resource-class raw-class)}
    (catch :default err
      {:ok false
       :error (or (ex-message err) (str err))})))

(defn safe-contract-class
  "Compatibility alias for old contract route clients."
  [raw-class]
  (safe-resource-class raw-class))

(def ^:private string-id-fields
  {"agents" [:contract/id #":contract/id\s+\"[^\"]+\""]
   "policies" [:contract/id #":contract/id\s+\"[^\"]+\""]
   "actors" [:actor/id #":actor/id\s+\"[^\"]+\""]
   "model_families" [:model-family/id #":model-family/id\s+\"[^\"]+\""]
   "models" [:model/id #":model/id\s+\"[^\"]+\""]})

(defn- replace-id-text [edn-text id-key pattern rendered-id]
  (let [replacement (str id-key " " rendered-id)]
    (if (str/includes? edn-text (str id-key))
      (str/replace edn-text pattern replacement)
      (str replacement "\n" edn-text))))

(defn- replace-string-id [edn-text [id-key pattern] new-id]
  (replace-id-text edn-text id-key pattern (str "\"" new-id "\"")))

(defn- replace-source-id [edn-text new-id]
  (cond-> (replace-string-id edn-text (get string-id-fields "agents") new-id)
    (str/includes? edn-text ":source/id")
    (str/replace #":source/id\s+:[^\s\]}]+"
                 (str ":source/id :source/" (str/replace (str new-id) #"_" "-")))))

(defn update-resource-id-in-edn-text
  "Rewrite the copied resource's own identity while preserving its namespace."
  [resource-class edn-text new-id]
  (let [resource-class (normalize-resource-class resource-class)]
    (if-let [field (get string-id-fields resource-class)]
      (replace-string-id edn-text field
                         (if (= resource-class "models") (str/replace (str new-id) #"_" ":") new-id))
      (case resource-class
        "sources" (replace-source-id edn-text new-id)
        "roles" (replace-id-text edn-text :role/id #":role/id\s+:[^\s\]}]+"
                                 (str ":role/" (str/replace new-id #"_" "-")))
        "capabilities" (replace-id-text edn-text :cap/id #":cap/id\s+:[^\s\]}]+"
                                        (str ":cap/" (str/replace (str/replace (str new-id) #"^cap_" "") #"_" "-")))
        "documents" (replace-qualified-resource-id edn-text :document/id #":document/id\s+:[^\s\]}]+" new-id)
        "gardens" (replace-qualified-resource-id edn-text :garden/id #":garden/id\s+:[^\s\]}]+" new-id)
        "publications" (replace-qualified-resource-id edn-text :publication/id #":publication/id\s+:[^\s\]}]+" new-id)
        edn-text))))
