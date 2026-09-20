(ns knoxx.backend.infra.agent.runner
  "Shared entrypoint for launching normal Knoxx agent turns.

   Event-triggered and chat-triggered work should converge on the same Knoxx
   turn runtime. This namespace provides a queue-style direct-start helper so
   non-HTTP callers can use the same semantics as /api/knoxx/direct/start."
  (:require [knoxx.backend.infra.run-event-payload :as run-payload]
            [clojure.string :as str]
            [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.domain.time :as time]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.infra.agent.policy :as agent-policy]
            [knoxx.backend.infra.agent.queued-run :as queued-run]
            [knoxx.backend.infra.agent.startup-settlement :as startup]
            [knoxx.backend.infra.run-events :as run-events]
            [knoxx.backend.extern.event-turn-admission :as admission]
            [knoxx.backend.shape.event-turn-queue :as queue-shape]
            [knoxx.backend.infra.agent.session :refer [active-agent-session]]
            [knoxx.backend.infra.agent.turn :as agent-turns]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]
            [knoxx.backend.infra.system-instance :as system-instance]
            [knoxx.backend.extern.agent-runner :as xrunner]
            [knoxx.backend.extern.agent-turn-node :as xturn-node]
            [knoxx.backend.law.spawn-diagnostic :as spawn-diagnostic]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.shape.agent :refer [streaming?]]))

(def ^:private default-event-agent-concurrency 1)
(def ^:private default-event-agent-queue-limit 256)

(defn- initial-event-turn-queue-state []
  (queue-shape/initial-state default-event-agent-concurrency default-event-agent-queue-limit))

(defonce ^:private event-turn-queue*
  (atom (initial-event-turn-queue-state)))

(defonce ^:private event-turn-settlers*
  (atom {}))

(defn current-runtime
  []
  @runtime-state/runtime*)

(defn- normalize-tool-policy
  [policy]
  (let [policy (xrunner/to-cljs-map policy)
        tool-id (some-> (or (:toolId policy)
                            (:tool-id policy)
                            (:tool_id policy))
                        str
                        not-empty)
        effect (some-> (or (:effect policy) "allow") str not-empty)]
    (when tool-id
      {:toolId tool-id :effect effect})))

(defn- spec-value
  "Extract a normalized string value from a spec map given keyword alternatives."
  [spec & keys]
  (some-> (some (fn [k] (get spec k)) keys)
          str
          not-empty))

(def ^:private agent-spec-string-aliases
  {:contract-id [:contract_id :contract-id :contractId
                 :agent_id :agent-id :agentId]
   :actor-id [:actor_id :actor-id :actorId]
   :role [:role :role_slug :role-slug :roleSlug]
   :task-source [:task_source :task-source :taskSource]
   :model [:model]
   :thinking-level [:thinking_level :thinking-level :thinkingLevel
                    :reasoning_effort :reasoning-effort :reasoningEffort]
   :sub-agent-id [:sub_agent_id :sub-agent-id :subAgentId]
   :parent-agent-id [:parent_agent_id :parent-agent-id :parentAgentId]
   :parent-run-id [:parent_run_id :parent-run-id :parentRunId]
   :spawn-kind [:spawn_kind :spawn-kind :spawnKind]
   :trigger-id [:trigger_id :trigger-id :triggerId]
   :event-type [:event_type :event-type :eventType
                :trigger_event_type :trigger-event-type :triggerEventType]
   :event-id [:event_id :event-id :eventId]
   :event-scope-id [:event_scope_id :event-scope-id :eventScopeId]
   :schedule-id [:schedule_id :schedule-id :scheduleId]})

(defn- normalized-string-fields
  [spec]
  (reduce-kv
   (fn [normalized field aliases]
     (if-let [value (apply spec-value spec aliases)]
       (assoc normalized field value)
       normalized))
   {}
   agent-spec-string-aliases))

(defn- normalized-prompt-fields
  [spec]
  (let [system-prompt (or (:system_prompt spec) (:system-prompt spec) (:systemPrompt spec))
        task-prompt (or (:task_prompt spec) (:task-prompt spec) (:taskPrompt spec))
        rendered (or (:rendered_task_prompt spec) (:rendered-task-prompt spec)
                     (:renderedTaskPrompt spec))
        deprecated? (boolean (or (:deprecated_agent_task_fallback spec)
                                 (:deprecated-agent-task-fallback spec)
                                 (:deprecatedAgentTaskFallback spec)))]
    (cond-> {}
      (some? system-prompt) (assoc :system-prompt system-prompt)
      (some? task-prompt) (assoc :task-prompt task-prompt)
      (some? rendered) (assoc :rendered-task-prompt rendered)
      deprecated? (assoc :deprecated-agent-task-fallback true))))

(defn- normalized-runtime-fields
  [spec]
  (let [tool-policies (->> (or (:tool_policies spec) (:tool-policies spec)
                               (:toolPolicies spec) [])
                           (keep normalize-tool-policy) vec)
        tools-choice-value (or (:tools_choice spec) (:tools-choice spec)
                               (:toolsChoice spec) (:tools/choice spec))
        tools-choice (some-> (if (keyword? tools-choice-value)
                               (name tools-choice-value)
                               tools-choice-value)
                             str
                             str/trim
                             not-empty)
        resources (or (:resource_policies spec) (:resource-policies spec)
                      (:resourcePolicies spec))
        sources (or (:sources spec) (:runtime_sources spec)
                    (:runtime-sources spec) (:runtimeSources spec))
        memory (or (:memory_hydration spec) (:memory-hydration spec)
                   (:memoryHydration spec))
        context (or (:context_policy spec) (:context-policy spec)
                    (:contextPolicy spec) (:context spec))]
    (cond-> {}
      (seq tool-policies) (assoc :tool-policies tool-policies)
      tools-choice (assoc :tools-choice tools-choice)
      resources (assoc :resource-policies resources)
      (seq sources) (assoc :sources sources)
      memory (assoc :memory-hydration memory)
      context (assoc :context-policy context))))

(defn- normalize-agent-spec
  [value]
  (let [spec (xrunner/to-cljs-map value)
        string-fields (normalized-string-fields spec)
        event-type (:event-type string-fields)
        event-types (->> (or (:event_types spec) (:event-types spec) (:eventTypes spec)
                             (when event-type [event-type]) [])
                         (map str) (remove str/blank?) distinct vec)]
    (cond-> (merge string-fields
                   (normalized-prompt-fields spec)
                   (normalized-runtime-fields spec))
      (seq event-types) (assoc :event-types event-types))))

(defn direct-start-payload->turn-params
  [payload]
  (let [payload (xrunner/to-cljs-map payload)
        auth-context (or (:auth_context payload)
                         (:auth-context payload))
        template-context (or (:template_context payload)
                             (:template-context payload)
                             (:templateContext payload))]
    (cond-> {:conversation-id (or (:conversation_id payload)
                                  (:conversation-id payload))
             :session-id (or (:session_id payload)
                             (:session-id payload))
             :run-id (or (:run_id payload)
                         (:run-id payload))
             :message (or (:message payload) "")
             :content-parts (or (:content_parts payload)
                                (:content-parts payload)
                                [])
             :model (:model payload)
             :mode "direct"
             :agent-spec (normalize-agent-spec (or (:agent_spec payload)
                                                   (:agent-spec payload)))}
      template-context (assoc :template-context template-context)
      auth-context (assoc :auth-context auth-context))))

(defn- policy-model
  [config body]
  (or (:model body)
      (get-in body [:agent-spec :model])
      (:llmModel config)
      (:proxx-default-model config)))

(defn event-turn-queue-snapshot
  "Return observable, serialization-safe state for the event-agent FIFO."
  []
  (queue-shape/snapshot @event-turn-queue*))

(defn reset-event-turn-queue!
  "Reset limiter bookkeeping for tests or a stopped runtime.
   Executing turns retain ownership and are not cancelled."
  []
  (doseq [entry (:pending @event-turn-queue*)]
    (run-state/release-owned-run! (:queue-id entry)))
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
        (compare-and-set! event-turn-queue* before after)
        (do (run-state/retain-owned-run! (:queue-id entry) (get-in entry [:body :run-id]))
          result)
        :else (recur)))))

(defn- release-event-turn! [queue-id]
  (loop []
    (let [before @event-turn-queue*]
      (when-let [{:keys [after next-entry]} (queue-shape/release-entry before queue-id)]
        (if (compare-and-set! event-turn-queue* before after)
          (do (run-state/release-owned-run! queue-id) next-entry)
          (recur))))))

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
  "Log and return private diagnostics without claiming this invocation was admitted.
   This compatibility entrypoint must never mutate a run found only by request ID."
  [body err]
  (xrunner/log-async-spawn-error! body err)
  (xrunner/error-diagnostic body err))

(defn- record-admitted-event-turn-failure!
  "Record a queue invocation whose own initial run/event admission already succeeded.
   Only the two queue paths below may call this after their awaited admission gate."
  [body err]
  (let [diagnostic (log-and-record-async-spawn-error! body err)
        run-id (:run-id body)]
    ;; Admission evidence is the caller's completed queue gate, not this lookup.
    ;; The lookup only avoids recreating an already evicted diagnostic record.
    (when (get @run-state/runs* run-id)
      (let [public (spawn-diagnostic/public-diagnostic diagnostic)
            event (run-payload/tool-event-payload run-id (:conversation-id body) (:session-id body)
                                                "async_spawn_failed"
                                                {:status "failed" :error (:message public)
                                                 :diagnostic public})]
        (run-state/update-run! run-id #(assoc % :status "failed" :error (:message public)))
        (run-state/append-run-event! run-id event)))
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
      (record-admitted-event-turn-failure! body err)
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
      (run-state/release-owned-run! queue-id)
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
  (let [config (assoc config startup/reservation-key (or (get config startup/reservation-key) (xturn-node/random-uuid!)))
        {:keys [concurrency queue-limit]} (event-queue-settings config)
        gate (admission/gate)
        entry {:queue-id (xturn-node/random-uuid!) :body body :admission (:promise gate)
               :event-turn-timeout-ms (:event-agent-turn-timeout-ms config) :start-turn! start-turn!}
        queue-result (reserve-event-turn! entry concurrency queue-limit)
        rejection (await (admit-event-reservation! config body queue-result entry gate))]
    (if rejection
      (do
        (record-admitted-event-turn-failure! body (js/Error. rejection))
        (await (run-events/flush! (:run-id body)))
        (await (busy-error rejection)))
      (do
        (when (= :running (:status queue-result)) (execute-event-turn! entry))
        (accepted-response body queue-result)))))

(defn- ^:async queue-turn!
  [runtime config body]
  (await (agent-policy/validate-chat-policy! (:auth-context body) (policy-model config body)))
  (if (event-triggered-turn? body)
    (let [config (assoc (event-turn-config config) startup/reservation-key (xturn-node/random-uuid!))]
      (await (enqueue-event-turn!
              config body
              (fn [] (agent-turns/send-agent-turn! runtime config body)))))
    (do
      (send-turn-and-record! runtime config body)
      (accepted-response body))))

;; ── Orphaned-session reclaim ──────────────────────────────────────────
;;
;; A session document can claim status "running" while no live run exists:
;; either the process that owned it restarted (different system instance),
;; or a run in this instance died without flipping the status. Without
;; reclaim, background trigger dispatches bounce off the corpse with
;; agent_already_processing until the 10-minute stale reaper fires.

(def DISPATCH_RECLAIM_COOLDOWN_MS 60000)

(defn- runtime-owns-live-run?
  "True when this process is actively executing work for the conversation."
  [conversation-id]
  (let [agent-session (active-agent-session conversation-id)]
    (or (and agent-session (streaming? agent-session))
        (some? (turn-control/active-turn conversation-id)))))

(defn- session-updated-ms
  "Best-effort epoch millis of the session document's last update."
  [session]
  (let [ts (or (:updated_at session) (:created_at session))]
    (cond
      (number? ts) ts
      (string? ts) (let [ms (.getTime (js/Date. ts))]
                     (if (js/isNaN ms) 0 ms))
      :else 0)))

(defn- session-cold?
  "True when the document is old enough that a live-but-unregistered run
   (e.g. one orphaned in-memory by a hot reload) cannot plausibly own it."
  [session]
  (>= (- (js/Date.now) (session-updated-ms session))
      DISPATCH_RECLAIM_COOLDOWN_MS))

(defn- ^:async reclaim-orphaned-session!
  "Mark a running session document that no live run owns as failed so the
   pending dispatch can proceed. Returns true on success; failures are
   logged and the caller falls through to the normal busy error."
  [body session reason]
  (try
    (js/console.warn "[agent-runner] reclaiming orphaned session"
                     (str (:session-id body)) "-" reason)
    (await (session-store/complete-session!
            (str (:session-id body))
            (str (or (:conversation-id body) ""))
            {:status "failed"
             :error (str "Session reclaimed by dispatch: " reason)
             :messages (:messages session)}))
    true
    (catch :default err
      (js/console.warn "[agent-runner] orphan reclaim failed:" err)
      false)))

(defn- ^:async reclaim-and-dispatch!
  "Reclaim an orphaned running session, then dispatch the pending turn."
  [runtime config body session reason]
  (await (reclaim-orphaned-session! body session reason))
  (await (queue-turn! runtime config body)))

(defn- ^:async dispatch-with-session-gate!
  "Resolve the session busy-gate for a direct spawn.

   running + stamped by a previous system instance  → reclaim, dispatch
   running + no live run here + document gone cold  → reclaim, dispatch
   running + live run in this instance              → busy error
   otherwise                                        → dispatch"
  [runtime config body]
  (let [session (await (session-store/get-session (:session-id body)))
        can-send-result (session-store/session-can-send? session)
        conversation-id (:conversation-id body)
        agent-session (active-agent-session conversation-id)]
    (cond
      (:can-send can-send-result)
      (if (and agent-session (streaming? agent-session))
        (await (busy-error "agent_already_processing: active stream"))
        (await (queue-turn! runtime config body)))

      (not (system-instance/owned-by-current-instance? session))
      (await (reclaim-and-dispatch! runtime config body session
                                    "owned by previous system instance (restart)"))

      (and (not (runtime-owns-live-run? conversation-id))
           (session-cold? session))
      (await (reclaim-and-dispatch! runtime config body session
                                    "no live run in current system instance"))

      :else
      (await (busy-error (str "agent_already_processing: "
                              (:reason can-send-result)))))))

(defn- normalize-body
  [_runtime payload]
  (let [params (direct-start-payload->turn-params payload)
        provided-session-id (:session-id params)
        session-id (agent-turns/ensure-session-id provided-session-id)
        conversation-id (or (:conversation-id params) (xturn-node/random-uuid!))
        run-id (or (:run-id params) (xturn-node/random-uuid!))]
    (assoc params
           :session-id session-id
           :conversation-id conversation-id
           :run-id run-id
           :mode "direct")))

(defn spawn-direct!
  ([config payload]
   (spawn-direct! (current-runtime) config payload))
  ([runtime config payload]
   (if-not runtime
     (busy-error "Knoxx runtime unavailable for direct agent spawn")
     (let [payload (xrunner/to-cljs-map payload)
           body (normalize-body runtime payload)
           provided-session-id (or (:session_id payload)
                                   (:session-id payload))]
       (if-not provided-session-id
         (queue-turn! runtime config body)
         (dispatch-with-session-gate! runtime config body))))))
