(ns knoxx.backend.realtime-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.realtime :as realtime]))

(deftest ws-client-matches-payload-by-conversation
  (testing "conversation id is authoritative when the client has one"
    (is (true? (realtime/ws-client-matches-payload?
                #js {:sessionId "session-1" :conversationId "conversation-1"}
                "session-1"
                {:conversation_id "conversation-1"})))
    (is (false? (realtime/ws-client-matches-payload?
                 #js {:sessionId "session-1" :conversationId "other-conversation"}
                 "session-1"
                 {:conversation_id "conversation-1"})))))

(deftest ws-client-matches-payload-by-session-before-conversation-is-known
  (testing "blank client conversation can still receive its session's first stream"
    (is (true? (realtime/ws-client-matches-payload?
                #js {:sessionId "session-1" :conversationId ""}
                "session-1"
                {:conversation_id "conversation-1"})))
    (is (false? (realtime/ws-client-matches-payload?
                 #js {:sessionId "other-session" :conversationId ""}
                 "session-1"
                 {:conversation_id "conversation-1"})))))
