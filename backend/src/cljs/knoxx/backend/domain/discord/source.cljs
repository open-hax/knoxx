(ns knoxx.backend.domain.discord.source
  "Discord source adapter for event-agent runtime.

   This namespace owns Discord gateway lifecycle and Discord REST reads. It emits
   normalized message/voice callbacks to the generic event runtime instead of
   letting the runtime depend directly on discord-gateway internals."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.discord.gateway :as dg]
            [knoxx.backend.domain.discord.rest-client :as discord-rest]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.domain.label.quality :as quality-labels]
            [knoxx.backend.infra.db.policy :as policy-db]))

(defonce ^:private gateway-unsubscribe* (atom nil))

(defn bot-token
  [config]
  (:discord-bot-token config))

(defn- manager-active?
  [manager]
  (when manager
    (let [status (.status manager)]
      (boolean (or (aget status "ready")
                   (aget status "started"))))))

(defn active-gateway-entries
  []
  (let [actor-entries (->> (dg/gateway-managers)
                           (filter (fn [[_ manager]] (manager-active? manager)))
                           vec)]
    (if (seq actor-entries)
      actor-entries
      (when-let [manager (dg/gateway-manager)]
        (when (manager-active? manager)
          [[nil manager]])))))

(defn active?
  []
  (boolean (seq (active-gateway-entries))))

(defn gateway-user-id
  []
  (or (some (fn [[_ manager]]
              (let [status (.status manager)]
                (some-> (aget status "userId") str str/trim not-empty)))
            (active-gateway-entries))
      (when-let [manager (dg/gateway-manager)]
        (let [status (.status manager)]
          (some-> (aget status "userId") str str/trim not-empty)))))

(defn- message-role-ids
  [msg]
  (->> (or (get-in msg [:member :roles]) [])
       (map str)
       vec))

(defn map-message
  [msg]
  (let [author (or (:author msg) {})]
    {:id (:id msg)
     :channelId (or (:channel_id msg) (:channelId msg) "")
     :guildId (or (:guild_id msg) (:guildId msg) "")
     :content (or (:content msg) "")
     :authorId (or (:id author) "")
     :authorUsername (or (:username author) "unknown")
     :authorIsBot (boolean (:bot author))
     :authorRoleIds (message-role-ids msg)
     :timestamp (or (:timestamp msg) "")
     :attachments (->> (or (:attachments msg) [])
                       (mapv (fn [attachment]
                               {:id (or (:id attachment) "")
                                :filename (or (:filename attachment) "")
                                :contentType (or (:content_type attachment) (:contentType attachment))
                                :size (or (:size attachment) 0)
                                :url (or (:url attachment) "")})))
     :embeds (->> (or (:embeds msg) [])
                  (mapv (fn [embed]
                          {:title (:title embed)
                           :description (:description embed)
                           :url (:url embed)})))}))

(defn- sort-newest-first
  [messages]
  (sort-by :timestamp #(compare %2 %1) messages))

(defn- record-id
  [message]
  (str "discord:message:" (:channelId message) ":" (:id message)))

(defn- label-for-record-id
  [labels rid]
  (or (get labels rid)
      (get labels (keyword rid))
      {}))

(defn- ^:async attach-openplanner-labels!
  [config messages]
  (let [client (openplanner-client/client config)]
    (if (or (empty? messages) (not (openplanner-client/enabled? client)))
      (quality-labels/good-first-then-not-bad messages)
      (try
        (let [ids (mapv record-id messages)
              labels (:labels (await (openplanner-client/record-labels! client ids)))]
          (->> messages
               (mapv (fn [message]
                       (assoc message :openplannerLabels (label-for-record-id labels (record-id message)))))
               quality-labels/good-first-then-not-bad))
        (catch :default error
          (.error js/console "[event-agents.discord] OpenPlanner label lookup failed; failing closed to avoid surfacing crossed/bad Discord context" error)
          [])))))

(defn- fetch-channel-from-gateway-entries!
  [entries channel-id opts]
  (if-let [[_actor-id manager] (first entries)]
    (-> (.fetchChannelMessages manager channel-id opts)
        (.catch (fn [_]
                  (fetch-channel-from-gateway-entries! (rest entries) channel-id opts))))
    (js/Promise.reject (js/Error. (str "No active Discord actor gateway can read channel " channel-id)))))

(defn read-channel!
  [config channel-id limit]
  (if-let [entries (seq (active-gateway-entries))]
    (-> (fetch-channel-from-gateway-entries! entries channel-id (clj->js {:limit (max 1 (min 100 (or limit 25)))}))
        (.then (fn [messages]
                 (->> (js->clj messages :keywordize-keys true)
                      sort-newest-first
                      vec)))
        (.then (fn [messages]
                 (attach-openplanner-labels! config messages))))
    (let [token (bot-token config)]
      (if (str/blank? token)
        (js/Promise.reject (js/Error. "Discord bot token not configured"))
        (-> (discord-rest/channel-messages! (discord-rest/client token) channel-id {:limit (max 1 (min 100 (or limit 25)))})
            (.then (fn [payload]
                     (->> (or payload [])
                          (map map-message)
                          sort-newest-first
                          vec)))
            (.then (fn [messages]
                     (attach-openplanner-labels! config messages))))))))

(defn list-channels!
  []
  (let [entries (active-gateway-entries)]
    (if (seq entries)
      (-> (js/Promise.all
           (clj->js
            (mapv (fn [[_ manager]]
                    (-> (.listChannels manager nil)
                        (.catch (fn [_] #js []))))
                  entries)))
          (.then (fn [results]
                   (->> (js/Array.from results)
                        (mapcat #(js/Array.from %))
                        (map #(js->clj % :keywordize-keys true))
                        (group-by :id)
                        vals
                        (map first)
                        clj->js))))
      (or (dg/list-channels)
          (js/Promise.resolve #js [])))))

(defn resolve-channel-ids!
  [{:keys [explicit-channels guild-ids]}]
  (cond
    (seq guild-ids)
    (-> (list-channels!)
        (.then (fn [channels]
                 (let [rows (js->clj channels :keywordize-keys true)
                       guild-id-set (set guild-ids)]
                   (->> rows
                        (filter (fn [channel]
                                  (contains? guild-id-set (:guildId channel))))
                        (map :id)
                        distinct
                        vec)))))

    :else
    ;; Publish channels are output sinks, not read sources. Never infer scan
    ;; channels from publish targets.
    (js/Promise.resolve explicit-channels)))

(defn message-match-kind
  [{:keys [bot-user-id content keyword? created? match-all?]}]
  (let [text (str/lower-case (str content ""))
        mention? (and bot-user-id
                      (or (str/includes? text (str "<@" bot-user-id ">"))
                          (str/includes? text (str "<@!" bot-user-id ">"))))]
    (cond
      mention? "discord.message.mention"
      keyword? "discord.message.keyword"
      (and created? match-all?) "discord.message.created"
      :else nil)))

(defn execute-patrol!
  [{:keys [config channel-ids limit unseen-messages remember-latest! match-kind dispatch-message!]}]
  ;; Skip patrol dispatch entirely when the gateway is live — the gateway fires
  ;; message events in real time, so patrol would double-fire.
  (if (active?)
    (js/Promise.resolve nil)
    (if (seq channel-ids)
      (js/Promise.all
       (clj->js
        (mapv (fn [channel-id]
                (-> (read-channel! config channel-id limit)
                    (.then (fn [messages]
                             (let [fresh (unseen-messages channel-id messages)]
                               (doseq [message fresh]
                                 (when-let [kind (match-kind message)]
                                   (dispatch-message! message kind)))
                               (remember-latest! channel-id messages)
                               {:channelId channel-id
                                :fetched (count messages)
                                :fresh (count fresh)})))
                    (.catch (fn [err]
                              (println "[event-agents.discord] patrol failed for" channel-id ":" (.-message err))
                              {:channelId channel-id
                               :error true}))))
              channel-ids)))
      (js/Promise.resolve nil))))

(defn summarize-channel
  [channel-id messages]
  (->> messages
       (remove :authorIsBot)
       (take 8)
       (map (fn [message]
              (let [attachments (:attachments message)
                    attachment-text (when (seq attachments)
                                      (str " attachments="
                                           (str/join ", " (map (fn [a] (str (:filename a) (when (:url a) (str " <" (:url a) ">"))))
                                                                attachments))))]
                (str "[" channel-id "] <" (:authorUsername message) " (id:" (:authorId message) ")> "
                     (subs (:content message) 0 (min 180 (count (:content message))))
                     (or attachment-text "")))))
       (str/join "\n")))

(defn image-attachments
  [rows]
  (->> rows
       (mapcat (fn [{:keys [messages]}]
                 (mapcat :attachments messages)))
       (filter :url)
       (filter (fn [attachment]
                 (some-> (:contentType attachment)
                         str
                         str/lower-case
                         (str/starts-with? "image/"))))
       (take 8)
       vec))

(defn execute-synthesis!
  [{:keys [config channel-ids limit dispatch-summary!]}]
  (if-not (seq channel-ids)
    (js/Promise.resolve nil)
    (let [fetch-row! (fn [channel-id]
                       (-> (read-channel! config channel-id limit)
                           (.then (fn [messages]
                                    {:channelId channel-id
                                     :messages messages}))
                           (.catch (fn [_]
                                     {:channelId channel-id
                                      :messages []}))))]
      (-> (js/Promise.all (clj->js (mapv fetch-row! channel-ids)))
          (.then (fn [results]
                   (dispatch-summary! (js->clj results :keywordize-keys true))))))))

(defn bind-gateways!
  [{:keys [policy-db on-message! on-voice-state!]}]
  (when-let [unsubscribe @gateway-unsubscribe*]
    (unsubscribe)
    (reset! gateway-unsubscribe* nil))
  (if policy-db
    (-> (policy-db/list-actor-credentials! policy-db "discord_bot")
        (.then (fn [result]
                 (dg/start-actor-gateways! (or (:credentials result) []))))
        (.then
         (fn [_started]
           (let [unsubscribes
                 (->> (dg/gateway-managers)
                      (mapcat
                       (fn [[actor-id manager]]
                         (let [status (.status manager)
                               bot-user-id (some-> (aget status "userId") str str/trim not-empty)
                               msg-unsub (.onMessage manager
                                                     (fn [mapped _raw]
                                                       (on-message!
                                                        (assoc (js->clj mapped :keywordize-keys true)
                                                               :gatewayActorId actor-id
                                                               :gatewayBotUserId bot-user-id))))
                               voice-unsub (.onVoiceStateUpdate manager
                                                                 (fn [mapped _old _new]
                                                                   (on-voice-state!
                                                                    (assoc (js->clj mapped :keywordize-keys true)
                                                                           :gatewayActorId actor-id
                                                                           :gatewayBotUserId bot-user-id))))]
                           [msg-unsub voice-unsub])))
                      vec)]
             (println "[event-agents] bound" (/ (count unsubscribes) 2) "Discord actor gateway(s)")
             (reset! gateway-unsubscribe*
                     (fn []
                       (doseq [unsubscribe unsubscribes]
                         (try (unsubscribe) (catch js/Error _))))))))
        (.catch (fn [err]
                  (println "[event-agents] discord actor gateway bind failed:" (.-message err))
                  nil)))
    (println "[event-agents] policy DB unavailable; Discord actor gateways not bound")))

(defn stop!
  []
  (when-let [unsubscribe @gateway-unsubscribe*]
    (unsubscribe)
    (reset! gateway-unsubscribe* nil)))
