(ns knoxx.backend.infra.agent.recovery
  "Session recovery after restarts: resume or abort stale runs."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.agent-recovery :as native]
            [knoxx.backend.extern.agent-turn-node :as host]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.infra.agent.startup-settlement :as startup]
            [knoxx.backend.infra.agent.session :as sessions :refer [ensure-agent-session!]]
            [knoxx.backend.shape.agent :as agent]
            [knoxx.backend.infra.agent.turn :as turn]
            [knoxx.backend.infra.auth.authz :refer [auth-snapshot-has-principal?]]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]
            [knoxx.backend.domain.voice.turn-control :as turn-control]))

(defn recovered-auth-context
  "Restore the persisted authorization snapshot after provider admission."
  [session]
  {:orgId (:org_id session)
   :orgSlug (:org_slug session)
   :userId (:user_id session)
   :userEmail (:user_email session)
   :membershipId (:membership_id session)
   :actorId (:actor_id session)
   :roleSlugs (vec (or (:role_slugs session) []))
   :permissions (vec (or (:permissions session) []))
   :toolPolicies (vec (or (:tool_policies session) []))
   :membershipToolPolicies (vec (or (:membership_tool_policies session) []))
   :isSystemAdmin (boolean (:is_system_admin session))})

(defn recovered-agent-spec
  "Normalize the historical persisted agent specification."
  [session]
  (when-let [agent-spec (or (:agent_spec session)
                            (:agent-spec session)
                            (:agentSpec session))]
    (let [tools-choice-value (or (:tools-choice agent-spec)
                                 (:tools_choice agent-spec)
                                 (:toolsChoice agent-spec)
                                 (:tools/choice agent-spec))
          tools-choice (some-> (if (keyword? tools-choice-value)
                                 (name tools-choice-value)
                                 tools-choice-value)
                               str
                               str/trim
                               not-empty)]
      (cond-> agent-spec
        tools-choice (assoc :tools-choice tools-choice)))))

(defn restored-conversation-access!
  "Restore conversation access only after the observed owner was admitted for recovery."
  [session]
  (let [conversation-id (str (or (:conversation_id session) ""))
        snapshot (select-keys session [:org_id
                                       :org_slug
                                       :user_id
                                       :user_email
                                       :membership_id
                                       :actor_id
                                       :role_slugs
                                       :permissions
                                       :tool_policies
                                       :membership_tool_policies
                                       :is_system_admin])]
    (when (and (not (str/blank? conversation-id))
               (auth-snapshot-has-principal? snapshot))
      (swap! turn/conversation-access* assoc conversation-id snapshot))))

(defn last-session-user-message
  "Find the pending user message preserved by the recovered transcript."
  [session]
  (some (fn [message]
          (let [role (some-> (:role message) str str/lower-case)
                content (some-> (:content message) str)]
            (when (and (= role "user")
                       (not (str/blank? content)))
              content)))
        (reverse (vec (or (:messages session) [])))))

(defn- recovery-result [session extras]
  (merge {:session_id (:session_id session) :conversation_id (:conversation_id session)} extras))

(defn- resume-request [session config]
  {:conversation-id (:conversation_id session)
   :session-id (:session_id session)
   ;; A recovered turn is a new attempt. Neither old run ID nor old token grants admission.
   :run-id (host/random-uuid!)
   :message (last-session-user-message session)
   :model (:model session) :mode (or (:mode session) "direct")
   :thinking-level (or (:thinking_level session) (:agent-thinking-level config) "off")
   :auth-context (recovered-auth-context session) :agent-spec (recovered-agent-spec session)})

(defn- ^:async launch-recovery! [runtime config session request wait-for]
  (let [launch (turn/send-agent-turn! runtime (dissoc config startup/reservation-key) request)]
    (if (= wait-for :kickoff)
      (do
        (native/observe-launch! launch (:session_id session) (:conversation_id session))
        (await (native/wait-for-kickoff! #(= (:run-id request) (:run_id (turn-control/active-turn (:conversation_id session)))) launch))
        (recovery-result session {:resumed true :wait_for "kickoff"
                                  :run_id (:run-id request) :previous_run_id (:run_id session)}))
      (do (await launch) (recovery-result session {:resumed true :run_id (:run-id request)
                                                        :previous_run_id (:run_id session)})))))

(defn- ^:async recover-claimed! [runtime config session opts]
  (let [request (resume-request session config)]
    (restored-conversation-access! session)
    (if (str/blank? (:message request))
      (do
        (await (ensure-agent-session! runtime config (:conversation-id request) (:model request)
                                       (:auth-context request) (:thinking-level request)
                                       (:session-id request) (:agent-spec request)))
        (recovery-result session {:resumed false :reason "no pending user message to resume"}))
      (await (launch-recovery! runtime config session request (or (:wait-for opts) :completion))))))

(defn- locally-active? [conversation-id]
  (or (turn-control/active-turn conversation-id)
      (when-let [session (sessions/active-agent-session conversation-id)]
        (or (agent/streaming? session) (agent/current-turn session)))))

(defn ^:async resume-recovered-session!
  "Release an exact eligible persisted owner, then admit a distinct ordinary attempt."
  ([runtime config session] (resume-recovered-session! runtime config session nil))
  ([runtime config session opts]
   (try
     (when (locally-active? (:conversation_id session))
       (throw (ex-info "Conversation already has an active turn"
                       {:status 409 :code "thread_recovery_conflict"})))
     ;; This is the only recovery write. The provider fences both value and generation;
     ;; failures after release belong to normal turn admission/finalization, not this snapshot.
     (await (session-store/release-recovery! session))
     (await (recover-claimed! runtime config session opts))
     (catch :default error
       (let [diagnostic (native/report-failure! (:session_id session) (:conversation_id session) error)]
         (recovery-result session {:resumed false :error (str error) :code (:code (ex-data error))
                                  :diagnostic diagnostic}))))))

(defn ^:async recover-active-agent-sessions!
  "Read authoritative running snapshots and independently attempt their conditional recovery."
  [runtime config]
  (let [sessions (await (session-store/recover-sessions!))]
    (await (promise/all-vec (mapv #(resume-recovered-session! runtime config %) sessions)))))
