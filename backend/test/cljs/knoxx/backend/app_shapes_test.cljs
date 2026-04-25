(ns knoxx.backend.app-shapes-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.app-shapes :as shapes]))

;; ─── normalize-chat-body ─────────────────────────────────────────

(deftest normalize-chat-body-empty
  (let [body (shapes/normalize-chat-body #js {})]
    (is (= ""       (:message body)))
    (is (= "direct" (:mode body)))
    (is (nil? (:model body)))
    (is (nil? (:conversation-id body)))
    (is (nil? (:session-id body)))
    (is (nil? (:run-id body)))
    (is (nil? (:thinking-level body)))
    (is (nil? (:agent-spec body)))
    (is (nil? (:auth-context body)))))

(deftest normalize-chat-body-camelCase
  (let [body (shapes/normalize-chat-body
               #js {:message "hello"
                    :conversationId "conv-1"
                    :sessionId "sess-1"
                    :runId "run-1"
                    :model "gpt-4o"
                    :thinkingLevel "high"
                    :mode "agent"})]
    (is (= "hello"  (:message body)))
    (is (= "conv-1" (:conversation-id body)))
    (is (= "sess-1" (:session-id body)))
    (is (= "run-1"  (:run-id body)))
    (is (= "gpt-4o" (:model body)))
    (is (= "high"   (:thinking-level body)))
    (is (= "agent"  (:mode body)))))

(deftest normalize-chat-body-snake-aliases
  (testing "snake_case field aliases resolve correctly"
    (let [body (shapes/normalize-chat-body
                 #js {:conversation_id "conv-2"
                      :session_id "sess-2"
                      :run_id "run-2"
                      :reasoning_effort "medium"})]
      (is (= "conv-2"  (:conversation-id body)))
      (is (= "sess-2"  (:session-id body)))
      (is (= "run-2"   (:run-id body)))
      (is (= "medium"  (:thinking-level body))))))

(deftest normalize-chat-body-reasoningEffort-alias
  (testing "reasoningEffort is an alias for thinkingLevel"
    (let [body (shapes/normalize-chat-body #js {:reasoningEffort "low"})]
      (is (= "low" (:thinking-level body))))))

;; ─── normalize-control-body ──────────────────────────────────────

(deftest normalize-control-body-smoke
  (let [body (shapes/normalize-control-body
               #js {:message "stop"
                    :conversationId "c-1"
                    :sessionId "s-1"
                    :runId "r-1"})]
    (is (= "stop" (:message body)))
    (is (= "c-1"  (:conversation-id body)))
    (is (= "s-1"  (:session-id body)))
    (is (= "r-1"  (:run-id body)))))

(deftest normalize-control-body-empty
  (let [body (shapes/normalize-control-body #js {})]
    (is (= "" (:message body)))
    (is (nil? (:conversation-id body)))
    (is (nil? (:session-id body)))
    (is (nil? (:run-id body)))))

(deftest normalize-control-body-snake-aliases
  (let [body (shapes/normalize-control-body
               #js {:conversation_id "c-2" :session_id "s-2" :run_id "r-2"})]
    (is (= "c-2" (:conversation-id body)))
    (is (= "s-2" (:session-id body)))
    (is (= "r-2" (:run-id body)))))
