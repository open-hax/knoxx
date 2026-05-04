(ns knoxx.backend.tools.shared
  "Shared utilities for agent tool factories.
   Sanitization, TypeBox helpers, and generic tool-update callbacks."
  (:require [clojure.string :as str]
            [knoxx.backend.runtime.state :as runtime-state]
            [malli.json-schema :as mjs]          ;; ← add this
            [knoxx.backend.http :refer [js-array-seq]]))

;; ← add this
(defn ->params
  "Convert a Malli schema to a Pi tool :parameters JS object."
  [schema]
  (clj->js (mjs/transform schema)))
(defn create-tool-obj [ name label description prompt prompt-guidelines params  execute  runtime config]

  #js {:name name
       :label label
       :description description
       :promptSnippet prompt
       :promptGuidelines (clj->js prompt-guidelines)
       :parameters (->params params)
       :execute (partial execute runtime config)})
(defn maybe-tool-update!
  "Call an on-update callback with a text status update."
  [f text]
  (when (fn? f)
    (f #js {:content #js [#js {:type "text" :text text}]})))

(defn type-optional
  [^js Type schema]
  (.Optional Type schema))

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
        (aset tool "description" (str description " Original tool id: " name "."))))
    tool))

(defn sanitize-custom-tools
  [tools]
  (let [items (if (array? tools) (array-seq tools) [])]
    (into-array (map sanitize-custom-tool-name items))))

(defn filter-custom-tools-by-allow-set
  "Filter a collection of tool objects to only those whose name (or originalName)
   appears in allowed-tool-ids."
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

(defn json-parse
  "Parse JSON string to Clojure data."
  [text]
  (try
    (js->clj (.parse js/JSON text) :keywordize-keys true)
    (catch :default err
      (throw (js/Error. (str "Invalid JSON: " (.-message err)))))))

(defn live-config
  "Resolve live config, preferring the runtime atom."
  [config]
  (or @runtime-state/config* config))

(defn agent-custom-tool-suite
  "Classify an agent spec into a tool-suite keyword."
  [agent-spec]
  (let [role (some-> (:role agent-spec) str str/trim str/lower-case)
        contract-id (some-> (:contract-id agent-spec) str str/trim str/lower-case)]
    (if (or (= role "contract_librarian")
            (= contract-id "contract_librarian"))
      :contract-librarian
      :knoxx)))
