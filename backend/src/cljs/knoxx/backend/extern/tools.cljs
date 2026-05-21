(ns knoxx.backend.extern.tools
  "Agent tool runtime boundary.

   Owns JS tool object construction, Malli JSON-schema conversion to runtime JS
   params, generic tool update callback payloads, custom tool JS object
   sanitation/mutation, and JSON parsing for tool string parameters. Domain tool
   namespaces should delegate here instead of importing generic JS/JSON helpers."
  (:require [clojure.string :as str]
            [malli.json-schema :as mjs]))

(defn parameters
  "Convert a Malli schema to a Pi/eta-mu runtime JS parameters object."
  [schema]
  (clj->js (mjs/transform schema)))

(defn tool-definition
  [{:keys [name label description prompt-snippet prompt-guidelines parameters-schema execute runtime config]}]
  #js {:name name
       :label label
       :description description
       :promptSnippet prompt-snippet
       :promptGuidelines (clj->js prompt-guidelines)
       :parameters (parameters parameters-schema)
       :execute (partial execute runtime config)})

(defn send-update!
  "Call a tool runtime update callback with a CLJS payload map."
  [f payload]
  (when (fn? f)
    (f (clj->js payload))))

(defn type-optional
  [^js Type schema]
  (.Optional Type schema))

(defn- js-array-seq
  [value]
  (if (array? value)
    (array-seq value)
    []))

(defn- replace-tool-name
  [text original-name sanitized-name]
  (some-> (str text)
          (str/replace original-name sanitized-name)))

(defn- sanitize-tool-guidelines
  [guidelines original-name sanitized-name]
  (clj->js
   (mapv (fn [guideline]
           (str "Use " sanitized-name " (canonical " original-name ") when "
                (replace-tool-name guideline original-name sanitized-name)))
         (js-array-seq guidelines))))

(defn sanitize-custom-tool-name
  [tool]
  (let [name (some-> (aget tool "name") str)
        sanitized (some-> name
                          (str/replace #"[^A-Za-z0-9_-]" "_")
                          (str/replace #"_+" "_"))]
    (when (and sanitized (not= sanitized name))
      (aset tool "name" sanitized)
      (aset tool "originalName" name)
      (when-let [description (some-> (aget tool "description") str)]
        (aset tool "description"
              (str description " Call this tool as `" sanitized "`. Canonical tool id: `" name "`.")))
      (when-let [snippet (some-> (aget tool "promptSnippet") str)]
        (aset tool "promptSnippet"
              (str "Call as `" sanitized "` (canonical `" name "`). "
                   (replace-tool-name snippet name sanitized))))
      (when-let [guidelines (aget tool "promptGuidelines")]
        (aset tool "promptGuidelines"
              (sanitize-tool-guidelines guidelines name sanitized))))
    tool))

(defn sanitize-custom-tools
  [tools]
  (into-array (map sanitize-custom-tool-name (js-array-seq tools))))

(defn filter-custom-tools-by-allow-set
  [tools allowed-tool-ids]
  (if (nil? allowed-tool-ids)
    tools
    (into-array
     (filter (fn [tool]
               (let [runtime-id (some-> tool (aget "name") str str/trim not-empty)
                     original-id (some-> tool (aget "originalName") str str/trim not-empty)]
                 (or (and runtime-id (contains? allowed-tool-ids runtime-id))
                     (and original-id (contains? allowed-tool-ids original-id)))))
             (js-array-seq tools)))))

(defn parse-json
  [text]
  (try
    (js->clj (.parse js/JSON text) :keywordize-keys true)
    (catch :default err
      (throw (js/Error. (str "Invalid JSON: " (.-message err)))))))
