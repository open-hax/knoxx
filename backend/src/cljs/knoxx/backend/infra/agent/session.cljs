(ns knoxx.backend.domain.agent.session
  (:require ["@open-hax/eta-mu-cli" :as eta-mu]
            ["node:fs/promises" :as fs]

            [knoxx.backend.runtime.models :refer [normalize-thinking-level effective-thinking-level models-config allowlisted-model-id? resolve-model-contract]]
            ["node:path" :as path]
            ))

(defonce sessions* (atom {}))

;; Session registry bounds to prevent memory leaks under sustained load.
(def ^:private max-sessions 500)
(def ^:private inactive-ttl-ms (* 4 60 60 1000)) ; 4 hours
(def ^:private sweep-interval-ms 300000) ; 5 minutes
(defn- effective-tool-auth-context
  [auth-context allowed-tool-ids]
  (if-not auth-context
    nil
    (assoc auth-context
           :toolPolicies (mapv (fn [tool-id]
                                 {:toolId tool-id :effect "allow"})
                               (sort (vec allowed-tool-ids))))))

(defn- compaction-settings
  [config]
  {:enabled (not= false (:agent-compaction-enabled? config))
   :reserveTokens (or (positive-int-value (:agent-compaction-reserve-tokens config)) 16384)
   :keepRecentTokens (or (positive-int-value (:agent-compaction-keep-recent-tokens config)) 20000)})
(defn active-session
  [conversation-id]
  (:session (get @sessions* conversation-id)))
(defn- evict-oldest!
  "Remove the least-recently-accessed session when the registry is over capacity."
  []
  (when (> (count @sessions*) max-sessions)
    (let [oldest (apply min-key (comp :last-accessed val) @sessions*)]
      (when oldest
        (swap! sessions* dissoc (key oldest))))))

(defn- start-sweep!
  "Periodically remove agent sessions that have been inactive beyond the TTL."
  []
  (js/setInterval
   (fn []
     (let [cutoff (- (js/Date.now) inactive-ttl-ms)
           stale (for [[id entry] @sessions*
                       :when (< (or (:last-accessed entry) 0) cutoff)]
                   id)]
       (when (seq stale)
         (swap! sessions* #(apply dissoc % stale)))))
   sweep-interval-ms))

(defn- visible-session-signature
  [runtime config auth-context agent-spec]
  (let [allowed-tool-ids (allowed-tool-id-set config
                                              (:role agent-spec)
                                              auth-context
                                              (:contract-id agent-spec)
                                              (:actor-id agent-spec))
        tool-auth-context (effective-tool-auth-context auth-context allowed-tool-ids)
        builtin-tools (or (create-runtime-tools runtime config tool-auth-context (:role agent-spec) (:contract-id agent-spec) (:actor-id agent-spec)) [])
        custom-tools (if-let [tools (create-agent-custom-tools runtime config tool-auth-context agent-spec allowed-tool-ids)]
                       (if (array? tools) (array-seq tools) [])
                       [])]
    (pr-str {:tools (->> (concat builtin-tools custom-tools)
                         (keep tool-runtime-name)
                         sort
                         distinct
                         vec)
             :contract-id (some-> (:contract-id agent-spec) str str/trim not-empty)
             :actor-id (some-> (:actor-id agent-spec) str str/trim not-empty)
             :role (some-> (:role agent-spec) str str/trim not-empty)
             :system-prompt (some-> (:system-prompt agent-spec) str str/trim not-empty)
             :task-prompt (some-> (:task-prompt agent-spec) str str/trim not-empty)})))
(defn rehydrate-session-manager-from-redis!
  ([config session-manager conversation-id]
   (rehydrate-session-manager-from-redis! config session-manager conversation-id nil nil))
  ([config session-manager conversation-id agent-spec]
   (rehydrate-session-manager-from-redis! config session-manager conversation-id nil agent-spec))
  ([config session-manager conversation-id session-id agent-spec]
   (let [redis-client (redis/get-client)
         preferred-session-id (some-> session-id str str/trim not-empty)
         fetch-session-messages!
         (fn [target-session-id]
           (if (or (str/blank? (str (or target-session-id "")))
                   (nil? redis-client))
             (js/Promise.resolve [])
             (-> (session-store/get-session redis-client target-session-id)
                 (.then (fn [session]
                          (vec (or (:messages session) [])))))))]
     (-> (.all js/Promise
               #js [(fetch-openplanner-session-messages! config conversation-id)
                    (if preferred-session-id
                      (fetch-session-messages! preferred-session-id)
                      (if (or (str/blank? conversation-id) (nil? redis-client))
                        (js/Promise.resolve [])
                        (-> (session-store/get-conversation-active-session redis-client conversation-id)
                            (.then (fn [active-session-id]
                                     (fetch-session-messages! active-session-id))))))])
         (.then (fn [parts]
                  (let [openplanner-messages (vec (or (aget parts 0) []))
                        redis-messages (vec (or (aget parts 1) []))
                        merged-messages (-> (merge-restored-session-messages openplanner-messages redis-messages)
                                            (sync-system-message (:system-prompt agent-spec))
                                            (#(prune-session-messages agent-spec %)))]
                    (doseq [message merged-messages]
                      (when-let [agent-message (stored-session-message->agent-message message)]
                        (.appendMessage session-manager agent-message)))
                    #js {:sessionManager session-manager
                         :restored (boolean (seq merged-messages))})))))))
(defn ^:async ensure-eta-mu-runtime!
  [_runtime config]
  (let [node-fs fs
        node-path path
        runtime-dir (:agent-dir config)
        models-file (.join node-path runtime-dir "models.json")
        auth-file (.join node-path runtime-dir "auth.json")
        SettingsManager (aget eta-mu "SettingsManager")
        AuthStorage (aget eta-mu "AuthStorage")
        ModelRegistry (aget eta-mu "ModelRegistry")
        DefaultResourceLoader (aget eta-mu "DefaultResourceLoader")
        settings-manager (.inMemory SettingsManager (clj->js {:compaction (compaction-settings config)
                                                              :retry {:enabled true :maxRetries 1}}))]
    (await (.mkdir node-fs runtime-dir #js {:recursive true}))
    (await (.writeFile node-fs
                       models-file
                       (.stringify js/JSON (clj->js (models-config config (await (fetch-proxx-model-ids! config)))) nil 2)
                       "utf8"))
    (let [auth-storage (.create AuthStorage auth-file)
          _ (when-not (str/blank? (:proxx-auth-token config))
              (.setRuntimeApiKey auth-storage "proxx" (:proxx-auth-token config)))
          _ (doseq [[provider-id env-var] (or (:provider-auth-tokens config) {})]
              (let [provider-id (some-> provider-id str str/trim not-empty)
                    env-var (some-> env-var str str/trim not-empty)
                    token (when env-var (aget js/process.env env-var))]
                (when (and provider-id (string? token) (not (str/blank? token)))
                  (.setRuntimeApiKey auth-storage provider-id token))))
          model-registry (ModelRegistry. auth-storage models-file)
          loader (DefaultResourceLoader.
                  #js {:cwd (:workspace-root config)
                       :agentDir runtime-dir
                       :settingsManager settings-manager})]
      (await (.reload loader))
      #js {:authStorage auth-storage
           :modelRegistry model-registry
           :settingsManager settings-manager
           :loader loader
           :runtimeDir runtime-dir})))
(defn ^:async create-session-manager!
  ([runtime config conversation-id model-id] (create-session-manager! runtime config conversation-id model-id nil (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context] (create-session-manager! runtime config conversation-id model-id auth-context (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context thinking-level]
   (create-session-manager! runtime config conversation-id model-id auth-context thinking-level nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id]
   (create-session-manager! runtime config conversation-id model-id auth-context thinking-level session-id nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id agent-spec]
   (await (ensure-eta-mu-runtime! runtime config))
   (let [SessionManager (aget eta-mu "SessionManager")
         createAgentSession (aget eta-mu "createAgentSession")
         model-registry (aget eta-mu "modelRegistry")
         auth-storage (aget eta-mu "authStorage")
         loader (aget eta-mu "loader")
         settings-manager (aget eta-mu "settingsManager")
         thinking-level (effective-thinking-level config model-id (or (normalize-thinking-level thinking-level)
                                                                      thinking-level
                                                                      (:agent-thinking-level config)
                                                                      "off"))
         model-provider-id (or (some-> (resolve-model-contract config model-id) :provider)
                               "proxx")
         model (or (.find model-registry (str model-provider-id) model-id)
                   (.find model-registry "proxx" model-id)
                   (.find model-registry "proxx" (:proxx-default-model config)))
         allowed-tool-ids (allowed-tool-id-set config
                                               (:role agent-spec)
                                               auth-context
                                               (:contract-id agent-spec)
                                               (:actor-id agent-spec))
         tool-auth-context (effective-tool-auth-context auth-context allowed-tool-ids)
         builtin-tools (create-runtime-tools runtime config tool-auth-context (:role agent-spec) (:contract-id agent-spec) (:actor-id agent-spec))
         custom-tools (wrap-custom-tools-with-agent-context!
                       (create-agent-custom-tools runtime config tool-auth-context agent-spec allowed-tool-ids)
                       {:session-id session-id
                        :conversation-id conversation-id
                        :agent-spec agent-spec})
         tool-name-allowlist (enabled-tool-name-allowlist builtin-tools custom-tools)
         create-session ]
     (if (no-content? model)
       (js/Promise.reject (js/Error. (str "No eta-mu model configured for " model-id)))
       (let [session-manager (.inMemory SessionManager (:workspace-root config))]
         (when-not (str/blank? (str (or session-id "")))
           (.newSession session-manager #js {:id (str session-id)}))
         (.appendModelChange session-manager (str model-provider-id) model-id)
         (.appendThinkingLevelChange session-manager thinking-level)
         (-> (rehydrate-session-manager-from-redis! config session-manager conversation-id session-id agent-spec)
             (.then (fn [result]
                      (create-session (aget result "sessionManager"))))))))))
(defn create-session [session-manager]
  (let [session (-> (createAgentSession
                  #js {:cwd (:workspace-root config)
                       :agentDir (aget eta-mu "runtimeDir")
                       :authStorage auth-storage
                       :modelRegistry model-registry
                       :resourceLoader loader
                       :settingsManager settings-manager
                       :sessionManager session-manager
                       :model model
                       :thinkingLevel thinking-level
                       :tools (clj->js tool-name-allowlist)
                       :customTools custom-tools})
                 (aget "session"))]
    (.setThinkingLevel session thinking-level)
    ;; Wire afterToolCall: inject media from tool results
    ;; into the LLM context immediately. When the agent
    ;; reads a Discord channel image or loads workspace audio,
    ;; it sees/hears it inline on the next model turn.
    (when (fn? (some-> session (aget "agent") (aget "setAfterToolCall")))
      (.setAfterToolCall
       (aget session "agent")
       (fn [ctx _signal]
         (let [result     (aget ctx "result")
               details    (when result (aget result "details"))
               raw-parts  (or (when details (aget details "content_parts"))
                              (when details (aget details "contentParts"))
                              #js [])
               media-parts (->> (array-seq raw-parts)
                                (filter #(contains? #{"image" "audio"}
                                                    (some-> (aget % "type") str str/lower-case)))
                                vec)
               fetch-b64! (fn [url media-type]
                            (-> (js/fetch url)
                                (.then (fn [r]
                                         (when-not (.-ok r)
                                           (throw (js/Error. (str media-type " fetch failed: " (.-status r)))))
                                         (.-arrayBuffer r)))
                                (.then (fn [ab]
                                         (let [buf (js/Buffer.from ab)]
                                           (str "data:" media-type ";base64," (.toString buf "base64")))))))
               audio-format (fn [mime]
                              (mime->audio-format mime))
               media-object (fn [part-type data mime]
                              (let [obj #js {:type part-type
                                             :data data
                                             :mimeType mime}]
                                (when (= "audio" part-type)
                                  (aset obj "format" (or (audio-format mime) "mp3")))
                                obj))
               materialize! (fn [part]
                              (let [part-type (some-> (aget part "type") str str/lower-case)
                                    url       (some-> (aget part "url")  str not-empty)
                                    data      (some-> (aget part "data") str not-empty)
                                    mime      (or (some-> (aget part "mimeType") str not-empty)
                                                  (if (= "audio" part-type) "audio/mpeg" "image/png"))]
                                (cond
                                  (and data (str/starts-with? data "data:"))
                                  (js/Promise.resolve
                                   (let [comma (.indexOf data ",")]
                                     (media-object part-type
                                                   (if (>= comma 0) (.slice data (inc comma)) data)
                                                   mime)))
                                  (and data (not (str/starts-with? data "http")))
                                  (js/Promise.resolve
                                   (media-object part-type data mime))
                                  url
                                  (-> (fetch-b64! url mime)
                                      (.then (fn [data-url]
                                               (let [comma (.indexOf data-url ",")]
                                                 (media-object part-type
                                                               (if (>= comma 0) (.slice data-url (inc comma)) data-url)
                                                               mime)))))
                                  :else (js/Promise.resolve nil))))]
           (if (seq media-parts)
             (-> (.all js/Promise (clj->js (mapv materialize! media-parts)))
                 (.then (fn [materialized]
                          (let [good (->> (array-seq materialized) (remove nil?) vec)]
                            (when (seq good)
                              (let [existing (or (some-> result (aget "content")) #js [])
                                    merged   (clj->js (into (vec (array-seq existing)) good))]
                                #js {:content merged})))))
                 (.catch (fn [_] nil)))
             (js/Promise.resolve nil))))))
    session))

(defn ensure-agent-session!
  ([runtime config conversation-id model-id] (ensure-agent-session! runtime config conversation-id model-id nil (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context] (ensure-agent-session! runtime config conversation-id model-id auth-context (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context thinking-level]
   (ensure-agent-session! runtime config conversation-id model-id auth-context thinking-level nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id]
   (ensure-agent-session! runtime config conversation-id model-id auth-context thinking-level session-id nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id agent-spec]
   (let [thinking-level (effective-thinking-level config model-id (or (normalize-thinking-level thinking-level)
                                                                      thinking-level
                                                                      (:agent-thinking-level config)
                                                                      "off"))
         current-tool-signature (visible-session-signature runtime config auth-context agent-spec)]
     (if-let [entry (get @sessions* conversation-id)]
       (let [session (:session entry)
             active-model (:model-id entry)
             active-tool-signature (:tool-signature entry)]
         (if (and (some? session)
                  (= (str active-model) (str model-id))
                  (= (str (or active-tool-signature "")) (str (or current-tool-signature ""))))
           (do
             (.setThinkingLevel session thinking-level)
             (register-actor-live-route! runtime conversation-id session-id agent-spec)
             (js/Promise.resolve session))
           ;; Model or tool access changed mid-conversation: rebuild session so the requested runtime is respected.
           (-> (create-agent-session! runtime config conversation-id model-id auth-context thinking-level session-id agent-spec)
               (.then (fn [next-session]
                        (let [ctx (ext-runtime/build-extension-ctx runtime config
                                                                   :conversation-id conversation-id
                                                                   :session-id session-id
                                                                   :model-id model-id
                                                                   :auth-context auth-context)]
                          (ext-runtime/dispatch-event "session_switch"
                                                      #js {:conversationId conversation-id
                                                           :sessionId session-id}
                                                      ctx))
                         (evict-oldest!)
                         (swap! agent-sessions* assoc conversation-id {:session next-session
                                                                       :model-id model-id
                                                                       :tool-signature current-tool-signature
                                                                       :session-id session-id
                                                                       :actor-id (:actor-id agent-spec)
                                                                       :last-accessed (js/Date.now)})
                        (register-actor-live-route! runtime conversation-id session-id agent-spec)
                        next-session)))))
       (-> (create-agent-session! runtime config conversation-id model-id auth-context thinking-level session-id agent-spec)
           (.then (fn [session]
                    (let [ctx (ext-runtime/build-extension-ctx runtime config
                                                               :conversation-id conversation-id
                                                               :session-id session-id
                                                               :model-id model-id
                                                               :auth-context auth-context)]
                      (ext-runtime/dispatch-event "session_start"
                                                  #js {:conversationId conversation-id
                                                       :sessionId session-id}
                                                  ctx))
                     (evict-oldest!)
                     (swap! agent-sessions* assoc conversation-id {:session session
                                                                   :model-id model-id
                                                                   :tool-signature current-tool-signature
                                                                   :session-id session-id
                                                                   :actor-id (:actor-id agent-spec)
                                                                   :last-accessed (js/Date.now)})
                     (register-actor-live-route! runtime conversation-id session-id agent-spec)
                     session)))))))


(defn- visible-session-signature
  [runtime config auth-context agent-spec]
  (let [allowed-tool-ids (allowed-tool-id-set config
                                              (:role agent-spec)
                                              auth-context
                                              (:contract-id agent-spec)
                                              (:actor-id agent-spec))
        tool-auth-context (effective-tool-auth-context auth-context allowed-tool-ids)
        builtin-tools (or (create-runtime-tools runtime config tool-auth-context (:role agent-spec) (:contract-id agent-spec) (:actor-id agent-spec)) [])
        custom-tools (if-let [tools (create-agent-custom-tools runtime config tool-auth-context agent-spec allowed-tool-ids)]
                       (if (array? tools) (array-seq tools) [])
                       [])]
    (pr-str {:tools (->> (concat builtin-tools custom-tools)
                         (keep tool-runtime-name)
                         sort
                         distinct
                         vec)
             :contract-id (some-> (:contract-id agent-spec) str str/trim not-empty)
             :actor-id (some-> (:actor-id agent-spec) str str/trim not-empty)
             :role (some-> (:role agent-spec) str str/trim not-empty)
             :system-prompt (some-> (:system-prompt agent-spec) str str/trim not-empty)
             :task-prompt (some-> (:task-prompt agent-spec) str str/trim not-empty)})))

(defn rehydrate-session-manager-from-redis!
  ([config session-manager conversation-id]
   (rehydrate-session-manager-from-redis! config session-manager conversation-id nil nil))
  ([config session-manager conversation-id agent-spec]
   (rehydrate-session-manager-from-redis! config session-manager conversation-id nil agent-spec))
  ([config session-manager conversation-id session-id agent-spec]
   (let [redis-client (redis/get-client)
         preferred-session-id (some-> session-id str str/trim not-empty)
         fetch-session-messages!
         (fn [target-session-id]
           (if (or (str/blank? (str (or target-session-id "")))
                   (nil? redis-client))
             (js/Promise.resolve [])
             (-> (session-store/get-session redis-client target-session-id)
                 (.then (fn [session]
                          (vec (or (:messages session) [])))))))]
     (-> (.all js/Promise
               #js [(fetch-openplanner-session-messages! config conversation-id)
                    (if preferred-session-id
                      (fetch-session-messages! preferred-session-id)
                      (if (or (str/blank? conversation-id) (nil? redis-client))
                        (js/Promise.resolve [])
                        (-> (session-store/get-conversation-active-session redis-client conversation-id)
                            (.then (fn [active-session-id]
                                     (fetch-session-messages! active-session-id))))))])
         (.then (fn [parts]
                  (let [openplanner-messages (vec (or (aget parts 0) []))
                        redis-messages (vec (or (aget parts 1) []))
                        merged-messages (-> (merge-restored-session-messages openplanner-messages redis-messages)
                                            (sync-system-message (:system-prompt agent-spec))
                                            (#(prune-session-messages agent-spec %)))]
                    (doseq [message merged-messages]
                      (when-let [agent-message (stored-session-message->agent-message message)]
                        (.appendMessage session-manager agent-message)))
                    #js {:sessionManager session-manager
                         :restored (boolean (seq merged-messages))})))))))

(defn remove!
  "Dispatch session_shutdown to extensions, then release the in-process session entry.
   Redis/OpenPlanner rehydration remains the fallback path across restarts or instance changes."
  [conversation-id]
  (when-let [entry (get @agent-sessions* conversation-id)]
    (let [ctx (ext-runtime/build-extension-ctx
               #js {} {}
               :conversation-id conversation-id
               :session-id (:session-id entry))]
      (ext-runtime/dispatch-event "session_shutdown"
                                  #js {:conversationId conversation-id}
                                  ctx)))
  (swap! agent-sessions* dissoc conversation-id)
  nil)
