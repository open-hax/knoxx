(ns knoxx.backend.infra.agent.run-admission
  "Ordered initial admission for run snapshots, conversation threads and live events."
  (:require [knoxx.backend.domain.action.run-state :refer [append-run-event! store-run!]]
            [knoxx.backend.domain.agent.content :as content]
            [knoxx.backend.domain.agent.run-admission :as data]
            [knoxx.backend.domain.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.extern.agent-turn-node :as host]
            [knoxx.backend.infra.agent.startup-settlement :as startup]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.infra.run-event-payload :refer [tool-event-payload]]
            [knoxx.backend.infra.run-events :as run-events]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]))

(defn- ^:async emit-action-task-rendered-event!
  [run-id conversation-id session-id agent-spec]
  (when-let [rendered-task (content/nonblank (:rendered-task-prompt agent-spec))]
    (let [task-event (tool-event-payload
                      run-id conversation-id session-id "action_task_rendered"
                      (cond-> {:preview rendered-task}
                        (:task-source agent-spec) (assoc :task_source (:task-source agent-spec))
                        (:trigger-id agent-spec) (assoc :trigger_id (:trigger-id agent-spec))
                        (:deprecated-agent-task-fallback agent-spec)
                        (assoc :deprecated_agent_task_fallback true)))]
      (append-run-event! run-id task-event)
      (await (run-events/flush! run-id))
      (broadcast-ws-session! session-id "events" task-event))))

(defn- ^:async publish-run-started! [run-id conversation-id session-id mode model-id thinking-level]
  (let [event (tool-event-payload run-id conversation-id session-id "run_started"
                                  {:status "running" :mode mode :model model-id :thinking_level thinking-level})]
    (append-run-event! run-id event)
    (await (run-events/flush! run-id))
    (broadcast-ws-session! session-id "events" event)))

(defn- initial-thread [base-run agent-spec auth-extra request-messages token]
  (merge (cond-> (assoc (select-keys base-run [:run_id :session_id :conversation_id :status :model :mode
                                              :thinking_level :created_at :updated_at])
                         :has_active_stream false :messages request-messages :startup_token token)
           agent-spec (assoc :agent_spec (data/agent-spec-summary agent-spec))) auth-extra))

(defn- ^:async create-owned-run! [base-run thread before-prompt! agent-spec mode thinking-level]
  (let [run-id (:run_id base-run) session-id (:session_id base-run)
        run-receipt (await (startup/prepare! @registry/session-store* run-id (dissoc base-run :events)))
        thread-receipt (await (startup/prepare! (session-store/startup-provider) session-id thread))
        attempted* (atom [])]
    (await (startup/attempt!
            attempted*
            (^:async fn []
              (await (run-events/flush! run-id))
              (await (startup/claim! attempted* run-receipt))
              (await (startup/claim! attempted* thread-receipt))
              (store-run! run-id base-run)
              (await (publish-run-started! run-id (:conversation_id base-run) session-id mode (:model base-run) thinking-level))
              (await (emit-action-task-rendered-event! run-id (:conversation_id base-run) session-id agent-spec))
              (await (before-prompt!)))))))

(defn ^:async create-initial-run!
  "Admit startup and its continuation, conditionally settling any partial failure."
  ([run-id session-id conversation-id started-at model-id mode thinking-level
    agent-spec auth-extra request-messages config]
   (await (create-initial-run! run-id session-id conversation-id started-at model-id mode thinking-level
                               agent-spec auth-extra request-messages config (fn []))))
  ([run-id session-id conversation-id started-at model-id mode thinking-level
    agent-spec auth-extra request-messages config before-prompt!]
   (let [token (or (get config startup/reservation-key) (host/random-uuid!))
         base-run (assoc (data/build-initial-run run-id session-id conversation-id started-at model-id mode thinking-level
                                                  agent-spec auth-extra request-messages config)
                         :startup_token token)
         thread (assoc (initial-thread base-run agent-spec auth-extra request-messages token)
                        :mode mode :thinking_level thinking-level)]
     (await (create-owned-run! base-run thread before-prompt! agent-spec mode thinking-level)))))
