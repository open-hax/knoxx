(ns knoxx.frontend.components.agent-audit.logic
  "Pure merge/scoring/matching logic for the agent audit session list.
   CLJS port of the utilities in
   src/components/agent-audit/AgentAuditSessionList.tsx."
  (:require [clojure.string :as str]))

(defn normalize-search [value]
  (-> (or value "") str/trim str/lower-case))

(defn format-maybe-date [value]
  (when (some? value)
    (let [date (js/Date. value)]
      (if (js/Number.isNaN (.getTime date))
        (str value)
        (.toLocaleString date)))))

(defn spec-string [spec ks]
  (some (fn [k]
          (let [v (get spec k)]
            (when (and (string? v) (not (str/blank? v)))
              (str/trim v))))
        ks))

(defn- run-spec [run] (or (:agent_spec run) {}))

(defn- run-string [run ks] (spec-string (run-spec run) ks))

(defn run-sub-agent-id [run] (run-string run [:subAgentId :sub_agent_id :sub-agent-id]))
(defn run-parent-agent-id [run] (run-string run [:parentAgentId :parent_agent_id :parent-agent-id]))
(defn run-role [run] (run-string run [:role]))
(defn run-contract-id [run] (run-string run [:contractId :contract_id :contract-id]))
(defn run-actor-id [run] (run-string run [:actorId :actor_id :actor-id]))
(defn run-trigger-id [run] (run-string run [:triggerId :trigger_id :trigger-id]))
(defn run-event-type [run]
  (run-string run [:eventType :event_type :event-type
                   :triggerEventType :trigger_event_type :trigger-event-type]))
(defn run-event-id [run] (run-string run [:eventId :event_id :event-id]))
(defn run-event-scope-id [run] (run-string run [:eventScopeId :event_scope_id :event-scope-id]))
(defn run-schedule-id [run] (run-string run [:scheduleId :schedule_id :schedule-id]))

(defn run-event-types [run]
  (let [spec (run-spec run)
        values (or (:eventTypes spec) (:event_types spec) (get spec :event-types))]
    (when (sequential? values)
      (let [normalized (filterv #(and (string? %) (seq (str/trim %))) values)]
        (when (seq normalized) normalized)))))

(defn run-title [run]
  (if-let [sub-agent-id (run-sub-agent-id run)]
    (str "sub-agent " sub-agent-id)
    (or (run-contract-id run) (run-role run)
        (:conversation_id run) (:session_id run) (:run_id run))))

(defn session-timestamp [session]
  (let [parsed (js/Date.parse (or (:last_ts session) ""))]
    (if (js/Number.isFinite parsed) parsed 0)))

(defn activity-score [session]
  (cond
    (:has_active_stream session) 50
    (= "running" (:active_status session)) 45
    (= "queued" (:active_status session)) 40
    (= "waiting_input" (:active_status session)) 35
    (:is_active session) 30
    (= "failed" (:active_status session)) 20
    :else 0))

(defn- contract-target [contract-id]
  (let [target (some-> contract-id str/trim)]
    (when (and (seq target) (not= target "new-agent"))
      target)))

(defn session-matches-contract? [session contract-id]
  (if-let [target (contract-target contract-id)]
    (boolean (or (= (:contract_id session) target)
                 (= (:sub_agent_id session) target)
                 (= (:parent_agent_id session) target)
                 (some #{target} (:contract_actors session))))
    true))

(defn run-matches-contract? [run contract-id]
  (if-let [target (contract-target contract-id)]
    (boolean (or (= (run-contract-id run) target)
                 (= (run-sub-agent-id run) target)
                 (= (run-parent-agent-id run) target)
                 (= (run-role run) target)))
    true))

(defn active-run->session
  "Live audit-session card source for an active run; nil without any id."
  [run]
  (when-let [session (or (:conversation_id run) (:session_id run) (:run_id run))]
    (let [contract-id (run-contract-id run)
          sub-agent-id (run-sub-agent-id run)
          parent-agent-id (run-parent-agent-id run)]
      {:auditSource "active"
       :project "knoxx-session"
       :session session
       :title (run-title run)
       :last_ts (or (:updated_at run) (:created_at run))
       :event_count 0
       :actor_id (or (run-actor-id run) sub-agent-id parent-agent-id (run-role run))
       :contract_id (or contract-id sub-agent-id)
       :sub_agent_id sub-agent-id
       :parent_agent_id parent-agent-id
       :is_active true
       :active_status (:status run)
       :has_active_stream (:has_active_stream run)
       :active_session_id (:session_id run)
       :run_id (:run_id run)
       :model (:model run)
       :latest_user_message (:latest_user_message run)
       :trigger_id (run-trigger-id run)
       :event_type (run-event-type run)
       :event_types (run-event-types run)
       :event_id (run-event-id run)
       :event_scope_id (run-event-scope-id run)
       :schedule_id (run-schedule-id run)})))

(defn- merge-session [left right]
  (let [live (if (>= (activity-score right) (activity-score left)) right left)
        history (cond
                  (= "history" (:auditSource left)) left
                  (= "history" (:auditSource right)) right
                  :else left)]
    (merge history live
           {:title (or (:title live) (:title history))
            :last_ts (if (>= (session-timestamp live) (session-timestamp history))
                       (:last_ts live)
                       (:last_ts history))
            :event_count (max (or (:event_count left) 0) (or (:event_count right) 0))
            :contract_id (or (:contract_id live) (:contract_id history))
            :contract_actors (or (:contract_actors live) (:contract_actors history))
            :actor_id (or (:actor_id live) (:actor_id history))
            :auditSource (if (or (= "active" (:auditSource live))
                                 (= "active" (:auditSource history)))
                           "active" "history")
            :is_active (boolean (or (:is_active left) (:is_active right)))
            :has_active_stream (boolean (or (:has_active_stream left) (:has_active_stream right)))
            :active_session_id (or (:active_session_id live) (:active_session_id history))
            :trigger_id (or (:trigger_id live) (:trigger_id history))
            :event_type (or (:event_type live) (:event_type history))
            :event_types (or (:event_types live) (:event_types history))
            :event_id (or (:event_id live) (:event_id history))
            :event_scope_id (or (:event_scope_id live) (:event_scope_id history))
            :schedule_id (or (:schedule_id live) (:schedule_id history))})))

(defn- session-order [left right]
  (let [score-diff (- (activity-score right) (activity-score left))]
    (if (not= 0 score-diff)
      score-diff
      (let [time-diff (- (session-timestamp right) (session-timestamp left))]
        (cond
          (pos? time-diff) 1
          (neg? time-diff) -1
          :else (.localeCompare (or (:title left) (:session left))
                                (or (:title right) (:session right))))))))

(defn- merge-into-index [by-id session]
  (update by-id (:session session)
          #(if % (merge-session % session) session)))

(defn merge-sessions
  "Merges memory sessions and active runs into one active-first list,
   scoped to `contract-id` when given."
  [memory-sessions active-runs contract-id]
  (let [with-history (reduce (fn [by-id session]
                               (if (session-matches-contract? session contract-id)
                                 (assoc by-id (:session session)
                                        (assoc session :auditSource
                                               (if (:is_active session) "active" "history")))
                                 by-id))
                             {} memory-sessions)
        with-active (reduce (fn [by-id run]
                              (if (run-matches-contract? run contract-id)
                                (if-let [session (active-run->session run)]
                                  (merge-into-index by-id session)
                                  by-id)
                                by-id))
                            with-history active-runs)]
    (vec (sort session-order (vals with-active)))))

(defn merge-session-pages
  "Folds a newly loaded page into the current rows, deduping on session id."
  [current next-page]
  (->> (concat current next-page)
       (reduce merge-into-index {})
       vals
       (sort session-order)
       vec))

(defn session-status [session]
  (cond
    (:has_active_stream session) {:label "Live" :variant :warning}
    (= "running" (:active_status session)) {:label "Active" :variant :success}
    (= "waiting_input" (:active_status session)) {:label "Waiting" :variant :info}
    (= "failed" (:active_status session)) {:label "Failed" :variant :error}
    (:is_active session) {:label "Active" :variant :success}
    :else {:label "History" :variant :default}))

(defn session-search-text [session]
  (-> (str/join " "
                [(:session session) (or (:title session) "")
                 (or (:actor_id session) "") (or (:contract_id session) "")
                 (or (:sub_agent_id session) "") (or (:parent_agent_id session) "")
                 (or (:model session) "") (or (:latest_user_message session) "")
                 (or (:trigger_id session) "") (or (:event_type session) "")
                 (or (:event_id session) "") (or (:event_scope_id session) "")
                 (or (:schedule_id session) "")
                 (str/join " " (or (:event_types session) []))
                 (str/join " " (or (:contract_actors session) []))])
      str/lower-case))
