(ns kms-ingestion.drivers.audio-test
  (:require
   [babashka.fs :as fs]
   [clojure.test :refer [deftest is testing]]
   [kms-ingestion.drivers.audio :as audio]))

(defn- temp-audio-file [suffix bytes]
  (let [path (str (fs/create-temp-file {:prefix "audio-driver-test-" :suffix suffix}))]
    (spit path bytes)
    path))

(deftest audio-format-and-mime-test
  (testing "Recognizes common audio formats for multimodal chat payloads"
    (is (= "wav" (#'audio/audio-format "/tmp/sample.wav")))
    (is (= "mp3" (#'audio/audio-format "/tmp/sample.mp3")))
    (is (= "mp4" (#'audio/audio-format "/tmp/sample.m4a")))
    (is (= "audio/wav" (#'audio/audio-mime-type "/tmp/sample.wav")))
    (is (= "audio/mpeg" (#'audio/audio-mime-type "/tmp/sample.mp3")))
    (is (= "audio/mp4" (#'audio/audio-mime-type "/tmp/sample.m4a")))))

(deftest audio-chat-content-attaches-bytes-test
  (testing "Audio model prompt includes an input_audio part with file bytes"
    (let [path (temp-audio-file ".wav" "RIFFfake-wav")]
      (try
        (let [content (#'audio/audio-chat-content path)
              text-part (first content)
              audio-part (second content)]
          (is (= "text" (:type text-part)))
          (is (re-find #"primary evidence" (:text text-part)))
          (is (= "input_audio" (:type audio-part)))
          (is (= "wav" (get-in audio-part [:input_audio :format])))
          (is (string? (get-in audio-part [:input_audio :data])))
          (is (not (empty? (get-in audio-part [:input_audio :data])))))
        (finally
          (fs/delete-if-exists path))))))
