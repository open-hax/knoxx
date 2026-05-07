(ns kms-ingestion.drivers.audio
  "Audio file driver that uses gemma4:e4b to describe audio files."
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]
   [kms-ingestion.drivers.protocol :as protocol])
  (:import
   [java.nio.file Files FileVisitor FileVisitResult]
   [java.security MessageDigest]
   [java.util Base64]))

(def audio-extensions
  #{"mp3" "wav" "ogg" "m4a" "flac" "aac" "opus" "wma" "aiff" "aif" "mp4" "webm"})

(defn- normalized-extension [path]
  (-> (or (fs/extension path) "")
      str/lower-case
      (str/replace #"^\." "")))

(defn- audio-file? [path]
  (audio-extensions (normalized-extension path)))

(defn- get-root-path [config]
  (or (:root-path config)
      (:root_path config)
      (throw (ex-info "Missing root_path in driver config" {:config config}))))

(defn- sha256 [path]
  (let [bytes (Files/readAllBytes (.toPath (fs/file path)))
        digest (MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest digest bytes)))))

(defn- human-audio-size [size]
  (cond
    (< size 1024) (str size " bytes")
    (< size (* 1024 1024)) (str (format "%.1f" (/ size 1024.0)) " KB")
    :else (str (format "%.1f" (/ size (* 1024.0 1024.0))) " MB")))

(defn- filename->song-title [path]
  (let [filename (fs/file-name path)
        stem (or (some-> filename (str/replace #"\.[^.]+$" "")) "")]
    (-> stem
        (str/replace #"[_-]+" " ")
        (str/replace #"\s+" " ")
        str/trim)))

(defn- base-audio-description [path]
  (let [filename (fs/file-name path)
        ext (fs/extension path)
        size (fs/size path)]
    (str "Audio file: " filename "\n"
         "Song title: " (filename->song-title path) "\n"
         "Format: " ext "\n"
         "Size: " (human-audio-size size) "\n"
         "Path: " path)))

(defn- audio-format [path]
  (case (normalized-extension path)
    "wav" "wav"
    "mp3" "mp3"
    "m4a" "mp4"
    "mp4" "mp4"
    "webm" "webm"
    "ogg" "ogg"
    "opus" "opus"
    "flac" "flac"
    "aac" "aac"
    "aiff" "aiff"
    "aif" "aiff"
    "mp3"))

(defn- audio-mime-type [path]
  (case (normalized-extension path)
    "wav" "audio/wav"
    "mp3" "audio/mpeg"
    "m4a" "audio/mp4"
    "mp4" "audio/mp4"
    "webm" "audio/webm"
    "ogg" "audio/ogg"
    "opus" "audio/opus"
    "flac" "audio/flac"
    "aac" "audio/aac"
    "aiff" "audio/aiff"
    "aif" "audio/aiff"
    "audio/mpeg"))

(defn- file->base64 [path]
  (.encodeToString (Base64/getEncoder) (Files/readAllBytes (.toPath (fs/file path)))))

(defn- audio-chat-content [path]
  (let [filename (fs/file-name path)
        ext (fs/extension path)
        size (fs/size path)]
    [{:type "text"
      :text (str "Listen to the attached audio bytes as primary evidence. "
                 "Do not infer from the filename alone. Return a compact library note with: "
                 "(1) what is audible, (2) likely content type, (3) reusable labels.\n"
                 "Filename: " filename "\n"
                 "Format: " ext "\n"
                 "MIME: " (audio-mime-type path) "\n"
                 "Size: " size " bytes\n"
                 "Fallback title from filename: " (filename->song-title path))}
     {:type "input_audio"
      :input_audio {:data (file->base64 path)
                    :format (audio-format path)}}]))

(defn- gemma-audio-description
  "Use gemma4:e4b with the actual audio bytes, not filename-only metadata."
  [path chat-url]
  (try
    (let [response (http/post
                    (str chat-url "/v1/chat/completions")
                    {:headers {"Content-Type" "application/json"}
                     :body (json/generate-string
                            {:model "gemma4-e4b"
                             :messages [{:role "system"
                                         :content "You are an audio-library analyzer. Hear the attached audio directly and produce truthful, reusable catalogue metadata. If the audio is unclear, say so."}
                                        {:role "user"
                                         :content (audio-chat-content path)}]
                             :max_tokens 240
                             :temperature 0.2
                             :chat_template_kwargs {:enable_thinking false}})
                     :as :json
                     :socket-timeout 120000
                     :connection-timeout 30000
                     :throw-exceptions false})]
      (when (= 200 (:status response))
        (some-> (get-in response [:body :choices]) first (get-in [:message :content]) str/trim not-empty)))
    (catch Exception e
      (println "[audio-driver] gemma4 describe failed: " (.getMessage e))
      nil)))

(defn audio-context
  "Return a structured audio context map for UI pinning and ingestion content."
  [path chat-url]
  (let [song-title (filename->song-title path)
        base-description (base-audio-description path)
        ai-description (gemma-audio-description path chat-url)
        content (cond-> base-description
                  ai-description (str "\n\nAI Description: " ai-description))]
    {:song-title song-title
     :description (or ai-description base-description)
     :content content}))

(deftype AudioDriver [config state]
  protocol/Driver
  
  (discover [this opts]
    (let [root-path (get-root-path config)
          existing-state (or (:existing-state opts) {})
          all-files (atom [])
          new-count (atom 0)
          changed-count (atom 0)
          unchanged-count (atom 0)
          visitor (reify FileVisitor
                    (visitFile [_ path attrs]
                      (when (audio-file? (str path))
                        (let [rel-path (str/replace (str path) (str root-path "/") "")
                              file-id rel-path
                              content-hash (sha256 path)
                              size (Files/size path)
                              modified-at (Files/getLastModifiedTime path (into-array java.nio.file.LinkOption []))
                              existing (get existing-state file-id)
                              status (cond
                                       (nil? existing) :new
                                       (not= content-hash (:content-hash existing)) :changed
                                       :else :unchanged)]
                          (when (= :new status) (swap! new-count inc))
                          (when (= :changed status) (swap! changed-count inc))
                          (when (= :unchanged status) (swap! unchanged-count inc))
                          (swap! all-files conj
                                  {:id file-id
                                   :path rel-path
                                   :content-hash content-hash
                                   :size size
                                   :modified-at (str modified-at)
                                   :status status})))
                      FileVisitResult/CONTINUE)
                    (visitFileFailed [_ path exc]
                      (println "[audio-driver] visit failed:" path (.getMessage exc))
                      FileVisitResult/CONTINUE)
                    (preVisitDirectory [_ dir attrs]
                      FileVisitResult/CONTINUE)
                    (postVisitDirectory [_ dir exc]
                      FileVisitResult/CONTINUE))]
      (Files/walkFileTree (.toPath (fs/file root-path)) visitor)
      {:total-files (count @all-files)
       :new-files @new-count
       :changed-files @changed-count
       :deleted-files 0
       :unchanged-files @unchanged-count
       :skipped-files 0
       :files @all-files}))
  
  (extract [this file-id]
    (let [root-path (get-root-path config)
          full-path (fs/path root-path file-id)
          chat-url (or (:chat-url config) (:chat_url config) "http://localhost:8082")]
      (if (fs/exists? full-path)
        (let [{:keys [content]} (audio-context full-path chat-url)
              content-hash (sha256 full-path)]
          {:id file-id
           :path file-id
           :content content
           :content-hash content-hash})
        {:id file-id
         :path file-id
         :content nil
         :error (str "File not found: " file-id)})))
  
  (extract-batch [this file-ids]
    (map #(protocol/extract this %) file-ids))
  
  (get-state [this]
    @state)
  
  (set-state [this new-state]
    (reset! state new-state))
  
  (close [this]
    ;; Nothing to clean up
    ))

(defn create-audio-driver [config]
  (AudioDriver. config (atom {})))
