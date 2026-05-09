(ns kms-ingestion.drivers.audio
  "Audio file driver that uses the Knoxx agent runtime to describe audio files."
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]
   [kms-ingestion.config :as app-config]
   [kms-ingestion.drivers.protocol :as protocol])
  (:import
   [java.nio.file Files FileVisitor FileVisitResult]
   [java.security MessageDigest]
   [java.util Base64 UUID]
   [java.util.concurrent Semaphore]))

(def audio-extensions
  #{"mp3" "wav" "ogg" "m4a" "flac" "aac" "opus" "wma" "aiff" "aif" "mp4" "webm"})

(defonce audio-agent-semaphore
  (delay
    (let [permits (max 1 (app-config/audio-analysis-agent-max-concurrency))]
      (println (str "[audio-driver] Knoxx audio agent concurrency limit=" permits))
      (Semaphore. permits true))))

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

(defn- nonblank-str [value]
  (let [s (some-> value str str/trim)]
    (when-not (str/blank? s) s)))

(defn- config-value [config kebab-key snake-key default-value]
  (or (get config kebab-key)
      (get config snake-key)
      default-value))


(defn- knoxx-backend-url [config]
  (nonblank-str (config-value config :knoxx-backend-url :knoxx_backend_url (app-config/knoxx-backend-url))))

(defn- knoxx-api-key [config]
  (nonblank-str (config-value config :knoxx-api-key :knoxx_api_key (app-config/knoxx-api-key))))

(defn- knoxx-user-email [config]
  (nonblank-str (config-value config :knoxx-user-email :knoxx_user_email (app-config/knoxx-user-email))))

(defn- audio-agent-model [config]
  (nonblank-str (config-value config :audio-analysis-model :audio_analysis_model (app-config/audio-analysis-model))))

(defn- audio-agent-contract-id [config]
  (nonblank-str (config-value config :audio-analysis-agent-contract-id :audio_analysis_agent_contract_id (app-config/audio-analysis-agent-contract-id))))

(defn- audio-agent-actor-id [config]
  (nonblank-str (config-value config :audio-analysis-agent-actor-id :audio_analysis_agent_actor_id (app-config/audio-analysis-agent-actor-id))))

(defn- audio-agent-timeout-ms [config]
  (long (config-value config :audio-analysis-agent-timeout-ms :audio_analysis_agent_timeout_ms (app-config/audio-analysis-agent-timeout-ms))))

(defn- audio-agent-poll-ms [config]
  (long (config-value config :audio-analysis-agent-poll-ms :audio_analysis_agent_poll_ms (app-config/audio-analysis-agent-poll-ms))))

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

(defn- data-url [path]
  (str "data:" (audio-mime-type path) ";base64," (file->base64 path)))

(defn- audio-agent-message [path]
  (let [filename (fs/file-name path)
        ext (fs/extension path)
        size (fs/size path)]
    (str "Analyze the attached audio for library ingestion. Treat the audio bytes as primary evidence; do not infer from the filename alone.\n"
         "Return a compact, indexable catalogue note with: (1) what is audible, (2) likely content type, "
         "(3) mood/pacing/production traits, and (4) reusable labels.\n"
         "Filename: " filename "\n"
         "Format: " ext "\n"
         "MIME: " (audio-mime-type path) "\n"
         "Size: " size " bytes\n"
         "Filename-derived title: " (filename->song-title path))))

(defn- audio-agent-content-part [path]
  {:type "audio"
   :data (data-url path)
   :mimeType (audio-mime-type path)
   :filename (fs/file-name path)
   :size (fs/size path)})

(defn- knoxx-headers [config]
  (cond-> {"Content-Type" "application/json"}
    (knoxx-user-email config) (assoc "x-knoxx-user-email" (knoxx-user-email config))
    (knoxx-api-key config) (assoc "X-API-Key" (knoxx-api-key config))))

(defn- knoxx-url [config path]
  (str (str/replace (knoxx-backend-url config) #"/+$" "") path))

(defn- uuid []
  (str (UUID/randomUUID)))

(defn- with-audio-agent-permit
  [path f]
  (let [^Semaphore semaphore @audio-agent-semaphore]
    (when (zero? (.availablePermits semaphore))
      (println (str "[audio-driver] waiting for audio agent slot: " path)))
    (.acquire semaphore)
    (try
      (f)
      (finally
        (.release semaphore)))))

(defn- http-success? [status]
  (<= 200 (long status) 299))

(defn- post-knoxx-json [config path body]
  (let [response (http/post (knoxx-url config path)
                            {:headers (knoxx-headers config)
                             :body (json/generate-string body)
                             :as :json
                             :socket-timeout (audio-agent-timeout-ms config)
                             :connection-timeout 30000
                             :throw-exceptions false})]
    (if (http-success? (:status response))
      (:body response)
      (throw (ex-info (str "Knoxx HTTP " (:status response) ": " (:body response))
                      {:status (:status response)
                       :body (:body response)})))))

(defn- get-knoxx-json [config path]
  (let [response (http/get (knoxx-url config path)
                           {:headers (knoxx-headers config)
                            :as :json
                            :socket-timeout 30000
                            :connection-timeout 15000
                            :throw-exceptions false})]
    (when (http-success? (:status response))
      (:body response))))

(defn- run-answer [run]
  (nonblank-str (or (:answer run)
                    (get-in run [:run :answer]))))

(defn- run-status [run]
  (nonblank-str (or (:status run)
                    (get-in run [:run :status]))))

(defn- wait-for-agent-run! [config run-id]
  (let [deadline (+ (System/currentTimeMillis) (audio-agent-timeout-ms config))
        poll-ms (max 250 (audio-agent-poll-ms config))]
    (loop []
      (let [body (get-knoxx-json config (str "/api/knoxx/runs/" run-id))
            run (or (:run body) body)
            status (run-status run)
            answer (run-answer run)]
        (cond
          (and (= "completed" status) answer)
          answer

          (= "completed" status)
          (throw (ex-info (str "Knoxx audio agent completed without an answer: " run-id)
                          {:run-id run-id
                           :run run}))

          (= "failed" status)
          (throw (ex-info (str "Knoxx audio agent failed: " (or (:error run) "unknown error"))
                          {:run-id run-id
                           :run run}))

          (> (System/currentTimeMillis) deadline)
          (throw (ex-info (str "Timed out waiting for Knoxx audio agent run " run-id)
                          {:run-id run-id}))

          :else
          (do
            (Thread/sleep poll-ms)
            (recur)))))))

(defn- knoxx-agent-audio-description
  "Start a visible Knoxx agent run for audio analysis and wait for its answer."
  [path config]
  (let [run-id (uuid)
        conversation-id (str "audio-ingest-" run-id)
        session-id (uuid)
        model (audio-agent-model config)
        body {:conversation_id conversation-id
              :session_id session-id
              :run_id run-id
              :message (audio-agent-message path)
              :content_parts [(audio-agent-content-part path)]
              :agent_spec {:contract_id (audio-agent-contract-id config)
                           :actor_id (audio-agent-actor-id config)
                           :role "audio-library-analyzer"
                           :model model
                           :thinking_level "off"
                           :context_policy {:max-messages 1
                                            :max-chars 1000
                                            :preserve-system true}
                           :resource_policies {:source "kms-ingestion"
                                               :path (str path)
                                               :filename (fs/file-name path)}}
              :model model}]
    (with-audio-agent-permit
      path
      (fn []
        (post-knoxx-json config "/api/knoxx/direct/start" body)
        (wait-for-agent-run! config run-id)))))

(defn audio-context
  "Return a structured audio context map for UI pinning and ingestion content."
  ([path]
   (audio-context path {}))
  ([path config-or-ignored]
   (let [config (if (map? config-or-ignored) config-or-ignored {})
         song-title (filename->song-title path)
         base-description (base-audio-description path)
         ai-description (knoxx-agent-audio-description path config)
         content (str base-description "\n\nAI Description: " ai-description)]
     {:song-title song-title
      :description ai-description
      :content content})))

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
          full-path (fs/path root-path file-id)]
      (if (fs/exists? full-path)
        (let [{:keys [content]} (audio-context full-path config)
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
