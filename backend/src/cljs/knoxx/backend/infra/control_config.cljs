(ns knoxx.backend.infra.control-config
  (:require [clojure.string :as str]
            [knoxx.backend.domain.agent.agent-templates :as prompt-templates]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.models :as models]
            [knoxx.backend.runtime.roles :as roles]
            [knoxx.backend.infra.tooling :as tooling]
            [knoxx.backend.infra.registry.tools :as tools]
            [knoxx.backend.law.control :as control-law]
            [knoxx.backend.shape.parse :refer [parse-positive-int]]))

(defn- env
  [k default]
  (or (aget js/process.env k) default))

(defn parse-string-list
  [raw]
  (->> (str/split (str (or raw "")) #",")
       (map (fn [value] (some-> value str str/trim not-empty)))
       (remove nil?)
       vec))

(declare default-discord-model)

(defn- keywordish->string
  [value]
  (cond
    (keyword? value) (name value)
    (string? value) (some-> value str str/trim not-empty)
    (nil? value) nil
    :else (some-> value str str/trim not-empty)))

(defn- nonblank-str
  [value]
  (some-> value str str/trim not-empty))

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

(defn- source-mode-record
  [config value]
  (let [wanted (keywordish->string value)]
    (when wanted
      (some (fn [record]
              (when (and (= "source_modes" (:contractClass record))
                         (= wanted (:id record)))
                record))
            (contract-loader/load-all-contracts-sync config)))))

(defn- source-mode-contract
  [config value]
  (some-> (source-mode-record config value) :contract))

(defn- source-mode-runtime-kind
  [mode-contract fallback]
  (or (keywordish->string (:source/kind mode-contract))
      (keywordish->string fallback)
      "manual"))

(defn- source-mode-runtime-mode
  [mode-contract fallback]
  (or (keywordish->string (:source/mode mode-contract))
      (keywordish->string fallback)
      "respond"))

(defn- combine-prompt-values
  [& parts]
  (let [segments (->> parts
                      (keep prompt-templates/prompt-value)
                      vec)]
    (cond
      (empty? segments) nil
      (= 1 (count segments)) (first segments)
      :else (list (symbol "template") {:separator "\n\n"} segments))))

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
        ;; :data/tools is an additive escape hatch for contract-local tools.
        ;; It must not replace role/capability-derived tools; contract agents
        ;; are composed from reusable roles/capabilities plus local additions.
        tool-ids (->> (concat role-tool-ids explicit)
                      distinct
                      vec)]
    (mapv (fn [tool-id]
            {:toolId tool-id :effect "allow"})
          tool-ids)))

(defn- filters-from-contract
  [contract]
  (let [filters (or (get-in contract [:data :filters]) {})]
    (cond-> {}
      (seq (or (:channels filters) [])) (assoc :channels (vec (:channels filters)))
      (seq (or (:keywords filters) [])) (assoc :keywords (vec (:keywords filters)))
      (or (true? (:matchAll filters)) (true? (:match_all filters)))
      (assoc :matchAll true)
      (seq (or (:publishChannels filters) (:publish_channels filters) []))
      (assoc :publishChannels (vec (or (:publishChannels filters) (:publish_channels filters))))
      (seq (or (:guildIds filters) (:guild_ids filters) []))
      (assoc :guildIds (vec (or (:guildIds filters) (:guild_ids filters))))
      (seq (or (:authorIds filters) (:author_ids filters) (:authors filters) []))
      (assoc :authorIds (vec (or (:authorIds filters) (:author_ids filters) (:authors filters))))
      (seq (or (:repositories filters) [])) (assoc :repositories (vec (:repositories filters))))))

(defn- memory-hydration-from-contract
  [contract]
  (or (:memory-hydration contract)
      (:memoryHydration contract)
      (get-in contract [:memory :passive-hydration])
      (get-in contract [:memory :passiveHydration])))

(defn- contract-event-kinds
  [contract]
  (let [normalize (fn [values]
                    (->> (or values [])
                         (map keywordish->event-kind)
                         (remove nil?)
                         distinct
                         vec))
        always-kinds (normalize (get-in contract [:events :always]))
        maybe-kinds (normalize (get-in contract [:events :maybe]))]
    {:always-kinds always-kinds
     :maybe-kinds maybe-kinds
     :event-kinds (vec (distinct (concat always-kinds maybe-kinds)))}))

(defn- contract-source-mode-context
  [config contract]
  (let [source-mode-ref (:source-mode contract)
        mode-contract (source-mode-contract config source-mode-ref)]
    {:source-mode-ref source-mode-ref
     :source-mode-contract mode-contract
     :source-kind (source-mode-runtime-kind mode-contract (:source-kind contract))
     :source-mode (source-mode-runtime-mode mode-contract source-mode-ref)
     :source-mode-task-prompt (prompt-templates/prompt-value (get-in mode-contract [:prompts :task]))}))

(defn- vectorish-string-list
  [value]
  (cond
    (sequential? value) (->> value (map str) (map str/trim) (remove str/blank?) vec)
    (string? value) (parse-string-list value)
    :else []))

;; TODO belongs in shapes
(defn- contract-source-spec
  [contract source-kind source-mode source-mode-contract]
  (let [source-config (or (get-in contract [:data :source]) {})
        trusted-role-ids (or (seq (vectorish-string-list (:trustedRoleIds source-config)))
                             (seq (vectorish-string-list (:trusted-role-ids source-config)))
                             (seq (vectorish-string-list (:trusted_role_ids source-config)))
                             (seq (vectorish-string-list (get-in source-mode-contract [:data :trusted-role-ids]))))
        sticky-session? (or (true? (:stickySession source-config))
                            (true? (:sticky_session source-config)))
        session-max-messages (or (parse-positive-int (:sessionMaxMessages source-config))
                                 (parse-positive-int (:session_max_messages source-config)))
        max-total-messages (or (parse-positive-int (:maxTotalMessages source-config))
                               (parse-positive-int (:max-total-messages source-config))
                               (parse-positive-int (:max_messages_total source-config)))
        max-message-chars (or (parse-positive-int (:maxMessageChars source-config))
                              (parse-positive-int (:max-message-chars source-config))
                              (parse-positive-int (:max_message_chars source-config)))
        streaming-behavior (some-> (or (:streamingBehavior source-config)
                                       (:streaming_behavior source-config))
                                   str str/trim not-empty)]
    {:kind source-kind
     :mode source-mode
     :config (cond-> {:maxMessages (control-law/clamp-max-messages (get-in contract [:data :source :max-messages])
                                                                   (get-in contract [:data :source :maxMessages]))}
               max-total-messages (assoc :maxTotalMessages (control-law/clamp-max-messages max-total-messages max-total-messages))
               max-message-chars (assoc :maxMessageChars (control-law/clamp-max-message-chars max-message-chars max-message-chars))
               trusted-role-ids (assoc :trustedRoleIds (vec trusted-role-ids))
               sticky-session? (assoc :stickySession true)
               session-max-messages (assoc :sessionMaxMessages session-max-messages)
               streaming-behavior (assoc :streamingBehavior streaming-behavior))}))

;; TODO belongs in shapes
(defn- contract-agent-spec
  [config contract-id contract resolved source-mode-ref source-mode-task-prompt]
  (let [role (:role resolved)]
    {:role (if (str/blank? (str (or role ""))) (:knoxx-default-role config) role)
     :model (or (:model resolved) (default-discord-model config))
     :thinkingLevel (or (:thinking-level resolved)
                        (some-> (get-in contract [:agent :thinking]) keywordish->string)
                        "off")
     :systemPrompt (or (:system-prompt resolved)
                       (prompt-templates/prompt-value (get-in contract [:prompts :system]))
                       "")
     :taskPrompt (or (combine-prompt-values
                      source-mode-task-prompt
                      (or (:task-prompt resolved)
                          (prompt-templates/prompt-value (get-in contract [:prompts :task]))))
                     "")
     :toolPolicies (vec (or (:tool-policies resolved)
                            (tool-policies-from-contract config role contract)))
     :sources (vec (or (:sources resolved) []))
     :memoryHydration (memory-hydration-from-contract contract)
     :contextPolicy (:context-policy resolved)
     :contractId contract-id
     :sourceMode (keywordish->string source-mode-ref)
     :actorId (:actor-id resolved)
     :contractActors (vec (or (:contract-actor-ids resolved) []))}))
;; TODO Legacy  smell event agent not good.
(defn- contract->event-agent-job
  [config contract-id contract contract-hash]
  (let [trigger-kind (keywordish->string (:trigger-kind contract))
        resolved (tooling/resolve-agent-contract config contract-id)]
    (when (and (= :agent (:contract/kind contract))
               (contains? control-law/trigger-kinds trigger-kind))
      (let [{:keys [always-kinds maybe-kinds event-kinds]} (contract-event-kinds contract)
            {:keys [source-mode-ref source-mode-contract source-kind source-mode source-mode-task-prompt]}
            (contract-source-mode-context config contract)]
        {:id contract-id
         :name contract-id
         :enabled (not (false? (:enabled contract)))
         :trigger {:kind trigger-kind
                   :cadenceMinutes (max 1 (or (parse-positive-int (:cadence-min contract)) 5))
                   :eventKinds event-kinds
                   :alwaysKinds always-kinds
                   :maybeKinds maybe-kinds
                   :eventWeights (or (get-in contract [:events :weights]) {})
                   :eventThreshold (or (get-in contract [:events :threshold]) 1)}
         :source (contract-source-spec contract source-kind source-mode source-mode-contract)
         :filters (filters-from-contract contract)
         :agentSpec (contract-agent-spec config contract-id contract resolved source-mode-ref source-mode-task-prompt)
         :description (or (prompt-templates/prompt-preview (get-in contract [:prompts :task]))
                          (prompt-templates/prompt-preview (get-in contract [:prompts :system]))
                          contract-id)
         :contractSourceId contract-id
         :contractSourceKind "agent"
         :contractSourceKey (str "agent:" contract-id)
         :contractHash contract-hash
         :actorId (:actor-id resolved)}))))

(defn- contract-agent-jobs
  [config]
  (try
    (->> (contract-loader/load-all-contracts-sync config)
         (filter #(= "agents" (:contractClass %)))
         (map (fn [record]
                (contract->event-agent-job config
                                           (:id record)
                                           (:contract record)
                                           (hash (:edn-text record)))))
         (remove nil?)
         (sort-by :id)
         vec)
    (catch :default _ [])))

(defn- default-discord-channels
  []
  (parse-string-list (env "DISCORD_CHANNEL_IDS" "")))
;; TODO Extract  discord related logic from control config
(defn- default-discord-keywords
  []
  (let [keywords (parse-string-list (env "DISCORD_TARGET_KEYWORDS" "knoxx,cephalon"))]
    (if (seq keywords)
      (mapv str/lower-case keywords)
      ["knoxx" "cephalon"])))

(def ^:private event-agent-control-redis-key "event-agent:control-config")

;; TODO Legacy concept event-agent  is  smell
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

;; TODO Legacy concept event-agent  is  smell
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
        configured-role (roles/normalize-role config (:knoxx-default-role config))
        default-role (or (when (contains? known-roles configured-role)
                           configured-role)
                         (first (sort known-roles)))]
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
  control-law/source-kinds)

(defn event-agent-trigger-kind-options
  []
  control-law/trigger-kind-options)

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
  ;; Publishing targets are not read sources. Keep this fallback narrow for
  ;; legacy/manual jobs only; contract-sourced jobs without explicit channels do
  ;; not fall back to these defaults in event_agents/job-channels.
  (->> (or jobs [])
       (mapcat (fn [job]
                 (or (get-in job [:filters :channels]) [])))
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
                           "off")
        actor-id (or (nonblank-str (:actorId source))
                     (nonblank-str (:actor-id source))
                     (nonblank-str (:actor_id source))
                     (nonblank-str (:actorId agent-source))
                     (nonblank-str (:actor-id agent-source))
                     (nonblank-str (:actor_id agent-source))
                     (nonblank-str (:actorId default-job))
                     (nonblank-str (:actorId (:agentSpec default-job)))
                     (nonblank-str (:actor_id (:agentSpec default-job))))
        sources (vec (or (:sources agent-source)
                         (:runtimeSources agent-source)
                         (:runtime-sources agent-source)
                         (:sources (:agentSpec default-job))
                         []))
        memory-hydration (or (:memoryHydration agent-source)
                             (:memory-hydration agent-source)
                             (:memory_hydration agent-source)
                             (:memoryHydration (:agentSpec default-job))
                             (:memory-hydration (:agentSpec default-job))
                             (:memory_hydration (:agentSpec default-job)))
        context-policy (or (:contextPolicy agent-source)
                           (:context-policy agent-source)
                           (:context_policy agent-source)
                           (:contextPolicy (:agentSpec default-job))
                           (:context-policy (:agentSpec default-job))
                           (:context_policy (:agentSpec default-job)))]
    {:id (:id default-job)
     :name (or (some-> (:name source) str str/trim not-empty) (:name default-job))
     :enabled (not (false? (:enabled source)))
     :trigger {:kind trigger-kind
               :cadenceMinutes (max 1 (min 10080 cadence))
               :eventKinds event-kinds}
     :source {:kind source-kind
              :mode source-mode
              :config (control-law/clamp-source-config (:config source-config)
                                                      (get-in default-job [:source :config]))}
     :filters (or (:filters source) (:filters default-job) {})
     :agentSpec (cond-> {:role role
                        :model (or (some-> (:model agent-source) str str/trim not-empty)
                                   (:model (:agentSpec default-job))
                                   (default-discord-model config))
                        :thinkingLevel thinking-level
                        :systemPrompt (or (prompt-templates/prompt-value (:systemPrompt agent-source))
                                          (prompt-templates/prompt-value (:systemPrompt (:agentSpec default-job)))
                                          "")
                        :taskPrompt (or (prompt-templates/prompt-value (:taskPrompt agent-source))
                                        (prompt-templates/prompt-value (:taskPrompt (:agentSpec default-job)))
                                        "")
                        :toolPolicies (let [normalized (control-law/normalize-tool-policy-list (:toolPolicies agent-source))]
                                        (if (and (seq normalized) (every? some? normalized))
                                          normalized
                                          (control-law/normalize-tool-policy-list (:toolPolicies (:agentSpec default-job)))))
                        :contractId (or (nonblank-str (:contractId agent-source))
                                        (nonblank-str (:contractSourceId source))
                                        (nonblank-str (:contractSourceId default-job)))
                        :actorId actor-id
                        :contractActors (vec (or (:contractActors agent-source)
                                                 (:contractActors (:agentSpec default-job))
                                                 []))}
                  (seq sources) (assoc :sources sources)
                  memory-hydration (assoc :memoryHydration memory-hydration)
                  context-policy (assoc :contextPolicy context-policy))
     :contractSourceId (or (:contractSourceId source)
                           (:contractSourceId default-job))
     :contractSourceKind (or (:contractSourceKind source)
                             (:contractSourceKind default-job))
     :contractSourceKey (or (:contractSourceKey source)
                            (:contractSourceKey default-job))
     :contractHash (or (:contractHash default-job)
                       (:contractHash source))
     :actorId actor-id
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
