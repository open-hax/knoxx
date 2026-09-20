(ns knoxx.backend.run-state-retention-test
  "Run heap bounds through the actual public registry API."
  (:require [cljs.test :as t]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.extern.run-record :as record]))

(defn- with-empty-heap [run]
  (let [previous-runs @state/runs* previous-order @state/run-order*]
    (reset! state/runs* {})
    (reset! state/run-order* [])
    (try (run)
         (finally
           (reset! state/runs* previous-runs)
           (reset! state/run-order* previous-order)))))

(t/deftest storing-more-than-the-limit-evicts-stale-run-values
  (let [previous-runs @state/runs* previous-order @state/run-order*]
    (reset! state/runs* {})
    (reset! state/run-order* [])
    (try
      (doseq [index (range (+ state/MAX_RUNS 2))]
        (state/store-run! index {:run_id index :events []}))
      (t/is (= state/MAX_RUNS (count @state/run-order*)))
      (t/is (= state/MAX_RUNS (count @state/runs*)))
      (t/is (= (set @state/run-order*) (set (keys @state/runs*))))
      (t/is (not (contains? @state/runs* 0)))
      (t/is (not (contains? @state/runs* 1)))
      (state/store-run! 2 {:run_id 2 :status "updated"})
      (t/is (= 2 (first @state/run-order*)))
      (t/is (= state/MAX_RUNS (count @state/runs*)))
      (t/is (= "updated" (get-in @state/runs* [2 :status])))
      (finally
        (reset! state/runs* previous-runs)
        (reset! state/run-order* previous-order)))))

(t/deftest native-array-normalization-preserves-portable-values
  (let [portable [{:id "already-portable"}]
        value (record/normalize-run {:events #js [#js {:type "started"}]
                                     :tool_receipts portable :status "running"})]
    (t/is (= [{:type "started"}] (:events value)))
    (t/is (identical? portable (:tool_receipts value)))
    (t/is (= "running" (:status value)))))

(t/deftest tool-lifecycle-retains-input-and-bounds-progress
  (with-empty-heap
    (fn []
      (state/store-run! "trace" {:trace_blocks []})
      (let [event {:tool_call_id "call" :tool_name "read" :at "now"}]
        (state/apply-run-tool-trace-event! "trace" (assoc event :type "tool_start" :preview "input"))
        (doseq [index (range 10)]
          (state/apply-run-tool-trace-event! "trace" (assoc event :type "tool_update" :preview (str index))))
        (state/apply-run-tool-trace-event! "trace" (assoc event :type "tool_end" :preview "failed" :is_error true))
        (let [before (get-in @state/runs* ["trace" :trace_blocks]) block (first before)]
          (t/is (= ["input" "failed" "error" true]
                   ((juxt :inputPreview :outputPreview :status :isError) block)))
          (t/is (= (mapv str (range 2 10)) (:updates block)))
          (state/apply-run-tool-trace-event! "trace" (assoc event :type "unrelated"))
          (t/is (= before (get-in @state/runs* ["trace" :trace_blocks]))))))))

(t/deftest missing-tool-input-creates-observations-and-rejects-sentinels
  (with-empty-heap
    (fn []
      (state/store-run! "empty" {})
      (doseq [preview [nil "" "null" " undefined "]]
        (state/backfill-run-tool-input-preview! "empty" "call" "read" preview))
      (t/is (= {} (get @state/runs* "empty")))
      (state/backfill-run-tool-input-preview! "empty" "call" "read" "concrete")
      (let [run (get @state/runs* "empty")]
        (t/is (= ["call" "read" "concrete" "running"]
                 ((juxt :id :tool_name :input_preview :status) (first (:tool_receipts run)))))
        (t/is (= ["tool:call" "call" "concrete" "streaming"]
                 ((juxt :id :toolCallId :inputPreview :status) (first (:trace_blocks run)))))))))
