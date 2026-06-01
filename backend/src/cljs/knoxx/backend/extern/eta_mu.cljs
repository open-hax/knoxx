(ns knoxx.backend.extern.eta-mu
  "JS boundary for the eta-mu agent SDK.
   All aget/aset/#js/interop on eta-mu objects lives here.
   Callers receive CLJS maps or EtaMuSession records — never raw JS shapes."
  (:require [clojure.string :as str]
            ["@open-hax/eta-mu-cli" :as eta-mu]
            ["node:fs/promises" :as fs]
            ["node:path" :as node-path]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.shape.agent :refer [IAgentSession]]))

;; ─── Class constructors (read from module at call time) ──────────────────────

(defn settings-manager-class ^js [] (aget eta-mu "SettingsManager"))
(defn auth-storage-class     ^js [] (aget eta-mu "AuthStorage"))
(defn model-registry-class   ^js [] (aget eta-mu "ModelRegistry"))
(defn resource-loader-class  ^js [] (aget eta-mu "DefaultResourceLoader"))
(defn session-manager-class  ^js [] (aget eta-mu "SessionManager"))
(defn create-agent-session-fn ^js [] (aget eta-mu "createAgentSession"))

;; ─── Post-init singleton accessors ──────────────────────────────────────────
;; These are populated by the eta-mu runtime after loader.reload() completes.

(defn model-registry   ^js [] (aget eta-mu "modelRegistry"))
(defn auth-storage     ^js [] (aget eta-mu "authStorage"))
(defn loader           ^js [] (aget eta-mu "loader"))
(defn settings-manager ^js [] (aget eta-mu "settingsManager"))
(defn runtime-dir      []     (aget eta-mu "runtimeDir"))

;; ─── Small JS helpers local to the eta-mu boundary ───────────────────────────

(defn- js-array-seq
  [value]
  (if (array? value)
    (array-seq value)
    []))

(defn- write-file!
  [path text]
  (.writeFile fs (str path) (str text) "utf8"))

(defn- mkdirp!
  [path]
  (.mkdir fs (str path) #js {:recursive true}))

(defn- path-join
  [& parts]
  (.apply (.-join node-path) node-path (clj->js (keep #(when % (str %)) parts))))

(defn- provider-token
  [env-var]
  (when-let [env-var (some-> env-var str str/trim not-empty)]
    (let [token (aget js/process.env env-var)]
      (when (and (string? token) (not (str/blank? token)))
        token))))

;; ─── Eta-mu runtime setup ────────────────────────────────────────────────────

(defn ^:async setup-runtime!
  "Initialise eta-mu runtime and persist models.json.
   Accepts CLJS config/model maps; returns a CLJS map containing opaque SDK
   objects under :auth-storage, :model-registry, :settings-manager, :loader,
   and :runtime-dir."
  [config model-config compaction-settings]
  (let [runtime-dir-value (:agent-dir config)
        models-file       (path-join runtime-dir-value "models.json")
        auth-file         (path-join runtime-dir-value "auth.json")
        SettingsManager   (settings-manager-class)
        AuthStorage       (auth-storage-class)
        ModelRegistry     (model-registry-class)
        ResourceLoader    (resource-loader-class)
        settings-manager  (.inMemory SettingsManager
                                      (clj->js {:compaction compaction-settings
                                                :retry {:enabled true
                                                        :maxRetries 1}}))]
    (await (mkdirp! runtime-dir-value))
    (await (write-file! models-file (.stringify js/JSON (clj->js model-config) nil 2)))
    (let [auth-storage (.create AuthStorage auth-file)]
      (when-not (str/blank? (:proxx-auth-token config))
        (.setRuntimeApiKey auth-storage "proxx" (:proxx-auth-token config)))
      (doseq [[provider-id env-var] (or (:provider-auth-tokens config) {})]
        (when-let [token (provider-token env-var)]
          (when-let [provider-id (some-> provider-id str str/trim not-empty)]
            (.setRuntimeApiKey auth-storage provider-id token))))
      (let [model-registry (ModelRegistry. auth-storage models-file)
            resource-loader (ResourceLoader.
                             #js {:cwd (:workspace-root config)
                                  :agentDir runtime-dir-value
                                  :settingsManager settings-manager})]
        (await (.reload resource-loader))
        {:auth-storage auth-storage
         :model-registry model-registry
         :settings-manager settings-manager
         :loader resource-loader
         :runtime-dir runtime-dir-value}))))

(defn make-session-manager!
  "Create an eta-mu SessionManager and optionally seed a specific session id."
  [workspace-root session-id]
  (let [manager (.inMemory (session-manager-class) workspace-root)
        session-id (some-> session-id str str/trim not-empty)]
    (when session-id
      (.newSession manager (clj->js {:id session-id})))
    manager))

(defn append-message!
  [session-manager agent-message]
  (.appendMessage session-manager agent-message))

(defn append-model-change!
  [session-manager provider-id model-id]
  (.appendModelChange session-manager (str provider-id) model-id))

(defn append-thinking-level-change!
  [session-manager thinking-level]
  (.appendThinkingLevelChange session-manager thinking-level))

(defn find-model
  [model-registry provider-id model-id fallback-model-id]
  (or (.find model-registry (str provider-id) model-id)
      (.find model-registry "proxx" model-id)
      (.find model-registry "proxx" fallback-model-id)))

;; ─── Eta-mu tool object helpers ──────────────────────────────────────────────

(defn tool-seq
  "Return a CLJS seq for an eta-mu JS tool array or []."
  [tools]
  (js-array-seq tools))

(defn tool-runtime-name
  [tool]
  (cond
    (string? tool) (some-> tool str str/trim not-empty)
    :else (or (some-> tool (aget "name") str str/trim not-empty)
              (some-> tool (aget "id") str str/trim not-empty)
              (some-> tool (aget "label") str str/trim not-empty))))

(defn tool-execute
  [tool]
  (let [execute (and tool (aget tool "execute"))]
    (when (fn? execute) execute)))

(defn set-tool-execute!
  [tool execute]
  (aset tool "execute" execute)
  tool)

(defn with-promise-finally
  [result finalizer]
  (if (and result (fn? (aget result "finally")))
    (.finally result finalizer)
    (do
      (finalizer)
      result)))

;; ─── Media materialisation hook ──────────────────────────────────────────────

(defn- raw-media-part->map
  [part]
  {:type (some-> (aget part "type") str)
   :url (some-> (aget part "url") str not-empty)
   :data (some-> (aget part "data") str not-empty)
   :mimeType (some-> (aget part "mimeType") str not-empty)})

(defn- media-part?
  [part]
  (contains? #{"image" "audio"} (some-> (:type part) str str/lower-case)))

(defn- result-media-parts
  [ctx]
  (let [result    (aget ctx "result")
        details   (when result (aget result "details"))
        raw-parts (or (when details (aget details "content_parts"))
                      (when details (aget details "contentParts"))
                      #js [])]
    (->> (js-array-seq raw-parts)
         (map raw-media-part->map)
         (filter media-part?)
         vec)))

(defn media-materialize-hook
  "Build an eta-mu after-tool-call hook from a CLJS materialize fn.
   materialize! receives a CLJS media part map and resolves to a CLJS media map."
  [materialize!]
  (fn [ctx _signal]
    (let [result (aget ctx "result")
          media-parts (result-media-parts ctx)]
      (if (seq media-parts)
        (-> (promise/all (mapv materialize! media-parts))
            (.then (fn [materialized]
                     (let [good (->> (js-array-seq materialized) (remove nil?) vec)]
                       (when (seq good)
                         (let [existing (or (some-> result (aget "content")) #js [])
                               merged   (clj->js (into (vec (js-array-seq existing)) good))]
                           #js {:content merged})))))
            (.catch (fn [_] nil)))
        (js/Promise.resolve nil)))))

;; ─── Session wrapper / creation ──────────────────────────────────────────────

(defrecord EtaMuSession [^js raw]
  IAgentSession
  (streaming?          [_]   (true? (aget raw "isStreaming")))
  (current-turn        [_]   (aget raw "currentTurn"))
  (messages            [_]   (let [msgs (aget raw "messages")]
                               (when (array? msgs) (array-seq msgs))))
  (subscribe!          [_ h] (.subscribe raw h))
  (send-user-message!  [_ c] (.sendUserMessage raw c))
  (follow-up!          [_ m] (.followUp raw m))
  (steer!              [_ m] (.steer raw m))
  (set-thinking-level! [_ l] (.setThinkingLevel raw l)))

(defn wrap-eta-mu-session
  "Wrap a raw eta-mu JS session object, optionally registering an after-tool-call
   hook. Returns an EtaMuSession that implements IAgentSession."
  ([^js raw-session]
   (->EtaMuSession raw-session))
  ([^js raw-session on-tool-call]
   (when (and (fn? on-tool-call)
              (fn? (some-> raw-session (aget "agent") (aget "setAfterToolCall"))))
     (.setAfterToolCall (aget raw-session "agent") on-tool-call))
   (->EtaMuSession raw-session)))

(defn create-session!
  "Create and wrap an eta-mu agent session from CLJS options. Returns a Promise
   because the eta-mu SDK creates sessions asynchronously."
  [opts]
  (let [create-agent-session (create-agent-session-fn)
        runtime-dir-value    (or (:runtime-dir opts) (runtime-dir))
        hook                 (when-let [materialize! (:materialize! opts)]
                               (media-materialize-hook materialize!))]
    (-> (create-agent-session
         #js {:cwd (:workspace-root opts)
              :agentDir runtime-dir-value
              :authStorage (:auth-storage opts)
              :modelRegistry (:model-registry opts)
              :resourceLoader (:loader opts)
              :settingsManager (:settings-manager opts)
              :sessionManager (:session-manager opts)
              :model (:model opts)
              :thinkingLevel (:thinking-level opts)
              :tools (clj->js (or (:tool-name-allowlist opts) []))
              :customTools (:custom-tools opts)})
        (.then (fn [created]
                 (wrap-eta-mu-session (aget created "session") hook))))))
