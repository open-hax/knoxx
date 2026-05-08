(ns kms-ingestion.drivers.audio-test
  (:require
   [babashka.fs :as fs]
   [cheshire.core]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [kms-ingestion.drivers.audio :as audio]))

(defn- temp-audio-file [suffix bytes]
  (let [path (str (fs/create-temp-file {:prefix "audio-driver-test-" :suffix suffix}))]
    (spit path bytes)
    path))

(deftest audio-mime-test
  (testing "Recognizes common audio MIME types for Knoxx content parts"
    (is (= "audio/wav" (#'audio/audio-mime-type "/tmp/sample.wav")))
    (is (= "audio/mpeg" (#'audio/audio-mime-type "/tmp/sample.mp3")))
    (is (= "audio/mp4" (#'audio/audio-mime-type "/tmp/sample.m4a")))))

(deftest audio-agent-content-part-attaches-bytes-test
  (testing "Audio agent prompt includes an audio content part with file bytes"
    (let [path (temp-audio-file ".wav" "RIFFfake-wav")]
      (try
        (let [text (#'audio/audio-agent-message path)
              audio-part (#'audio/audio-agent-content-part path)]
          (is (re-find #"primary evidence" text))
          (is (= "audio" (:type audio-part)))
          (is (= "audio/wav" (:mimeType audio-part)))
          (is (str/starts-with? (:data audio-part) "data:audio/wav;base64,")))
        (finally
          (fs/delete-if-exists path))))))

(deftest knoxx-agent-audio-description-starts-visible-run-test
  (testing "Audio analysis goes through Knoxx direct/start with audio content parts"
    (let [path (temp-audio-file ".wav" "RIFFfake-wav")
          posted (atom nil)
          fetched (atom [])]
      (try
        (with-redefs [clj-http.client/post (fn [url opts]
                                             (reset! posted {:url url :opts opts})
                                             {:status 202
                                              :body {:ok true
                                                     :queued true
                                                     :run_id "run-1"}})
                      clj-http.client/get (fn [url opts]
                                            (swap! fetched conj {:url url :opts opts})
                                            {:status 200
                                             :body {:ok true
                                                    :run {:status "completed"
                                                          :answer "audible drums and synths"}}})]
          (let [answer (#'audio/knoxx-agent-audio-description
                        path
                        {:knoxx-backend-url "http://knoxx.local"
                         :knoxx-user-email "system-admin@open-hax.local"
                         :audio-analysis-model "gemma4:e4b"
                         :audio-analysis-agent-contract-id "broadcast_studio_audio_describer"
                         :audio-analysis-agent-actor-id "broadcast_studio_audio_task"
                         :audio-analysis-agent-timeout-ms 1000
                         :audio-analysis-agent-poll-ms 250})
                body (cheshire.core/parse-string (get-in @posted [:opts :body]) keyword)
                audio-part (first (:content_parts body))]
            (is (= "audible drums and synths" answer))
            (is (= "http://knoxx.local/api/knoxx/direct/start" (:url @posted)))
            (is (re-find #"/api/knoxx/runs/" (:url (first @fetched))))
            (is (= "broadcast_studio_audio_describer" (get-in body [:agent_spec :contract_id])))
            (is (= "broadcast_studio_audio_task" (get-in body [:agent_spec :actor_id])))
            (is (= "audio" (:type audio-part)))
            (is (= "audio/wav" (:mimeType audio-part)))
            (is (str/starts-with? (:data audio-part) "data:audio/wav;base64,"))))
        (finally
          (fs/delete-if-exists path))))))

(deftest audio-context-propagates-agent-failure-test
  (testing "Audio analysis has no alternate hidden model path when the Knoxx agent run is rejected"
    (let [path (temp-audio-file ".wav" "RIFFfake-wav")]
      (try
        (with-redefs [clj-http.client/post (fn [_url _opts]
                                             {:status 500
                                              :body {:error "agent rejected"}})]
          (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                #"Knoxx HTTP 500"
                                (audio/audio-context path {:knoxx-backend-url "http://knoxx.local"}))))
        (finally
          (fs/delete-if-exists path))))))
