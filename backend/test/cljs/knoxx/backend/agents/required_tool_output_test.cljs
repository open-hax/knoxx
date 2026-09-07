(ns knoxx.backend.agents.required-tool-output-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.agent.turn :as agent-turns]))

(deftest required-first-turns-fail-closed-without-tool-receipts
  (testing "call-shaped assistant prose cannot satisfy a required tool invocation"
    (is (= {:diagnostic-type :agent-turn/required-tool-not-called
            :message "Agent turn completed without calling a tool required by tools-choice"
            :reason "required_tool_not_called"}
           (#'agent-turns/turn-output-failure
            "save_translation({translated_text: 'bonjour'})"
            {:tool_receipts []}
            []
            {:tools-choice "required-first"}))))
  (testing "keyword tools-choice values receive the same enforcement"
    (is (= "required_tool_not_called"
           (:reason
            (#'agent-turns/turn-output-failure
             "I translated the document"
             {}
             []
             {:tools-choice :required-first})))))
  (testing "a recorded tool invocation satisfies required-first"
    (is (nil?
         (#'agent-turns/turn-output-failure
          ""
          {:tool_receipts [{:tool_name "save_translation"
                            :status "completed"}]}
          []
          {:tools-choice "required-first"}))))
  (testing "ordinary conversational turns remain valid without tools"
    (is (nil?
         (#'agent-turns/turn-output-failure
          "A normal assistant answer"
          {:tool_receipts []}
          []
          {:tools-choice "auto"}))))
  (testing "the existing empty-output guard remains intact"
    (is (= "empty_output"
           (:reason
            (#'agent-turns/turn-output-failure
             ""
             {:tool_receipts []}
             []
             {}))))))
