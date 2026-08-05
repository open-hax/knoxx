(ns knoxx.backend.infra.routes.mcp.transport
  "The MCP Streamable HTTP transport boundary.

   Everything here is about the wire, not about authorization: reading a bearer
   token off a request, naming the session, making the SDK's transport behave,
   and answering an unauthenticated caller with the challenge RFC 9728 expects.
   Extracted from infra.routes.mcp so the OAuth flow and the transport quirks
   stop sharing one file.

   Nothing here decides who may call anything."
  (:require [clojure.string :as str]))

(defn bearer-token
  "The bearer credential on a request, or nil."
  [req]
  (let [raw (str (or (some-> req (aget "headers") (aget "authorization")) ""))
        m   (.match raw (js/RegExp. "^Bearer\\s+(.+)$" "i"))]
    (when m (str/trim (aget m 1)))))

(defn resolve-session-id
  "The MCP session id, from the header or the query, or nil."
  [req]
  (let [headers   (or (aget req "headers") (js/Object.))
        header-id (aget headers "mcp-session-id")
        q         (or (aget req "query") (js/Object.))
        query-id  (aget q "sessionId")]
    (cond
      (and (string? header-id) (not (str/blank? header-id))) header-id
      (and (string? query-id) (not (str/blank? query-id)))   query-id
      :else nil)))

(defn protected-resource-metadata-url [base]
  (.toString (js/URL. "/.well-known/oauth-protected-resource" base)))

(defn www-authenticate-challenge
  "The WWW-Authenticate value that points a client at our resource metadata."
  [base]
  (str "Bearer realm=\"mcp\", resource_metadata=\""
       (protected-resource-metadata-url base) "\""))

(defn challenge-unauthorized!
  "Refuse via Fastify's reply object."
  [^js reply base]
  (-> (.header reply "WWW-Authenticate" (www-authenticate-challenge base))
      (.code 401)
      (.send "Unauthorized")))

(defn unauthorized!
  "Refuse via the raw response, for a route that has hijacked the reply."
  [base ^js raw-res]
  (.writeHead raw-res 401 (clj->js {"WWW-Authenticate" (www-authenticate-challenge base)
                                    "Content-Type"     "text/plain"}))
  (.end raw-res "Unauthorized"))

(defn stateless-transport-options
  "Options that actually select the MCP SDK's stateless mode.

   The key must be ABSENT, not undefined. (clj->js {:sessionIdGenerator
   js/undefined}) emits {sessionIdGenerator: null}, and the SDK selects
   stateless only on === undefined — so null selected *stateful* mode, where
   everything after initialize is rejected with \"Server not initialized\" and a
   client sees no tools. SDK 1.18/1.24/1.29/1.30 all read null as stateful."
  []
  #js {})

(defn handle-request!
  ([^js t req reply]      (.handleRequest t req reply))
  ([^js t req reply body] (.handleRequest t req reply body)))

(defn ensure-streamable-accept!
  "Force the Accept header the SDK requires, in both places it reads it.

   A client that sends only application/json is otherwise rejected by the
   transport, so this normalizes rather than refuses. rawHeaders is rewritten as
   well as headers because the SDK reads whichever it reaches first."
  [req]
  (let [raw          (aget req "raw")
        headers      (or (aget raw "headers") (js/Object.))
        raw-headers  (or (aget raw "rawHeaders") (js/Array.))
        accept-value "application/json, text/event-stream"
        accept       (str/lower-case (str (or (aget headers "accept") "")))
        has-json?    (str/includes? accept "application/json")
        has-sse?     (str/includes? accept "text/event-stream")]
    (when (or (str/blank? accept) (not has-json?) (not has-sse?))
      (aset headers "accept" accept-value)
      (aset raw "headers" headers)
      (let [filtered (->> (partition 2 (array-seq raw-headers))
                          (remove (fn [[k _]] (= "accept" (str/lower-case (str k)))))
                          (mapcat identity)
                          vec)]
        (aset raw "rawHeaders" (clj->js (conj filtered "accept" accept-value)))))
    req))
