(ns knoxx.backend.extern.discord-gateway-codec
  "Discord SDK message, attachment, PCM and enumeration boundary."
  (:require ["discord.js" :as discord]))

(defn intent-bits
  "Discord gateway operation: intent-bits." [] (aget discord "GatewayIntentBits"))
(defn partials-enum
  "Discord gateway operation: partials-enum." [] (aget discord "Partials"))
(defn events-enum
  "Discord gateway operation: events-enum." [] (aget discord "Events"))
(defn channel-type-enum
  "Discord gateway operation: channel-type-enum." [] (aget discord "ChannelType"))
(defn Client-class
  "Discord gateway operation: Client-class." [] (aget discord "Client"))

;; ---------------------------------------------------------------------------
;; Internal helpers
;; ---------------------------------------------------------------------------

(defn pcm16le->wav-buffer
  "Wrap raw PCM16LE bytes in a WAV container so ffmpeg (and thus STT) can decode it.

   pcm: Node Buffer of signed 16-bit little-endian samples.
   rate: sample rate in Hz (Discord voice is typically 48000)
   channels: 1 or 2 (Discord voice is typically 2)

   Returns a Node Buffer containing a complete .wav file."
  [pcm rate channels]
  (let [rate (max 1 (long (or rate 48000)))
        channels (max 1 (long (or channels 2)))
        data-size (.-length pcm)
        byte-rate (* rate channels 2)
        block-align (* channels 2)
        wav (js/Buffer.alloc (+ 44 data-size))]
    (.write wav "RIFF" 0)
    (.writeUInt32LE wav (+ 36 data-size) 4)
    (.write wav "WAVE" 8)
    (.write wav "fmt " 12)
    (.writeUInt32LE wav 16 16)
    (.writeUInt16LE wav 1 20)
    (.writeUInt16LE wav channels 22)
    (.writeUInt32LE wav rate 24)
    (.writeUInt32LE wav byte-rate 28)
    (.writeUInt16LE wav block-align 32)
    (.writeUInt16LE wav 16 34)
    (.write wav "data" 36)
    (.writeUInt32LE wav data-size 40)
    (.copy pcm wav 44)
    wav))

(defn- member-role-ids
  [member]
  (try
    (let [roles (when member (.-roles member))
          cache (when roles (.-cache roles))]
      (if cache
        (into-array (for [[role-id _role] cache] role-id))
        #js []))
    (catch js/Error _
      #js [])))

(defn map-message
  "Convert a discord.js Message to a plain JS map."
  [message]
  (let [author (.-author message)
        guild (.-guild message)
        member (.-member message)]
    #js {:id (.-id message)
         :channelId (.-channelId message)
         :guildId (or (when guild (.-id guild)) "")
         :content (or (.-content message) "")
         :authorId (or (when author (.-id author)) "")
         :authorUsername (or (when author (.-username author)) "unknown")
         :authorIsBot (boolean (when author (.-bot author)))
         :authorRoleIds (member-role-ids member)
         :timestamp (try (.toISOString (.-createdAt message))
                         (catch js/Error _ (.toISOString (js/Date.))))
         :attachments (into-array
                       (for [[_id att] (.-attachments message)]
                         #js {:id (.-id att)
                              :filename (or (.-name att) "")
                              :contentType (or (.-contentType att) nil)
                              :size (or (.-size att) 0)
                              :url (or (.-url att) "")}))
         :embeds (into-array
                  (for [embed (.-embeds message)]
                    #js {:title (or (.-title embed) nil)
                         :description (or (.-description embed) nil)
                         :url (or (.-url embed) nil)}))}))

(defn readable-text-channel?
  "Check if a channel is a text-based channel we can read."
  [channel]
  (and channel
       (fn? (.-isTextBased channel))
       (.isTextBased channel)))

(defn sort-newest-first
  "Sort an array of message maps by timestamp, newest first."
  [messages]
  (js/Array.from (.sort (into-array messages)
                        (fn [a b]
                          (.localeCompare (str (aget b "timestamp"))
                                          (str (aget a "timestamp")))))))

(defn split-message
  "Split text into chunks of ≤2000 chars, preferring paragraph/line/word breaks."
  [text]
  (let [normalized (.trim (str (or text "")))]
    (if (<= (.-length normalized) 2000)
      #js [normalized]
      (let [parts (atom #js [])
            remaining (atom normalized)]
        (while (> (.-length @remaining) 2000)
          (let [r @remaining
                split-at-para (.lastIndexOf r "\n\n" 2000)
                split-at-line (.lastIndexOf r "\n" 2000)
                split-at-space (.lastIndexOf r " " 2000)
                split-index (cond
                           (> split-at-para 1000) split-at-para
                           (> split-at-line 1000) split-at-line
                           (> split-at-space 1000) split-at-space
                           :else 2000)]
            (swap! parts (fn [p] (.concat p #js [(.trimEnd (.slice r 0 split-index))])))
            (reset! remaining (.trimStart (.slice r split-index)))))
        (when (> (.-length @remaining) 0)
          (swap! parts (fn [p] (.concat p #js [@remaining]))))
        @parts))))

(defn- attachment-value
  "Read an attachment field from either a CLJS map or a plain JS object."
  [attachment k js-key]
  (or (when (map? attachment) (get attachment k))
      (when (object? attachment) (aget attachment js-key))))

(defn discord-file-payload
  "Discord gateway operation: discord-file-payload."
  [attachment]
  (let [buffer (or (attachment-value attachment :buffer "buffer")
                   (attachment-value attachment :attachment "attachment"))
        filename (or (attachment-value attachment :name "name")
                 (attachment-value attachment :filename "filename")
                 "attachment.bin")]
    (when-not buffer
      (throw (js/Error. "Discord attachment is missing file data")))
    #js {:attachment buffer
         :name filename}))

;; ---------------------------------------------------------------------------
;; Gateway method implementations (extracted for readability)
;; ---------------------------------------------------------------------------

(defn log-fn
  "Return a logger function (or nil) for a given level keyword.

   We avoid the old (.-info? log) style because js/console doesn't expose
   predicate fields; it only exposes methods like .info/.warn/.error."
  [log level]
  (let [candidate (case level
                    :info  (aget log "info")
                    :warn  (aget log "warn")
                    :error (aget log "error")
                    :debug (aget log "debug")
                    nil)]
    (when (fn? candidate)
      (fn [& args]
        (try
          (.apply candidate log (to-array args))
          (catch js/Error _ nil))))))

