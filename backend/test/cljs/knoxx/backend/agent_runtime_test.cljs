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

(deftest sync-system-message-replaces-stale-agent-persona
  (testing "switching agents rewrites the restored system prompt instead of keeping the old persona"
    (let [messages [{:role "system" :content "You are Knoxx, the grounded workspace actor."}
                    {:role "user" :content "make this more musical"}]]
      (is (= [{:role "system" :content "You are Knoxx's creative music studio agent."}
              {:role "user" :content "make this more musical"}]
             (agent-runtime/sync-system-message messages "You are Knoxx's creative music studio agent."))))))

(deftest sync-system-message-inserts-missing-system-prompt
  (testing "missing restored system prompts are inserted at the front of the transcript"
    (let [messages [{:role "user" :content "hello"}
                    {:role "assistant" :content "hi"}]]
      (is (= [{:role "system" :content "stay grounded"}
              {:role "user" :content "hello"}
              {:role "assistant" :content "hi"}]
             (agent-runtime/sync-system-message messages "stay grounded"))))))
