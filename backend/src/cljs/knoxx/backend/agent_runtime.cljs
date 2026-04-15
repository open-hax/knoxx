(ns knoxx.backend.agent-runtime
  (:require [clojure.string :as str]
            [knoxx.backend.agent-hydration :refer [create-knoxx-custom-tools]]
            [knoxx.backend.http :refer [no-content? request-query-string request-forward-headers request-forward-body]]
            [knoxx.backend.openplanner-memory :refer [rehydrate-session-manager!]]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.run-state :refer [tool-event-payload append-run-event!]]
            [knoxx.backend.runtime-config :refer [normalize-thinking-level models-config]]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.tooling :refer [create-runtime-tools]]))

(defonce sdk-runtime* (atom nil))
(defonce agent-sessions* (atom {}))

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
                                  distinct
                                  vec)]
                     ids)))
          (.catch (fn [_err]
                    ;; Keep Knoxx running even if Proxx is offline or auth fails.
                    (js/Promise.resolve [])))))))

(defn stored-session-message->agent-message
  [message]
  (let [role (some-> (:role message) str)
        content (some-> (:content message) str)]
    (when (and (contains? #{"user" "assistant" "system"} role)
               (not (str/blank? content)))
      #js {:role role
           :content #js [#js {:type "text" :text content}]
           :timestamp (.now js/Date)})))

(defn rehydrate-session-manager-from-redis!
  [session-manager conversation-id]
  (let [redis-client (redis/get-client)]
    (if (or (str/blank? conversation-id) (nil? redis-client))
      (js/Promise.resolve #js {:sessionManager session-manager
                               :restored false})
      (-> (session-store/get-conversation-active-session redis-client conversation-id)
          (.then (fn [session-id]
                   (if (str/blank? (str (or session-id "")))
                     #js {:sessionManager session-manager
                          :restored false}
                     (-> (session-store/get-session redis-client session-id)
                         (.then (fn [session]
                                  (let [messages (vec (or (:messages session) []))]
                                    (doseq [message messages]
                                      (when-let [agent-message (stored-session-message->agent-message message)]
                                        (.appendMessage session-manager agent-message)))
                                    #js {:sessionManager session-manager
                                         :restored (boolean (seq messages))})))))))))))

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
                thinking-level (or (normalize-thinking-level thinking-level)
                                   (:agent-thinking-level config)
                                   "off")
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
                 (.appendModelChange session-manager "proxx" model-id)
                 (.appendThinkingLevelChange session-manager thinking-level)
                 (-> (rehydrate-session-manager-from-redis! session-manager conversation-id)
                     (.then (fn [result]
                              (let [restored? (aget result "restored")
                                    hydrated-manager (aget result "sessionManager")]
                                (if restored?
                                  (create-session hydrated-manager)
                                  (-> (rehydrate-session-manager! config hydrated-manager conversation-id model-id)
                                      (.then (fn [openplanner-manager]
                                               (create-session openplanner-manager)))))))))))))))))

(defn ensure-agent-session!
  ([runtime config conversation-id model-id] (ensure-agent-session! runtime config conversation-id model-id nil (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context] (ensure-agent-session! runtime config conversation-id model-id auth-context (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context thinking-level]
  (let [thinking-level (or (normalize-thinking-level thinking-level)
                           (:agent-thinking-level config)
                           "off")]
    (if-let [entry (get @agent-sessions* conversation-id)]
      (let [session (:session entry)
            active-model (:model-id entry)]
        (if (and (some? session)
                 (= (str active-model) (str model-id)))
          (do
            (.setThinkingLevel session thinking-level)
            (js/Promise.resolve session))
          ;; Model changed mid-conversation: rebuild session so the requested model is respected.
          (-> (create-agent-session! runtime config conversation-id model-id auth-context thinking-level)
              (.then (fn [next-session]
                       (swap! agent-sessions* assoc conversation-id {:session next-session :model-id model-id})
                       next-session)))))
      (-> (create-agent-session! runtime config conversation-id model-id auth-context thinking-level)
          (.then (fn [session]
                   (swap! agent-sessions* assoc conversation-id {:session session :model-id model-id})
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
