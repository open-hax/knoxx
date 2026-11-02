(ns knoxx.backend.triggers.source-dispatch-test
  (:require [cljs.test :refer [deftest is testing use-fixtures]]
            [cljs.reader :as reader]
            [knoxx.backend.domain.condition.builtin :as condition-builtins]
            [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.source.runtime :as source-runtime]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.triggers.action-fixture :as action-fixture]))

(def action-calls (atom []))
(use-fixtures :each (action-fixture/recording-fixture action-calls))

(def discord-source
  {:contract/id "discord_gateway"
   :contract/kind :source
   :source/id :source/discord-gateway
   :source/type :event-generator
   :source/name "Discord gateway"
   :source/driver :driver/discord
   :source/actor "discord_automation"
   :source/listens [:discord.message
                    :discord.message.mention
                    :discord.message.created
                    :discord.voice.state-update]})

(def discord-source-record
  {:resource/id "discord_gateway"
   :resource/kind :source
   :resource/class "sources"
   :resource/definition discord-source})

(def trigger-edn
  (reader/read-string
    "{:contract/kind :trigger\n     :contract/id \"ussyverse_social_replies_event\"\n     :enabled true\n     :trigger/kind :event\n     :trigger/actor \"discord_automation\"\n     :trigger/events [:discord.message]\n     :trigger/condition (or (conditions/discord.mention event)\n                            (conditions/discord.keyword event [\"frankie\"]))\n     :trigger/action :actions/start-agent-session\n     :trigger/agent \"ussyverse_social_replies\"\n     :trigger/task \"Test task\"\n     :data {:context {:reason \"test\"}}}"))

(def trigger-record
  {:resource/id "ussyverse_social_replies_event"
   :resource/kind :trigger
   :resource/class "triggers"
   :resource/definition trigger-edn})

(def fixture-config
  {:contracts-dir "test/fixtures/trigger-contracts"})

(deftest ^:async discord-source-dispatches-through-trigger
  (testing "source normalization and trigger matching deliver the configured action"
    (driver-builtin/register-built-in-drivers!)
    (condition-builtins/register-builtins!)
    (event-dispatch/reset-dedup!)
    (with-redefs [resources/load-all-resources-sync (fn [_] [discord-source-record trigger-record])]
      (let [result (await (source-runtime/dispatch-driver-event!
                           fixture-config :driver/discord "discord_automation"
                           {:event/type :discord.message
                            :event/payload {:content "hey frankie" :gatewayBotUserId "12345"
                                            :gatewayActorId "discord_automation" :channelId "123"}}))]
        (is (= ["ussyverse_social_replies_event"] (:matchedTriggers result)))
        (is (= 1 (count @action-calls)))
        (is (= "discord_automation" (get-in @action-calls [0 :ctx :event :event/actor])))
        (is (= "Test task" (get-in @action-calls [0 :action :action/with :task])))))))
