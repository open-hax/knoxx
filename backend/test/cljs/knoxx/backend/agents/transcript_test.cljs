(ns knoxx.backend.agents.transcript-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.agent.transcript :as transcript]
            [knoxx.backend.shape.agent :as agent-shape]))

(defn- session-with-messages
  [messages]
  (reify agent-shape/IAgentSession
    (streaming? [_] false)
    (current-turn [_] nil)
    (messages [_] messages)
    (subscribe! [_ _handler] nil)
    (send-user-message! [_ _content] (js/Promise.resolve nil))
    (follow-up! [_ _message] (js/Promise.resolve nil))
    (steer! [_ _message] (js/Promise.resolve nil))
    (set-thinking-level! [_ _level] nil)))

(deftest session->stored-messages-normalizes-supported-provider-content
  (testing "text content arrays are joined and unsupported roles/blank messages are skipped"
    (let [session (session-with-messages
                   [#js {:role "system" :content "rules"}
                    #js {:role "user" :content #js [#js {:type "text" :text "hello"}
                                                      #js {:type "output_text" :text "world"}
                                                      #js {:type "image" :url "image.png"}]}
                    #js {:role "assistant" :text "fallback text"}
                    #js {:role "tool" :content "ignored"}
                    #js {:role "assistant" :content "  "}])]
      (is (= [{:role "system" :content "rules"}
              {:role "user" :content "hello\n\nworld"}
              {:role "assistant" :content "fallback text"}]
             (transcript/session->stored-messages session))))))

(deftest transcript-before-prompt-preserves-rich-session-messages
  (testing "rich transcript keeps compaction summaries, media parts, usage, and system prompt sync"
    (let [session (session-with-messages
                   [#js {:role "compactionSummary"
                         :summary " earlier context "
                         :tokensBefore 123}
                    #js {:role "user"
                         :content #js [#js {:type "text" :text "look"}
                                       #js {:type "image" :url "workspace://image.png" :mimeType "image/png"}]}
                    #js {:role "assistant"
                         :content #js [#js {:type "text" :text "nice"}]
                         :usage #js {:input 1 :output 2}}])
          prompt {:role "user" :content "next"}
          result (transcript/transcript-before-prompt session prompt {:system-prompt "be helpful"})]
      (is (= "system" (:role (first result))))
      (is (= "be helpful" (:content (first result))))
      (is (= {:role "compactionSummary"
              :summary "earlier context"
              :content "earlier context"
              :tokensBefore 123}
             (second result)))
      (is (= {:role "user"
              :content "look"
              :content-parts [{:type "image"
                               :url "workspace://image.png"
                               :mimeType "image/png"}]}
             (nth result 2)))
      (is (= {:role "assistant"
              :content "nice"
              :usage {:input 1 :output 2}}
             (nth result 3)))
      (is (= prompt (last result))))))

(deftest transcript-before-prompt-does-not-duplicate-last-message
  (let [prompt {:role "user" :content "already there"}
        session (session-with-messages [#js {:role "user" :content "already there"}])]
    (is (= [prompt]
           (transcript/transcript-before-prompt session prompt {})))))

(deftest transcript-after-turn-falls-back-only-when-session-has-no-snapshot
  (testing "non-empty provider snapshot wins over fallback"
    (let [session (session-with-messages [#js {:role "assistant" :content "fresh"}])]
      (is (= [{:role "assistant" :content "fresh"}]
             (transcript/transcript-after-turn session [{:role "assistant" :content "stale"}])))))
  (testing "empty session snapshot keeps caller fallback"
    (is (= [{:role "user" :content "fallback"}]
           (transcript/transcript-after-turn (session-with-messages [])
                                             [{:role "user" :content "fallback"}])))))
