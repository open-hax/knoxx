(ns knoxx.backend.infra.routes.app.session-views
  "Project active run and session snapshots for operator views."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.infra.auth.authz :as auth-authz]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]))

(defn- active-run-measurements
  [run session conversation-id]
    {:run_id (:run_id run)
     :session_id (:session_id run)
     :conversation_id (:conversation_id run)
     :status (:status run)
     :model (:model run)
     :created_at (:created_at run)
     :updated_at (:updated_at run)
     :ttft_ms (:ttft_ms run)
     :total_time_ms (:total_time_ms run)
     :input_tokens (:input_tokens run)
     :output_tokens (:output_tokens run)
     :tokens_per_s (:tokens_per_s run)
     :error (:error run)
     :event_count (count (or (:events run) []))
     :tool_receipt_count (count (or (:tool_receipts run) []))
     :has_active_stream (boolean (:has_active_stream session))
     :active_turn_registered (boolean (and conversation-id (turn-control/active-turn conversation-id)))
     :agent_spec (get-in run [:settings :agentSpec])
     :resource_policies (get-in run [:resources :agentResourcePolicies])
})

(defn- active-run-summary
  [run session]
  (let [messages   (vec (or (:request_messages run) []))
        user-msg   (some #(when (= "user" (some-> (:role %) str/lower-case)) %) (reverse messages))
        conversation-id (:conversation_id run)]
    (merge (active-run-measurements run session conversation-id)
     {     :latest_user_message (:content user-msg)
     :content_parts (mapv (fn [p]
                            (-> p
                                (dissoc :data)
                                (select-keys [:type :url :mimeType :filename :text])))
                          (or (:content-parts user-msg) []))
     :tool_receipts (mapv (fn [r]
                            (select-keys r [:id :tool_name :status
                                            :input_preview :result_preview
                                            :started_at :ended_at]))
                          (or (:tool_receipts run) []))
     :trace_blocks (mapv (fn [b]
                           (select-keys b [:id :kind :status :toolName
                                           :toolCallId :content :at
                                           :inputPreview :outputPreview :isError]))
                         (or (:trace_blocks run) []))
     :latest_event (some-> (:events run) last (select-keys [:type :status :tool_name :preview :at]))})))

(defn- active-session-summary
  [session]
  (let [messages (vec (or (:messages session) []))
        user-msg (some #(when (= "user" (some-> (:role %) str/lower-case)) %) (reverse messages))
        conversation-id (:conversation_id session)]
    {:run_id (:run_id session)
     :session_id (:session_id session)
     :conversation_id conversation-id
     :status (:status session)
     :model (:model session)
     :created_at (:created_at session)
     :updated_at (:updated_at session)
     :error (:error session)
     :event_count 0
     :tool_receipt_count 0
     :has_active_stream (boolean (:has_active_stream session))
     :active_turn_registered (boolean (and conversation-id (turn-control/active-turn conversation-id)))
     :agent_spec (or (:agent_spec session) (:agentSpec session))
     :resource_policies (:resource_policies session)
     :latest_user_message (:content user-msg)
     :latest_event nil}))

(defn- live-session-items
  [run-items include-all? sessions]
  (let [run-session-ids (set (keep :session_id run-items))]
    (->> sessions
         (filter some?)
         (filter #(or include-all?
                      (contains? #{"queued" "running" "waiting_input"} (:status %))))
         (remove #(contains? run-session-ids (:session_id %)))
         (map active-session-summary)
         vec)))

(defn- active-item-time-ms
  [item]
  (let [value (or (:updated_at item) (:created_at item))]
    (cond
      (number? value) value
      (string? value) (let [parsed (js/Date.parse value)]
                        (if (js/isNaN parsed) 0 parsed))
      :else 0)))

(defn- sort-active-items
  [limit items]
  (->> items
       (sort-by active-item-time-ms #(compare %2 %1))
       (take limit)
       vec))

(defn ^:async live-active-agent-summaries!
  "Combine active in-memory runs and persisted session summaries in recency order."
  [limit include-all?]
  (let [limit (max 1 (or limit 25))
         sessions-by-id (into {}
                             (map (fn [session]
                                    [(:session_id session) session]))
                             (session-store/active-session-snapshots))
        run-items (->> @run-state/run-order*
                       (map #(get @run-state/runs* %))
                       (filter some?)
                       (filter #(contains? #{"queued" "running" "waiting_input"} (:status %)))
                       (map (fn [run]
                              (active-run-summary run (get sessions-by-id (:session_id run)))))
                                               vec)]
    (sort-active-items limit (concat run-items (live-session-items run-items include-all? (await (session-store/list-active-sessions)))))))

(defn build-active-runs
  "Project active runs visible to the authenticated context." [ctx limit]
  (let [sessions-by-id (into {}
                             (map (fn [session]
                                    [(:session_id session) session]))
                             (session-store/active-session-snapshots))]
    (->> @run-state/run-order*
         (map #(get @run-state/runs* %))
         (filter some?)
         (filter #(contains? #{"queued" "running" "waiting_input"} (:status %)))
         (filter #(auth-authz/run-visible? ctx %))
         (map (fn [run]
                (active-run-summary run (get sessions-by-id (:session_id run)))))
         (take limit)
         vec)))
