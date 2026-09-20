(ns knoxx.frontend.api.event-agents
  "Native event-agent HTTP client preserving the established admin endpoint wire."
  (:require [knoxx.frontend.lib.api :as api]))

(defn get-discord-config "Read configured token metadata." []
  (api/request "/api/admin/config/discord"))
(defn update-discord-config "Write a token through the existing Discord configuration contract." [token]
  (api/request "/api/admin/config/discord" {:method "PUT" :body {:discordBotToken token}}))
(defn get-event-agent-control "Read runtime and editable event-agent control." []
  (api/request "/api/admin/config/events"))
(defn update-event-agent-control "Replace control with its established direct JSON body." [control]
  (api/request "/api/admin/config/events" {:method "PUT" :body control}))
(defn run-event-agent-job "Queue a named job using a safely encoded path segment." [job-id]
  (api/request (str "/api/admin/config/events/jobs/" (js/encodeURIComponent job-id) "/run") {:method "POST"}))
(defn fire-trigger "Fire a named trigger through its existing control route." [trigger-id]
  (api/request (str "/api/admin/triggers/" (js/encodeURIComponent trigger-id) "/fire") {:method "POST"}))
(defn dispatch-event-agent-event "Dispatch source kind, event kind and payload without rewrapping them." [event]
  (api/request "/api/admin/config/events/dispatch" {:method "POST" :body event}))
(defn stop-event-agent-runtime "Stop the runtime scheduler." []
  (api/request "/api/admin/config/events/runtime/stop" {:method "POST"}))
(defn start-event-agent-runtime "Start the runtime scheduler." []
  (api/request "/api/admin/config/events/runtime/start" {:method "POST"}))
(defn reset-event-agent-runtime "Reset persisted runtime state and cron enablement." []
  (api/request "/api/admin/config/events/runtime/reset" {:method "POST"}))

(defn- string-value [record fields fallback]
  (let [field (first (filter #(contains? record %) fields)) value (get record field)]
    (if (string? value) value fallback)))
(defn- tool-definition [record]
  (let [id (string-value record [:id] "")]
    {:id id :label (string-value record [:label] id) :description (string-value record [:description] "")
     :riskLevel (string-value record [:riskLevel :risk-level :risk_level] "standard")}))
(defn ^:async list-admin-tools "Normalize tool summaries with the existing TypeScript client defaults." []
  (let [response (await (api/request "/api/admin/tools")) tools (:tools response)]
    {:tools (if (sequential? tools) (mapv tool-definition (filter map? tools)) [])}))
