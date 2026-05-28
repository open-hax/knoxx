(ns knoxx.backend.agents.context-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.agent.agent-context :as agent-context]))

(deftest agent-context-requires-session-and-conversation
  (testing "complete context is available until cleared"
    (agent-context/clear-context!)
    (agent-context/set-context! {:session-id "session-1"
                                 :conversation-id "conversation-1"
                                 :run-id "run-1"
                                 :agent-spec {:id "knoxx_default"}})
    (is (= {:session-id "session-1"
            :conversation-id "conversation-1"
            :run-id "run-1"
            :agent-spec {:id "knoxx_default"}}
           (agent-context/get-context)))
    (agent-context/clear-context!)
    (is (nil? (agent-context/get-context))))
  (testing "incomplete context never leaks stale turn data"
    (agent-context/set-context! {:session-id "session-1"
                                 :conversation-id "conversation-1"})
    (agent-context/set-context! {:session-id "session-only"})
    (is (nil? (agent-context/get-context)))
    (agent-context/set-context! {:conversation-id "conversation-only"})
    (is (nil? (agent-context/get-context)))))
