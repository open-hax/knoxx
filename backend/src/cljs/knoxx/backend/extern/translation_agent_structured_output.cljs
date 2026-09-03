(ns knoxx.backend.extern.translation-agent-structured-output
  "Named JSON boundary for native Ollama translation completions."
  (:require [knoxx.backend.extern.json :as xjson]))

(defn encode-request-content
  [value]
  (xjson/stringify value))

(defn decode-response-content
  [value]
  (xjson/parse-object value))
