(ns knoxx.backend.e2e.mcp-client
  "A minimal MCP Streamable HTTP client for the e2e suite.

   The SDK's own client is deliberately not used: it would normalize or reject
   the very responses these tests exist to inspect. When a tool's schema
   arrives malformed we want the wire payload, not a typed object the SDK
   already decided about on our behalf.

   Knoxx serves /mcp in the SDK's stateless mode, so every POST is independent
   and carries no session id. initialize is still sent first, because a real
   client sends it and a server that only breaks on that path should break here
   too."
  (:require [clojure.string :as str]))

(def protocol-version "2025-06-18")

(defn parse-event-stream
  "JSON-RPC payloads carried in a text/event-stream body.

   A frame that will not parse is reported as {:unparseable raw} rather than
   dropped: skipping it silently would read as \"the server sent nothing\",
   which is the wrong diagnosis for a framing bug."
  [text]
  (->> (str/split text #"\r?\n\r?\n")
       (keep (fn [block]
               (let [data (->> (str/split block #"\r?\n")
                               (filter #(str/starts-with? % "data:"))
                               (map #(str/trim (subs % 5)))
                               (str/join ""))]
                 (when-not (str/blank? data)
                   (try
                     (js->clj (js/JSON.parse data) :keywordize-keys true)
                     (catch :default _ {:unparseable data}))))))
       vec))

(defn- ^:async decode-response!
  [^js resp]
  (let [content-type (str/lower-case (str (or (.get (.-headers resp) "content-type") "")))
        text         (await (.text resp))]
    (cond
      (str/includes? content-type "text/event-stream")
      {:messages (parse-event-stream text) :raw text}

      (str/includes? content-type "application/json")
      {:messages (try
                   (let [parsed (js->clj (js/JSON.parse text) :keywordize-keys true)]
                     (if (vector? parsed) parsed [parsed]))
                   (catch :default _ []))
       :raw text}

      :else {:messages [] :raw text})))

(defn- body-error
  "A readable error for a response that is not a JSON-RPC reply.

   /mcp answers its own failures with a plain {:error :detail} body rather than
   a JSON-RPC envelope, so reading only the envelope's :error reported every
   one of them as unknown — hiding exactly the sentence that says what broke."
  [{:keys [messages raw]} status]
  (let [body (first messages)]
    (if (and (map? body) (not (:jsonrpc body)))
      {:message (->> [(:error body) (:detail body) (:message body)]
                     (remove str/blank?)
                     (str/join ": "))}
      {:message (let [t (str/trim (str raw))]
                  (if (str/blank? t) (str "HTTP " status " with an empty body") t))})))

(defonce ^:private next-id* (atom 0))

(defn ^:async rpc!
  "One JSON-RPC call against `base-url`/mcp.

   Returns {:ok :status :result :error :raw}. Never throws for a protocol-level
   failure — a test asserting that an unauthenticated call is refused needs the
   401 as data, not as an exception."
  [{:keys [base-url token]} method params]
  (let [id (swap! next-id* inc)
        headers (cond-> {"content-type" "application/json"
                         "accept" "application/json, text/event-stream"}
                  token (assoc "authorization" (str "Bearer " token)))
        resp (await (js/fetch (str base-url "/mcp")
                              (clj->js {:method "POST"
                                        :headers headers
                                        :body (js/JSON.stringify
                                               (clj->js {:jsonrpc "2.0" :id id
                                                         :method method
                                                         :params (or params {})}))})))
        decoded (await (decode-response! resp))
        reply   (some #(when (and (:jsonrpc %) (= id (:id %))) %) (:messages decoded))
        ok      (and (.-ok resp) (some? reply) (nil? (:error reply)))]
    {:ok ok
     :status (.-status resp)
     :result (:result reply)
     :error (when-not ok (or (:error reply) (body-error decoded (.-status resp))))
     :raw (:raw decoded)}))

(defn initialize!
  [client]
  (rpc! client "initialize"
        {:protocolVersion protocol-version
         :clientInfo {:name "knoxx-e2e" :version "1.0.0"}
         :capabilities {}}))

(defn list-tools!
  [client]
  (rpc! client "tools/list" {}))

(defn call-tool!
  [client tool-name arguments]
  (rpc! client "tools/call" {:name tool-name :arguments (or arguments {})}))

(defn tool-text
  "The text content of a tools/call result, flattened to one string."
  [result]
  (->> (:content result)
       (map (fn [part] (if (= "text" (:type part)) (:text part) (str "[" (:type part) "]"))))
       (str/join " ")
       str/trim))

(defn call-outcome
  "Whether a tools/call actually succeeded.

   MCP reports a tool's own failure as a *successful* JSON-RPC result carrying
   isError, so treating a 200 as a pass would mark every broken tool green."
  [{:keys [ok result error]}]
  (cond
    (not ok) {:status :rpc-error :detail (:message error)}
    (:isError result) {:status :tool-error :detail (tool-text result)}
    :else {:status :ok :detail (tool-text result)}))
