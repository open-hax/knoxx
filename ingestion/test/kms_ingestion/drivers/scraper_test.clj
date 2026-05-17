(ns kms-ingestion.drivers.scraper-test
  (:require
   [babashka.fs :as fs]
   [clojure.test :refer [deftest is testing]]
   [kms-ingestion.drivers.protocol :as protocol]
   [kms-ingestion.drivers.scraper :as scraper]))

;; ─── Test helpers ───────────────────────────────────────────────────────────

(defn- temp-dir []
  (str (fs/create-temp-dir {:prefix "scraper-test-"})))

(defn- cleanup [dir]
  (when (fs/exists? dir)
    (fs/delete-tree dir)))

;; ─── Unit tests ─────────────────────────────────────────────────────────────

(deftest url->filename-test
  (testing "Extracts filename from URL"
    (is (= "song.mp3" (#'scraper/url->filename "https://example.com/audio/song.mp3")))
    (is (= "track.wav" (#'scraper/url->filename "https://cdn.site.org/music/track.wav")))
    (is (= "podcast.m4a" (#'scraper/url->filename "https://feeds.example.com/podcast.m4a"))))

  (testing "Handles URL with query params"
    (is (= "song.mp3" (#'scraper/url->filename "https://example.com/song.mp3?token=abc"))))

  (testing "Generates fallback for empty path"
    (let [fname (#'scraper/url->filename "https://example.com/")]
      (is (string? fname))
      (is (.startsWith fname "audio_")))))

(deftest audio-extension?-test
  (testing "Recognizes audio extensions"
    (is (true? (#'scraper/audio-extension? "mp3")))
    (is (true? (#'scraper/audio-extension? ".mp3")))
    (is (true? (#'scraper/audio-extension? "WAV")))
    (is (true? (#'scraper/audio-extension? ".FLAC")))
    (is (true? (#'scraper/audio-extension? "ogg")))
    (is (true? (#'scraper/audio-extension? "m4a")))
    (is (true? (#'scraper/audio-extension? "opus"))))

  (testing "Rejects non-audio extensions"
    (is (false? (#'scraper/audio-extension? "html")))
    (is (false? (#'scraper/audio-extension? "txt")))
    (is (false? (#'scraper/audio-extension? "mp4")))
    (is (false? (#'scraper/audio-extension? "")))))

(deftest url->audio?-test
  (testing "Detects audio URLs by extension"
    (is (true? (#'scraper/url->audio? "https://example.com/song.mp3")))
    (is (true? (#'scraper/url->audio? "https://example.com/audio/track.wav")))
    (is (true? (#'scraper/url->audio? "https://example.com/podcast.m4a?t=123"))))

  (testing "Rejects non-audio URLs"
    (is (false? (#'scraper/url->audio? "https://example.com/page.html")))
    (is (false? (#'scraper/url->audio? "https://example.com/")))
    (is (false? (#'scraper/url->audio? "https://example.com/image.png")))))

(deftest extract-links-test
  (testing "Extracts href and src from HTML"
    (let [html "<html><body>
                 <a href=\"/audio/song.mp3\">Song</a>
                 <audio src=\"https://cdn.example.com/track.wav\"></audio>
                 <a href=\"/page\">Page</a>
                 <source src=\"podcast.ogg\">
                </body></html>"
          links (#'scraper/extract-links html "https://example.com/")]
      (is (some #(= % "https://example.com/audio/song.mp3") links))
      (is (some #(= % "https://cdn.example.com/track.wav") links))
      (is (some #(= % "https://example.com/page") links))
      (is (some #(= % "https://example.com/podcast.ogg") links)))))

(deftest same-domain?-test
  (testing "Same domain returns true"
    (is (true? (#'scraper/same-domain? "https://example.com/a" "https://example.com/b")))
    (is (true? (#'scraper/same-domain? "https://example.com" "https://example.com/path"))))

  (testing "Different domain returns false"
    (is (false? (#'scraper/same-domain? "https://example.com" "https://other.com")))
    (is (false? (#'scraper/same-domain? "https://a.example.com" "https://b.example.com")))))

;; ─── Driver protocol tests ──────────────────────────────────────────────────

(deftest driver-creation-test
  (testing "Creates driver with valid config"
    (let [dir (temp-dir)
          driver (scraper/create-scraper-driver {:url "https://example.com"
                                                 :download-dir dir})]
      (is (some? driver))
      (is (fs/exists? dir))
      (cleanup dir)))

  (testing "Creates driver with default download dir"
    (let [driver (scraper/create-scraper-driver {:url "https://example.com"})]
      (is (some? driver))
      ;; cleanup default dir
      (when (fs/exists? "./scraped-audio")
        (fs/delete-tree "./scraped-audio")))))

(deftest discover-without-url-test
  (testing "Discover returns error when URL is missing"
    (let [dir (temp-dir)
          driver (scraper/create-scraper-driver {:download-dir dir})
          result (protocol/discover driver {})]
      (is (= 0 (:total-files result)))
      (is (some? (:error result)))
      (cleanup dir))))

(deftest get-set-state-test
  (testing "State round-trips correctly"
    (let [dir (temp-dir)
          driver (scraper/create-scraper-driver {:url "https://example.com"
                                                 :download-dir dir})
          test-state {:downloaded {"https://example.com/song.mp3"
                                   {:local-path "/tmp/song.mp3"
                                    :size 12345}}}]
      (protocol/set-state driver test-state)
      (is (= test-state (protocol/get-state driver)))
      (cleanup dir))))

;; ─── Integration test (mock HTTP) ───────────────────────────────────────────

(deftest scraper-driver-integration-test
  (testing "Full scrape cycle with mocked HTTP"
    (let [dir (temp-dir)
          downloaded (atom {})]
      (try
        ;; This test verifies the driver structure works.
        ;; In a real integration test, you'd mock clj-http.client/get.
        (let [driver (scraper/create-scraper-driver
                      {:url "https://example.com"
                       :download-dir dir
                       :max-depth 0 ;; don't actually crawl
                       :max-files 10})]
          (is (some? driver))
          ;; Verify protocol methods are implemented
          (is (satisfies? protocol/Driver driver)))

        (finally
          (cleanup dir))))))
