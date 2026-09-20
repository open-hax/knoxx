(ns knoxx.backend.infra.run-events
  "Run event durability independent of optional OpenPlanner projections."
  (:require [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.extern.run-event-queue :as queue]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.shape.session-persistence :as persistence]))

(defonce ^:private installed* (atom nil))

(defn install!
  "Install one ordered durable event writer for the selected run provider."
  [store]
  (when (and store (not (satisfies? persistence/IRunEventStore store)))
    (throw (ex-info "The selected run provider does not support durable events"
                    {:status 503 :code "run_event_provider_unsupported"})))
  (let [writer (when store
                 (queue/create #(persistence/append-event! store %)))]
    (reset! installed* writer)
    (state/set-durable-event-sink! (:submit! writer))))

(defn ^:async flush!
  "Wait for all admitted run events, surfacing the first persistence failure."
  [run-id]
  (when-let [writer @installed*] (await ((:flush! writer) run-id)))
  true)

(defn ^:async persist-run!
  "Persist a run snapshot after its ordered events, without duplicating events."
  [run]
  (let [store @registry/session-store*]
    (when-not store
      (throw (ex-info "A durable run provider is required for run admission"
                      {:status 503 :code "run_provider_unavailable"})))
    (await (flush! (:run_id run)))
    (await (persistence/put-run! store (dissoc run :events :run_events :sequence)))))
