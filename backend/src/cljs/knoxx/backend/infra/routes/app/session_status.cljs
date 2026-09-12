(ns knoxx.backend.infra.routes.app.session-status
  "Report live session state and request on-demand recovery."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.app-session-requests :as session-requests]
            [knoxx.backend.infra.agent.service :as agent-service]
            [knoxx.backend.infra.http :as infra-http]
            [knoxx.backend.infra.routes.app.session-recovery :as app-session-recovery]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]))

(defn- session-status-running-response
  [session-id session runtime-active? can-send stalled? latest-event]
  {:session_id session-id
   :conversation_id (:conversation_id session)
   :run_id (:run_id session)
   :status (:status session)
   :has_active_stream (boolean (or (:has_active_stream session) runtime-active?))
   :can_send (if stalled? false (:can-send can-send))
   :reason (cond
             stalled? "Session looked stalled after restart; recovery requested."
             runtime-active? "Session is already processing. Use steer, follow-up, abort, or wait."
             :else (:reason can-send))
   :model (:model session)
   :updated_at (:updated_at session)
   :latest_event_at (:at latest-event)
   :recovery_requested stalled?})

(defn- ^:async resume-on-demand! [runtime config session]
  (try
    (await (agent-service/resume-recovered-session! runtime config session))
    (catch :default err
      (js/console.error "On-demand session recovery failed" err))))

(defn- ^:async existing-session-status!
  [runtime config reply session-id conversation-id session]
  (let [conversation-id' (str (or (:conversation_id session) conversation-id ""))
        runtime-active? (app-session-recovery/runtime-processing-session? conversation-id')
        can-send (session-store/session-can-send? session)
        latest-event (await (if (= "running" (:status session))
                              (app-session-recovery/latest-run-event! (:run_id session))
                              (js/Promise.resolve nil)))
        stalled? (and (= "running" (:status session)) (not runtime-active?)
                      (app-session-recovery/stale-running-session? session latest-event))]
    (when stalled? (resume-on-demand! runtime config session))
    (infra-http/json-response! reply 200
                    (session-status-running-response
                     session-id session runtime-active? can-send stalled? latest-event))))

(defn- missing-session-status [session-id conversation-id]
  ;; Preserve a live in-memory turn when its persisted session is absent.
  (if (app-session-recovery/runtime-processing-session? conversation-id)
    {:session_id session-id :conversation_id conversation-id :status "running"
     :has_active_stream true :can_send false
     :reason "Session is already processing. Use steer, follow-up, abort, or wait."}
    {:session_id session-id :conversation_id conversation-id :status "not_found"
     :has_active_stream false :can_send true :reason "No session state found. Ready for new turn."}))

(defn ^:async handle-session-status
  "Report persisted or live session state and request recovery for stale work." [runtime config reply request]
  (let [{:keys [session-id conversation-id]} (session-requests/status-query request)]
    (if (str/blank? session-id)
      (infra-http/json-response! reply 400 {:error "session_id is required"})
      (try
        (if-let [session (await (session-store/get-session session-id))]
          (await (existing-session-status! runtime config reply session-id conversation-id session))
          (infra-http/json-response! reply 200 (missing-session-status session-id conversation-id)))
        (catch :default err
          (js/console.error "Session status check failed" err)
          (infra-http/json-response! reply 500 {:error (str err)}))))))
