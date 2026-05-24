(ns knoxx.backend.message-source-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.stores.message-source :refer [IMessageSource fetch-messages!]]
            [knoxx.backend.infra.stores.composite-message-source :refer [->CompositeMessageSource]]
            [knoxx.backend.infra.stores.redis-message-source :refer [->RedisMessageSource]]
            [knoxx.backend.infra.stores.openplanner-message-source :refer [->OpenPlannerMessageSource]]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.infra.stores.session-store :as session-store]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]))

;; ─── In-memory IMessageSource for testing ───────────────────────────────────

(defn- fixed-source
  "IMessageSource that always returns the given messages."
  [messages]
  (reify IMessageSource
    (fetch-messages! [_ _] (js/Promise.resolve (vec messages)))))

;; ─── CompositeMessageSource ──────────────────────────────────────────────────

(deftest ^:async composite-merges-non-overlapping-messages
  (let [primary   (fixed-source [{:role "user" :content "first"}])
        secondary (fixed-source [{:role "assistant" :content "reply"}])
        composite (->CompositeMessageSource primary secondary)
        result    (await (fetch-messages! composite "conv-1"))]
    (testing "both source messages appear in order"
      (is (= [{:role "user" :content "first"}
              {:role "assistant" :content "reply"}]
             result)))))

(deftest ^:async composite-dedupes-overlapping-tail
  ;; merge-restored-session-messages uses the overlap-suffix algorithm:
  ;; if the end of primary matches the start of secondary, collapse them.
  (let [primary   (fixed-source [{:role "user" :content "a"}
                                 {:role "user" :content "b"}])
        secondary (fixed-source [{:role "user" :content "b"}
                                 {:role "user" :content "c"}])
        composite (->CompositeMessageSource primary secondary)
        result    (await (fetch-messages! composite "conv-1"))]
    (testing "overlapping tail is not duplicated"
      (is (= [{:role "user" :content "a"}
              {:role "user" :content "b"}
              {:role "user" :content "c"}]
             result)))))

(deftest ^:async composite-returns-empty-when-both-empty
  (let [composite (->CompositeMessageSource (fixed-source []) (fixed-source []))
        result    (await (fetch-messages! composite "conv-1"))]
    (testing "empty result when both sources have nothing"
      (is (= [] result)))))

;; ─── RedisMessageSource ──────────────────────────────────────────────────────

(deftest ^:async redis-source-uses-preferred-session-id
  (with-redefs [redis/get-client (fn [] :client)
                session-store/get-session
                (fn [_client session-id]
                  (js/Promise.resolve
                   (when (= "session-42" session-id)
                     {:messages [{:role "user" :content "from preferred session"}]})))]
    (let [src    (->RedisMessageSource "session-42")
          result (await (fetch-messages! src "conversation-ignored"))]
      (testing "preferred session-id bypasses conversation lookup"
        (is (= [{:role "user" :content "from preferred session"}] result))))))

(deftest ^:async redis-source-falls-back-to-conversation-lookup
  (with-redefs [redis/get-client (fn [] :client)
                session-store/get-conversation-active-session
                (fn [_client conversation-id]
                  (js/Promise.resolve
                   (when (= "conv-x" conversation-id) "sess-from-lookup")))
                session-store/get-session
                (fn [_client session-id]
                  (js/Promise.resolve
                   (when (= "sess-from-lookup" session-id)
                     {:messages [{:role "user" :content "from lookup"}]})))]
    (let [src    (->RedisMessageSource nil)
          result (await (fetch-messages! src "conv-x"))]
      (testing "falls back to conversation->session index when no preferred id"
        (is (= [{:role "user" :content "from lookup"}] result))))))

(deftest ^:async redis-source-returns-empty-when-no-client
  (with-redefs [redis/get-client (fn [] nil)]
    (let [src    (->RedisMessageSource nil)
          result (await (fetch-messages! src "conv-x"))]
      (testing "returns empty vec when Redis client unavailable"
        (is (= [] result))))))

;; ─── OpenPlannerMessageSource ────────────────────────────────────────────────

(deftest ^:async openplanner-source-returns-empty-when-disabled
  (with-redefs [openplanner-client/enabled? (fn [_] false)]
    (let [src    (->OpenPlannerMessageSource {})
          result (await (fetch-messages! src "conv-1"))]
      (testing "returns empty when OpenPlanner not configured"
        (is (= [] result))))))

(deftest ^:async openplanner-source-returns-empty-for-blank-conversation
  (with-redefs [openplanner-client/enabled? (fn [_] true)]
    (let [src    (->OpenPlannerMessageSource {})
          result (await (fetch-messages! src nil))]
      (testing "returns empty for nil conversation-id"
        (is (= [] result))))))

(defn- stub-op-client [rows]
  (reify openplanner-client/IOpenPlannerClient
    (enabled? [_] true)
    (session! [_ _conversation-id _opts]
      (js/Promise.resolve {:rows rows}))))

(defn- failing-op-client []
  (reify openplanner-client/IOpenPlannerClient
    (enabled? [_] true)
    (session! [_ _conversation-id _opts]
      (js/Promise.reject (js/Error. "OpenPlanner request failed")))))

(deftest ^:async openplanner-source-maps-rows-to-messages
  (let [src    (->OpenPlannerMessageSource
                {:openplanner-client
                 (stub-op-client [{:role "user" :text "hello from op"}
                                  {:role "assistant" :text "reply from op"}
                                  {:role "unknown" :text "filtered out"}])})
        result (await (fetch-messages! src "conv-1"))]
    (testing "rows are mapped to stored-message format"
      (is (= [{:role "user" :content "hello from op"}
              {:role "assistant" :content "reply from op"}]
             result)))))

(deftest ^:async openplanner-source-rejects-when-history-restore-fails
  (let [src (->OpenPlannerMessageSource {:openplanner-client (failing-op-client)})]
    (testing "OpenPlanner restore failure is explicit and blocks transcript construction"
      (try
        (await (fetch-messages! src "conv-1"))
        (is false "expected OpenPlanner history restore to reject")
        (catch :default err
          (is (re-find #"OpenPlanner request failed" (.-message err))))))))
