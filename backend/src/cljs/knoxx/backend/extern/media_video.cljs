(ns knoxx.backend.extern.media-video
  "Decode materialized video bytes with a bounded native process and no inherited credentials.
   SPDX-License-Identifier: GPL-3.0-or-later"
  (:require [knoxx.backend.law.media-video :as law]
            [knoxx.backend.extern.media-embedding :as binary]
            [malli.core :as m]
            ["node:child_process" :as child]
            ["node:crypto" :as crypto]))

(defn- digest [bytes]
  (-> (.createHash crypto "sha256") (.update bytes) (.digest "hex")))

(defn- decode-source [part]
  (let [encoded (second (re-matches #"data:video/[^;,]+;base64,(.+)" (or (:data part) "")))
        bytes (when encoded (js/Buffer.from encoded "base64"))]
    (when-not (and bytes (pos? (.-length bytes)) (<= (.-length bytes) law/max-source-bytes))
      (throw (ex-info "Video inspection requires materialized video bytes under25MiB"
                      {:code "video_source_invalid"})))
    bytes))

(defn- run-input! [program bytes arguments max-bytes]
  (js/Promise.
   (fn [resolve reject]
     (let [process (.execFile child program (clj->js arguments)
                              #js {:timeout 30000 :maxBuffer max-bytes :encoding "buffer"
                                   :env #js {:PATH "/usr/local/bin:/usr/bin:/bin"}}
                              (fn [error stdout _stderr]
                                (if error
                                  (reject (ex-info "Bounded video decoding failed"
                                                   {:code "video_decode_failed" :exit-code (.-code error)}))
                                  (resolve stdout))))]
       (.on (.-stdin process) "error" (fn [_] nil))
       (.end (.-stdin process) bytes)))))

(defn- input-arguments []
  ["-v" "error" "-protocol_whitelist" "cache,pipe"
   "-read_ahead_limit" (str law/max-source-bytes) "-i" "cache:pipe:0"])

(defn ^:async probe!
  "Read duration/stream facts only; nested network and file protocols are unavailable."
  [bytes]
  (let [out (await (run-input! "ffprobe" bytes
                              (into (input-arguments)
                                    ["-show_entries" "format=duration:stream=codec_type" "-of" "json"])
                              1048576))
        data (js->clj (.parse js/JSON (.toString out "utf8")) :keywordize-keys true)
        duration (js/Number (get-in data [:format :duration]))]
    (when-not (and (js/Number.isFinite duration) (pos? duration)
                   (some #(= "video" (:codec_type %)) (:streams data)))
      (throw (ex-info "Video duration or stream is unavailable" {:code "video_probe_invalid"})))
    {:duration_ms (js/Math.round (* 1000 duration))
     :has_audio (boolean (some #(= "audio" (:codec_type %)) (:streams data)))}))

(defn ^:async frame!
  "Decode one requested representative position, without claiming measured source PTS."
  [bytes milliseconds]
  (await (run-input! "ffmpeg" bytes
                     (into (input-arguments)
                           ["-threads" "1" "-ss" (str (/ milliseconds 1000))
                            "-map" "0:v:0" "-frames:v" "1" "-vf" "scale=512:512:force_original_aspect_ratio=decrease"
                            "-f" "image2pipe" "-vcodec" "mjpeg" "-q:v" "6" "pipe:1"])
                     200000)))

(defn ^:async audio!
  "Extract only the declared clip as real mono16k WAV bytes."
  [bytes start end]
  (let [pcm (await (run-input! "ffmpeg" bytes
                     (into (input-arguments)
                           ["-threads" "1" "-ss" (str (/ start 1000)) "-t" (str (/ (- end start) 1000))
                            "-map" "0:a:0" "-vn" "-ar" "16000" "-ac" "1" "-f" "s16le" "pipe:1"])
                     524288))]
    (when (zero? (.-length pcm))
      (throw (ex-info "Requested video window contains no audio samples" {:code "video_empty_audio"})))
    (binary/wav-bytes pcm)))

(defn- derivative [bytes mime kind provenance]
  (when-not (pos? (.-length bytes))
    (throw (ex-info "Video derivative contains no bytes" {:code "video_empty_derivative"})))
  {:type kind :mimeType mime :data (str "data:" mime ";base64," (.toString bytes "base64"))
   :derived_from (cond-> provenance
                   (= kind "audio") (assoc :decoded_audio_duration_ms (/ (- (.-length bytes) 44) 32)))})

(defn- ^:async capture!
  [read! mime kind provenance]
  (try {:part (derivative (await (read!)) mime kind provenance)}
       (catch :default error
         {:failure {:kind (:kind provenance) :requested_ms (:requested_ms provenance)
                    :code (or (:code (ex-data error)) "video_decode_failed")}})))

(defn ^:async prepare!
  "Keep successful derivatives and explicit failures, including silent-video audio absence."
  [part window]
  (when-not (m/validate law/Window window)
    (throw (ex-info "Video window requires nonnegative start_ms and duration_ms1..12000"
                    {:code "video_window_invalid"})))
  (let [bytes (decode-source part)
        info (await (probe! bytes))
        _ (when-not (m/validate law/Probe info)
            (throw (ex-info "Video duration or stream metadata is unsupported" {:code "video_probe_invalid"})))
        plan (law/window-plan info window)
        source {:source_sha256 (digest bytes) :source_bytes (.-length bytes)
                :source_url (:source_url part) :source_mime_type (:mimeType part)}
        frames (loop [positions (:requested_frame_ms plan) results []]
                 (if-let [position (first positions)]
                   (recur (next positions)
                          (conj results (await (capture! #(frame! bytes position) "image/jpeg" "image"
                                                         (law/lineage source plan "video-frame" position)))))
                   results))
        audio (when (:has_audio plan)
                (await (capture! #(audio! bytes (:start_ms plan) (:end_ms plan)) "audio/wav" "audio"
                                 (law/lineage source plan "video-audio" (:start_ms plan)))))
        results (cond-> frames audio (conj audio))
        result {:parts (vec (keep :part results)) :coverage plan :source source
                :failed (vec (keep :failure results))}]
    (when-not (m/validate law/Prepared result)
      (throw (ex-info "Video derivative bundle exceeds its output contract" {:code "video_preparation_invalid"})))
    result))
