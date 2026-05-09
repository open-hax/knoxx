(ns knoxx.backend.agent-runtime
  (:require [clojure.string :as str]
            [knoxx.backend.actor-mailbox :as actor-mailbox]
            [knoxx.backend.agent-context :as agent-context]
            [knoxx.backend.agent-hydration :refer [create-agent-custom-tools]]
            [knoxx.backend.http :as http]
            [knoxx.backend.http :refer [no-content? request-query-string request-forward-headers request-forward-body]]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.run-state :refer [tool-event-payload append-run-event!]]
            [knoxx.backend.runtime.models :refer [normalize-thinking-level effective-thinking-level models-config allowlisted-model-id? resolve-model-contract]]
            [knoxx.backend.extension-runtime :as ext-runtime]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.tooling :refer [allowed-tool-id-set create-runtime-tools]]
            ["@open-hax/eta-mu-cli" :as sdk]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

;; Initialize extension runtime at load time
(ext-runtime/init!)

(defonce sdk-runtime* (atom nil))
(defonce agent-sessions* (atom {}))

;; Session registry bounds to prevent memory leaks under sustained load.
(def ^:private max-agent-sessions 500)
(def ^:private agent-session-inactive-ttl-ms (* 4 60 60 1000)) ; 4 hours
(def ^:private agent-session-sweep-interval-ms 300000) ; 5 minutes

(defn- evict-oldest-agent-session!
  "Remove the least-recently-accessed session when the registry is over capacity."
  []
  (when (> (count @agent-sessions*) max-agent-sessions)
    (let [oldest (apply min-key (comp :last-accessed val) @agent-sessions*)]
      (when oldest
        (swap! agent-sessions* dissoc (key oldest))))))

(defn- start-agent-session-sweep!
  "Periodically remove agent sessions that have been inactive beyond the TTL."
  []
  (js/setInterval
   (fn []
     (let [cutoff (- (js/Date.now) agent-session-inactive-ttl-ms)
           stale (for [[id entry] @agent-sessions*
                       :when (< (or (:last-accessed entry) 0) cutoff)]
                   id)]
       (when (seq stale)
         (swap! agent-sessions* #(apply dissoc % stale)))))
   agent-session-sweep-interval-ms))

;; Start the sweep loop once at load time.
(start-agent-session-sweep!)

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
  "Fetch available model ids from Proxx /v1/models so Knoxx's eta-mu model registry includes
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

(defn- mime->audio-format [mime-type]
  (let [mime (some-> mime-type str str/lower-case)]
    (case mime
      "audio/mpeg" "mp3"
      "audio/mp4" "mp4"
      "audio/wav" "wav"
      "audio/x-wav" "wav"
      "audio/ogg" "ogg"
      "audio/flac" "flac"
      "audio/aac" "aac"
      (some-> mime (str/split #"/") second))))

(defn- stored-content-part->agent-part
  [part]
  (let [part-type (some-> (:type part) str str/lower-case)
        text (some-> (:text part) str)
        url (some-> (:url part) str)
        data (some-> (:data part) str)
        mime-type (some-> (:mimeType part) str)
        filename (some-> (:filename part) str)]
    (case part-type
      "text" (when (not (str/blank? (str text)))
               #js {:type "text" :text text})
      ;; Image part routing for the eta-mu SDK:
      ;; eta-mu requires {type "image" :data <RAW-base64-no-prefix> :mimeType "image/..."}.
      ;; Never use image_url shape — eta-mu does not handle it.
      ;; Remote URLs should have been materialized (fetched → data URI) before reaching here.
      "image" (cond
                (and (string? data) (not (str/blank? data)) (str/starts-with? data "data:"))
                (let [comma (.indexOf data ",")
                      raw   (if (>= comma 0) (.slice data (inc comma)) data)
                      mime  (or mime-type
                                (second (re-find #"data:([^;,]+)" data))
                                "image/png")]
                  #js {:type "image" :data raw :mimeType mime})

                (and (string? data) (not (str/blank? data)))
                #js {:type "image" :data data :mimeType (or mime-type "image/png")}

                (and (string? url) (not (str/blank? url)))
                #js {:type "image_url" :image_url #js {:url url}}

                :else nil)
      "audio" (cond
                 (and (string? data) (not (str/blank? data)) (str/starts-with? data "data:"))
                 (let [comma (.indexOf data ",")
                       raw   (if (>= comma 0) (.slice data (inc comma)) data)
                       mime  (or mime-type
                                 (second (re-find #"data:([^;,]+)" data))
                                 "audio/mpeg")]
                   #js {:type "audio"
                        :data raw
                        :mimeType mime
                        :format (or (mime->audio-format mime) "mp3")})

                 (and (string? data) (not (str/blank? data)))
                 #js {:type "audio"
                      :data data
                      :mimeType mime-type
                      :format (or (mime->audio-format mime-type) "mp3")}

                 (and (string? url) (not (str/blank? url)))
                 #js {:type "audio"
                      :data url
                      :mimeType mime-type
                      :format (or (mime->audio-format mime-type) "mp3")}

                 :else nil)
      "video" (when (not (str/blank? (str (or data url))))
                 #js {:type "video" :data (or data url) :mimeType mime-type})
      "document" (when (not (str/blank? (str (or data url))))
                     #js {:type "document" :data (or data url) :mimeType mime-type :filename filename})
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

(defn sync-system-message
  [messages system-prompt]
  (let [items (vec (or messages []))
        prompt (some-> system-prompt str str/trim not-empty)]
    (if-not prompt
      items
      (let [system-index (reduce-kv (fn [_ idx entry]
                                      (when (= "system" (some-> (:role entry) str str/lower-case))
                                        (reduced idx)))
                                    nil
                                    items)]
        (if (some? system-index)
          (let [updated (assoc items system-index {:role "system" :content prompt})]
            (->> updated
                 (keep-indexed (fn [idx entry]
                                 (when (or (not= "system" (some-> (:role entry) str str/lower-case))
                                           (= idx system-index))
                                   entry)))
                 vec))
          (into [{:role "system" :content prompt}] items))))))

(defn- context-policy
  [agent-spec]
  (or (:context-policy agent-spec)
      (:contextPolicy agent-spec)
      (:context agent-spec)
      (get-in agent-spec [:extras :context])
      (get-in agent-spec [:extras :context-policy])))

(defn- positive-int-value
  [value]
  (let [n (js/Number value)]
    (when (and (not (js/isNaN n)) (pos? n))
      (js/Math.floor n))))

(defn- message-text-size
  [message]
  (+ (count (str (or (:content message) "")))
     (reduce + 0 (map #(count (str (or (:text %) (:filename %) (:url %) "")))
                      (or (:content-parts message) (:contentParts message) [])))))

(defn prune-session-messages
  "Apply an agent contract context policy to stored transcript messages.

   Supported contract shape:
   :context {:max-messages 40
             :max-chars 80000
             :preserve-system true}

   This is intentionally a deterministic sliding-window prune. Summary-based
   compression can be layered later, but this prevents unbounded sticky sessions."
  [agent-spec messages]
  (let [items (vec (or messages []))
        policy (context-policy agent-spec)]
    (if-not policy
      items
      (let [max-messages (positive-int-value (or (:max-messages policy)
                                                (:maxMessages policy)
                                                (:max_messages policy)))
            max-chars (positive-int-value (or (:max-chars policy)
                                             (:maxChars policy)
                                             (:max_chars policy)))
            preserve-system? (not= false (or (:preserve-system policy)
                                             (:preserveSystem policy)
                                             (:preserve_system policy)))
            system-messages (if preserve-system?
                              (filterv #(= "system" (some-> (:role %) str str/lower-case)) items)
                              [])
            body-messages (if preserve-system?
                            (remove #(= "system" (some-> (:role %) str str/lower-case)) items)
                            items)
            by-count (if max-messages
                       (take-last max-messages (vec body-messages))
                       (vec body-messages))
            by-chars (if max-chars
                       (loop [remaining (reverse by-count)
                              total 0
                              kept '()]
                         (if-let [message (first remaining)]
                           (let [size (message-text-size message)]
                             (if (and (seq kept) (> (+ total size) max-chars))
                               (vec kept)
                               (recur (rest remaining) (+ total size) (conj kept message))))
                           (vec kept)))
                       (vec by-count))]
        (vec (concat system-messages by-chars))))))

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

(defn- effective-tool-auth-context
  [auth-context allowed-tool-ids]
  (if-not auth-context
    nil
    (assoc auth-context
           :toolPolicies (mapv (fn [tool-id]
                                 {:toolId tool-id :effect "allow"})
                               (sort (vec allowed-tool-ids))))))

(defn- restore-agent-context!
  [previous]
  (if previous
    (agent-context/set-context! previous)
    (agent-context/clear-context!)))

(defn- wrap-tool-execute-with-agent-context!
  [tool context]
  (let [execute (and tool (aget tool "execute"))]
    (when (fn? execute)
      (aset tool "execute"
            (fn [& args]
              (let [previous (agent-context/get-context)]
                (agent-context/set-context! context)
                (try
                  (let [result (apply execute args)]
                    (if (and result (fn? (aget result "finally")))
                      (.finally result (fn [] (restore-agent-context! previous)))
                      (do
                        (restore-agent-context! previous)
                        result)))
                  (catch :default err
                    (restore-agent-context! previous)
                    (throw err))))))))
  tool)

(defn- wrap-custom-tools-with-agent-context!
  [custom-tools context]
  (when custom-tools
    (doseq [tool (if (array? custom-tools) (array-seq custom-tools) [])]
      (wrap-tool-execute-with-agent-context! tool context)))
  custom-tools)

(defn- tool-runtime-name
  "Return the eta-mu runtime name for a built-in/custom tool entry.
   Built-ins are strings after eta-mu 0.70; custom tools are JS objects and may
   have sanitized names such as discord_send with originalName=discord.send."
  [tool]
  (cond
    (string? tool) (some-> tool str str/trim not-empty)
    :else (or (some-> tool (aget "name") str str/trim not-empty)
              (some-> tool (aget "id") str str/trim not-empty)
              (some-> tool (aget "label") str str/trim not-empty))))

(defn- enabled-tool-name-allowlist
  [builtin-tools custom-tools]
  (->> (concat (or builtin-tools [])
               (if (array? custom-tools) (array-seq custom-tools) []))
       (keep tool-runtime-name)
       distinct
       vec))

(defn- path-resolve
  [^js node-path & parts]
  (case (count parts)
    0 (.resolve node-path)
    1 (.resolve node-path (nth parts 0))
    2 (.resolve node-path (nth parts 0) (nth parts 1))
    3 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2))
    4 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3))
    5 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4))
    6 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4) (nth parts 5))
    7 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4) (nth parts 5) (nth parts 6))
    (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4) (nth parts 5) (nth parts 6) (nth parts 7))))

(defn- path-relative
  [^js node-path from to]
  (.relative node-path from to))

(defn- path-is-absolute?
  [^js node-path value]
  (.isAbsolute node-path value))

(defn- configured-extra-root-records
  [node-path config]
  (let [music-root (some-> (:music-library-root config) str str/trim not-empty)
        extra-roots (->> (or (:extra-workspace-roots config) [])
                         (map (fn [raw]
                                (some-> raw str str/trim not-empty)))
                         (remove nil?))]
    (->> (concat
          (when music-root
            [{:alias "Music"
              :root (path-resolve node-path music-root)}])
          (map (fn [raw-root]
                 {:alias nil
                  :root (path-resolve node-path raw-root)})
               extra-roots))
         (reduce (fn [acc entry]
                   (if (some #(= (:root %) (:root entry)) acc)
                     acc
                     (conj acc entry)))
                 [])
         vec)))

(defn- path-resolve
  [^js node-path & parts]
  (case (count parts)
    0 (.resolve node-path)
    1 (.resolve node-path (nth parts 0))
    2 (.resolve node-path (nth parts 0) (nth parts 1))
    3 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2))
    4 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3))
    5 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4))
    6 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4) (nth parts 5))
    7 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4) (nth parts 5) (nth parts 6))
    (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4) (nth parts 5) (nth parts 6) (nth parts 7))))

(defn- path-relative
  [^js node-path from to]
  (.relative node-path from to))

(defn- path-is-absolute?
  [^js node-path value]
  (.isAbsolute node-path value))

(defn- allowed-root-records
  [node-path config]
  (vec (cons {:alias nil
              :root (path-resolve node-path (:workspace-root config))}
             (configured-extra-root-records node-path config))))

(defn- root-relative-path
  [node-path root candidate]
  (let [rel (path-relative node-path root candidate)]
    (when-not (or (str/starts-with? rel "..")
                  (path-is-absolute? node-path rel))
      rel)))

(defn resolve-workspace-path
  [_runtime config raw-path]
  (let [node-path path
        requested (some-> raw-path str str/trim not-empty)
        roots (allowed-root-records node-path config)
        music-root (some #(when (= "Music" (:alias %)) %) roots)
        candidate (cond
                    (path-is-absolute? node-path (or requested ""))
                    (path-resolve node-path requested)

                    (and requested
                         music-root
                         (or (= requested "Music")
                             (str/starts-with? requested "Music/")))
                    (let [suffix (subs requested (min (count requested) (count "Music/")))]
                      (path-resolve node-path (:root music-root) suffix))

                    :else
                    (path-resolve node-path (:workspace-root config) (or requested "")))
        matched-root (some (fn [root-record]
                             (when (root-relative-path node-path (:root root-record) candidate)
                               root-record))
                           roots)]
    (when-not matched-root
      (throw (js/Error. "Path escapes allowed workspace roots")))
    candidate))

(defn ensure-sdk-runtime!
  [_runtime config]
  (if-let [p @sdk-runtime*]
    p
    (let [node-fs fs
          node-path path
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
                           (-> (.reload loader)
                               (.then (fn []
                                        #js {:authStorage auth-storage
                                             :modelRegistry model-registry
                                             :settingsManager settings-manager
                                             :loader loader
                                             :runtimeDir runtime-dir}))))))
                (.catch (fn [err]
                          (reset! sdk-runtime* nil)
                          (js/Promise.reject err))))]
      (reset! sdk-runtime* p)
      p)))

(defn create-agent-session!
  ([runtime config conversation-id model-id] (create-agent-session! runtime config conversation-id model-id nil (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context] (create-agent-session! runtime config conversation-id model-id auth-context (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context thinking-level]
   (create-agent-session! runtime config conversation-id model-id auth-context thinking-level nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id]
   (create-agent-session! runtime config conversation-id model-id auth-context thinking-level session-id nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id agent-spec]
   (-> (ensure-sdk-runtime! runtime config)
       (.then
        (fn [sdk-runtime]
          (let [SessionManager (aget sdk "SessionManager")
                createAgentSession (aget sdk "createAgentSession")
                model-registry (aget sdk-runtime "modelRegistry")
                auth-storage (aget sdk-runtime "authStorage")
                loader (aget sdk-runtime "loader")
                settings-manager (aget sdk-runtime "settingsManager")
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
                                           :tools (clj->js tool-name-allowlist)
                                           :customTools custom-tools})
                                     (.then (fn [result]
                                              (let [session (aget result "session")]
                                                (.setThinkingLevel session thinking-level)
                                                ;; Wire afterToolCall: inject images from tool results
                                                ;; into the LLM context immediately. When the agent
                                                ;; reads a Discord channel and encounters an image, it
                                                ;; sees it inline — same as a human scrolling Discord.
                                                (when (fn? (some-> session (aget "agent") (aget "setAfterToolCall")))
                                                  (.setAfterToolCall
                                                   (aget session "agent")
                                                   (fn [ctx _signal]
                                                    (let [result     (aget ctx "result")
                                                          details    (when result (aget result "details"))
                                                          raw-parts  (or (when details (aget details "content_parts"))
                                                                         (when details (aget details "contentParts"))
                                                                         #js [])
                                                          img-parts  (->> (array-seq raw-parts)
                                                                          (filter #(= "image" (some-> (aget % "type") str str/lower-case)))
                                                                          vec)
                                                          fetch-b64! (fn [url]
                                                                       (-> (js/fetch url)
                                                                           (.then (fn [r]
                                                                                    (when-not (.-ok r)
                                                                                      (throw (js/Error. (str "img fetch failed: " (.-status r)))))
                                                                                    (.-arrayBuffer r)))
                                                                           (.then (fn [ab]
                                                                                    (let [buf (js/Buffer.from ab)]
                                                                                      (str "data:image/png;base64," (.toString buf "base64")))))))
                                                          materialize! (fn [part]
                                                                         (let [url  (some-> (aget part "url")  str not-empty)
                                                                               data (some-> (aget part "data") str not-empty)
                                                                               mime (or (some-> (aget part "mimeType") str not-empty) "image/png")]
                                                                           (cond
                                                                             (and data (str/starts-with? data "data:"))
                                                                             (js/Promise.resolve
                                                                               (let [comma (.indexOf data ",")]
                                                                                 #js {:type "image"
                                                                                      :data (if (>= comma 0) (.slice data (inc comma)) data)
                                                                                      :mimeType mime}))
                                                                             (and data (not (str/starts-with? data "http")))
                                                                             (js/Promise.resolve #js {:type "image" :data data :mimeType mime})
                                                                             url
                                                                             (-> (fetch-b64! url)
                                                                                 (.then (fn [data-url]
                                                                                          (let [comma (.indexOf data-url ",")]
                                                                                            #js {:type "image"
                                                                                                 :data (if (>= comma 0) (.slice data-url (inc comma)) data-url)
                                                                                                 :mimeType mime}))))
                                                                             :else (js/Promise.resolve nil))))]
                                                      (if (seq img-parts)
                                                        (-> (.all js/Promise (clj->js (mapv materialize! img-parts)))
                                                            (.then (fn [materialized]
                                                                     (let [good (->> (array-seq materialized) (remove nil?) vec)]
                                                                       (when (seq good)
                                                                         (let [existing (or (some-> result (aget "content")) #js [])
                                                                               merged   (clj->js (into (vec (array-seq existing)) good))]
                                                                           #js {:content merged})))))
                                                            (.catch (fn [_] nil)))
                                                        (js/Promise.resolve nil))))))
                                                session)))))]
            (if (no-content? model)
              (js/Promise.reject (js/Error. (str "No eta-mu model configured for " model-id)))
              (let [session-manager (.inMemory SessionManager (:workspace-root config))]
                (when-not (str/blank? (str (or session-id "")))
                  (.newSession session-manager #js {:id (str session-id)}))
                (.appendModelChange session-manager (str model-provider-id) model-id)
                (.appendThinkingLevelChange session-manager thinking-level)
                (-> (rehydrate-session-manager-from-redis! config session-manager conversation-id session-id agent-spec)
                    (.then (fn [result]
                             (create-session (aget result "sessionManager")))))))))))))

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

(defn- register-actor-live-route!
  [runtime conversation-id session-id agent-spec]
  (when-let [actor-id (some-> (:actor-id agent-spec) str str/trim not-empty)]
    (-> (actor-mailbox/register-live-session!
         runtime
         {:actor-id actor-id
          :conversation-id conversation-id
          :session-id session-id
          :contract-id (some-> (:contract-id agent-spec) str str/trim not-empty)
          :source {:registeredBy "agent-runtime"
                   :contractId (:contract-id agent-spec)}})
        (.catch (fn [err]
                  (.warn js/console "[actor-mailbox] failed to register live actor route" (.-message err)))))))

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
     (if-let [entry (get @agent-sessions* conversation-id)]
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
                         (evict-oldest-agent-session!)
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
                     (evict-oldest-agent-session!)
                     (swap! agent-sessions* assoc conversation-id {:session session
                                                                   :model-id model-id
                                                                   :tool-signature current-tool-signature
                                                                   :session-id session-id
                                                                   :actor-id (:actor-id agent-spec)
                                                                   :last-accessed (js/Date.now)})
                     (register-actor-live-route! runtime conversation-id session-id agent-spec)
                     session)))))))

(defn active-agent-session
  [conversation-id]
  (:session (get @agent-sessions* conversation-id)))

(defn remove-agent-session!
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

(defn queue-agent-control!
  [_runtime _config {:keys [conversation-id session-id run-id message kind metadata]}]
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
              metadata (or metadata {})
              invoke (if (= kind "follow_up")
                       #(.followUp session message)
                       #(.steer session message))]
          (-> (invoke)
              (.then (fn []
                       (let [event (tool-event-payload run-id conversation-id session-id event-type
                                                       {:status "queued"
                                                        :preview preview
                                                        :metadata metadata})]
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
                                                         :preview preview
                                                         :metadata metadata})]
                          (when run-id
                            (append-run-event! run-id event))
                          (broadcast-ws-session! session-id "events" event))
                        (throw err))))))
      (js/Promise.reject (js/Error. "Conversation is not active in the agent runtime")))))
