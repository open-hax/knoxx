(ns knoxx.backend.infra.openplanner-event-sink
  "Process-wide optional event projection, scoped by each event's actual run."
  (:require [knoxx.backend.domain.action.run-state :as runs]
            [knoxx.backend.domain.error-observatory :as errors]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.extern.local-openplanner :as host]
            [knoxx.backend.infra.clients.openplanner :as client]
            [knoxx.backend.infra.openplanner.memory :as memory]
            [knoxx.backend.law.local-openplanner :as law]))

(defn envelope
  "Bind a diagnostic event to authoritative run identity, never the latest turn."
  [config run event]
  (law/scope! {:org_id (:org_id run)})
  (when-not (and (some? (:run_id run)) (= (:run_id run) (:run_id event))
                 (or (nil? (:org_id event)) (= (:org_id event) (:org_id run))))
    (throw (ex-info "OpenPlanner diagnostic event disagrees with its run scope"
                    {:status 409 :code "openplanner_event_run_scope_conflict"})))
  (let [id (str (:run_id run) ":diagnostic:"
                (crypto/sha256-hex (pr-str (into (sorted-map) event))))]
    (host/normalize-event
     (memory/openplanner-event
      config {:id id :ts (host/instant! (:at event)) :kind (str "knoxx." (:type event))
              :project (:session-project-name config) :session (:conversation_id run)
              :message (:run_id run) :role "system"
              :text (str (:type event) (when-let [tool (:tool_name event)] (str ": " tool))
                         (when-let [preview (:preview event)] (str "\n" preview)))
              :extra (merge event (memory/run-scope-extra run)
                            (select-keys run [:run_id :conversation_id :session_id]))}))))

(defn ^:async project!
  "Project one event; canonical run persistence is handled by the durable sink."
  [config run event]
  (let [value (envelope config run event)
        provider (client/client (assoc config :openplanner-org-id (:org_id run)))]
    (when (client/enabled? provider)
      (await (client/events! provider [value])))))

(defn install!
  "Install once at bootstrap. Individual turns cannot replace or clear this sink."
  [config]
  (runs/set-event-stream-sink!
   (^:async fn [event]
     (try
       (await (project! config (get @runs/runs* (:run_id event)) event))
       (catch :default error
         ;; This is a disposable search projection; durable run-event admission
         ;; already has its own awaited subscription and cannot be bypassed here.
         (errors/log-error! "openplanner.event-projection" {:run-id (:run_id event)} error)))))
  true)
