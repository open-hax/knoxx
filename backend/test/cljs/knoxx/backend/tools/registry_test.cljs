(ns knoxx.backend.tools.registry-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.registry.tools :as registry]))

(deftest normalize-tool-id-accepts-sanitized-runtime-aliases
  (testing "dot tools can be called with underscore-sanitized ids"
    (is (= "discord.read" (registry/normalize-tool-id "discord_read")))
    (is (= "discord.voice.stop_listen" (registry/normalize-tool-id "discord_voice_stop_listen")))
    (is (= "events.dispatch" (registry/normalize-tool-id "events_dispatch")))
    (is (= "actors.send-message" (registry/normalize-tool-id "actors_send-message")))))

(deftest normalize-tool-id-preserves-canonical-underscore-tools
  (testing "canonical underscore ids stay unchanged"
    (is (= "memory_search" (registry/normalize-tool-id "memory_search")))
    (is (= "semantic_query" (registry/normalize-tool-id "semantic_query")))))
