(ns knoxx.backend.tools.twitch
  "Twitch IRC tools for monitoring and interacting with chat."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.text :refer [tool-text-result]]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional live-config]]))

(defonce ^:private twitch-connection
  (atom nil))

(defonce ^:private chat-buffer
  (atom {})) ;; {channel-id [{user message timestamp} ...]}

(defn- get-twitch-config [config]
  (let [token (:twitch-oauth-token (live-config config))
        user (:twitch-username (live-config config))]
    (when (or (str/blank? token) (str/blank? user))
      (throw (js/Error. "Twitch credentials (TWITCH_OAUTH_TOKEN, TWITCH_USERNAME) not configured.")))
    {:token token :user user}))

(defn- parse-twitch-message [msg]
  (let [text-msg (str msg)]
    ;; Standard Twitch IRC message: @badges=... :username!username@username.tmi.twitch.tv PRIVMSG #channel :message
    (if (str/includes? text-msg " PRIVMSG ")
      (let [parts (str/split text-msg #" PRIVMSG ")
            header (first parts)
            body (str/join " PRIVMSG " (rest parts))
            user-part (some-> header (str/split #":") last (str/split #"!"))
            username (if (seq user-part) (first user-part) "unknown")
            channel-part (some-> body (str/split #"\s+") first)
            channel (if (seq channel-part) (str/replace (first channel-part) #"^#" "") "unknown")
            message (if (str/includes? body " :")
                      (str/substring body (inc (.indexOf body ":")))
                      (str/substring body (if (seq channel-part) (inc (.indexOf body (first channel-part))) 0)))]
        {:username username
         :channel channel
         :message (str/trim message)})
      {:username "system" :channel "global" :message text-msg})))

(defn- handle-twitch-message [msg]
  (let [{:keys [username channel message]} (parse-twitch-message msg)]
    (swap! chat-buffer (fn [buffer]
                         (let [messages (get buffer channel [])
                               new-msg {:user username :text message :timestamp (js/Date.)}
                               updated-msgs (conj messages new-msg)]
                           (assoc buffer channel (take 100 updated-msgs)))))))

(defn- connect-twitch! [config]
  (let [{:keys [token user]} (get-twitch-config config)]
    (if-let [conn @twitch-connection]
      (js/Promise.resolve conn)
      (let [ws (js/WebSocket. "wss://irc-ws.chat.twitch.tv:443")]
        (-> (.on ws "open" (fn []
                             (.send ws (str "PASS oauth:" token))
                             (.send ws (str "NICK " user))
                             (println "Twitch connection opened.")))
            (.on ws "message" (fn [event]
                               (handle-twitch-message (.data event))))
            (.on ws "close" (fn []
                              (reset! twitch-connection nil)
                              (println "Twitch connection closed.")))
            (.on ws "error" (fn [err]
                               (println "Twitch connection error:" js/JSON.stringify err)))
            (do (swap! twitch-connection (fn [_] ws))
                (js/Promise.resolve ws)))))))

(defn- twitch-send-message! [config channel-id text]
  (let [ws (connect-twitch! config)]
    (.then ws (fn [conn]
                (let [formatted-channel (str "#" (str/replace channel-id #"^#" ""))]
                  (.send conn (str "PRIVMSG " formatted-channel " :" text))
                  {:sent true :channel channel-id :text text}))))))

(defn- twitch-join-channel! [config channel-id]
  (let [ws (connect-twitch! config)]
    (.then ws (fn [conn]
                (let [formatted-channel (str "#" (str/replace channel-id #"^#" ""))]
                  (.send conn (str "JOIN " formatted-channel))
                  {:joined true :channel channel-id}))))))

(defn- twitch-read-messages! [config channel-id]
  (let [formatted-channel (str/replace channel-id #"^#" "")]
    (let [messages (get @chat-buffer formatted-channel [])]
      (js/Promise.resolve {:messages messages :count (count messages)}))))

(defn create-twitch-custom-tools
  ([runtime config] (create-twitch-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))
         send-params (.Object Type
                              #js {:channel_id (.String Type #js {:description "Twitch channel name (e.g. 'error0815')."})
                                   :text (.String Type #js {:description "Message to send to Twitch chat."})})
         read-params (.Object Type
                                #js {:channel_id (.String Type #js {:description "Twitch channel name to read messages from."})})
         join-params (.Object Type
                                #js {:channel_id (.String Type #js {:description "Twitch channel name to join/monitor."})})

         twitch-send-execute (fn [_tool-call-id params a b c]
                                (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                      channel-id (or (aget params "channel_id") (aget params "channelId") "")
                                      text (or (aget params "text") "")]
                                  (maybe-tool-update! on-update (str "Sending Twitch message to " channel-id "…"))
                                  (-> (twitch-send-message! config channel-id text)
                                      (.then (fn [result]
                                               (tool-text-result (str "Sent Twitch message to " channel-id)
                                                                 result))))))

         twitch-read-execute (fn [_tool-call-id params a b c]
                                (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                      channel-id (or (aget params "channel_id") (aget params "channelId") "")]
                                  (maybe-tool-update! on-update (str "Reading Twitch chat from " channel-id "…"))
                                  (-> (twitch-read-messages! config channel-id)
                                      (.then (fn [result]
                                               (let [msgs (->> (:messages result)
                                                               (map (fn [m] (str "<" (:user m) "> " (:text m)))
                                                               (str/join "\n"))]
                                                 (tool-text-result (str "Recent messages from " channel-id ":\n" msgs)
                                                                   result)))))))

         twitch-join-execute (fn [_tool-call-id params a b c]
                                (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                      channel-id (or (aget params "channel_id") (aget params "channelId") "")]
                                  (maybe-tool-update! on-update (str "Joining Twitch channel " channel-id "…"))
                                  (-> (twitch-join-channel! config channel-id)
                                      (.then (fn [result]
                                               (tool-text-result (str "Joined Twitch channel " channel-id ". Now monitoring...")
                                                                  result))))))

         ]

     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "twitch.send")
                  (doto (js-obj)
                    (aset "name" "twitch.send")
                    (aset "label" "Twitch Send")
                    (aset "description" "Send a message to a Twitch channel.")
                    (aset "promptSnippet" "Chat with viewers on Twitch.")
                    (aset "parameters" send-params)
                    (aset "execute" twitch-send-execute)))
                (when (allowed? "twitch.read")
                  (doto (js-obj)
                    (aset "name" "twitch.read")
                    (aset "label" "Twitch Read")
                    (aset "description" "Read recent messages from a Twitch channel.")
                    (aset "promptSnippet" "Read Twitch chat history.")
                    (aset "parameters" read-params)
                    (aset "execute" twitch-read-execute)))
                (when (allowed? "twitch.monitor")
                  (doto (js-obj)
                    (aset "name" "twitch.monitor")
                    (aset "label" "Twitch Monitor")
                    (aset "description" "Join a Twitch channel to start monitoring chat.")
                    (aset "promptSnippet" "Monitor a Twitch stream for phrases.")
                    (aset "parameters" join-params)
                    (aset "execute" twitch-join-execute)))
                ]))))))
