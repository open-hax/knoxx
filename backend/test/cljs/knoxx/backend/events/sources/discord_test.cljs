(ns knoxx.backend.events.sources.discord-test
  (:require [cljs.test :refer-macros [async deftest is]]
            [knoxx.backend.domain.discord.source :as discord-source]))

(deftest publish-channels-are-not-read-channel-fallbacks
  (async done
    (-> (discord-source/resolve-channel-ids! {:explicit-channels []
                                              :publish-channels ["publish-only"]})
        (.then (fn [channel-ids]
                 (is (= [] channel-ids))
                 (done)))
        (.catch (fn [err]
                  (is false (str "rejected: " (.-message err)))
                  (done))))))
