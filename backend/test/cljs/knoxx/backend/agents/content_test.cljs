(ns knoxx.backend.agents.content-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.agent.content :as content
             :refer [diff-appended-text]]))

(deftest diff-appended-text-test
  (testing "returns only novel suffix for cumulative and overlapping streams"
    (is (= "hello" (diff-appended-text "" "hello")))
    (is (= "" (diff-appended-text "hello" "hello")))
    (is (= " world" (diff-appended-text "hello" "hello world")))
    (is (= " again" (diff-appended-text "hello world" "world again"))))
  (testing "collapses Gemma/Ollama hosted duplicated first reasoning token"
    (is (= " model should reason once."
           (diff-appended-text "The" "TheThe model should reason once.")))
    (is (= " need one token."
           (diff-appended-text "We" "WeWe need one token.")))
    (is (= " can I help?"
           (diff-appended-text "Ready. How" "Ready. HowReady. How can I help?")))
    (is (= ""
           (diff-appended-text "Ready." "Ready.Ready")))
    (is (= " How can I help?"
           (diff-appended-text "Ready." "Ready.Ready. How can I help?"))))
  (testing "does not collapse intentional adjacent token continuation without a boundary"
    (is (= "ha" (diff-appended-text "ha" "haha")))))

(deftest assistant-content-parts-normalizes-provider-media-shapes
  (testing "base64 image source metadata becomes a model-ready content part"
    (let [message #js {:content #js [#js {:type "input_image"
                                          :source #js {:type "base64"
                                                       :media_type "image/jpeg"
                                                       :data "abc123"}
                                          :filename "sketch.jpg"
                                          :size 42}
                                      #js {:type "text" :text "ignore me"}]}]
      (is (= [{:type "image"
               :data "data:image/jpeg;base64,abc123"
               :mimeType "image/jpeg"
               :filename "sketch.jpg"
               :size 42}]
             (content/assistant-content-parts message)))))
  (testing "url sources use fallback media-kind mime defaults"
    (let [message #js {:content #js [#js {:type "output_audio"
                                          :source #js {:type "url"
                                                       :url "https://cdn.example/audio.wav"}}]}]
      (is (= [{:type "audio"
               :url "https://cdn.example/audio.wav"
               :mimeType "audio/wav"}]
             (content/assistant-content-parts message))))))

(deftest tool-result-content-parts-normalizes-details-and-attachments
  (let [tool-result #js {:details #js {:attachments #js [#js {:type "audio_url"
                                                              :url "https://cdn.example/clip.mp3"
                                                              :media_type "audio/mpeg"
                                                              :name "clip.mp3"
                                                              :byteSize 2048}
                                                          #js {:type "text"
                                                               :text "not media"}]}}]
    (is (= [{:type "audio"
             :url "https://cdn.example/clip.mp3"
             :mimeType "audio/mpeg"
             :filename "clip.mp3"
             :size 2048}]
           (content/tool-result-content-parts tool-result)))))

(deftest content-part-collection-helpers-dedupe-and-filter
  (testing "merge-content-parts preserves order while deduping equal maps"
    (let [image {:type "image" :url "https://cdn.example/a.png"}
          audio {:type "audio" :url "https://cdn.example/a.wav"}]
      (is (= [image audio]
             (content/merge-content-parts [image] [image audio] nil)))))
  (testing "reply attachments only expose workspace_media.attach content parts"
    (is (= [{:type "document" :url "file:///workspace/doc.md"}]
           (content/reply-attachment-content-parts
            [{:tool_name "memory.search"
              :content_parts [{:type "text" :text "ignore"}]}
             {:tool_name "workspace_media.attach"
              :content_parts [{:type "document" :url "file:///workspace/doc.md"}]}])))))

(deftest model-ready-content-parts-degrades-unsupported-media-to-text
  (let [config {:contracts-dir "test/fixtures/empty-contracts"}]
    (is (= [{:type :text
             :text "Uploaded audio source 'voice.wav' is available, but model text-only-model does not declare audio input. Use audio.spectrogram if you need an image-friendly audio view."}
            {:type :text
             :text "Uploaded document 'notes.pdf' is available, but model text-only-model does not declare document input."}]
           (content/model-ready-content-parts config
                                              "text-only-model"
                                              [{:type "audio" :filename "voice.wav"}
                                               {:type "document" :filename "notes.pdf"}])))))
