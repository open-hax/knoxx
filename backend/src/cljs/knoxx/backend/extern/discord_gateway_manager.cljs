(ns knoxx.backend.extern.discord-gateway-manager
  "Discord native gateway composition; the caller owns registry state."
  (:require [knoxx.backend.extern.discord-gateway-client :as client]
            [knoxx.backend.extern.discord-gateway-messages :as messages]
            [knoxx.backend.extern.discord-gateway-voice :as voice-ops]
            [knoxx.backend.extern.discord-gateway-voice-listener :as listener]))

(defn- destroy-voice-connections!
  [voice-connections]
  (.forEach voice-connections (fn [conn _key] (try (.destroy conn) (catch js/Error _))))
  (.clear voice-connections))

(defn- gateway-stop!
  [voice-connections this-stop]
  (destroy-voice-connections! voice-connections)
  (this-stop))

(defn- ^:async gateway-restart!
  [this-stop this-fn token]
  (await (this-stop))
  (await (.start (this-fn) token)))

(defn- register-gateway-listener!
  [listener-set listener]
  (.add @listener-set listener)
  (fn [] (.delete @listener-set listener)))

(defn- ^:async gateway-join-voice!
  [ensure-client voice-connections channel-id]
  (let [conn (await (voice-ops/gw-join-voice ensure-client channel-id))
        guild-id (voice-ops/voice-connection-guild-id conn)]
    (.set voice-connections guild-id conn)
    #js {:guildId guild-id :channelId channel-id :joined true}))

(defn- gateway-active-voice-connection
  [voice-connections guild-id]
  (if guild-id
    (.get voice-connections guild-id)
    (when (> (.-size voice-connections) 0)
      (let [connections (.values voice-connections)]
        (.-value (.next connections))))))

(defn- gateway-lifecycle-methods
  [client-state ready-promise current-token listeners log this-stop build-client this-fn voice-connections]
  #js {:start (fn [token] (client/gw-start client-state ready-promise current-token listeners log this-stop build-client token))
       :stop (fn [] (gateway-stop! voice-connections this-stop))
       :restart (fn [token] (gateway-restart! this-stop this-fn token))
       :status (fn [] (client/gw-status client-state))})

(defn- gateway-listener-methods
  [listeners reaction-listeners voice-state-listeners]
  #js {:onMessage (fn [listener] (register-gateway-listener! listeners listener))
       :onReaction (fn [listener] (register-gateway-listener! reaction-listeners listener))
       :onVoiceStateUpdate (fn [listener] (register-gateway-listener! voice-state-listeners listener))})

(defn- gateway-message-methods
  [ensure-client log this-fn]
  #js {:listServers (fn [] (messages/gw-list-servers ensure-client))
       :listChannels (fn [guild-id] (messages/gw-list-channels ensure-client log guild-id))
       :fetchChannelMessages (fn [channel-id opts] (messages/gw-fetch-channel-messages ensure-client channel-id opts))
       :fetchDmMessages (fn [user-id opts] (messages/gw-fetch-dm-messages ensure-client user-id opts))
       :searchMessages (fn [scope opts] (messages/gw-search-messages (this-fn) scope opts))
       :sendMessage (fn [channel-id text reply-to attachments] (messages/gw-send-message ensure-client channel-id text reply-to attachments))})

(defn- gateway-voice-methods
  [ensure-client voice-connections]
  #js {:joinVoice (fn [channel-id] (gateway-join-voice! ensure-client voice-connections channel-id))
       :leaveVoice (fn [guild-id] (voice-ops/gw-leave-voice voice-connections guild-id) #js {:guildId guild-id :left true})
       :playAudio (fn [guild-id audio-buffer] (voice-ops/gw-play-audio voice-connections guild-id audio-buffer))
       :subscribeVoice (fn [guild-id user-id callback] (voice-ops/gw-subscribe-voice voice-connections guild-id user-id callback))
       :startVoiceListener (fn [guild-id on-start on-audio] (listener/gw-start-voice-listener voice-connections guild-id on-start on-audio))
       :getVoiceConnection (fn [guild-id] (gateway-active-voice-connection voice-connections guild-id))
       :listVoiceMembers (fn [guild-id channel-id] (voice-ops/gw-list-voice-members ensure-client guild-id channel-id))})

(defn- build-gateway-manager-methods
  [client-state ready-promise current-token listeners reaction-listeners voice-state-listeners
   log this-stop build-client ensure-client voice-connections this-fn]
  (js/Object.assign
   #js {}
   (gateway-lifecycle-methods client-state ready-promise current-token listeners log this-stop build-client this-fn voice-connections)
   (gateway-listener-methods listeners reaction-listeners voice-state-listeners)
   (gateway-message-methods ensure-client log this-fn)
   (gateway-voice-methods ensure-client voice-connections)))

(defn- parse-gateway-manager-opts
  "Parse gateway manager options from a CLJS map or JS object."
  [opts]
  {:log (or (when (map? opts) (:log opts))
            (when (object? opts) (aget opts "log"))
            js/console)
   :set-default? (not= false (cond
                              (map? opts) (:set-default? opts)
                              (object? opts) (aget opts "setDefault")
                              :else nil))})

(defn- gateway-manager-state
  []
  {:client-state (atom nil)
   :ready-promise (atom nil)
   :current-token (atom nil)
   :listeners (atom (js/Set.))
   :reaction-listeners (atom (js/Set.))
   :voice-state-listeners (atom (js/Set.))
   :voice-connections (js/Map.)})

(defn- gateway-manager-deps
  [log {:keys [client-state ready-promise listeners reaction-listeners voice-state-listeners]}]
  (let [notify-message (partial client/notify-message! listeners log)
        notify-reaction (partial client/notify-reaction! reaction-listeners log)
        notify-voice-state (partial client/notify-voice-state! voice-state-listeners log)]
    {:build-client (partial client/build-discord-client log notify-message notify-reaction notify-voice-state)
     :ensure-client (partial client/ensure-client! client-state ready-promise)}))

(defn- create-gateway-manager-object!
  [log {:keys [client-state ready-promise current-token listeners reaction-listeners
               voice-state-listeners voice-connections] :as state}]
  (let [{:keys [build-client ensure-client]} (gateway-manager-deps log state)
        this-stop (fn [] (client/gw-stop client-state ready-promise current-token))
        this-obj (atom nil)]
    (letfn [(this-fn [] @this-obj)]
      (reset! this-obj
              (build-gateway-manager-methods
               client-state ready-promise current-token listeners reaction-listeners voice-state-listeners
               log this-stop build-client ensure-client voice-connections this-fn))
      @this-obj)))

(defn createDiscordGatewayManager
  "Create a Discord gateway manager. Returns a JS object with async methods."
  [opts set-default!]
  (let [{:keys [log set-default?]} (parse-gateway-manager-opts opts)
        manager (create-gateway-manager-object! log (gateway-manager-state))]
    (when set-default?
      (set-default! manager))
    manager))

;; ---------------------------------------------------------------------------
;; Convenience CLJS API
;; ---------------------------------------------------------------------------
