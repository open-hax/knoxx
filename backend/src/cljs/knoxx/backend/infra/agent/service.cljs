(ns knoxx.backend.infra.agent.service
  "Small consumer-facing facade for Knoxx agent runtime operations.

   Existing lower-level functions remain as compatibility wrappers while routes
   and tools migrate to this namespace."
  (:require [knoxx.backend.infra.agent.recovery :as recovery]
            [knoxx.backend.infra.agent.runner :as runner]
            [knoxx.backend.infra.agent.runtime :as runtime]
            [knoxx.backend.infra.agent.session :as session]
            [knoxx.backend.infra.agent.turn :as turn]))

(defprotocol IAgentService
  (-start-turn! [svc turn-request])
  (-queue-turn! [svc turn-request])
  (-control-turn! [svc control-request])
  (-resume-turn! [svc recovery-request])
  (-active-turn [svc conversation-id]))

(defn- accepted-response
  [turn-request]
  {:ok true
   :queued true
   :run_id (:run-id turn-request)
   :runId (:run-id turn-request)
   :conversation_id (:conversation-id turn-request)
   :conversationId (:conversation-id turn-request)
   :session_id (:session-id turn-request)
   :sessionId (:session-id turn-request)
   :model (or (:model turn-request)
              (get-in turn-request [:agent-spec :model]))})

(defrecord KnoxxAgentService [runtime config delegates]
  IAgentService
  (-start-turn! [_ turn-request]
    ((or (:start-turn! delegates) turn/send-agent-turn!) runtime config turn-request))

  (-queue-turn! [this turn-request]
    (if-let [queue-fn (:queue-turn! delegates)]
      (queue-fn runtime config turn-request)
      (do
        (-> (-start-turn! this turn-request)
            (.catch (fn [err]
                      (.error js/console "[agent-service] queued turn failed" err))))
        (js/Promise.resolve (accepted-response turn-request)))))

  (-control-turn! [_ control-request]
    ((or (:control-turn! delegates) runtime/queue-agent-control!) runtime config control-request))

  (-resume-turn! [_ recovery-request]
    (let [resume-fn (or (:resume-turn! delegates) recovery/resume-recovered-session!)
          session (or (:session recovery-request) recovery-request)
          opts (:opts recovery-request)]
      (if opts
        (resume-fn runtime config session opts)
        (resume-fn runtime config session))))

  (-active-turn [_ conversation-id]
    (if-let [active-fn (:active-turn delegates)]
      (active-fn conversation-id)
      (session/active-agent-session conversation-id))))

(defn agent-service
  ([runtime config]
   (agent-service runtime config {}))
  ([runtime config delegates]
   (->KnoxxAgentService runtime config (or delegates {}))))

(defonce default-service* (atom nil))

(defn set-default-service!
  [svc]
  (reset! default-service* svc)
  svc)

(defn default-service
  []
  (or @default-service*
      (throw (js/Error. "No default Knoxx agent service configured"))))

(defn start-turn-runtime!
  [runtime config turn-request]
  (-start-turn! (agent-service runtime config) turn-request))

(defn queue-turn-runtime!
  [runtime config turn-request]
  (-queue-turn! (agent-service runtime config) turn-request))

(defn control-turn-runtime!
  [runtime config control-request]
  (-control-turn! (agent-service runtime config) control-request))

(defn resume-turn-runtime!
  [runtime config recovery-request]
  (-resume-turn! (agent-service runtime config) recovery-request))

(defn start-turn!
  ([turn-request]
   (-start-turn! (default-service) turn-request))
  ([svc turn-request]
   (-start-turn! svc turn-request))
  ([runtime config turn-request]
   (start-turn-runtime! runtime config turn-request)))

(defn queue-turn!
  ([turn-request]
   (-queue-turn! (default-service) turn-request))
  ([svc turn-request]
   (-queue-turn! svc turn-request))
  ([runtime config turn-request]
   (queue-turn-runtime! runtime config turn-request)))

(defn control-turn!
  ([control-request]
   (-control-turn! (default-service) control-request))
  ([svc control-request]
   (-control-turn! svc control-request))
  ([runtime config control-request]
   (control-turn-runtime! runtime config control-request)))

(defn resume-turn!
  ([recovery-request]
   (-resume-turn! (default-service) recovery-request))
  ([svc recovery-request]
   (-resume-turn! svc recovery-request))
  ([runtime config recovery-request]
   (resume-turn-runtime! runtime config recovery-request)))

(defn active-turn
  ([conversation-id]
   (-active-turn (default-service) conversation-id))
  ([svc conversation-id]
   (-active-turn svc conversation-id))
  ([_runtime _config conversation-id]
   (-active-turn (agent-service _runtime _config) conversation-id)))

;; Transitional compatibility wrappers. Prefer start-turn!/queue-turn!/control-turn!
;; in new callers.
(defn send-agent-turn!
  [runtime config turn-request]
  (start-turn-runtime! runtime config turn-request))

(defn queue-agent-control!
  [runtime config control-request]
  (control-turn-runtime! runtime config control-request))

(defn resume-recovered-session!
  ([runtime config recovered-session]
   (resume-turn-runtime! runtime config {:session recovered-session}))
  ([runtime config recovered-session opts]
   (resume-turn-runtime! runtime config {:session recovered-session :opts opts})))

(defn active-agent-session
  [conversation-id]
  (-active-turn (agent-service nil nil) conversation-id))

(defn spawn-direct!
  ([config payload]
   (runner/spawn-direct! config payload))
  ([runtime config payload]
   (runner/spawn-direct! runtime config payload)))
