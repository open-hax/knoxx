(ns knoxx.backend.e2e.discord-double
  "A recording stand-in for the Discord gateway manager.

   \"Gateway not started\" is a true answer and a useless one: it says the tool
   refused before doing anything, which tells us nothing about whether it would
   drive the gateway correctly, or as whom. This double can be installed as an
   actor-owned manager or as the legacy default, so identity selection itself
   is observable rather than assumed."
  (:require [knoxx.backend.domain.discord.gateway :as gateway]))

(defn- record!
  [calls* method args]
  (swap! calls* conj {:method method :args (vec args)}))

(defn- method-answers
  "[JS name, call keyword, answer-fn, async?] for every method the tools reach.

   Data rather than a wall of closures, so the modelled surface can be read at
   a glance — and so what is *not* modelled is equally visible. An unmodelled
   method is left absent on purpose: a tool reaching for one fails loudly
   rather than reading undefined, which is how discord.voice.status's
   getVoiceConnection was found missing here rather than assumed present."
  [started* {:keys [identity-token bot-user-id guild-id connection]}]
  [["start" :start (fn [_] (reset! started* true) #js {:ok true}) true]
   ["status" :status (fn [_] #js {:running @started*
                                  :ready @started*
                                  :botUserId (or bot-user-id "e2e-bot-user")
                                  :token identity-token}) false]
   ["joinVoice" :join-voice (fn [channel-id] #js {:guildId (or guild-id "e2e-guild")
                                                  :channelId channel-id}) true]
   ["leaveVoice" :leave-voice (fn [_] #js {:left true}) true]
   ["voiceStatus" :voice-status (fn [_] #js {:connections #js []}) false]
   ;; What discord.voice.status actually reads. nil models "not in a channel",
   ;; the honest default for a manager that has joined nothing.
   ["getVoiceConnection" :get-voice-connection (fn [_] connection) false]
   ["listVoiceMembers" :list-voice-members (fn [_] #js []) true]])

(defn- manager-object
  "The JS surface the tools call, with every invocation recorded."
  [calls* started* opts]
  (reduce (fn [obj [js-name call-key answer async?]]
            (aset obj js-name
                  (fn [& args]
                    (record! calls* call-key args)
                    (let [value (answer (first args))]
                      (if async? (js/Promise.resolve value) value))))
            obj)
          (js-obj)
          (method-answers started* opts)))

(defn manager
  "A gateway manager recording every method a tool calls on it.

   `identity-token` is what .status reports as the logged-in bot, so a test can
   ask which credential the manager was standing up as."
  [{:keys [guild-id connected?] :as opts}]
  (let [calls*   (atom [])
        started* (atom false)]
    {:calls (fn [] @calls*)
     :started? (fn [] @started*)
     :object (manager-object calls* started*
                             (assoc opts :connection
                                    (when connected?
                                      #js {:guildId (or guild-id "e2e-guild")})))}))

(defn ^:async with-gateway!
  "Run `f` with a recording manager installed as the default gateway.

   The previous manager is restored afterwards. It is normally nil under test,
   but restoring rather than clearing keeps this usable from a suite that has
   stood a real one up."
  [opts f]
  (let [previous (gateway/gateway-manager)
        double   (manager opts)]
    (try
      (gateway/set-manager! (:object double))
      (await (f double))
      (finally
        (gateway/set-manager! previous)))))

(defn ^:async with-actor-gateway!
  "Run `f` with a recording manager owned by exactly `actor-id`."
  [actor-id opts f]
  (let [previous (gateway/gateway-manager actor-id)
        double   (manager opts)]
    (try
      (gateway/set-actor-manager! actor-id (:object double))
      (await (f double))
      (finally
        (gateway/set-actor-manager! actor-id previous)))))
