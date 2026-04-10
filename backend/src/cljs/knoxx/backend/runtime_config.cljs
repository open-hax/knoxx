(ns knoxx.backend.runtime-config
  (:require [clojure.string :as str]))

(def role-tools
  {"system_admin" [["read" "Read" "Read files and retrieved context"]
                    ["write" "Write" "Create new markdown drafts and artifacts"]
                    ["edit" "Edit" "Revise existing documents and drafts"]
                    ["bash" "Shell" "Run controlled shell commands"]
                    ["canvas" "Canvas" "Open long-form markdown drafting canvas"]
                    ["email.send" "Email" "Send drafts through configured email account"]
                    ["discord.publish" "Discord" "Publish updates to Discord"]
                    ["bluesky.publish" "Bluesky" "Publish updates to Bluesky"]]
   "org_admin" [["read" "Read" "Read files and retrieved context"]
                 ["write" "Write" "Create new markdown drafts and artifacts"]
                 ["edit" "Edit" "Revise existing documents and drafts"]
                 ["bash" "Shell" "Run controlled shell commands"]
                 ["canvas" "Canvas" "Open long-form markdown drafting canvas"]
                 ["email.send" "Email" "Send drafts through configured email account"]
                 ["discord.publish" "Discord" "Publish updates to Discord"]
                 ["bluesky.publish" "Bluesky" "Publish updates to Bluesky"]]
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
                 ["canvas" "Canvas" "Open long-form markdown drafting canvas"]]
   "principal_architect" [["read" "Read" "Read files and retrieved context"]
                          ["write" "Write" "Create new markdown drafts and artifacts"]
                          ["edit" "Edit" "Revise existing documents and drafts"]
                          ["bash" "Shell" "Run controlled shell commands"]
                          ["canvas" "Canvas" "Open long-form markdown drafting canvas"]]
   "junior_dev" [["read" "Read" "Read files and retrieved context"]
                  ["write" "Write" "Create new markdown drafts and notes"]
                  ["canvas" "Canvas" "Open long-form markdown drafting canvas"]]})

(def role-aliases
  {"executive" "knowledge_worker"
   "principal_architect" "developer"
   "junior_dev" "knowledge_worker"})

(defn env
  [k default]
  (or (aget js/process.env k) default))

(def ^:private thinking-levels
  #{"off" "minimal" "low" "medium" "high" "xhigh"})

(defn normalize-thinking-level
  [value]
  (let [normalized (some-> value str str/trim str/lower-case not-empty)]
    (when (contains? thinking-levels normalized)
      normalized)))

(defn cfg []
  {:app-name (env "APP_NAME" "Knoxx Backend CLJS")
   :host (env "HOST" "0.0.0.0")
   :port (js/parseInt (env "PORT" "8000") 10)
   :workspace-root (env "WORKSPACE_ROOT" "/app/workspace/devel")
   :project-name (env "WORKSPACE_PROJECT_NAME" "devel")
   :session-project-name (env "KNOXX_SESSION_PROJECT_NAME" "knoxx-session")
   :collection-name (env "KNOXX_COLLECTION_NAME" "devel_docs")
   :proxx-base-url (env "PROXX_BASE_URL" "http://proxx:8789")
   :proxx-auth-token (env "PROXX_AUTH_TOKEN" "")
   :proxx-default-model (env "PROXX_DEFAULT_MODEL" "glm-5")
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
   :agent-dir (env "KNOXX_AGENT_DIR" "/tmp/knoxx-agent")
   :redis-url (env "REDIS_URL" "")
   :agent-system-prompt (env "KNOXX_AGENT_SYSTEM_PROMPT"
                             "You are Knoxx, the grounded workspace assistant for the devel corpus. Preserve multi-turn context within the active conversation, use workspace tools when needed, cite file paths when they matter, and prefer grounded synthesis over shallow enumeration. Treat passive semantic hydration as helpful but incomplete; when corpus grounding matters, use graph_query as the primary unified graph/memory tool, then semantic_read or memory_session for exact drill-down instead of guessing. Long-term conversational memory lives in OpenPlanner; when the user asks about previous sessions, prior decisions, or your own earlier actions, use graph_query or the knoxx-session scoped memory_search alias, then memory_session for the exact transcript.")})

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

(defn models-config
  [config]
  (let [default-model (:proxx-default-model config)
        compat (cond-> {:supportsDeveloperRole false}
                 (model-supports-reasoning? config default-model)
                 (assoc :supportsReasoningEffort true)
                 (some? (model-thinking-format default-model))
                 (assoc :thinkingFormat (model-thinking-format default-model)))]
    {:providers
     {:proxx
      {:baseUrl (proxx-openai-base-url config)
       :apiKey "PROXX_AUTH_TOKEN"
       :authHeader true
       :api "openai-completions"
       :compat compat
       :models [(provider-model-config config default-model)]}}}))

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
