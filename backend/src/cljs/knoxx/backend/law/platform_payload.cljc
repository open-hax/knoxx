(ns knoxx.backend.law.platform-payload
  "Basic pinned payload shape checks; the platform validates its complete schema."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(defn- refuse!
  [code]
  (throw (ex-info "JSON payload does not satisfy the pinned operation's basic shape"
                  {:code code :phase :preflight})))

(defn resolve-schema
  "Resolve only a definition in the pinned asset; no network or user origin."
  [asset document-id schema]
  (if-let [reference (or (get schema "$ref") (when (= "ref" (get schema "type")) (get schema "ref")))]
    (let [[nsid fragment] (str/split reference #"#" 2)
          id (if (str/blank? nsid) document-id nsid)
          path (if (str/starts-with? (or fragment "") "/")
                 (mapv #(-> % (str/replace "~1" "/") (str/replace "~0" "~")) (str/split (subs fragment 1) #"/"))
                 ["defs" (if (str/blank? fragment) "main" fragment)])
          resolved (get-in asset (into ["documents" id] path))]
      (when-not resolved (refuse! "platform_payload_schema_missing"))
      [id resolved])
    [document-id schema]))

(declare basic-shape?)

(defn- object-shape?
  [asset document-id schema value depth]
  (and (map? value)
       (every? #(contains? value %) (get schema "required"))
       (or (not= false (get schema "additionalProperties"))
           (every? #(contains? (get schema "properties") %) (keys value)))
       (every? (fn [[field shape]]
                 (or (not (contains? value field))
                     (basic-shape? asset document-id shape (get value field) (inc depth))))
               (get schema "properties"))))

(defn- type-shape?
  [asset document-id schema value depth]
  (let [kind (get schema "type") choices (or (get schema "oneOf") (get schema "anyOf"))]
    (cond
      (or (get schema "$ref") (= "ref" kind)) (basic-shape? asset document-id schema value (inc depth))
      choices (some #(basic-shape? asset document-id % value (inc depth)) choices)
      (vector? kind) (some #(basic-shape? asset document-id (assoc schema "type" %) value (inc depth)) kind)
      (= kind "object") (object-shape? asset document-id schema value depth)
      (= kind "array") (and (vector? value)
                            (<= (get schema "minItems" (get schema "minLength" 0)) (count value)
                                (get schema "maxItems" (get schema "maxLength" 50000)))
                            (every? #(basic-shape? asset document-id (get schema "items" {}) % (inc depth)) value))
      (= kind "union") (and (map? value) (string? (get value "$type"))
                            (let [reference (get value "$type")
                                  refs (map #(if (str/starts-with? % "#") (str document-id %) %) (get schema "refs"))]
                              (if (some #{reference} refs)
                                (basic-shape? asset document-id {"type" "ref" "ref" reference} value (inc depth))
                                (not (true? (get schema "closed"))))))
      (= kind "string") (and (string? value)
                             (<= (get schema "minLength" 0) (count value) (get schema "maxLength" 1048576)))
      (= kind "integer") (integer? value)
      (= kind "number") (number? value)
      (= kind "boolean") (boolean? value)
      (= kind "null") (nil? value)
      :else true)))

(defn basic-shape?
  "Check required fields and declared scalar/container types, enum and basic
   bounds. Preserve undeclared JSON fields unless explicitly forbidden. Formats,
   semantic constraints and the authoritative full Lexicon remain upstream."
  [asset document-id raw-schema value depth]
  (when (< depth 48)
    (let [[document-id schema] (resolve-schema asset document-id raw-schema)]
      (and (type-shape? asset document-id schema value depth)
           (or (not (contains? schema "const")) (= value (get schema "const")))
           (or (not (contains? schema "enum")) (some #(= value %) (get schema "enum")))
           (or (not (number? value))
               (and (<= (get schema "minimum" ##-Inf) value) (<= value (get schema "maximum" ##Inf))))
           (every? #(basic-shape? asset document-id % value (inc depth)) (get schema "allOf"))))))

(def registry
  {::path [:vector [:or :string :int]]
   ::issue [:map {:closed true} [:path [:ref ::path]] [:expect :string]]
   ::issues [:vector {:max 20} [:ref ::issue]]})

(def Issues ::issues)
(def issues-valid? (m/validator [:schema {:registry registry} Issues]))

(defn- issue [path expectation] [{:path path :expect expectation}])

(defn- container-match?
  [schema value]
  (let [kind (get schema "type")]
    (cond
      (= "object" kind) (map? value)
      (= "array" kind) (vector? value)
      (= "null" kind) (nil? value)
      (vector? kind) (some #(container-match? (assoc schema "type" %) value) kind)
      :else true)))

(declare shape-issues)

(defn- choice-issues
  [asset document-id choices value path depth]
  (let [resolved (map #(second (resolve-schema asset document-id %)) choices)
        matching (filter #(container-match? % value) resolved)
        candidates (if (seq matching) matching resolved)]
    (first (sort-by count (map #(shape-issues asset document-id % value path (inc depth)) candidates)))))

(defn- object-issues
  [asset document-id schema value path depth]
  (concat
   (mapcat #(when-not (contains? value %) (issue (conj path %) "required field")) (get schema "required"))
   (when (and (= false (get schema "additionalProperties"))
              (some #(not (contains? (get schema "properties") %)) (keys value)))
     (issue path "only fields declared in properties"))
   (mapcat (fn [[field shape]]
             (when (contains? value field)
               (shape-issues asset document-id shape (get value field) (conj path field) (inc depth))))
           (get schema "properties"))))

(defn- bounds-issues
  [schema value path]
  (let [[size lower upper] (cond
                            (string? value) [(count value) (get schema "minLength") (get schema "maxLength")]
                            (vector? value) [(count value) (get schema "minItems" (get schema "minLength"))
                                            (get schema "maxItems" (get schema "maxLength"))]
                            (number? value) [value (get schema "minimum") (get schema "maximum")])]
    (when (or (and lower (< size lower)) (and upper (> size upper)))
      (issue path (str (if (number? value) "number" "length")
                       (when lower (str " >= " lower)) (when upper (str " <= " upper))
                       (when-let [description (get schema "description")] (str "; " (subs description 0 (min 400 (count description))))))))))

(defn shape-issues
  "Bounded field paths and schema expectations, never submitted field values.
   Alternatives prefer the matching container so a nullable object explains its
   missing children rather than merely asking the caller to send null."
  [asset document-id raw-schema value path depth]
  (vec (take 20
         (if (>= depth 48)
           (issue path "schema nesting below 48 levels")
           (let [[document-id schema] (resolve-schema asset document-id raw-schema)
                 kind (get schema "type") choices (or (get schema "oneOf") (get schema "anyOf"))]
             (concat
              (cond
                choices (choice-issues asset document-id choices value path depth)
                (vector? kind) (choice-issues asset document-id (map #(assoc schema "type" %) kind) value path depth)
                (and (= "object" kind) (map? value)) (object-issues asset document-id schema value path depth)
                (and (= "array" kind) (vector? value))
                (mapcat (fn [[index item]] (shape-issues asset document-id (get schema "items" {}) item (conj path index) (inc depth)))
                        (map-indexed vector value))
                (not (type-shape? asset document-id schema value depth)) (issue path (str "type " kind))
                :else [])
              (when (and (contains? schema "const") (not= value (get schema "const"))) (issue path "pinned constant"))
              (when (and (contains? schema "enum") (not (some #(= value %) (get schema "enum"))))
                (issue path (str "one of " (pr-str (get schema "enum")))))
              (bounds-issues schema value path)
              (mapcat #(shape-issues asset document-id % value path (inc depth)) (get schema "allOf"))))))))

(defn assert-body!
  "Validate before credential access. Include bounded repair guidance in both
   exception data and text because runtime adapters may discard exception data."
  [asset document-id schema body]
  (when-not (basic-shape? asset document-id schema body 0)
    (let [issues (shape-issues asset document-id schema body ["body"] 0)
          issues (if (seq issues) issues [{:path ["body"] :expect "pinned operation basic shape"}])]
      (when-not (issues-valid? issues) (refuse! "platform_payload_diagnostics_invalid"))
      (throw (ex-info (str "Not attempted: JSON payload does not satisfy the pinned operation's basic shape. Repair "
                          (str/join "; " (map #(str (pr-str (:path %)) ": " (:expect %)) issues))
                          ". Inspect social.describe with the indicated schema reference for complete fields; do not replace a failed native feature with a text imitation.")
                      {:code "platform_payload_shape_invalid" :phase :preflight :issues issues}))))
  body)
