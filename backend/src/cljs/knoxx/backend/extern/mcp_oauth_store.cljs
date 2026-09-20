(ns knoxx.backend.extern.mcp-oauth-store
  "Opaque OAuth bytes become hashes before crossing into durable provider data."
  (:require ["node:crypto" :as crypto]
            [knoxx.backend.law.mcp-oauth-store :as law]))

(defn now-ms "Read the live credential expiry clock." [] (.now js/Date))
(defn digest "Hash a credential; the raw value never enters a Clio operation." [value]
  (law/validate! law/Text value)
  (.digest (.update (crypto/createHash "sha256") value "utf8") "hex"))
(defn decode "Decode one JSON provider boundary to CLJS data." [value]
  (if (string? value) (js->clj (js/JSON.parse value) :keywordize-keys true) value))
(defn encode "Preserve the existing JSON-string OAuth facade response." [value]
  (when (some? value) (js/JSON.stringify (clj->js value))))
