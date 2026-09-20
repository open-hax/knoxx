(ns knoxx.frontend.admin.event-agent-commands
  "Awaited runtime commands preserving editable control and visible refusal state."
  (:require [clojure.string :as str]
            [knoxx.frontend.admin.event-agent-utils :as u]
            [knoxx.frontend.api.event-agents :as api]))

(defn- error-message [error] (or (.-message error) (:message error) (str error)))
(defn- install-control! [updated set-status set-draft set-json-drafts]
  (set-status updated) (set-draft (:control updated))
  (set-json-drafts (u/seed-json-drafts (get-in updated [:control :jobs]))))

(defn- ^:async perform! [set-busy idle set-error set-notice operation]
  (set-busy true) (set-error "") (set-notice nil)
  (try (await (operation))
       (catch :default error (set-notice {:tone :error :text (error-message error)}))
       (finally (set-busy idle))))

(defn ^:async load-data
  "Read token metadata and control together, always releasing the loading state."
  [set-loading set-error set-notice set-status set-draft set-draft-token set-json-drafts set-event-source-kind]
  (set-loading true) (set-error "") (set-notice nil)
  (try
    (let [results (await (js/Promise.all (array (api/get-discord-config) (api/get-event-agent-control))))
          token (aget results 0) control (aget results 1)
          merged (merge control (select-keys token [:configured :tokenPreview]))
          sources (:availableSourceKinds merged)]
      (install-control! merged set-status set-draft set-json-drafts)
      (set-draft-token "")
      (set-event-source-kind (if (some #{"github"} sources) "github" (or (first sources) "manual"))))
    (catch :default error (set-error (error-message error)))
    (finally (set-loading false))))

(defn- deep-merge [left right]
  (merge-with (fn [a b] (if (and (map? a) (map? b)) (deep-merge a b) b)) left right))

(defn update-job
  "Merge an edited job field while retaining untouched nested configuration."
  [draft set-draft job-id patch]
  (when draft
    (set-draft (update draft :jobs #(mapv (fn [job] (if (= (:id job) job-id) (deep-merge job patch) job)) %)))))

(defn ^:async handle-save-token
  "Save nonblank token input and refresh the configuration."
  [reload-data can-manage draft-token set-saving-token set-notice set-error set-status set-draft-token]
  (when can-manage
    (let [normalized (str/trim draft-token)]
      (if (empty? normalized) (set-error "Bot token must not be blank")
          (await (perform! set-saving-token false set-error set-notice
                   (fn ^:async execute-command []
                     (let [updated (await (api/update-discord-config normalized))]
                       (set-status #(when % (merge % (select-keys updated [:configured :tokenPreview]))))
                       (set-draft-token "") (await (reload-data))
                       (set-notice {:tone :success :text (str "Discord bot token saved. Preview: " (:tokenPreview updated))})))))))))

(defn- parse-json-draft [job label value]
  (try (js->clj (js/JSON.parse value) :keywordize-keys true)
       (catch :default error
         (throw (js/Error. (str "Invalid " label " JSON for job " (:name job) ": " (error-message error)))))))

(defn- parse-job [json-drafts job]
  (let [drafts (or (get json-drafts (:id job))
                   {:source-config (u/pretty-json (or (get-in job [:source :config]) {}))
                    :filters (u/pretty-json (or (:filters job) {}))
                    :tool-policies (u/pretty-json (or (get-in job [:agentSpec :toolPolicies]) []))})]
    (-> job
        (assoc-in [:source :config] (parse-json-draft job "source config" (:source-config drafts)))
        (assoc :filters (parse-json-draft job "filters" (:filters drafts)))
        (assoc-in [:agentSpec :toolPolicies] (parse-json-draft job "tool policy" (:tool-policies drafts))))))

(defn parse-control-for-save
  "Validate editable JSON fragments before constructing the existing control body."
  [draft json-drafts]
  (when-not draft (throw (js/Error. "No draft control loaded")))
  (update draft :jobs #(mapv (partial parse-job json-drafts) %)))

(defn update-json-draft
  "Retain other editable JSON fields when replacing one fragment."
  [set-json-drafts json-drafts job-id field value]
  (set-json-drafts (update json-drafts job-id #(assoc (or % {:source-config "{}" :filters "{}" :tool-policies "[]"}) field value))))

(defn ^:async handle-save-control
  "Persist validated control; a refusal leaves the current draft intact."
  [set-error set-notice set-status set-draft set-json-drafts set-saving-control can-manage draft json-drafts]
  (when (and can-manage draft)
    (await (perform! set-saving-control false set-error set-notice
             (fn ^:async execute-command []
               (let [updated (await (api/update-event-agent-control (parse-control-for-save draft json-drafts)))]
                 (install-control! updated set-status set-draft set-json-drafts)
                 (set-notice {:tone :success :text "Event-agent control plane updated and runtime reloaded."})))))))

(defn ^:async handle-run-job
  "Queue an authorized job and release the selected job's busy indicator."
  [reload-data set-error set-notice can-manage set-running-job-id job-id]
  (when can-manage
    (await (perform! #(set-running-job-id (when % job-id)) false set-error set-notice
             (fn ^:async execute-command []
               (await (api/run-event-agent-job job-id)) (await (reload-data))
               (set-notice {:tone :success :text (str "Queued job " job-id ".")}))))))

(defn ^:async handle-dispatch-event
  "Dispatch an authorized source event after parsing its payload."
  [reload-data set-error set-notice can-manage set-dispatching-event event-payload event-source-kind event-kind]
  (when can-manage
    (await (perform! set-dispatching-event false set-error set-notice
             (fn ^:async execute-command []
               (let [payload (js->clj (js/JSON.parse (or event-payload "{}")) :keywordize-keys true)
                     result (await (api/dispatch-event-agent-event {:sourceKind event-source-kind :eventKind event-kind :payload payload}))]
                 (await (reload-data))
                 (set-notice {:tone :success :text (str "Dispatched " event-source-kind ":" event-kind ". Matched jobs: "
                                                       (if (seq (:matchedJobs result)) (str/join ", " (:matchedJobs result)) "none") ".")})))))))

(defn ^:async handle-stop-runtime
  "Stop scheduling and display the returned durable control."
  [set-status set-draft set-json-drafts set-toggling-runtime set-error set-notice can-manage]
  (when can-manage
    (await (perform! set-toggling-runtime false set-error set-notice
             (fn ^:async execute-command []
               (install-control! (await (api/stop-event-agent-runtime)) set-status set-draft set-json-drafts)
               (set-notice {:tone :success :text "Event-agent runtime stopped (schedulers cleared)."}))))))

(defn ^:async handle-start-runtime
  "Start scheduling, refresh the runtime and preserve the success notice."
  [reload-data set-status set-draft set-json-drafts set-toggling-runtime set-error set-notice can-manage]
  (when can-manage
    (await (perform! set-toggling-runtime false set-error set-notice
             (fn ^:async execute-command []
               (install-control! (await (api/start-event-agent-runtime)) set-status set-draft set-json-drafts)
               (await (reload-data))
               (set-notice {:tone :success :text "Event-agent runtime started."}))))))

(defn ^:async handle-reset-runtime
  "Reset durable state and leave scheduling stopped for human review."
  [set-status set-draft set-json-drafts set-resetting-runtime set-error set-notice can-manage]
  (when can-manage
    (await (perform! set-resetting-runtime false set-error set-notice
             (fn ^:async execute-command []
               (let [updated (await (api/reset-event-agent-runtime))]
                 (install-control! updated set-status set-draft set-json-drafts)
                 (set-notice {:tone :success :text (str "Event-agent runtime reset. Cleared " (get-in updated [:reset :deletedCount])
                                                       " persisted state key(s) and disabled " (get-in updated [:reset :disabledCronJobCount])
                                                       " cron job(s). Review schedules before restarting.")})))))))
