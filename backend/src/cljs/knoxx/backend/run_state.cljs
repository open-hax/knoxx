(ns knoxx.backend.run-state
  (:require [clojure.string :as str]
            [knoxx.backend.util.time :as time]
            [knoxx.backend.redis-client :as redis]))

(def RUN_EVENTS_KEY_PREFIX "knoxx:run_events:")
(def RUN_EVENTS_MAX 1000)
(def RUN_EVENTS_TTL 7200) ; 2 hours TTL for run event lists

(defn run-events-key
  [run-id]
  (str RUN_EVENTS_KEY_PREFIX run-id))

(defonce runs* (atom {}))
(defonce run-order* (atom []))
(defonce retrieval-stats* (atom {:samples []
                                 :avgRetrievalMs 0
                                 :p95RetrievalMs 0
                                 :recentSamples 0
                                 :modeCounts {:dense 0 :hybrid 0 :hybrid_rerank 0}}))


(defonce event-stream-sink* (atom nil))

(defn set-event-stream-sink!
  "Register a 1-arity fire-and-forget fn called with each event as it is appended.
   Intended for streaming events to OpenPlanner during an active run."
  [f]
  (reset! event-stream-sink* f))

(defn clear-event-stream-sink!
  []
  (reset! event-stream-sink* nil))
(defn latest-assistant-message
  [session]
  (let [messages (if (array? (aget session "messages"))
                   (array-seq (aget session "messages"))
                   [])]
    (last (filter #(= (aget % "role") "assistant") messages))))

(defn usage-map
  [usage]
  (when usage
    {:input_tokens (or (aget usage "input") 0)
     :output_tokens (or (aget usage "output") 0)}))

(defn- ensure-clj
  "Defensive: coerce raw JS arrays/objects back to CLJS data before
   storing in the in-memory run atom.  Redis round-trips and SDK
   interop can leak raw #js [] / #js {} into fields."
  [value]
  (cond
    (array? value) (js->clj value :keywordize-keys true)
    :else value))

(def MAX_RUNS 200)

(defn store-run!
  [run-id run]
  (let [clean (cond-> run
                (array? (:tool_receipts run))
                (update :tool_receipts ensure-clj)
                (array? (:trace_blocks run))
                (update :trace_blocks ensure-clj)
                (array? (:content_parts run))
                (update :content_parts ensure-clj)
                (array? (:events run))
                (update :events ensure-clj)
                (array? (:request_messages run))
                (update :request_messages ensure-clj)
                (array? (:resources run))
                (update :resources ensure-clj)
                (array? (:settings run))
                (update :settings ensure-clj))]
    (swap! runs* assoc run-id clean)
    (swap! run-order* (fn [order]
                        (let [new-order (->> (cons run-id (remove #{run-id} order))
                                             (take MAX_RUNS)
                                             vec)]
                          ;; Evict stale entries from runs* to prevent unbounded memory growth.
                          ;; run-order* is the canonical set of live run ids; anything not in it
                          ;; is a stale ghost that was trimmed and should be released.
                          (when (> (count order) MAX_RUNS)
                            (let [stale-ids (remove (set new-order) (keys @runs*))]
                              (when (seq stale-ids)
                                (swap! runs* #(apply dissoc % stale-ids)))))
                          new-order)))
    clean))

(defn summarize-run
  [run]
  (select-keys run [:run_id :created_at :updated_at :status :model :ttft_ms :total_time_ms :input_tokens :output_tokens :tokens_per_s :error]))

(defn append-limited
  [items item limit]
  (let [v (conj (vec items) item)]
    (if (> (count v) limit)
      (subvec v (- (count v) limit))
      v)))

(defn update-run!
  [run-id f]
  (let [state (swap! runs* update run-id (fn [run]
                                           (when run
                                             (f run))))]
    (get state run-id)))

(defn append-run-event!
  [run-id event]
  (update-run! run-id
               (fn [run]
                 (-> run
                     (assoc :updated_at (time/now-iso))
                     (update :events #(append-limited % event 200)))))
  ;; Persist event to Redis for crash recovery / WS reconnect replay
  (when-let [redis-client (redis/get-client)]
    (redis/lpush-json redis-client (run-events-key run-id) event)
    ;; Trim the list to prevent unbounded growth
    (try
      (.lTrim redis-client (run-events-key run-id) 0 (dec RUN_EVENTS_MAX))
      (.expire redis-client (run-events-key run-id) RUN_EVENTS_TTL)
      (catch :default _ nil)))
  ;; Stream event to OpenPlanner as it happens — fire-and-forget
  (when-let [sink @event-stream-sink*]
    (try (sink event) (catch :default _ nil))))

(defn- trace-tool-block-id
  [{:keys [tool_call_id tool_name at]}]
  (cond
    (and (string? tool_call_id) (seq tool_call_id)) (str "tool:" tool_call_id)
    (and (string? tool_name) (seq tool_name)) (str "tool:" tool_name ":" (or at ""))
    :else (str "tool:" (or at ""))))

(defn append-run-trace-text!
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
  [run-id {:keys [type tool_name tool_call_id preview is_error at]}]
  (update-run! run-id
               (fn [run]
                 (update run :trace_blocks
                         (fn [blocks]
                           (let [items (vec blocks)
                                 block-id (trace-tool-block-id {:tool_call_id tool_call_id
                                                                :tool_name tool_name
                                                                :at at})
                                 idx (first (keep-indexed (fn [i item]
                                                            (when (= (:id item) block-id) i))
                                                          items))
                                 existing (when (number? idx) (nth items idx))]
                             (cond
                               (= type "tool_start")
                               (let [block {:id block-id
                                            :kind :tool_call
                                            :toolName tool_name
                                            :toolCallId tool_call_id
                                            :inputPreview preview
                                            :status "streaming"
                                            :at at
                                            :updates []}]
                                 (if (number? idx)
                                   (assoc items idx (merge existing block))
                                   (conj items block)))

                               (= type "tool_update")
                               (if (number? idx)
                                 (assoc items idx
                                        (cond-> (assoc existing
                                                       :status "streaming"
                                                       :at (or at (:at existing)))
                                          (seq preview) (update :updates #(append-limited % preview 8))))
                                 (conj items {:id block-id
                                              :kind :tool_call
                                              :toolName tool_name
                                              :toolCallId tool_call_id
                                              :status "streaming"
                                              :at at
                                              :updates (cond-> [] (seq preview) (conj preview))}))

                               (= type "tool_end")
                               (let [block {:id block-id
                                            :kind :tool_call
                                            :toolName tool_name
                                            :toolCallId tool_call_id
                                            :status (if is_error "error" "done")
                                            :outputPreview preview
                                            :isError (boolean is_error)
                                            :at at}]
                                 (if (number? idx)
                                   (assoc items idx (merge existing block {:updates (:updates existing)
                                                                           :inputPreview (:inputPreview existing)}))
                                   (conj items (assoc block :updates []))))

                               :else items)))))))

(defn finalize-run-trace-blocks!
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

(defn- preview-present?
  [value]
  (when (string? value)
    (let [trimmed (str/trim value)
          lowered (str/lower-case trimmed)]
      (and (not (str/blank? trimmed))
           (not= lowered "null")
           (not= lowered "undefined")))))

(defn backfill-run-tool-input-preview!
  [run-id receipt-id tool-name input-preview]
  (when (and (string? receipt-id) (seq receipt-id) (preview-present? input-preview))
    (update-run! run-id
                 (fn [run]
                   (let [receipt-items (vec (:tool_receipts run))
                         receipt-idx (first (keep-indexed (fn [i receipt]
                                                            (when (= (:id receipt) receipt-id)
                                                              i))
                                                          receipt-items))
                         next-receipt (if (number? receipt-idx)
                                        (cond-> (nth receipt-items receipt-idx)
                                          (not (preview-present? (:input_preview (nth receipt-items receipt-idx))))
                                          (assoc :input_preview input-preview)
                                          (and (nil? (:input (nth receipt-items receipt-idx))))
                                          (assoc :input input-preview)
                                          (and (not (seq (:tool_name (nth receipt-items receipt-idx)))) (seq tool-name))
                                          (assoc :tool_name tool-name))
                                        {:id receipt-id
                                         :tool_name tool-name
                                         :status "running"
                                         :input input-preview
                                         :input_preview input-preview})
                         next-receipts (if (number? receipt-idx)
                                         (assoc receipt-items receipt-idx next-receipt)
                                         (append-limited receipt-items next-receipt 40))
                         block-items (vec (:trace_blocks run))
                         block-idx (first (keep-indexed (fn [i block]
                                                          (when (and (= (:kind block) :tool_call)
                                                                     (or (= (:toolCallId block) receipt-id)
                                                                         (= (:id block) (str "tool:" receipt-id))))
                                                            i))
                                                        block-items))
                         next-block (if (number? block-idx)
                                      (cond-> (nth block-items block-idx)
                                        (not (preview-present? (:inputPreview (nth block-items block-idx))))
                                        (assoc :inputPreview input-preview)
                                        (and (not (seq (:toolName (nth block-items block-idx)))) (seq tool-name))
                                        (assoc :toolName tool-name))
                                      {:id (str "tool:" receipt-id)
                                       :kind :tool_call
                                       :toolName tool-name
                                       :toolCallId receipt-id
                                       :status "streaming"
                                       :inputPreview input-preview
                                       :updates []})
                         next-blocks (if (number? block-idx)
                                       (assoc block-items block-idx next-block)
                                       (conj block-items next-block))]
                     (assoc run
                            :tool_receipts next-receipts
                            :trace_blocks next-blocks))))))

(defn tool-event-payload
  [run-id conversation-id session-id type extra]
  (merge {:run_id run-id
          :conversation_id conversation-id
          :session_id session-id
          :type type
          :at (time/now-iso)}
         extra))

(defn percentile-95
  [values]
  (if (seq values)
    (let [sorted (sort values)
          idx (js/Math.floor (* 0.95 (dec (count sorted))))]
      (nth sorted idx 0))
    0))

(defn record-retrieval-sample!
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
  []
  (->> @runs*
       vals
       (filter #(contains? #{"queued" "running"} (:status %)))
       count))

(defn get-run-events-since
  "Get run events from Redis that occurred after the given timestamp.
   Returns a promise resolving to a vector of events.
   Events are stored newest-first in Redis (LPUSH), so we reverse for chronological order."
  [redis-client run-id since-timestamp]
  (if-not redis-client
    (js/Promise.resolve [])
    (let [key (run-events-key run-id)]
      (-> (redis/lrange-json redis-client key 0 (dec RUN_EVENTS_MAX))
          (.then (fn [events]
                   (vec
                    (filter (fn [event]
                              (let [at (or (:at event) (aget event "at"))]
                                (and at
                                     (> (compare at since-timestamp) 0))))
                            (reverse events)))))
          (.catch (fn [_]
                    []))))))
