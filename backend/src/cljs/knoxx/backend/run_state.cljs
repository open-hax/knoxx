(ns knoxx.backend.run-state
  (:require [knoxx.backend.runtime-config :as runtime-config]))

(defonce runs* (atom {}))
(defonce run-order* (atom []))
(defonce retrieval-stats* (atom {:samples []
                                 :avgRetrievalMs 0
                                 :p95RetrievalMs 0
                                 :recentSamples 0
                                 :modeCounts {:dense 0 :hybrid 0 :hybrid_rerank 0}}))

(defn latest-assistant-message
  [session]
  (let [messages (if (array? (aget session "messages"))
                   (array-seq (aget session "messages"))
                   [])]
    (last (filter #(= (aget % "role") "assistant") messages))))

(defn usage-map
  [usage]
  {:input_tokens (or (aget usage "input") 0)
   :output_tokens (or (aget usage "output") 0)})

(defn store-run!
  [run-id run]
  (swap! runs* assoc run-id run)
  (swap! run-order* (fn [order]
                      (->> (cons run-id (remove #{run-id} order))
                           (take 200)
                           vec)))
  run)

(defn summarize-run
  [run]
  (select-keys run [:run_id :created_at :updated_at :status :model :ttft_ms :total_time_ms :input_tokens :output_tokens :tokens_per_s :error]))

(defn append-limited
  [items item limit]
  (->> (conj (vec items) item)
       (take-last limit)
       vec))

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
                     (assoc :updated_at (runtime-config/now-iso))
                     (update :events #(append-limited % event 200))))))

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

(defn tool-event-payload
  [run-id conversation-id session-id type extra]
  (merge {:run_id run-id
          :conversation_id conversation-id
          :session_id session-id
          :type type
          :at (runtime-config/now-iso)}
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
