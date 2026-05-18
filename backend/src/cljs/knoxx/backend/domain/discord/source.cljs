(ns knoxx.backend.events.sources.discord
  "Discord source adapter for event-agent runtime.

   This namespace owns Discord gateway lifecycle and Discord REST reads. It emits
   normalized message/voice callbacks to the generic event runtime instead of
   letting the runtime depend directly on discord-gateway internals."
  (:require [clojure.string :as str]
            [knoxx.backend.discord-gateway :as dg]
            [knoxx.backend.http :as backend-http]
            [knoxx.backend.quality-labels :as quality-labels]))

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

(defn- discord-headers
  [token]
  #js {"Authorization" (str "Bot " token)
       "Content-Type" "application/json"})

(defn- fetch-json!
  [url options]
  (-> (js/fetch url options)
      (.then (fn [resp]
               (if (.-ok resp)
                 (.json resp)
                 (-> (.text resp)
                     (.then (fn [text]
                              (throw (js/Error. (str "HTTP " (.-status resp) ": " text)))))))))))

(defn- message-role-ids
  [msg]
  (->> (if (array? (aget msg "member" "roles"))
         (array-seq (aget msg "member" "roles"))
         [])
       (map str)
       vec))

(defn map-message
  [msg]
  {:id (aget msg "id")
   :channelId (or (aget msg "channel_id") "")
   :guildId (or (aget msg "guild_id") "")
   :content (or (aget msg "content") "")
   :authorId (or (aget msg "author" "id") "")
   :authorUsername (or (aget msg "author" "username") "unknown")
   :authorIsBot (boolean (aget msg "author" "bot"))
   :authorRoleIds (message-role-ids msg)
   :timestamp (or (aget msg "timestamp") "")
   :attachments (->> (if (array? (aget msg "attachments")) (array-seq (aget msg "attachments")) [])
                     (mapv (fn [attachment]
                             {:id (or (aget attachment "id") "")
                              :filename (or (aget attachment "filename") "")
                              :contentType (or (aget attachment "content_type") (aget attachment "contentType"))
                              :size (or (aget attachment "size") 0)
                              :url (or (aget attachment "url") "")})))
   :embeds (->> (if (array? (aget msg "embeds")) (array-seq (aget msg "embeds")) [])
                (mapv (fn [embed]
                        {:title (or (aget embed "title") nil)
                         :description (or (aget embed "description") nil)
                         :url (or (aget embed "url") nil)})))})

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

(defn- attach-openplanner-labels!
  [config messages]
  (if (or (empty? messages) (not (backend-http/openplanner-enabled? config)))
    (js/Promise.resolve (quality-labels/good-first-then-not-bad messages))
    (let [ids (mapv record-id messages)]
      (-> (backend-http/openplanner-request! config "POST" "/v1/labels/records/lookup" {:ids ids})
          (.then (fn [response]
                   (let [labels (:labels response)]
                     (->> messages
                          (mapv (fn [message]
                                  (assoc message :openplannerLabels (label-for-record-id labels (record-id message)))))
                          quality-labels/good-first-then-not-bad))))
          (.catch (fn [error]
                    (.warn js/console "[event-agents.discord] OpenPlanner label lookup failed; failing closed to avoid surfacing crossed/bad Discord context" error)
                    []))))))

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
        (-> (fetch-json!
             (str "https://discord.com/api/v10/channels/" channel-id "/messages?limit=" (max 1 (min 100 (or limit 25))))
             #js {:method "GET"
                  :headers (discord-headers token)})
            (.then (fn [payload]
                     (->> (if (array? payload) (array-seq payload) [])
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
    (-> (.listActorCredentials policy-db "discord_bot")
        (.then (fn [result]
                 (dg/start-actor-gateways! (or (aget result "credentials") #js []))))
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
