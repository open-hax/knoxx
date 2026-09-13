(ns knoxx.backend.extern.mcp-token
  "The native token-record boundary consumed by the MCP route.

   Authentication policy and grant construction stay in CLJS data. The MCP
   route still integrates with persisted JSON token records and downstream
   JavaScript APIs, so it owns the single validated conversion into their
   native shape. Malformed identity or grant data fails before JavaScript code
   can reinterpret it."
  (:require [knoxx.backend.law.mcp-token :as token-law]))

(defn stored-record
  "Decode persisted grant data; storage already enforces live expiry and revocation."
  [raw]
  (try
    (let [value (if (map? raw) raw (js->clj (js/JSON.parse raw) :keywordize-keys true))]
      (when (map? value) value))
    (catch :default _ nil)))

(defn http-error
  "Expose a validated MCP refusal status to native Fastify and hijacked transports."
  [status code detail data]
  (let [error (ex-info detail (merge data {:status status :code code :error code :detail detail}))]
    (set! (.-statusCode error) status)
    (set! (.-code error) code)
    error))

(defn native-record
  "Validate and convert one CLJS token record to the route's JavaScript object."
  [record]
  (when record
    (-> record
        token-law/assert-record!
        clj->js)))
