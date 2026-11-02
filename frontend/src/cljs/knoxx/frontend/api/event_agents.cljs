(ns knoxx.frontend.api.event-agents
  "CLJS-native API client for event-agent admin endpoints.

   Wraps the JS bridge and converts at the boundary:
   - Responses:  js->clj  (keyword keys)
   - Requests:   clj->js   (before fetch)

   The rest of the frontend works with plain CLJS data."
  (:require ["@open-hax/knoxx-frontend-bridge" :as bridge]))

(defn- kw [js-obj]
  (js->clj js-obj :keywordize-keys true))

;; ---------------------------------------------------------------------------
;; Discord config
;; ---------------------------------------------------------------------------

(defn get-discord-config []
  (-> (bridge/getDiscordConfig)
      (.then kw)))

(defn update-discord-config [token]
  (-> (bridge/updateDiscordConfig token)
      (.then kw)))

;; ---------------------------------------------------------------------------
;; Event agent control
;; ---------------------------------------------------------------------------

(defn get-event-agent-control []
  (-> (bridge/getEventAgentControl)
      (.then kw)))

(defn update-event-agent-control [control]
  (-> (bridge/updateEventAgentControl (clj->js control))
      (.then kw)))

(defn run-event-agent-job [job-id]
  (-> (bridge/runEventAgentJob job-id)
      (.then kw)))

(defn fire-trigger [trigger-id]
  (-> (bridge/fireTrigger trigger-id)
      (.then kw)))

(defn dispatch-event-agent-event [event]
  (-> (bridge/dispatchEventAgentEvent (clj->js event))
      (.then kw)))

(defn stop-event-agent-runtime []
  (-> (bridge/stopEventAgentRuntime)
      (.then kw)))

(defn start-event-agent-runtime []
  (-> (bridge/startEventAgentRuntime)
      (.then kw)))

(defn reset-event-agent-runtime []
  (-> (bridge/resetEventAgentRuntime)
      (.then kw)))

;; ---------------------------------------------------------------------------
;; Admin tools
;; ---------------------------------------------------------------------------

(defn list-admin-tools []
  (-> (bridge/listAdminTools)
      (.then kw)))
