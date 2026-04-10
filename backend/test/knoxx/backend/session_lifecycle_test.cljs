(ns knoxx.backend.session-lifecycle-test
  "Unit tests for session lifecycle management.
   
   Tests session-usable? validation, remove-session!, and
   validate-or-remove-session! — the core logic for preventing
   stale session reuse.
   
   Run with: npx shadow-cljs compile test && node out/knoxx.backend.session-lifecycle-test.js"
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [knoxx.backend.session-lifecycle :as lifecycle]))

;; ============================================================================
;; session-usable? Tests
;; ============================================================================

(deftest session-usable?-test
  (testing "nil session is not usable"
    (is (false? (lifecycle/session-usable? nil))))

  (testing "fresh session with no flags is usable"
    (is (true? (lifecycle/session-usable? #js {}))))

  (testing "session with isStreaming=true is usable"
    (is (true? (lifecycle/session-usable? #js {:isStreaming true}))))

  (testing "session with isStreaming=false but no abort is usable"
    (is (true? (lifecycle/session-usable? #js {:isStreaming false}))))

  (testing "session with abortRequested=true is NOT usable"
    (is (false? (lifecycle/session-usable? #js {:isStreaming false :abortRequested true}))))

  (testing "session with isStreaming=true and abortRequested=true is NOT usable"
    ;; abort takes precedence — even if streaming, an aborted session should not be reused
    (is (false? (lifecycle/session-usable? #js {:isStreaming true :abortRequested true}))))

  (testing "session with only abortRequested=true is NOT usable"
    (is (false? (lifecycle/session-usable? #js {:abortRequested true}))))

  (testing "session with undefined isStreaming is usable (nil check)"
    (is (true? (lifecycle/session-usable? #js {:abortRequested false})))))

;; ============================================================================
;; remove-session! Tests
;; ============================================================================

(deftest remove-session!-test
  (testing "removes existing session from atom"
    (let [sessions (atom {"conv-1" #js {:id "session-1"}})]
      (lifecycle/remove-session! sessions "conv-1")
      (is (nil? (get @sessions "conv-1")))))

  (testing "does not throw on nil conversation-id"
    (let [sessions (atom {"conv-1" #js {:id "session-1"}})]
      (lifecycle/remove-session! sessions nil)
      ;; Original session should still be there
      (is (some? (get @sessions "conv-1")))))

  (testing "does not throw on nil atom"
    ;; Should gracefully handle nil atom
    (is (nil? (lifecycle/remove-session! nil "conv-1"))))

  (testing "removing non-existent conversation-id is a no-op"
    (let [sessions (atom {"conv-1" #js {:id "session-1"}})]
      (lifecycle/remove-session! sessions "conv-nonexistent")
      (is (some? (get @sessions "conv-1"))))))

;; ============================================================================
;; validate-or-remove-session! Tests
;; ============================================================================

(deftest validate-or-remove-session!-test
  (testing "returns usable session without removing it"
    (let [sessions (atom {"conv-1" #js {:isStreaming true}})]
      (let [result (lifecycle/validate-or-remove-session! sessions "conv-1")]
        (is (some? result))
        (is (some? (get @sessions "conv-1"))))))

  (testing "removes and returns nil for aborted session"
    (let [sessions (atom {"conv-1" #js {:isStreaming false :abortRequested true}})]
      (let [result (lifecycle/validate-or-remove-session! sessions "conv-1")]
        (is (nil? result))
        (is (nil? (get @sessions "conv-1"))))))

  (testing "returns nil without error for missing conversation-id"
    (let [sessions (atom {})]
      (let [result (lifecycle/validate-or-remove-session! sessions "conv-nonexistent")]
        (is (nil? result)))))

  (testing "handles concurrent removal — second call returns nil"
    (let [sessions (atom {"conv-1" #js {:abortRequested true}})]
      ;; First call removes the session
      (lifecycle/validate-or-remove-session! sessions "conv-1")
      ;; Second call should return nil gracefully
      (let [result (lifecycle/validate-or-remove-session! sessions "conv-1")]
        (is (nil? result)))))

  ;; --- Multiple sessions isolation ---

  (testing "removing one session does not affect another"
    (let [sessions (atom {"conv-1" #js {:abortRequested true}
                          "conv-2" #js {:isStreaming true}})]
      ;; Remove conv-1 (aborted)
      (lifecycle/validate-or-remove-session! sessions "conv-1")
      ;; conv-2 should still be there and usable
      (let [result (lifecycle/validate-or-remove-session! sessions "conv-2")]
        (is (some? result))
        (is (some? (get @sessions "conv-2"))))))

  (testing "validating multiple sessions in sequence"
    (let [sessions (atom {"conv-1" #js {:isStreaming true}
                          "conv-2" #js {:isStreaming false :abortRequested true}
                          "conv-3" #js {}})]
      ;; conv-1 should be usable
      (is (some? (lifecycle/validate-or-remove-session! sessions "conv-1")))
      ;; conv-2 should be removed (aborted)
      (is (nil? (lifecycle/validate-or-remove-session! sessions "conv-2")))
      ;; conv-3 should be usable (fresh, no flags)
      (is (some? (lifecycle/validate-or-remove-session! sessions "conv-3"))))))

;; ============================================================================
;; Run Tests
;; ============================================================================

(defn -main []
  (run-tests 'knoxx.backend.session-lifecycle-test))
