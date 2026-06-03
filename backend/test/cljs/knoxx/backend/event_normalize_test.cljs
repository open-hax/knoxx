(ns knoxx.backend.event-normalize-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.event.normalize :as event-normalize]))

(deftest dotted-json-event-types-stay-resource-keywords
  (testing "JSON admin dispatch can match EDN trigger events such as :discord.message"
    (is (= :discord.message (event-normalize/event-type "discord.message")))
    (is (= :discord.message.mention (event-normalize/event-type "discord.message.mention")))
    (is (= :message/greeting (event-normalize/event-type "message/greeting")))))
