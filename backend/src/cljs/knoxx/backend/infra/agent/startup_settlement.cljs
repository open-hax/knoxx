(ns knoxx.backend.infra.agent.startup-settlement
  "Owned startup attempts and conditional compensation across independent providers."
  (:require [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.domain.error-observatory :as errors]
            [knoxx.backend.law.startup-admission :as law]
            [knoxx.backend.shape.startup-admission :as port]))

(def reservation-key
  "Private server config coordinate connecting a FIFO reservation to its actual turn."
  ::reservation-token)

(defn ^:async prepare!
  "Capture this provider's exact preimage before the attempt writes anything."
  [store id record]
  (law/validate! record)
  (when-not (satisfies? port/IStartupAdmission store)
    (throw (ex-info "The provider cannot safely settle partial startup"
                    {:status 503 :code "startup_admission_unsupported"})))
  {:store store :record record :view (await (port/startup-view store id))})

(defn ^:async claim!
  "Remember an attempted write before awaiting its possibly ambiguous result."
  [attempted* {:keys [store record view] :as receipt}]
  (swap! attempted* conj receipt)
  (await (port/claim-startup! store record view)))

(defn- observe-unconfirmed! [record]
  (try
    (errors/log-error! :agent-turn/startup-compensation-unconfirmed
                       (select-keys record [:run_id :session_id :conversation_id])
                       (ex-info "Startup failure settlement could not be confirmed"
                                {:status 503 :code "startup_compensation_unconfirmed"}))
    {:settled? false :code "startup_compensation_unconfirmed" :diagnostic-emitted true}
    (catch :default _logging-failure
      {:settled? false :code "startup_compensation_unconfirmed" :diagnostic-emitted false})))

(defn- ^:async settle! [{:keys [store record view]}]
  (try
    (let [result (await (port/settle-startup! store record view))
          current (get @state/runs* (:run_id record))]
      (when (and current (:startup_failure result) (law/same-owner? current record))
        (state/update-run! (:run_id record) #(law/failed-record % record)))
      result)
    (catch :default _failure (observe-unconfirmed! record))))

(defn ^:async attempt!
  "Preserve the original error after settling each attempted provider independently.
   Settlement records a failed attempt; it never rolls back accepted ledger facts."
  [attempted* operation!]
  (try
    (await (operation!))
    (catch :default failure
      (doseq [receipt (reverse @attempted*)] (await (settle! receipt)))
      (throw failure))))
