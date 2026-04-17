(ns knoxx.backend.runtime-config
  (:require [clojure.string :as str]))

(declare default-model-prefix-allowlist parse-positive-int)

(def role-tools
  {"system_admin" [["read" "Read" "Read files and retrieved context"]
                    ["write" "Write" "Create new markdown drafts and artifacts"]
                    ["edit" "Edit" "Revise existing documents and drafts"]
                    ["bash" "Shell" "Run controlled shell commands"]
                    ["websearch" "Web Search" "Search the live web through Proxx websearch"]
                    ["canvas" "Canvas" "Open long-form markdown drafting canvas"]
                    ["email.send" "Email" "Send drafts through configured email account"]
                    ["discord.publish" "Discord Publish" "Publish updates to Discord"]
                    ["discord.send" "Discord Send" "Send Discord messages and replies"]
                    ["discord.read" "Discord Read" "Read messages from Discord channels"]
                    ["discord.channel.messages" "Discord Channel Messages" "Fetch messages from a Discord channel"]
                    ["discord.channel.scroll" "Discord Channel Scroll" "Scroll older messages in a Discord channel"]
                    ["discord.dm.messages" "Discord DM Messages" "Fetch messages from a Discord DM channel"]
                    ["discord.search" "Discord Search" "Search messages in Discord channels"]
                    ["discord.guilds" "Discord Guilds" "List Discord servers the bot is in"]
                    ["discord.channels" "Discord Channels" "List channels in a Discord server"]
                    ["discord.list.servers" "Discord List Servers" "List all Discord servers the bot can access"]
                    ["discord.list.channels" "Discord List Channels" "List channels across one or all Discord servers"]
                    ["event_agents.status" "Event Agent Status" "Inspect scheduled event-agent runtime state and configuration"]
                    ["event_agents.dispatch" "Event Agent Dispatch" "Dispatch a structured event into the event-agent runtime"]
                    ["event_agents.run_job" "Event Agent Run Job" "Trigger a configured event-agent job immediately"]
                    ["event_agents.upsert_job" "Event Agent Upsert Job" "Create or update a scheduled event-agent job"]
                    ["schedule_event_agent" "Schedule Event Agent" "Create or update a scheduled event-agent job with prompts, tools, triggers, and source config"]
                    ["bluesky.publish" "Bluesky" "Publish updates to Bluesky"]
                    ["music.identify_file" "Music Identify" "Identify songs from audio files using AudD API"]
                    ["music.acoustid_lookup" "AcoustID Lookup" "Look up audio fingerprints via AcoustID"]
                    ["music.musicbrainz_recording" "MusicBrainz" "Look up recording metadata by MBID"]
                    ["music.copyright_check" "Copyright Check" "Check copyright status of audio"]
                    ["audio.spectrogram" "Audio Spectrogram" "Generate spectrogram from audio"]
                    ["audio.waveform" "Audio Waveform" "Generate waveform from audio"]
                    ["multimodal.upload" "Multimodal Upload" "Upload images, audio, video, and documents for multimodal AI"]]
   "org_admin" [["read" "Read" "Read files and retrieved context"]
                 ["write" "Write" "Create new markdown drafts and artifacts"]
                 ["edit" "Edit" "Revise existing documents and drafts"]
                 ["bash" "Shell" "Run controlled shell commands"]
                 ["websearch" "Web Search" "Search the live web through Proxx websearch"]
                 ["canvas" "Canvas" "Open long-form markdown drafting canvas"]
                 ["email.send" "Email" "Send drafts through configured email account"]
                 ["discord.publish" "Discord Publish" "Publish updates to Discord"]
                 ["discord.send" "Discord Send" "Send Discord messages and replies"]
                 ["discord.read" "Discord Read" "Read messages from Discord channels"]
                 ["discord.channel.messages" "Discord Channel Messages" "Fetch messages from a Discord channel"]
                 ["discord.channel.scroll" "Discord Channel Scroll" "Scroll older messages in a Discord channel"]
                 ["discord.dm.messages" "Discord DM Messages" "Fetch messages from a Discord DM channel"]
                 ["discord.search" "Discord Search" "Search messages in Discord channels"]
                 ["discord.guilds" "Discord Guilds" "List Discord servers the bot is in"]
                 ["discord.channels" "Discord Channels" "List channels in a Discord server"]
                 ["discord.list.servers" "Discord List Servers" "List all Discord servers the bot can access"]
                 ["discord.list.channels" "Discord List Channels" "List channels across one or all Discord servers"]
                 ["event_agents.status" "Event Agent Status" "Inspect scheduled event-agent runtime state and configuration"]
                 ["event_agents.dispatch" "Event Agent Dispatch" "Dispatch a structured event into the event-agent runtime"]
                 ["event_agents.run_job" "Event Agent Run Job" "Trigger a configured event-agent job immediately"]
                 ["event_agents.upsert_job" "Event Agent Upsert Job" "Create or update a scheduled event-agent job"]
                 ["schedule_event_agent" "Schedule Event Agent" "Create or update a scheduled event-agent job with prompts, tools, triggers, and source config"]
                 ["bluesky.publish" "Bluesky" "Publish updates to Bluesky"]
                 ["music.identify_file" "Music Identify" "Identify songs from audio files using AudD API"]
                 ["music.acoustid_lookup" "AcoustID Lookup" "Look up audio fingerprints via AcoustID"]
                 ["music.musicbrainz_recording" "MusicBrainz" "Look up recording metadata by MBID"]
                 ["music.copyright_check" "Copyright Check" "Check copyright status of audio"]
                 ["audio.spectrogram" "Audio Spectrogram" "Generate spectrogram from audio"]
                 ["audio.waveform" "Audio Waveform" "Generate waveform from audio"]
                 ["multimodal.upload" "Multimodal Upload" "Upload images, audio, video, and documents for multimodal AI"]]
   "knowledge_worker" [["read" "Read" "Read files and retrieved context"]
                        ["canvas" "Canvas" "Open long-form markdown drafting canvas"]]
   "data_analyst" [["read" "Read" "Read files and retrieved context"]
                    ["write" "Write" "Create new markdown drafts and artifacts"]
                    ["edit" "Edit" "Revise existing documents and drafts"]
                    ["canvas" "Canvas" "Open long-form markdown drafting canvas"]]
   "developer" [["read" "Read" "Read files and retrieved context"]
                 ["write" "Write" "Create new markdown drafts and artifacts"]
                 ["edit" "Edit" "Revise existing documents and drafts"]
                 ["bash" "Shell" "Run controlled shell commands"]
                 ["canvas" "Canvas" "Open long-form markdown drafting canvas"]]
   "executive" [["read" "Read" "Read files and retrieved context"]
                 ["websearch" "Web Search" "Search the live web through Proxx websearch"]
                 ["canvas" "Canvas" "Open long-form markdown drafting canvas"]
                 ["discord.read" "Discord Read" "Read messages from Discord channels"]
                 ["discord.channel.messages" "Discord Channel Messages" "Fetch messages from a Discord channel"]
                 ["discord.search" "Discord Search" "Search messages in Discord channels"]
                 ["discord.list.servers" "Discord List Servers" "List all Discord servers the bot can access"]
                 ["discord.list.channels" "Discord List Channels" "List channels across one or all Discord servers"]
                 ["event_agents.status" "Event Agent Status" "Inspect scheduled event-agent runtime state and configuration"]
                 ["event_agents.upsert_job" "Event Agent Upsert Job" "Create or update a scheduled event-agent job"]
                 ["event_agents.run_job" "Event Agent Run Job" "Trigger a configured event-agent job immediately"]
                 ["event_agents.dispatch" "Event Agent Dispatch" "Dispatch a structured event into the event-agent runtime"]
                 ["schedule_event_agent" "Schedule Event Agent" "Create or update a scheduled event-agent job with prompts, tools, triggers, and source config"]]
   "principal_architect" [["read" "Read" "Read files and retrieved context"]
                          ["write" "Write" "Create new markdown drafts and artifacts"]
                          ["edit" "Edit" "Revise existing documents and drafts"]
                          ["bash" "Shell" "Run controlled shell commands"]
                          ["canvas" "Canvas" "Open long-form markdown drafting canvas"]]
   "junior_dev" [["read" "Read" "Read files and retrieved context"]
                  ["write" "Write" "Create new markdown drafts and notes"]
                  ["canvas" "Canvas" "Open long-form markdown drafting canvas"]]
   "translator" [["read" "Read" "Read source documents for translation"]
                 ["memory_search" "Memory Search" "Search prior translation sessions"]
                 ["memory_session" "Memory Session" "Load prior translation context"]
                 ["graph_query" "Graph Query" "Query translation examples from knowledge graph"]
                 ["save_translation" "Save Translation" "Save translated content to database"]]})

(def role-aliases
  {"executive" "knowledge_worker"
   "principal_architect" "developer"
   "junior_dev" "knowledge_worker"})

(defn env
  [k default]
  (or (aget js/process.env k) default))

(defonce config* (atom nil))

(defn- parse-prefix-allowlist
  [raw]
  (-> (str (or raw ""))
      (str/split #",")
      (->> (map (fn [v] (some-> v str str/trim not-empty)))
           (remove nil?)
           vec)))

(defn parse-string-list
  [raw]
  (->> (str/split (str (or raw "")) #",")
       (map (fn [value] (some-> value str str/trim not-empty)))
       (remove nil?)
       vec))

(defn allowlisted-model-id?
  "Returns true if model-id should be visible/selectable in Knoxx.

   Allowlist is configured via config key :model-prefix-allowlist, which defaults
   to glm-5*, gpt-5*, qwen3*, gemma4:*.

   Note: this is a simple prefix match, not a glob/regex engine."
  [config model-id]
  (let [prefixes (let [configured (seq (:model-prefix-allowlist config))]
                   (or configured default-model-prefix-allowlist))
        id (str (or model-id ""))]
    (boolean (some (fn [prefix]
                     (str/starts-with? id (str prefix)))
                   prefixes))))

(def ^:private thinking-levels
  #{"off" "minimal" "low" "medium" "high" "xhigh"})

(defn normalize-thinking-level
  [value]
  (let [normalized (some-> value str str/trim str/lower-case not-empty)]
    (when (contains? thinking-levels normalized)
      normalized)))

(defn cfg []
  (let [fresh-config {:app-name (env "APP_NAME" "Knoxx Backend CLJS")
   :host (env "HOST" "0.0.0.0")
   :port (js/parseInt (env "PORT" "8000") 10)
   :workspace-root (env "WORKSPACE_ROOT" "/app/workspace/devel")
   :project-name (env "WORKSPACE_PROJECT_NAME" "devel")
   :session-project-name (env "KNOXX_SESSION_PROJECT_NAME" "knoxx-session")
   :collection-name (env "KNOXX_COLLECTION_NAME" "devel_docs")
   :proxx-base-url (env "PROXX_BASE_URL" "http://proxx:8789")
   :proxx-auth-token (env "PROXX_AUTH_TOKEN" "")
   :proxx-default-model (env "PROXX_DEFAULT_MODEL" "glm-5")
   :model-prefix-allowlist (parse-prefix-allowlist
                            (env "KNOXX_MODEL_PREFIX_ALLOWLIST" "glm-5,gpt-5,qwen3,gemma4:,gemma3:,deepseek,kimi-k2,nemotron,cogito,devstral,minimax,ministral,mistral-large"))
   :agent-thinking-level (or (normalize-thinking-level (env "KNOXX_THINKING_LEVEL" "off")) "off")
   :reasoning-model-prefixes (env "KNOXX_REASONING_MODEL_PREFIXES" "glm-")
   :proxx-embed-model (env "PROXX_EMBED_MODEL" "nomic-embed-text:latest")
   :openplanner-base-url (or (aget js/process.env "OPENPLANNER_BASE_URL")
                             (aget js/process.env "OPENPLANNER_URL")
                             "http://host.docker.internal:7777")
   :openplanner-api-key (env "OPENPLANNER_API_KEY" "")
   :model-lab-openai-api-key (env "MODEL_LAB_OPENAI_API_KEY" "")
   :knoxx-admin-url (env "KNOXX_ADMIN_URL" "http://localhost")
   :knoxx-base-url (env "KNOXX_BASE_URL" "http://localhost:8000")
   :knoxx-api-key (env "KNOXX_API_KEY" "")
   :shibboleth-base-url (env "SHIBBOLETH_BASE_URL" "")
   :shibboleth-ui-url (env "SHIBBOLETH_UI_URL" "")
   :knoxx-default-role (env "KNOXX_DEFAULT_ROLE" "executive")
   :gmail-app-email (env "GMAIL_APP_EMAIL" "")
   :gmail-app-password (env "GMAIL_APP_PASSWORD" "")
   :discord-bot-token (env "DISCORD_BOT_TOKEN" "")
   ;; Voice / speech
   :stt-base-url (env "KNOXX_STT_BASE_URL" "")
   :agent-dir (env "KNOXX_AGENT_DIR" "/tmp/knoxx-agent")
   :redis-url (env "REDIS_URL" "")
   :agent-system-prompt (env "KNOXX_AGENT_SYSTEM_PROMPT"
                             "You are Knoxx, the grounded workspace assistant for the devel corpus. Preserve multi-turn context within the active conversation, use workspace tools when needed, cite file paths when they matter, and prefer grounded synthesis over shallow enumeration. Treat passive semantic hydration as helpful but incomplete; when corpus grounding matters, use semantic_query, semantic_read, and graph_query instead of guessing. Long-term conversational memory lives in OpenPlanner; when the user asks about previous sessions, prior decisions, or your own earlier actions, use memory_search and memory_session instead of pretending to remember.")}
        existing @config*]
    (if existing
      (merge fresh-config existing)
      fresh-config)))

(defn discord-agent-role-options
  []
  (->> (keys role-tools)
       sort
       vec))

(defn- default-discord-channels
  []
  (parse-string-list (env "DISCORD_CHANNEL_IDS" "")))

(defn- default-discord-keywords
  []
  (let [keywords (parse-string-list (env "DISCORD_TARGET_KEYWORDS" "knoxx,cephalon"))]
    (if (seq keywords)
      (mapv str/lower-case keywords)
      ["knoxx" "cephalon"])))

(defn default-discord-agent-jobs
  [config]
  (let [default-model (or (:proxx-default-model config) "glm-5")
        default-role (if (contains? role-tools "system_admin") "system_admin" (:knoxx-default-role config))]
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
  (let [allowed-roles (set (discord-agent-role-options))
        source (merge default-job (or raw-job {}))
        cadence (or (parse-positive-int (:cadenceMinutes source))
                    (:cadenceMinutes default-job)
                    5)
        role (let [candidate (some-> (:role source) str str/trim not-empty)]
               (if (contains? allowed-roles candidate)
                 candidate
                 (:role default-job)))
        thinking-level (or (normalize-thinking-level (:thinkingLevel source))
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
  []
  (discord-agent-role-options))

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
       :effect (if (#{"allow" "deny"} effect)
                 effect
                 "allow")})))

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
   {:toolId "memory_search" :effect "allow"}
   {:toolId "graph_query" :effect "allow"}])

(defn- default-custom-event-agent-job
  [config job-id]
  (let [default-model (or (:proxx-default-model config) "glm-5")
        default-role (if (contains? role-tools "system_admin") "system_admin" (:knoxx-default-role config))]
    {:id job-id
     :name (or job-id "custom-job")
     :enabled true
     :trigger {:kind "event"
               :cadenceMinutes 5
               :eventKinds []}
     :source {:kind "manual"
              :mode "respond"
              :config {}}
     :filters {}
     :agentSpec {:role default-role
                 :model default-model
                 :thinkingLevel "off"
                 :systemPrompt "You are Knoxx's scheduled event agent. Respond to dispatched events, use Discord tools when needed, and emit useful actions without filler."
                 :taskPrompt "A structured event matched this job. Read context, decide what action is useful, and use available tools deliberately."
                 :toolPolicies (default-discord-tool-policies)}
     :description "Custom scheduled event-agent job"}))

(defn default-event-agent-control
  [config]
  (let [default-model (or (:proxx-default-model config) "glm-5")
        default-role (if (contains? role-tools "system_admin") "system_admin" (:knoxx-default-role config))
        default-discord-source {:botUserId (or (some-> (env "DISCORD_BOT_USER_ID" "") str str/trim not-empty) "")
                                :defaultChannels (default-discord-channels)
                                :targetKeywords (default-discord-keywords)}]
    {:sources {:discord default-discord-source
               :github {:webhookSecretConfigured (boolean (some-> (env "GITHUB_WEBHOOK_SECRET" "") str str/trim not-empty))}
               :cron {}}
     :jobs [{:id "discord-patrol"
             :name "Discord patrol"
             :enabled false
             :trigger {:kind "cron"
                       :cadenceMinutes 5
                       :eventKinds []}
             :source {:kind "discord"
                      :mode "patrol"
                      :config {:maxMessages 25}}
             :filters {:channels (default-discord-channels)
                       :keywords (default-discord-keywords)}
             :agentSpec {:role default-role
                         :model default-model
                         :thinkingLevel "off"
                         :systemPrompt "Observe configured Discord channels, detect fresh human signals, and queue structured events without speaking publicly."
                         :taskPrompt "Read recent channel messages, update freshness state, and dispatch normalized Discord events for worthy human signals."
                         :toolPolicies []}}
            {:id "discord-mention-response"
             :name "Discord mention response"
             :enabled true
             :trigger {:kind "event"
                       :cadenceMinutes 1
                       :eventKinds ["discord.message.mention" "discord.message.keyword"]}
             :source {:kind "discord"
                      :mode "respond"
                      :config {:maxMessages 12}}
             :filters {:channels (default-discord-channels)
                       :keywords (default-discord-keywords)}
             :agentSpec {:role default-role
                         :model default-model
                         :thinkingLevel "off"
                         :systemPrompt "You are Knoxx's targeted event-driven Discord responder. Read the room, use tools when needed, and prefer silence over filler."
                         :taskPrompt "A normalized Discord event matched this job. Read more context if needed, then decide whether to reply with discord.publish."
                         :toolPolicies (default-discord-tool-policies)}}
            {:id "discord-deep-synthesis"
             :name "Discord deep synthesis"
             :enabled true
             :trigger {:kind "cron"
                       :cadenceMinutes 120
                       :eventKinds []}
             :source {:kind "discord"
                      :mode "synthesize"
                      :config {:maxMessages 12}}
             :filters {:channels (default-discord-channels)
                       :keywords (default-discord-keywords)}
             :agentSpec {:role default-role
                         :model default-model
                         :thinkingLevel "minimal"
                         :systemPrompt "You are Knoxx's strategic Discord synthesizer. Look across channels, find meaningful patterns, and only intervene when synthesis helps humans."
                         :taskPrompt "Summarize recent cross-channel Discord activity, identify meaningful opportunities or risks, and decide whether to publish a concise proactive message."
                         :toolPolicies (default-discord-tool-policies)}}]}))

(defn- normalize-event-agent-job
  [config default-job raw-job]
  (let [allowed-roles (set (event-agent-role-options))
        source (merge default-job (or raw-job {}))
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
        thinking-level (or (normalize-thinking-level (:thinkingLevel agent-source))
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
              :config (or (:config source-config) {})}
     :filters (or (:filters source) (:filters default-job) {})
     :agentSpec {:role role
                 :model (or (some-> (:model agent-source) str str/trim not-empty)
                            (:model (:agentSpec default-job))
                            (:proxx-default-model config))
                 :thinkingLevel thinking-level
                 :systemPrompt (or (some-> (:systemPrompt agent-source) str not-empty)
                                   (:systemPrompt (:agentSpec default-job))
                                   "")
                 :taskPrompt (or (some-> (:taskPrompt agent-source) str not-empty)
                                 (:taskPrompt (:agentSpec default-job))
                                 "")
                 :toolPolicies (let [normalized (normalize-tool-policy-list (:toolPolicies agent-source))]
                                 (if (seq normalized)
                                   normalized
                                   (normalize-tool-policy-list (:toolPolicies (:agentSpec default-job)))))}
     :description (or (some-> (:description source) str str/trim not-empty)
                      (:description default-job))}))

(defn event-agent-control-config
  [config]
  (let [saved (or (:event-agent-control config) {})
        defaults (default-event-agent-control config)
        default-sources (:sources defaults)
        saved-sources (or (:sources saved) {})
        default-jobs (:jobs defaults)
        saved-jobs (vec (or (:jobs saved) []))
        default-job-ids (into #{} (map :id) default-jobs)
        saved-jobs-by-id (into {} (map (fn [job] [(:id job) job])) saved-jobs)
        custom-jobs (->> saved-jobs
                         (keep (fn [job]
                                 (let [job-id (some-> (:id job) str str/trim not-empty)]
                                   (when (and job-id (not (contains? default-job-ids job-id)))
                                     (normalize-event-agent-job config
                                                                (default-custom-event-agent-job config job-id)
                                                                job)))))
                         vec)]
    {:sources {:discord (merge (:discord default-sources) (or (:discord saved-sources) {}))
               :github (merge (:github default-sources) (or (:github saved-sources) {}))
               :cron (merge (:cron default-sources) (or (:cron saved-sources) {}))}
     :jobs (vec (concat
                 (mapv (fn [default-job]
                         (normalize-event-agent-job config default-job (get saved-jobs-by-id (:id default-job))))
                       default-jobs)
                 custom-jobs))}))

(def ^:private default-model-prefix-allowlist
  ["glm-5" "gpt-5" "qwen3" "gemma4:" "gemma3:" "deepseek" "kimi-k2" "nemotron" "cogito" "devstral" "minimax" "ministral" "mistral-large"])

(defn model-supports-reasoning?
  [config model-id]
  (let [normalized-model (some-> model-id str str/trim str/lower-case)
        prefixes (->> (str/split (or (:reasoning-model-prefixes config) "") #",")
                      (map str/trim)
                      (remove str/blank?))]
    (boolean
     (and normalized-model
          (some (fn [prefix]
                  (let [normalized-prefix (-> prefix
                                              str/lower-case
                                              (str/replace #"\*$" ""))]
                    (str/starts-with? normalized-model normalized-prefix)))
                prefixes)))))

(defn model-thinking-format
  [model-id]
  (let [normalized-model (some-> model-id str str/trim str/lower-case)]
    (cond
      (and normalized-model (str/starts-with? normalized-model "glm-")) "zai"
      :else nil)))

(defn now-iso []
  (.toISOString (js/Date.)))

(defn parse-positive-int
  [value]
  (let [n (cond
            (string? value) (js/parseInt value 10)
            (number? value) value
            :else js/NaN)]
    (when (and (number? n)
               (not (js/isNaN n))
               (pos? n))
      n)))

(defn truthy-param?
  [value]
  (cond
    (true? value) true
    (number? value) (pos? value)
    (string? value) (contains? #{"1" "true" "yes" "on" "force"}
                                (str/lower-case (str/trim value)))
    :else false))

(defn tool-cost []
  {:input 0 :output 0 :cacheRead 0 :cacheWrite 0})

(defn provider-model-config
  [config model-id]
  {:id model-id
   :name model-id
   :reasoning (model-supports-reasoning? config model-id)
   :input ["text"]
   :contextWindow 128000
   :maxTokens 8192
   :cost (tool-cost)})

(defn proxx-openai-base-url
  [config]
  (let [base (:proxx-base-url config)]
    (cond
      (str/ends-with? base "/v1") base
      (str/ends-with? base "/") (str base "v1")
      :else (str base "/v1"))))

(defn per-model-compat
  "Compute per-model compat so reasoning/thinking settings aren't
   incorrectly shared across models that don't support them."
  [config model-id]
  (cond-> {:supportsDeveloperRole false}
    (model-supports-reasoning? config model-id)
    (assoc :supportsReasoningEffort true)
    (some? (model-thinking-format model-id))
    (assoc :thinkingFormat (model-thinking-format model-id))))

(defn models-config
  ([config]
   (models-config config nil))
  ([config model-ids]
   (let [default-model (:proxx-default-model config)
         normalized-models (->> (or model-ids [])
                                (map (fn [m] (some-> m str str/trim not-empty)))
                                (remove nil?)
                                distinct
                                vec)
         models (if (seq normalized-models)
                  normalized-models
                  [default-model])
         base-compat {:supportsDeveloperRole false}]
     {:providers
      {:proxx
       {:baseUrl (proxx-openai-base-url config)
        :apiKey "PROXX_AUTH_TOKEN"
        :authHeader true
        :api "openai-completions"
        :compat base-compat
        :models (mapv (fn [model-id]
                        (merge (provider-model-config config model-id)
                               {:compat (per-model-compat config model-id)}))
                      models)}}})))

(defn default-settings
  [config]
  {:llmModel (:proxx-default-model config)
   :embedModel (:proxx-embed-model config)
   :maxContextTokens 128000
   :llmMaxTokens 8192
   :llmBaseUrl (proxx-openai-base-url config)
   :embedBaseUrl (:proxx-base-url config)
   :retrievalMode "dense"
   :retrievalTopK 6
   :hybridTopKDense 12
   :hybridTopKSparse 20
   :hybridTopKFinal 6
   :hybridFusion "rrf"
   :hybridRrfK 60
   :vectorDim 1024
   :chunkTargetTokens 500
   :chunkMaxTokens 700
   :projectName (:project-name config)
   :qdrantCollection (:collection-name config)
   :docsPath (str (:workspace-root config) "/.knoxx/databases/default/docs")
   :docsExtensions ".md,.mdx,.txt,.json,.org,.html,.csv,.pdf"})
