(ns knoxx.backend.triggers.trigger-runner
  "Generic trigger dispatcher. Reads :trigger contracts, schedules cron/event
   dispatches, and fires :pipeline or :agent/:action targets."
  (:require ["node:fs" :as fs]
            ["node:path" :as path]
            [cljs.reader :as reader]
            [clojure.string :as str]
            [knoxx.backend.contracts.loader :as loader]
            [knoxx.backend.discord-io :as discord-io]
            [knoxx.backend.pipeline-runner :as pipeline-runner]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.models :as models]))

(defonce running?* (atom false))
(defonce trigger-id->interval* (atom {}))

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

;; ─── fire helpers ─────────────────────────────────────────────────────

(defn- fire-pipeline!
  [config trigger-ctx pipeline-id]
  (when-let [contract (loader/load-contract config "pipelines" pipeline-id)]
    (pipeline-runner/run-pipeline! config (assoc contract :trigger-ctx trigger-ctx))))

(defn- fire-agent!
  [config trigger-ctx agent-id]
  (when-let [contract (loader/load-contract config "agents" agent-id)]
    (let [ctx (merge (:context trigger-ctx) (:data/context contract))
          discord-ctx (select-keys trigger-ctx [:channelId :channelName
                                                 :authorUsername :content :reason])]
      (discord-io/start-agent-session!
       config
       (assoc contract :taskPrompt (get-in contract [:prompts :task]))
       discord-ctx))))

(defn- fire-action!
  [config _trigger-ctx action-id]
  (js/console.warn "[trigger-runner] action dispatch not yet wired:" action-id))

;; ─── cron scheduling ────────────────────────────────────────────────

(defn- parse-cron-to-ms
  "Naive cron-to-ms: supports '*/N * * * *' and 'N * * * *' forms.
   Falls back to 5 minutes."
  [cron-expr]
  (cond
    (re-find #"/(\d+)" cron-expr)
    (let [n (-> (re-find #"/(\d+)" cron-expr) second js/parseInt)]
      (* 60 1000 (or n 5)))

    (re-find #"^\d+" cron-expr)
    (-> (re-find #"^\d+" cron-expr) js/parseInt (* 60 1000))

    :else (* 5 60 1000)))

(defn- schedule-cron-trigger!
  [config trigger-id cron-expr]
  (let [ms (parse-cron-to-ms cron-expr)
        id (js/setInterval
             (fn []
               (when @running?*
                 (when-let [contract (loader/load-contract config "triggers" trigger-id)]
                   (when (:enabled contract)
                     (let [target (:trigger/target contract)
                           ctx    (:data/context contract)]
                       (cond
                         (str/starts-with? target "pipeline") (fire-pipeline! config ctx target)
                         (str/starts-with? target "action")   (fire-action! config ctx target)
                         :else                                 (fire-agent! config ctx target)))))))
             ms)]
    (swap! trigger-id->interval* assoc trigger-id id)
    (js/console.log "[trigger-runner] scheduled" trigger-id "every" ms "ms")))

;; ─── public API ─────────────────────────────────────────────────────

(defn start!
  "Load all :trigger contracts from disk and schedule enabled cron ones."
  []
  (when-not @running?*
    (reset! running?* true)
    (let [config (cfg)]
      (doseq [trigger-id (loader/list-contract-ids-sync config "triggers")]
        (when-let [contract (loader/load-contract config "triggers" trigger-id)]
          (when (:enabled contract)
            (case (:trigger/kind contract)
              :cron (schedule-cron-trigger! config trigger-id (:trigger/schedule contract))
              :manual (js/console.log "[trigger-runner] registered manual trigger" trigger-id)
              (js/console.log "[trigger-runner] unknown trigger kind" (:trigger/kind contract)))))))))

(defn stop!
  "Stop all scheduled triggers."
  []
  (doseq [[trigger-id interval-id] @trigger-id->interval*]
    (js/clearInterval interval-id)
    (js/console.log "[trigger-runner] stopped" trigger-id))
  (reset! trigger-id->interval* {})
  (reset! running?* false))

(defn fire!
  "Manually fire a trigger by its contract id, with optional context map."
  ([trigger-id] (fire! trigger-id {}))
  ([trigger-id ctx]
   (let [config (cfg)
         contract (loader/load-contract config "triggers" trigger-id)]
     (if contract
       (let [target (:trigger/target contract)
             ctx'   (merge (:data/context contract) ctx)]
         (cond
           (str/starts-with? target "pipeline") (fire-pipeline! config ctx' target)
           (str/starts-with? target "action")   (fire-action! config ctx' target)
           :else                                 (fire-agent! config ctx' target))
         (js/Promise.resolve {:fired trigger-id}))
       (js/Promise.reject (js/Error. (str "Unknown trigger: " trigger-id)))))))

(defn status
  "Return a snapshot of all trigger states."
  []
  (let [config (cfg)]
    {:running @running?*
     :triggers (->> (loader/list-contract-ids-sync config "triggers")
                   (mapv (fn [id]
                           (when-let [c (loader/load-contract config "triggers" id)]
                              (select-keys c [:contract/id :trigger/kind :trigger/target :enabled])))))}))