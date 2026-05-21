(ns knoxx.backend.domain.condition.registry-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.condition.registry :as registry]
            [knoxx.backend.domain.condition.builtin :as builtin]))

(deftest register-and-evaluate-condition
  (testing "conditions can be registered and evaluated"
    (registry/register-condition! :conditions/test.always
                                  (fn [_event _actor _trigger _config] true))
    (is (true? (registry/evaluate '(conditions/test.always event)
                                  {:event/type :test/event}
                                  nil nil nil)))
    (is (false? (registry/evaluate '(conditions/test.never event)
                                   {:event/type :test/event}
                                   nil nil nil)))))

(deftest safe-eval-boolean-combinators
  (testing "and/or/not work in condition expressions"
    (registry/register-condition! :conditions/test.true (fn [& _] true))
    (registry/register-condition! :conditions/test.false (fn [& _] false))
    (is (true? (registry/evaluate '(and (conditions/test.true event)
                                        (conditions/test.true event))
                                  {} nil nil nil)))
    (is (false? (registry/evaluate '(and (conditions/test.true event)
                                         (conditions/test.false event))
                                   {} nil nil nil)))
    (is (true? (registry/evaluate '(or (conditions/test.false event)
                                       (conditions/test.true event))
                                  {} nil nil nil)))
    (is (false? (registry/evaluate '(not (conditions/test.true event))
                                   {} nil nil nil)))))

(deftest safe-eval-comparisons
  (testing "comparison operators work"
    (is (true? (registry/evaluate '(= 1 1) {} nil nil nil)))
    (is (false? (registry/evaluate '(= 1 2) {} nil nil nil)))
    (is (true? (registry/evaluate '(< 1 2) {} nil nil nil)))
    (is (true? (registry/evaluate '(>= 2 2) {} nil nil nil)))))

(deftest safe-eval-with-bindings
  (testing "event/actor/trigger/config bindings are available"
    (is (= "hello" (registry/safe-eval 'event {'event "hello"})))))

(deftest condition-fails-closed-on-error
  (testing "unknown condition returns false, not throws"
    (is (false? (registry/evaluate '(conditions/test.does-not-exist event)
                                   {} nil nil nil)))))

(deftest builtin-discord-mention-condition
  (testing "discord.mention detects bot mentions in event payload"
    (builtin/register-builtins!)
    (let [event {:event/payload {:gatewayBotUserId "12345"
                                 :content "hello <@12345>"}}]
      (is (true? (registry/evaluate '(conditions/discord.mention event)
                                    event nil nil nil))))
    (let [event {:event/payload {:gatewayBotUserId "12345"
                                 :content "hello world"}}]
      (is (false? (registry/evaluate '(conditions/discord.mention event)
                                     event nil nil nil))))
    (let [event {:event/payload {:content "no bot id"}}]
      (is (false? (registry/evaluate '(conditions/discord.mention event)
                                     event nil nil nil))))))

(deftest builtin-discord-keyword-condition
  (testing "discord.keyword matches keywords case-insensitively"
    (builtin/register-builtins!)
    (let [event {:event/payload {:content "I love frankie music"}}]
      (is (true? (registry/evaluate
                  '(conditions/discord.keyword event ["frankie" "yap"])
                  event nil nil nil))))
    (let [event {:event/payload {:content "nothing relevant here"}}]
      (is (false? (registry/evaluate
                   '(conditions/discord.keyword event ["frankie"])
                   event nil nil nil))))
    (let [event {:event/payload {:content "FRANKIE is great"}}]
      (is (true? (registry/evaluate
                  '(conditions/discord.keyword event ["frankie"])
                  event nil nil nil))))))

(deftest builtin-discord-channel-condition
  (testing "discord.channel filters by channel id"
    (builtin/register-builtins!)
    (let [event {:event/payload {:channelId "123"}}]
      (is (true? (registry/evaluate
                  '(conditions/discord.channel event ["123" "456"])
                  event nil nil nil))))
    (let [event {:event/payload {:channelId "999"}}]
      (is (false? (registry/evaluate
                   '(conditions/discord.channel event ["123"])
                   event nil nil nil))))))

(deftest complex-condition-expression
  (testing "or of mention and keyword"
    (builtin/register-builtins!)
    (let [mention-event {:event/payload {:gatewayBotUserId "123"
                                         :content "<@123> hello"
                                         :channelId "1"}}
          keyword-event {:event/payload {:content "frankie rocks"
                                         :channelId "1"}}
          no-match-event {:event/payload {:content "boring"
                                          :channelId "1"}}]
      (is (true? (registry/evaluate
                  '(or (conditions/discord.mention event)
                       (conditions/discord.keyword event ["frankie"]))
                  mention-event nil nil nil)))
      (is (true? (registry/evaluate
                  '(or (conditions/discord.mention event)
                       (conditions/discord.keyword event ["frankie"]))
                  keyword-event nil nil nil)))
      (is (false? (registry/evaluate
                   '(or (conditions/discord.mention event)
                        (conditions/discord.keyword event ["frankie"]))
                   no-match-event nil nil nil))))))