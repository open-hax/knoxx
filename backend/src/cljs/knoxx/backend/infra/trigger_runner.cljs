(ns knoxx.backend.infra.trigger-runner
  "Generic trigger dispatcher. Reads :trigger contracts, schedules cron/event
   dispatches, and fires actions through the action registry.

   Trigger contracts MUST use :trigger/action with a keyword from the action
   registry. Strings are no longer supported.

   Built-in actions:
     :actions/start-agent-session  — requires :trigger/agent
     :actions/run-pipeline         — requires :trigger/with {:pipeline-id ...}
     :actions/noop                 — succeeds immediately"
  (:require ["node:fs" :as fs]
            ["node:path" :as path]
            [cljs.reader :as reader]
            [clojure.string :as str]
            [knoxx.backend.domain.contracts.loader :as loader]
            [knoxx.backend.domain.action.registry :as action-registry]
            [knoxx.backend.domain.action.start-agent-session]
            [knoxx.backend.domain.action.run-pipeline]
            [knoxx.backend.infra.config :as runtime-config]
            [knoxx.backend.domain.models :as models]))

(defonce running?* (atom false))
(defonce trigger-id->interval* (atom {}))
(defonce trigger-id->inflight* (atom {}))
(defonce reload-timer* (atom nil))

(declare fire!)

(defn- cfg [] (models/enrich-config (runtime-config/cfg)))

;; ─── EDN reading (sync, for startup) ─────────────────────────────────

(defn- read-trigger-file
  [file-path]
  (try
    (let [text (.readFileSync fs file-path "utf8")]
      (when-not (str/blank? text)
        (reader/read-string text)))
    (catch :default e
      (js/console.error "Failed to read trigger file" file-path e)
      nil)))

(defn- trigger-file-path
  [config trigger-id]
  (loader/contract-file-path config "triggers" trigger-id))

(defn- load-contract-sync
  [config contract-class contract-id]
  (let [klass (loader/normalize-contract-class contract-class)
        wanted-id (some-> contract-id str str/trim not-empty)]
    (some (fn [record]
            (when (and (= klass (:contractClass record))
                       (= wanted-id (:id record)))
              (:contract record)))
          (loader/load-all-contracts-sync config))))

;; ─── action dispatch ──────────────────────────────────────────────────

(defn- dispatch-trigger-action!
  "Dispatch a trigger's action through the action registry.
   Builds the action map and execution context from the trigger contract.
   Every invocation receives a synthetic :event so actions have a uniform
   interface regardless of whether they were fired by cron, manual call, or
   an external event dispatcher."
  [config trigger trigger-ctx]
  (let [action (action-registry/action-map trigger)
        ctx {:config config
             :event {:event/type (keyword "trigger" (name (:trigger/kind trigger)))
                     :event/payload trigger-ctx}
             :trigger trigger
             :actor/id (:trigger/actor trigger)
             :trigger-ctx trigger-ctx}]
    (if (:action/kind action)
      (action-registry/run-action! ctx action)
      (js/Promise.reject
       (js/Error. (str "Trigger has no :trigger/action: " (:contract/id trigger)))))))

;; ─── cron scheduling ──────────────────────────────────────────────────

(defn- parse-cron-to-ms
  "Naive cron-to-ms: supports '*/N * * * *' and 'N * * * *' forms.
   Falls back to 5 minutes. Never returns < 1 minute to avoid busy-loops."
  [cron-expr]
  (let [ms (cond
             (re-find #"/(\d+)" cron-expr)
             (let [n (-> (re-find #"/(\d+)" cron-expr) second js/parseInt)]
               (* 60 1000 (max (or n 5) 1)))

             (re-find #"^(\d+)\s+\*" cron-expr)
             (let [n (-> (re-find #"^(\d+)\s+\*" cron-expr) second js/parseInt)]
               (* 60 1000 (max n 1)))

             :else (* 5 60 1000))]
    (max ms (* 60 1000))))

(defn- schedule-cron-trigger!
  [config trigger-id cron-expr]
  (let [ms (parse-cron-to-ms cron-expr)
        id (js/setInterval
            (fn []
              (when @running?*
                (when-not (get @trigger-id->inflight* trigger-id)
                  (when-let [contract (load-contract-sync config "triggers" trigger-id)]
                    (when (:enabled contract)
                      (swap! trigger-id->inflight* assoc trigger-id true)
                      (-> (js/Promise.resolve
                           (dispatch-trigger-action!
                            config
                            contract
                            (get-in contract [:data :context])))
                          (.then (fn [_] (swap! trigger-id->inflight* dissoc trigger-id)))
                          (.catch (fn [err]
                                    (swap! trigger-id->inflight* dissoc trigger-id)
                                    (js/console.error "[trigger-runner] failed to fire" trigger-id err))))))))
              ms))]
    (swap! trigger-id->interval* assoc trigger-id id)
    (js/console.log "[trigger-runner] scheduled" trigger-id "every" ms "ms")))

;; ─── public API ─────────────────────────────────────────────────────

(defn start!
  "Load all :trigger contracts from disk and schedule enabled cron ones."
  ([]
   (start! (cfg)))
  ([config]
   (when-not @running?*
     (reset! running?* true)
     (doseq [trigger-id (loader/list-contract-ids-sync config "triggers")]
       (when-let [contract (load-contract-sync config "triggers" trigger-id)]
         (when (:enabled contract)
           (case (:trigger/kind contract)
             :cron (schedule-cron-trigger! config trigger-id (:trigger/schedule contract))
             :manual (js/console.log "[trigger-runner] registered manual trigger" trigger-id)
             :event (js/console.log "[trigger-runner] registered event trigger" trigger-id)
             (js/console.log "[trigger-runner] unknown trigger kind" (:trigger/kind contract)))))))))

(defn stop!
  "Stop all scheduled triggers."
  []
  (doseq [[trigger-id interval-id] @trigger-id->interval*]
    (js/clearInterval interval-id)
    (js/console.log "[trigger-runner] stopped" trigger-id))
  (reset! trigger-id->interval* {})
  (reset! trigger-id->inflight* {})
  (reset! running?* false))

(defn reload!
  ([]
   (reload! (cfg)))
  ([config]
   (stop!)
   (start! config)
   (js/Promise.resolve {:ok true :action "reload"})))

(defn debounced-reload!
  []
  (when-let [timer @reload-timer*]
    (js/clearTimeout timer))
  (reset! reload-timer*
          (js/setTimeout
           (fn []
             (reset! reload-timer* nil)
             (reload!))
           350)))

(defn reset-runtime!
  ([]
   (reset-runtime! (cfg)))
  ([config]
   (reload! config)))

(defn run-job!
  [trigger-id]
  (fire! trigger-id))

(defn fire!
  "Manually fire a trigger by its contract id, with optional context map."
  ([trigger-id] (fire! trigger-id {}))
  ([trigger-id ctx]
   (let [config (cfg)
         contract (load-contract-sync config "triggers" trigger-id)]
     (if contract
       (let [ctx' (merge (get-in contract [:data :context]) ctx)]
         (-> (js/Promise.resolve (dispatch-trigger-action! config contract ctx'))
             (.then (fn [result]
                      (if (:ok result)
                        {:fired trigger-id}
                        (js/Promise.reject (js/Error. (:error result))))))))
       (js/Promise.reject (js/Error. (str "Unknown trigger: " trigger-id)))))))

(defn status
  "Return a snapshot of all trigger states."
  []
  (let [config (cfg)]
    {:running @running?*
     :triggers (->> (loader/list-contract-ids-sync config "triggers")
                    (mapv (fn [id]
                            (when-let [c (load-contract-sync config "triggers" id)]
                              (select-keys c [:contract/id :trigger/kind :trigger/action :enabled])))))}))
