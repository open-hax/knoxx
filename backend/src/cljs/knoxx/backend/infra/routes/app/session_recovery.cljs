(ns knoxx.backend.infra.routes.app.session-recovery
  "Detect stale sessions and coordinate turn recovery before chat admission."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.infra.agent.service :as agent-service]
            [knoxx.backend.infra.http :as infra-http]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]
            [knoxx.backend.shape.agent :as shape-agent]))

(def SESSION_RECOVERY_STALE_MS
  "Compatibility entrypoint for 60000." 60000)

(defn- clear-ghost-turn!
  "If turn-control has an entry for conversation-id but the underlying Proxx
   session shows no active streaming or current turn, the entry is a ghost
   from a previous hung run. Unregister it so zombie recovery can proceed."
  [conversation-id]
  (let [agent-session (agent-service/active-agent-session conversation-id)
        active-streaming? (and agent-session (shape-agent/streaming? agent-session))
        active-turn? (and agent-session
                          (try
                            (some? (shape-agent/current-turn agent-session))
                            (catch js/Error _ false)))]
    (when (and (not active-streaming?) (not active-turn?))
      (turn-control/unregister-active-turn! conversation-id))))

(defn runtime-processing-session?
  "Check live streaming, current-turn and registered-turn ownership."
  [conversation-id]
  (let [agent-session (agent-service/active-agent-session conversation-id)
        active-streaming? (and agent-session (shape-agent/streaming? agent-session))
        active-turn? (and agent-session
                          (try
                            (some? (shape-agent/current-turn agent-session))
                            (catch js/Error _ false)))
        registered-turn? (some? (turn-control/active-turn conversation-id))]
    (or active-streaming? active-turn? registered-turn?)))

(defn- parse-iso-ms
  [value]
  (let [parsed (js/Date.parse (str (or value "")))]
    (when-not (js/isNaN parsed)
      parsed)))

(defn ^:async latest-run-event!
  "Return the latest in-memory event for the requested run, when present."
  [run-id]
  (let [run-id (str (or run-id ""))]
    (cond
      (str/blank? run-id)
      nil

      (seq (get-in @run-state/runs* [run-id :events]))
      (last (get-in @run-state/runs* [run-id :events]))

      :else
      nil)))

(defn stale-running-session?
  "Check the existing recovery timeout against the latest session activity."
  [session latest-event]
  (let [stamp (or (:at latest-event)
                  (:updated_at session)
                  (:created_at session))
        stamp-ms (parse-iso-ms stamp)]
    (or (nil? stamp-ms)
        (> (- (.now js/Date) stamp-ms) SESSION_RECOVERY_STALE_MS))))

(defn ^:async detect-zombies
  "Abort a stale unowned session before retrying admission, otherwise report conflict."
  [conversation-id session session-id queue-turn! can-send-result reply latest-event]
  (clear-ghost-turn! conversation-id)
  (let [stalled? (and (= "running" (:status session))
                      (not (runtime-processing-session? conversation-id))
                      (stale-running-session? session latest-event))]
    (if stalled?
      (try
        (await (session-store/complete-session! session-id
                                                conversation-id
                                                {:status "failed"
                                                 :error "Session was stale/zombie; auto-aborted before new turn."
                                                 :messages (:messages session)}))
        (await (queue-turn! "Async direct agent chat failed (recovered from zombie)"))
        (catch :default err
          (.error js/console "Failed to abort zombie session" err)
          (infra-http/json-response! reply 409 {:ok false
                                     :error (str "Agent is already processing. Zombie recovery failed: " err)
                                     :code "agent_already_processing"
                                     :has_active_stream false
                                     :can_send false})))
      (infra-http/json-response! reply 409 {:ok false
                                 :error (str "Agent is already processing. " (or (:reason can-send-result) ""))
                                 :code "agent_already_processing"
                                 :has_active_stream (boolean (:has_active_stream session))
                                 :can_send false}))))
