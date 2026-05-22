(ns knoxx.backend.infra.agent.session
  (:require [clojure.string :as str]
            [knoxx.backend.domain.models :refer [normalize-thinking-level effective-thinking-level resolve-model-contract]]
            [knoxx.backend.extern.eta-mu :as eta-mu-extern]
            [knoxx.backend.extern.extension :as extension-extern]
            [knoxx.backend.infra.agent.content-codec :as content-codec]
            [knoxx.backend.infra.agent.history :as history]
            [knoxx.backend.infra.agent.provider.eta-mu :as eta-mu-provider]
            [knoxx.backend.infra.agent.session-registry :as session-registry]
            [knoxx.backend.infra.agent.tool-catalog :as tool-catalog]
            [knoxx.backend.infra.http :refer [no-content?]]
            [knoxx.backend.infra.stores.composite-message-source :refer [->CompositeMessageSource]]
            [knoxx.backend.infra.stores.openplanner-message-source :refer [->OpenPlannerMessageSource]]
            [knoxx.backend.infra.stores.redis-message-source :refer [->RedisMessageSource]]
            [knoxx.backend.domain.extension-runtime :as ext-runtime]
            [knoxx.backend.domain.actor.mailbox :as actor-mailbox]
            [knoxx.backend.domain.agent.agent-context :as agent-context]
            [knoxx.backend.shape.agent :refer [set-thinking-level!]]))

(defonce sessions* (atom {}))

(def ^:private inactive-ttl-ms session-registry/default-inactive-ttl-ms)
(def ^:private sweep-interval-ms 300000)
(def ^:private active-session-registry
  (session-registry/atom-registry sessions* {:inactive-ttl-ms inactive-ttl-ms}))

;; ─── Private helpers ────────────────────────────────────────────────────────


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

(defn prune-session-messages
  [agent-spec messages]
  (history/prune-session-messages agent-spec messages))

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
  (session-registry/get-active-session active-session-registry conversation-id))

(defn- active-session-entry
  [conversation-id]
  (session-registry/get-active-session-entry active-session-registry conversation-id))

(defn- start-sweep!
  []
  (js/setInterval
   (fn []
     (session-registry/sweep-expired-sessions! active-session-registry (js/Date.now)))
   sweep-interval-ms))

(start-sweep!)

;; ─── Media helpers ───────────────────────────────────────────────────────────

(defn fetch-b64!
  [url media-type]
  (content-codec/fetch-b64! url media-type))

(defn materialize!
  [part]
  (content-codec/materialize! part))

;; ─── Session hydration ──────────────────────────────────────────────────────

(defn ^:async rehydrate-session-manager!
  [message-source session-manager conversation-id agent-spec]
  (await (history/rehydrate-session-manager! message-source session-manager conversation-id agent-spec)))

;; ─── Runtime setup ───────────────────────────────────────────────────────────

(defn ^:async ensure-eta-mu-runtime!
  [runtime config]
  (await (eta-mu-provider/ensure-runtime! (eta-mu-provider/eta-mu-provider runtime config))))

;; ─── Session creation ────────────────────────────────────────────────────────

(defn- visible-session-signature
  [runtime config auth-context agent-spec]
  (tool-catalog/visible-session-signature runtime config auth-context agent-spec))

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
         provider (eta-mu-provider/eta-mu-provider runtime config)
         model (eta-mu-provider/resolve-model provider
                                              model-registry
                                              model-provider-id
                                              model-id
                                              (:proxx-default-model config))
         allowed-tool-ids (tool-catalog/allowed-tool-ids config auth-context agent-spec)
         tool-auth-context (tool-catalog/effective-tool-auth-context auth-context allowed-tool-ids)
         builtin-tools (tool-catalog/builtin-tools runtime config tool-auth-context agent-spec)
         custom-tools (wrap-custom-tools-with-agent-context!
                       (tool-catalog/custom-tools runtime config tool-auth-context agent-spec allowed-tool-ids)
                       {:session-id session-id
                        :conversation-id conversation-id
                        :agent-spec agent-spec})
         tool-name-allowlist (tool-catalog/tool-runtime-names builtin-tools custom-tools)
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
                      (let [session (eta-mu-provider/create-session!
                                     provider
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
                                (extension-extern/event-payload {:conversationId conversation-id
                                                                 :sessionId session-id})
                                ctx)
    (session-registry/put-active-session! active-session-registry
                                          conversation-id
                                          {:session next-session
                                           :model-id model-id
                                           :tool-signature current-tool-signature
                                           :session-id session-id
                                           :actor-id (:actor-id agent-spec)})
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
     (if-let [entry (active-session-entry conversation-id)]
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
  (when-let [entry (active-session-entry conversation-id)]
    (let [ctx (ext-runtime/build-extension-ctx
               (extension-extern/empty-event-payload) {}
               :conversation-id conversation-id
               :session-id (:session-id entry))]
      (ext-runtime/dispatch-event "session_shutdown"
                                  (extension-extern/event-payload {:conversationId conversation-id})
                                  ctx)))
  (session-registry/remove-active-session! active-session-registry conversation-id)
  nil)
