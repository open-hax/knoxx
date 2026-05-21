(ns knoxx.backend.extern-agent-message-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.agent-message :as agent-message]
            [knoxx.backend.extern.json :as xjson]))

(deftest stored-assistant-message-adds-usage-sentinel
  (let [message (agent-message/stored-message->agent-message
                 {:role "assistant"
                  :content "Older stored assistant response without usage."})
        result (xjson/to-cljs message)]
    (is (= "assistant" (:role result)))
    (is (= [{:type "text" :text "Older stored assistant response without usage."}]
           (:content result)))
    (is (= {:input 0
            :output 0
            :cacheRead 0
            :cacheWrite 0
            :totalTokens 0}
           (:usage result)))))

(deftest content-part-normalizes-image-data-url
  (let [part (agent-message/content-part->agent-part
              {:type "image"
               :data "data:image/jpeg;base64,abc123"})]
    (is (= {:type "image"
            :data "abc123"
            :mimeType "image/jpeg"}
           (xjson/to-cljs part)))))

(deftest content-part-normalizes-audio-data-url-with-format
  (let [part (agent-message/content-part->agent-part
              {:type "audio"
               :data "data:audio/wav;base64,UklGRg=="})]
    (is (= {:type "audio"
            :data "UklGRg=="
            :mimeType "audio/wav"
            :format "wav"}
           (xjson/to-cljs part)))))

(deftest stored-compaction-summary-message
  (let [message (agent-message/stored-message->agent-message
                 {:role "compactionSummary"
                  :summary "Earlier context."
                  :tokensBefore 42})
        result (xjson/to-cljs message)]
    (is (= {:role "compactionSummary"
            :summary "Earlier context."
            :tokensBefore 42}
           (select-keys result [:role :summary :tokensBefore])))))
