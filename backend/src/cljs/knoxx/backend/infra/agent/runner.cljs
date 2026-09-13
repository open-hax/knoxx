(ns knoxx.backend.infra.agent.runner
  "Shared entrypoint for launching normal Knoxx agent turns.

   Event-triggered and chat-triggered work should converge on the same Knoxx
   turn runtime. This namespace provides a queue-style direct-start helper so
   non-HTTP callers can use the same semantics as /api/knoxx/direct/start."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.domain.time :as time]
            [knoxx.backend.extern.agent-runner :as xrunner]
            [knoxx.backend.extern.agent-turn-node :as xturn-node]
            [knoxx.backend.extern.agent-turn-request :as turn-request]
            [knoxx.backend.extern.event-turn-admission :as admission]
            [knoxx.backend.infra.agent.policy :as agent-policy]
            [knoxx.backend.infra.agent.queued-run :as queued-run]
            [knoxx.backend.infra.agent.session-gate :as session-gate]
            [knoxx.backend.infra.agent.turn :as agent-turns]
            [knoxx.backend.infra.run-event-payload :as run-payload]
            [knoxx.backend.infra.run-events :as run-events]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.shape.event-turn-queue :as queue-shape]))

(def ^:private default-event-agent-concurrency 1)
(def ^:private default-event-agent-queue-limit 256)

(defn- initial-event-turn-queue-state []
  (queue-shape/initial-state default-event-agent-concurrency default-event-agent-queue-limit))

(defonce ^:private event-turn-queue*
  (atom (initial-event-turn-queue-state)))

(defonce ^:private event-turn-settlers*
  (atom {}))

(defn current-runtime
  "Return the currently installed runtime handle."
  []
  @runtime-state/runtime*)

(defn direct-start-payload->turn-params
  "Decode the supported direct-start field spellings at their owned request boundary."
  [payload] (turn-request/direct-start-payload->turn-params payload))

(defn- policy-model
  [config body]
  (or (:model body)
      (get-in body [:agent-spec :model])
      (:llmModel config)
      (:proxx-default-model config)))

(defn- queue-snapshot-from-state [state] (queue-shape/snapshot state))

(defn event-turn-queue-snapshot
  "Return observable, serialization-safe state for the event-agent FIFO."
  []
  (queue-snapshot-from-state @event-turn-queue*))

(defn reset-event-turn-queue!
  "Reset limiter bookkeeping for tests or a stopped runtime.

   This does not cancel a turn that is already executing."
  []
  (reset! event-turn-queue* (initial-event-turn-queue-state))
  (event-turn-queue-snapshot))

(defn- forget-settler-if-current!
  [event-id settle! settlement]
  (swap! event-turn-settlers*
         (fn [settlers]
           (let [entry (get settlers event-id)]
             (if (and (= settle! (:settle! entry))
                      (= settlement (:settlement entry)))
               (dissoc settlers event-id)
               settlers)))))

(defn- ^:async deliver-event-turn-settlement!
  [event-id settle! settlement]
  (try
    (await (settle! settlement))
    (forget-settler-if-current! event-id settle! settlement)
    true
    (catch :default err
      (js/console.error "[agent-runner] event turn settlement callback failed"
                        event-id "-" (ex-message err))
      false)))

(defn- rearm-settler-if-cleared!
  [event-id settle!]
  (swap! event-turn-settlers*
         (fn [settlers]
           (if (contains? settlers event-id)
             settlers
             (assoc settlers event-id
                    {:settle! settle! :settlement nil})))))

(defn ^:async register-event-turn-settler!
  "Register one process-local settlement callback for a durable event id.

   The callback receives the terminal full-turn result. That result is retained
   until accepted; re-registration immediately redelivers it, so same-process
   replay can heal a transient callback failure without rerunning the turn."
  [event-id settle!]
  (let [event-id (some-> event-id str str/trim not-empty)]
    (when-not (and event-id (fn? settle!))
      (throw (ex-info "event turn settler requires an event id and callback"
                      {:event-id event-id})))
    (let [settlement (get-in @event-turn-settlers* [event-id :settlement])
          redelivery-accepted?
          (volatile! nil)]
      (swap! event-turn-settlers* assoc event-id
             {:settle! settle! :settlement settlement})
      (when settlement
        (vreset! redelivery-accepted?
                 (await (deliver-event-turn-settlement!
                         event-id settle! settlement)))
        ;; Registration names the next replay owner as well as accepting the old
        ;; result. Re-arm only after successful delivery; a failed callback must
        ;; retain the cached result for the next registration attempt.
        (when @redelivery-accepted?
          (rearm-settler-if-cleared! event-id settle!)))
      {:event-turn/registered? true
       :event-turn/redelivered? (boolean settlement)
       :event-turn/redelivery-accepted? @redelivery-accepted?})))

(defn unregister-event-turn-settler!
  "Forget the settlement callback for an event that never entered the FIFO."
  [event-id]
  (swap! event-turn-settlers* dissoc (str event-id))
  true)

(defn reset-event-turn-settlers!
  "Clear settlement callbacks for tests or a stopped runtime."
  []
  (reset! event-turn-settlers* {})
  true)

(defn event-turn-owner-state
  "Return `:in-flight`, `:settled`, or nil for one registered event owner.

   A settled entry is retained only when its terminal callback still needs
   redelivery. Callers may replace that callback to repair the durable effect;
   they must not replace an in-flight owner's callback."
  [event-id]
  (when-let [entry (get @event-turn-settlers* (str event-id))]
    (if (some? (:settlement entry))
      :settled
      :in-flight)))

(defn event-triggered-turn?
  "True only for turns carrying normalized trigger audit metadata."
  [body]
  (boolean
   (some-> (get-in body [:agent-spec :trigger-id])
           str
           str/trim
           not-empty)))

(defn- event-queue-settings
  [config]
  {:concurrency (max 1 (or (:event-agent-concurrency config)
                           default-event-agent-concurrency))
   :queue-limit (max 1 (or (:event-agent-queue-limit config)
                           default-event-agent-queue-limit))})

(defn- event-turn-reservation [state entry concurrency queue-limit]
  (queue-shape/reservation state entry concurrency queue-limit))

(defn- reserve-event-turn!
  [entry concurrency queue-limit]
  (loop []
    (let [before @event-turn-queue*
          {:keys [after queue-full? result]}
          (event-turn-reservation before entry concurrency queue-limit)]
      (cond
        queue-full? result
        (compare-and-set! event-turn-queue* before after) result
        :else (recur)))))

(defn- release-event-turn! [queue-id]
  (loop []
    (let [before @event-turn-queue*]
      (when-let [{:keys [after next-entry]} (queue-shape/release-entry before queue-id)]
        (if (compare-and-set! event-turn-queue* before after) next-entry (recur))))))

(defn- accepted-response
  ([body]
   (accepted-response body nil))
  ([body queue-result]
   (cond-> {:ok true
            :queued true
            :run_id (:run-id body)
            :conversation_id (:conversation-id body)
            :session_id (:session-id body)
            :model (or (:model body)
                       (get-in body [:agent-spec :model]))}
     queue-result (assoc :event_queue (queue-shape/response-queue-metadata queue-result)))))

(defn- mark-event-turn-started!
  [body]
  (let [run-id (:run-id body)
        conversation-id (:conversation-id body)
        session-id (:session-id body)]
    (run-state/update-run!
     run-id
     (fn [run]
       (-> run
           (assoc :status "running"
                  :updated_at (time/now-iso))
           (assoc-in [:settings :eventQueue :status] "running")
           (assoc-in [:settings :eventQueue :position] 0))))
    (run-state/append-run-event!
     run-id
     (run-payload/tool-event-payload
      run-id conversation-id session-id "event_turn_started"
      {:status "running"
       :restart_aware false}))))

(defn log-and-record-async-spawn-error!
  "Record a failed admitted turn and its diagnostic event."
  [body err]
  (let [diagnostic (xrunner/error-diagnostic body err)
        run-id (:run-id body)
        conversation-id (:conversation-id body)
        session-id (:session-id body)
        event (run-payload/tool-event-payload run-id conversation-id session-id
                                            "async_spawn_failed"
                                            {:status "failed"
                                             :error (:message diagnostic)
                                             :diagnostic diagnostic})]
    (xrunner/log-async-spawn-error! body err)
    (when run-id
      (run-state/update-run! run-id
                             (fn [run]
                               (cond-> run
                                 run (assoc :status "failed"
                                            :error (:message diagnostic)))))
      (run-state/append-run-event! run-id event))
    diagnostic))

(defn- ^:async send-turn-and-record!
  [runtime config body]
  (try
    (await (agent-turns/send-agent-turn! runtime config body))
    nil
    (catch :default err
      (log-and-record-async-spawn-error! body err))))

(defn- event-turn-config
  "Apply the event-only timeout without changing interactive turn settings."
  [config]
  (assoc config
         :agent-turn-timeout-ms
         (or (:event-agent-turn-timeout-ms config) 0)))

(defn- event-turn-deadline-ms
  [timeout-ms]
  (when (and (number? timeout-ms) (pos? timeout-ms))
    (+ (xrunner/now-ms) timeout-ms)))

(defn- busy-error
  [message]
  (js/Promise.reject (js/Error. message)))

(defn- event-turn-settlement
  [result deadline-ms]
  (cond->
   (if-let [detail (some-> (:error result) str str/trim not-empty)]
     {:event-turn/status :failed
      :event-turn/detail detail}
     {:event-turn/status :completed})
    deadline-ms (assoc :event-turn/deadline-ms deadline-ms)))

(defn- event-turn-failure
  [err deadline-ms]
  (cond->
   {:event-turn/status :failed
    :event-turn/detail (or (some-> (ex-message err) str str/trim not-empty)
                           "event-triggered agent turn failed")}
    deadline-ms (assoc :event-turn/deadline-ms deadline-ms)))

(defn- event-id-from-body
  [body]
  (some-> (get-in body [:agent-spec :event-id]) str str/trim not-empty))

(defn- cache-event-turn-settlement!
  [event-id settlement]
  (swap! event-turn-settlers*
         (fn [settlers]
           (if-let [entry (get settlers event-id)]
             (assoc settlers event-id (assoc entry :settlement settlement))
             settlers)))
  (get @event-turn-settlers* event-id))

(defn- ^:async notify-event-turn-settler!
  "Retryable delivery of one full-turn settlement to its current owner.

   The result is cached before delivery. A failed callback therefore leaves both
   callback and result registered; durable replay installs a fresh equivalent
   callback, and registration redelivers this same result before re-emission."
  [body settlement]
  (when-let [event-id (event-id-from-body body)]
    (when-let [settle! (:settle! (cache-event-turn-settlement!
                                  event-id settlement))]
      (await (deliver-event-turn-settlement!
              event-id settle! settlement)))))

(defn- ^:async execute-admitted-turn! [body start-turn! deadline-ms]
  (try
    (mark-event-turn-started! body)
    (await (run-events/flush! (:run-id body)))
    (let [result (await (start-turn!))]
      (await (notify-event-turn-settler! body (event-turn-settlement result deadline-ms))))
    (catch :default err
      (log-and-record-async-spawn-error! body err)
      (await (run-events/persist-run! (get @run-state/runs* (:run-id body))))
      (await (notify-event-turn-settler! body (event-turn-failure err deadline-ms))))))

(defn- ^:async execute-event-turn!
  [{:keys [queue-id body start-turn! admission event-turn-timeout-ms]}]
  (try
    (when-not (await admission)
      (await (execute-admitted-turn! body start-turn! (event-turn-deadline-ms event-turn-timeout-ms))))
    (catch :default error
      (xrunner/log-async-spawn-error! body error))
    (finally
      (when-let [next-entry (release-event-turn! queue-id)]
        (execute-event-turn! next-entry)))))

(defn- ^:async admit-event-reservation! [config body queue-result entry gate]
  (let [full? (= :full (:status queue-result))
        message (when full? (str "event_agent_queue_full: pending queue limit "
                                 (get-in queue-result [:snapshot :queue-limit]) " reached"))]
    (try
      (await (queued-run/record-event-turn! config body queue-result
                                 (if full? "failed" "queued")
                                 (if full? "event_turn_queue_rejected" "event_turn_queued") message))
      ((:complete! gate) nil)
      message
      (catch :default error
        ((:complete! gate) error)
        (when-let [next-entry (release-event-turn! (:queue-id entry))]
          (execute-event-turn! next-entry))
        (throw error)))))

(defn ^:async enqueue-event-turn!
  "Durably admit a run before acknowledging its reserved process-local FIFO position.
  Await this result. The reservation precedes I/O; a promoted pending entry waits
  for its own admission before starting. Durable event replay owns restart recovery."
  [config body start-turn!]
  (let [{:keys [concurrency queue-limit]} (event-queue-settings config)
        gate (admission/gate)
        entry {:queue-id (xturn-node/random-uuid!) :body body :admission (:promise gate)
               :event-turn-timeout-ms (:event-agent-turn-timeout-ms config) :start-turn! start-turn!}
        queue-result (reserve-event-turn! entry concurrency queue-limit)
        rejection (await (admit-event-reservation! config body queue-result entry gate))]
    (if rejection
      (do
        (log-and-record-async-spawn-error! body (js/Error. rejection))
        (await (run-events/flush! (:run-id body)))
        (await (busy-error rejection)))
      (do
        (when (= :running (:status queue-result)) (execute-event-turn! entry))
        (accepted-response body queue-result)))))

(defn- ^:async queue-turn!
  [runtime config body]
  (await (agent-policy/validate-chat-policy! (:auth-context body) (policy-model config body)))
  (if (event-triggered-turn? body)
    (await (enqueue-event-turn!
            config body
            (fn []
              (agent-turns/send-agent-turn!
               runtime (event-turn-config config) body))))
    (do
      (send-turn-and-record! runtime config body)
      (accepted-response body))))

(def DISPATCH_RECLAIM_COOLDOWN_MS
  "Minimum spacing between orphaned-session reclaim attempts."
  session-gate/DISPATCH_RECLAIM_COOLDOWN_MS)

(defn spawn-direct!
  "Admit one direct turn after resolving the existing session busy gate."
  ([config payload]
   (spawn-direct! (current-runtime) config payload))
  ([runtime config payload]
   (if-not runtime
     (busy-error "Knoxx runtime unavailable for direct agent spawn")
     (let [payload (xrunner/to-cljs-map payload)
           body (turn-request/normalize-body payload agent-turns/ensure-session-id xturn-node/random-uuid!)
           provided-session-id (or (:session_id payload)
                                   (:session-id payload))]
       (if-not provided-session-id
         (queue-turn! runtime config body)
         (session-gate/dispatch-with-session-gate! runtime config body queue-turn!))))))
