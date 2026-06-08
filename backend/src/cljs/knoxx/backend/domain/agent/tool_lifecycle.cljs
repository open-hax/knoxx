(ns knoxx.backend.domain.agent.tool-lifecycle
  "Pure transformations for provider tool lifecycle events."
  (:require [clojure.string :as str]))

(def empty-tool-call-id-state
  "Per-turn occurrence tracking for provider tool-call ids."
  {:counts {} :active {}})

(defn- tool-call-alias-key
  [tool-name tool-call-id]
  (or (some-> tool-call-id str str/trim not-empty)
      (str "tool:" (or (some-> tool-name str str/trim not-empty) "tool"))))

(defn resolve-tool-call-start-id
  "Uniquify a provider tool-call id at tool_execution_start.

   Providers can reuse index-based ids across rounds (proxx/ollama emit call_0
   for the first tool call of every round) or omit ids entirely. Receipts,
   lifecycle events, and trace blocks keyed by the raw id then collapse onto
   the first call. Returns {:state <next id-state> :tool-call-id <unique id>}:
   the raw id for its first occurrence, raw#N for repeats, and tool:<name>#N
   when the provider sent no id."
  [id-state {:keys [tool-name tool-call-id]}]
  (let [raw (some-> tool-call-id str str/trim not-empty)
        alias-key (tool-call-alias-key tool-name tool-call-id)
        n (inc (get-in id-state [:counts alias-key] 0))
        unique-id (cond
                    (and raw (= n 1)) raw
                    raw (str raw "#" n)
                    :else (str alias-key "#" n))]
    {:state (-> id-state
                (assoc-in [:counts alias-key] n)
                (assoc-in [:active alias-key] unique-id))
     :tool-call-id unique-id}))

(defn active-tool-call-id
  "Resolve a raw provider tool-call id (or bare tool name) to the unique id
   registered by its most recent tool start, or nil when none started."
  [id-state {:keys [tool-name tool-call-id]}]
  (get-in id-state [:active (tool-call-alias-key tool-name tool-call-id)]))

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
