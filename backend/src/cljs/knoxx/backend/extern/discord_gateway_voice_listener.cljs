(ns knoxx.backend.extern.discord-gateway-voice-listener
  "Discord Opus capture, PCM chunking and listener lifetime boundary."
  (:require ["prism-media" :as prism]
            [knoxx.backend.extern.discord-gateway-codec :as codec]))

(def ^:private voice-listener-sample-rate 48000)
(def ^:private voice-listener-channels 2)
(def ^:private voice-listener-bytes-per-sample 2)
(def ^:private voice-listener-min-duration-s 0.8)
(def ^:private voice-listener-silence-debounce-ms 900)
;; Chunk audio into ~25s segments with ~5s overlap so the NPU STT never
;; receives more than it can handle.  When the buffer hits the threshold we
;; flush everything except the overlap (which becomes the head of the next
;; chunk).  This prevents both OOM and mid-word cuts at chunk boundaries.
;;
;; 48000 Hz × 2 channels × 2 bytes/sample × 25 s = 4 800 000 bytes
(def ^:private voice-listener-chunk-threshold-s 25)
;; Overlap must be ≥ the NPU model’s receptive context (≈ 1–2 s) plus a
;; safety margin so words spanning the boundary appear in both chunks.
(def ^:private voice-listener-chunk-overlap-s 5)

(def ^:private voice-listener-chunk-threshold-bytes
  (* voice-listener-sample-rate
     voice-listener-channels
     voice-listener-bytes-per-sample
     voice-listener-chunk-threshold-s))

(def ^:private voice-listener-chunk-overlap-bytes
  (* voice-listener-sample-rate
     voice-listener-channels
     voice-listener-bytes-per-sample
     voice-listener-chunk-overlap-s))

;; ---------------------------------------------------------------------------
;; discord.js imports
;; ---------------------------------------------------------------------------

(defn- voice-listener-create-decoder
  []
  (let [OpusDecoder (some-> prism (aget "opus") (aget "Decoder"))]
    (when-not (fn? OpusDecoder)
      (throw (js/Error. "prism-media Opus decoder unavailable")))
    (new OpusDecoder #js {:rate 48000 :channels 2 :frameSize 960})))

(defn- voice-listener-flush-audio!
  [pcm-buffers silence-timers on-audio uid]
  (when-let [buf (get @pcm-buffers uid)]
    (let [pcm (js/Buffer.concat (js/Array.from buf))
          duration-s (/ (.-length pcm)
                        voice-listener-sample-rate
                        voice-listener-bytes-per-sample
                        voice-listener-channels)
          wav (codec/pcm16le->wav-buffer pcm voice-listener-sample-rate voice-listener-channels)]
      (swap! pcm-buffers dissoc uid)
      (swap! silence-timers dissoc uid)
      (if (< duration-s voice-listener-min-duration-s)
        (js/console.log "[voice:listener] skipping very short audio for" uid "duration:" duration-s "s")
        (do
          (js/console.log "[voice:listener] calling on-audio for" uid "wav bytes:" (.-length wav) "duration:" duration-s "s")
          (on-audio uid wav))))))

(defn- voice-listener-chunk-and-flush!
  [pcm-buffers on-audio uid]
  (when-let [buf (get @pcm-buffers uid)]
    (let [total-pcm (js/Buffer.concat (js/Array.from buf))
          total-len (.-length total-pcm)]
      (when (> total-len voice-listener-chunk-overlap-bytes)
        (let [flush-len (- total-len voice-listener-chunk-overlap-bytes)
              flush-pcm (.slice total-pcm 0 flush-len)
              keep-pcm (.slice total-pcm flush-len)
              duration-s (/ flush-len voice-listener-sample-rate voice-listener-bytes-per-sample voice-listener-channels)
              overlap-s (/ voice-listener-chunk-overlap-bytes voice-listener-sample-rate voice-listener-bytes-per-sample voice-listener-channels)
              wav (codec/pcm16le->wav-buffer flush-pcm voice-listener-sample-rate voice-listener-channels)]
          (swap! pcm-buffers assoc uid #js [keep-pcm])
          (js/console.log "[voice:listener] chunk-flush for" uid
                          "flushed:" duration-s "s"
                          "overlap-kept:" overlap-s "s")
          (on-audio uid wav))))))

(defn- voice-listener-destroy-user-streams!
  [streams decoders uid]
  (when-let [audio-stream (get @streams uid)]
    (try (.destroy audio-stream) (catch js/Error _))
    (swap! streams dissoc uid))
  (when-let [decoder (get @decoders uid)]
    (try (.destroy decoder) (catch js/Error _))
    (swap! decoders dissoc uid)))

(defn- voice-listener-on-start-speaking
  [receiver pcm-buffers streams decoders active-users silence-timers on-start on-audio]
  (fn [user-id]
    (let [uid (str user-id)]
      (when-let [t (get @silence-timers uid)]
        (js/clearTimeout t)
        (swap! silence-timers dissoc uid))
      (when-not (contains? @active-users uid)
        (js/console.log "[voice:listener] >>> SPEAKING START:" uid)
        (swap! active-users conj uid)
        (when on-start (on-start uid))
        (when-not (get @pcm-buffers uid)
          (swap! pcm-buffers assoc uid #js []))
        (let [audio-stream (.subscribe receiver uid)
              decoder (voice-listener-create-decoder)]
          (.pipe audio-stream decoder)
          (.on decoder "data"
               (fn [pcm-chunk]
                 (when-let [buf (get @pcm-buffers uid)]
                   (.push buf pcm-chunk)
                   (let [current-size (reduce (fn [acc b] (+ acc (.-length b))) 0 buf)]
                     (when (> current-size voice-listener-chunk-threshold-bytes)
                       (voice-listener-chunk-and-flush! pcm-buffers on-audio uid))))))
          (.on decoder "error" #(js/console.error "[voice:listener] decoder error for" uid ":" (.-message %)))
          (.on audio-stream "error" #(js/console.error "[voice:listener] audio stream error for" uid ":" (.-message %)))
          (.on audio-stream "end" #(js/console.log "[voice:listener] audio stream ended for" uid))
          (swap! streams assoc uid audio-stream)
          (swap! decoders assoc uid decoder))))))

(defn- voice-listener-on-end-speaking
  [streams decoders active-users silence-timers flush-audio!]
  (fn [user-id]
    (let [uid (str user-id)]
      (js/console.log "[voice:listener] >>> SPEAKING END:" uid)
      (swap! active-users disj uid)
      (voice-listener-destroy-user-streams! streams decoders uid)
      (let [t (js/setTimeout #(flush-audio! uid) voice-listener-silence-debounce-ms)]
        (swap! silence-timers assoc uid t)))))

(defn- voice-listener-stop!
  [guild-id speaking-map on-start-speaking on-end-speaking pcm-buffers streams decoders active-users silence-timers flush-audio!]
  (fn []
    (js/console.log "[voice:listener] stopping for guild:" guild-id)
    (.removeListener speaking-map "start" on-start-speaking)
    (.removeListener speaking-map "end" on-end-speaking)
    (doseq [[_ s] @streams]
      (try (.destroy s) (catch js/Error _)))
    (doseq [[_ d] @decoders]
      (try (.destroy d) (catch js/Error _)))
    (doseq [[uid t] @silence-timers]
      (js/clearTimeout t)
      (flush-audio! uid))
    (reset! pcm-buffers {})
    (reset! streams {})
    (reset! decoders {})
    (reset! active-users #{})
    (reset! silence-timers {})))

(defn gw-start-voice-listener
  "Start voice capture and return a Promise of a stop function."
  [connections guild-id on-start on-audio]
  (js/console.log "[voice:listener] starting for guild:" guild-id "connections:" (.-size connections))
  (let [conn (.get connections guild-id)]
    (if-not conn
      (do
        (js/console.error "[voice:listener] no connection for guild:" guild-id)
        (js/Promise.reject (js/Error. (str "No voice connection for guild: " guild-id))))
      (let [receiver (.-receiver conn)
            speaking-map (.-speaking receiver)
            pcm-buffers (atom {})
            streams (atom {})
            decoders (atom {})
            active-users (atom #{})
            silence-timers (atom {})
            flush-audio! #(voice-listener-flush-audio! pcm-buffers silence-timers on-audio %)
            on-start-speaking (voice-listener-on-start-speaking receiver pcm-buffers streams decoders active-users silence-timers on-start on-audio)
            on-end-speaking (voice-listener-on-end-speaking streams decoders active-users silence-timers flush-audio!)]
        (js/console.log "[voice:listener] attaching listeners")
        (.on speaking-map "start" on-start-speaking)
        (.on speaking-map "end" on-end-speaking)
        (js/Promise.resolve
         (voice-listener-stop! guild-id speaking-map on-start-speaking on-end-speaking
                               pcm-buffers streams decoders active-users silence-timers flush-audio!))))))


;; ---------------------------------------------------------------------------
;; Factory
;; ---------------------------------------------------------------------------

