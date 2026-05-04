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
   [java.security MessageDigest]))

(def audio-extensions
  #{".mp3" ".wav" ".ogg" ".m4a" ".flac" ".aac" ".opus" ".wma"})

(defn- audio-file? [path]
  (let [ext (str/lower-case (or (fs/extension path) ""))]
    (audio-extensions ext)))

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

(defn- gemma-audio-description
  "Use gemma4:e4b to describe an audio file."
  [path chat-url]
  (let [filename (fs/file-name path)
        ext (fs/extension path)
        size (fs/size path)]
    (try
      (let [response (http/post
                      (str chat-url "/v1/chat/completions")
                      {:headers {"Content-Type" "application/json"}
                       :body (json/generate-string
                              {:model "gemma4-e4b"
                               :messages [{:role "system"
                                           :content "You are an audio file analyzer. Given a filename and metadata, generate a brief description of what this audio file might contain. Focus on likely content based on the filename. Keep it to 2-3 sentences."}
                                          {:role "user"
                                           :content (str "Analyze this audio file:\nFilename: " filename "\nFormat: " ext "\nSize: " size " bytes\nSuggested song title: " (filename->song-title path))}]
                               :max_tokens 200
                               :temperature 0.7})
                       :as :json
                       :socket-timeout 30000
                       :connection-timeout 30000})]
        (when (= 200 (:status response))
          (some-> (get-in response [:body :choices]) first (get-in [:message :content]) str/trim not-empty)))
      (catch Exception e
        (println "[audio-driver] gemma4 describe failed: " (.getMessage e))
        nil))))

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
