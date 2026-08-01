(ns knoxx.backend.triggers.trigger-matching-test
  (:require [cljs.test :refer [deftest is testing]]
            [cljs.reader :as reader]
            [knoxx.backend.domain.trigger.normalize :as trigger-normalize]
            [knoxx.backend.domain.event.normalize :as event-normalize]
            [knoxx.backend.domain.condition.builtin :as condition-builtins]
            [knoxx.backend.domain.condition.registry :as condition-registry]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]))

;; Load the actual trigger EDN from string so symbols are not compiled
(def trigger-edn
  (reader/read-string
    "{:contract/kind :trigger\n     :contract/id \"ussyverse_social_replies_event\"\n     :enabled true\n     :trigger/kind :event\n     :trigger/actor \"discord_automation\"\n     :trigger/events [:discord.message]\n     :trigger/condition (or (conditions/discord.mention event)\n                            (conditions/discord.keyword event [\"frankie\" \"yap\" \"music\" \"song\" \"slop\" \"ussy\"\n                                                               \"beat\" \"loop\" \"track\" \"art\" \"create\" \"mix\" \"drop\"]))\n     :trigger/action :actions/start-agent-session\n     :trigger/agent \"ussyverse_social_replies\"\n     :trigger/task \"A Discord event fired in one of your home channels. Read nearby context, inspect links or attachment URLs if useful, and decide whether to reply in-channel. Replies should feel alive, funny, musical, or socially connective.\"\n     :data {:context {:reason \"contract event: ussyverse social replies\"}}}"))

(def fixture-config
  {:contracts-dir "test/fixtures/trigger-contracts"})

(deftest trigger-normalizes-correctly
  (testing "the trigger EDN normalizes legacy arguments into :trigger/with"
    (let [normalized (trigger-normalize/normalize-trigger trigger-edn)]
      (is (= "ussyverse_social_replies_event" (:trigger/id normalized)))
      (is (= :event (:trigger/kind normalized)))
      (is (true? (:trigger/enabled? normalized)))
      (is (= ["discord_automation"] (mapv str [(:trigger/actor normalized)])))
      (is (= [:discord.message] (:trigger/events normalized)))
      (is (= :actions/start-agent-session (:trigger/action normalized)))
      (is (= "ussyverse_social_replies" (get-in normalized [:trigger/with :agent-id])))
      (is (string? (get-in normalized [:trigger/with :task])))
      (is (nil? (:trigger/agent normalized)) "legacy agent must not remain a parallel runtime field"))))

(deftest keyword-condition-matches
  (testing "the trigger condition matches when message contains a keyword"
    (condition-builtins/register-builtins!)
    (let [normalized (trigger-normalize/normalize-trigger trigger-edn)
          event (event-normalize/normalize-event
                 {:event/type :discord.message
                  :event/payload {:content "hey frankie what's up"
                                  :gatewayBotUserId "12345"
                                  :channelId "123"}})]
      (is (true? (condition-registry/evaluate (:trigger/condition normalized)
                                              event nil normalized nil))))))

(deftest mention-condition-matches
  (testing "the trigger condition matches when message mentions the bot"
    (condition-builtins/register-builtins!)
    (let [normalized (trigger-normalize/normalize-trigger trigger-edn)
          event (event-normalize/normalize-event
                 {:event/type :discord.message
                  :event/payload {:content "hello <@12345>"
                                  :gatewayBotUserId "12345"
                                  :channelId "123"}})]
      (is (true? (condition-registry/evaluate (:trigger/condition normalized)
                                              event nil normalized nil))))))

(deftest no-match-condition-rejects
  (testing "the trigger condition does not match unrelated messages"
    (condition-builtins/register-builtins!)
    (let [normalized (trigger-normalize/normalize-trigger trigger-edn)
          event (event-normalize/normalize-event
                 {:event/type :discord.message
                  :event/payload {:content "hello world"
                                  :gatewayBotUserId "12345"
                                  :channelId "123"}})]
      (is (false? (condition-registry/evaluate (:trigger/condition normalized)
                                               event nil normalized nil))))))

(defn- ussyverse-trigger-record
  []
  {:resource/id "ussyverse_social_replies_event"
   :resource/kind :trigger
   :resource/class "triggers"
   :resource/definition trigger-edn})

(deftest ^:async ussyverse-trigger-dispatches-on-keyword
  (testing "full dispatch pipeline: keyword event reaches the trigger"
    (condition-builtins/register-builtins!)
    (with-redefs [resources/load-all-resources-sync (fn [_] [(ussyverse-trigger-record)])]
      (event-dispatch/reset-dedup!)
      (let [result (await (event-dispatch/dispatch!
                           fixture-config
                           {:event/type :discord.message
                            :event/actor "discord_automation"
                            :event/payload {:content "hey frankie what's up"
                                            :gatewayBotUserId "12345"
                                            :gatewayActorId "discord_automation"
                                            :channelId "123"
                                            :id "msg-1"}}))]
        (is (= ["ussyverse_social_replies_event"] (:matchedTriggers result))
            (str "Expected trigger to match but got: " (pr-str result)))))))
