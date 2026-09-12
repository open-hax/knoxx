(ns knoxx.backend.extern.discord-tools-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.discord.tools :as legacy]
            [knoxx.backend.extern.discord-message-selection :as selection]
            [knoxx.backend.extern.discord-tool-operations :as operations]
            [knoxx.backend.extern.discord-upload :as upload]))

(def expected-tool-ids
  ["discord.publish" "discord.send" "discord.react" "discord.thread.create"
   "discord.read" "discord.channel.messages" "discord.channel.scroll"
   "discord.dm.messages" "discord.search" "discord.guilds" "discord.list.servers"
   "discord.channels" "discord.list.channels"])

(defn- tool-ids [tools]
  (mapv #(aget % "name") (array-seq tools)))

(deftest compatibility-catalog-preserves-thirteen-tools-and-policy-selection
  (is (= expected-tool-ids (tool-ids (legacy/create-discord-custom-tools {} {}))))
  (is (= [] (tool-ids (legacy/create-discord-custom-tools {} {} {}))))
  (is (= ["discord.read"]
         (tool-ids (legacy/create-discord-custom-tools
                     {} {} {:tool-policies [{:tool-id "discord.read" :effect "allow"}
                                            {:tool-id "discord.publish" :effect "deny"}]})))))

(deftest ^:async native-publish-adapts-camel-case-and-snake-case-parameters
  (let [calls (atom []) updates (atom [])]
    (with-redefs [operations/discord-send-message!
                  (fn [runtime config channel text reply attachments]
                    (swap! calls conj [runtime config channel text reply attachments])
                    {:messageId "message-1" :sent true})]
      (let [tool (legacy/publish-tool {:actor "a"} {:project "p"})]
        (doseq [params [#js {:channelId "channel" :content "hello"
                            :attachmentUrls #js [" photo.png " ""]}
                        {:channel_id "channel" :text "hello"
                         :attachment_urls ["photo.png"]}]]
          (let [result (await ((aget tool "execute") "call" params
                               #(swap! updates conj (js->clj % :keywordize-keys true)) nil nil))]
            (is (= "message-1" (aget (aget result "details") "messageId")))))
        (is (= (repeat 2 [{:actor "a"} {:project "p"} "channel" "hello" nil ["photo.png"]]) @calls))
        (is (= ["Sending Discord message to channel…" "Sending Discord message to channel…"]
               (mapv #(get-in % [:content 0 :text]) @updates)))))))

(deftest ^:async attachment-data-url-preserves-utf8-and-refuses-malformed-input
  (let [attachment (await (legacy/fetch-discord-upload-attachment!
                           {} {} "data:text/plain;charset=utf-8,caf%C3%A9" 2))]
    (is (= "text/plain" (:mimeType attachment)))
    (is (= "café" (.toString (:buffer attachment) "utf8"))))
  (try
    (await (upload/fetch-discord-upload-attachment! {} {} "data:text/plain" 0))
    (is false "Malformed data URLs must refuse")
    (catch :default error
      (is (= "Invalid data URL attachment" (.-message error))))))

(deftest selection-keeps-good-chronology-and-removes-bad-context
  (let [rows [{:id "neutral" :timestamp "2026-01-01T00:00:00Z"}
              {:id "good-new" :timestamp "2026-01-03T00:00:00Z" :quality "good"}
              {:id "bad" :timestamp "2026-01-01T00:00:00Z" :quality "bad"}
              {:id "good-old" :timestamp "2026-01-02T00:00:00Z" :quality "good"}]]
    (is (= ["good-old" "good-new" "neutral"]
           (mapv :id (selection/good-first-then-not-bad rows))))))
