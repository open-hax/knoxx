(ns knoxx.backend.agent-events-test
  "Unit tests for agent event handling.
   
   Tests the event dispatch, handler isolation, and context management
   for the extracted agent_events.cljs namespace.
   
   Run with: npx shadow-cljs compile test && node out/knoxx.backend.agent-events-test.js"
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [knoxx.backend.agent-events :as events]))

;; ============================================================================
;; Test Helpers
;; ============================================================================

(defn make-fake-context
  "Create a context map for testing event handlers.
   Uses real atoms so handlers can mutate them."
  ([]
   (make-fake-context "run-1" "sess-1" "conv-1"))
  ([run-id session-id conversation-id]
   (events/make-context
    run-id session-id conversation-id
    (.now js/Date)
    ;; node-crypto mock
    #js {:randomUUID (fn [] "mock-uuid")}
    (atom [])
    (atom [])
    (atom false))))

(defn fake-js-event
  "Create a JS object simulating an agent SDK event."
  [type-keyword & kvs]
  (let [obj (js/Object.)]
    (aset obj "type" (name type-keyword))
    (doseq [[k v] (partition 2 kvs)]
      (aset obj (name k) v))
    obj))

;; ============================================================================
;; make-context Tests
;; ============================================================================

(deftest make-context-test
  (testing "creates context with all required keys"
    (let [ctx (make-fake-context "r1" "s1" "c1")]
      (is (= "r1" (:run-id ctx)))
      (is (= "s1" (:session-id ctx)))
      (is (= "c1" (:conversation-id ctx)))
      (is (some? (:started-ms ctx)))
      (is (some? (:node-crypto ctx)))
      (is (some? (:chunks ctx)))
      (is (some? (:reasoning-chunks ctx)))
      (is (some? (:ttft-recorded? ctx))))))

;; ============================================================================
;; handle-text-delta! Tests
;; ============================================================================

(deftest handle-text-delta!-test
  (testing "accumulates text chunks"
    (let [ctx (make-fake-context)
          event #js {"delta" "hello"}]
      (events/handle-text-delta! ctx event)
      (is (= ["hello"] @(:chunks ctx))))))

(deftest handle-text-delta!-multiple-chunks-test
  (testing "accumulates multiple text chunks in order"
    (let [ctx (make-fake-context)]
      (events/handle-text-delta! ctx #js {"delta" "hello "})
      (events/handle-text-delta! ctx #js {"delta" "world"})
      (is (= ["hello " "world"] @(:chunks ctx))))))

;; ============================================================================
;; handle-reasoning-delta! Tests
;; ============================================================================

(deftest handle-reasoning-delta!-test
  (testing "accumulates reasoning chunks"
    (let [ctx (make-fake-context)
          event #js {"delta" "thinking..."}]
      (events/handle-reasoning-delta! ctx event)
      (is (= ["thinking..."] @(:reasoning-chunks ctx))))))

(deftest handle-reasoning-delta!-ignores-empty-test
  (testing "ignores empty reasoning deltas"
    (let [ctx (make-fake-context)
          event #js {"delta" ""}]
      (events/handle-reasoning-delta! ctx event)
      (is (= [] @(:reasoning-chunks ctx))))))

;; ============================================================================
;; handle-message-update! dispatch Tests
;; ============================================================================

(deftest handle-message-update!-text-delta-test
  (testing "dispatches text_delta to handle-text-delta!"
    (let [ctx (make-fake-context)
          ;; message_update event wrapping a text_delta assistant event
          event #js {"type" "message_update"
                     "assistantMessageEvent" #js {"type" "text_delta"
                                                  "delta" "dispatched"}}]
      (events/handle-message-update! ctx event)
      (is (= ["dispatched"] @(:chunks ctx))))))

(deftest handle-message-update!-reasoning-delta-test
  (testing "dispatches reasoning_delta to handle-reasoning-delta!"
    (let [ctx (make-fake-context)
          event #js {"type" "message_update"
                     "assistantMessageEvent" #js {"type" "reasoning_delta"
                                                  "delta" "reasoning dispatched"}}]
      (events/handle-message-update! ctx event)
      (is (= ["reasoning dispatched"] @(:reasoning-chunks ctx))))))

(deftest handle-message-update!-unknown-type-test
  (testing "ignores unknown assistant event types"
    (let [ctx (make-fake-context)
          event #js {"type" "message_update"
                     "assistantMessageEvent" #js {"type" "unknown_type"}}]
      ;; Should not throw
      (events/handle-message-update! ctx event)
      (is (= [] @(:chunks ctx)))
      (is (= [] @(:reasoning-chunks ctx))))))

;; ============================================================================
;; make-subscription-callback Tests
;; ============================================================================

(deftest make-subscription-callback-dispatch-test
  (testing "callback dispatches message_update events"
    (let [ctx (make-fake-context)
          callback (events/make-subscription-callback ctx)]
      (callback #js {"type" "message_update"
                     "assistantMessageEvent" #js {"type" "text_delta"
                                                  "delta" "via callback"}})
      (is (= ["via callback"] @(:chunks ctx))))))

(deftest make-subscription-callback-ignores-unknown-test
  (testing "callback ignores unknown event types without error"
    (let [ctx (make-fake-context)
          callback (events/make-subscription-callback ctx)]
      ;; Should not throw
      (callback #js {"type" "custom_event"})

      (callback #js {"type" "another_unknown"})
      (is (= [] @(:chunks ctx))))))

;; ============================================================================
;; Session Isolation Tests
;; ============================================================================

(deftest multiple-sessions-isolated-contexts-test
  (testing "two contexts with different IDs accumulate chunks independently"
    (let [ctx-a (make-fake-context "run-a" "sess-a" "conv-a")
          ctx-b (make-fake-context "run-b" "sess-b" "conv-b")]
      ;; Feed events to session A
      (events/handle-text-delta! ctx-a #js {"delta" "session A chunk"})
      ;; Feed events to session B
      (events/handle-text-delta! ctx-b #js {"delta" "session B chunk"})

      ;; Each context should only have its own chunks
      (is (= ["session A chunk"] @(:chunks ctx-a)))
      (is (= ["session B chunk"] @(:chunks ctx-b))))))

(deftest multiple-sessions-reasoning-isolated-test
  (testing "reasoning chunks are isolated between sessions"
    (let [ctx-a (make-fake-context "run-a" "sess-a" "conv-a")
          ctx-b (make-fake-context "run-b" "sess-b" "conv-b")]
      (events/handle-reasoning-delta! ctx-a #js {"delta" "reasoning A"})
      (events/handle-reasoning-delta! ctx-b #js {"delta" "reasoning B"})

      (is (= ["reasoning A"] @(:reasoning-chunks ctx-a)))
      (is (= ["reasoning B"] @(:reasoning-chunks ctx-b))))))

(deftest multiple-sessions-ttft-isolated-test
  (testing "TTFT recording is isolated per session"
    (let [ctx-a (make-fake-context "run-a" "sess-a" "conv-a")
          ctx-b (make-fake-context "run-b" "sess-b" "conv-b")]
      ;; Record TTFT for session A only
      (events/handle-text-delta! ctx-a #js {"delta" "first token A"})

      ;; Session A should have TTFT recorded
      (is (true? @(:ttft-recorded? ctx-a)))
      ;; Session B should NOT have TTFT recorded yet
      (is (false? @(:ttft-recorded? ctx-b))))))

;; ============================================================================
;; Run Tests
;; ============================================================================

(defn -main []
  (run-tests 'knoxx.backend.agent-events-test))
