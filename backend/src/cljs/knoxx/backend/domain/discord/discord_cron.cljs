(ns knoxx.backend.domain.discord.discord-cron
  "Discord cron compatibility shim.
   Delegates all scheduling to trigger_runner.cljs.
   Keeps status-snapshot for admin dashboard backwards compatibility."
  (:require [clojure.string :as str]
            [knoxx.backend.triggers.trigger-runner :as trigger-runner]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.models :as runtime-models]))

(defn- cfg []
  (runtime-models/enrich-config (runtime-config/cfg)))

;; ─── backwards-compatible status API ──────────────────────────────────────

(defn status-snapshot
  [config]
  (let [triggers (trigger-runner/status)]
    {:running (:running triggers)
     :configured (not (str/blank? (:discord-bot-token config)))
     :channelCount 0
     :channels []
     :lastSeenChannels []
     :mentionQueueCount 0
     :jobs (->> (:triggers triggers)
                 (map (fn [t]
                        {:id (:contract/id t)
                         :name (:contract/id t)
                         :enabled (:enabled t)
                         :scheduleLabel (str (:trigger/kind t) " → " (:trigger/target t))}))
                 vec)}))

;; ─── lifecycle (delegated) ─────────────────────────────────────────────

(defn start!
  "Start all :trigger contracts via trigger-runner."
  [_config]
  (trigger-runner/start!))

(defn stop!
  "Stop all triggers via trigger-runner."
  []
  (trigger-runner/stop!))

(defn reload!
  []
  (stop!)
  (start! nil))

(defn run-job!
  "Manually fire a trigger by its contract id."
  [trigger-id]
  (trigger-runner/fire! trigger-id))
