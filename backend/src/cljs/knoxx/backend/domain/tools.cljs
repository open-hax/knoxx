(ns knoxx.backend.domain.tools
  "Shared utilities for agent tool factories.
   Sanitization, TypeBox helpers, and generic tool-update callbacks."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.tools :as xtools]
            [knoxx.backend.runtime.state :as runtime-state]))

(defn ->params
  "Convert a Malli schema to a Pi tool :parameters JS object."
  [schema]
  (xtools/parameters schema))

(defn create-tool-obj [name label description prompt prompt-guidelines params execute runtime config]
  (xtools/tool-definition {:name name
                           :label label
                           :description description
                           :prompt-snippet prompt
                           :prompt-guidelines prompt-guidelines
                           :parameters-schema params
                           :execute execute
                           :runtime runtime
                           :config config}))

(defn maybe-tool-update!
  "Call an on-update callback with a text status update."
  [f text]
  (xtools/send-update! f {:content [{:type "text" :text text}]}))

(defn type-optional
  [Type schema]
  (xtools/type-optional Type schema))

(defn sanitize-custom-tool-name
  [tool]
  (xtools/sanitize-custom-tool-name tool))

(defn sanitize-custom-tools
  [tools]
  (xtools/sanitize-custom-tools tools))

(defn filter-custom-tools-by-allow-set
  "Filter a collection of tool objects to only those whose name (or originalName)
   appears in allowed-tool-ids."
  [tools allowed-tool-ids]
  (xtools/filter-custom-tools-by-allow-set tools allowed-tool-ids))

(defn json-parse
  "Parse JSON string to Clojure data."
  [text]
  (xtools/parse-json text))

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
