(ns knoxx.backend.agent-turns-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.agent-turns :as agent-turns]))

(deftest ensure-session-id-preserves-provided-value
  (testing "existing session ids are kept intact"
    (let [crypto #js {:randomUUID (fn [] "generated-session-id")}]
      (is (= "existing-session-id"
             (agent-turns/ensure-session-id crypto "existing-session-id"))))))

(deftest ensure-session-id-generates-missing-value
  (testing "missing session ids are generated before provider requests"
    (let [crypto #js {:randomUUID (fn [] "generated-session-id")}]
      (is (= "generated-session-id"
             (agent-turns/ensure-session-id crypto nil))))))

(deftest assistant-tool-call-previews-extracts-string-arguments-from-assistant-message
  (testing "string tool call arguments from assistant messages can backfill missing tool receipt inputs"
    (let [assistant-message #js {:role "assistant"
                                 :content #js [#js {:type "toolCall"
                                                    :id "tool-123"
                                                    :name "custom_tool"
                                                    :arguments "path=docs/guide.md limit=20"}]}
          previews (agent-turns/assistant-tool-call-previews assistant-message)]
      (is (= [{:tool_call_id "tool-123"
               :tool_name "custom_tool"
               :input_preview "path=docs/guide.md limit=20"}]
             previews)))))

(deftest session->stored-messages-preserves-prior-transcript
  (testing "live session snapshots keep existing user and assistant turns for restart recovery"
    (let [session #js {:messages #js [#js {:role "system"
                                           :content #js [#js {:type "text" :text "stay grounded"}]}
                                      #js {:role "user"
                                           :content #js [#js {:type "text" :text "first request"}]}
                                      #js {:role "assistant"
                                           :content #js [#js {:type "text" :text "first answer"}]}]}]
      (is (= [{:role "system" :content "stay grounded"}
              {:role "user" :content "first request"}
              {:role "assistant" :content "first answer"}]
             (agent-turns/session->stored-messages session))))))

