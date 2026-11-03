(ns knoxx.backend.extern.discord-gateway-client
  "Discord native client lifecycle and event delivery boundary."
  (:require [knoxx.backend.extern.discord-gateway-codec :as codec]))

(defn notify-message!
  "Discord gateway operation: notify-message!."
  [listeners log message]
  (let [mapped (codec/map-message message)
        log-error (codec/log-fn log :error)]
    (.forEach @listeners
              (fn [listener]
                (try
                  (listener mapped message)
                  (catch js/Error error
                    (when log-error
                      (log-error "[discord-gateway] listener failed" error))))))))

(defn notify-reaction!
  "Discord gateway operation: notify-reaction!."
  [reaction-listeners log reaction user]
  (let [message (.-message reaction)
        emoji (.-emoji reaction)
        mapped #js {:emoji (or (.-name emoji) "")
                    :message (when message (codec/map-message message))
                    :messageId (or (when message (.-id message)) "")
                    :channelId (or (when message (.-channelId message)) "")
                    :userId (or (when user (.-id user)) "")
                    :userUsername (or (when user (.-username user)) "unknown")}
        log-error (codec/log-fn log :error)]
    (.forEach @reaction-listeners
              (fn [listener]
                (try
                  (listener mapped reaction user)
                  (catch js/Error error
                    (when log-error
                      (log-error "[discord-gateway] reaction listener failed" error))))))))

(defn notify-voice-state!
  "Discord gateway operation: notify-voice-state!."
  [voice-state-listeners log old-state new-state]
  (let [old-channel-id (when old-state (.-channelId old-state))
        new-channel-id (when new-state (.-channelId new-state))
        user (when new-state (.-member new-state))
        user-id (when user (.-id user))
        guild-id (when new-state (.-guild new-state) (.-id (.-guild new-state)))
        action (cond
                 (and (nil? old-channel-id) new-channel-id) "join"
                 (and old-channel-id (nil? new-channel-id)) "leave"
                 (and old-channel-id new-channel-id
                      (not= old-channel-id new-channel-id)) "move"
                 :else nil)
        mapped #js {:action action
                    :userId user-id
                    :username (or (when user (.-user user)) (when user (.-username user)) "unknown")
                    :guildId guild-id
                    :channelId (or new-channel-id old-channel-id)
                    :oldChannelId old-channel-id
                    :newChannelId new-channel-id}
        log-error (codec/log-fn log :error)]
    (when action
      (.forEach @voice-state-listeners
                (fn [listener]
                  (try
                    (listener mapped old-state new-state)
                    (catch js/Error error
                      (when log-error
                        (log-error "[discord-gateway] voice state listener failed" error)))))))))

(defn- handle-client-ready
  [log-info ready-client]
  (when log-info
    (log-info (str "[discord-gateway] ready as "
                   (or (when (.-user ready-client) (.-tag (.-user ready-client))) "unknown")
                   " in " (.. ready-client -guilds -cache -size) " guilds"))))

(defn- handle-message-create
  [notify-message message]
  (notify-message message))

(defn- ^:async fetch-partial!
  [x]
  (if (.-partial x)
    (await (.fetch x))
    x))

(defn- ^:async fetch-reaction-message!
  [reaction]
  (let [message (.-message reaction)]
    (when (and message (.-partial message))
      (await (.fetch message)))
    message))

(defn- ^:async handle-reaction-add
  [log-warn notify-reaction reaction user]
  (try
    (let [full-reaction (await (fetch-partial! reaction))]
      (await (fetch-reaction-message! full-reaction))
      (notify-reaction full-reaction user))
    (catch js/Error error
      (when log-warn
        (log-warn "[discord-gateway] reaction ingest failed" error)))))

(defn- handle-client-error
  [log-error error]
  (when log-error
    (log-error "[discord-gateway] client error" error)))

(defn- handle-voice-state-update
  [notify-voice-state old-state new-state]
  (notify-voice-state old-state new-state))

(defn build-discord-client
  "Create a new discord.js Client and attach event listeners."
  [log notify-message notify-reaction notify-voice-state]
  (let [Client (codec/Client-class)
        GatewayIntentBits (codec/intent-bits)
        Partials (codec/partials-enum)
        Events (codec/events-enum)
        log-info (codec/log-fn log :info)
        log-warn (codec/log-fn log :warn)
        log-error (codec/log-fn log :error)
        next-client (new Client
                        (clj->js {:intents [(.-Guilds GatewayIntentBits)
                                            (.-GuildMessages GatewayIntentBits)
                                            (.-DirectMessages GatewayIntentBits)
                                            (.-GuildMessageReactions GatewayIntentBits)
                                            (.-DirectMessageReactions GatewayIntentBits)
                                            (.-GuildVoiceStates GatewayIntentBits)
                                            (.-MessageContent GatewayIntentBits)]
                                  :partials [(.-Channel Partials)
                                             (.-Message Partials)
                                             (.-Reaction Partials)]}))]
    (.on next-client (.-ClientReady Events) (partial handle-client-ready log-info))
    (.on next-client (.-MessageCreate Events) (partial handle-message-create notify-message))
    (.on next-client (.-MessageReactionAdd Events) (partial handle-reaction-add log-warn notify-reaction))
    (.on next-client (.-Error Events) (partial handle-client-error log-error))
    (.on next-client (.-VoiceStateUpdate Events) (partial handle-voice-state-update notify-voice-state))
    next-client))

(defn ^:async ensure-client!
  "Discord gateway operation: ensure-client!."
  [client-state ready-promise]
  (when-not @client-state
    (throw (js/Error. "Discord gateway client is not started")))
  (when @ready-promise
    (await @ready-promise))
  @client-state)

(defn- reset-client-state!
  [client-state ready-promise current-token]
  (reset! client-state nil)
  (reset! ready-promise nil)
  (reset! current-token nil))

(defn- log-login-failed!
  [log error]
  (when-let [log-error (codec/log-fn log :error)]
    (log-error "[discord-gateway] login failed" error)))

(defn- ^:async login-client!
  [client-state ready-promise current-token log new-client token]
  (try
    (await (.login new-client token))
    new-client
    (catch js/Error error
      (log-login-failed! log error)
      (try (.destroy new-client) (catch js/Error _))
      (reset-client-state! client-state ready-promise current-token)
      (throw error))))

(defn ^:async gw-start
  "Start the gateway client with a bot token."
  [client-state ready-promise current-token _listeners log this-stop build-client token]
  (let [next-token (.trim (str (or token "")))]
    (cond
      (= next-token "") (do (await (this-stop)) nil)
      (and @client-state (= @current-token next-token)) (if @ready-promise
                                                           (await @ready-promise)
                                                           @client-state)
      :else (do
              (await (this-stop))
              (reset! current-token next-token)
              (let [new-client (build-client)
                    login-promise (login-client! client-state ready-promise current-token log new-client next-token)]
                (reset! client-state new-client)
                (reset! ready-promise login-promise)
                (await login-promise))))))

(defn ^:async gw-stop
  "Stop the gateway client."
  [client-state ready-promise current-token]
  (when-let [client @client-state]
    (try
      (await (.destroy client))
      (catch js/Error _ nil)))
  (reset-client-state! client-state ready-promise current-token)
  nil)

(defn gw-status
  "Get gateway status."
  [client-state]
  (let [c @client-state]
    (cond-> #js {:started (some? c)
                 :ready false
                 :userId nil
                 :userTag nil
                 :guildCount 0}
      c (doto
            (aset "ready" (try (.isReady c) (catch js/Error _ false)))
          (aset "userId" (try (.-id (.-user c)) (catch js/Error _ nil)))
          (aset "userTag" (try (.-tag (.-user c)) (catch js/Error _ nil)))
          (aset "guildCount" (try (.. c -guilds -cache -size) (catch js/Error _ 0)))))))
