(ns knoxx.backend.domain.action.run-pipeline
  "Built-in action: run a :pipeline contract."
  (:require [clojure.string :as str]
            [knoxx.backend.contracts.loader :as loader]
            [knoxx.backend.domain.action.registry :refer [run-action!]]
            [knoxx.backend.infra.pipeline-runner :as pipeline-runner]))

(defn- nonblank
  [value]
  (some-> value str str/trim not-empty))

(defn- load-contract-sync
  [config contract-class contract-id]
  (let [klass (loader/normalize-contract-class contract-class)
        wanted-id (nonblank contract-id)]
    (some (fn [record]
            (when (and (= klass (:contractClass record))
                       (= wanted-id (:id record)))
              (:contract record)))
          (loader/load-all-contracts-sync config))))

(defn- trigger-ctx
  "Extract trigger context from the execution ctx map.
   For events: merge event payload with trigger data context.
   For cron/manual: use trigger data context directly."
  [{:keys [event trigger trigger-ctx]}]
  (or trigger-ctx
      (merge (get-in trigger [:data :context]) {})
      (get-in event [:event/payload]) {}))

(defmethod run-action! :actions/run-pipeline
  [{:keys [config trigger] :as ctx} action]
  (let [pipeline-id (or (get-in action [:action/with :pipeline-id])
                        (get-in action [:action/with :pipelineId]))]
    (if-not pipeline-id
      (js/Promise.reject
       (js/Error. "Action :actions/run-pipeline requires :pipeline-id in :action/with"))
      (if-let [contract (load-contract-sync config "pipelines" pipeline-id)]
        (pipeline-runner/run-pipeline! config
                                       (assoc contract
                                              :trigger-ctx (trigger-ctx ctx)))
        (js/Promise.reject
         (js/Error. (str "Pipeline contract not found: " pipeline-id)))))))
