(ns knoxx.backend.infra.agent.session
  (:require [clojure.string :as str]
            [knoxx.backend.domain.models :refer [normalize-thinking-level effective-thinking-level models-config resolve-model-contract]]
            [knoxx.backend.extern.eta-mu :as eta-mu-extern]
            [knoxx.backend.extern.js :as xjs]
            [knoxx.backend.infra.agent.hydration :refer [create-agent-custom-tools]]
            [knoxx.backend.infra.agent.message :as msg]
            [knoxx.backend.infra.agent.provider :refer [fetch-proxx-model-ids!]]
            [knoxx.backend.infra.http :as http :refer [no-content?]]
            [knoxx.backend.infra.stores.composite-message-source :refer [->CompositeMessageSource]]
            [knoxx.backend.infra.stores.message-source :refer [fetch-messages!]]
            [knoxx.backend.infra.stores.openplanner-message-source :refer [->OpenPlannerMessageSource]]
            [knoxx.backend.infra.stores.redis-message-source :refer [->RedisMessageSource]]
            [knoxx.backend.infra.tooling :refer [allowed-tool-id-set create-runtime-tools]]
            [knoxx.backend.domain.extension-runtime :as ext-runtime]
            [knoxx.backend.domain.actor.mailbox :as actor-mailbox]
            [knoxx.backend.domain.agent.agent-context :as agent-context]
            [knoxx.backend.shape.agent :refer [set-thinking-level!]]))

(defonce sessions* (atom {}))

(def ^:private max-sessions 500)
(def ^:private inactive-ttl-ms (* 4 60 60 1000))
(def ^:private sweep-interval-ms 300000)

;; ─── Private helpers ────────────────────────────────────────────────────────

(defn- positive-int-value [v] (when (integer? v) (max v 0)))

(defn- message-text-size
  [message]
  (+ (count (str (or (:content message) (:summary message) "")))
     (reduce + 0 (map #(count (str (or (:text %) (:filename %) (:url %) "")))
                      (or (:content-parts message) (:contentParts message) [])))))

(defn- enabled-tool-name-allowlist
  [builtin-tools custom-tools]
  (->> (concat (or builtin-tools [])
               (eta-mu-extern/tool-seq custom-tools))
       (keep eta-mu-extern/tool-runtime-name)
       distinct
       vec))

(defn- restore-agent-context!
  [previous]
  (if previous
    (agent-context/set-context! previous)
    (agent-context/clear-context!)))

(defn- wrap-tool-execute-with-agent-context!
  [tool context]
  (when-let [execute (eta-mu-extern/tool-execute tool)]
    (eta-mu-extern/set-tool-execute!
     tool
     (fn [& args]
       (let [previous (agent-context/get-context)]
         (agent-context/set-context! context)
         (try
           (eta-mu-extern/with-promise-finally
            (apply execute args)
            (fn [] (restore-agent-context! previous)))
           (catch :default err
             (restore-agent-context! previous)
             (throw err)))))))
  tool)

(defn- wrap-custom-tools-with-agent-context!
  [custom-tools context]
  (when custom-tools
    (doseq [tool (eta-mu-extern/tool-seq custom-tools)]
      (wrap-tool-execute-with-agent-context! tool context)))
  custom-tools)

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

(defn- context-policy
  [agent-spec]
  (or (:context-policy agent-spec)
      (:contextPolicy agent-spec)
      (:context agent-spec)
      (get-in agent-spec [:extras :context])
      (get-in agent-spec [:extras :context-policy])))

(defn prune-session-messages
  [agent-spec messages]
  (let [items (vec (or messages []))
        policy (context-policy agent-spec)]
    (if-not policy
      items
      (let [max-messages (positive-int-value (or (:max-messages policy) (:maxMessages policy) (:max_messages policy)))
            max-chars (positive-int-value (or (:max-chars policy) (:maxChars policy) (:max_chars policy)))
            preserve-system? (not= false (or (:preserve-system policy) (:preserveSystem policy) (:preserve_system policy)))
            system-messages (if preserve-system?
                              (filterv #(= "system" (some-> (:role %) str str/lower-case)) items)
                              [])
            body-messages (if preserve-system?
                            (remove #(= "system" (some-> (:role %) str str/lower-case)) items)
                            items)
            by-count (if max-messages (take-last max-messages (vec body-messages)) (vec body-messages))
            by-chars (if max-chars
                       (loop [remaining (reverse by-count) total 0 kept '()]
                         (if-let [message (first remaining)]
                           (let [size (message-text-size message)]
                             (if (and (seq kept) (> (+ total size) max-chars))
                               (vec kept)
                               (recur (rest remaining) (+ total size) (conj kept message))))
                           (vec kept)))
                       (vec by-count))]
        (vec (concat system-messages by-chars))))))

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

;; ─── Session registry ────────────────────────────────────────────────────────

(defn active-agent-session
  [conversation-id]
  (:session (get @sessions* conversation-id)))

(defn- evict-oldest!
  []
  (when (> (count @sessions*) max-sessions)
    (let [oldest (apply min-key (comp :last-accessed val) @sessions*)]
      (when oldest
        (swap! sessions* dissoc (key oldest))))))

(defn- start-sweep!
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

(start-sweep!)

;; ─── Media helpers ───────────────────────────────────────────────────────────

(defn fetch-b64!
  [url media-type]
  (-> (js/fetch url)
      (.then (fn [r]
               (when-not (.-ok r)
                 (throw (js/Error. (str media-type " fetch failed: " (.-status r)))))
               (.-arrayBuffer r)))
      (.then (fn [ab]
               (let [buf (js/Buffer.from ab)]
                 (str "data:" media-type ";base64," (.toString buf "base64")))))))

(defn- audio-format [mime] (msg/mime->audio-format mime))

(defn- media-map
  [part-type data mime]
  (cond-> {:type part-type
           :data data
           :mimeType mime}
    (= "audio" part-type) (assoc :format (or (audio-format mime) "mp3"))))

(defn materialize!
  [part]
  (let [part-type (some-> (:type part) str str/lower-case)
        url       (some-> (:url part) str not-empty)
        data      (some-> (:data part) str not-empty)
        mime      (or (some-> (:mimeType part) str not-empty)
                      (if (= "audio" part-type) "audio/mpeg" "image/png"))]
    (cond
      (and data (str/starts-with? data "data:"))
      (js/Promise.resolve
       (let [comma (.indexOf data ",")]
         (media-map part-type (if (>= comma 0) (.slice data (inc comma)) data) mime)))

      (and data (not (str/starts-with? data "http")))
      (js/Promise.resolve (media-map part-type data mime))

      url
      (-> (fetch-b64! url mime)
          (.then (fn [data-url]
                   (let [comma (.indexOf data-url ",")]
                     (media-map part-type (if (>= comma 0) (.slice data-url (inc comma)) data-url) mime)))))

      :else (js/Promise.resolve nil))))

;; ─── Session hydration ──────────────────────────────────────────────────────

(defn ^:async rehydrate-session-manager!
  [message-source session-manager conversation-id agent-spec]
  (let [messages (await (fetch-messages! message-source conversation-id))
        merged-messages (-> (vec (or messages []))
                            (msg/sync-system-message (:system-prompt agent-spec))
                            (#(prune-session-messages agent-spec %)))]
    (doseq [message merged-messages]
      (when-let [agent-message (msg/stored-session-message->agent-message message)]
        (eta-mu-extern/append-message! session-manager agent-message)))
    {:session-manager session-manager
     :restored (boolean (seq merged-messages))}))

;; ─── Runtime setup ───────────────────────────────────────────────────────────

(defn ^:async ensure-eta-mu-runtime!
  [_runtime config]
  (eta-mu-extern/setup-runtime!
   config
   (models-config config (await (fetch-proxx-model-ids! config)))
   (compaction-settings config)))

;; ─── Session creation ────────────────────────────────────────────────────────

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
                       (eta-mu-extern/tool-seq tools)
                       [])]
    (pr-str {:tools (->> (concat builtin-tools custom-tools)
                         (keep eta-mu-extern/tool-runtime-name)
                         sort
                         distinct
                         vec)
             :contract-id (some-> (:contract-id agent-spec) str str/trim not-empty)
             :actor-id (some-> (:actor-id agent-spec) str str/trim not-empty)
             :role (some-> (:role agent-spec) str str/trim not-empty)
             :system-prompt (some-> (:system-prompt agent-spec) str str/trim not-empty)
             :task-prompt (some-> (:task-prompt agent-spec) str str/trim not-empty)})))

(defn ^:async create-session-manager!
  ([runtime config conversation-id model-id] (create-session-manager! runtime config conversation-id model-id nil (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context] (create-session-manager! runtime config conversation-id model-id auth-context (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context thinking-level]
   (create-session-manager! runtime config conversation-id model-id auth-context thinking-level nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id]
   (create-session-manager! runtime config conversation-id model-id auth-context thinking-level session-id nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id agent-spec]
   (let [{:keys [auth-storage model-registry settings-manager loader runtime-dir]} (await (ensure-eta-mu-runtime! runtime config))
         thinking-level (effective-thinking-level config model-id (or (normalize-thinking-level thinking-level)
                                                                      thinking-level
                                                                      (:agent-thinking-level config)
                                                                      "off"))
         model-provider-id (or (some-> (resolve-model-contract config model-id) :provider)
                               "proxx")
         model (eta-mu-extern/find-model model-registry
                                         model-provider-id
                                         model-id
                                         (:proxx-default-model config))
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
         preferred-session-id (some-> session-id str str/trim not-empty)
         message-source (->CompositeMessageSource
                          (->OpenPlannerMessageSource config)
                          (->RedisMessageSource preferred-session-id))]
     (if (no-content? model)
       (js/Promise.reject (js/Error. (str "No eta-mu model configured for " model-id)))
       (let [session-manager (eta-mu-extern/make-session-manager! (:workspace-root config) preferred-session-id)]
         (eta-mu-extern/append-model-change! session-manager model-provider-id model-id)
         (eta-mu-extern/append-thinking-level-change! session-manager thinking-level)
         (-> (rehydrate-session-manager! message-source session-manager conversation-id agent-spec)
             (.then (fn [{:keys [session-manager]}]
                      (let [session (eta-mu-extern/create-session!
                                     {:workspace-root (:workspace-root config)
                                      :runtime-dir runtime-dir
                                      :auth-storage auth-storage
                                      :model-registry model-registry
                                      :loader loader
                                      :settings-manager settings-manager
                                      :session-manager session-manager
                                      :model model
                                      :thinking-level thinking-level
                                      :tool-name-allowlist tool-name-allowlist
                                      :custom-tools custom-tools
                                      :materialize! materialize!})]
                        (set-thinking-level! session thinking-level)
                        session)))))))))

(defn ^:async construct-session-and-ext-ctx!
  [runtime config conversation-id model-id auth-context thinking-level session-id agent-spec current-tool-signature life-cycle-event-name]
  (let [next-session (await (create-session-manager! runtime config conversation-id model-id auth-context thinking-level session-id agent-spec))
        ctx (ext-runtime/build-extension-ctx runtime config
                                             :conversation-id conversation-id
                                             :session-id session-id
                                             :model-id model-id
                                             :auth-context auth-context)]
    (ext-runtime/dispatch-event life-cycle-event-name
                                (xjs/object {:conversationId conversation-id
                                             :sessionId session-id})
                                ctx)
    (evict-oldest!)
    (swap! sessions* assoc conversation-id {:session next-session
                                            :model-id model-id
                                            :tool-signature current-tool-signature
                                            :session-id session-id
                                            :actor-id (:actor-id agent-spec)
                                            :last-accessed (js/Date.now)})
    (register-actor-live-route! runtime conversation-id session-id agent-spec)
    next-session))

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
         current-tool-signature (visible-session-signature runtime config auth-context agent-spec)
         construct-this-session! (partial construct-session-and-ext-ctx! runtime config conversation-id model-id
                                          auth-context thinking-level session-id agent-spec current-tool-signature)]
     (if-let [entry (get @sessions* conversation-id)]
       (let [session (:session entry)
             active-model (:model-id entry)
             active-tool-signature (:tool-signature entry)]
         (if (and (some? session)
                  (= (str active-model) (str model-id))
                  (= (str (or active-tool-signature "")) (str (or current-tool-signature ""))))
           (do
             (set-thinking-level! session thinking-level)
             (register-actor-live-route! runtime conversation-id session-id agent-spec)
             (js/Promise.resolve session))
           (construct-this-session! "session_switch")))
       (construct-this-session! "session_start")))))

(defn remove-agent-session!
  "Dispatch session_shutdown to extensions, then release the in-process session entry."
  [conversation-id]
  (when-let [entry (get @sessions* conversation-id)]
    (let [ctx (ext-runtime/build-extension-ctx
               (xjs/empty-object) {}
               :conversation-id conversation-id
               :session-id (:session-id entry))]
      (ext-runtime/dispatch-event "session_shutdown"
                                  (xjs/object {:conversationId conversation-id})
                                  ctx)))
  (swap! sessions* dissoc conversation-id)
  nil)
