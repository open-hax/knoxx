(ns knoxx.backend.triggers.control-config
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.runtime.contract-loader :as contract-loader]
            [knoxx.backend.runtime.models :as models]
            [knoxx.backend.runtime.roles :as roles]
            [knoxx.backend.tooling :as tooling]
            [knoxx.backend.tools.registry :as tools]
            [knoxx.backend.util.parse :refer [parse-positive-int]]
            ["node:fs" :as fs]))

(defn- env
  [k default]
  (or (aget js/process.env k) default))

(defn parse-string-list
  [raw]
  (->> (str/split (str (or raw "")) #",")
       (map (fn [value] (some-> value str str/trim not-empty)))
       (remove nil?)
       vec))

(declare default-discord-model clamp-max-messages event-agent-trigger-kinds)

(defn- keywordish->string
  [value]
  (cond
    (keyword? value) (name value)
    (string? value) (some-> value str str/trim not-empty)
    (nil? value) nil
    :else (some-> value str str/trim not-empty)))

(defn- keywordish->event-kind
  [value]
  (cond
    (keyword? value) (let [ns (namespace value)
                           nm (name value)]
                       (if (and ns (not (str/blank? ns)))
                         (str ns "." nm)
                         nm))
    (string? value) (some-> value str str/trim not-empty (str/replace #"/" "."))
    :else nil))

(defn- read-edn-with-hash-sync
  [file-path]
  (try
    (let [edn-text (some-> (.readFileSync fs file-path "utf8") str)]
      (when edn-text
        {:edn (reader/read-string edn-text)
         :hash (hash edn-text)}))
    (catch :default _ nil)))

(defn- explicit-tool-ids
  [contract]
  (->> (or (get-in contract [:data :tools]) [])
       (map tools/normalize-tool-id)
       (remove str/blank?)
       distinct
       vec))

(defn- tool-policies-from-contract
  [config role contract]
  (let [explicit (explicit-tool-ids contract)
        role-tool-ids (if (str/blank? (str (or role "")))
                        []
                        (roles/role-tool-ids config role))
        tool-ids (if (seq explicit) explicit role-tool-ids)]
    (mapv (fn [tool-id]
            {:toolId tool-id :effect "allow"})
          tool-ids)))

(defn- filters-from-contract
  [contract]
  (let [filters (or (get-in contract [:data :filters]) {})]
    (cond-> {}
      (seq (or (:channels filters) [])) (assoc :channels (vec (:channels filters)))
      (seq (or (:keywords filters) [])) (assoc :keywords (vec (:keywords filters)))
      (seq (or (:publishChannels filters) (:publish_channels filters) []))
      (assoc :publishChannels (vec (or (:publishChannels filters) (:publish_channels filters))))
      (seq (or (:guildIds filters) (:guild_ids filters) []))
      (assoc :guildIds (vec (or (:guildIds filters) (:guild_ids filters))))
      (seq (or (:repositories filters) [])) (assoc :repositories (vec (:repositories filters))))))

(defn- contract->event-agent-job
  [config contract-id contract contract-hash]
  (let [trigger-kind (keywordish->string (:trigger-kind contract))
        resolved (tooling/resolve-agent-contract config contract-id)]
    (when (and (= :agent (:contract/kind contract))
               (contains? event-agent-trigger-kinds trigger-kind))
        (let [role (:role resolved)
            model (or (:model resolved)
                      (default-discord-model config))
            thinking-level (or (:thinking-level resolved)
                               (some-> (get-in contract [:agent :thinking]) keywordish->string)
                               "off")
            source-kind (or (keywordish->string (:source-kind contract))
                            "manual")
            source-mode (or (keywordish->string (:source-mode contract))
                            "respond")
            cadence (max 1 (or (parse-positive-int (:cadence-min contract)) 5))
            event-kinds (->> (concat (or (get-in contract [:events :always]) [])
                                     (or (get-in contract [:events :maybe]) []))
                             (map keywordish->event-kind)
                             (remove nil?)
                             distinct
                             vec)
            filters (filters-from-contract contract)
            source-config (or (get-in contract [:data :source]) {})
            sticky-session? (or (true? (:stickySession source-config))
                                (true? (:sticky_session source-config)))
            session-max-messages (or (parse-positive-int (:sessionMaxMessages source-config))
                                     (parse-positive-int (:session_max_messages source-config)))]
        {:id contract-id
         :name contract-id
         :enabled (not (false? (:enabled contract)))
         :trigger {:kind trigger-kind
                   :cadenceMinutes cadence
                   :eventKinds event-kinds}
         :source {:kind source-kind
                  :mode source-mode
                  :config (cond-> {:maxMessages (clamp-max-messages (get-in contract [:data :source :max-messages])
                                                                   (get-in contract [:data :source :maxMessages]))}
                           sticky-session? (assoc :stickySession true)
                           session-max-messages (assoc :sessionMaxMessages session-max-messages))}
         :filters filters
         :agentSpec {:role (if (str/blank? (str (or role ""))) (:knoxx-default-role config) role)
                     :model model
                     :thinkingLevel thinking-level
                     :systemPrompt (or (:system-prompt resolved)
                                       (some-> (get-in contract [:prompts :system]) str not-empty)
                                       "")
                     :taskPrompt (or (some-> (get-in contract [:prompts :task]) str not-empty) "")
                     :toolPolicies (vec (or (:tool-policies resolved)
                                            (tool-policies-from-contract config role contract)))}
         :description (or (some-> (get-in contract [:prompts :task]) str str/trim not-empty)
                          (some-> (get-in contract [:prompts :system]) str str/trim not-empty)
                          contract-id)
         :contractSourceId contract-id
         :contractHash contract-hash
         :actorId (:actor-id resolved)}))))

(defn- contract-agent-jobs
  [config]
  (try
    (->> (contract-loader/list-contract-ids-sync config "agents")
         (map (fn [contract-id]
                (let [{:keys [edn hash]} (read-edn-with-hash-sync (contract-loader/contract-file-path config "agents" contract-id))]
                  (when edn
                    (contract->event-agent-job config contract-id edn hash)))))
         (remove nil?)
         (sort-by :id)
         vec)
    (catch :default _ [])))

(defn- default-discord-channels
  []
  (parse-string-list (env "DISCORD_CHANNEL_IDS" "")))

(defn- default-discord-keywords
  []
  (let [keywords (parse-string-list (env "DISCORD_TARGET_KEYWORDS" "knoxx,cephalon"))]
    (if (seq keywords)
      (mapv str/lower-case keywords)
      ["knoxx" "cephalon"])))

(def ^:private event-agent-control-redis-key "event-agent:control-config")
(def ^:private event-agent-trigger-kinds #{"cron" "event"})

(def ^:private max-messages-limit 100)

(defn- clamp-max-messages
  [value fallback]
  (let [n (or (parse-positive-int value)
              (parse-positive-int fallback)
              25)]
    (max 1 (min max-messages-limit n))))

(defn- clamp-source-config
  [source-config default-config]
  (let [cfg (or source-config {})]
    (cond-> (assoc cfg :maxMessages (clamp-max-messages (:maxMessages cfg)
                                                        (:maxMessages (or default-config {}))))
      (contains? cfg :stickySession)
      (assoc :stickySession (boolean (:stickySession cfg)))

      (parse-positive-int (:sessionMaxMessages cfg))
      (assoc :sessionMaxMessages (parse-positive-int (:sessionMaxMessages cfg))))))

(defn persist-event-agent-control!
  "Persist the event-agent-control overrides to Redis so they survive restarts." 
  [control]
  (if-let [client (redis/get-client)]
    (-> (redis/set-json client event-agent-control-redis-key control)
        (.then (fn [_]
                 (println "[control-config] persisted event-agent-control to Redis")
                 control))
        (.catch (fn [err]
                  (println "[control-config] failed to persist event-agent-control to Redis:" (.-message err))
                  control)))
    (js/Promise.resolve control)))

(defn load-event-agent-control
  "Load event-agent-control overrides from Redis. Returns nil if not found." 
  []
  (if-let [client (redis/get-client)]
    (-> (redis/get-json client event-agent-control-redis-key)
        (.then (fn [saved]
                 (when saved
                   (println "[control-config] loaded event-agent-control from Redis"))
                 saved))
        (.catch (fn [err]
                  (println "[control-config] failed to load event-agent-control from Redis:" (.-message err))
                  nil)))
    (js/Promise.resolve nil)))

(defn discord-agent-role-options
  "Roles that can be used for scheduled Discord jobs.

   Source of truth lives in contracts/roles/*.edn." 
  [config]
  (roles/list-role-slugs config))

(defn- default-discord-agent-jobs
  [config]
  (let [default-model (default-discord-model config)
        known-roles (set (roles/list-role-slugs config))
        default-role (if (contains? known-roles "system_admin")
                       "system_admin"
                       (:knoxx-default-role config))]
    [{:id "patrol"
      :name "Channel patrol"
      :kind "observer"
      :description "Poll configured Discord channels, remember fresh messages, and queue signals for follow-up jobs."
      :enabled true
      :cadenceMinutes 5
      :role default-role
      :model default-model
      :thinkingLevel "off"
      :channels (default-discord-channels)
      :keywords (default-discord-keywords)
      :maxMessages 25
      :systemPrompt "Observe configured channels, detect fresh human signals, and queue them without speaking publicly."
      :taskPrompt "Read recent channel messages, update freshness state, and queue human messages that mention the bot or contain target keywords."}
     {:id "mentions"
      :name "Mention response"
      :kind "response"
      :description "Process queued mentions and keyword hits, then decide whether Knoxx should answer."
      :enabled true
      :cadenceMinutes 1
      :role default-role
      :model default-model
      :thinkingLevel "off"
      :channels (default-discord-channels)
      :keywords (default-discord-keywords)
      :maxMessages 12
      :systemPrompt "You are Knoxx's targeted Discord responder. Read the room before replying, stay useful, and prefer silence over filler."
      :taskPrompt "A queued Discord message needs triage. Use discord.read or discord.search if needed, then either stay silent or reply with discord.publish."}
     {:id "deep-synthesis"
      :name "Deep synthesis"
      :kind "synthesis"
      :description "Periodically synthesize cross-channel activity and decide whether a proactive intervention is warranted."
      :enabled true
      :cadenceMinutes 120
      :role default-role
      :model default-model
      :thinkingLevel "minimal"
      :channels (default-discord-channels)
      :keywords (default-discord-keywords)
      :maxMessages 12
      :systemPrompt "You are Knoxx's strategic Discord synthesizer. Look across channels, find meaningful patterns, and only speak when synthesis helps humans."
      :taskPrompt "Summarize recent cross-channel activity, identify important opportunities or risks, and decide whether to publish a concise proactive message."}]))

(defn- normalize-discord-job
  [config default-job raw-job]
  (let [allowed-roles (set (discord-agent-role-options config))
        source (merge default-job (or raw-job {}))
        cadence (or (parse-positive-int (:cadenceMinutes source))
                    (:cadenceMinutes default-job)
                    5)
        role (let [candidate (some-> (:role source) str str/trim not-empty)]
               (if (contains? allowed-roles candidate)
                 candidate
                 (:role default-job)))
        thinking-level (or (models/normalize-thinking-level (:thinkingLevel source))
                           (:thinkingLevel default-job)
                           (:agent-thinking-level config)
                           "off")
        channels (let [candidate (->> (or (:channels source) [])
                                      (map (fn [value] (some-> value str str/trim not-empty)))
                                      (remove nil?)
                                      vec)]
                   (if (seq candidate) candidate (:channels default-job)))
        keywords (let [candidate (->> (or (:keywords source) [])
                                      (map (fn [value] (some-> value str str/trim str/lower-case not-empty)))
                                      (remove nil?)
                                      distinct
                                      vec)]
                   (if (seq candidate) candidate (:keywords default-job)))
        max-messages (or (parse-positive-int (:maxMessages source))
                         (:maxMessages default-job)
                         25)]
    {:id (:id default-job)
     :name (or (some-> (:name source) str str/trim not-empty) (:name default-job))
     :kind (:kind default-job)
     :description (or (some-> (:description source) str str/trim not-empty) (:description default-job))
     :enabled (not (false? (:enabled source)))
     :cadenceMinutes (max 1 (min 10080 cadence))
     :role role
     :model (or (some-> (:model source) str str/trim not-empty)
                (:model default-job)
                (:proxx-default-model config))
     :thinkingLevel thinking-level
     :channels channels
     :keywords keywords
     :maxMessages (max 1 (min 100 max-messages))
     :systemPrompt (or (some-> (:systemPrompt source) str str/trim not-empty)
                       (:systemPrompt default-job)
                       "")
     :taskPrompt (or (some-> (:taskPrompt source) str str/trim not-empty)
                     (:taskPrompt default-job)
                     "")}))

(defn discord-agent-control-config
  [config]
  (let [saved (or (:discord-agent-control config) {})
        defaults (default-discord-agent-jobs config)
        saved-by-id (into {} (map (fn [job] [(:id job) job])) (or (:jobs saved) []))
        merged-jobs (mapv (fn [default-job]
                            (normalize-discord-job config default-job (get saved-by-id (:id default-job))))
                          defaults)]
    {:botUserId (or (some-> (:botUserId saved) str str/trim not-empty)
                    (some-> (env "DISCORD_BOT_USER_ID" "") str str/trim not-empty)
                    "")
     :defaultChannels (let [saved-channels (->> (or (:defaultChannels saved) [])
                                                (map (fn [value] (some-> value str str/trim not-empty)))
                                                (remove nil?)
                                                vec)]
                        (if (seq saved-channels) saved-channels (default-discord-channels)))
     :targetKeywords (let [saved-keywords (->> (or (:targetKeywords saved) [])
                                               (map (fn [value] (some-> value str str/trim str/lower-case not-empty)))
                                               (remove nil?)
                                               distinct
                                               vec)]
                       (if (seq saved-keywords) saved-keywords (default-discord-keywords)))
     :jobs merged-jobs}))

(defn event-agent-role-options
  [config]
  (discord-agent-role-options config))

(defn event-agent-source-kind-options
  []
  ["discord" "github" "cron" "manual"])

(defn event-agent-trigger-kind-options
  []
  ["cron" "event"])

(defn- normalize-tool-policy-entry
  [policy]
  (let [tool-id (some-> (or (:toolId policy)
                            (:tool-id policy)
                            (:tool_id policy))
                        str
                        str/trim
                        not-empty)
        effect (some-> (or (:effect policy) "allow")
                       str
                       str/trim
                       str/lower-case
                       not-empty)]
    (when tool-id
      {:toolId tool-id
       :effect (if (#{"allow" "deny"} effect) effect "allow")})))

(defn- normalize-tool-policy-list
  [policies]
  (->> (or policies [])
       (keep normalize-tool-policy-entry)
       vec))

(defn- default-discord-tool-policies
  []
  [{:toolId "discord.read" :effect "allow"}
   {:toolId "discord.channel.messages" :effect "allow"}
   {:toolId "discord.channel.scroll" :effect "allow"}
   {:toolId "discord.dm.messages" :effect "allow"}
   {:toolId "discord.search" :effect "allow"}
   {:toolId "discord.publish" :effect "allow"}
   {:toolId "discord.send" :effect "allow"}
   {:toolId "discord.guilds" :effect "allow"}
   {:toolId "discord.channels" :effect "allow"}
   {:toolId "discord.list.servers" :effect "allow"}
   {:toolId "discord.list.channels" :effect "allow"}
   {:toolId "websearch" :effect "allow"}
   {:toolId "web.read" :effect "allow"}
   {:toolId "memory_search" :effect "allow"}
   {:toolId "graph_query" :effect "allow"}])

(defn- derive-default-discord-channels
  [jobs]
  (->> (or jobs [])
       (mapcat (fn [job]
                 (concat (or (get-in job [:filters :channels]) [])
                         (or (get-in job [:filters :publishChannels]) [])
                         (or (get-in job [:filters :publish_channels]) []))))
       (map (fn [value] (some-> value str str/trim not-empty)))
       (remove nil?)
       distinct
       vec))

(defn- default-discord-model
  [_config]
  "gemma4:31b")

(defn default-event-agent-control
  [config]
  (let [jobs (contract-agent-jobs config)
        inferred-default-channels (derive-default-discord-channels jobs)
        configured-default-channels (default-discord-channels)
        default-discord-source {:botUserId (or (some-> (env "DISCORD_BOT_USER_ID" "") str str/trim not-empty) "")
                                :defaultChannels (if (seq configured-default-channels)
                                                   configured-default-channels
                                                   inferred-default-channels)
                                :targetKeywords (default-discord-keywords)}]
    {:sources {:discord default-discord-source
               :github {:webhookSecretConfigured (boolean (some-> (env "GITHUB_WEBHOOK_SECRET" "") str str/trim not-empty))}
               :cron {}}
     :jobs jobs}))

(defn- normalize-event-agent-job
  [config default-job raw-job]
  (let [allowed-roles (set (event-agent-role-options config))
        contract-sourced? (some-> (:contractSourceId default-job) str str/trim not-empty)
        saved-contract-hash (:contractHash raw-job)
        current-contract-hash (:contractHash default-job)
        saved-job-current? (or (not contract-sourced?)
                               (and (= (:contractSourceId raw-job) (:contractSourceId default-job))
                                    current-contract-hash
                                    saved-contract-hash
                                    (= saved-contract-hash current-contract-hash)))
        source (merge default-job (if saved-job-current? (or raw-job {}) {}))
        trigger-source (merge (:trigger default-job) (or (:trigger source) {}))
        source-config (merge (:source default-job) (or (:source source) {}))
        agent-source (merge (:agentSpec default-job) (or (:agentSpec source) {}))
        trigger-kind (let [candidate (some-> (:kind trigger-source) str str/trim str/lower-case not-empty)]
                       (if (#{"cron" "event"} candidate) candidate (:kind (:trigger default-job))))
        cadence (or (parse-positive-int (:cadenceMinutes trigger-source))
                    (:cadenceMinutes (:trigger default-job))
                    5)
        event-kinds (->> (or (:eventKinds trigger-source) [])
                         (map (fn [value] (some-> value str str/trim not-empty)))
                         (remove nil?)
                         distinct
                         vec)
        source-kind (let [candidate (some-> (:kind source-config) str str/trim str/lower-case not-empty)]
                      (if (some #(= candidate %) (event-agent-source-kind-options))
                        candidate
                        (:kind (:source default-job))))
        source-mode (or (some-> (:mode source-config) str str/trim not-empty)
                        (:mode (:source default-job))
                        "observe")
        role (let [candidate (some-> (:role agent-source) str str/trim not-empty)]
               (if (contains? allowed-roles candidate)
                 candidate
                 (:role (:agentSpec default-job))))
        thinking-level (or (models/normalize-thinking-level (:thinkingLevel agent-source))
                           (:thinkingLevel (:agentSpec default-job))
                           (:agent-thinking-level config)
                           "off")]
    {:id (:id default-job)
     :name (or (some-> (:name source) str str/trim not-empty) (:name default-job))
     :enabled (not (false? (:enabled source)))
     :trigger {:kind trigger-kind
               :cadenceMinutes (max 1 (min 10080 cadence))
               :eventKinds event-kinds}
     :source {:kind source-kind
              :mode source-mode
              :config (clamp-source-config (:config source-config)
                                          (get-in default-job [:source :config]))}
     :filters (or (:filters source) (:filters default-job) {})
     :agentSpec {:role role
                 :model (or (some-> (:model agent-source) str str/trim not-empty)
                            (:model (:agentSpec default-job))
                            (default-discord-model config))
                 :thinkingLevel thinking-level
                 :systemPrompt (or (some-> (:systemPrompt agent-source) str not-empty)
                                   (:systemPrompt (:agentSpec default-job))
                                   "")
                 :taskPrompt (or (some-> (:taskPrompt agent-source) str not-empty)
                                 (:taskPrompt (:agentSpec default-job))
                                 "")
                 :toolPolicies (let [normalized (normalize-tool-policy-list (:toolPolicies agent-source))]
                                 (if (and (seq normalized) (every? some? normalized))
                                   normalized
                                   (normalize-tool-policy-list (:toolPolicies (:agentSpec default-job))))) }
     :contractSourceId (or (:contractSourceId source)
                           (:contractSourceId default-job))
     :contractHash (or (:contractHash default-job)
                       (:contractHash source))
:actorId (or (:actorId source)
                   (:actorId default-job)
                   ;; Always prefer contract-sourced actorId if available
                   (when contract-sourced?
                     (:actor_id (:agentSpec default-job))))
     :description (or (some-> (:description source) str str/trim not-empty)
                      (:description default-job))}))

(defn event-agent-control-config
  [config]
  (let [saved (or (:event-agent-control config) {})
        defaults (default-event-agent-control config)
        default-sources (:sources defaults)
        saved-sources (or (:sources saved) {})
        saved-discord-source (or (:discord saved-sources) {})
        saved-discord-channels (->> (or (:defaultChannels saved-discord-source) [])
                                    (map (fn [value] (some-> value str str/trim not-empty)))
                                    (remove nil?)
                                    vec)
        saved-discord-keywords (->> (or (:targetKeywords saved-discord-source) [])
                                    (map (fn [value] (some-> value str str/trim str/lower-case not-empty)))
                                    (remove nil?)
                                    distinct
                                    vec)
        github-webhook-secret-configured (get-in default-sources [:github :webhookSecretConfigured])
        default-jobs (:jobs defaults)
        saved-jobs (vec (or (:jobs saved) []))
        default-job-ids (into #{} (map :id) default-jobs)
        saved-jobs-by-id (into {} (map (fn [job] [(:id job) job])) saved-jobs)
        custom-jobs (->> saved-jobs
                         (keep (fn [job]
                                 (let [job-id (some-> (:id job) str str/trim not-empty)]
                                   (when (and job-id (not (contains? default-job-ids job-id)))
                                     (normalize-event-agent-job config
                                                                {:id job-id
                                                                 :name (or job-id "custom-job")
                                                                 :enabled true
                                                                 :trigger {:kind "event" :cadenceMinutes 5 :eventKinds []}
                                                                 :source {:kind "manual" :mode "respond" :config {}}
                                                                 :filters {}
                                                                 :agentSpec {:role (:knoxx-default-role config)
                                                                             :model (default-discord-model config)
                                                                             :thinkingLevel "off"
                                                                             :systemPrompt "You are Knoxx's scheduled event agent. Respond to dispatched events, use Discord tools when needed, and emit useful actions without filler."
                                                                             :taskPrompt "A structured event matched this job. Read context, decide what action is useful, and use available tools deliberately."
                                                                             :toolPolicies (default-discord-tool-policies)}
                                                                 :description "Custom scheduled event-agent job"}
                                                                job)))))
                         vec)]
    {:sources (-> {:discord (cond-> (merge (:discord default-sources) saved-discord-source)
                              (empty? saved-discord-channels)
                              (assoc :defaultChannels (get-in default-sources [:discord :defaultChannels]))

                              (seq saved-discord-channels)
                              (assoc :defaultChannels saved-discord-channels)

                              (empty? saved-discord-keywords)
                              (assoc :targetKeywords (get-in default-sources [:discord :targetKeywords]))

                              (seq saved-discord-keywords)
                              (assoc :targetKeywords saved-discord-keywords))
                   :github (merge (:github default-sources) (or (:github saved-sources) {}))
                   :cron (merge (:cron default-sources) (or (:cron saved-sources) {}))}
                 ;; Never let persisted Redis overrides lie about secret configuration.
                 (assoc-in [:github :webhookSecretConfigured]
                           (boolean github-webhook-secret-configured)))
     :jobs (vec (concat
                 (mapv (fn [default-job]
                         (normalize-event-agent-job config default-job (get saved-jobs-by-id (:id default-job))))
                       default-jobs)
                 custom-jobs))}))
