(ns knoxx.backend.infra.agent.session
  (:require [clojure.string :as str]
            [knoxx.backend.domain.bluesky.bluesky :as bluesky]
            [knoxx.backend.domain.models :refer [normalize-thinking-level effective-thinking-level resolve-model-contract]]
            [knoxx.backend.extern.eta-mu :as eta-mu-extern]
            [knoxx.backend.extern.extension :as extension-extern]
            [knoxx.backend.extern.platform-action :as platform-action]
            [knoxx.backend.extern.tools :as xtools]
            [knoxx.backend.domain.social.action-admission :as action-domain]
            [knoxx.backend.infra.actor.acting :as acting]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.agent.action-admission :as action-admission]
            [knoxx.backend.infra.agent.social-delivery :as social-delivery]
            [knoxx.backend.infra.agent.tool-context :as tool-context]
            [knoxx.backend.infra.execution.client :as execution]
            [knoxx.backend.infra.agent.content-codec :as content-codec]
            [knoxx.backend.infra.agent.history :as history]
            [knoxx.backend.infra.agent.provider.eta-mu :as eta-mu-provider]
            [knoxx.backend.infra.agent.provider.native-subscription :as native-subscription]
            [knoxx.backend.infra.agent.session-registry :as session-registry]
            [knoxx.backend.infra.agent.tool-catalog :as tool-catalog]
            [knoxx.backend.infra.http :refer [no-content?]]
            [knoxx.backend.infra.stores.composite-message-source :refer [->CompositeMessageSource]]
            [knoxx.backend.infra.stores.openplanner-message-source :refer [->OpenPlannerMessageSource]]
            [knoxx.backend.infra.stores.mongo-message-source :refer [->MongoMessageSource]]
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

(defn- evidence-guard
  [tool-name context]
  (case tool-name
    ("discord_send" "discord_publish") (get-in context [:delivery-guards :discord])
    "bluesky_publish" (get-in context [:delivery-guards :bluesky])
    "bluesky_repost" (some-> (get-in context [:delivery-guards :bluesky]) meta :amplify!)
    nil))

(defn- ^:async execute-platform-write!
  [execute args admission reservation]
  (let [execution (await (platform-action/execute! #(apply execute args)))
        outcome ((:settle! admission) reservation execution)]
    (if-let [error (:error execution)]
      (throw (platform-action/failure-with-outcome error outcome))
      (platform-action/result-with-outcome (:value execution) outcome))))

(defn- capture-follow-run!
  [context]
  (when-not (:platform-admission context)
    (throw (ex-info "Platform writes require an admitted actor-bound turn"
                    {:code "platform_action_context_required" :phase :preflight})))
  (:run_id (social-delivery/captured-run!
            {:conversation-id (:conversation-id context) :actor-id (get-in context [:agent-spec :actor-id])}
            "Bluesky follow")))

(defn- ^:async canonical-follow-params!
  [params context]
  (let [input (:actor params)
        resolve! (:resolve-bluesky-actor! context)
        did (or (action-domain/canonical-follow-target input)
                (when (and (string? input) (not (str/blank? input)) resolve!)
                  (action-domain/canonical-follow-target (:did (await (resolve! input))))))]
    (when-not did
      (throw (ex-info "Bluesky follow needs a verified destination DID"
                      {:code "platform_follow_target_unresolved" :phase :preflight})))
    (assoc params :actor did)))

(defn- ^:async invoke-context-tool!
  [execute args tool-name context]
  (let [original-params (xtools/parameter-map (second args))
        params (if (contains? #{"bluesky.follow" "bluesky_follow"} tool-name)
                 (let [run-id (capture-follow-run! context)
                       canonical (await (canonical-follow-params! original-params context))]
                   (when-not (= run-id (capture-follow-run! context))
                     (throw (ex-info "The original turn ended before the follow could be admitted"
                                     {:code "platform_action_run_changed" :phase :preflight})))
                   canonical)
                 original-params)
        args (if (= original-params params) args (xtools/replace-parameters args params))
        intent (action-domain/native-intent tool-name params (get-in context [:agent-spec :actor-id]))
        guard (evidence-guard tool-name context)
        guard-params (cond
                       (= "bluesky_repost" tool-name) {:text "" :replyTo (:uri params)}
                       (and (= "discord" (:platform intent)) (= :publish (:effect intent)))
                       (assoc params :channel_id (:target intent) :text (get-in intent [:arguments :text]))
                       :else params)]
    (when guard (guard guard-params))
    (if intent
      (let [admission (:platform-admission context)
            _ (when-not admission
                (throw (ex-info "Platform actions require an actor-bound conversation"
                                {:code "platform_action_context_required"})))
            reservation ((:reserve! admission) intent)]
        (await (execute-platform-write! execute args admission reservation)))
      (await (apply execute args)))))

(defn- tool-acting-scope
  [context]
  (let [actor-id (get-in context [:agent-spec :actor-id])
        inherited (when (= actor-id (acting/current-actor-id)) (acting/current-lookup-scope))
        declared (:acting-scope context)]
    (assoc (merge inherited (when (= actor-id (:actor-id declared)) declared)) :actor-id actor-id)))

(defn- wrap-tool-execute-with-agent-context!
  [tool context]
  (when-let [execute (eta-mu-extern/tool-execute tool)]
    (let [name (eta-mu-extern/tool-runtime-name tool)]
      (eta-mu-extern/set-tool-execute!
       tool
       (^:async fn [& args]
         (let [previous (agent-context/get-context)]
           (agent-context/set-context! context)
           (try
             (await (acting/run-as! (tool-acting-scope context)
                                    (fn []
                                      (if (:platform-admission context)
                                        (tool-context/run-with! context #(invoke-context-tool! execute args name context))
                                        (invoke-context-tool! execute args name context)))))
             (finally (restore-agent-context! previous))))))))
  tool)

(defn wrap-custom-tools-with-agent-context!
  "Bind one trusted context, shared action admission, and shared evidence guards
   to the tool suite; native aliases cannot acquire independent reservations."
  [custom-tools context]
  (when custom-tools
    (let [policy (:delivery-policy context)
          captured {:conversation-id (:conversation-id context)
                    :actor-id (get-in context [:agent-spec :actor-id])}
          guard-context (assoc captured :policy policy)
          context (if (and (seq (:actor-id captured)) (seq (:conversation-id captured)))
                    (assoc context :platform-admission (action-admission/create captured)
                           :delivery-guards {:discord (when policy (social-delivery/make-guard guard-context))
                                             :bluesky (when (:bluesky-publish? policy)
                                                        (social-delivery/make-bluesky-guard guard-context))})
                    context)]
      (doseq [tool (eta-mu-extern/tool-seq custom-tools)]
        (wrap-tool-execute-with-agent-context! tool context))))
  custom-tools)

(defn prune-session-messages
  [agent-spec messages]
  (history/prune-session-messages agent-spec messages))

(defn- ^:async register-actor-live-route!
  [runtime conversation-id session-id agent-spec]
  (when-let [actor-id (some-> (:actor-id agent-spec) str str/trim not-empty)]
    (try
      (await (actor-mailbox/register-live-session!
              runtime
              {:actor-id actor-id
               :conversation-id conversation-id
               :session-id session-id
               :contract-id (some-> (:contract-id agent-spec) str str/trim not-empty)
               :source {:registeredBy "agent-runtime"
                        :contractId (:contract-id agent-spec)}}))
      (catch :default err
        (.warn js/console "[actor-mailbox] failed to register live actor route" (.-message err))))))

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

(defn- session-provider-tools
  [runtime config tool-auth-context agent-spec allowed-tool-ids _model-id session-id conversation-id]
  (let [builtin-tools (tool-catalog/builtin-tools runtime config tool-auth-context agent-spec)
        custom-tools (wrap-custom-tools-with-agent-context!
                      (tool-catalog/custom-tools runtime
                                                 (assoc config :graph-actor-id (:actor-id agent-spec)
                                                               :discord-self-message-context (get-in agent-spec [:context-policy :self-messages])
                                                               :memory-search-exclude-session-ids
                                                        (vec (remove str/blank? [session-id conversation-id])))
                                                 tool-auth-context agent-spec allowed-tool-ids)
                      {:session-id session-id
                       :conversation-id conversation-id
                       :acting-scope (when (= (:actor-id agent-spec) (authz/ctx-actor-binding tool-auth-context))
                                       {:actor-id (:actor-id agent-spec)
                                        :org-id (authz/ctx-org-id tool-auth-context)
                                        :membership-id (authz/ctx-membership-id tool-auth-context)})
                       :delivery-policy (tool-catalog/social-delivery-policy config agent-spec)
                       :resolve-bluesky-actor! (partial bluesky/bluesky-profile! runtime)
                       :agent-spec agent-spec})]
    {:custom-tools custom-tools
     :tool-name-allowlist (tool-catalog/tool-runtime-names builtin-tools custom-tools)}))

(defn ^:async create-session-manager!
  ([runtime config conversation-id model-id] (create-session-manager! runtime config conversation-id model-id nil (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context] (create-session-manager! runtime config conversation-id model-id auth-context (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context thinking-level]
   (create-session-manager! runtime config conversation-id model-id auth-context thinking-level nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id]
   (create-session-manager! runtime config conversation-id model-id auth-context thinking-level session-id nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id agent-spec]
   (if-let [selection (native-subscription/resolve-worker! config auth-context model-id agent-spec)]
     (native-subscription/create-session selection (:system-prompt agent-spec))
     (let [effective-config (execution/effective-config config agent-spec)
           config (await (execution/workspace-config! effective-config auth-context (:actor-id agent-spec)))
           {:keys [auth-storage model-registry settings-manager loader runtime-dir]} (await (ensure-eta-mu-runtime! runtime config))
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
            {:keys [custom-tools tool-name-allowlist]} (session-provider-tools runtime config tool-auth-context agent-spec
                                                                               allowed-tool-ids model-id session-id conversation-id)
            preferred-session-id (some-> session-id str str/trim not-empty)
            message-source (->CompositeMessageSource
                             (->OpenPlannerMessageSource config)
                            (->MongoMessageSource preferred-session-id))]
       (if (no-content? model)
         (js/Promise.reject (js/Error. (str "No eta-mu model configured for " model-id)))
         (let [session-manager (eta-mu-extern/make-session-manager! (:workspace-root config) preferred-session-id)]
           (eta-mu-extern/append-model-change! session-manager model-provider-id model-id)
           (eta-mu-extern/append-thinking-level-change! session-manager thinking-level)
           (let [{:keys [session-manager]} (await (rehydrate-session-manager! message-source session-manager conversation-id agent-spec))
                 session (await (eta-mu-provider/create-session!
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
                                  :system-prompt (:system-prompt agent-spec)
                                  :tools-choice (:tools-choice agent-spec)
                                  :materialize! materialize!}))]
             (set-thinking-level! session thinking-level)
             session)))))))

(defn- claim-startup-session!
  "Join a pending construction; established sessions never become startup-owned."
  [conversation-id session owner]
  (swap! sessions*
         (fn [entries]
           (let [entry (get entries conversation-id)]
             (if (and (identical? session (:session entry)) (::startup-owners entry))
               (assoc entries conversation-id
                      (if owner (update entry ::startup-owners conj owner)
                          (dissoc entry ::startup-owners)))
               entries)))))

(defn- shutdown-session-entry! [conversation-id entry]
  (let [ctx (ext-runtime/build-extension-ctx
             (extension-extern/empty-event-payload) {}
             :conversation-id conversation-id :session-id (:session-id entry))]
    (ext-runtime/dispatch-event "session_shutdown"
                               (extension-extern/event-payload {:conversationId conversation-id}) ctx)))

(defn settle-startup-session!
  "Promote a successful construction or release only this pending startup claim.
   The last failed claimant removes an unadmitted construction. A replacement,
   established session or another pending claimant is never removed."
  [conversation-id owner admitted?]
  (loop []
    (let [before @sessions* entry (get before conversation-id) owners (::startup-owners entry)]
      (when (and owner (contains? owners owner))
        (let [remaining (disj owners owner)
              next-entry (cond admitted? (dissoc entry ::startup-owners)
                               (seq remaining) (assoc entry ::startup-owners remaining))
              after (if next-entry (assoc before conversation-id next-entry) (dissoc before conversation-id))]
          (if (compare-and-set! sessions* before after)
            (when-not next-entry (shutdown-session-entry! conversation-id entry))
            (recur))))))
  nil)

(defn ^:async construct-session-and-ext-ctx!
  ([runtime config conversation-id model-id auth-context thinking-level session-id agent-spec current-tool-signature life-cycle-event-name]
   (construct-session-and-ext-ctx! runtime config conversation-id model-id auth-context thinking-level
                                   session-id agent-spec current-tool-signature life-cycle-event-name nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id agent-spec current-tool-signature life-cycle-event-name owner]
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
                                          (cond-> {:session next-session
                                           :model-id model-id
                                           :tool-signature current-tool-signature
                                           :session-id session-id
                                           :actor-id (:actor-id agent-spec)}
                                            owner (assoc ::startup-owners #{owner})))
    (register-actor-live-route! runtime conversation-id session-id agent-spec)
    next-session)))

(defn- reuse-session! [runtime conversation-id session-id agent-spec session thinking-level owner]
  (claim-startup-session! conversation-id session owner)
  (set-thinking-level! session thinking-level)
  (register-actor-live-route! runtime conversation-id session-id agent-spec)
  (js/Promise.resolve session))

(defn ensure-agent-session!
  ([runtime config conversation-id model-id] (ensure-agent-session! runtime config conversation-id model-id nil (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context] (ensure-agent-session! runtime config conversation-id model-id auth-context (:agent-thinking-level config)))
  ([runtime config conversation-id model-id auth-context thinking-level]
   (ensure-agent-session! runtime config conversation-id model-id auth-context thinking-level nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id]
   (ensure-agent-session! runtime config conversation-id model-id auth-context thinking-level session-id nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id agent-spec]
   (ensure-agent-session! runtime config conversation-id model-id auth-context thinking-level session-id agent-spec nil))
  ([runtime config conversation-id model-id auth-context thinking-level session-id agent-spec owner]
   (let [thinking-level (effective-thinking-level config model-id (or (normalize-thinking-level thinking-level)
                                                                      thinking-level
                                                                      (:agent-thinking-level config)
                                                                      "off"))
         native-selection (native-subscription/resolve-worker! config auth-context model-id agent-spec)
         current-tool-signature (if native-selection
                                  [session-id (:system-prompt agent-spec) (:native-subscription-owner config) native-selection]
                                  (visible-session-signature runtime config auth-context agent-spec))
         construct-this-session! (partial construct-session-and-ext-ctx! runtime config conversation-id model-id
                                          auth-context thinking-level session-id agent-spec current-tool-signature)]
     (if-let [entry (active-session-entry conversation-id)]
       (let [session (:session entry)
             active-model (:model-id entry)
             active-tool-signature (:tool-signature entry)]
         (if (and (some? session)
                  (= (str active-model) (str model-id))
                  (= (str (or active-tool-signature "")) (str (or current-tool-signature ""))))
           (reuse-session! runtime conversation-id session-id agent-spec session thinking-level owner)
           (construct-this-session! "session_switch" owner)))
       (construct-this-session! "session_start" owner)))))

(defn remove-agent-session!
  "Dispatch session_shutdown to extensions, then release the in-process session entry."
  [conversation-id]
  (when-let [entry (active-session-entry conversation-id)]
    (shutdown-session-entry! conversation-id entry))
  (session-registry/remove-active-session! active-session-registry conversation-id)
  nil)
