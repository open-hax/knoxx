(ns kms-ingestion.drivers.image
  "Image file driver that uses gemma4:e4b to label and describe images."
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]
   [kms-ingestion.drivers.protocol :as protocol])
  (:import
   [java.nio.file Files FileVisitor FileVisitResult]
   [java.security MessageDigest]))

(def image-extensions
  #{".png" ".jpg" ".jpeg" ".gif" ".webp" ".bmp" ".svg" ".tif" ".tiff"})

(defn- image-file? [path]
  (let [ext (str/lower-case (or (fs/extension path) ""))]
    (image-extensions ext)))

(defn- get-root-path [config]
  (or (:root-path config)
      (:root_path config)
      (throw (ex-info "Missing root_path in driver config" {:config config}))))

(defn- sha256 [path]
  (let [bytes (Files/readAllBytes (.toPath (fs/file path)))
        digest (MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest digest bytes)))))

(defn- human-image-size [size]
  (cond
    (< size 1024) (str size " bytes")
    (< size (* 1024 1024)) (str (format "%.1f" (/ size 1024.0)) " KB")
    :else (str (format "%.1f" (/ size (* 1024.0 1024.0))) " MB")))

(defn- filename->image-title [path]
  (let [filename (fs/file-name path)
        stem (or (some-> filename (str/replace #"\.[^.]+$" "")) "")]
    (-> stem
        (str/replace #"[_-]+" " ")
        (str/replace #"\s+" " ")
        str/trim)))

(defn- image-mime-type [path]
  (case (str/lower-case (or (fs/extension path) ""))
    ".png" "image/png"
    ".jpg" "image/jpeg"
    ".jpeg" "image/jpeg"
    ".gif" "image/gif"
    ".webp" "image/webp"
    ".bmp" "image/bmp"
    ".svg" "image/svg+xml"
    ".tif" "image/tiff"
    ".tiff" "image/tiff"
    "image/png"))

(defn- image-data-url [path]
  (let [bytes (Files/readAllBytes (.toPath (fs/file path)))
        mime-type (image-mime-type path)
        b64 (.encodeToString (java.util.Base64/getEncoder) bytes)]
    (str "data:" mime-type ";base64," b64)))

(defn- base-image-description [path]
  (let [filename (fs/file-name path)
        ext (fs/extension path)
        size (fs/size path)]
    (str "Image file: " filename "\n"
         "Image title: " (filename->image-title path) "\n"
         "Format: " ext "\n"
         "Size: " (human-image-size size) "\n"
         "Path: " path)))

(defn- parse-json-object [text]
  (try
    (some-> text str/trim not-empty (json/parse-string true))
    (catch Exception _ nil)))

(defn- normalize-labels [value]
  (->> (cond
         (vector? value) value
         (sequential? value) value
         (string? value) (str/split value #",")
         :else [])
       (map #(some-> % str str/trim))
       (remove str/blank?)
       vec))

(defn- gemma-image-analysis
  "Use gemma4:e4b to label and describe an image file."
  [path chat-url]
  (let [filename (fs/file-name path)
        ext (fs/extension path)
        size (fs/size path)
        title (filename->image-title path)
        data-url (image-data-url path)]
    (try
      (let [response (http/post
                      (str chat-url "/v1/chat/completions")
                      {:headers {"Content-Type" "application/json"}
                       :body (json/generate-string
                              {:model "gemma4-e4b"
                               :messages [{:role "system"
                                           :content "You label and describe images. Return strict JSON with keys labels and description. labels must be a short array of lowercase tags. description must be 1-2 concise sentences."}
                                          {:role "user"
                                           :content [{:type "text"
                                                      :text (str "Analyze this image. Filename: " filename " Format: " ext " Size: " size " bytes Suggested title: " title)}
                                                     {:type "image_url"
                                                      :image_url {:url data-url :detail "low"}}]}]
                               :max_tokens 220
                               :temperature 0.3})
                       :as :json
                       :socket-timeout 60000
                       :connection-timeout 30000})]
        (when (= 200 (:status response))
          (let [text (some-> (get-in response [:body :choices]) first (get-in [:message :content]) str/trim not-empty)
                parsed (parse-json-object text)]
            {:labels (normalize-labels (:labels parsed))
             :description (or (some-> (:description parsed) str str/trim not-empty)
                              text)})))
      (catch Exception e
        (println "[image-driver] gemma4 image analysis failed:" (.getMessage e))
        nil))))

(defn image-context
  "Return a structured image context map for ingestion/search."
  [path chat-url]
  (let [image-title (filename->image-title path)
        base-description (base-image-description path)
        analysis (gemma-image-analysis path chat-url)
        labels (vec (or (:labels analysis) []))
        description (or (:description analysis) base-description)
        labels-line (when (seq labels)
                      (str "\nLabels: " (str/join ", " labels)))
        content (str base-description
                     (or labels-line "")
                     "\n\nAI Description: " description)]
    {:image-title image-title
     :labels labels
     :description description
     :content content}))

(deftype ImageDriver [config state]
  protocol/Driver

  (discover [_ opts]
    (let [root-path (get-root-path config)
          existing-state (or (:existing-state opts) {})
          all-files (atom [])
          new-count (atom 0)
          changed-count (atom 0)
          unchanged-count (atom 0)
          visitor (reify FileVisitor
                    (visitFile [_ path _attrs]
                      (when (image-file? (str path))
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
                      (println "[image-driver] visit failed:" path (.getMessage exc))
                      FileVisitResult/CONTINUE)
                    (preVisitDirectory [_ _dir _attrs]
                      FileVisitResult/CONTINUE)
                    (postVisitDirectory [_ _dir _exc]
                      FileVisitResult/CONTINUE))]
      (Files/walkFileTree (.toPath (fs/file root-path)) visitor)
      {:total-files (count @all-files)
       :new-files @new-count
       :changed-files @changed-count
       :deleted-files 0
       :unchanged-files @unchanged-count
       :skipped-files 0
       :files @all-files}))

  (extract [_ file-id]
    (let [root-path (get-root-path config)
          full-path (fs/path root-path file-id)
          chat-url (or (:chat-url config) (:chat_url config) "http://localhost:8082")]
      (if (fs/exists? full-path)
        (let [{:keys [content]} (image-context full-path chat-url)
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

  (get-state [_]
    @state)

  (set-state [_ new-state]
    (reset! state new-state))

  (close [_]))

(defn create-image-driver [config]
  (ImageDriver. config (atom {})))
