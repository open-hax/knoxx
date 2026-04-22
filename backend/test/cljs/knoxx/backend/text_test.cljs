(ns knoxx.backend.text-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.text :as text]))

(deftest assistant-message-text-collapses-duplicate-and-cumulative-parts
  (testing "assistant text prefers the longest cumulative segment instead of repeating mirrored parts"
    (let [message #js {:content #js [#js {:type "text" :text "Not"}
                                     #js {:type "output_text" :text "Not much — I’m here."}
                                     #js {:type "text" :text "Not much — I’m here."}] }]
      (is (= "Not much — I’m here."
             (text/assistant-message-text message))))))

(deftest assistant-message-reasoning-text-collapses-overlapping-parts
  (testing "reasoning text preserves independent fragments but removes cumulative overlap"
    (let [message #js {:content #js [#js {:type "reasoning" :text "Think"}
                                     #js {:type "reasoning_text" :text "Thinking step 1"}
                                     #js {:type "thinking" :thinking "Thinking step 1\nThinking step 2"}] }]
      (is (= "Thinking step 1\nThinking step 2"
             (text/assistant-message-reasoning-text message))))))
