(ns kms-ingestion.drivers.scraper
  "Audio scraper driver — crawls URLs, discovers audio links, downloads to a local folder.
   
   Config:
   - :url              — base URL to scrape (required)
   - :download-dir     — local folder to store downloaded files (required)
   - :max-depth        — max crawl depth (default 2)
   - :max-files        — max files to download per run (default 100)
   - :follow-redirects — follow HTTP redirects (default true)
   - :user-agent       — custom User-Agent header
   - :extensions       — set of audio extensions to look for (default: standard set)
   - :recursive        — follow links within same domain (default false)
   
   The driver does two things:
   1. DISCOVER — crawls the URL tree, finds audio links, reports what's new
   2. EXTRACT  — downloads a discovered file to :download-dir, returns metadata
   
   After extraction, the standard 'audio' driver can index the local folder."
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [kms-ingestion.drivers.protocol :as protocol])
  (:import
   [java.net URI URL HttpURLConnection]
   [java.nio.file Files]
   [java.security MessageDigest]
   [java.time Instant]))

;; ─── Audio extensions ───────────────────────────────────────────────────────

(def default-audio-extensions
  #{"mp3" "wav" "ogg" "m4a" "flac" "aac" "opus" "wma" "aiff" "alac"})

(defn- audio-extension? [ext]
  (contains? default-audio-extensions (str/lower-case (str/replace ext #"^\." ""))))

(defn- url->filename [^String url]
  (let [path (.getPath (URI. url))
        fname (last (str/split path #"/"))]
    (if (str/blank? fname)
      (str "audio_" (System/currentTimeMillis) ".mp3")
      fname)))

(defn- url->audio? [^String url]
  (let [fname (url->filename url)
        ext (last (str/split fname #"\."))]
    (when (and ext (not (str/blank? ext)))
      (audio-extension? ext))))

(defn- normalize-url [base relative]
  (try
    (let [base-uri (URI. base)
          resolved (.resolve base-uri relative)]
      (.toString resolved))
    (catch Exception _ relative)))

(defn- same-domain? [base-url target-url]
  (try
    (let [base-host (.getHost (URI. base-url))
          target-host (.getHost (URI. target-url))]
      (= base-host target-host))
    (catch Exception _ false)))

;; ─── HTML Parsing (lightweight, no deps) ────────────────────────────────────

(defn- extract-links
  "Naive but effective link extraction from HTML.
   Returns a seq of href values found in <a>, <audio>, <source> tags."
  [^String html base-url]
  (let [;; Match href and src attributes
        href-pattern #"(?i)(?:href|src)\s*=\s*[\"']([^\"']+)[\"']"
        matches (re-seq href-pattern html)]
    (->> matches
         (map second)
         (map #(normalize-url base-url %))
         distinct)))

;; ─── HTTP helpers ───────────────────────────────────────────────────────────

(def default-headers
  {"User-Agent" "kms-ingestion-scraper/1.0"})

(defn- fetch-html [url & [{:keys [user-agent timeout follow-redirects]
                           :or {timeout 30000 follow-redirects true}}]]
  (try
    (let [headers (cond-> default-headers
                    user-agent (assoc "User-Agent" user-agent))
          resp (http/get url
                        {:headers headers
                         :socket-timeout timeout
                         :connection-timeout timeout
                         :redirect-strategy (if follow-redirects :lax :none)
                         :as :string
                         :throw-exceptions false})]
      (when (= 200 (:status resp))
        (:body resp)))
    (catch Exception e
      (println "[scraper] fetch-html failed:" url (.getMessage e))
      nil)))

(defn- download-file
  "Download a URL to a local path. Returns {:ok true :path p} or {:ok false :error e}."
  [^String url ^String dest-path & [{:keys [user-agent timeout]
                                      :or {timeout 120000}}]]
  (try
    (let [headers (cond-> default-headers
                    user-agent (assoc "User-Agent" user-agent))
          resp (http/get url
                        {:headers headers
                         :socket-timeout timeout
                         :connection-timeout timeout
                         :as :byte-array
                         :throw-exceptions false})]
      (if (= 200 (:status resp))
        (do
          (fs/create-dirs (fs/parent dest-path))
          (with-open [out (io/output-stream dest-path)]
            (.write out ^bytes (:body resp)))
          {:ok true :path dest-path :size (count (:body resp))})
        {:ok false :error (str "HTTP " (:status resp))}))
    (catch Exception e
      {:ok false :error (.getMessage e)})))

;; ─── SHA-256 ────────────────────────────────────────────────────────────────

(defn- sha256 [path]
  (try
    (let [bytes (Files/readAllBytes (fs/path path))
          digest (MessageDigest/getInstance "SHA-256")]
      (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest digest bytes))))
    (catch Exception _ nil)))

;; ─── Crawler ────────────────────────────────────────────────────────────────

(defn- crawl-for-audio
  "Crawl a URL and discover audio file links.
   Returns a seq of {:url \"...\" :source-page \"...\"} maps."
  [start-url opts]
  (let [{:keys [max-depth max-files recursive user-agent follow-redirects]
         :or {max-depth 2 max-files 200 recursive false follow-redirects true}} opts
        visited (atom #{})
        results (atom [])
        queue (atom [{:url start-url :depth 0}])]
    (while (and (seq @queue) (< (count @results) max-files))
      (let [{:keys [url depth]} (first @queue)]
        (swap! queue rest)
        (when-not (or (@visited url) (> depth max-depth))
          (swap! visited conj url)
          (cond
            ;; If it's a direct audio link, record it
            (url->audio? url)
            (swap! results conj {:url url :source-page start-url})

            ;; If it's HTML and we haven't exceeded depth, parse for links
            (and (<= depth max-depth) (not (url->audio? url)))
            (when-let [html (fetch-html url {:user-agent user-agent
                                             :follow-redirects follow-redirects})]
              (let [links (extract-links html url)]
                (doseq [link links]
                  (when-not (@visited link)
                    (cond
                      ;; Direct audio link
                      (url->audio? link)
                      (swap! results conj {:url link :source-page url})

                      ;; Same-domain link for further crawling
                      (and recursive
                           (same-domain? start-url link)
                           (<= (inc depth) max-depth)
                           (not (url->audio? link)))
                      (swap! queue conj {:url link :depth (inc depth)}))))))))))
    @results))

;; ─── Scraper Driver ─────────────────────────────────────────────────────────

(deftype ScraperDriver [config state]
  protocol/Driver

  (discover [this opts]
    (let [url (:url config)
          download-dir (or (:download-dir config) (:download_dir config))
          existing-state (or (:existing-state opts) @state {})
          downloaded-map (or (:downloaded existing-state) {})]

      (if (str/blank? url)
        {:total-files 0 :new-files 0 :changed-files 0
         :unchanged-files 0 :deleted-files 0 :skipped-files 0
         :files [] :error "Missing :url in config"}

        ;; Crawl and find audio links
        (let [audio-links (crawl-for-audio url config)
              classified
              (map (fn [{:keys [url source-page]}]
                     (let [filename (url->filename url)
                           file-id url
                           existing (get downloaded-map url)
                           status (if existing :unchanged :new)]
                       {:id file-id
                        :path filename
                        :url url
                        :source-page source-page
                        :content-hash nil ;; computed on extract
                        :status status}))
                   audio-links)
              counts (frequencies (map :status classified))]

          {:total-files (count classified)
           :new-files (get counts :new 0)
           :changed-files (get counts :changed 0)
           :unchanged-files (get counts :unchanged 0)
           :deleted-files 0
           :skipped-files 0
           :files (vec classified)}))))

  (extract [this file-id]
    (let [download-dir (or (:download-dir config) (:download_dir config))
          url file-id ;; file-id IS the URL
          filename (url->filename url)
          dest-path (str (fs/path download-dir filename))
          existing-state @state
          downloaded-map (or (:downloaded existing-state) {})]

      (if-let [existing (get downloaded-map url)]
        ;; Already downloaded — return cached info
        {:id url
         :path filename
         :content (str "Previously downloaded audio: " filename
                       "\nURL: " url
                       "\nLocal path: " (:local-path existing)
                       "\nSize: " (:size existing) " bytes")
         :content-hash (:content-hash existing)
         :local-path (:local-path existing)}

        ;; Download the file
        (let [result (download-file url dest-path)]
          (if (:ok result)
            (let [hash (sha256 dest-path)
                  file-size (:size result)
                  ;; Update state
                  new-downloaded (assoc downloaded-map url
                                        {:local-path dest-path
                                         :filename filename
                                         :size file-size
                                         :content-hash hash
                                         :downloaded-at (str (Instant/now))})]
              (reset! state {:downloaded new-downloaded})
              {:id url
               :path filename
               :content (str "Downloaded audio: " filename
                             "\nURL: " url
                             "\nLocal path: " dest-path
                             "\nSize: " file-size " bytes"
                             "\nSHA-256: " hash)
               :content-hash hash
               :local-path dest-path})

            ;; Download failed
            {:id url
             :path filename
             :content nil
             :error (:error result)})))))

  (extract-batch [this file-ids]
    (mapv #(protocol/extract this %) file-ids))

  (get-state [_this]
    @state)

  (set-state [_this new-state]
    (reset! state (or new-state {})))

  (close [_this]
    ;; Nothing to clean up
    ))

(defn create-scraper-driver [config]
  (let [download-dir (or (:download-dir config) (:download_dir config) "./scraped-audio")]
    (fs/create-dirs download-dir)
    (ScraperDriver. (assoc config :download-dir download-dir)
                    (atom {}))))
