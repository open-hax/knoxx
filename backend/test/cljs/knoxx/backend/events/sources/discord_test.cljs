(ns knoxx.backend.events.sources.discord-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.discord.source :as discord-source]))

(deftest ^:async publish-channels-are-not-read-channel-fallbacks
  (let [channel-ids (await (discord-source/resolve-channel-ids! {:explicit-channels []
                                                                  :publish-channels ["publish-only"]}))]
    (is (= [] channel-ids))))
