(ns knoxx.backend.extern-agent-turn-prompt-test
  (:require [cljs.test :refer [deftest is]]
            [clojure.string :as str]
            [knoxx.backend.extern.agent-turn-prompt :as agent-turn-prompt]
            [knoxx.backend.extern.json :as xjson]))

(deftest prompt-content-keeps-text-only-prompts-as-strings
  (is (= "hello"
         (agent-turn-prompt/prompt-content [] "hello")))
  (is (= "text"
         (agent-turn-prompt/content-type "hello"))))

(deftest prompt-content-builds-multipart-js-content
  (let [content (agent-turn-prompt/prompt-content
                 [{:type "image"
                   :data "abc123"
                   :mimeType "image/png"}]
                 "Describe this image.")]
    (is (= "multipart" (agent-turn-prompt/content-type content)))
    (is (= [{:type "image"
             :data "abc123"
             :mimeType "image/png"}
            {:type "text"
             :text "Describe this image."}]
           (xjson/to-cljs content)))))

(deftest prompt-log-payload-redacts-long-media-data
  (let [raw-data (apply str (repeat 80 "A"))
        content (agent-turn-prompt/prompt-content
                 [{:type "image"
                   :data raw-data
                   :mimeType "image/png"}]
                 "Describe this image.")
        payload (xjson/to-cljs
                 (agent-turn-prompt/log-payload
                  {:run-id "run-1"
                   :session-id "session-1"
                   :conversation-id "conversation-1"
                   :model-id "model-1"
                   :mode "direct"
                   :parts-count 2
                   :media-parts-count 1
                   :omitted-count 0
                   :content content}))
        redacted (get-in payload [:content 0 :data])]
    (is (= "multipart" (:content_type payload)))
    (is (str/starts-with? redacted "[img:sha="))
    (is (str/includes? redacted "len=80"))
    (is (not= raw-data redacted))
    (is (= {:type "text" :text "Describe this image."}
           (get-in payload [:content 1])))))
