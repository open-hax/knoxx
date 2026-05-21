(ns knoxx.backend.extern.row-extra
  "Data-specific JSON row-extra codecs.

   Owns JSON.parse/js->clj for persisted row metadata. Callers should use the
   named codec matching the row family they are decoding instead of importing a
   generic JSON boundary. Accepted object key spellings are preserved as written
   after keywordization, so snake_case, kebab-case, and camelCase compatibility
   remains caller-owned at semantic lookup sites."
  (:require [knoxx.backend.extern.json :as json]))

(defn parse-row-extra
  "Parse row :extra metadata into a CLJS keyword map.

   - CLJS maps pass through unchanged.
   - JSON object strings decode to keyword maps.
   - Invalid JSON, nil, and non-object JSON return nil."
  [value]
  (json/parse-object value))

(defn parse-session-title-extra
  [value]
  (parse-row-extra value))

(defn parse-core-memory-extra
  [value]
  (parse-row-extra value))
