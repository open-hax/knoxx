(ns knoxx.backend.mcp-bridge
  "MCP (Model Context Protocol) gateway — CLJS implementation.

   Manages connections to MCP servers (HTTP and stdio transports) and routes
   tool calls between Knoxx agents and MCP servers.

   Previously wrapped globalThis.__mcp_gateway exposed by mcp_gateway.mjs.
   Now self-contained in CLJS — the mjs module is no longer needed."
  (:require [clojure.string :as str]))

(def ^:private PROTOCOL-VERSION "2024-11-05")

(defonce ^:private servers* (atom {}))
(defonce ^:private request-counter* (atom 0))

;; ---------------------------------------------------------------------------
;; Config parsing
;; ---------------------------------------------------------------------------

(defn- parse-mcp-servers-env
  "Parse MCP_SERVERS env: \"id:url:transport,id:command:args:transport\""
  [env-value]
  (if (str/blank? env-value)
    {}
    (let [entries (->> (str/split env-value #",")
                       (map str/trim)
                       (remove str/blank?))]
      (into {}
            (keep
             (fn [entry]
               (let [parts (str/split entry #":")]
                 (when (>= (count parts) 3)
                   (let [id (first parts)
                         rest-parts (rest parts)
                         transport (last rest-parts)]
                     (cond
                       (= transport "http")
                       [id {:url (str/join ":" (butlast rest-parts)) :transport "http"}]

                       (= transport "stdio")
                       [id {:command (first rest-parts)
                            :args    (vec (rest (butlast rest-parts)))
                            :transport "stdio"}]

                       :else nil))))))
            entries))))

(defn- get-mcp-servers-from-env
  []
  (parse-mcp-servers-env (or (aget js/process.env "MCP_SERVERS") "")))

;; ---------------------------------------------------------------------------
;; SSE response parsing
;; ---------------------------------------------------------------------------

(defn- parse-sse-response
  [text expected-id]
  (let [lines (str/split text #"\n")
        data-line (reduce
                   (fn [_ line]
                     (when (str/starts-with? (str/trim line) "data:")
                       (reduced (str/trim (subs (str/trim line) 5)))))
                   nil
                   lines)]
    (if data-line
      (let [result (js/JSON.parse data-line)]
        (if (aget result "error")
          (throw (js/Error. (str "MCP error: " (aget (aget result "error") "message"))))
          (aget result "result")))
      (try
        (let [result (js/JSON.parse text)]
          (if (aget result "error")
            (throw (js/Error. (str "MCP error: " (aget (aget result "error") "message"))))
            (aget result "result")))
        (catch :default _
          (throw (js/Error. (str "Failed to parse MCP response: " (subs text 0 200)))))))))

;; ---------------------------------------------------------------------------
;; HTTP client
;; ---------------------------------------------------------------------------

(defn- create-http-client
  [config]
  (let [base-url (str/replace (or (:url config) "") #"/$" "")]
    (fn [method params]
      (let [id (swap! request-counter* inc)
            body (js/JSON.stringify
                  (clj->js {:jsonrpc "2.0"
                            :id id
                            :method method
                            :params (or params {})}))
            headers (doto (js/Object.)
                      (aset "Content-Type" "application/json")
                      (aset "Accept" "application/json, text/event-stream"))]
        (when (:shared-secret config)
          (aset headers "Authorization" (str "Bearer " (:shared-secret config))))
        (-> (js/fetch base-url
                      (clj->js {:method "POST"
                                :headers (js/Object.entries headers)
                                :body body}))
            (.then
             (fn [response]
               (if (not (.-ok response))
                 (throw (js/Error.
                         (str "MCP HTTP error: "
                              (.-status response) " "
                              (.-statusText response))))
                 (-> (.text response)
                     (.then (fn [text]
                              (parse-sse-response text id))))))))))))

;; ---------------------------------------------------------------------------
;; Server connection
;; ---------------------------------------------------------------------------

(defn- initialize-http-server!
  [server]
  (let [client-fn (:client server)]
    (-> (client-fn "initialize"
                   (clj->js {:protocolVersion PROTOCOL-VERSION
                             :capabilities {}
                             :clientInfo {:name "knoxx" :version "1.0.0"}}))
        (.then (fn [init-result]
                 (js/console.log "[mcp-gateway]" (:id server) "initialized:"
                                 (or (aget init-result "serverInfo" "name") "unknown"))
                 (client-fn "tools/list" nil)))
        (.then (fn [tools-result]
                 (let [js-tools (or (aget tools-result "tools") #js [])
                       tools (vec (for [i (range (.-length js-tools))]
                                    (let [tool (aget js-tools i)]
                                      {:name (or (aget tool "name") "")
                                       :description (or (aget tool "description") "")
                                       :input-schema (js->clj (or (aget tool "inputSchema") {}) :keywordize-keys true)})))]
                   (swap! servers* assoc-in [(:id server) :tools] tools)
                   (js/console.log "[mcp-gateway] Connected to" (:id server)
                                   ", found" (count tools) "tools")))))))

(defn- connect-server!
  [id config]
  (js/console.log "[mcp-gateway] Connecting to" id "(" (:transport config) ")...")
  (cond
    (= (:transport config) "http")
    (let [client-fn (create-http-client config)
          server {:id id :config config :tools [] :connected true :client client-fn}]
      (swap! servers* assoc id server)
      (initialize-http-server! server))

    (= (:transport config) "stdio")
    (do
      (js/console.log "[mcp-gateway] stdio transport not yet implemented")
      (swap! servers* assoc id {:id id :config config :tools [] :connected false :client nil}))

    :else
    (js/Promise.reject (js/Error. (str "Unknown transport: " (:transport config))))))

;; ---------------------------------------------------------------------------
;; Public API
;; ---------------------------------------------------------------------------

(defn available?
  "Returns true if any MCP servers are connected."
  []
  (some? (seq @servers*)))

(defn initialize!
  "Initialize the MCP gateway with configured servers.
   Returns a Promise that resolves when all servers are connected."
  ([]
   (initialize! {}))
  ([config]
   (let [server-configs (or (:servers config) (get-mcp-servers-from-env))]
     (-> (.all js/Promise
               (into-array
                (for [[id server-config] server-configs]
                  (-> (js/Promise.resolve (connect-server! id server-config))
                      (.catch (fn [err]
                                (js/console.error "[mcp-gateway] Failed to connect to" id ":"
                                                  (aget err "message"))))))))
         (.then (fn [] @servers*))))))

(defn enabled?
  "Check if MCP is enabled and has connected servers."
  []
  (and (not= (aget js/process.env "MCP_ENABLED") "false")
       (some? (seq @servers*))))

(defn status
  "Get MCP gateway status as a CLJS map."
  []
  {:enabled (enabled?)
   :servers (for [[id server] @servers*]
              {:id id
               :transport (get-in server [:config :transport])
               :connected (:connected server)
               :tool-count (count (:tools server))
               :tools (mapv :name (:tools server))})})

(defn catalog
  "Get the MCP tool catalog as a vector of tool maps."
  []
  (vec
   (for [[server-id server] @servers*
         tool (:tools server)]
     (assoc tool
            :id (str "mcp." server-id "." (:name tool))
            :serverId server-id
            :toolId (str "mcp." server-id "." (:name tool))))))

(defn tools-map
  "Get all MCP tools as a map keyed by tool ID."
  []
  (into {}
        (for [tool (catalog)]
          [(keyword (:id tool)) tool])))

(defn- format-mcp-result
  [result]
  (if-not result
    {:content "" :isError false}
    (if (and (aget result "content")
             (array? (aget result "content")))
      {:content (str/join "\n"
                          (keep identity
                                (for [i (range (.-length (aget result "content")))]
                                  (let [block (aget (aget result "content") i)]
                                    (when (= (aget block "type") "text")
                                      (aget block "text"))))))
       :isError (boolean (aget result "isError"))}
      {:content (js/JSON.stringify result nil 2)
       :isError false})))

(defn call-tool!
  "Call an MCP tool by its full ID (e.g. \"mcp.grep.searchGitHub\").
   Returns a Promise that resolves with {:content \"...\" :isError bool}."
  [tool-id args]
  (let [match (.match (str tool-id) #"^mcp\.([^.]+)\.(.+)$")]
    (when-not match
      (throw (js/Error. (str "Invalid MCP tool ID: " tool-id))))
    (let [server-id (aget match 1)
          tool-name (aget match 2)
          server (get @servers* server-id)]
      (when-not server
        (throw (js/Error. (str "MCP server not found: " server-id))))
      (when-not (:connected server)
        (throw (js/Error. (str "MCP server not connected: " server-id))))
      (js/console.log "[mcp-gateway] Calling" server-id "." tool-name
                      "with args:" (subs (js/JSON.stringify (clj->js (or args {}))) 0 200))
      (if (= (get-in server [:config :transport]) "http")
        (let [client-fn (:client server)]
          (-> (client-fn "tools/call"
                         (clj->js {:name tool-name :arguments (or args {})}))
              (.then format-mcp-result)))
        (throw (js/Error. (str "Transport not supported: "
                               (get-in server [:config :transport]))))))))

(defn mcp-tools-for-agent
  "Return MCP tools formatted as agent SDK custom tools (JavaScript array)."
  []
  (when (enabled?)
    (let [tools (catalog)]
      (clj->js
       (mapv (fn [tool]
               (let [tool-id (:id tool)
                     input-schema (or (:input-schema tool) {})
                     execute-fn (fn [_tool-call-id tool-args a b c]
                                  (let [on-update (or (when (fn? a) a)
                                                      (when (fn? b) b)
                                                      (when (fn? c) c))
                                        args (js->clj tool-args :keywordize-keys true)]
                                    (when (fn? on-update)
                                      (on-update (clj->js {:content [{:type "text"
                                                                      :text (str "Calling MCP tool " tool-id "...")}]})))
                                    (-> (call-tool! tool-id args)
                                        (.then (fn [result]
                                                 (clj->js {:content [{:type "text"
                                                                      :text (or (:content result) "")}]})))
                                        (.catch (fn [err]
                                                  (clj->js {:content [{:type "text"
                                                                       :text (str "MCP tool error: "
                                                                                  (or (aget err "message")
                                                                                      (str err)))}]}))))))]
                 {:name tool-id
                  :label (or (:name tool) tool-id)
                  :description (or (:description tool) "")
                  :promptSnippet (str "Call MCP tool " tool-id)
                  :promptGuidelines [(str "Use " tool-id
                                          " when the task requires capabilities from the "
                                          (:serverId tool) " MCP server.")]
                  :parameters (clj->js (or input-schema {}))
                  :execute execute-fn}))
             tools)))))
