(ns knoxx.backend.infra.agent.handoff
  "Execute contract-pinned delegation through the existing Knoxx runtime."
  (:require [malli.core :as m]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.domain.agent.handoff :as handoff]
            [knoxx.backend.domain.contracts.loader :as contracts]
            [knoxx.backend.domain.text :refer [tool-text-result]]
            [knoxx.backend.domain.tools :refer [create-tool-obj]]
            [knoxx.backend.extern.tools :as xtools]
            [knoxx.backend.infra.actor.acting :as acting]
            [knoxx.backend.infra.agent.tool-context :as tool-context]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.clients.knoxx-control :as control]
            [knoxx.backend.infra.stores.session-store-registry :as stores]
            [knoxx.backend.shape.session-persistence :as persistence]))

(defn- session-owner!
  [auth-context]
  (let [{:keys [conversation-id session-id agent-spec]} (tool-context/current)
        actor-id (:actor-id agent-spec)
        scope (acting/current-lookup-scope)]
    (when-not (and (seq conversation-id) (seq actor-id) (= actor-id (acting/current-actor-id)))
      (throw (ex-info "Delegation requires an actor-bound agent session"
                      {:status 403 :code "agent_handoff_scope_denied"})))
    {:actor-id actor-id :conversation-id conversation-id :session-id session-id
     :org-id (or (:org-id scope) (authz/ctx-org-id auth-context))
     :membership-id (or (:membership-id scope) (authz/ctx-membership-id auth-context))}))

(defn- resource [records resource-class id]
  (some (fn [record]
          (when (and (= resource-class (:contractClass record))
                     (= id (or (get-in record [:contract :contract/id])
                               (get-in record [:contract :actor/id]))))
            (:contract record)))
        records))

(defn make-dispatch-execute
  "Capture identity and coalesce identical requests within this tool/session instance.
   A failed transport remains failed: silently retrying could duplicate a child."
  ([auth-context] (make-dispatch-execute auth-context (atom {})))
  ([auth-context issued*]
  (let [accepted* (atom {})]
    (^:async fn [_runtime config _call-id params _a _b _c]
      (let [parameters (xtools/parameter-map params)
            owner (session-owner! auth-context)
            actor-id (:actor-id owner)
            records (contracts/load-all-contracts-sync config)
            payload (handoff/request-plan actor-id
                                         (resource records "actors" actor-id)
                                         (resource records "agents" (:agent parameters))
                                         parameters)
            key [owner parameters]
            pending (or (get @accepted* key)
                        (let [started (control/direct-start! (control/client config) payload)]
                          (swap! accepted* assoc key started)
                          started))
            result (await pending)]
        (swap! issued* assoc (:run_id result) (handoff/issued-run owner result))
        (tool-text-result (str "Accepted child run_id=" (:run_id result)
                               "\nModel: " (:model result)
                               "\nCall agent_dispatch_status from this session with the exact run_id. Acceptance is not completion; do not dispatch again to obtain status.")
                          result))))))

(defn ^:async wait-for-result!
  "Read authenticated status until terminal or the bounded polling budget expires.
   Each poll rechecks ownership; callers inject the existing run-store and timer."
  [issued-run poll-budget read-run! pause!]
  (loop [remaining poll-budget]
    (let [stored (await (read-run! (:run_id issued-run)))
          result (handoff/visible-result issued-run (or stored issued-run))]
      (if (or (handoff/terminal? result) (zero? remaining))
        result
        (do (await (pause!)) (recur (dec remaining)))))))

(defn make-status-execute
  "Wait for actual progress rather than forcing the model into rapid polling."
  ([auth-context] (make-status-execute auth-context (atom {})))
  ([auth-context issued*]
  (^:async fn [_runtime _config _call-id params _a _b _c]
    (let [{:keys [run_id wait_ms] :as parameters} (xtools/parameter-map params)]
      (when-not (m/validate handoff/StatusParameters parameters)
        (throw (ex-info "Invalid child-status request" {:status 400})))
      (let [owner (session-owner! auth-context)
            issued (handoff/require-issued-run! owner run_id (get @issued* run_id))
            store @stores/session-store*
            result (await (wait-for-result! issued
                                           (quot (or wait_ms handoff/default-status-wait-ms) 1000)
                                           #(persistence/get-run store %)
                                           #(promise/delay-ms 1000)))]
        (tool-text-result (str (pr-str result)
                               (when-not (handoff/terminal? result)
                                 "\nStored status is still nonterminal after a bounded wait. Report it as pending, not completed; this snapshot alone is not proof of a live worker. Keep this exact run_id and do not dispatch another child for status.")) result))))))

(defn create-tools
  "Expose delegation surfaces only when granted by the resolved tool policy."
  [runtime config auth-context]
  (let [issued* (atom {})]
  (into []
        (keep (fn [[id label description schema execute]]
                (when (or (nil? auth-context) (authz/ctx-tool-allowed? auth-context id))
                  (create-tool-obj id label description description
                                   ["Use agent_dispatch with only agent and message. Acceptance is not completion; read agent_dispatch_status with the exact returned run_id for its result."]
                                   schema execute runtime config))))
        [["agent.dispatch" "Dispatch Specialized Agent"
          "Call agent_dispatch with only agent (declared worker contract ID) and message (bounded task). Models, reasoning and tools are pinned by that contract; no overrides are accepted."
          handoff/Parameters (make-dispatch-execute auth-context issued*)]
         ["agent.dispatch.status" "Read Specialized Agent Result"
          "Call agent_dispatch_status with the exact accepted run_id. It waits up to 180 one-second polling intervals by default (plus storage latency), returning early on a terminal result. Pending is not completed work."
          handoff/StatusParameters (make-status-execute auth-context issued*)]])))
