(defn stored-session-message->agent-message
  [message]
  (let [role (some-> (:role message) str)
        content (some-> (:content message) str)
        content-parts (vec (keep stored-content-part->agent-part (or (:content-parts message) [])))
        payload (cond
                  (seq content-parts) (clj->js content-parts)
                  (not (str/blank? content)) #js [#js {:type "text" :text content}]
                  :else nil)]
    (cond
      (= "compactionSummary" role)
      (when-let [summary (some-> (or (:summary message) (:content message)) str str/trim not-empty)]
        #js {:role "compactionSummary"
             :summary summary
             :tokensBefore (or (:tokensBefore message) (:tokens-before message) 0)
             :timestamp (.now js/Date)})

      (and (contains? #{"user" "assistant" "system"} role)
           payload)
      (let [agent-message #js {:role role
                               :content payload
                               :timestamp (.now js/Date)}]
        ;; Eta-mu auto-compaction's pre-prompt check assumes assistant messages
        ;; have a fully populated usage map. Rehydrated Knoxx transcripts often
        ;; predate usage persistence, so provide a zero-value sentinel instead
        ;; of crashing on `usage.totalTokens` before the next sticky turn.
        (when (= "assistant" role)
          (aset agent-message "usage" (clj->js (or (:usage message)
                                                    {:input 0
                                                     :output 0
                                                     :cacheRead 0
                                                     :cacheWrite 0
                                                     :totalTokens 0}))))
        agent-message))))

(defn- planner-row->stored-session-message
  [row]
  (let [role (some-> (:role row) str)
        text (some-> (:text row) str)]
    (when (and (contains? #{"user" "assistant" "system"} role)
               (not (str/blank? text)))
      {:role role
       :content text})))

(defn- fetch-openplanner-session-messages!
  [config conversation-id]
  (if (or (str/blank? conversation-id)
          (not (http/openplanner-enabled? config)))
    (js/Promise.resolve [])
    (-> (http/openplanner-request! config "GET" (str "/v1/sessions/" conversation-id))
        (.then (fn [body]
                 (->> (or (:rows body) [])
                      (keep planner-row->stored-session-message)
                      vec)))
        (.catch (fn [err]
                  (.warn js/console "[knoxx] failed to fetch OpenPlanner session transcript" err)
                  [])))))

(defn- comparable-session-message
  [message]
  {:role (some-> (:role message) str)
   :content (some-> (:content message) str)})

(defn merge-restored-session-messages
  [base-messages overlay-messages]
  (let [base (vec (or base-messages []))
        overlay (vec (or overlay-messages []))
        base* (mapv comparable-session-message base)
        overlay* (mapv comparable-session-message overlay)
        overlap (loop [n (min (count base*) (count overlay*))]
                  (cond
                    (zero? n) 0
                    (= (subvec base* (- (count base*) n))
                       (subvec overlay* 0 n)) n
                    :else (recur (dec n))))]
    (into base (subvec overlay overlap))))

(defn sync-system-message
  [messages system-prompt]
  (let [items (vec (or messages []))
        prompt (some-> system-prompt str str/trim not-empty)]
    (if-not prompt
      items
      (let [system-index (reduce-kv (fn [_ idx entry]
                                      (when (= "system" (some-> (:role entry) str str/lower-case))
                                        (reduced idx)))
                                    nil
                                    items)]
        (if (some? system-index)
          (let [updated (assoc items system-index {:role "system" :content prompt})]
            (->> updated
                 (keep-indexed (fn [idx entry]
                                 (when (or (not= "system" (some-> (:role entry) str str/lower-case))
                                           (= idx system-index))
                                   entry)))
                 vec))
          (into [{:role "system" :content prompt}] items))))))

(defn message-text-size
  [message]
  (+ (count (str (or (:content message) (:summary message) "")))
     (reduce + 0 (map #(count (str (or (:text %) (:filename %) (:url %) "")))
                      (or (:content-parts message) (:contentParts message) [])))))
(defn stored-content-part->agent-part
  [part]
  (let [part-type (some-> (:type part) str str/lower-case)
        text (some-> (:text part) str)
        url (some-> (:url part) str)
        data (some-> (:data part) str)
        mime-type (some-> (:mimeType part) str)
        filename (some-> (:filename part) str)]
    (case part-type
      "text" (when (not (str/blank? (str text)))
               #js {:type "text" :text text})
      ;; Image part routing for the eta-mu SDK:
      ;; eta-mu requires {type "image" :data <RAW-base64-no-prefix> :mimeType "image/..."}.
      ;; Never use image_url shape — eta-mu does not handle it.
      ;; Remote URLs should have been materialized (fetched → data URI) before reaching here.
      "image" (cond
                (and (string? data) (not (str/blank? data)) (str/starts-with? data "data:"))
                (let [comma (.indexOf data ",")
                      raw   (if (>= comma 0) (.slice data (inc comma)) data)
                      mime  (or mime-type
                                (second (re-find #"data:([^;,]+)" data))
                                "image/png")]
                  #js {:type "image" :data raw :mimeType mime})

                (and (string? data) (not (str/blank? data)))
                #js {:type "image" :data data :mimeType (or mime-type "image/png")}

                (and (string? url) (not (str/blank? url)))
                #js {:type "image_url" :image_url #js {:url url}}

                :else nil)
      "audio" (cond
                (and (string? data) (not (str/blank? data)) (str/starts-with? data "data:"))
                (let [comma (.indexOf data ",")
                      raw   (if (>= comma 0) (.slice data (inc comma)) data)
                      mime  (or mime-type
                                (second (re-find #"data:([^;,]+)" data))
                                "audio/mpeg")]
                  #js {:type "audio"
                       :data raw
                       :mimeType mime
                       :format (or (mime->audio-format mime) "mp3")})

                (and (string? data) (not (str/blank? data)))
                #js {:type "audio"
                     :data data
                     :mimeType mime-type
                     :format (or (mime->audio-format mime-type) "mp3")}

                (and (string? url) (not (str/blank? url)))
                #js {:type "audio"
                     :data url
                     :mimeType mime-type
                     :format (or (mime->audio-format mime-type) "mp3")}

                :else nil)
      "video" (when (not (str/blank? (str (or data url))))
                #js {:type "video" :data (or data url) :mimeType mime-type})
      "document" (when (not (str/blank? (str (or data url))))
                   #js {:type "document" :data (or data url) :mimeType mime-type :filename filename})
      nil)))

(defn- mime->audio-format [mime-type]
  (let [mime (some-> mime-type str str/lower-case)]
    (case mime
      "audio/mpeg" "mp3"
      "audio/mp4" "mp4"
      "audio/wav" "wav"
      "audio/x-wav" "wav"
      "audio/ogg" "ogg"
      "audio/flac" "flac"
      "audio/aac" "aac"
      (some-> mime (str/split #"/") second))))

(defn- split-think-tags
  "Extract a leading <think>...</think> block from assistant text.

   Some Gemma-family models emit thinking traces inline instead of as structured
   reasoning parts. This keeps the assistant answer clean while preserving
   the trace in :reasoning."
  [text]
  (let [text (str (or text ""))
        open-idx (.indexOf text "<think>")
        close-idx (.indexOf text "</think>")]
    (if (and (>= open-idx 0)
             (>= close-idx 0)
             (< open-idx 64)
             (> close-idx open-idx))
      (let [thinking (subs text (+ open-idx (count "<think>")) close-idx)
            after (subs text (+ close-idx (count "</think>")))
            before (subs text 0 open-idx)
            answer (str (or before "") (or after ""))]
        {:reasoning (str/trim thinking)
         :answer (str/trim answer)
         :hadThinkTags true})
      {:reasoning ""
       :answer text
       :hadThinkTags false})))

(defn content-part-type  [part]
  (cond
    (keyword? (:type part)) (name (:type part))
    (string? (:type part)) (:type part)
    :else nil))
