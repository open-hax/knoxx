(ns knoxx.backend.source-runtime-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.source.runtime :as source-runtime]))

(def discord-source-record
  {:resource/kind :source
   :resource/class "sources"
   :resource/id "discord_gateway"
   :resource/definition {:contract/id "discord_gateway"
                         :contract/kind :source
                         :enabled true
                         :source/id :source/discord-gateway
                         :source/type :event-generator
                         :source/driver :driver/discord
                         :source/actor "discord_automation"
                         :source/listens [:discord.message]}})

(deftest ^:async source-runtime-dispatches-through-actor-owned-source
  (testing "a driver event becomes a dispatcher event only through a matching source"
    (with-redefs [resources/load-all-resources-sync (fn [_] [discord-source-record])]
      (let [result (await (source-runtime/dispatch-driver-event!
                           {}
                           :driver/discord
                           "discord_automation"
                           {:event/type :discord.message
                            :event/payload {:content "hello"}}))]
        (is (= [] (:matchedTriggers result)))
        (is (= :discord.message (get-in result [:event :event/type])))
        (is (= "discord_automation" (get-in result [:event :event/actor])))
        (is (= {:kind :discord
                :driver :driver/discord
                :source :source/discord-gateway}
               (get-in result [:event :event/generator])))))))

(deftest ^:async source-runtime-skips-unregistered-actor-source
  (testing "multiple bots are resource-gated by actor-owned source resources"
    (with-redefs [resources/load-all-resources-sync (fn [_] [discord-source-record])]
      (let [result (await (source-runtime/dispatch-driver-event!
                           {}
                           :driver/discord
                           "other_discord_bot"
                           {:event/type :discord.message
                            :event/payload {:content "hello"}}))]
        (is (= true (:skipped result)))
        (is (= :no-matching-source (:reason result)))))))
