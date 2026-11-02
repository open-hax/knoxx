(ns knoxx.backend.extern.discord-gateway-test
  (:require [cljs.test :as t]
            [knoxx.backend.domain.discord.gateway :as gateway]
            [knoxx.backend.extern.discord-gateway-client :as client]
            [knoxx.backend.extern.discord-gateway-codec :as codec]
            [knoxx.backend.extern.discord-gateway-messages :as messages]
            [knoxx.backend.extern.discord-gateway-voice :as voice]
            [knoxx.backend.extern.discord-gateway-voice-listener :as listener]))

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

(defn- ^:async teardown-resources* []
  (let [manager (gateway/createDiscordGatewayManager {:set-default? false})
        events (atom []) conn #js {:guildId "guild" :destroy #(swap! events conj :destroy)}]
    (with-redefs [voice/gw-join-voice (fn [_ _] conn)
                  listener/gw-start-voice-listener (fn [& _] (js/Promise.resolve #(swap! events conj :listener-stop)))]
      (await (.joinVoice manager "channel"))
      (let [stop-listener (await (.startVoiceListener manager "guild" nil nil))]
        (await (.restart manager ""))
        (t/is (= [:listener-stop :destroy] @events))
        (t/is (nil? (.getVoiceConnection manager "guild")))
        (stop-listener)
        (t/is (= [:listener-stop :destroy] @events))))))

(t/deftest teardown-cleans-voice-resources
  (t/async done
    ((^:async fn [] (try (await (teardown-resources*)) (catch js/Error e (t/is false (str "teardown rejected: " e))) (finally (done)))))))

(defn- ^:async late-listener* []
  (let [manager (gateway/createDiscordGatewayManager {:set-default? false})
        resolve-stop (atom nil) stopped (atom 0)
        pending (js/Promise. (fn [resolve _] (reset! resolve-stop resolve)))]
    (with-redefs [listener/gw-start-voice-listener (fn [& _] pending)]
      (let [started (.startVoiceListener manager "guild" nil nil)]
        (await (.stop manager))
        (@resolve-stop #(swap! stopped inc))
        (let [stop-listener (await started)]
          (t/is (= 1 @stopped))
          (stop-listener)
          (t/is (= 1 @stopped)))))))

(t/deftest late-listener-cannot-outlive-shutdown
  (t/async done
    ((^:async fn [] (try (await (late-listener*)) (catch js/Error e (t/is false (str "late listener rejected: " e))) (finally (done)))))))

(defn- ^:async token-replacement* []
  (let [events (atom [])
        fake-client (fn [& _] #js {:login (fn [_] (js/Promise.resolve true))
                                    :destroy (fn [] (swap! events conj :client-stop))})
        conn #js {:guildId "guild" :destroy #(swap! events conj :voice-stop)}]
    (with-redefs [client/build-discord-client fake-client
                  voice/gw-join-voice (fn [_ _] conn)
                  listener/gw-start-voice-listener (fn [& _] (js/Promise.resolve #(swap! events conj :listener-stop)))]
      (let [manager (gateway/createDiscordGatewayManager {:set-default? false})]
        (await (.start manager "old-fixture-token"))
      (await (.joinVoice manager "channel"))
      (await (.startVoiceListener manager "guild" nil nil))
      (await (.start manager "new-fixture-token"))
      (t/is (= [:listener-stop :voice-stop :client-stop] @events))
      (t/is (nil? (.getVoiceConnection manager "guild")))
        (await (.stop manager))))))

(t/deftest token-replacement-cleans-resources
  (t/async done
    ((^:async fn [] (try (await (token-replacement*)) (catch js/Error e (t/is false (str "replacement rejected: " e))) (finally (done)))))))

(defn- ^:async leave-result* []
  (let [manager (gateway/createDiscordGatewayManager {:set-default? false})]
    (with-redefs [voice/gw-leave-voice (fn [_ _] (js/Promise.resolve true))]
      (let [result (await (.leaveVoice manager "guild"))]
        (t/is (= "guild" (.-guildId result)))
        (t/is (true? (.-left result)))))
    (with-redefs [voice/gw-leave-voice (fn [_ _] (js/Promise.reject (js/Error. "leave failed")))]
      (let [failure (try (await (.leaveVoice manager "guild")) nil
                         (catch js/Error e (.-message e)))]
        (t/is (= "leave failed" failure))))))

(t/deftest leave-waits-and-propagates-failure
  (t/async done
    ((^:async fn [] (try (await (leave-result*)) (catch js/Error e (t/is false (str "leave rejected unexpectedly: " e))) (finally (done)))))))

(defn- ^:async pending-join-shutdown* []
  (let [manager (gateway/createDiscordGatewayManager {:set-default? false})
        resolve-join (atom nil) destroyed (atom 0)
        pending (js/Promise. (fn [resolve _] (reset! resolve-join resolve)))
        conn #js {:guildId "guild" :destroy #(swap! destroyed inc)}]
    (with-redefs [voice/gw-join-voice (fn [_ _] pending)]
      (let [joining (.joinVoice manager "channel")]
        (await (.stop manager))
        (@resolve-join conn)
        (let [error (try (await joining) nil (catch js/Error e (.-message e)))]
          (t/is (= "Voice join cancelled by gateway shutdown" error)))
        (t/is (= 1 @destroyed) "late join is destroyed")
        (t/is (nil? (.getVoiceConnection manager "guild")) "late join is not registered")))))

(t/deftest pending-join-cannot-outlive-shutdown
  (t/async done
    ((^:async fn [] (try (await (pending-join-shutdown*))
                         (catch js/Error e (t/is false (str "join test rejected: " e)))
                         (finally (done)))))))
