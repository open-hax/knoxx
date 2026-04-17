(ns knoxx.backend.discord-gateway
  "Discord gateway manager — CLJS API wrapping the JS discord-gateway module.

   The discord.js Client is created and managed by discord-gateway.mjs (loaded
   by server.mjs) and exposed via globalThis.knoxxDiscordGateway.  This namespace
   provides a clean CLJS API so consumers don't need to reach through globalThis.

   The discord.js package uses node:events/node:buffer requires that shadow-cljs
   cannot analyse with :js-provider :import.  Keeping the JS gateway module as the
   runtime owner avoids that classpath issue while still giving us a CLJS API.

   When discord.js adds ESM-first support or shadow-cljs handles node: protocol
   requires natively, the full port can be completed by inlining the JS code here.")

;; ---------------------------------------------------------------------------
;; Internal accessor
;; ---------------------------------------------------------------------------

(defn- gw
  "Access the gateway manager object from globalThis."
  []
  (aget js/globalThis "knoxxDiscordGateway"))

(defn started?
  "Returns true if the gateway client exists."
  []
  (some? (gw)))

(defn ready?
  "Returns true if the gateway client is connected and ready."
  []
  (when-let [manager (gw)]
    (let [status (.status manager)]
      (boolean (or (aget status "ready")
                   (aget status "started"))))))

(defn status
  "Get gateway status as a JS object."
  []
  (when-let [manager (gw)]
    (.status manager)))

(defn start!
  "Start the Discord gateway with the given token."
  [token]
  (when-let [manager (gw)]
    (.start manager token)))

(defn stop!
  "Stop the Discord gateway client."
  []
  (when-let [manager (gw)]
    (.stop manager)))

(defn restart!
  "Stop and restart with the given token."
  [token]
  (when-let [manager (gw)]
    (.restart manager token)))

(defn on-message!
  "Register a message listener. Returns an unsubscribe function."
  [listener]
  (when-let [manager (gw)]
    (.onMessage manager listener)))

(defn list-servers
  "List all guilds the bot is in. Returns a Promise."
  []
  (when-let [manager (gw)]
    (.listServers manager)))

(defn list-channels
  "List channels in a guild (or all guilds if guild-id is nil). Returns a Promise."
  ([]
   (when-let [manager (gw)]
     (.listChannels manager)))
  ([guild-id]
   (when-let [manager (gw)]
     (.listChannels manager guild-id))))

(defn fetch-channel-messages
  "Fetch messages from a channel. Returns a Promise."
  [channel-id opts]
  (when-let [manager (gw)]
    (.fetchChannelMessages manager channel-id opts)))

(defn fetch-dm-messages
  "Fetch DM messages with a user. Returns a Promise."
  [user-id opts]
  (when-let [manager (gw)]
    (.fetchDmMessages manager user-id opts)))

(defn search-messages
  "Search messages in a channel or DM. Returns a Promise."
  [scope opts]
  (when-let [manager (gw)]
    (.searchMessages manager scope opts)))

(defn send-message
  "Send a message to a channel. Returns a Promise."
  [channel-id text reply-to]
  (when-let [manager (gw)]
    (.sendMessage manager channel-id text reply-to)))

(defn gateway-manager
  "Returns the raw JS gateway manager object (backward compat).
   New code should prefer the CLJS fns above."
  []
  (gw))
