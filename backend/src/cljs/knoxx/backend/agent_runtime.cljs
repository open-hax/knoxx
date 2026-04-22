(ns knoxx.backend.agent-runtime
  (:require [clojure.string :as str]
            [knoxx.backend.agent-hydration :refer [create-knoxx-custom-tools]]
            [knoxx.backend.http :as http]
            [knoxx.backend.http :refer [no-content? request-query-string request-forward-headers request-forward-body]]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.run-state :refer [tool-event-payload append-run-event!]]
            [knoxx.backend.runtime.models :refer [normalize-thinking-level effective-thinking-level models-config allowlisted-model-id?]]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.tooling :refer [create-runtime-tools]]))

(defonce sdk-runtime* (atom nil))
(defonce agent-sessions* (atom {}))

;; Proxx session affinity: Proxx uses prompt_cache_key to lock a session to an
;; account+provider pair. pi-coding-agent does not emit prompt_cache_key by
;; default, so we inject it via an always-on extension installed into the
;; Knoxx agent runtime directory.
(def ^:private proxx-session-affinity-extension-code
  (str
   "import type { ExtensionAPI } from \"@mariozechner/pi-coding-agent\";\n\n"
   "function isRecord(value: unknown): value is Record<string, unknown> {\n"
   "  return typeof value === 'object' && value !== null;\n"
   "}\n\n"
   "export default function (pi: ExtensionAPI) {\n"
   "  pi.on('before_provider_request', (event, ctx) => {\n"
   "    // Only touch Knoxx→Proxx traffic.\n"
   "    if (ctx.model?.provider !== 'proxx') return;\n"
   "\n"
   "    const sessionKey = typeof ctx.sessionManager?.getSessionId === 'function'\n"
   "      ? String(ctx.sessionManager.getSessionId() ?? '').trim()\n"
   "      : '';\n"
   "    if (!sessionKey) return;\n"
   "\n"
   "    const payload = event.payload;\n"
   "    if (payload === null || typeof payload !== 'object') return;\n"
   "    const record = payload as Record<string, unknown>;\n"
   "\n"
   "    const existing = typeof record.prompt_cache_key === 'string'\n"
   "      ? record.prompt_cache_key.trim()\n"
   "      : typeof record.promptCacheKey === 'string'\n"
   "        ? record.promptCacheKey.trim()\n"
   "        : '';\n"
   "\n"
   "    if (existing) return record;\n"
   "\n"
   "    // Proxx extracts prompt_cache_key from request bodies to enforce affinity.\n"
   "    return { ...record, prompt_cache_key: sessionKey };\n"
   "  });\n"
   "}\n"))

(defn- js-array-seq
  [value]
  (if (array? value) (array-seq value) []))

(defn- proxx-models-url
  [config]
  (let [base (str (or (:proxx-base-url config) ""))]
    (cond
      (str/ends-with? base "/v1") (str base "/models")
      (str/ends-with? base "/v1/") (str base "models")
      (str/ends-with? base "/") (str base "v1/models")
      :else (str base "/v1/models"))))

(defn- fetch-proxx-model-ids!
  "Fetch available model ids from Proxx /v1/models so Knoxx's pi model registry includes
   local Ollama (gemma4, qwen, etc) as well as upstream hosted models.

   Returns a Promise of vector of strings."
  [config]
  (let [token (str (or (:proxx-auth-token config) ""))
        url (proxx-models-url config)]
    (if (str/blank? token)
      (js/Promise.resolve [])
      (-> (js/fetch url #js {:headers #js {"Authorization" (str "Bearer " token)
                                           "Accept" "application/json"}})
          (.then (fn [resp]
                   (if (aget resp "ok")
                     (.json resp)
                     (js/Promise.reject (js/Error. (str "Proxx /v1/models failed with status " (aget resp "status")))))))
          (.then (fn [payload]
                   (let [items (js-array-seq (or (aget payload "data") #js []))
                         ids (->> items
                                  (map (fn [item]
                                         (let [raw (aget item "id")]
                                           (when (and raw (not (str/blank? (str raw))))
                                             (str raw)))))
                                  (remove nil?)
                                  (filter (fn [model-id]
                                            (allowlisted-model-id? config model-id)))
                                  distinct
                                  vec)]
                     ids)))
          (.catch (fn [_err]
                    ;; Keep Knoxx running even if Proxx is offline or auth fails.
                    (js/Promise.resolve [])))))))

(defn- stored-content-part->agent-part
  [part]
  (let [part-type (some-> (:type part) str str/lower-case)
        text (some-> (:text part) str)
        data (or (:data part) (:url part))
        mime-type (some-> (:mimeType part) str)
        filename (some-> (:filename part) str)]
    (case part-type
      "text" (when (not (str/blank? (str text)))
               #js {:type "text" :text text})
      "image" (when (not (str/blank? (str data)))
                 #js {:type "image" :data data :mimeType mime-type})
      "audio" (when (not (str/blank? (str data)))
                 #js {:type "audio" :data data :mimeType mime-type})
      "video" (when (not (str/blank? (str data)))
                 #js {:type "video" :data data :mimeType mime-type})
      "document" (when (not (str/blank? (str data)))
                     #js {:type "document" :data data :mimeType mime-type :filename filename})
      nil)))

(defn stored-session-message->agent-message
  [message]
  (let [role (some-> (:role message) str)
        content (some-> (:content message) str)
        content-parts (vec (keep stored-content-part->agent-part (or (:content-parts message) [])))
        payload (cond
                  (seq content-parts) (clj->js content-parts)
                  (not (str/blank? content)) #js [#js {:type "text" :text content}]
                  :else nil)]
    (when (and (contains? #{"user" "assistant" "system"} role)
               payload)
      #js {:role role
           :content payload
           :timestamp (.now js/Date)})))

(defn- planner-row->stored-session-message
  [row]
  (let [role (some-> (:role row) str)
        text (some-> (:text row) str)]
    (when (and (contains? #{"user" "assistant" "system"} role)
               (not (str/blank? text)))
      {:role role
       :content text})))

(defn- fetch-openplanner-session-messages!
  [config conversation-id]
  (if (or (str/blank? conversation-id)
          (not (http/openplanner-enabled? config)))
    (js/Promise.resolve [])
    (-> (http/openplanner-request! config "GET" (str "/v1/sessions/" conversation-id))
        (.then (fn [body]
                 (->> (or (:rows body) [])
                      (keep planner-row->stored-session-message)
                      vec)))
        (.catch (fn [err]
                  (.warn js/console "[knoxx] failed to fetch OpenPlanner session transcript" err)
                  [])))))

(defn- comparable-session-message
  [message]
  {:role (some-> (:role message) str)
   :content (some-> (:content message) str)})

(defn merge-restored-session-messages
  [base-messages overlay-messages]
  (let [base (vec (or base-messages []))
        overlay (vec (or overlay-messages []))
        base* (mapv comparable-session-message base)
        overlay* (mapv comparable-session-message overlay)
        overlap (loop [n (min (count base*) (count overlay*))]
                  (cond
                    (zero? n) 0
                    (= (subvec base* (- (count base*) n))
                       (subvec overlay* 0 n)) n
                    :else (recur (dec n))))]
    (into base (subvec overlay overlap))))

(defn rehydrate-session-manager-from-redis!
  [config session-manager conversation-id]
  (let [redis-client (redis/get-client)]
    (-> (.all js/Promise
              #js [(fetch-openplanner-session-messages! config conversation-id)
                   (if (or (str/blank? conversation-id) (nil? redis-client))
                     (js/Promise.resolve [])
                     (-> (session-store/get-conversation-active-session redis-client conversation-id)
                         (.then (fn [session-id]
                                  (if (str/blank? (str (or session-id "")))
                                    []
                                    (-> (session-store/get-session redis-client session-id)
                                        (.then (fn [session]
                                                 (vec (or (:messages session) []))))))))))])
        (.then (fn [parts]
                 (let [openplanner-messages (vec (or (aget parts 0) []))
                       redis-messages (vec (or (aget parts 1) []))
                       merged-messages (merge-restored-session-messages openplanner-messages redis-messages)]
                   (doseq [message merged-messages]
                     (when-let [agent-message (stored-session-message->agent-message message)]
                       (.appendMessage session-manager agent-message)))
                   #js {:sessionManager session-manager
                        :restored (boolean (seq merged-messages))})))))

(defn request-stream-body
  [request]
  (let [method (str/upper-case (or (aget request "method") "GET"))
        body (request-forward-body request)
        content-type (str/lower-case (str (or (aget request "headers" "content-type") "")))]
    (cond
      (contains? #{"GET" "HEAD"} method) #js {}
      (some? body) #js {:body body}
      (str/includes? content-type "multipart/form-data") #js {:body (aget request "raw")
                                                               :duplex "half"}
      :else #js {})))

(defn forward-knoxx-request!
  [config request method path extra]
  (let [target-url (str (:knoxx-base-url config) "/api/" path (request-query-string request))
        base #js {:method method
                  :headers (request-forward-headers request {"x-api-key" (when-not (str/blank? (:knoxx-api-key config)) (:knoxx-api-key config))})}
        stream-opts (request-stream-body request)]
    (js/fetch target-url (.assign js/Object base stream-opts (clj->js extra)))))

(defn resolve-workspace-path
  [runtime config raw-path]
  (let [node-path (aget runtime "path")
        workspace-root (.resolve node-path (:workspace-root config))
        candidate (if (.isAbsolute node-path raw-path)
                    (.resolve node-path raw-path)
                    (.resolve node-path workspace-root raw-path))
        rel (.relative node-path workspace-root candidate)]
    (when (or (str/starts-with? rel "..") (.isAbsolute node-path rel))
      (throw (js/Error. "Path escapes workspace root")))
    candidate))

(defn ensure-sdk-runtime!
  [runtime config]
  (if-let [p @sdk-runtime*]
    p
    (let [node-fs (aget runtime "fs")
          node-path (aget runtime "path")
          sdk (aget runtime "sdk")
          runtime-dir (:agent-dir config)
          models-file (.join node-path runtime-dir "models.json")
          auth-file (.join node-path runtime-dir "auth.json")
          extensions-dir (.join node-path runtime-dir "extensions")
          affinity-extension-file (.join node-path extensions-dir "proxx-session-affinity.ts")
          SettingsManager (aget sdk "SettingsManager")
          AuthStorage (aget sdk "AuthStorage")
          ModelRegistry (aget sdk "ModelRegistry")
          DefaultResourceLoader (aget sdk "DefaultResourceLoader")
          settings-manager (.inMemory SettingsManager (clj->js {:compaction {:enabled false}
                                                                :retry {:enabled true :maxRetries 1}}))
          p (-> (.mkdir node-fs runtime-dir #js {:recursive true})
                (.then (fn []
                         (-> (fetch-proxx-model-ids! config)
                             (.then (fn [model-ids]
                                      (-> (.writeFile node-fs
                                                      models-file
                                                      (.stringify js/JSON (clj->js (models-config config model-ids)) nil 2)
                                                      "utf8")
                                          (.then (fn [] nil))))))))
                (.then (fn []
                         (-> (.mkdir node-fs extensions-dir #js {:recursive true})
                             (.then (fn []
                                      (.writeFile node-fs
                                                  affinity-extension-file
                                                  proxx-session-affinity-extension-code
                                                  "utf8"))))))
                (.then (fn []
                         (let [auth-storage (.create AuthStorage auth-file)
                               _ (when-not (str/blank? (:proxx-auth-token config))
                                   (.setRuntimeApiKey auth-storage "proxx" (:proxx-auth-token config)))
                               model-registry (ModelRegistry. auth-storage models-file)
                               loader (DefaultResourceLoader.
                                       #js {:cwd (:workspace-root config)
                                            :agentDir runtime-dir
                                            :settingsManager settings-manager
                                            :systemPromptOverride (fn [_] (:agent-system-prompt config))})]
                           (-> (.reload loader)
                               (.then (fn []
                                        #js {:authStorage auth-storage
                                             :modelRegistry model-registry
                                             :settingsManager settings-manager
                                             :loader loader
                                             :runtimeDir runtime-dir})))))))]
      (reset! sdk-runtime* p)
      p)))

(defn create-agent-session!
  ([runtime config conversation-id model-id] (create-agent-session! runtime config conversation-id model-id nil (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context] (create-agent-session! runtime config conversation-id model-id auth-context (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context thinking-level]
   (create-agent-session! runtime config conversation-id model-id auth-context thinking-level nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id]
   (-> (ensure-sdk-runtime! runtime config)
      (.then
       (fn [sdk-runtime]
         (let [sdk (aget runtime "sdk")
               SessionManager (aget sdk "SessionManager")
               createAgentSession (aget sdk "createAgentSession")
                model-registry (aget sdk-runtime "modelRegistry")
                auth-storage (aget sdk-runtime "authStorage")
                loader (aget sdk-runtime "loader")
                settings-manager (aget sdk-runtime "settingsManager")
                thinking-level (effective-thinking-level config model-id (or (normalize-thinking-level thinking-level)
                                                                                 thinking-level
                                                                                 (:agent-thinking-level config)
                                                                                 "off"))
                model (or (.find model-registry "proxx" model-id)
                          (.find model-registry "proxx" (:proxx-default-model config)))
                create-session (fn [session-manager]
                                 (-> (createAgentSession
                                      #js {:cwd (:workspace-root config)
                                          :agentDir (aget sdk-runtime "runtimeDir")
                                          :authStorage auth-storage
                                          :modelRegistry model-registry
                                          :resourceLoader loader
                                          :settingsManager settings-manager
                                          :sessionManager session-manager
                                          :model model
                                           :thinkingLevel thinking-level
                                           :tools (clj->js (create-runtime-tools runtime config auth-context))
                                           :customTools (create-knoxx-custom-tools runtime config auth-context)})
                                     (.then (fn [result]
                                              (let [session (aget result "session")]
                                                (.setThinkingLevel session thinking-level)
                                                session)))))]
             (if (no-content? model)
               (js/Promise.reject (js/Error. (str "No pi model configured for " model-id)))
               (let [session-manager (.inMemory SessionManager (:workspace-root config))]
                 (when-not (str/blank? (str (or session-id "")))
                   (.newSession session-manager #js {:id (str session-id)}))
                 (.appendModelChange session-manager "proxx" model-id)
                 (.appendThinkingLevelChange session-manager thinking-level)
                 (-> (rehydrate-session-manager-from-redis! config session-manager conversation-id)
                     (.then (fn [result]
                              (create-session (aget result "sessionManager"))))))))))))))

(defn- visible-tool-signature
  [runtime config auth-context]
  (let [name-of (fn [tool]
                  (or (some-> tool (aget "name") str str/trim not-empty)
                      (some-> tool (aget "id") str str/trim not-empty)
                      (some-> tool (aget "label") str str/trim not-empty)))
        builtin-tools (or (create-runtime-tools runtime config auth-context) [])
        custom-tools (if-let [tools (create-knoxx-custom-tools runtime config auth-context)]
                       (if (array? tools) (array-seq tools) [])
                       [])]
    (->> (concat builtin-tools custom-tools)
         (keep name-of)
         sort
         distinct
         (str/join "|"))))

(defn ensure-agent-session!
  ([runtime config conversation-id model-id] (ensure-agent-session! runtime config conversation-id model-id nil (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context] (ensure-agent-session! runtime config conversation-id model-id auth-context (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context thinking-level]
   (ensure-agent-session! runtime config conversation-id model-id auth-context thinking-level nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id]
  (let [thinking-level (effective-thinking-level config model-id (or (normalize-thinking-level thinking-level)
                                                                        thinking-level
                                                                        (:agent-thinking-level config)
                                                                        "off"))
        current-tool-signature (visible-tool-signature runtime config auth-context)]
    (if-let [entry (get @agent-sessions* conversation-id)]
      (let [session (:session entry)
            active-model (:model-id entry)
            active-tool-signature (:tool-signature entry)]
        (if (and (some? session)
                 (= (str active-model) (str model-id))
                 (= (str (or active-tool-signature "")) (str (or current-tool-signature ""))))
          (do
            (.setThinkingLevel session thinking-level)
            (js/Promise.resolve session))
          ;; Model or tool access changed mid-conversation: rebuild session so the requested runtime is respected.
          (-> (create-agent-session! runtime config conversation-id model-id auth-context thinking-level session-id)
              (.then (fn [next-session]
                       (swap! agent-sessions* assoc conversation-id {:session next-session
                                                                     :model-id model-id
                                                                     :tool-signature current-tool-signature})
                       next-session)))))
      (-> (create-agent-session! runtime config conversation-id model-id auth-context thinking-level session-id)
          (.then (fn [session]
                   (swap! agent-sessions* assoc conversation-id {:session session
                                                                 :model-id model-id
                                                                 :tool-signature current-tool-signature})
                   session)))))))

(defn active-agent-session
  [conversation-id]
  (:session (get @agent-sessions* conversation-id)))

(defn remove-agent-session!
  "Keep completed conversation sessions warm in-process so follow-up turns retain live context.
   Redis/OpenPlanner rehydration remains the fallback path across restarts or instance changes."
  [_conversation-id]
  nil)

(defn queue-agent-control!
  [_runtime _config {:keys [conversation-id session-id run-id message kind]}]
  (cond
    (str/blank? conversation-id)
    (js/Promise.reject (js/Error. "conversation_id is required for live controls"))

    (str/blank? message)
    (js/Promise.reject (js/Error. "message is required for live controls"))

    :else
    (if-let [session (active-agent-session conversation-id)]
      (if-not (true? (aget session "isStreaming"))
        (js/Promise.reject (js/Error. "No active running turn is available for live controls"))
        (let [preview (if (> (count message) 240)
                        (str (subs message 0 240) "…")
                        message)
              event-type (if (= kind "follow_up") "follow_up_queued" "steer_queued")
              failure-type (if (= kind "follow_up") "follow_up_failed" "steer_failed")
              invoke (if (= kind "follow_up")
                       #(.followUp session message)
                       #(.steer session message))]
          (-> (invoke)
              (.then (fn []
                       (let [event (tool-event-payload run-id conversation-id session-id event-type
                                                       {:status "queued"
                                                        :preview preview})]
                         (when run-id
                           (append-run-event! run-id event))
                         (broadcast-ws-session! session-id "events" event)
                         {:ok true
                          :conversation_id conversation-id
                          :session_id session-id
                          :run_id run-id
                          :kind kind})))
              (.catch (fn [err]
                        (let [event (tool-event-payload run-id conversation-id session-id failure-type
                                                        {:status "failed"
                                                         :error (str err)
                                                         :preview preview})]
                          (when run-id
                            (append-run-event! run-id event))
                          (broadcast-ws-session! session-id "events" event))
                        (throw err))))))
      (js/Promise.reject (js/Error. "Conversation is not active in the agent runtime")))))
