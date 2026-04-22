(ns knoxx.backend.agent-runtime-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.agent-runtime :as agent-runtime]))

(deftest merge-restored-session-messages-appends-only-redis-suffix
  (testing "OpenPlanner history and Redis in-flight history merge without duplicating overlap"
    (let [openplanner [{:role "system" :content "stay grounded"}
                       {:role "user" :content "investigate the restart bug"}
                       {:role "assistant" :content "I found the failing path."}]
          redis [{:role "user" :content "investigate the restart bug"}
                 {:role "assistant" :content "I found the failing path."}
                 {:role "user" :content "continue"}]]
      (is (= [{:role "system" :content "stay grounded"}
              {:role "user" :content "investigate the restart bug"}
              {:role "assistant" :content "I found the failing path."}
              {:role "user" :content "continue"}]
             (agent-runtime/merge-restored-session-messages openplanner redis))))))