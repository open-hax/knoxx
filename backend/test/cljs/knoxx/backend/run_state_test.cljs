(ns knoxx.backend.run-state-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.run-state :as run-state]))

(deftest backfill-run-tool-input-preview-upgrades-missing-receipts-and-trace-blocks
  (testing "missing or sentinel tool inputs are replaced with a real preview"
    (let [run-id "run-backfill"
          _ (run-state/store-run! run-id {:run_id run-id
                                          :tool_receipts [{:id "tool-1"
                                                           :tool_name "read"
                                                           :status "completed"
                                                           :input_preview "null"}]
                                          :trace_blocks [{:id "tool:tool-1"
                                                          :kind :tool_call
                                                          :toolName "read"
                                                          :toolCallId "tool-1"
                                                          :status "done"
                                                          :inputPreview "null"}]})]
      (run-state/backfill-run-tool-input-preview! run-id "tool-1" "read" "```yaml\npath: docs/guide.md\n```")
      (let [run (run-state/update-run! run-id identity)]
        (is (= "```yaml\npath: docs/guide.md\n```"
               (get-in run [:tool_receipts 0 :input_preview])))
        (is (= "```yaml\npath: docs/guide.md\n```"
               (get-in run [:trace_blocks 0 :inputPreview])))
        (is (= "done" (get-in run [:trace_blocks 0 :status])))))))

(deftest backfill-run-tool-input-preview-does-not-overwrite-existing-values
  (testing "existing observability data stays intact"
    (let [run-id "run-existing"
          _ (run-state/store-run! run-id {:run_id run-id
                                          :tool_receipts [{:id "tool-2"
                                                           :tool_name "bash"
                                                           :status "completed"
                                                           :input_preview "```bash\necho hi\n```"}]
                                          :trace_blocks [{:id "tool:tool-2"
                                                          :kind :tool_call
                                                          :toolName "bash"
                                                          :toolCallId "tool-2"
                                                          :status "done"
                                                          :inputPreview "```bash\necho hi\n```"}]})]
      (run-state/backfill-run-tool-input-preview! run-id "tool-2" "bash" "```bash\nwhoami\n```")
      (let [run (run-state/update-run! run-id identity)]
        (is (= "```bash\necho hi\n```"
               (get-in run [:tool_receipts 0 :input_preview])))
        (is (= "```bash\necho hi\n```"
               (get-in run [:trace_blocks 0 :inputPreview])))))))
