(ns knoxx.backend.infra.agent.run-admission
  "Ordered initial admission for run snapshots, conversation threads and live events."
  (:require [knoxx.backend.domain.action.run-state :refer [append-run-event! store-run!]]
            [knoxx.backend.domain.agent.content :as content]
            [knoxx.backend.domain.agent.run-admission :as data]
            [knoxx.backend.domain.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.extern.agent-turn-node :as host]
            [knoxx.backend.infra.run-event-payload :refer [tool-event-payload]]
            [knoxx.backend.infra.run-events :as run-events]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]))

(defn- emit-action-task-rendered-event!
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
      (broadcast-ws-session! session-id "events" task-event))))

(defn- ^:async persist-initial-session!
  [session-payload session-id]
  (try
    (await (session-store/put-session! session-payload))
    (catch :default err
      (host/log-initial-session-failure! session-id err)
      (throw err))))

(defn ^:async create-initial-run!
  "Await both initial admissions before exposing a live run or emitting run_started."
  [run-id session-id conversation-id started-at model-id mode thinking-level
   agent-spec auth-extra request-messages config]
  (let [base-run (data/build-initial-run run-id session-id conversation-id started-at model-id mode thinking-level
                                    agent-spec auth-extra request-messages config)]
    (await (run-events/persist-run! base-run))
    (await (persist-initial-session! (merge (cond-> {:session_id session-id
                                              :conversation_id conversation-id
                                              :run_id run-id
                                              :status "running"
                                              :model model-id
                                              :mode mode
                                              :thinking_level thinking-level
                                              :created_at started-at
                                              :updated_at started-at
                                              :has_active_stream false
                                              :messages request-messages}
                                       agent-spec (assoc :agent_spec (data/agent-spec-summary agent-spec)))
                                     auth-extra)
                              session-id))
    (store-run! run-id base-run)
    (let [initial-event (tool-event-payload run-id conversation-id session-id "run_started"
                                            {:status "running"
                                             :mode mode
                                             :model model-id
                                             :thinking_level thinking-level})]
      (append-run-event! run-id initial-event)
      (broadcast-ws-session! session-id "events" initial-event))
    (emit-action-task-rendered-event! run-id conversation-id session-id agent-spec)))

