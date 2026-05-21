(ns knoxx.backend.agent-runtime-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.agent.runtime :as agent-runtime]
            [knoxx.backend.infra.agent.message :as agent-message]
            [knoxx.backend.infra.agent.session :as agent-session]
            [knoxx.backend.infra.stores.message-source :refer [IMessageSource]]
            ["node:path" :as node-path]))

(deftest resolve-workspace-path-allows-configured-music-library-alias
  (let [runtime #js {:path node-path}
        config {:workspace-root "/tmp/knoxx-workspace"
                :music-library-root "/tmp/music-library"
                :extra-workspace-roots ["/tmp/extra-root"]}]
    (testing "Music/... resolves into the configured music library root"
      (is (= (.resolve node-path "/tmp/music-library" "playlists/ritual.m3u")
             (agent-runtime/resolve-workspace-path runtime config "Music/playlists/ritual.m3u"))))
    (testing "absolute paths under extra allowed roots are accepted"
      (is (= (.resolve node-path "/tmp/extra-root" "stems/take.wav")
             (agent-runtime/resolve-workspace-path runtime config "/tmp/extra-root/stems/take.wav"))))
    (testing "paths outside the allowed roots are rejected"
      (is (thrown-with-msg? js/Error #"Path escapes allowed workspace roots"
                            (agent-runtime/resolve-workspace-path runtime config "../../etc/passwd"))))))

(deftest stored-compaction-summary-rehydrates-as-agent-message
  (let [message (agent-message/stored-session-message->agent-message
                 {:role "compactionSummary"
                  :summary "Earlier sticky context was summarized."
                  :tokensBefore 12345})]
    (is (= "compactionSummary" (aget message "role")))
    (is (= "Earlier sticky context was summarized." (aget message "summary")))
    (is (= 12345 (aget message "tokensBefore")))))

(deftest stored-assistant-message-rehydrates-with-usage-sentinel
  (let [message (agent-message/stored-session-message->agent-message
                 {:role "assistant"
                  :content "Older stored assistant response without usage."})
        usage (aget message "usage")]
    (is (= "assistant" (aget message "role")))
    (is (= 0 (aget usage "totalTokens")))
    (is (= 0 (aget usage "input")))
    (is (= 0 (aget usage "output")))))

(deftest ^:async rehydrate-session-manager-appends-messages-and-system-prompt
  (let [appended*       (atom [])
        session-manager #js {:appendMessage (fn [m] (swap! appended* conj (js->clj m :keywordize-keys true)))}
        message-source  (reify IMessageSource
                          (fetch-messages! [_ _]
                            (js/Promise.resolve
                             [{:role "user" :content "Respect the selected contract."}])))
        result          (await (agent-session/rehydrate-session-manager!
                                message-source
                                session-manager
                                "conversation-123"
                                {:system-prompt "You are the creative music studio agent."}))]
    (testing "reports restored when messages were found"
      (is (true? (:restored result))))
    (testing "system prompt prepended before user messages"
      (is (= 2 (count @appended*)))
      (is (= {:role "system"
              :content [{:type "text" :text "You are the creative music studio agent."}]}
             (select-keys (first @appended*) [:role :content])))
      (is (= {:role "user"
              :content [{:type "text" :text "Respect the selected contract."}]}
             (select-keys (second @appended*) [:role :content]))))))
