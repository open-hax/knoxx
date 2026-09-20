(ns knoxx.backend.infra.agent.turn-finalization
  "Turn settlement keeps mandatory persistence visible while always releasing owned resources."
  (:require [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.domain.error-observatory :as errors]
            [knoxx.backend.domain.time :as time]
            [knoxx.backend.infra.agent.session :as sessions]
            [knoxx.backend.infra.agent.transcript :as transcript]
            [knoxx.backend.infra.run-events :as run-events]
            [knoxx.backend.infra.stores.mongo-session-store :as threads]))

(defn ^:async complete!
  "Complete the thread from the current provider transcript or the fallback messages."
  [session agent-spec session-id conversation-id payload fallback-messages]
  (let [messages (sessions/prune-session-messages
                  agent-spec (transcript/transcript-after-turn session fallback-messages))]
    (await (threads/complete-session! session-id conversation-id (assoc payload :messages messages)))))

(defn ^:async settle!
  "Always attempt thread completion, release the owned sink, then remove the agent session.
   A secondary completion failure is observed without hiding the mandatory persistence failure.
   Retirement last discards this run's queue entries once its failure has been observed."
  [{:keys [run-id conversation-id event-stream-sink] :as context} persist! complete!]
  (let [failure* (volatile! nil)]
    (try
      (try (await (persist!))
           (catch :default error (vreset! failure* error) (throw error)))
      (finally
        (try
          (await (complete!))
          (catch :default error
            (if @failure*
              (errors/log-error! :agent-turn/session-completion-failed (dissoc context :event-stream-sink)
                                 (ex-info "Session completion failed during run cleanup"
                                          (select-keys (or (ex-data error) {}) [:status :code])))
              (throw error)))
          (finally
            (try (state/clear-event-stream-sink-if! event-stream-sink)
                 (finally
                   (try (sessions/remove-agent-session! conversation-id)
                        ;; `persist!` above already surfaced any durable event
                        ;; failure to this owner, and the turn admits no further
                        ;; events, so release the run's queue entries here.
                        (finally (run-events/retire! run-id)))))))))))

(defn refusal-diagnostic!
  "Record the existing refusal diagnostic with its run and agent coordinates."
  [context agent-spec diagnostic-type error]
  (errors/log-error!
   diagnostic-type
   (assoc context :contract-id (:contract-id agent-spec) :actor-id (:actor-id agent-spec)
          :trigger-id (:trigger-id agent-spec) :task-source (:task-source agent-spec))
   error))

(defn mark-failed!
  "Mark a refused run without altering its existing resource evidence."
  [run-id payload]
  (state/update-run! run-id #(merge % payload {:updated_at (time/now-iso) :status "failed"})))

(defn fail-run!
  "Mark the failed run and retain its existing hydration evidence."
  [run-id payload hydration memory-hydration]
  (let [resources (cond-> {}
                    hydration (assoc :passiveHydration (select-keys hydration [:query :tokens :database :elapsedMs :results]))
                    memory-hydration (assoc :memoryHydration (select-keys memory-hydration [:query :mode :hits :elapsedMs :conversationId])))]
    (state/update-run! run-id #(-> %
                                  (merge payload {:updated_at (time/now-iso) :status "failed"})
                                  (update :resources merge resources)))))
