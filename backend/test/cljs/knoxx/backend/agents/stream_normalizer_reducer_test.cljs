(ns knoxx.backend.agents.stream-normalizer-reducer-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.agent.reasoning :as reasoning]
            [knoxx.backend.domain.agent.text-delta :as text-delta]
            [knoxx.backend.domain.agent.tool-lifecycle :as tool-lifecycle]
            [knoxx.backend.domain.agent.turn-guards :as turn-guards]
            [knoxx.backend.infra.agent.stream.provider-events :as provider-events]
            [knoxx.backend.infra.agent.stream.reducer :as reducer]))

(deftest pure-text-delta-suppresses-replayed-prefixes
  (testing "appended extraction still handles cumulative provider chunks"
    (is (= " model should reason once."
           (text-delta/diff-appended-text "The" "TheThe model should reason once."))))
  (testing "prefix replay suppression is pure and carries an offset"
    (let [first-pass (text-delta/suppress-replayed-prefix-delta "Ready. How" nil "Ready")
          second-pass (text-delta/suppress-replayed-prefix-delta "Ready. How" (:replay-offset first-pass) ". How")]
      (is (= {:delta "" :replay-offset 5} first-pass))
      (is (= {:delta "" :replay-offset nil} second-pass)))))

(deftest reasoning-routes-think-tag-deltas
  (testing "early think tags become reasoning emissions and answer text stays clean"
    (is (= {:mode :done
            :emissions [{:kind :reasoning :delta "private"}
                        {:kind :agent_message :delta "public"}]}
           (reasoning/route-think-delta {:mode :off
                                         :last-assistant-text ""
                                         :delta "<think>private</think>public"}))))
  (testing "terminal assistant text extraction uses the same pure helper"
    (is (= {:reasoning "why"
            :answer "answer"
            :hadThinkTags true}
           (reasoning/split-think-tags "<think>why</think>answer")))))

(deftest provider-events-normalize-js-shapes
  (testing "message_update provider JS shape becomes canonical CLJS data"
    (let [message #js {:role "assistant" :content "hello"}
          normalized (provider-events/normalize
                      #js {:type "message_update"
                           :assistantMessageEvent #js {:type "text_delta"
                                                       :delta "hello"
                                                       :partial message}
                           :message message})]
      (is (= "message_update" (:type normalized)))
      (is (= "text_delta" (:assistant-event-type normalized)))
      (is (= "hello" (:delta normalized)))
      (is (= message (:partial-message normalized)))
      (is (= message (:message normalized)))))
  (testing "tool lifecycle provider JS shape includes parsed data and preview"
    (let [start (provider-events/normalize
                 #js {:type "tool_execution_start"
                      :toolName "memory.search"
                      :toolCallId "call-1"
                      :params #js {:query "knoxx"}})
          end (provider-events/normalize
               #js {:type "tool_execution_end"
                    :toolName "memory.search"
                    :toolCallId "call-1"
                    :result #js {:ok true}
                    :isError false})]
      (is (= {:query "knoxx"} (:input-raw start)))
      (is (= "memory.search" (:tool-name start)))
      (is (string? (:input-preview start)))
      (is (= {:ok true} (:result-raw end)))
      (is (= "completed" (:status (tool-lifecycle/run-event-extra :end end)))))))

(deftest reducer-produces-pure-effects
  (testing "text deltas produce token effects without sinks"
    (let [result (reducer/reduce-event (reducer/initial-state)
                                       {:type "message_update"
                                        :assistant-event-type "text_delta"
                                        :delta "Hello"})]
      (is (= "Hello" (get-in result [:state :assistant-text])))
      (is (= [{:effect :token :kind :agent_message :delta "Hello"}]
             (:effects result)))))
  (testing "cumulative text emits only appended suffix"
    (let [state (:state (reducer/reduce-event (reducer/initial-state)
                                              {:type "message_update"
                                               :assistant-event-type "text_delta"
                                               :delta "Hello"}))
          result (reducer/reduce-event state
                                       {:type "message_update"
                                        :assistant-event-type "text_delta"
                                        :delta "Hello world"})]
      (is (= "Hello world" (get-in result [:state :assistant-text])))
      (is (= [{:effect :token :kind :agent_message :delta " world"}]
             (:effects result)))))
  (testing "think-tag routing is reduced into reasoning and answer token effects"
    (let [result (reducer/reduce-event (reducer/initial-state)
                                       {:type "message_update"
                                        :assistant-event-type "text_delta"
                                        :delta "<think>secret</think>shown"})]
      (is (= "secret" (get-in result [:state :reasoning-text])))
      (is (= "shown" (get-in result [:state :assistant-text])))
      (is (= [{:effect :token :kind :reasoning :delta "secret"}
              {:effect :token :kind :agent_message :delta "shown"}]
             (:effects result)))))
  (testing "tool lifecycle and death spiral decisions are pure effects"
    (let [start-event {:type "tool_execution_start"
                       :tool-name "memory.search"
                       :tool-call-id "call-1"
                       :input-preview "query"}
          first-result (reducer/reduce-event (reducer/initial-state) start-event)
          repeated-state (reduce (fn [state _]
                                   (:state (reducer/reduce-event state start-event)))
                                 (:state first-result)
                                 (range (dec turn-guards/default-death-spiral-streak-limit)))
          death-result (reducer/reduce-event repeated-state start-event)]
      (is (= :tool-start (-> first-result :effects first :effect)))
      (is (= "running" (-> first-result :effects first :receipt :status)))
      (is (= :abort (-> death-result :effects second :effect)))
      (is (re-find #"death_spiral_detected" (-> death-result :effects second :reason))))))
