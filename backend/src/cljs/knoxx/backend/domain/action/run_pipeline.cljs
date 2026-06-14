(ns knoxx.backend.domain.action.run-pipeline
  "DEPRECATED: Use :actions/run-steps instead.

   Built-in action: run a :pipeline contract. This action is retained for
   backward compatibility only. Delegates to :actions/run-steps handler."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.contracts.loader :as loader]
            [knoxx.backend.domain.action.registry :as registry]))

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

(defmethod registry/run-action! :actions/run-pipeline
  [{:keys [config] :as ctx} action]
  (js/console.warn "[knoxx/actions] :actions/run-pipeline is deprecated; use :actions/run-steps")
  (let [pipeline-id (or (get-in action [:action/with :pipeline-id])
                        (get-in action [:action/with :pipelineId]))]
    (if-not pipeline-id
      (js/Promise.reject
       (js/Error. "Action :actions/run-pipeline requires :pipeline-id in :action/with"))
      (if-let [contract (load-contract-sync config "pipelines" pipeline-id)]
        (let [steps (or (:pipeline/steps contract) [])
              run-steps-action {:action/kind :actions/run-steps
                                :action/with {:steps (mapv (fn [step]
                                                             {:action (keyword (:step/contract step))
                                                              :with (get-in step [:step/data :context] {})})
                                                           steps)}}]
          (registry/run-steps-handler ctx run-steps-action))
        (js/Promise.reject
         (js/Error. (str "Pipeline contract not found: " pipeline-id)))))))
