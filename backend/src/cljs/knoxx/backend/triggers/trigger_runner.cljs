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
            [knoxx.backend.runtime.models :as models]
            [knoxx.backend.tooling :as tooling]))

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

(defn- load-contract-sync
  [config contract-class contract-id]
  (let [klass (loader/normalize-contract-class contract-class)
        wanted-id (some-> contract-id str str/trim not-empty)]
    (some (fn [record]
            (when (and (= klass (:contractClass record))
                       (= wanted-id (:id record)))
              (:contract record)))
          (loader/load-all-contracts-sync config))))

;; ─── fire helpers ─────────────────────────────────────────────────────

(defn- fire-pipeline!
  [config trigger-ctx pipeline-id]
  (when-let [contract (load-contract-sync config "pipelines" pipeline-id)]
    (pipeline-runner/run-pipeline! config (assoc contract :trigger-ctx trigger-ctx))))

(defn- fire-agent!
  [config trigger-ctx agent-id]
  (when-let [contract (load-contract-sync config "agents" agent-id)]
    (let [resolved (tooling/resolve-agent-contract config agent-id)
          job {:id agent-id
               :name agent-id
               :contractId agent-id
               :role (:role resolved)
               :systemPrompt (:system-prompt resolved)
               :taskPrompt (or (:task-prompt resolved)
                               (get-in contract [:prompts :task]))
               :model (:model resolved)
               :thinkingLevel (:thinking-level resolved)
               :toolPolicies (:tool-policies resolved)
               :sources (:sources resolved)
               :memoryHydration (:memory-hydration resolved)
               :contextPolicy (:context-policy resolved)}
          discord-ctx (select-keys (or trigger-ctx {}) [:channelId :channelName
                                                        :authorUsername :content :reason])]
      (discord-io/start-agent-session!
       config
       job
       discord-ctx))))

(defn- fire-action!
  [config _trigger-ctx action-id]
  (js/console.warn "[trigger-runner] action dispatch not yet wired:" action-id))

(defn- target-contract-id
  [target]
  (some-> target
          str
          str/trim
          (str/replace #"^(pipeline|agent|action):" "")
          not-empty))

(defn- dispatch-target!
  [config trigger-ctx target]
  (let [contract-id (target-contract-id target)]
    (cond
      (nil? contract-id)
      (js/Promise.reject (js/Error. "Trigger target is blank"))

      (load-contract-sync config "pipelines" contract-id)
      (fire-pipeline! config trigger-ctx contract-id)

      (load-contract-sync config "actions" contract-id)
      (fire-action! config trigger-ctx contract-id)

      (load-contract-sync config "agents" contract-id)
      (fire-agent! config trigger-ctx contract-id)

      :else
      (js/Promise.reject (js/Error. (str "Unknown trigger target: " target))))))

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
                  (when-let [contract (load-contract-sync config "triggers" trigger-id)]
                    (when (:enabled contract)
                      (-> (js/Promise.resolve
                           (dispatch-target! config (:data/context contract) (:trigger/target contract)))
                          (.catch (fn [err]
                                    (js/console.error "[trigger-runner] failed to fire" trigger-id err))))))))
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
        (when-let [contract (load-contract-sync config "triggers" trigger-id)]
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
          contract (load-contract-sync config "triggers" trigger-id)]
     (if contract
        (let [target (:trigger/target contract)
              ctx'   (merge (:data/context contract) ctx)]
          (-> (js/Promise.resolve (dispatch-target! config ctx' target))
              (.then (fn [_] {:fired trigger-id}))))
       (js/Promise.reject (js/Error. (str "Unknown trigger: " trigger-id)))))))

(defn status
  "Return a snapshot of all trigger states."
  []
  (let [config (cfg)]
    {:running @running?*
     :triggers (->> (loader/list-contract-ids-sync config "triggers")
                   (mapv (fn [id]
                            (when-let [c (load-contract-sync config "triggers" id)]
                              (select-keys c [:contract/id :trigger/kind :trigger/target :enabled])))))}))
