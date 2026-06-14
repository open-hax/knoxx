(ns knoxx.frontend.lib.ws
  "WebSocket stream client. CLJS port of src/lib/ws.ts connectStream:
   channel-routed handlers with auto-reconnect (exponential backoff) and
   conversation rebinding. Returns {:disconnect f :set-conversation-id f}."
  (:require [clojure.string :as str]))

(def ^:private max-reconnect-delay 30000)

(defn- ws-url [session-id conversation-id]
  (let [params (js/URLSearchParams.)]
    (when session-id (.set params "session_id" session-id))
    (when conversation-id (.set params "conversation_id" conversation-id))
    (let [q (let [s (.toString params)] (if (seq s) (str "?" s) ""))
          protocol (if (= "https:" (.. js/window -location -protocol)) "wss" "ws")]
      (str protocol "://" (.. js/window -location -host) "/ws/stream" q))))

(defn- route-message! [{:keys [on-token on-stats on-console on-event on-lounge]} ^js event]
  (try
    (let [^js message (js/JSON.parse (.-data event))
          channel (.-channel message)
          ^js payload (or (.-payload message) #js {})]
      (case channel
        "tokens" (when on-token
                   (on-token (str (or (.-token payload) ""))
                             {:run-id (.-run_id payload)
                              :kind (when (string? (.-kind payload)) (.-kind payload))}))
        "stats" (when on-stats (on-stats payload))
        "console" (when on-console
                    (on-console (str "[" (or (.-stream payload) "log") "] "
                                     (or (.-line payload) ""))))
        "events" (when on-event (on-event payload))
        "lounge" (when on-lounge (on-lounge payload))
        nil))
    (catch :default _
      (when on-console (on-console "Malformed websocket packet")))))

(defn connect-stream
  "Opens the /ws/stream socket. `handlers` is a map of :on-token :on-stats
   :on-console :on-event :on-lounge :on-status. Single fixed arity (pass
   nils) so tests can replace it with a plain fn via set!."
  [{:keys [on-status] :as handlers} session-id initial-conversation-id]
   (let [state (atom {:socket nil
                      :conversation-id initial-conversation-id
                      :disposed false
                      :reconnect-timer nil
                      :reconnect-delay 1000})
         connect! (fn connect! []
                    (when-not (:disposed @state)
                      (let [socket (js/WebSocket. (ws-url session-id (:conversation-id @state)))]
                        (swap! state assoc :socket socket)
                        (.addEventListener socket "open"
                                           (fn []
                                             (swap! state assoc :reconnect-delay 1000)
                                             (when on-status (on-status :connected))))
                        (.addEventListener socket "close"
                                           (fn [_]
                                             (when on-status (on-status :closed))
                                             (when-not (:disposed @state)
                                               (let [delay (:reconnect-delay @state)]
                                                 (swap! state assoc
                                                        :reconnect-timer (js/setTimeout connect! delay)
                                                        :reconnect-delay (min (* delay 2) max-reconnect-delay))))))
                        (.addEventListener socket "error"
                                           (fn [_] (when on-status (on-status :error))))
                        (.addEventListener socket "message"
                                           #(route-message! handlers %)))))]
     (connect!)
     {:disconnect (fn []
                    (swap! state assoc :disposed true)
                    (when-let [timer (:reconnect-timer @state)]
                      (js/clearTimeout timer))
                    (when-let [^js socket (:socket @state)]
                      (.close socket)))
      :set-conversation-id (fn [conversation-id]
                             (when (not= conversation-id (:conversation-id @state))
                               (swap! state assoc :conversation-id conversation-id)
                               (let [^js socket (:socket @state)]
                                 (cond
                                   (nil? socket) (connect!)
                                   (= (.-readyState socket) (.-OPEN js/WebSocket))
                                   (.send socket (js/JSON.stringify
                                                  #js {:type "set_conversation"
                                                       :conversation_id conversation-id}))
                                   (= (.-readyState socket) (.-CLOSED js/WebSocket))
                                   (connect!)))))}))
