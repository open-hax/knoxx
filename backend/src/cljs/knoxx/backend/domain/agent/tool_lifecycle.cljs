(ns knoxx.backend.domain.agent.tool-lifecycle
  "Pure transformations for provider tool lifecycle events.")

(defn start-receipt
  [receipt {:keys [tool-name input-raw input-preview at]}]
  (cond-> (merge receipt {:tool_name tool-name
                          :status "running"
                          :started_at (or (:started_at receipt) at)})
    (some? input-raw) (assoc :input input-raw)
    input-preview (assoc :input_preview input-preview)))

(defn update-receipt
  [receipt {:keys [tool-name preview append-preview]}]
  (cond-> (merge receipt {:tool_name tool-name
                          :status "running"})
    preview (update :updates (or append-preview conj) preview)))

(defn end-receipt
  [receipt {:keys [tool-name is-error result-raw result-preview content-parts at]}]
  (cond-> (merge receipt {:tool_name tool-name
                          :status (if is-error "failed" "completed")
                          :ended_at at
                          :is_error (boolean is-error)})
    (some? result-raw) (assoc :result result-raw)
    result-preview (assoc :result_preview result-preview)
    (seq content-parts) (assoc :content_parts content-parts)))

(defn trace-event
  [phase {:keys [tool-name tool-call-id input-preview preview result-preview is-error at]}]
  (case phase
    :start {:type "tool_start"
            :tool_name tool-name
            :tool_call_id tool-call-id
            :preview input-preview
            :at at}
    :update {:type "tool_update"
             :tool_name tool-name
             :tool_call_id tool-call-id
             :preview preview
             :at at}
    :end {:type "tool_end"
          :tool_name tool-name
          :tool_call_id tool-call-id
          :preview result-preview
          :is_error is-error
          :at at}))

(defn run-event-extra
  [phase {:keys [tool-name tool-call-id input-preview preview result-preview is-error count streak]}]
  (case phase
    :start {:status "running"
            :tool_name tool-name
            :tool_call_id tool-call-id
            :preview input-preview}
    :update {:status "running"
             :tool_name tool-name
             :tool_call_id tool-call-id
             :preview preview}
    :end {:status (if is-error "failed" "completed")
          :tool_name tool-name
          :tool_call_id tool-call-id
          :is_error (boolean is-error)
          :preview result-preview}
    :death-spiral {:status "failed"
                   :tool_name tool-name
                   :tool_call_id tool-call-id
                   :count count
                   :streak streak}))
