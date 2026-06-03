(ns knoxx.backend.domain.condition.builtin-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.condition.builtin :as builtin]))

(deftest condition-discord-mention-detects-bot-mention
  (testing "true when content mentions bot user id"
    (is (true? (builtin/condition-discord-mention
                 {:event/payload {:gatewayBotUserId "123"
                                  :content "hello <@123>"}})))
    (is (true? (builtin/condition-discord-mention
                 {:event/payload {:gatewayBotUserId "123"
                                  :content "<@!123> hi"}})))))

(deftest condition-discord-mention-rejects-non-mentions
  (testing "false when no bot user id or no mention"
    (is (false? (builtin/condition-discord-mention
                 {:event/payload {:gatewayBotUserId "123"
                                  :content "hello world"}})))
    (is (false? (builtin/condition-discord-mention
                 {:event/payload {:content "<@123>"}})))
    (is (false? (builtin/condition-discord-mention {})))))

(deftest condition-discord-keyword-matches-keywords
  (testing "true when content contains any keyword"
    (is (true? (builtin/condition-discord-keyword
                 {:event/payload {:content "hello Frankie"}}
                 ["frankie" "yap"])))
    (is (true? (builtin/condition-discord-keyword
                 {:event/payload {:content "YAP session"}}
                 ["frankie" "yap"])))))

(deftest condition-discord-keyword-empty-list-passes
  (testing "true when keyword list is empty"
    (is (true? (builtin/condition-discord-keyword
                 {:event/payload {:content "anything"}}
                 [])))))

(deftest condition-discord-keyword-rejects-mismatches
  (testing "falsey when no keywords match"
    (is (not (builtin/condition-discord-keyword
              {:event/payload {:content "hello world"}}
              ["frankie"])))))

(deftest condition-discord-channel-matches-channel
  (testing "true when channel id is in allowed list"
    (is (true? (builtin/condition-discord-channel
                 {:event/payload {:channelId "456"}}
                 ["123" "456"])))))

(deftest condition-discord-channel-empty-list-passes
  (testing "true when channel list is empty"
    (is (true? (builtin/condition-discord-channel
                 {:event/payload {:channelId "456"}}
                 [])))))

(deftest condition-discord-channel-rejects-mismatches
  (testing "false when channel id is not in list"
    (is (false? (builtin/condition-discord-channel
                 {:event/payload {:channelId "999"}}
                 ["123" "456"])))))

(deftest condition-discord-author-matches-author
  (testing "true when author id is in allowed list"
    (is (true? (builtin/condition-discord-author
                 {:event/payload {:authorId "789"}}
                 ["789"])))))

(deftest condition-discord-author-empty-list-passes
  (testing "true when author list is empty"
    (is (true? (builtin/condition-discord-author
                 {:event/payload {:authorId "789"}}
                 [])))))

(deftest condition-discord-author-rejects-mismatches
  (testing "false when author id is not in list"
    (is (false? (builtin/condition-discord-author
                 {:event/payload {:authorId "999"}}
                 ["789"])))))

(deftest condition-always-returns-true
  (testing "always returns true regardless of args"
    (is (true? (builtin/condition-always)))
    (is (true? (builtin/condition-always 1 2 3)))
    (is (true? (builtin/condition-always nil)))))

(deftest condition-never-returns-false
  (testing "always returns false regardless of args"
    (is (false? (builtin/condition-never)))
    (is (false? (builtin/condition-never 1 2 3)))
    (is (false? (builtin/condition-never nil)))))
