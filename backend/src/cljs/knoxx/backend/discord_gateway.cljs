(ns knoxx.backend.discord-gateway
  "Discord gateway manager — native CLJS implementation using discord.js.

   Uses direct discord.js import via shadow-cljs :keep-as-import.
   The :keep-as-import #{\"discord.js\"} in shadow-cljs.edn tells shadow-cljs
   to skip dependency analysis for discord.js, generating a bare import statement.
   Node.js resolves transitive Node.js built-in deps (events, buffer, etc.) at runtime.

   Exported: createDiscordGatewayManager — factory function returning a JS object
   with async methods. Also provides a CLJS convenience API via set-manager!."
  (:require ["discord.js" :as discord]))

;; ---------------------------------------------------------------------------
;; discord.js imports
;; ---------------------------------------------------------------------------

(defn- intent-bits [] (aget discord "GatewayIntentBits"))
(defn- partials-enum [] (aget discord "Partials"))
(defn- events-enum [] (aget discord "Events"))
(defn- channel-type-enum [] (aget discord "ChannelType"))
(defn- Client-class [] (aget discord "Client"))

;; ---------------------------------------------------------------------------
;; Internal helpers
;; ---------------------------------------------------------------------------

(defn- map-message
  "Convert a discord.js Message to a plain JS map."
  [message]
  (let [author (.-author message)]
    #js {:id (.-id message)
         :channelId (.-channelId message)
         :content (or (.-content message) "")
         :authorId (or (when author (.-id author)) "")
         :authorUsername (or (when author (.-username author)) "unknown")
         :authorIsBot (boolean (when author (.-bot author)))
         :timestamp (try (.toISOString (.-createdAt message))
                         (catch js/Error _ (.toISOString (js/Date.))))
         :attachments (into-array
                       (for [[_id att] (.-attachments message)]
                         #js {:id (.-id att)
                              :filename (or (.-name att) "")
                              :contentType (or (.-contentType att) nil)
                              :size (or (.-size att) 0)
                              :url (or (.-url att) "")}))
         :embeds (into-array
                  (for [embed (.-embeds message)]
                    #js {:title (or (.-title embed) nil)
                         :description (or (.-description embed) nil)
                         :url (or (.-url embed) nil)}))}))

(defn- readable-text-channel?
  "Check if a channel is a text-based channel we can read."
  [channel]
  (and channel
       (fn? (.-isTextBased channel))
       (.isTextBased channel)))

(defn- sort-newest-first
  "Sort an array of message maps by timestamp, newest first."
  [messages]
  (js/Array.from (.sort (into-array messages)
                         (fn [a b]
                           (.localeCompare (str (aget b "timestamp"))
                                           (str (aget a "timestamp")))))))

(defn- split-message
  "Split text into chunks of ≤2000 chars, preferring paragraph/line/word breaks."
  [text]
  (let [normalized (.trim (str (or text "")))]
    (if (<= (.-length normalized) 2000)
      #js [normalized]
      (let [parts (atom #js [])
            remaining (atom normalized)]
        (while (> (.-length @remaining) 2000)
          (let [r @remaining
                split-at-para (.lastIndexOf r "\n\n" 2000)
                split-at-line (.lastIndexOf r "\n" 2000)
                split-at-space (.lastIndexOf r " " 2000)
                split-at (cond
                           (> split-at-para 1000) split-at-para
                           (> split-at-line 1000) split-at-line
                           (> split-at-space 1000) split-at-space
                           :else 2000)]
            (swap! parts (fn [p] (.concat p #js [(.trimEnd (.slice r 0 split-at))])))
            (reset! remaining (.trimStart (.slice r split-at)))))
        (when (> (.-length @remaining) 0)
          (swap! parts (fn [p] (.concat p #js [@remaining]))))
        @parts))))

;; ---------------------------------------------------------------------------
;; Gateway method implementations (extracted for readability)
;; ---------------------------------------------------------------------------

(defn- gw-start
  "Start the gateway client with a bot token."
  [client-state ready-promise current-token listeners log this-stop build-client token]
  (let [next-token (.trim (str (or token "")))]
    (if (= next-token "")
      (.then (this-stop) (fn [_] nil))
      (if (and @client-state (= @current-token next-token))
        (if @ready-promise @ready-promise (js/Promise.resolve @client-state))
        (.then (this-stop)
               (fn [_]
                 (reset! current-token next-token)
                 (let [new-client (build-client)]
                   (reset! client-state new-client)
                   (let [login-promise
                         (-> (.login new-client next-token)
                             (.then (fn [_] new-client))
                             (.catch (fn [error]
                                       (when (.-error? log)
                                         (.error log "[discord-gateway] login failed" error))
                                       (try (.destroy new-client) (catch js/Error _))
                                       (reset! client-state nil)
                                       (reset! ready-promise nil)
                                       (reset! current-token nil)
                                       (js/Promise.reject error))))]
                     (reset! ready-promise login-promise)
                     login-promise))))))))

(defn- gw-stop
  "Stop the gateway client."
  [client-state ready-promise current-token]
  (let [result (if @client-state
                 (try (.then (.destroy @client-state) (fn [_] nil))
                      (catch js/Error _ (js/Promise.resolve nil)))
                 (js/Promise.resolve nil))]
    (reset! client-state nil)
    (reset! ready-promise nil)
    (reset! current-token nil)
    result))

(defn- gw-status
  "Get gateway status."
  [client-state]
  (let [c @client-state]
    (cond-> #js {:started (some? c)
                 :ready false
                 :userTag nil
                 :guildCount 0}
      c (doto
          (aset "ready" (try (.isReady c) (catch js/Error _ false)))
          (aset "userTag" (try (.-tag (.-user c)) (catch js/Error _ nil)))
          (aset "guildCount" (try (.. c -guilds -cache -size) (catch js/Error _ 0)))))))
(defn- gw-list-servers
  "List all guilds the bot is in."
  [ensure-client]
  (.then (ensure-client)
         (fn [active-client]
           (into-array
            (for [[_id guild] (.. active-client -guilds -cache)]
              #js {:id (.-id guild)
                   :name (.-name guild)
                   :memberCount (or (.-memberCount guild) nil)})))))

(defn- gw-list-channels
  "List channels in a guild or all guilds."
  [ensure-client log guild-id]
  (.then (ensure-client)
         (fn [active-client]
           (let [ChannelType (channel-type-enum)
                 collect (fn [guild]
                           (-> (.fetch (.. guild -channels))
                               (.then (fn [fetched]
                                        (into-array
                                         (for [[_id ch] fetched
                                               :when (and ch
                                                          (readable-text-channel? ch)
                                                          (not= (.-type ch) (.-DM ChannelType)))]
                                           #js {:id (.-id ch)
                                                :name (or (.-name ch) "")
                                                :guildId (.-id guild)
                                                :type (str (.-type ch))}))))))]
             (if guild-id
               (let [guild (.. active-client -guilds -cache (get guild-id))]
                 (if-not guild
                   (js/Promise.reject (js/Error. (str "Guild not found: " guild-id)))
                   (collect guild)))
               ;; All guilds — collect from each, suppress per-guild errors
               (let [promises (atom #js [])]
                 (doseq [[_id guild] (.. active-client -guilds -cache)]
                   (swap! promises (fn [ps]
                                     (.concat ps #js [(-> (collect guild)
                                                          (.catch (fn [err]
                                                                    (when (.-warn? log)
                                                                      (.warn log "[discord-gateway] listChannels guild failed" (.-id guild) err))
                                                                    #js [])))]))))
                 (.then (js/Promise.all @promises)
                        (fn [results]
                          (let [flat (atom #js [])]
                            (doseq [r results]
                              (swap! flat (fn [f] (.concat f r))))
                            @flat)))))))))

(defn- gw-fetch-channel-messages
  "Fetch messages from a channel."
  [ensure-client channel-id opts]
  (.then (ensure-client)
         (fn [active-client]
           (-> (.fetch (.. active-client -channels) channel-id)
               (.then (fn [channel]
                        (if (or (not channel) (not (readable-text-channel? channel)))
                          (js/Promise.reject (js/Error. (str "Channel not found or not text-based: " channel-id)))
                          (-> (.fetch (.. channel -messages)
                                      (clj->js {:limit (max 1 (min 100 (or (aget opts "limit") 50)))
                                                 :before (aget opts "before")
                                                 :after (aget opts "after")
                                                 :around (aget opts "around")}))
                              (.then (fn [fetched]
                                       (sort-newest-first
                                        (map map-message (for [[_id msg] fetched] msg)))))))))))))

(defn- gw-fetch-dm-messages
  "Fetch DM messages with a user."
  [ensure-client user-id opts]
  (.then (ensure-client)
         (fn [active-client]
           (-> (.fetch (.. active-client -users) user-id)
               (.then (fn [user]
                        (-> (.createDM user)
                            (.then (fn [dm]
                                     (-> (.fetch (.. dm -messages)
                                                 (clj->js {:limit (max 1 (min 100 (or (aget opts "limit") 50)))
                                                            :before (aget opts "before")}))
                                         (.then (fn [fetched]
                                                  #js {:dmChannelId (.-id dm)
                                                       :messages (sort-newest-first
                                                                  (map map-message (for [[_id msg] fetched] msg)))}))))))))))))

(defn- search-filter-fn
  "Create a filter function for message search."
  [opts]
  (let [needle (.toLowerCase (str (or (aget opts "query") "")))
        target-user-id (aget opts "userId")]
    (fn [message]
      (let [content-ok (or (= needle "")
                           (.includes (.toLowerCase (or (aget message "content") "")) needle))
            author-ok (or (not target-user-id)
                          (= (aget message "authorId") target-user-id))]
        (and content-ok author-ok)))))

(defn- gw-search-messages
  "Search messages in a channel or DM."
  [this-fn scope opts]
  (let [normalized-scope (.toLowerCase (str (or scope "channel")))]
    (if (= normalized-scope "dm")
      (-> (.fetchDmMessages this-fn (aget opts "userId")
                            (clj->js {:limit 100 :before (aget opts "before")}))
          (.then (fn [result]
                   (let [filtered (.filter (aget result "messages") (search-filter-fn opts))]
                     #js {:dmChannelId (aget result "dmChannelId")
                          :messages (.slice filtered 0 (or (aget opts "limit") 50))
                          :count (min (.-length filtered) (or (aget opts "limit") 50))
                          :source "gateway-cache"}))))
      (-> (.fetchChannelMessages this-fn (aget opts "channelId")
                                 (clj->js {:limit 100
                                            :before (aget opts "before")
                                            :after (aget opts "after")}))
          (.then (fn [messages]
                   (let [filtered (.filter messages (search-filter-fn opts))]
                     #js {:channelId (aget opts "channelId")
                          :messages (.slice filtered 0 (or (aget opts "limit") 50))
                          :count (min (.-length filtered) (or (aget opts "limit") 50))
                          :source "gateway-cache"})))))))

(defn- gw-send-message
  "Send a message to a channel, splitting into chunks if needed."
  [ensure-client channel-id text reply-to]
  (.then (ensure-client)
         (fn [active-client]
           (-> (.fetch (.. active-client -channels) channel-id)
               (.then (fn [channel]
                        (if (or (not channel) (not (readable-text-channel? channel)))
                          (js/Promise.reject (js/Error. (str "Channel not found or not text-based: " channel-id)))
                          (let [chunks (split-message text)]
                            (-> (.reduce chunks
                                         (fn [promise chunk index]
                                           (.then (or promise (js/Promise.resolve nil))
                                                  (fn [_]
                                                    (let [payload (clj->js {:content chunk})]
                                                      (when (and (= index 0) reply-to)
                                                        (aset payload "reply" (clj->js {:messageReference reply-to})))
                                                      (.send channel payload)))))
                                         nil)
                                (.then (fn [_]
                                         #js {:channelId channel-id
                                              :messageId ""
                                              :sent true
                                              :timestamp (.toISOString (js/Date.))
                                              :chunkCount (.-length chunks)})))))))))))

;; ---------------------------------------------------------------------------
;; Factory
;; ---------------------------------------------------------------------------

(defn createDiscordGatewayManager
  "Create a Discord gateway manager. Returns a JS object with async methods.

   Options (CLJS map or JS object):
     - :log / \"log\": logger object (default: console)

   Methods: start, stop, restart, onMessage, status, listServers,
            listChannels, fetchChannelMessages, fetchDmMessages,
            searchMessages, sendMessage"
  [opts]
  (let [log (or (when (map? opts) (:log opts))
                (when (object? opts) (aget opts "log"))
                js/console)
        client-state (atom nil)
        ready-promise (atom nil)
        current-token (atom nil)
        listeners (atom (js/Set.))]

    (letfn [(notify-message [message]
              (let [mapped (map-message message)]
                (.forEach @listeners
                          (fn [listener]
                            (try
                              (listener mapped message)
                              (catch js/Error error
                                (when (.-error? log)
                                  (.error log "[discord-gateway] listener failed" error))))))))

            (build-client []
              (let [Client (Client-class)
                    GatewayIntentBits (intent-bits)
                    Partials (partials-enum)
                    Events (events-enum)
                    next-client (new Client
                                     (clj->js {:intents [(.-Guilds GatewayIntentBits)
                                                         (.-GuildMessages GatewayIntentBits)
                                                         (.-DirectMessages GatewayIntentBits)
                                                         (.-MessageContent GatewayIntentBits)]
                                               :partials [(.-Channel Partials)]}))]
                (.on next-client (.-ClientReady Events)
                     (fn [ready-client]
                       (when (.-info? log)
                         (.info log (str "[discord-gateway] ready as "
                                         (or (when (.-user ready-client) (.-tag (.-user ready-client))) "unknown")
                                         " in " (.. ready-client -guilds -cache -size) " guilds")))))
                (.on next-client (.-MessageCreate Events)
                     (fn [message] (notify-message message)))
                (.on next-client (.-Error Events)
                     (fn [error]
                       (when (.-error? log)
                         (.error log "[discord-gateway] client error" error))))
                next-client))

            (ensure-client []
              (if-not @client-state
                (js/Promise.reject (js/Error. "Discord gateway client is not started"))
                (if @ready-promise
                  (.then @ready-promise (fn [_] @client-state))
                  (js/Promise.resolve @client-state))))]

      (let [this-stop (fn [] (gw-stop client-state ready-promise current-token))
            this-obj (atom nil)]

        (letfn [(this-fn [] @this-obj)]

          (reset! this-obj
                  #js {:start (fn [token] (gw-start client-state ready-promise current-token listeners log this-stop build-client token))
                       :stop this-stop
                       :restart (fn [token] (.then (this-stop) (fn [_] (.start (this-fn) token))))
                       :onMessage (fn [listener] (.add @listeners listener) (fn [] (.delete @listeners listener)))
                       :status (fn [] (gw-status client-state))
                       :listServers (fn [] (gw-list-servers ensure-client))
                       :listChannels (fn [guild-id] (gw-list-channels ensure-client log guild-id))
                       :fetchChannelMessages (fn [channel-id opts] (gw-fetch-channel-messages ensure-client channel-id opts))
                       :fetchDmMessages (fn [user-id opts] (gw-fetch-dm-messages ensure-client user-id opts))
                       :searchMessages (fn [scope opts] (gw-search-messages (this-fn) scope opts))
                       :sendMessage (fn [channel-id text reply-to] (gw-send-message ensure-client channel-id text reply-to))})

          (set-manager! @this-obj)
          @this-obj)))))

;; ---------------------------------------------------------------------------
;; Convenience CLJS API
;; ---------------------------------------------------------------------------

(defonce ^:private manager* (atom nil))

(defn set-manager!
  "Store the gateway manager instance for CLJS API access."
  [m]
  (reset! manager* m))

(defn gateway-manager
  "Returns the current gateway manager instance (or nil)."
  []
  @manager*)

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
  [channel-id text reply-to]
  (when-let [manager @manager*]
    (.sendMessage manager channel-id text reply-to)))
