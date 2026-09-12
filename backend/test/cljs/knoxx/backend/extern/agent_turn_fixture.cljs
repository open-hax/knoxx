(ns knoxx.backend.extern.agent-turn-fixture
  "Real isolated thread/run providers for runtime lifecycle tests."
  (:require [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.extern.provider-recovery-fixture :as disk]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.clio-run-store :as runs]
            [knoxx.backend.infra.stores.clio-thread-store :as threads]
            [knoxx.backend.infra.stores.mongo-session-store :as sessions]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.shape.session-persistence :as run-port]))

(defn ^:async with-run!
  "Seed valid durable lifecycle coordinates and restore every installed fixture."
  [{:keys [run_id session_id conversation_id] :as coordinates} body]
  (let [directory (disk/temporary-directory)
        previous-run @registry/session-store* previous-thread @sessions/provider*
        previous-heap @state/runs* previous-order @state/run-order*
        at (disk/instant (disk/now-ms))
        run (merge {:status "running" :created_at at :updated_at at :messages []}
                   coordinates)
        provider (runs/open! {:directory (str directory "/runs")})]
    (try
      (reset! registry/session-store* provider)
      (events/install! provider)
      (sessions/install! (threads/open! {:directory (str directory "/threads")}))
      (await (run-port/put-run! provider run))
      (await (sessions/put-session! (assoc run :session_id session_id :conversation_id conversation_id)))
      (state/store-run! run_id run)
      (await (body))
      (finally
        (try (await (events/flush! run_id))
             (finally
               (reset! registry/session-store* previous-run)
               (events/install! previous-run)
               (sessions/install! previous-thread)
               (reset! state/runs* previous-heap)
               (reset! state/run-order* previous-order)
               (disk/remove! directory)))))))
