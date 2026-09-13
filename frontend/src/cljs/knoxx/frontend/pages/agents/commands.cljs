(ns knoxx.frontend.pages.agents.commands
  "Awaited workbench commands over explicitly supplied contract and runtime clients."
  (:require [clojure.string :as str]
            [knoxx.frontend.pages.agents.model :as model]))

(defn- error-message [error] (or (.-message error) (:message error) (str error)))
(defn- patch! [set-state fields] (set-state #(merge % fields)))
(defn- sorted-contracts [response] (vec (sort-by :id (:contracts response))))

(defn- library-state [state [agents roles triggers pipelines]]
  (let [items (sorted-contracts agents)
        selected (:selected-id state)
        selected-exists? (some #(= selected (:id %)) items)]
    (cond-> (assoc state :agents items :triggers (sorted-contracts triggers) :pipelines (sorted-contracts pipelines)
                   :role-options (->> (:contracts roles) (map :id) (remove str/blank?) distinct sort vec))
      (and (seq items) (not selected-exists?)) (assoc :selected-id (:id (first items))))))

(defn ^:async load-library!
  "Load the agent, role, trigger and pipeline library without clearing the draft."
  [{:keys [set-state contracts]}]
  (patch! set-state {:loading-library true :error ""})
  (try
    (let [responses (await (js/Promise.all (to-array (map (:list contracts) ["agents" "roles" "triggers" "pipelines"]))))]
      (set-state #(library-state % (vec (array-seq responses)))))
    (catch :default error
      (patch! set-state {:error (str "Failed to load agent contracts: " (error-message error))
                         :agents [] :triggers [] :pipelines [] :role-options []}))
    (finally (patch! set-state {:loading-library false}))))

(defn- patch-selected! [set-state selected-id fields]
  (set-state #(if (= selected-id (:selected-id %)) (merge % fields) %)))

(defn ^:async load-contract!
  "Ignore a completed contract load after the user selects another contract."
  [{:keys [set-state contracts]} contract-id]
  (patch! set-state {:loading-contract true :error "" :notice nil})
  (try
    (let [response (await ((:get contracts) contract-id "agents"))
          contract (or (:contract response) model/default-agent-contract)]
      (patch-selected! set-state contract-id {:draft contract :edn-text (or (:ednText response) (model/draft->edn contract)) :parse-error nil}))
    (catch :default error
      (patch-selected! set-state contract-id {:error (str "Failed to load agent contract: " (error-message error))}))
    (finally (patch-selected! set-state contract-id {:loading-contract false}))))

(defn ^:async load-runtime!
  "Read runtime state only when its capability is available."
  [{:keys [set-state runtime can-control-runtime?]}]
  (when can-control-runtime?
    (patch! set-state {:runtime-loading true :runtime-error ""})
    (try (patch! set-state {:runtime-status (await ((:get runtime)))})
         (catch :default error (patch! set-state {:runtime-status nil :runtime-error (str "Failed to load event runtime: " (error-message error))}))
         (finally (patch! set-state {:runtime-loading false})))))

(defn- reset-notice [response]
  (str "Event runtime reset. Cleared " (or (get-in response [:reset :deletedCount]) 0)
       " state key(s); review schedules before restarting."))

(defn ^:async runtime-command!
  "Apply an admitted runtime command and always clear its matching busy state."
  [{:keys [set-state runtime can-control-runtime?]} command]
  (when can-control-runtime?
    (let [busy (if (= command :reset) :resetting-runtime :toggling-runtime)]
      (patch! set-state {busy true :runtime-error "" :runtime-notice nil})
      (try
        (let [response (await ((get runtime command)))]
          (patch! set-state {:runtime-status response
                             :runtime-notice {:tone :success :text (case command :start "Event runtime started."
                                                                       :stop "Event runtime stopped." :reset (reset-notice response))}}))
        (catch :default error (patch! set-state {:runtime-notice {:tone :error :text (error-message error)}}))
        (finally (patch! set-state {busy false}))))))

(defn ^:async run-trigger!
  "Fire the selected trigger only when runtime control is permitted."
  [{:keys [set-state runtime can-control-runtime?]} trigger-id]
  (when (and can-control-runtime? (seq trigger-id))
    (patch! set-state {:running-job-id trigger-id :runtime-error "" :runtime-notice nil})
    (try
      (await ((:fire runtime) trigger-id))
      (patch! set-state {:runtime-notice {:tone :success :text (str "Fired trigger " trigger-id ".")}})
      (catch :default error (patch! set-state {:runtime-notice {:tone :error :text (error-message error)}}))
      (finally (patch! set-state {:running-job-id nil})))))

(defn replace-draft!
  "Keep the raw EDN and structured editor synchronized."
  [{:keys [set-state]} draft]
  (patch! set-state {:draft draft :edn-text (model/draft->edn draft) :parse-error nil :notice nil}))

(defn update-draft!
  "Apply a structured edit to the current contract snapshot."
  [{:keys [state] :as context} operation & arguments]
  (replace-draft! context (apply operation (or (:draft state) model/default-agent-contract) arguments)))

(defn raw-change!
  "Retain invalid EDN text while preserving the last parsed structured draft."
  [{:keys [set-state]} text]
  (patch! set-state {:edn-text text :notice nil})
  (try (patch! set-state {:draft (model/parse-contract-edn text) :parse-error nil})
       (catch :default error (patch! set-state {:parse-error (error-message error)}))))

(defn new-contract!
  "Start an unsaved contract and discard the previous selection's loading state."
  [{:keys [set-state] :as context}]
  (patch! set-state {:selected-id nil :loading-contract false :error ""})
  (replace-draft! context model/default-agent-contract))

(defn ^:async validate!
  "Validate the raw contract and show the server's admissibility result."
  [{:keys [set-state contracts state]}]
  (patch! set-state {:validating true :notice nil :error ""})
  (try
    (let [response (await ((:validate contracts) (:edn-text state) "agents"))
          ok? (:ok response) errors (:errors response)]
      (patch! set-state (cond-> {:notice {:tone (if ok? :success :error)
                                         :text (if ok? "Validation passed." (str "Validation failed: " (count errors) " error(s)."))}}
                         (:contract response) (assoc :draft (:contract response)))))
    (catch :default error (patch! set-state {:notice {:tone :error :text (error-message error)}}))
    (finally (patch! set-state {:validating false}))))

(defn- saved-state [state response contract-id]
  (let [{:keys [contract ednText validation]} response ok? (:ok validation)]
    (assoc state :selected-id contract-id :draft (or contract (:draft state)) :edn-text (or ednText (:edn-text state))
           :parse-error nil :notice {:tone (if ok? :success :error)
                                     :text (if ok? (str "Saved " contract-id ".")
                                               (str "Saved " contract-id ", but validation has " (count (:errors validation)) " error(s)."))})))

(defn ^:async save!
  "Save only with the declared capability; retain the draft on refusal."
  [{:keys [set-state contracts state can-save-contracts?] :as context}]
  (when can-save-contracts?
    (if-let [contract-id (model/normalize-contract-id (get-in state [:draft :contract/id]))]
      (do
        (patch! set-state {:saving true :notice nil :error ""})
        (try
          (let [response (await ((:save contracts) contract-id (:edn-text state) "agents"))]
            (set-state #(saved-state % response contract-id))
            (await (load-library! context)))
          (catch :default error (patch! set-state {:notice {:tone :error :text (error-message error)}}))
          (finally (patch! set-state {:saving false}))))
      (patch! set-state {:notice {:tone :error :text "Missing :contract/id."}}))))
