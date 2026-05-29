(ns knoxx.backend.tools.blaze-music-generate-test
  (:require [clojure.string :as str]
            [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.models :as models]
            [knoxx.backend.domain.media.blaze :as blaze]
            [knoxx.backend.domain.media :as media]
            ["node:crypto" :as crypto]
            ["node:fs/promises" :as fs]
            ["node:http" :as http]))

(def ^:private fixture-mp3
  "../Audio/broadcasts/brain_fart_alert.mp3")

(defn- join-chunks
  "Concat Node buffers into a utf8 string."
  [chunks]
  (->> chunks
       (map (fn [^js chunk] (.toString chunk "utf8")))
       (apply str)))

(defn- start-mock-proxx-recording!
  "Start a minimal Proxx-like HTTP server that serves Blaze-compatible JSON for
   POST /v1/music/generations and records request bodies.

   Returns a promise of {:server <Server> :base-url <string> :requests* <atom>}"
  [response-payload]
  (js/Promise.
   (fn [resolve _reject]
     (let [requests* (atom [])
           handler (fn [req res]
                     (let [method (some-> (.-method req) str)
                           url (some-> (.-url req) str)]
                       (if (and (= method "POST") (= url "/v1/music/generations"))
                         (let [chunks (atom [])]
                           (.on req "data" (fn [chunk] (swap! chunks conj chunk)))
                           (.on req
                                "end"
                                (fn []
                                  (let [raw (join-chunks @chunks)
                                        body (js->clj (js/JSON.parse raw) :keywordize-keys true)]
                                    (swap! requests* conj body)
                                    (.end res (js/JSON.stringify (clj->js response-payload)))))))
                         (.end res (js/JSON.stringify (clj->js {:error "not found"}))))))
           server (.createServer http handler)]
       (.listen server 0 "127.0.0.1"
                (fn []
                  (let [port (.-port (.address server))]
                    (resolve {:server server
                              :base-url (str "http://127.0.0.1:" port)
                              :requests* requests*}))))))))

(defn- start-mock-proxx!
  "Start a mock Proxx server that returns hex audio."
  [hex-audio]
  (start-mock-proxx-recording!
   {:id "mock-music-1"
    :object "music.generation"
    :status "success"
    :model "MiniMax-music-2.6-highspeed"
    :choices [{:message {:content "ok"}}]
    :data {:format "mp3"
           :task_id "mock-task"
           :audio hex-audio}}))

(defn- stop-server!
  [server]
  (js/Promise.
   (fn [resolve _reject]
     (.close server (fn [] (resolve nil))))))

(defn- buffer-prefix
  "Return a Buffer containing the first n bytes."
  [^js buf n]
  (.from js/Buffer (.subarray buf 0 (min n (.-length buf)))))

(deftest ^:async music-generate-song-saves-hex-audio-payload
  (testing "music.generate_song writes a valid mp3 buffer when Blaze returns hex audio"
    (let [workspace-root (str "/tmp/knoxx-blaze-mock-" (.randomUUID crypto))
          output-path (str "Music/blaze/mock-" (.randomUUID crypto) ".mp3")
          server* (atom nil)]
      (try
        (let [fixture-bytes (await (.readFile fs fixture-mp3))
              hex (.toString fixture-bytes "hex")
              {:keys [server base-url]} (await (start-mock-proxx! hex))
              _ (reset! server* server)
              runtime #js {}
              config* (assoc (models/enrich-config
                              {:contracts-dir "test/fixtures/empty-contracts"
                               :workspace-root workspace-root
                               :proxx-base-url base-url
                               :proxx-auth-token "test-token"})
                             :proxx-base-url base-url)]
          (await (.mkdir fs workspace-root #js {:recursive true}))
          (await (blaze/blaze-music-generate-execute
                  runtime config* "tool-call-mock"
                  #js {"prompt" "test" "output_path" output-path}
                  nil nil nil))
          (let [{:keys [absolute]} (media/resolve-workspace-media-path runtime config* output-path)
                saved-bytes (await (.readFile fs absolute))
                want (buffer-prefix fixture-bytes 16)
                got (buffer-prefix saved-bytes 16)]
            (is (= (.-length fixture-bytes) (.-length saved-bytes)) "saved size matches fixture")
            (is (= (.toString want "hex") (.toString got "hex"))
                "saved prefix matches fixture (hex decoded correctly)")))
        (catch :default e
          (is false (str e)))
        (finally
          (when-let [server @server*]
            (await (stop-server! server))))))))

(deftest ^:async music-generate-song-saves-asset-even-when-status-terminated
  (testing "status=terminated is not treated as fatal when the payload still contains an audio asset"
    (let [workspace-root (str "/tmp/knoxx-blaze-mock-" (.randomUUID crypto))
          output-path (str "Music/blaze/mock-" (.randomUUID crypto) ".mp3")
          server* (atom nil)]
      (try
        (let [fixture-bytes (await (.readFile fs fixture-mp3))
              hex (.toString fixture-bytes "hex")
              {:keys [server base-url]} (await (start-mock-proxx-recording!
                                                {:id "mock-music-terminated"
                                                 :object "music.generation"
                                                 :status "terminated"
                                                 :model "MiniMax-music-2.6-highspeed"
                                                 :choices [{:message {:content "terminated but with audio"}}]
                                                 :data {:format "mp3"
                                                        :task_id "mock-task"
                                                        :audio hex}}))
              _ (reset! server* server)
              runtime #js {}
              config* (assoc (models/enrich-config
                              {:contracts-dir "test/fixtures/empty-contracts"
                               :workspace-root workspace-root
                               :proxx-base-url base-url
                               :proxx-auth-token "test-token"})
                             :proxx-base-url base-url)]
          (await (.mkdir fs workspace-root #js {:recursive true}))
          (await (blaze/blaze-music-generate-execute
                  runtime config* "tool-call-mock"
                  #js {"prompt" "test" "output_path" output-path}
                  nil nil nil))
          (let [{:keys [absolute]} (media/resolve-workspace-media-path runtime config* output-path)
                saved-bytes (await (.readFile fs absolute))
                want (buffer-prefix fixture-bytes 16)
                got (buffer-prefix saved-bytes 16)]
            (is (= (.-length fixture-bytes) (.-length saved-bytes)) "saved size matches fixture")
            (is (= (.toString want "hex") (.toString got "hex"))
                "saved prefix matches fixture")))
        (catch :default e
          (is false (str e)))
        (finally
          (when-let [server @server*]
            (await (stop-server! server))))))))

(deftest ^:async music-generate-song-sends-lyrics-and-normalizes-section-tags
  (testing "music.generate_song sends Blaze-compatible lyrics payload"
    (let [workspace-root (str "/tmp/knoxx-blaze-mock-" (.randomUUID crypto))
          output-path (str "Music/blaze/mock-" (.randomUUID crypto) ".mp3")
          server* (atom nil)
          prompt "A minor, 86 BPM. Industrial glitch hymn with restrained vocal delivery."
          lyrics "(Verse 1)\nHello world\n"
          response {:id "mock-music-req"
                    :object "music.generation"
                    :status "success"
                    :model "MiniMax-music-2.6-highspeed"
                    :choices [{:message {:content "ok"}}]
                    :data {}}]
      (try
        (let [{:keys [server base-url requests*]} (await (start-mock-proxx-recording! response))
              _ (reset! server* server)
              runtime #js {}
              config* (assoc (models/enrich-config
                              {:contracts-dir "test/fixtures/empty-contracts"
                               :workspace-root workspace-root
                               :proxx-base-url base-url
                               :proxx-auth-token "test-token"})
                             :proxx-base-url base-url)]
          (await (.mkdir fs workspace-root #js {:recursive true}))
          (await (blaze/blaze-music-generate-execute
                  runtime config* "tool-call-mock"
                  #js {"prompt" prompt
                       "lyrics" lyrics
                       "model" "MiniMax-music-2.6-highspeed"
                       "output_path" output-path}
                  nil nil nil))
          (let [req (first @requests*)]
            (is (= "MiniMax-music-2.6-highspeed" (:model req)))
            (is (= "mp3" (:audio_format req)) "output_path .mp3 infers audio_format mp3")
            (is (= 44100 (:sample_rate req)) "default sample_rate")
            (is (= 256000 (:bitrate req)) "default bitrate")
            (is (= false (:lyrics_optimizer req)) "default lyrics_optimizer false")
            (is (= false (:is_instrumental req)) "lyrics imply non-instrumental by default")
            (is (= prompt (:prompt req)) "prompt already implies vocals; no extra clause appended")
            (is (= "[Verse 1]\nHello world" (:lyrics req)) "(Verse 1) is normalized to [Verse 1]")))
        (catch :default e
          (is false (str e)))
        (finally
          (when-let [server @server*]
            (await (stop-server! server))))))))

(deftest ^:async music-generate-song-appends-vocal-arrangement-when-prompt-not-vocal
  (testing "lyrics present + prompt not vocal => blaze tool appends explicit vocal arrangement clause"
    (let [workspace-root (str "/tmp/knoxx-blaze-mock-" (.randomUUID crypto))
          output-path (str "Music/blaze/mock-" (.randomUUID crypto) ".mp3")
          server* (atom nil)
          prompt "A minor, 86 BPM. Industrial glitch hymn."
          lyrics "(Intro)\nHello\n"
          response {:id "mock-music-req"
                    :object "music.generation"
                    :status "success"
                    :model "MiniMax-music-2.6-highspeed"
                    :choices [{:message {:content "ok"}}]
                    :data {}}]
      (try
        (let [{:keys [server base-url requests*]} (await (start-mock-proxx-recording! response))
              _ (reset! server* server)
              runtime #js {}
              config* (assoc (models/enrich-config
                              {:contracts-dir "test/fixtures/empty-contracts"
                               :workspace-root workspace-root
                               :proxx-base-url base-url
                               :proxx-auth-token "test-token"})
                             :proxx-base-url base-url)]
          (await (.mkdir fs workspace-root #js {:recursive true}))
          (await (blaze/blaze-music-generate-execute
                  runtime config* "tool-call-mock"
                  #js {"prompt" prompt
                       "lyrics" lyrics
                       "model" "MiniMax-music-2.6-highspeed"
                       "output_path" output-path}
                  nil nil nil))
          (let [req (first @requests*)]
            (is (string? (:prompt req)))
            (is (re-find #"Vocal song arrangement" (:prompt req))
                "missing vocal cues triggers appended arrangement clause")
            (is (str/starts-with? (:prompt req) prompt)
                "appended arrangement keeps original prompt prefix")
            (is (= "[Intro]\nHello" (:lyrics req)) "(Intro) line normalized")))
        (catch :default e
          (is false (str e)))
        (finally
          (when-let [server @server*]
            (await (stop-server! server))))))))

(deftest ^:async music-generate-song-infers-wav-from-output-path
  (testing "output_path .wav sets audio_format wav"
    (let [workspace-root (str "/tmp/knoxx-blaze-mock-" (.randomUUID crypto))
          output-path (str "Music/blaze/mock-" (.randomUUID crypto) ".wav")
          server* (atom nil)
          response {:id "mock-music-req"
                    :object "music.generation"
                    :status "success"
                    :model "MiniMax-music-2.6-highspeed"
                    :choices [{:message {:content "ok"}}]
                    :data {}}]
      (try
        (let [{:keys [server base-url requests*]} (await (start-mock-proxx-recording! response))
              _ (reset! server* server)
              runtime #js {}
              config* (assoc (models/enrich-config
                              {:contracts-dir "test/fixtures/empty-contracts"
                               :workspace-root workspace-root
                               :proxx-base-url base-url
                               :proxx-auth-token "test-token"})
                             :proxx-base-url base-url)]
          (await (.mkdir fs workspace-root #js {:recursive true}))
          (await (blaze/blaze-music-generate-execute
                  runtime config* "tool-call-mock"
                  #js {"prompt" "Simple ambient test."
                       "output_path" output-path}
                  nil nil nil))
          (let [req (first @requests*)]
            (is (= "wav" (:audio_format req)))
            (is (= true (:is_instrumental req)) "no lyrics defaults to instrumental")
            (is (= false (:lyrics_optimizer req)) "default lyrics_optimizer false")
            (is (nil? (:lyrics req)) "no lyrics field emitted")))
        (catch :default e
          (is false (str e)))
        (finally
          (when-let [server @server*]
            (await (stop-server! server))))))))
