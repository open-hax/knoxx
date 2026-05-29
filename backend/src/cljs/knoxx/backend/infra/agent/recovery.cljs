(ns knoxx.backend.infra.agent.recovery
  "Session recovery after restarts: resume or abort stale runs."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.agent.session :refer [ensure-agent-session!]]
            [knoxx.backend.infra.agent.turn :as turn]
            [knoxx.backend.infra.auth.authz :as authz :refer [auth-snapshot-has-principal?]]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.infra.stores.session-store :as session-store]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.domain.time :refer [now-iso]]))

(def ^:private RECOVERED-SESSION-KICKOFF-TIMEOUT-MS 5000)
(def ^:private RECOVERED-SESSION-KICKOFF-POLL-MS 25)

(defn recovered-auth-context
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
  [session]
  (:agent_spec session))

(defn restored-conversation-access!
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
  [session]
  (some (fn [message]
          (let [role (some-> (:role message) str str/lower-case)
                content (some-> (:content message) str)]
            (when (and (= role "user")
                       (not (str/blank? content)))
              content)))
        (reverse (vec (or (:messages session) [])))))

(defn- wait-for-recovered-turn-kickoff!
  [conversation-id launch-promise]
  (if (some? (turn-control/active-turn conversation-id))
    (js/Promise.resolve true)
    (js/Promise.
     (fn [resolve reject]
       (let [done? (atom false)
             started-ms (.now js/Date)
             check! (fn check! []
                      (cond
                        @done? nil
                        (some? (turn-control/active-turn conversation-id))
                        (do
                          (reset! done? true)
                          (resolve true))

                        (> (- (.now js/Date) started-ms) RECOVERED-SESSION-KICKOFF-TIMEOUT-MS)
                        (do
                          (reset! done? true)
                          (reject (js/Error. (str "Timed out waiting for recovered session kickoff: " conversation-id))))

                        :else
                        (js/setTimeout check! RECOVERED-SESSION-KICKOFF-POLL-MS)))]
         (.catch launch-promise
                 (fn [err]
                   (when-not @done?
                     (reset! done? true)
                     (reject err))))
         (check!))))))

(defn ^:async resume-with-message!
  [runtime config session-id conversation-id run-id message model-id mode
   thinking-level auth-context agent-spec wait-for resume-failed!]
  (try
    (await (session-store/update-session! (redis/get-client) session-id
                                          {:status "running" :has_active_stream false :recovered_at (now-iso)}))
    (let [send-promise (turn/send-agent-turn! runtime config {:conversation-id conversation-id
                                                               :session-id session-id
                                                               :run-id run-id
                                                               :message message
                                                               :model model-id
                                                               :mode mode
                                                               :thinking-level thinking-level
                                                               :auth-context auth-context
                                                               :agent-spec agent-spec})]
      (if (= wait-for :kickoff)
        (do
          (.catch send-promise (fn [err]
                                 (js/console.error "[knoxx] recovered session failed after kickoff"
                                                   #js {:sessionId session-id :conversationId conversation-id :error (str err)})
                                 nil))
          (await (wait-for-recovered-turn-kickoff! conversation-id send-promise))
          {:session_id session-id :conversation_id conversation-id :resumed true :wait_for "kickoff"})
        (do
          (await send-promise)
          {:session_id session-id :conversation_id conversation-id :resumed true})))
    (catch :default err
      (resume-failed! err))))

(defn ^:async resume-recovered-session!
  ([runtime config session]
   (resume-recovered-session! runtime config session nil))
  ([runtime config session opts]
   (let [conversation-id (str (or (:conversation_id session) ""))
         session-id (str (or (:session_id session) ""))
         run-id (or (:run_id session) nil)
         model-id (or (:model session) nil)
         mode (or (:mode session) "direct")
         wait-for (or (:wait-for opts) :completion)
         thinking-level (or (:thinking_level session)
                            (:agent-thinking-level config)
                            "off")
         auth-context (recovered-auth-context session)
         agent-spec (recovered-agent-spec session)
         message (last-session-user-message session)
         resume-failed! (fn [err]
                          (js/console.error "[knoxx] failed to resume recovered session"
                                            #js {:sessionId session-id
                                                 :conversationId conversation-id
                                                 :error (str err)})
                          (-> (session-store/complete-session! (redis/get-client)
                                                               session-id
                                                               conversation-id
                                                               {:status "failed"
                                                                :error (str "Session recovery failed: " err)
                                                                :messages (:messages session)})
                              (.then (fn [_]
                                       {:session_id session-id
                                        :conversation_id conversation-id
                                        :resumed false
                                        :error (str err)}))))]
     (restored-conversation-access! session)
     (cond
       (or (str/blank? conversation-id)
           (str/blank? session-id))
       {:session_id session-id :conversation_id conversation-id :resumed false :reason "missing session or conversation id"}

       (str/blank? message)
       (do
         (await (ensure-agent-session! runtime config conversation-id model-id auth-context thinking-level session-id agent-spec))
         (await (session-store/update-session! (redis/get-client) session-id
                                               {:status "waiting_input"
                                                :has_active_stream false
                                                :recovered_at (now-iso)}))
         {:session_id session-id :conversation_id conversation-id :resumed false :reason "no pending user message to resume"})

       :else
       (await (resume-with-message! runtime config session-id conversation-id run-id message model-id mode
                                    thinking-level auth-context agent-spec wait-for resume-failed!))))))

(defn ^:async recover-active-agent-sessions!
  [runtime config redis-client]
  (let [sessions (await (session-store/recover-sessions! redis-client))
        items (vec sessions)]
    (if (seq items)
      (let [results (await (.all js/Promise (clj->js (mapv #(resume-recovered-session! runtime config %) items))))]
        (vec (js->clj results :keywordize-keys true)))
      [])))
