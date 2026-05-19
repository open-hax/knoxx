(ns knoxx.backend.infra.pipeline-runner
  "Executes :pipeline contracts. Runs steps in dependency order,
   interpolates {{memory.temp:key}} in :context, writes :output to temp memory."
  (:require [promesa.core :as p]
            [clojure.string :as str]
            [knoxx.backend.tools.temp-memory :as temp]
            [knoxx.backend.contracts.loader :as loader]
            [knoxx.backend.domain.discord.discord-io :as discord-io]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.models :as models]))

(defn- cfg [] (models/enrich-config (runtime-config/cfg)))

(defn- load-contract-sync
  [config contract-class contract-id]
  (let [klass (loader/normalize-contract-class contract-class)
        wanted-id (some-> contract-id str str/trim not-empty)]
    (some (fn [record]
            (when (and (= klass (:contractClass record))
                       (= wanted-id (:id record)))
              (:contract record)))
          (loader/load-all-contracts-sync config))))

;; ── async interpolation ─────────────────────────────

(defn- resolve-temps
  "Given a map m, find all {{memory.temp:k}} keys and resolve them.
   Returns Promise<{k value}>."
  [m]
  (let [re #"\{\{memory\.temp:([^}]+)\}\}"
        all-keys (->> m
                     vals
                     (keep (fn [v] (when (string? v)
                                     (->> (str/re-seq re v) (map second)))))
                     (apply concat)
                     distinct
                     vec)]
    (p/let [resolved (->> all-keys
                      (mapv (fn [k] (p/then (temp/get k) (fn [v] [k v]))))
                      (p/all))]
      (p/then resolved (fn [pairs] (into {} pairs))))))

(defn- interpolate-value
  "Replace {{memory.temp:k}} placeholders in string value."
  [value k->v]
  (if (and (string? value) (str/includes? value "{{memory.temp:"))
    (reduce (fn [s k]
              (if-let [v (get k->v k)]
                (str/replace s (str "{{memory.temp:" k "}}") (str v))
                s))
            value
            (keys k->v))
    value))

(defn- interpolate-map
  "Interpolate all {{memory.temp:}} in map m. k->v is {key value}."
  [m k->v]
  (->> m
       (mapv (fn [[k v]]
               [k (if (map? v)
                    (interpolate-map v k->v)
                    (interpolate-value v k->v))]))
       (into {})))

;; ── step execution ─────────────────────────────

(defn- run-action-step!
  "Execute an :action step."
  [_step]
  (js/console.log "[pipeline]" (:step/id _step) "action step")
  (p/let [_ (temp/set! (str "step:" (:step/id _step))
                      {:done true :step (:step/id _step)})]
    {:step-id (:step/id _step) :status "ok"}))

(defn- run-agent-step!
  "Execute an :agent step by starting a Knoxx agent session."
  [config step contract pipeline-ctx k->v]
  (let [output (get-in step [:step/data :output])]
    (js/console.log "[pipeline]" (:step/id step) "agent step" (:contract/id contract))
    (p/let [ctx (interpolate-map
                 (merge pipeline-ctx
                        (:context contract)
                         (get-in step [:step/data :context]))
                 k->v)
                result (discord-io/start-agent-session!
                       config
                       (assoc contract :task-prompt (get-in contract [:prompts :task]))
                       (select-keys ctx [:channelId :channelName
                                               :authorUsername :content :reason]))]
      (when output
        (temp/mem-set! (:key output) result {:ttl (:ttl output)}))
      {:step-id (:step/id step) :status "ok" :result result})))

(defn- run-step!
  "Execute one pipeline step. Returns Promise."
  [config step pipeline-ctx k->v]
  (let [contract-id (:step/contract step)]
    (if-let [contract (or (load-contract-sync config "actions" contract-id)
                           (load-contract-sync config "agents" contract-id))]
      (case (:contract/kind contract)
        :action (run-action-step! step)
        :agent  (run-agent-step! config step contract pipeline-ctx k->v)
        (do (js/console.warn "[pipeline] unknown step kind" (:contract/kind contract))
            (js/Promise.resolve {:step-id (:step/id step) :status "skip"})))
      (do (js/console.warn "[pipeline] step contract not found:" contract-id)
          (js/Promise.resolve {:step-id (:step/id step) :status "not-found"})))))

;; ── dependency ordering ─────────────────────

(defn- dependency-order
  "Naive topological sort: steps with fewer deps first."
  [steps]
  (->> steps
       (sort-by #(count (or (:step/depends-on %) [])))
       vec))

;; ── public API ─────────────────────────────

(defn run-pipeline!
  "Execute a :pipeline contract."
  [config pipeline-contract]
  (let [steps       (:pipeline/steps pipeline-contract)
        pipeline-ctx (merge (:context pipeline-contract)
                           (:trigger-ctx pipeline-contract))
        ordered     (dependency-order steps)]
    (js/console.log "[pipeline] running" (:contract/id pipeline-contract)
                  "steps:" (count ordered))
    (p/let [k->v (resolve-temps pipeline-ctx)]
      (p/let [_ (reduce (fn [p step]
                           (p/then p (fn [_] (run-step! config step pipeline-ctx k->v))))
                        (js/Promise.resolve nil)
                        ordered)
              output (:output pipeline-contract)]
        (when output
          (temp/set! (:key output)
                     {:done true :steps (count ordered)}
                     {:ttl (:ttl output)}))
        {:pipeline-id (:contract/id pipeline-contract)
         :steps-run  (count ordered)
         :status      "ok"}))))
