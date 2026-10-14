(ns knoxx.frontend.lib.ws
  "WebSocket stream client. CLJS port of src/lib/ws.ts connectStream:
   channel-routed handlers with auto-reconnect (exponential backoff) and
   conversation rebinding. Returns {:disconnect f :set-conversation-id f}."
  (:require [knoxx.frontend.domain.ws :as domain]))

(def ^:private max-reconnect-delay 30000)

(defn- ws-url [base session-id conversation-id]
  (let [params (js/URLSearchParams.)
        supplied? (some? base)]
    ;; Preserve the fixed native API's existing nil semantics; the JS adapter
    ;; retains the legacy TS truthy optional-ID semantics.
    (when (if supplied? (boolean (seq session-id)) session-id)
      (.set params "session_id" session-id))
    (when (if supplied? (boolean (seq conversation-id)) conversation-id)
      (.set params "conversation_id" conversation-id))
    (let [q (let [s (.toString params)] (if (seq s) (str "?" s) ""))]
      (if (seq base)
        (let [url (js/URL. base)]
          (set! (.-protocol url) (if (= "https:" (.-protocol url)) "wss:" "ws:"))
          (set! (.-pathname url) "/ws/stream")
          (set! (.-search url) q)
          (.toString url))
        (str (if (= "https:" (.. js/window -location -protocol)) "wss" "ws")
             "://" (.. js/window -location -host) "/ws/stream" q)))))

(defn- nullish-string [value fallback]
  (js/String (if (nil? value) fallback value)))

(defn- route-message! [{:keys [on-token on-stats on-console on-event on-lounge]} ^js event]
  (try
    (let [^js message (js/JSON.parse (.-data event))
          channel (.-channel message)
          ^js payload (or (.-payload message) #js {})]
      (case channel
        "tokens" (let [data (js->clj payload :keywordize-keys true)
                       decoded (domain/decode-token (if (map? data) data {})
                                                    (nullish-string (aget payload "token") ""))]
                   (if decoded
                     (when on-token (on-token (:token decoded) (:meta decoded)))
                     (when on-console (on-console "Malformed token packet"))))
        "stats" (when on-stats (on-stats payload))
        "console" (when on-console
                    (on-console (str "[" (nullish-string (aget payload "stream") "log") "] "
                                     (nullish-string (aget payload "line") ""))))
        "events" (when on-event (on-event payload))
        "lounge" (when on-lounge (on-lounge payload))
        nil))
    (catch :default _
      (when on-console (on-console "Malformed websocket packet")))))

(defn- socket-closed! [state on-status connect!]
  (when on-status (on-status :closed))
  (when-not (:disposed @state)
    (let [reconnect-ms (:reconnect-delay @state)]
      (swap! state assoc
             :reconnect-timer (js/setTimeout connect! reconnect-ms)
             :reconnect-delay (min (* reconnect-ms 2) max-reconnect-delay)))))

(defn- listen! [state {:keys [on-status] :as handlers} connect! socket]
  (.addEventListener socket "open"
                     (fn []
                       (swap! state assoc :reconnect-delay 1000)
                       (when on-status (on-status :connected))))
  (.addEventListener socket "close" (fn [_] (socket-closed! state on-status connect!)))
  (.addEventListener socket "error" (fn [_] (when on-status (on-status :error))))
  (.addEventListener socket "message" #(route-message! handlers %)))

(defn- connection-controls [state connect!]
  {:disconnect (fn []
                 (swap! state assoc :disposed true)
                 (when-let [timer (:reconnect-timer @state)] (js/clearTimeout timer))
                 (when-let [socket (:socket @state)] (.close socket)))
   :set-conversation-id
   (fn [conversation-id]
     (when (not= conversation-id (:conversation-id @state))
       (swap! state assoc :conversation-id conversation-id)
       (let [socket (:socket @state)]
         (cond
           (nil? socket) (connect!)
           (= (.-readyState socket) (.-OPEN js/WebSocket))
           (.send socket (js/JSON.stringify #js {:type "set_conversation"
                                                 :conversation_id conversation-id}))
           (= (.-readyState socket) (.-CLOSED js/WebSocket)) (connect!)))))} )

(defn- connect-configured-stream [handlers session-id initial-conversation-id base]
  (let [state (atom {:socket nil :conversation-id initial-conversation-id :disposed false
                     :reconnect-timer nil :reconnect-delay 1000})
        connect! (fn connect! []
                   (when-not (:disposed @state)
                     (let [socket (js/WebSocket. (ws-url base session-id (:conversation-id @state)))]
                       (swap! state assoc :socket socket)
                       (listen! state handlers connect! socket))))]
    (connect!)
    (connection-controls state connect!)))


(defn connect-stream
  "Connect with the existing fixed three-argument native API and map cleanup."
  [handlers session-id initial-conversation-id]
  (connect-configured-stream handlers session-id initial-conversation-id nil))

(defn- js-token-metadata [{:keys [run-id kind operation offset]}]
  (let [metadata #js {}]
    (aset metadata "runId" (if (nil? run-id) js/undefined run-id))
    (aset metadata "kind" (if (nil? kind) js/undefined kind))
    (when operation
      (aset metadata "operation" operation)
      (aset metadata "offset" offset))
    metadata))

(defn ^:export connect-stream-js
  "Named JS edge: camelCase callbacks, API base, and callable cleanup methods."
  [handlers session-id initial-conversation-id base]
  (let [on-token (aget handlers "onToken")
        on-status (aget handlers "onStatus")
        connection (connect-configured-stream
                    {:on-token (when on-token
                                 (fn [token metadata]
                                   (on-token token (js-token-metadata metadata))))
                     :on-status (when on-status (fn [status] (on-status (name status))))
                     :on-stats (aget handlers "onStats")
                     :on-console (aget handlers "onConsole")
                     :on-event (aget handlers "onEvent")
                     :on-lounge (aget handlers "onLounge")}
                    session-id (if (nil? initial-conversation-id) nil initial-conversation-id) base)
        disconnect (:disconnect connection)]
    (js/Reflect.set disconnect "disconnect" disconnect)
    (js/Reflect.set disconnect "setConversationId" (:set-conversation-id connection))
    disconnect))
