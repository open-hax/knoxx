(ns knoxx.backend.domain.discord.gateway
  "Compatibility API for opaque Discord manager handles. Registry defonce state remains here across reloads; native SDK operations belong to named extern boundaries."
  (:require ["node:module" :as node-module]
            [clojure.string :as str]
            [knoxx.backend.extern.discord-gateway-manager :as manager]))

;; CommonJS avoids the missing libsodium.mjs in libsodium-wrappers 0.7.16.
(defonce ^{:doc "True after the native Discord voice crypto dependency has loaded."}
  libsodium-wrappers-loaded?
  (let [req (node-module/createRequire (str (.cwd js/process) "/"))]
    (req "libsodium-wrappers")
    true))

(declare set-manager!)

(defn createDiscordGatewayManager
  "Create a native Discord manager, optionally installing it as the default."
  [opts]
  (manager/createDiscordGatewayManager opts set-manager!))

(defonce ^:private manager* (atom nil))
(defonce ^:private actor-managers* (atom {}))

(defn set-manager!
  "Store the gateway manager instance for CLJS API access."
  [m]
  (reset! manager* m))

(defn gateway-manager
  "Returns the legacy/default gateway manager instance, or an actor-owned manager."
  ([] @manager*)
  ([actor-id]
   (if-let [id (some-> actor-id str str/trim not-empty)]
     (get @actor-managers* id)
     @manager*)))

(defn set-actor-manager!
  "Store `manager` for exactly one actor, or remove it when manager is nil.

   The actor id is mandatory even for removal. That keeps a blank actor from
   becoming an alias for the legacy/default manager, which would turn an
   actor-scoped caller into a process-scoped one."
  [actor-id manager]
  (let [actor-id (some-> actor-id str str/trim not-empty)]
    (when-not actor-id
      (throw (js/Error. "actor id is required for Discord actor gateway")))
    (if (nil? manager)
      (swap! actor-managers* dissoc actor-id)
      (swap! actor-managers* assoc actor-id manager))
    manager))

(defn gateway-managers
  "Returns a map of actor-id to actor-owned Discord gateway managers."
  []
  @actor-managers*)

(defn- credential-value
  [credential k]
  (or (when (map? credential) (get credential k))
      (when (map? credential) (get credential (keyword k)))
      (when (map? credential) (get credential (name k)))
      (when (object? credential) (aget credential (name k)))))

(defn- credential-secret-value
  [credential & ks]
  (let [secrets (credential-value credential :secretJson)]
    (some (fn [k]
            (some-> (or (credential-value secrets k)
                        (credential-value secrets (keyword k))
                        (credential-value secrets (name k)))
                    str
                    str/trim
                    not-empty))
          ks)))

(defn- credential-actor-id
  [credential]
  (some-> (or (credential-value credential :actorId)
              (credential-value credential :actor-id)
              (credential-value credential :actor_id))
          str
          str/trim
          not-empty))

(defn- credential-bot-token
  [credential]
  (credential-secret-value credential :botToken :bot-token :token))

(defn ensure-actor-manager!
  "Discord gateway operation: ensure-actor-manager!."
  [actor-id]
  (let [actor-id (some-> actor-id str str/trim not-empty)]
    (when-not actor-id
      (throw (js/Error. "actor id is required for Discord actor gateway")))
    (or (get @actor-managers* actor-id)
        (let [manager (createDiscordGatewayManager #js {:log js/console :setDefault false})]
          (set-actor-manager! actor-id manager)
          manager))))

(defn ^:async start-actor-gateway!
  "Discord gateway operation: start-actor-gateway!."
  [actor-id token]
  (let [manager (ensure-actor-manager! actor-id)]
    (await (.start manager token))
    {:actorId actor-id
     :status (js->clj (.status manager) :keywordize-keys true)}))

(defn- credential->actor-gateway-start
  [credential]
  (let [actor-id (credential-actor-id credential)
        token (credential-bot-token credential)]
    (when (and actor-id token)
      {:actorId actor-id :token token})))

(defn- actor-gateway-starts
  [credentials]
  (->> (js->clj (or credentials #js []) :keywordize-keys true)
       (keep credential->actor-gateway-start)
       vec))

(defn- stop-inactive-actor-gateways!
  [active-actor-ids]
  (doseq [[actor-id manager] @actor-managers*]
    (when-not (contains? active-actor-ids actor-id)
      (try
        (.stop manager)
        (catch js/Error _)
        (finally
          (swap! actor-managers* dissoc actor-id))))))

(defn- ^:async start-actor-gateway-best-effort!
  [{:keys [actorId token]}]
  (try
    (await (start-actor-gateway! actorId token))
    (catch js/Error err
      (.warn js/console "[discord-gateway] actor gateway start failed" actorId (.-message err))
      {:actorId actorId :error (.-message err)})))

(defn ^:async start-actor-gateways!
  "Discord gateway operation: start-actor-gateways!."
  [credentials]
  (let [valid (actor-gateway-starts credentials)
        active-actor-ids (set (map :actorId valid))
        starts (clj->js (mapv start-actor-gateway-best-effort! valid))]
    (stop-inactive-actor-gateways! active-actor-ids)
    (js->clj (await (js/Promise.all starts)) :keywordize-keys true)))

(defn started?
  "Returns true if the gateway client exists."
  []
  (some? @manager*))

(defn ready?
  "Returns true if the gateway client is connected and ready."
  []
  (when-let [manager @manager*]
    (let [s (.status manager)]
      (boolean (aget s "ready")))))

(defn status
  "Get gateway status as a JS object."
  []
  (when-let [manager @manager*]
    (.status manager)))

(defn start!
  "Start the Discord gateway with the given token."
  [token]
  (when-let [manager @manager*]
    (.start manager token)))

(defn stop!
  "Stop the Discord gateway client."
  []
  (when-let [manager @manager*]
    (.stop manager)))

(defn restart!
  "Stop and restart with the given token."
  [token]
  (when-let [manager @manager*]
    (.restart manager token)))

(defn on-message!
  "Register a message listener. Returns an unsubscribe function."
  [listener]
  (when-let [manager @manager*]
    (.onMessage manager listener)))

(defn on-reaction!
  "Register a reaction listener. Returns an unsubscribe function."
  [listener]
  (when-let [manager @manager*]
    (.onReaction manager listener)))

(defn on-voice-state-update!
  "Register a voice state update listener. Returns an unsubscribe function."
  [listener]
  (when-let [manager @manager*]
    (.onVoiceStateUpdate manager listener)))

(defn list-servers
  "List all guilds the bot is in. Returns a Promise."
  []
  (when-let [manager @manager*]
    (.listServers manager)))

(defn list-channels
  "List channels in a guild (or all guilds if guild-id is nil). Returns a Promise."
  ([]
   (when-let [manager @manager*]
     (.listChannels manager)))
  ([guild-id]
   (when-let [manager @manager*]
     (.listChannels manager guild-id))))

(defn fetch-channel-messages
  "Fetch messages from a channel. Returns a Promise."
  [channel-id opts]
  (when-let [manager @manager*]
    (.fetchChannelMessages manager channel-id opts)))

(defn fetch-dm-messages
  "Fetch DM messages with a user. Returns a Promise."
  [user-id opts]
  (when-let [manager @manager*]
    (.fetchDmMessages manager user-id opts)))

(defn search-messages
  "Search messages in a channel or DM. Returns a Promise."
  [scope opts]
  (when-let [manager @manager*]
    (.searchMessages manager scope opts)))

(defn send-message
  "Send a message to a channel. Returns a Promise."
  ([channel-id text reply-to]
   (send-message channel-id text reply-to nil))
  ([channel-id text reply-to attachments]
   (when-let [manager @manager*]
     (.sendMessage manager channel-id text reply-to attachments))))

;; Voice convenience API

(defn join-voice
  "Join a voice channel. Returns a Promise."
  [channel-id]
  (when-let [manager @manager*]
    (.joinVoice manager channel-id)))

(defn leave-voice
  "Leave a voice channel for a guild. Returns a Promise."
  [guild-id]
  (when-let [manager @manager*]
    (.leaveVoice manager guild-id)))

(defn play-audio
  "Play an audio buffer in a voice channel. Returns a Promise."
  [guild-id audio-buffer]
  (when-let [manager @manager*]
    (.playAudio manager guild-id audio-buffer)))

(defn start-voice-listener
  "Start listening for voice input. Returns a Promise of a stop function."
  [guild-id on-start on-audio]
  (when-let [manager @manager*]
    (.startVoiceListener manager guild-id on-start on-audio)))

(defn get-voice-connection
  "Get the current voice connection for a guild."
  [guild-id]
  (when-let [manager @manager*]
    (.getVoiceConnection manager guild-id)))

(defn list-voice-members
  "List members in a voice channel. Returns a Promise."
  [guild-id channel-id]
  (when-let [manager @manager*]
    (.listVoiceMembers manager guild-id channel-id)))
