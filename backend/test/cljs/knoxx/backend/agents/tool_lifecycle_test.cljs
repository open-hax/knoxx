(ns knoxx.backend.agents.tool-lifecycle-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.agent.tool-lifecycle :as lifecycle]))

(deftest receipts-preserve-start-time-and-collect-previews
  (testing "start-receipt records first start only and optional input details"
    (is (= {:tool_name "memory.search"
            :status "running"
            :started_at "t0"
            :input {:query "knoxx"}
            :input_preview "{query}"}
           (lifecycle/start-receipt {:started_at "t0"}
                                    {:tool-name "memory.search"
                                     :input-raw {:query "knoxx"}
                                     :input-preview "{query}"
                                     :at "t1"}))))
  (testing "update-receipt appends previews by default"
    (is (= {:tool_name "memory.search"
            :status "running"
            :updates (list "second" "first")}
           (-> {}
               (lifecycle/update-receipt {:tool-name "memory.search"
                                          :preview "first"})
               (lifecycle/update-receipt {:tool-name "memory.search"
                                          :preview "second"})))))
  (testing "update-receipt can use a custom preview accumulator"
    (is (= {:tool_name "memory.search"
            :status "running"
            :updates "first | second"}
           (-> {:updates "first"}
               (lifecycle/update-receipt {:tool-name "memory.search"
                                          :preview "second"
                                          :append-preview (fn [existing preview]
                                                            (str existing " | " preview))}))))))

(deftest end-receipt-captures-success-error-and-media-parts
  (is (= {:tool_name "workspace_media.attach"
          :status "completed"
          :ended_at "t2"
          :is_error false
          :result {:ok true}
          :result_preview "attached"
          :content_parts [{:type "document" :url "file:///doc.md"}]}
         (lifecycle/end-receipt {}
                                {:tool-name "workspace_media.attach"
                                 :is-error false
                                 :result-raw {:ok true}
                                 :result-preview "attached"
                                 :content-parts [{:type "document" :url "file:///doc.md"}]
                                 :at "t2"})))
  (is (= {:tool_name "memory.search"
          :status "failed"
          :ended_at "t3"
          :is_error true}
         (lifecycle/end-receipt {}
                                {:tool-name "memory.search"
                                 :is-error true
                                 :content-parts []
                                 :at "t3"}))))

(deftest trace-events-are-phase-specific
  (is (= {:type "tool_start"
          :tool_name "memory.search"
          :tool_call_id "call-1"
          :preview "query"
          :at "t0"}
         (lifecycle/trace-event :start {:tool-name "memory.search"
                                        :tool-call-id "call-1"
                                        :input-preview "query"
                                        :at "t0"})))
  (is (= {:type "tool_update"
          :tool_name "memory.search"
          :tool_call_id "call-1"
          :preview "still running"
          :at "t1"}
         (lifecycle/trace-event :update {:tool-name "memory.search"
                                         :tool-call-id "call-1"
                                         :preview "still running"
                                         :at "t1"})))
  (is (= {:type "tool_end"
          :tool_name "memory.search"
          :tool_call_id "call-1"
          :preview "done"
          :is_error false
          :at "t2"}
         (lifecycle/trace-event :end {:tool-name "memory.search"
                                      :tool-call-id "call-1"
                                      :result-preview "done"
                                      :is-error false
                                      :at "t2"}))))

(deftest run-event-extra-covers-all-phases
  (is (= {:status "running"
          :tool_name "memory.search"
          :tool_call_id "call-1"
          :preview "query"}
         (lifecycle/run-event-extra :start {:tool-name "memory.search"
                                            :tool-call-id "call-1"
                                            :input-preview "query"})))
  (is (= {:status "running"
          :tool_name "memory.search"
          :tool_call_id "call-1"
          :preview "progress"}
         (lifecycle/run-event-extra :update {:tool-name "memory.search"
                                             :tool-call-id "call-1"
                                             :preview "progress"})))
  (is (= {:status "failed"
          :tool_name "memory.search"
          :tool_call_id "call-1"
          :is_error true
          :preview "boom"}
         (lifecycle/run-event-extra :end {:tool-name "memory.search"
                                          :tool-call-id "call-1"
                                          :is-error true
                                          :result-preview "boom"})))
  (is (= {:status "failed"
          :tool_name "memory.search"
          :tool_call_id "call-1"
          :count 8
          :streak 4}
         (lifecycle/run-event-extra :death-spiral {:tool-name "memory.search"
                                                   :tool-call-id "call-1"
                                                   :count 8
                                                   :streak 4}))))
