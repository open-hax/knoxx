(ns knoxx.backend.domain.action.run-state
  "Process-owned run registry; pure trace projections live in run-trace."
  (:require [knoxx.backend.domain.action.run-trace :as trace]
            [knoxx.backend.domain.time :as time]
            [knoxx.backend.extern.run-record :as run-record]
            [knoxx.backend.shape.agent :as agent]))

(defonce ^{:doc "Process-owned retained run records."}
  runs* (atom {}))
(defonce ^{:doc "Most recent run IDs in retention order."}
  run-order* (atom []))
(defonce ^{:doc "Process-owned bounded retrieval timing samples."}
  retrieval-stats* (atom {:samples []
                                 :avgRetrievalMs 0
                                 :p95RetrievalMs 0
                                 :recentSamples 0
                                 :modeCounts {:dense 0 :hybrid 0 :hybrid_rerank 0}}))


(defonce ^{:doc "Optional best-effort streaming observer for run events."}
  event-stream-sink* (atom nil))
(defonce ^:private durable-event-sink* (atom nil))

(defn set-durable-event-sink!
  "Install the application event admission hook independently of telemetry."
  [sink]
  (reset! durable-event-sink* sink))

(defn set-event-stream-sink!
  "Register a 1-arity fire-and-forget fn called with each event as it is appended.
   Intended for streaming events to OpenPlanner during an active run."
  [f]
  (reset! event-stream-sink* f))

(defn clear-event-stream-sink!
  "Remove the best-effort run event observer."
  []
  (reset! event-stream-sink* nil))
(defn latest-assistant-message
  "Return the latest assistant message from an opaque agent session."
  [session]
  (let [msgs (or (agent/messages session) [])]
    (last (filter #(= (aget % "role") "assistant") msgs))))

(defn usage-map
  "Project native agent usage counters into run record fields."
  [usage]
  (when usage
    {:input_tokens (or (aget usage "input") 0)
     :output_tokens (or (aget usage "output") 0)}))

(def MAX_RUNS
  "Maximum retained run records." 200)

(defn store-run!
  "Store a normalized run and keep heap keys bounded by the retained run order."
  [run-id run]
  (let [clean (run-record/normalize-run run)]
    (swap! runs* assoc run-id clean)
    (let [live-order (swap! run-order*
                            (fn [order]
                              (->> (cons run-id (remove #{run-id} order))
                                   (take MAX_RUNS)
                                   vec)))]
      (when (> (count @runs*) MAX_RUNS)
        (swap! runs* #(select-keys % live-order))))
    clean))

(defn summarize-run
  "Select the stable fields exposed by run summary responses."
  [run]
  (select-keys run [:run_id :created_at :updated_at :status :model :ttft_ms :total_time_ms :input_tokens :output_tokens :tokens_per_s :error]))

(defn append-limited
  "Append one value and retain the most recent limit entries."
  [items item limit]
  (trace/append-limited items item limit))

(defn update-run!
  "Transform an existing retained run and return its new value."
  [run-id f]
  (let [state (swap! runs* update run-id (fn [run]
                                           (when run
                                             (f run))))]
    (get state run-id)))

(defn append-run-event!
  "Admit a durable event before projecting and notifying best-effort observers."
  [run-id event]
  (when-let [sink @durable-event-sink*] (sink event))
  (update-run! run-id
               (fn [run]
                 (-> run
                     (assoc :updated_at (time/now-iso))
                     (update :events #(append-limited % event 200)))))
  ;; Stream event to OpenPlanner as it happens — fire-and-forget
  (when-let [sink @event-stream-sink*]
    (try (sink event) (catch :default _ nil))))

(defn append-run-trace-text!
  "Append a text delta, coalescing an adjacent streaming block."
  [run-id kind delta at]
  (when (seq (str delta))
    (update-run! run-id
                 (fn [run]
                   (update run :trace_blocks
                           (fn [blocks]
                             (let [items (vec blocks)
                                   last-block (peek items)]
                               (if (and last-block
                                        (= (:kind last-block) kind)
                                        (= (:status last-block) "streaming"))
                                 (assoc items (dec (count items))
                                        (-> last-block
                                            (update :content #(str (or % "") delta))
                                            (assoc :at (or at (:at last-block)))))
                                 (conj items {:id (str (name kind) ":" (count items))
                                              :kind kind
                                              :status "streaming"
                                              :content (str delta)
                                              :at at})))))))))

(defn apply-run-tool-trace-event!
  "Project one tool event into the retained run trace."
  [run-id event]
  (update-run! run-id #(update % :trace_blocks trace/apply-tool-event event)))

(defn finalize-run-trace-blocks!
  "Settle streaming trace blocks while preserving completed blocks."
  [run-id status]
  (update-run! run-id
               (fn [run]
                 (update run :trace_blocks
                         (fn [blocks]
                           (mapv (fn [block]
                                   (if (= (:status block) "streaming")
                                     (cond-> (assoc block :status status)
                                       (= status "error") (assoc :isError (or (:isError block)
                                                                               (= (:kind block) :tool_call))))
                                     block))
                                 (vec blocks)))))))

(defn update-run-tool-receipt!
  "Update a matching tool receipt or append a bounded new receipt."
  [run-id receipt-id default-receipt f]
  (update-run! run-id
               (fn [run]
                 (update run :tool_receipts
                         (fn [receipts]
                           (let [items (vec receipts)
                                 idx (first (keep-indexed (fn [i item]
                                                            (when (= (:id item) receipt-id)
                                                              i))
                                                          items))
                                 base (merge {:id receipt-id} default-receipt)]
                             (if (nil? idx)
                               (append-limited items (f base) 40)
                               (assoc items idx (f (merge base (nth items idx)))))))))))

(defn backfill-run-tool-input-preview!
  "Fill absent tool input observations without overwriting recorded values."
  [run-id receipt-id tool-name input-preview]
  (when (trace/valid-input-preview? receipt-id input-preview)
    (update-run! run-id #(trace/backfill-input-preview % receipt-id tool-name input-preview))))

(defn percentile-95
  "Compute the existing nearest-index retrieval timing percentile."
  [values]
  (if (seq values)
    (let [sorted (sort values)
          idx (js/Math.floor (* 0.95 (dec (count sorted))))]
      (nth sorted idx 0))
    0))

(defn record-retrieval-sample!
  "Retain bounded timing samples and update retrieval mode counters."
  [mode elapsed-ms]
  (swap! retrieval-stats*
         (fn [stats]
           (let [samples (->> (conj (vec (:samples stats)) elapsed-ms)
                              (take-last 100)
                              vec)
                 count-samples (count samples)
                 avg (if (pos? count-samples)
                       (/ (reduce + samples) count-samples)
                       0)
                 current-modes (or (:modeCounts stats) {:dense 0 :hybrid 0 :hybrid_rerank 0})]
             {:samples samples
              :avgRetrievalMs (js/Math.round avg)
              :p95RetrievalMs (js/Math.round (percentile-95 samples))
              :recentSamples count-samples
              :modeCounts (update current-modes (keyword (or mode "dense")) (fnil inc 0))}))))

(defn active-runs-count
  "Count retained queued and running work."
  []
  (->> @runs*
       vals
       (filter #(contains? #{"queued" "running"} (:status %)))
       count))

(defn get-run-events-since
  "Get run events that occurred after the given timestamp.
   Returns a promise resolving to a vector of events.
   Reads from the in-memory run state."
  [run-id since-timestamp]
  (let [run (get @runs* run-id)
        events (or (:events run) [])
        filtered (filter (fn [event]
                           (let [at (or (:at event) (aget event "at"))]
                             (and at
                                  (> (compare at since-timestamp) 0))))
                         events)]
    (js/Promise.resolve (vec filtered))))
