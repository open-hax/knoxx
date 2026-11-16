(ns knoxx.backend.domain.social.api-request
  "Plan schema-selected platform reads without URLs, credentials or I/O."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.social.api-catalog :as catalog]))

(defn- refuse!
  [code message]
  (throw (ex-info message {:code code})))

(defn- local-reference
  [document schema]
  (if-let [reference (get schema "$ref")]
    (if (str/starts-with? reference "#/")
      (or (get-in document (mapv #(-> % (str/replace "~1" "/") (str/replace "~0" "~"))
                                (str/split (subs reference 2) #"/")))
          (refuse! "platform_parameter_schema_missing" "The pinned local parameter definition is missing"))
      (refuse! "platform_parameter_schema_external" "An external parameter schema cannot be followed at runtime"))
    schema))

(defn- valid-value?
  [document raw-schema value depth]
  (when (< depth 8)
    (let [schema (local-reference document raw-schema)
          kind (get schema "type")
          choices (or (get schema "oneOf") (get schema "anyOf"))
          type-ok? (cond
                     choices (some #(valid-value? document % value (inc depth)) choices)
                     (vector? kind) (some #(valid-value? document (assoc schema "type" %) value (inc depth)) kind)
                     (= kind "string") (string? value)
                     (= kind "integer") (integer? value)
                     (= kind "number") (number? value)
                     (= kind "boolean") (boolean? value)
                     (= kind "array") (and (vector? value)
                                            (every? #(valid-value? document (get schema "items" {}) % (inc depth)) value))
                     (= kind "null") (nil? value)
                     :else true)]
      (and type-ok?
           (or (nil? (get schema "enum")) (some #{value} (get schema "enum")))
           (or (not (number? value))
               (and (or (nil? (get schema "minimum")) (<= (get schema "minimum") value))
                    (or (nil? (get schema "maximum")) (<= value (get schema "maximum")))))
           (or (not (string? value))
               (and (<= (get schema "minLength" 0) (count value) (get schema "maxLength" 8192))
                    (or (nil? (get schema "pattern"))
                        (boolean (re-find (re-pattern (get schema "pattern")) value)))))
           (or (not (vector? value))
               (<= (get schema "minItems" 0) (count value) (get schema "maxItems" 100)))))))

(defn- parameter-definitions
  [platform document operation schema]
  (if (= platform "discord")
    (->> (concat (get-in document ["paths" (get operation "route") "parameters"])
                 (get schema "parameters"))
         (map #(local-reference document %))
         (filter #(contains? #{"path" "query"} (get % "in")))
         (reduce #(assoc %1 (get %2 "name") %2) {}))
    (let [parameters (get schema "parameters") required (set (get parameters "required"))]
      (into {} (map (fn [[name type]]
                      [name {"name" name "in" "query" "required" (contains? required name) "schema" type}]))
            (get parameters "properties")))))

(defn- safe-path-value?
  [value]
  (and (or (string? value) (integer? value))
       (not (contains? #{"" "." ".."} (str value)))
       (not (re-find #"[/\\?#\u0000-\u001f\u007f]" (str value)))))

(defn- assert-readable!
  [platform selected schema]
  (when-not (and (= "query" (get selected "kind")) (= "GET" (get selected "method")))
    (refuse! "platform_operation_is_not_read" "social.read accepts GET/query operations; records, procedures and subscriptions need their execution interface"))
  (when (= (get selected "id") "com.atproto.server.getServiceAuth")
      (refuse! "platform_operation_mints_credential" "Service-auth token issuance is a credential operation, not a read"))
  (let [encoding (if (= platform "discord")
                   (get-in schema ["responses" "200" "content"])
                   {(get-in schema ["output" "encoding"]) true})]
    (when-not (contains? encoding "application/json")
      (refuse! "platform_read_encoding_unsupported" "This operation returns binary or another encoding; use its media/export interface"))))

(defn- assert-bindings!
  [document definitions parameters supplied path-keys]
  (when-not (= (count parameters) (count supplied))
    (refuse! "platform_parameter_duplicate" "Supply each named parameter once; array values represent repeated query values"))
  (doseq [[name definition] definitions]
      (when (and (or (get definition "required") (contains? path-keys name))
                 (not (contains? supplied name)))
        (refuse! "platform_parameter_required" (str "Missing required parameter: " name))))
  (doseq [[name value] supplied]
      (when (re-find #"token|password|secret|authorization" (str/lower-case name))
        (refuse! "platform_parameter_credential" "Credentials belong to the acting identity, never a tool parameter"))
      (let [definition (or (get definitions name)
                           (refuse! "platform_parameter_unknown" (str "Parameter is absent from the pinned operation: " name)))]
        (when-not (valid-value? document (get definition "schema" {}) value 0)
          (refuse! "platform_parameter_invalid" (str "Parameter does not satisfy the pinned type or bounds: " name)))
        (when (and (contains? path-keys name) (not (safe-path-value? value)))
          (refuse! "platform_path_parameter_invalid" "Path bindings must be single nonempty segments without control characters"))))
  (when-not (every? #(contains? supplied %) path-keys)
    (refuse! "platform_path_parameter_required" "A required path binding is missing from the operation")))

(defn operation-bindings
  "Select a pinned operation and validate named path/query bindings, independent
   of its effect. This does not admit execution or inspect the JSON payload."
  [asset {:keys [platform operation parameters] :or {parameters []}}]
  (catalog/assert-catalog! asset)
  (let [selected (or (some #(when (and (= platform (get % "platform"))
                                      (= operation (get % "id"))) %) (get asset "operations"))
                     (refuse! "platform_operation_not_found" "Search social.features for an exact operation first"))
        document (get-in asset ["documents" (get selected "document")])
        schema (get-in document (get selected "pointer"))
        definitions (parameter-definitions platform document selected schema)
        supplied (into {} (map (juxt :name :value)) parameters)
        path-keys (set (map second (re-seq #"\{([^}]+)\}" (or (get selected "route") ""))))]
    (assert-bindings! document definitions parameters supplied path-keys)
    {:selected selected :document document :schema schema
     :plan {:platform platform :operation operation :method (get selected "method") :path-template (get selected "route")
     :path (select-keys supplied path-keys) :query (apply dissoc supplied path-keys)
     :credential-provider (if (= platform "discord") "discord_bot" "bluesky")
     :service (cond (= platform "discord") :discord
                    (str/starts-with? operation "chat.bsky.") :bluesky-chat
                    (str/starts-with? operation "tools.ozone.") :bluesky-ozone
                    :else :bluesky-pds)
     :proxy (cond (str/starts-with? operation "chat.bsky.") "did:web:api.bsky.chat#bsky_chat"
                  (str/starts-with? operation "tools.ozone.") "did:plc:ar7c4by46qjdydhdevvrndac#atproto_labeler")
     :security (or (get schema "security") (get document "security"))}}))

(defn read-plan
  "Infer path/query bindings from a pinned read and validate before credential access."
  [asset parameters]
  (let [{:keys [selected schema plan]} (operation-bindings asset parameters)]
    (assert-readable! (:platform plan) selected schema)
    plan))
