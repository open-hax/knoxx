(ns knoxx.backend.extern-agent-turn-result-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.agent-message :as agent-message]
            [knoxx.backend.extern.agent-turn-result :as agent-turn-result]))

(deftest usage-tokens-extracts-provider-usage
  (let [message (agent-message/stored-message->agent-message
                 {:role "assistant"
                  :content "Done."
                  :usage {:input 12
                          :output 34}})]
    (is (= {:input-tokens 12
            :output-tokens 34}
           (agent-turn-result/usage-tokens message)))))

(deftest usage-tokens-defaults-missing-values-to-zero
  (testing "nil assistant messages are safe"
    (is (= {:input-tokens 0
            :output-tokens 0}
           (agent-turn-result/usage-tokens nil))))
  (testing "assistant messages without explicit usage are safe"
    (let [message (agent-message/stored-message->agent-message
                   {:role "assistant"
                    :content "Done."})]
      (is (= {:input-tokens 0
              :output-tokens 0}
             (agent-turn-result/usage-tokens message))))))
