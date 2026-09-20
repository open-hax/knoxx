(ns knoxx.backend.extern.discord-gateway-test
  (:require [cljs.test :as t]
            [knoxx.backend.domain.discord.gateway :as gateway]
            [knoxx.backend.extern.discord-gateway-client :as client]
            [knoxx.backend.extern.discord-gateway-codec :as codec]
            [knoxx.backend.extern.discord-gateway-messages :as messages]
            [knoxx.backend.extern.discord-gateway-voice :as voice]))

(defn- with-default-manager [run]
  (let [previous (gateway/gateway-manager)
        sentinel #js {:sentinel true}]
    (gateway/set-manager! sentinel)
    (try
      (run sentinel)
      (finally (gateway/set-manager! previous)))))

(t/deftest native-factory-respects-explicit-false-for-both-option-shapes
  (with-default-manager
    (fn [sentinel]
      (doseq [opts [{:set-default? false} #js {:setDefault false}]]
        (let [manager (gateway/createDiscordGatewayManager opts)]
          (t/is (identical? sentinel (gateway/gateway-manager)))
          (t/is (false? (aget (.status manager) "ready"))))))))

(t/deftest native-factory-still-installs-default-when-unspecified
  (with-default-manager
    (fn [_]
      (doseq [opts [nil {} #js {} {:set-default? true} #js {:setDefault true}]]
        (let [manager (gateway/createDiscordGatewayManager opts)]
          (t/is (identical? manager (gateway/gateway-manager))))))))

(t/deftest actor-manager-creation-preserves-default-and-existing-actor
  (with-default-manager
    (fn [sentinel]
      (let [actor-id (str "gateway-fixture-" (random-uuid))]
        (try
          (let [manager (gateway/ensure-actor-manager! actor-id)]
            (t/is (identical? sentinel (gateway/gateway-manager)))
            (t/is (identical? manager (gateway/gateway-manager actor-id)))
            (t/is (identical? manager (gateway/ensure-actor-manager! actor-id))))
          (finally (gateway/set-actor-manager! actor-id nil)))))))

(defn- native-message []
  #js {:id "m1" :channelId "channel" :content "café"
       :guild #js {:id "guild"}
       :author #js {:id "writer" :username "writer-name" :bot false}
       :member #js {:roles #js {:cache (js/Map. #js [#js ["editor" #js {}]])}}
       :createdAt (js/Date. "2026-09-12T00:00:00Z")
       :attachments (js/Map.) :embeds #js []})

(t/deftest native-message-and-audio-codecs-preserve-wire-fields
  (let [mapped (js->clj (codec/map-message (native-message)) :keywordize-keys true)
        pcm (js/Buffer.from #js [1 0 2 0 3 0 4 0])
        wav (codec/pcm16le->wav-buffer pcm 48000 2)]
    (t/is (= ["m1" "channel" "guild" "café" ["editor"]]
           ((juxt :id :channelId :guildId :content :authorRoleIds) mapped)))
    (t/is (= "2026-09-12T00:00:00.000Z" (:timestamp mapped)))
    (t/is (= "RIFF" (.toString wav "ascii" 0 4)))
    (t/is (= 48000 (.readUInt32LE wav 24)))
    (t/is (= 2 (.readUInt16LE wav 22)))
    (t/is (= 8 (.readUInt32LE wav 40)))
    (t/is (.equals pcm (.subarray wav 44)))))

(t/deftest ^:async native-discord-client-wires-events-without-login
  (let [received (atom []) message (native-message)
        discord-client (client/build-discord-client
                         #js {} #(swap! received conj %) (fn [& _]) (fn [& _]))]
    (try
      (.emit discord-client "messageCreate" message)
      (t/is (identical? message (first @received)))
      (t/is (= 1 (count @received)))
      (t/is (false? (.isReady discord-client)))
      (finally (await (.destroy discord-client))))))

(t/deftest ^:async publishing-chunks-attaches-files-and-reply-once
  (let [sent (atom [])
        channel #js {:isTextBased (fn [] true) :send #(swap! sent conj %)}
        native-client #js {:channels #js {:fetch (fn [_] channel)}}
        text (apply str (repeat 2001 "x"))
        buffer (js/Buffer.from "café" "utf8")
        result (await (messages/gw-send-message
                        (fn [] native-client) "channel" text "reply-id"
                        [{:buffer buffer :filename "note.txt"}]))]
    (t/is (= [2000 1] (mapv #(count (aget % "content")) @sent)))
    (t/is (= "reply-id" (.. (first @sent) -reply -messageReference)))
    (t/is (= "note.txt" (aget (aget (aget (first @sent) "files") 0) "name")))
    (t/is (identical? buffer (aget (aget (aget (first @sent) "files") 0) "attachment")))
    (t/is (nil? (aget (second @sent) "files")))
    (t/is (nil? (aget (second @sent) "reply")))
    (t/is (= [true 2 1] (mapv #(aget result %) ["sent" "chunkCount" "attachmentCount"])))))

(t/deftest ^:async active-voice-lookup-returns-the-connection-handle
  (let [manager (gateway/createDiscordGatewayManager {:set-default? false})
        destroyed (atom 0)
        connection #js {:guildId "guild" :destroy #(swap! destroyed inc)}]
    (with-redefs [voice/gw-join-voice (fn [_ _] connection)]
      (await (.joinVoice manager "voice-channel"))
      (t/is (identical? connection (.getVoiceConnection manager "guild")))
      (t/is (identical? connection (.getVoiceConnection manager)))
      (await (.stop manager))
      (t/is (= 1 @destroyed))
      (t/is (nil? (.getVoiceConnection manager))))))
