(ns knoxx.backend.turn-control-test
  (:require [cljs.test :refer [deftest is testing use-fixtures]]
            [knoxx.backend.turn-control :as turn-control]))

(use-fixtures :each
  {:before (fn [] (reset! turn-control/active-turns* {}))
   :after (fn [] (reset! turn-control/active-turns* {}))})

(deftest active-turn-entries-include-conversation-id
  (testing "active turn helpers expose resumable shutdown metadata"
    (turn-control/register-active-turn! "conversation-1"
                                        {:run_id "run-1"
                                         :session_id "session-1"})
    (is (= 1 (turn-control/active-turn-count)))
    (is (= [{:run_id "run-1"
             :session_id "session-1"
             :conversation_id "conversation-1"}]
           (turn-control/active-turn-entries)))))
