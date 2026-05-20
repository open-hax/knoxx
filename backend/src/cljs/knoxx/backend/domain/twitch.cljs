(ns knoxx.backend.tools.twitch
  "Twitch IRC tools for monitoring and interacting with chat."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.domain.text :refer [tool-text-result]]
            [knoxx.backend.domain.actor.credentials :as actor-credentials]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! create-tool-obj]]))

(defonce ^:private twitch-connections
  (atom {}))

(defonce ^:private chat-buffer
  (atom {}))

(def send-params
  [:map
   [:channel_id {:description "Twitch channel name (e.g. 'error0815')."} :string]
   [:text {:description "Message to send to Twitch chat."} :string]])

(def read-params
  [:map
   [:channel_id {:description "Twitch channel name to read messages from."} :string]])

(def join-params
  [:map
   [:channel_id {:description "Twitch channel name to join/monitor."} :string]])

(defn- get-twitch-config! [runtime]
  (-> (actor-credentials/get-credential! runtime "twitch")
      (.then (fn [credential]
               (let [token (actor-credentials/secret-value credential :oauthToken :oauth-token :token)
                     user (or (actor-credentials/secret-value credential :username :login)
                              (:accountIdentifier credential))]
                 (when (or (str/blank? (str token))
                           (str/blank? (str user)))
                   (throw (js/Error. "Twitch actor credential must include username and oauthToken.")))
                 {:token (str/replace token #"^oauth:" "") :user user}))))

(defn- parse-twitch-message [msg]
  (let [text-msg (str msg)]
    (if (str/includes? text-msg " PRIVMSG ")
      (let [parts (str/split text-msg #" PRIVMSG ")
            header (first parts)
            body (str/join " PRIVMSG " (rest parts))
            user-part (some-> header (str/split #":") last (str/split #"!"))
            username (if (seq user-part) (first user-part) "unknown")
            channel-part (some-> body (str/split #"\s+") first)
            channel (if (seq channel-part) (str/replace (first channel-part) #"^#" "") "unknown")
            message (if (str/includes? body " :")
                      (subs body (inc (.indexOf body ":")))
                      (subs body (if (seq channel-part) (inc (.indexOf body (first channel-part))) 0)))]
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

(defn- connect-twitch! [runtime]
  (-> (get-twitch-config! runtime)
      (.then (fn [{:keys [token user]}]
               (if-let [conn (get @twitch-connections user)]
                 conn
                 (let [ws (js/WebSocket. "wss://irc-ws.chat.twitch.tv:443")]
                   (-> (.on ws "open" (fn []
                                         (.send ws (str "PASS oauth:" token))
                                         (.send ws (str "NICK " user))
                                         (println "Twitch connection opened for actor credential.")))
                       (.on ws "message" (fn [event]
                                            (handle-twitch-message (.data event))))
                       (.on ws "close" (fn []
                                          (swap! twitch-connections dissoc user)
                                          (println "Twitch connection closed.")))
                       (.on ws "error" (fn [err]
                                          (println "Twitch connection error:" js/JSON.stringify err)))
                       (do (swap! twitch-connections assoc user ws)
                           ws)))))))

(defn- twitch-send-message! [runtime channel-id text]
  (-> (connect-twitch! runtime)
      (.then (fn [conn]
               (let [formatted-channel (str "#" (str/replace channel-id #"^#" ""))]
                 (.send conn (str "PRIVMSG " formatted-channel " :" text))
                 {:sent true :channel channel-id :text text})))))

(defn- twitch-join-channel! [runtime channel-id]
  (-> (connect-twitch! runtime)
      (.then (fn [conn]
               (let [formatted-channel (str "#" (str/replace channel-id #"^#" ""))]
                 (.send conn (str "JOIN " formatted-channel))
                 {:joined true :channel channel-id})))))

(defn- twitch-read-messages! [channel-id]
  (let [formatted-channel (str/replace channel-id #"^#" "")
        messages (get @chat-buffer formatted-channel [])]
    (js/Promise.resolve {:messages messages :count (count messages)})))

(defn twitch-send-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (aget params "channel_id") (aget params "channelId") "")
        text (or (aget params "text") "")]
    (maybe-tool-update! on-update (str "Sending Twitch message to " channel-id "…"))
    (-> (twitch-send-message! runtime channel-id text)
        (.then (fn [result]
                 (tool-text-result (str "Sent Twitch message to " channel-id)
                                   result))))))

(defn twitch-read-execute [_runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (aget params "channel_id") (aget params "channelId") "")]
    (maybe-tool-update! on-update (str "Reading Twitch chat from " channel-id "…"))
    (-> (twitch-read-messages! channel-id)
        (.then (fn [result]
                 (let [msgs (->> (:messages result)
                                 (map (fn [m] (str "<" (:user m) "> " (:text m))))
                                 (str/join "\n"))]
                   (tool-text-result (str "Recent messages from " channel-id ":\n" msgs)
                                     result)))))))

(defn twitch-join-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (aget params "channel_id") (aget params "channelId") "")]
    (maybe-tool-update! on-update (str "Joining Twitch channel " channel-id "…"))
    (-> (twitch-join-channel! runtime channel-id)
        (.then (fn [result]
                 (tool-text-result (str "Joined Twitch channel " channel-id ". Now monitoring...")
                                   result))))))

(def twitch-send-tool
  (partial create-tool-obj
           "twitch.send"
           "Twitch Send"
           "Send a message to a Twitch channel."
           "Chat with viewers on Twitch."
           []
           send-params
           twitch-send-execute))

(def twitch-read-tool
  (partial create-tool-obj
           "twitch.read"
           "Twitch Read"
           "Read recent messages from a Twitch channel."
           "Read Twitch chat history."
           []
           read-params
           twitch-read-execute))

(def twitch-monitor-tool
  (partial create-tool-obj
           "twitch.monitor"
           "Twitch Monitor"
           "Join a Twitch channel to start monitoring chat."
           "Monitor a Twitch stream for phrases."
           []
           join-params
           twitch-join-execute))

(defn create-twitch-custom-tools
  ([runtime config] (create-twitch-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "twitch.send")
                  (twitch-send-tool runtime config))
                (when (allowed? "twitch.read")
                  (twitch-read-tool runtime config))
                (when (allowed? "twitch.monitor")
                  (twitch-monitor-tool runtime config))]))))))
