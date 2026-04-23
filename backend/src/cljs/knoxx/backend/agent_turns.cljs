(ns knoxx.backend.agent-turns
  (:require [clojure.string :as str]
            [knoxx.backend.agent-hydration :refer [settings-state* ensure-settings! passive-hydration! passive-memory-hydration! build-agent-user-message build-agent-multimodal-message hydration-sources]]
            [knoxx.backend.agent-runtime :refer [ensure-agent-session! remove-agent-session! sync-system-message]]
            [knoxx.backend.authz :as authz :refer [auth-snapshot auth-snapshot-has-principal?]]
            [knoxx.backend.core-memory :refer [extract-mentioned-devel-paths extract-mentioned-urls]]
            [knoxx.backend.openplanner-memory :as openplanner-memory]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.run-state :refer [store-run! append-run-event! update-run! update-run-tool-receipt! backfill-run-tool-input-preview! append-limited latest-assistant-message record-retrieval-sample! tool-event-payload append-run-trace-text! apply-run-tool-trace-event! finalize-run-trace-blocks!]]
            [knoxx.backend.runtime.models :refer [effective-thinking-level normalize-thinking-level model-supports-input?]]
            [knoxx.backend.util.time :refer [now-iso]]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.session-titles :refer [maybe-prime-session-title!]]
            [knoxx.backend.turn-control :as turn-control]
            [knoxx.backend.text :refer [value->preview-text assistant-message-text assistant-message-reasoning-text clip-text content-part-text]]))

(defn- nonblank
  "Return s when it is a non-blank string (after trim)."
  [s]
  (when (string? s)
    (let [trimmed (str/trim s)]
      (when-not (str/blank? trimmed)
        trimmed))))

(defn ensure-session-id
  [node-crypto session-id]
  (or (nonblank session-id)
      (.randomUUID node-crypto)))

(def ^:private RECOVERED-SESSION-KICKOFF-TIMEOUT-MS 5000)
(def ^:private RECOVERED-SESSION-KICKOFF-POLL-MS 25)

(defn- chat-policy-constraints
  [auth-context]
  (let [constraints (authz/ctx-tool-constraints auth-context "agent.chat")]
    (cond
      (map? constraints) constraints
      (and constraints (= "object" (goog/typeOf constraints))) (js->clj constraints :keywordize-keys true)
      :else {})))

(defn- positive-int
  [value]
  (cond
    (number? value) (let [n (int value)]
                      (when (pos? n) n))
    (string? value) (let [parsed (js/parseInt value 10)]
                      (when (and (not (js/isNaN parsed)) (> parsed 0))
                        parsed))
    :else nil))

(defn- allowed-models
  [constraints]
  (let [raw (or (:allowedModels constraints)
                (:allowed-models constraints)
                (:models constraints))]
    (->> (cond
           (sequential? raw) raw
           (array? raw) (array-seq raw)
           :else [])
         (keep nonblank)
         set)))

(defn- chat-rate-limit-principal
  [auth-context]
  (or (get-in auth-context [:membership :id])
      (:membershipId auth-context)
      (get-in auth-context [:user :id])
      (:userId auth-context)
      (get-in auth-context [:user :email])
      (:userEmail auth-context)))

(defn- rate-limit-error
  [max-requests window-seconds]
  (doto (js/Error. (str "Chat rate limit exceeded: more than " max-requests
                        " requests in " window-seconds " seconds"))
    (aset "statusCode" 429)
    (aset "code" "chat_rate_limited")))

(defn- model-policy-error
  [model-id allowed]
  (doto (js/Error. (str "Model '" model-id "' is not allowed for this account. Allowed models: "
                        (str/join ", " (sort allowed))))
    (aset "statusCode" 403)
    (aset "code" "model_not_allowed")))

(defn- enforce-chat-policy!
  [auth-context model-id]
  (let [constraints (chat-policy-constraints auth-context)
        permitted-models (allowed-models constraints)
        max-requests (positive-int (or (:maxRequests constraints)
                                       (:max-requests constraints)))
        window-seconds (positive-int (or (:windowSeconds constraints)
                                         (:window-seconds constraints)))
        principal (some-> (chat-rate-limit-principal auth-context) str not-empty)
        redis-client (redis/get-client)]
    (cond
      (and (seq permitted-models)
           (not (contains? permitted-models model-id)))
      (js/Promise.reject (model-policy-error model-id permitted-models))

      (and principal redis-client max-requests window-seconds)
      (let [key (str "knoxx:chat-rate:" principal ":" window-seconds)]
        (-> (.incr redis-client key)
            (.then (fn [count]
                     (if (= count 1)
                       (-> (.expire redis-client key window-seconds)
                           (.then (fn [_] count)))
                       (js/Promise.resolve count))))
            (.then (fn [count]
                     (if (> count max-requests)
                       (js/Promise.reject (rate-limit-error max-requests window-seconds))
                       (js/Promise.resolve nil))))
            (.catch (fn [err]
                      (if (= (aget err "code") "chat_rate_limited")
                        (js/Promise.reject err)
                        (js/Promise.resolve nil))))))

      :else
      (js/Promise.resolve nil))))

(defn validate-chat-policy!
  [auth-context model-id]
  (enforce-chat-policy! auth-context model-id))

(defn- preview-text-nonblank
  "Like value->preview-text, but returns nil for blank previews so OR chains keep searching." 
  [value max-chars]
  (let [preview (some-> (value->preview-text value max-chars) nonblank)
        lowered (some-> preview str/lower-case)]
    (when-not (contains? #{"null" "undefined"} lowered)
      preview)))

(defn- fenced
  [lang text]
  (str "```" lang "\n" (or text "") "\n```"))

(defn- json-preview-nonblank
  [value max-chars]
  (when (and value (not= value js/undefined))
    (try
      (let [json (.stringify js/JSON value nil 2)]
        (when (string? json)
          (preview-text-nonblank json max-chars)))
      (catch :default _ nil))))

;; These helpers are referenced directly from tests and recovery code. Keep
;; small, top-level implementations available even if later runtime-heavy
;; sections are refactored.

(defn ^:export session->stored-messages
  [session]
  (let [messages (when (and session (array? (aget session "messages")))
                   (array-seq (aget session "messages")))]
    (->> messages
         (keep (fn [message]
                 (let [role (some-> (aget message "role") str)
                       content (aget message "content")
                       text (cond
                              (string? content) (nonblank content)
                              (array? content) (->> (array-seq content)
                                                    (map content-part-text)
                                                    (remove str/blank?)
                                                    (str/join "\n\n")
                                                    nonblank)
                              :else (some-> (aget message "text") nonblank))]
                   (when (and (contains? #{"user" "assistant" "system"} role)
                              text)
                     {:role role
                      :content text}))))
         vec)))

(defn ^:export model-ready-content-parts
  [config model-id content-parts]
  (->> (or content-parts [])
       (mapcat (fn [part]
                 (let [part-type (cond
                                   (keyword? (:type part)) (name (:type part))
                                   (string? (:type part)) (:type part)
                                   :else nil)
                       part-name (or (:filename part) (:url part) "attachment")]
                   (cond
                     (or (nil? part-type)
                         (= part-type "text")
                         (model-supports-input? config model-id part-type))
                     [part]

                     (= part-type "audio")
                     [{:type :text
                       :text (str "Uploaded audio source '" part-name "' is available, but model " model-id " does not declare audio input. Use audio.spectrogram if you need an image-friendly audio view.")}]

                     :else
                     [{:type :text
                       :text (str "Uploaded " part-type " '" part-name "' is available, but model " model-id " does not declare " part-type " input.")}]))))
       vec))

(defn- tool-args->markdown-preview
  "Tool-specific input previews that are always human readable (no raw JSON).

   This is intentionally conservative: we only special-case tools where we know
   the user expectation is strict (bash/read). Everything else falls back to
   value->preview-text rendering (which the frontend formats into bullets)."
  [tool-name raw-args]
  (let [tool-name (-> (str (or tool-name ""))
                      (str/split #"[./:]")
                      last
                      str/lower-case)
        args (cond
               (map? raw-args) raw-args
               (and raw-args (not= raw-args js/undefined))
               (try
                 (js->clj raw-args :keywordize-keys true)
                 (catch :default _ nil))
               :else nil)
        arg-value (fn [& keys]
                    (or (some (fn [k]
                                (cond
                                  (and (map? args) (keyword? k)) (get args k)
                                  (and (map? args) (string? k)) (or (get args k) (get args (keyword k)))
                                  :else nil))
                              keys)
                        (some (fn [k]
                                (cond
                                  (and (map? raw-args) (keyword? k)) (get raw-args k)
                                  (and (map? raw-args) (string? k)) (or (get raw-args k) (get raw-args (keyword k)))
                                  :else nil))
                              keys)
                        (some (fn [k]
                                (when (and raw-args
                                           (not= raw-args js/undefined)
                                           (or (object? raw-args) (fn? raw-args)))
                                  (aget raw-args (if (keyword? k) (name k) (str k)))))
                              keys)))]
    (case tool-name
      "bash"
      (when (map? args)
        (let [cmd (arg-value :command :cmd)
              timeout (arg-value :timeout :timeoutSeconds :timeoutMs)]
          (when (and (string? cmd) (not (str/blank? cmd)))
            (let [[clipped-cmd clipped?] (clip-text cmd 20000)]
              (str
               (fenced "bash" (if clipped? (str clipped-cmd "…") clipped-cmd))
               (when clipped? "\n\n_(truncated)_")
               (when (some? timeout)
                 (str "\n\n- timeout: " timeout)))))))

      "read"
      (let [path (arg-value :path "path")
            offset (arg-value :offset "offset")
            limit (arg-value :limit "limit")]
        (when (and (string? path) (not (str/blank? path)))
          (fenced "yaml"
                  (str "path: " path
                       "\noffset: " (if (some? offset) offset "(default)")
                       "\nlimit: " (if (some? limit) limit "(default)")))))

      nil)))

(defn- copy-js-object
  [value]
  (when (and value
             (not= value js/undefined)
             (not (array? value))
             (= "object" (goog/typeOf value)))
    (let [copy (js-obj)
          own-keys (distinct (concat (array-seq (.keys js/Object value))
                                     (array-seq (.getOwnPropertyNames js/Object value))))]
      (doseq [k own-keys]
        (aset copy k (aget value k)))
      copy)))

(defn- tool-call-input-preview
  [tool-name raw-args]
  (or (tool-args->markdown-preview tool-name raw-args)
      (preview-text-nonblank raw-args 20000)
      (json-preview-nonblank raw-args 20000)
      (let [copied (copy-js-object raw-args)]
        (or (tool-args->markdown-preview tool-name copied)
            (preview-text-nonblank copied 20000)
            (json-preview-nonblank copied 20000)))))

(defn- tool-call-preview-from-part
  [part]
  (let [part-type (some-> (aget part "type") str str/lower-case)]
    (when (contains? #{"toolcall" "tool_call"} part-type)
      (let [tool-call-id (some-> (aget part "id") str nonblank)
            tool-name (some-> (aget part "name") str nonblank)
            arguments (aget part "arguments")
            input-preview (tool-call-input-preview tool-name arguments)]
        (when (and tool-call-id input-preview)
          {:tool_call_id tool-call-id
           :tool_name tool-name
           :input_preview input-preview})))))

(defn ^:export assistant-tool-call-previews
  [assistant-message]
  (let [content (when assistant-message
                  (aget assistant-message "content"))]
    (if (array? content)
      (->> (array-seq content)
           (keep tool-call-preview-from-part)
           vec)
      [])))

;; Death-spiral guardrails: if the agent repeatedly calls the same tool with the same
;; input signature, abort the turn to prevent infinite loops.
(def ^:private DEATH_SPIRAL_STREAK_LIMIT 6)
(def ^:private DEATH_SPIRAL_TOTAL_LIMIT 12)

(defonce conversation-access* (atom {}))
(defonce lounge-messages* (atom []))

(defn ensure-conversation-access!
  [ctx conversation-id]
  (authz/ensure-conversation-access! conversation-access* ctx conversation-id))

(defn remember-conversation-access!
  [ctx conversation-id]
  (authz/remember-conversation-access! conversation-access* ctx conversation-id))

(defn index-run-memory!
  [config run]
  (openplanner-memory/index-run-memory! config run extract-mentioned-devel-paths extract-mentioned-urls))

(defn- requested-system-prompt
  [agent-spec]
  (some-> (:system-prompt agent-spec) str str/trim not-empty))

(defn- ensure-system-message
  [messages agent-spec]
  (sync-system-message messages (requested-system-prompt agent-spec)))

(defn- agent-spec-summary
  [agent-spec]
  (when agent-spec
    (cond-> {}
      (:contract-id agent-spec) (assoc :contractId (:contract-id agent-spec))
      (:actor-id agent-spec) (assoc :actorId (:actor-id agent-spec))
      (seq (:contract-actors agent-spec)) (assoc :contractActors (vec (:contract-actors agent-spec)))
      (:role agent-spec) (assoc :role (:role agent-spec))
      (:model agent-spec) (assoc :model (:model agent-spec))
      (:thinking-level agent-spec) (assoc :thinkingLevel (:thinking-level agent-spec))
      (:system-prompt agent-spec) (assoc :hasSystemPrompt true)
      (:task-prompt agent-spec) (assoc :hasTaskPrompt true)
      (seq (:tool-policies agent-spec)) (assoc :toolPolicies (vec (:tool-policies agent-spec)))
      (:resource-policies agent-spec) (assoc :resourcePolicies (:resource-policies agent-spec)))))

(defn- diff-appended-text
  [previous current]
  (let [previous (str (or previous ""))
        current (str (or current ""))
        max-overlap (fn [left right]
                      (loop [n (min (count left) (count right))]
                        (cond
                          (zero? n) 0
                          (str/ends-with? left (.slice right 0 n)) n
                          :else (recur (dec n)))))]
    (cond
      (str/blank? current) ""
      (str/blank? previous) current
      (= current previous) ""
      (str/starts-with? current previous) (.slice current (count previous))
      :else (.slice current (max-overlap previous current)))))

(defn- media-part-url
  [part]
  (or (nonblank (aget part "url"))
      (nonblank (aget part "file_url"))
      (nonblank (aget part "fileUrl"))
      (let [image-url (aget part "image_url")]
        (cond
          (string? image-url) (nonblank image-url)
          image-url (nonblank (aget image-url "url"))
          :else nil))
      (let [video-url (aget part "video_url")]
        (cond
          (string? video-url) (nonblank video-url)
          video-url (nonblank (aget video-url "url"))
          :else nil))
      (let [audio-url (aget part "audio_url")]
        (cond
          (string? audio-url) (nonblank audio-url)
          audio-url (nonblank (aget audio-url "url"))
          :else nil))
      (let [source (aget part "source")]
        (when (and source
                   (= "url" (some-> (aget source "type") str str/lower-case)))
          (nonblank (aget source "url"))))))

(defn- media-part-data
  [part]
  (or (nonblank (aget part "data"))
      (nonblank (aget part "b64_json"))
      (nonblank (aget part "result"))
      (let [input-audio (aget part "input_audio")]
        (when input-audio
          (nonblank (aget input-audio "data"))))
      (let [output-audio (aget part "output_audio")]
        (when output-audio
          (nonblank (aget output-audio "data"))))
      (let [source (aget part "source")]
        (when (and source
                   (= "base64" (some-> (aget source "type") str str/lower-case)))
          (nonblank (aget source "data"))))))

(defn- media-part-mime-type
  [part media-kind]
  (or (nonblank (aget part "mimeType"))
      (nonblank (aget part "mime_type"))
      (nonblank (aget part "mediaType"))
      (nonblank (aget part "media_type"))
      (let [source (aget part "source")]
        (when source
          (or (nonblank (aget source "media_type"))
              (nonblank (aget source "mime_type")))))
      (let [input-audio (aget part "input_audio")
            format (when input-audio
                     (nonblank (aget input-audio "format")))]
        (when format
          (str "audio/" format)))
      (let [output-audio (aget part "output_audio")
            format (when output-audio
                     (nonblank (aget output-audio "format")))]
        (when format
          (str "audio/" format)))
      (case media-kind
        "image" "image/png"
        "audio" "audio/wav"
        "video" "video/mp4"
        "document" "application/octet-stream"
        nil)))

(defn- media-part-filename
  [part]
  (or (nonblank (aget part "filename"))
      (nonblank (aget part "file_name"))
      (nonblank (aget part "fileName"))
      (nonblank (aget part "name"))))

(defn- media-part-size
  [part]
  (let [value (or (aget part "size")
                  (aget part "bytes")
                  (aget part "byte_size")
                  (aget part "byteSize"))]
    (when (number? value)
      value)))

(defn- assistant-media-part
  [part]
  (let [raw-type (some-> (aget part "type") str str/lower-case)
        media-kind (cond
                     (contains? #{"image" "image_url" "input_image" "output_image"} raw-type) "image"
                     (contains? #{"audio" "audio_url" "input_audio" "output_audio"} raw-type) "audio"
                     (contains? #{"video" "video_url" "input_video" "output_video"} raw-type) "video"
                     (contains? #{"document" "file" "input_file" "output_file"} raw-type) "document"
                     :else nil)
        url (media-part-url part)
        raw-data (media-part-data part)
        mime-type (media-part-mime-type part media-kind)
        data (when raw-data
               (if (str/starts-with? raw-data "data:")
                 raw-data
                 (str "data:" mime-type ";base64," raw-data)))
        filename (media-part-filename part)
        size (media-part-size part)]
    (when (and media-kind (or url data))
      (cond-> {:type media-kind}
        url (assoc :url url)
        data (assoc :data data)
        mime-type (assoc :mimeType mime-type)
        filename (assoc :filename filename)
        size (assoc :size size)))))

(defn- assistant-content-parts
  [assistant-message]
  (let [content (when assistant-message
                  (aget assistant-message "content"))]
    (if (array? content)
      (->> (array-seq content)
           (keep assistant-media-part)
           vec)
      [])))

(defn- session-message-text
  [message]
  (let [content (aget message "content")]
    (cond
      (string? content) content
      (array? content) (->> (array-seq content)
                            (map content-part-text)
                            (remove str/blank?)
                            (str/join "\n\n"))
      (string? (aget message "text")) (aget message "text")
      :else "")))

(defn session->stored-messages
  [session]
  (let [messages (when (and session (array? (aget session "messages")))
                   (array-seq (aget session "messages")))]
    (->> messages
         (keep (fn [message]
                 (let [role (some-> (aget message "role") str)
                       text (some-> (session-message-text message) nonblank)
                       parts (when (= role "assistant")
                               (assistant-content-parts message))]
                   (when (and (contains? #{"user" "assistant" "system"} role)
                              (or text (seq parts)))
                     (cond-> {:role role}
                       text (assoc :content text)
                       (seq parts) (assoc :content-parts parts))))))
         vec)))

(defn- append-message-if-novel
  [messages message]
  (let [items (vec (or messages []))
        last-message (peek items)
        comparable (fn [entry]
                     (select-keys entry [:role :content :content-parts]))]
    (if (= (comparable last-message) (comparable message))
      items
      (conj items message))))

(defn- transcript-before-prompt
  [session user-message agent-spec]
  (-> (session->stored-messages session)
      (ensure-system-message agent-spec)
      (append-message-if-novel user-message)))

(defn- transcript-after-turn
  [session fallback-messages]
  (let [snapshot (session->stored-messages session)]
    (if (seq snapshot)
      snapshot
      (vec fallback-messages))))

(defn- tool-result-media-type
  [value]
  (case (some-> value str str/lower-case)
    ("image" "image_url" "output_image") "image"
    ("audio" "audio_url" "output_audio") "audio"
    ("video" "video_url" "output_video") "video"
    ("document" "file" "output_file") "document"
    nil))

(defn- tool-result-content-part
  [part]
  (let [media-type (tool-result-media-type (aget part "type"))
        data (nonblank (aget part "data"))
        url (nonblank (aget part "url"))
        mime-type (or (nonblank (aget part "mimeType"))
                      (nonblank (aget part "mime_type"))
                      (nonblank (aget part "mediaType"))
                      (nonblank (aget part "media_type")))
        filename (or (nonblank (aget part "filename"))
                     (nonblank (aget part "fileName"))
                     (nonblank (aget part "name")))
        size (let [value (or (aget part "size")
                             (aget part "bytes")
                             (aget part "byteSize")
                             (aget part "byte_size"))]
               (when (number? value)
                 value))]
    (when (and media-type (or data url))
      (cond-> {:type media-type}
        data (assoc :data data)
        url (assoc :url url)
        mime-type (assoc :mimeType mime-type)
        filename (assoc :filename filename)
        size (assoc :size size)))))

(defn- tool-result-content-parts
  [tool-result]
  (let [details (when tool-result (aget tool-result "details"))
        raw-parts (or (when tool-result (aget tool-result "content_parts"))
                      (when tool-result (aget tool-result "contentParts"))
                      (when details (aget details "content_parts"))
                      (when details (aget details "contentParts"))
                      (when details (aget details "attachments")))]
    (if (array? raw-parts)
      (->> (array-seq raw-parts)
           (keep tool-result-content-part)
           vec)
      [])))

(defn merge-content-parts
  [& groups]
  (->> groups
       (mapcat #(or % []))
       (reduce (fn [acc part]
                 (if (some #(= % part) acc)
                   acc
                   (conj acc part)))
               [])
       vec))

(defn reply-attachment-content-parts
  [tool-receipts]
  (->> (or tool-receipts [])
       (filter #(= "workspace_media.attach" (:tool_name %)))
       (mapcat #(or (:content_parts %) (:contentParts %) []))
       vec))

(defn- content-part-label
  [part]
  (let [part-type (cond
                    (keyword? (:type part)) (name (:type part))
                    (string? (:type part)) (:type part)
                    :else nil)]
    (case part-type
      "image" "image"
      "audio" "audio file"
      "video" "video"
      "document" "document"
      "attachment")))

(defn- content-part-name
  [part]
  (or (:filename part)
      (:url part)
      (content-part-label part)))

(defn model-ready-content-parts
  [config model-id content-parts]
  (->> (or content-parts [])
       (mapcat (fn [part]
                 (let [part-type (cond
                                   (keyword? (:type part)) (name (:type part))
                                   (string? (:type part)) (:type part)
                                   :else nil)]
                   (cond
                     (or (nil? part-type)
                         (= part-type "text")
                         (model-supports-input? config model-id part-type))
                     [part]

                     (= part-type "audio")
                     [{:type :text
                       :text (str "Uploaded audio source '" (content-part-name part) "' is available, but model " model-id " does not declare audio input. Use audio.spectrogram if you need an image-friendly audio view.")}]

                     :else
                     [{:type :text
                       :text (str "Uploaded " (content-part-label part) " '" (content-part-name part) "' is available, but model " model-id " does not declare " part-type " input.")}]))))
       vec))

(defn send-agent-turn!
  [runtime config {:keys [conversation-id session-id message content-parts model mode run-id auth-context thinking-level agent-spec]}]
  (let [node-crypto (aget runtime "crypto")
        conversation-id (or conversation-id (.randomUUID node-crypto))
        session-id (ensure-session-id node-crypto session-id)
        _ (ensure-conversation-access! auth-context conversation-id)
        _ (remember-conversation-access! auth-context conversation-id)
        mode (or mode "direct")
        requested-model (or model (:model agent-spec))
        model-id (or requested-model (:llmModel (ensure-settings! config)) (:proxx-default-model config))
        thinking-level-raw (or thinking-level (:thinking-level agent-spec))
        parsed-thinking-level (when thinking-level-raw
                                (normalize-thinking-level thinking-level-raw))
        thinking-level (effective-thinking-level config model-id (or parsed-thinking-level
                                                                    thinking-level-raw
                                                                    (:agent-thinking-level config)
                                                                    "off"))
        run-id (or run-id (.randomUUID node-crypto))
        started-at (now-iso)
        started-ms (.now js/Date)
        existing-messages (vec (or (:messages (session-store/get-session-sync session-id)) []))
        seeded-messages (ensure-system-message existing-messages agent-spec)
        ;; Build user message - support both text and multimodal content
        user-message (if (seq content-parts)
                       {:role "user" :content message :content-parts content-parts}
                       {:role "user" :content message})
        prompt-content-parts (model-ready-content-parts config model-id content-parts)
        request-messages (conj seeded-messages user-message)
        _title-prime (maybe-prime-session-title! runtime config conversation-id message)
        auth-extra (auth-snapshot auth-context)
        base-run (merge {:run_id run-id
                         :session_id session-id
                         :conversation_id conversation-id
                         :created_at started-at
                         :updated_at started-at
                         :status "running"
                         :model model-id
                         :ttft_ms nil
                         :total_time_ms nil
                         :input_tokens nil
                         :output_tokens nil
                         :tokens_per_s nil
                         :error nil
                         :answer nil
                         :content_parts []
                         :events []
                         :trace_blocks []
                         :tool_receipts []
                         :request_messages request-messages
                         :settings (cond-> {:sessionId session-id
                                            :conversationId conversation-id
                                            :mode mode
                                            :thinkingLevel thinking-level
                                            :workspaceRoot (:workspace-root config)}
                                     agent-spec (assoc :agentSpec (agent-spec-summary agent-spec)))
                         :resources (cond-> {:provider "proxx"
                                             :collection (:collection-name config)}
                                      (get agent-spec :resource-policies) (assoc :agentResourcePolicies (get agent-spec :resource-policies))) }
                        auth-extra)
        _ (store-run! run-id base-run)
        _ (session-store/put-session! (redis/get-client)
                                      (merge (cond-> {:session_id session-id
                                                      :conversation_id conversation-id
                                                      :run_id run-id
                                                      :status "running"
                                                      :model model-id
                                                      :mode mode
                                                      :thinking_level thinking-level
                                                      :created_at started-at
                                                      :updated_at started-at
                                                      :has_active_stream false
                                                      :messages request-messages}
                                               agent-spec (assoc :agent_spec (agent-spec-summary agent-spec)))
                                             auth-extra))
        initial-event (tool-event-payload run-id conversation-id session-id "run_started"
                                          {:status "running"
                                           :mode mode
                                           :model model-id
                                           :thinking_level thinking-level})
        _ (append-run-event! run-id initial-event)
        _ (broadcast-ws-session! session-id "events" initial-event)
        chunks (atom [])
        reasoning-chunks (atom [])]
    (cond
      (and thinking-level-raw (nil? parsed-thinking-level))
      (js/Promise.reject (js/Error. (str "Unsupported thinking level: " thinking-level-raw ". Expected one of off, minimal, low, medium, high, xhigh.")))

      :else
      (-> (enforce-chat-policy! auth-context model-id)
          (.then (fn [_]
                   (-> (.all js/Promise
                              #js [(passive-hydration! runtime config mode message auth-context)
                                   (passive-memory-hydration! config conversation-id message auth-context)])
                       (.then (fn [results]
                 (let [hydration (aget results 0)
                       memory-hydration (aget results 1)]
                   (when hydration
                     (let [hydration-event (tool-event-payload run-id conversation-id session-id "passive_hydration"
                                                               {:status "ok"
                                                                :hits (count (:results hydration))
                                                                :elapsed_ms (:elapsedMs hydration)})]
                       (update-run! run-id
                                    (fn [run]
                                      (-> run
                                          (update :resources merge {:passiveHydration (select-keys hydration [:query :tokens :database :elapsedMs :results])})
                                          (assoc :updated_at (now-iso)))))
                       (append-run-event! run-id hydration-event)
                       (broadcast-ws-session! session-id "events" hydration-event)))
                   (when (seq (:hits memory-hydration))
                     (let [memory-event (tool-event-payload run-id conversation-id session-id "memory_hydration"
                                                            {:status "ok"
                                                             :hits (count (:hits memory-hydration))
                                                             :elapsed_ms (:elapsedMs memory-hydration)})]
                       (update-run! run-id
                                    (fn [run]
                                      (-> run
                                          (update :resources merge {:memoryHydration (select-keys memory-hydration [:query :mode :hits :elapsedMs :conversationId])})
                                          (assoc :updated_at (now-iso)))))
                       (append-run-event! run-id memory-event)
                       (broadcast-ws-session! session-id "events" memory-event)))
                   (-> (ensure-agent-session! runtime config conversation-id model-id auth-context thinking-level session-id agent-spec)
                     (.then (fn [session]
                              (let [persisted-request-messages (transcript-before-prompt session user-message agent-spec)
                                    _ (session-store/update-session! (redis/get-client)
                                                                     session-id
                                                                     {:status "running"
                                                                      :has_active_stream false
                                                                      :messages persisted-request-messages
                                                                      :conversation_id conversation-id
                                                                      :run_id run-id})
                                    ttft-recorded? (atom false)
                                    last-assistant-text* (atom "")
                                    last-reasoning-text* (atom "")
                                    aborting? (atom false)
                                    abort-reason* (atom nil)
                                    tool-loop* (atom {:last nil :streak 0 :counts {}})
                                    emit-streaming-delta! (fn [kind delta]
                                                            (when (seq delta)
                                                              (when (and (= kind :agent_message)
                                                                         (not @ttft-recorded?))
                                                                (reset! ttft-recorded? true)
                                                                (let [ttft-ms (- (.now js/Date) started-ms)
                                                                      ttft-event (tool-event-payload run-id conversation-id session-id "assistant_first_token"
                                                                                                     {:status "streaming"
                                                                                                      :ttft_ms ttft-ms})]
                                                                  (update-run! run-id #(assoc % :ttft_ms ttft-ms))
                                                                  (append-run-event! run-id ttft-event)
                                                                  (broadcast-ws-session! session-id "events" ttft-event)
                                                                  (session-store/mark-session-streaming! (redis/get-client) session-id true)))
                                                              (if (= kind :agent_message)
                                                                (do
                                                                  (swap! chunks conj delta)
                                                                  (swap! last-assistant-text* str delta))
                                                                (do
                                                                  (swap! reasoning-chunks conj delta)
                                                                  (swap! last-reasoning-text* str delta)))
                                                              (append-run-trace-text! run-id kind delta (now-iso))
                                                              (broadcast-ws-session! session-id "tokens"
                                                                                     {:run_id run-id
                                                                                      :conversation_id conversation-id
                                                                                      :session_id session-id
                                                                                      :kind (if (= kind :agent_message) "assistant_message" "reasoning")
                                                                                      :token delta})))
                                    sync-assistant-message! (fn [assistant-message]
                                                             (when (and assistant-message
                                                                        (= (aget assistant-message "role") "assistant"))
                                                               (let [full-text (assistant-message-text assistant-message)
                                                                     full-reasoning (assistant-message-reasoning-text assistant-message)
                                                                     tool-previews (assistant-tool-call-previews assistant-message)
                                                                     text-delta (diff-appended-text @last-assistant-text* full-text)
                                                                     reasoning-delta (diff-appended-text @last-reasoning-text* full-reasoning)]
                                                                 (reset! last-assistant-text* (str (or full-text "")))
                                                                 (reset! last-reasoning-text* (str (or full-reasoning "")))
                                                                 (doseq [{:keys [tool_call_id tool_name input_preview]} tool-previews]
                                                                   (backfill-run-tool-input-preview! run-id tool_call_id tool_name input_preview))
                                                                 (emit-streaming-delta! :agent_message text-delta)
                                                                 (emit-streaming-delta! :reasoning reasoning-delta))))
                                    request-abort! (fn [reason]
                                                     (let [reason (str (or reason "aborted"))]
                                                       (if @aborting?
                                                         (js/Promise.resolve nil)
                                                         (do
                                                           (reset! aborting? true)
                                                           (reset! abort-reason* reason)
                                                           ;; Drop streaming flag immediately so the UI can re-enable the composer.
                                                           (session-store/mark-session-streaming! (redis/get-client) session-id false)
                                                           (let [abort-event (tool-event-payload run-id conversation-id session-id "abort_requested"
                                                                                                 {:status "aborting"
                                                                                                  :reason reason})]
                                                             (append-run-event! run-id abort-event)
                                                             (broadcast-ws-session! session-id "events" abort-event))
                                                           (.abort session)))))
                                    _registered (turn-control/register-active-turn!
                                                 conversation-id
                                                 {:run_id run-id
                                                  :session_id session-id
                                                  :started_at started-at
                                                  :abort! request-abort!})
                                    unsubscribe (.subscribe session
                                                          (fn [event]
                                                            (let [event-type (aget event "type")]
                                                              (cond
                                                                (= event-type "message_update")
                                                                (let [assistant-event (aget event "assistantMessageEvent")
                                                                      assistant-event-type (aget assistant-event "type")]
                                                                  (cond
                                                                    (= assistant-event-type "text_delta")
                                                                    (let [delta (or (aget assistant-event "delta") "")]
                                                                      (emit-streaming-delta! :agent_message delta))

                                                                    (contains? #{"reasoning_delta" "reasoning" "reasoning_content_delta" "thinking_delta" "thinking"} assistant-event-type)
                                                                    (let [delta (or (aget assistant-event "delta")
                                                                                    (aget assistant-event "text")
                                                                                    (aget assistant-event "reasoning")
                                                                                    (aget assistant-event "thinking")
                                                                                    "")]
                                                                      (emit-streaming-delta! :reasoning delta))

                                                                    (contains? #{"toolcall_delta" "tool_call_delta"} assistant-event-type)
                                                                    (sync-assistant-message! (or (aget assistant-event "partial")
                                                                                                (aget event "message")))

                                                                    (contains? #{"toolcall_end" "tool_call_end"} assistant-event-type)
                                                                    (do
                                                                      (when-let [preview (tool-call-preview-from-part (aget assistant-event "toolCall"))]
                                                                        (backfill-run-tool-input-preview! run-id
                                                                                                          (:tool_call_id preview)
                                                                                                          (:tool_name preview)
                                                                                                          (:input_preview preview))
                                                                        (let [preview-event (tool-event-payload run-id conversation-id session-id "tool_input_backfill"
                                                                                                              {:status "running"
                                                                                                               :tool_name (:tool_name preview)
                                                                                                               :tool_call_id (:tool_call_id preview)
                                                                                                               :preview (:input_preview preview)})]
                                                                          (append-run-event! run-id preview-event)
                                                                          (broadcast-ws-session! session-id "events" preview-event)))
                                                                      (sync-assistant-message! (or (aget assistant-event "partial")
                                                                                                  (aget event "message"))))

                                                                    :else (sync-assistant-message! (aget event "message"))))

                                                                (= event-type "message_end")
                                                                (sync-assistant-message! (aget event "message"))

                                                                (= event-type "tool_execution_start")
                                                                (let [tool-name (or (aget event "toolName") "tool")
                                                                      tool-call-id (or (aget event "toolCallId") (.randomUUID node-crypto))
                                                                      raw-args (or (aget event "params")
                                                                                   (aget event "toolArgs")
                                                                                   (aget event "args")
                                                                                   (aget event "arguments")
                                                                                   (aget event "input")
                                                                                   (aget event "parameters"))
                                                                      input-preview (or (tool-call-input-preview tool-name (aget event "params"))
                                                                                        (tool-call-input-preview tool-name (aget event "toolArgs"))
                                                                                        (tool-call-input-preview tool-name (aget event "args"))
                                                                                        (tool-call-input-preview tool-name (aget event "arguments"))
                                                                                        (tool-call-input-preview tool-name (aget event "input"))
                                                                                        (tool-call-input-preview tool-name (aget event "parameters"))
                                                                                        (tool-call-input-preview tool-name raw-args))
                                                                      signature (str tool-name "::" (or input-preview ""))
                                                                      _death-spiral
                                                                      (let [{:keys [last streak counts]} @tool-loop*
                                                                            next-total (inc (get counts signature 0))
                                                                            next-counts (assoc counts signature next-total)
                                                                            next-streak (if (= signature last) (inc streak) 1)]
                                                                        (reset! tool-loop* {:last signature
                                                                                            :streak next-streak
                                                                                            :counts next-counts})
                                                                        (when (and (not @aborting?)
                                                                                   (or (>= next-streak DEATH_SPIRAL_STREAK_LIMIT)
                                                                                       (>= next-total DEATH_SPIRAL_TOTAL_LIMIT)))
                                                                          (let [reason (str "death_spiral_detected: tool '" tool-name "' repeated " next-total "x (streak " next-streak ")")
                                                                                spiral-event (tool-event-payload run-id conversation-id session-id "death_spiral_detected"
                                                                                                                 {:status "failed"
                                                                                                                  :tool_name tool-name
                                                                                                                  :tool_call_id tool-call-id
                                                                                                                  :count next-total
                                                                                                                  :streak next-streak})]
                                                                            (append-run-event! run-id spiral-event)
                                                                            (broadcast-ws-session! session-id "events" spiral-event)
                                                                            (request-abort! reason))))
                                                                      tool-event (tool-event-payload run-id conversation-id session-id "tool_start"
                                                                                                     {:status "running"
                                                                                                      :tool_name tool-name
                                                                                                      :tool_call_id tool-call-id
                                                                                                      :preview input-preview})]
                                                                  (update-run-tool-receipt! run-id tool-call-id {:tool_name tool-name}
                                                                                            (fn [receipt]
                                                                                              (cond-> (merge receipt {:tool_name tool-name
                                                                                                                      :status "running"
                                                                                                                      :started_at (or (:started_at receipt) (now-iso))})
                                                                                                input-preview (assoc :input_preview input-preview))))
                                                                  (apply-run-tool-trace-event! run-id {:type "tool_start"
                                                                                                       :tool_name tool-name
                                                                                                       :tool_call_id tool-call-id
                                                                                                       :preview input-preview
                                                                                                       :at (now-iso)})
                                                                  (append-run-event! run-id tool-event)
                                                                  (broadcast-ws-session! session-id "events" tool-event))

                                                                (= event-type "tool_execution_update")
                                                                (let [tool-name (or (aget event "toolName") "tool")
                                                                      tool-call-id (or (aget event "toolCallId") (str tool-name "-update"))
                                                                      preview (or (value->preview-text (aget event "delta") 400)
                                                                                  (value->preview-text (aget event "update") 400)
                                                                                  (value->preview-text (aget event "message") 400)
                                                                                  (value->preview-text (aget event "statusMessage") 400))]
                                                                  (update-run-tool-receipt! run-id tool-call-id {:tool_name tool-name}
                                                                                            (fn [receipt]
                                                                                              (cond-> (merge receipt {:tool_name tool-name
                                                                                                                      :status "running"})
                                                                                                preview (update :updates #(append-limited % preview 8)))))
                                                                  (apply-run-tool-trace-event! run-id {:type "tool_update"
                                                                                                       :tool_name tool-name
                                                                                                       :tool_call_id tool-call-id
                                                                                                       :preview preview
                                                                                                       :at (now-iso)})
                                                                  (when preview
                                                                    (let [tool-event (tool-event-payload run-id conversation-id session-id "tool_update"
                                                                                                         {:status "running"
                                                                                                          :tool_name tool-name
                                                                                                          :tool_call_id tool-call-id
                                                                                                          :preview preview})]
                                                                      (append-run-event! run-id tool-event)
                                                                      (broadcast-ws-session! session-id "events" tool-event))))

                                                                (= event-type "tool_execution_end")
                                                                (let [tool-name (or (aget event "toolName") "tool")
                                                                      tool-call-id (or (aget event "toolCallId") (.randomUUID node-crypto))
                                                                      is-error (boolean (aget event "isError"))
                                                                      raw-result (or (aget event "result")
                                                                                     (aget event "toolResult")
                                                                                     (aget event "output"))
                                                                      content-parts (tool-result-content-parts raw-result)
                                                                      result-preview (or (preview-text-nonblank (aget event "result") 20000)
                                                                                         (preview-text-nonblank (aget event "toolResult") 20000)
                                                                                         (preview-text-nonblank (aget event "output") 20000))
                                                                      tool-event (tool-event-payload run-id conversation-id session-id "tool_end"
                                                                                                     {:status (if is-error "failed" "completed")
                                                                                                      :tool_name tool-name
                                                                                                      :tool_call_id tool-call-id
                                                                                                      :is_error is-error
                                                                                                      :preview result-preview})]
                                                                  (update-run-tool-receipt! run-id tool-call-id {:tool_name tool-name}
                                                                                            (fn [receipt]
                                                                                              (cond-> (merge receipt {:tool_name tool-name
                                                                                                                      :status (if is-error "failed" "completed")
                                                                                                                      :ended_at (now-iso)
                                                                                                                      :is_error is-error})
                                                                                                result-preview (assoc :result_preview result-preview)
                                                                                                (seq content-parts) (assoc :content_parts content-parts))))
                                                                  (apply-run-tool-trace-event! run-id {:type "tool_end"
                                                                                                       :tool_name tool-name
                                                                                                       :tool_call_id tool-call-id
                                                                                                       :preview result-preview
                                                                                                       :is_error is-error
                                                                                                       :at (now-iso)})
                                                                  (append-run-event! run-id tool-event)
                                                                  (broadcast-ws-session! session-id "events" tool-event))

                                                                (= event-type "turn_end")
                                                                (let [tool-results (or (aget event "toolResults") #js [])
                                                                      turn-event (tool-event-payload run-id conversation-id session-id "turn_end"
                                                                                                     {:status "completed"
                                                                                                      :tool_result_count (or (.-length tool-results) 0)})]
                                                                  (append-run-event! run-id turn-event)
                                                                  (broadcast-ws-session! session-id "events" turn-event))

                                                                (= event-type "agent_end")
                                                                (broadcast-ws-session! session-id "events"
                                                                                       (tool-event-payload run-id conversation-id session-id "agent_end"
                                                                                                           {:status "completed"}))))))]
                                ;; Use multimodal message builder if content-parts are present
                                (let [prompt-promise (.prompt session
                                                              (if (seq prompt-content-parts)
                                                                (build-agent-multimodal-message message prompt-content-parts hydration memory-hydration)
                                                                (build-agent-user-message message hydration memory-hydration)))]
                                  (.catch
                                   (.then prompt-promise
                                          (fn []
                                            (unsubscribe)
                                            (turn-control/unregister-active-turn! conversation-id run-id)
                                            (let [assistant-message (latest-assistant-message session)
                                                  answer (let [chunked (apply str @chunks)]
                                                           (if (str/blank? chunked)
                                                             (assistant-message-text assistant-message)
                                                             chunked))
                                                  assistant-content-parts (assistant-content-parts assistant-message)
                                                  usage (or (aget assistant-message "usage") #js {})
                                                  reasoning-text (let [streamed (apply str @reasoning-chunks)
                                                                       final-reasoning (assistant-message-reasoning-text assistant-message)]
                                                                   (cond
                                                                     (and (str/blank? streamed) (not (str/blank? final-reasoning))) final-reasoning
                                                                     (and (not (str/blank? final-reasoning)) (> (count final-reasoning) (count streamed))) final-reasoning
                                                                     :else streamed))
                                                  elapsed (- (.now js/Date) started-ms)
                                                  output-tokens (or (aget usage "output") 0)
                                                  tokens-per-second (if (and (pos? output-tokens) (pos? elapsed))
                                                                      (* 1000 (/ output-tokens elapsed))
                                                                      nil)
                                                  sources (hydration-sources hydration)
                                                  message-parts (cond-> []
                                                               (not (str/blank? reasoning-text))
                                                               (conj {:role "thinking"
                                                                      :content reasoning-text
                                                                      :reasoningType "reasoning_summary"})
                                                               (not (str/blank? answer))
                                                               (conj {:role "assistant"
                                                                      :content answer}))
                                                  completed-event (tool-event-payload run-id conversation-id session-id "run_completed"
                                                                                      {:status "completed"
                                                                                       :model model-id
                                                                                       :sources_count (count sources)})]
                                              (when (= mode "rag")
                                                (record-retrieval-sample! (:retrievalMode @settings-state*) elapsed))
                                              (finalize-run-trace-blocks! run-id "done")
                                              (let [completed-run (update-run! run-id
                                                                               (fn [run]
                                                                                 (let [resource-patch (cond-> {:sources sources}
                                                                                                        hydration (assoc :passiveHydration (select-keys hydration [:query :tokens :database :elapsedMs :results]))
                                                                                                        memory-hydration (assoc :memoryHydration (select-keys memory-hydration [:query :mode :hits :elapsedMs :conversationId])))
                                                                                       merged-content-parts (merge-content-parts assistant-content-parts
                                                                                                                                (reply-attachment-content-parts (:tool_receipts run)))]
                                                                                   (-> run
                                                                                       (assoc :updated_at (now-iso)
                                                                                              :status "completed"
                                                                                              :total_time_ms elapsed
                                                                                              :input_tokens (or (aget usage "input") 0)
                                                                                              :output_tokens output-tokens
                                                                                              :tokens_per_s tokens-per-second
                                                                                              :answer answer
                                                                                              :content_parts merged-content-parts
                                                                                              :reasoning reasoning-text
                                                                                              :sources sources)
                                                                                       (update :resources merge resource-patch)))))
                                                    merged-content-parts (vec (or (:content_parts completed-run) assistant-content-parts))
                                                    response {:answer answer
                                                              :run_id run-id
                                                              :runId run-id
                                                              :conversation_id conversation-id
                                                              :conversationId conversation-id
                                                              :session_id session-id
                                                              :model model-id
                                                              :content_parts merged-content-parts
                                                              :sources sources
                                                              :message_parts message-parts
                                                              :compare nil}
                                                    _ (when completed-run
                                                        (index-run-memory! config completed-run))]
                                                (append-run-event! run-id completed-event)
                                                (broadcast-ws-session! session-id "events" completed-event)
                                                ;; Mark session as completed in Redis
                                                (let [final-messages (transcript-after-turn session
                                                                                           (conj persisted-request-messages
                                                                                                 (cond-> {:role "assistant"
                                                                                                          :content answer}
                                                                                                   (seq merged-content-parts) (assoc :content-parts merged-content-parts))))]
                                                  (session-store/complete-session! (redis/get-client)
                                                                                   session-id
                                                                                   conversation-id
                                                                                   {:status "completed"
                                                                                    :answer answer
                                                                                    :messages final-messages}))
                                                ;; Remove from in-memory cache to prevent stale isStreaming
                                                (remove-agent-session! conversation-id)
                                                response)))
                                   (fn [err]
                                     (unsubscribe)
                                     (turn-control/unregister-active-turn! conversation-id run-id)
                                     (let [err-text (or @abort-reason* (str err))
                                           error-event (tool-event-payload run-id conversation-id session-id "run_failed"
                                                                           {:status "failed"
                                                                            :error err-text})]
                                       (finalize-run-trace-blocks! run-id "error")
                                       (let [failed-run (update-run! run-id
                                                                     (fn [run]
                                                                       (let [resource-patch (cond-> {}
                                                                                              hydration (assoc :passiveHydration (select-keys hydration [:query :tokens :database :elapsedMs :results]))
                                                                                              memory-hydration (assoc :memoryHydration (select-keys memory-hydration [:query :mode :hits :elapsedMs :conversationId])))]
                                                                         (-> run
                                                                             (assoc :updated_at (now-iso)
                                                                                    :status "failed"
                                                                                    :total_time_ms (- (.now js/Date) started-ms)
                                                                                    :reasoning (apply str @reasoning-chunks)
                                                                                    :error err-text)
                                                                             (update :resources merge resource-patch)))))
                                             _ (when failed-run
                                                 (index-run-memory! config failed-run))]
                                         (append-run-event! run-id error-event)
                                        (broadcast-ws-session! session-id "events" error-event)
                                        ;; Mark session as failed in Redis
                                        (let [final-messages (transcript-after-turn session persisted-request-messages)]
                                         (session-store/complete-session! (redis/get-client)
                                                                           session-id
                                                                           conversation-id
                                                                           {:status "failed"
                                                                            :error err-text
                                                                            :messages final-messages}))
                                         ;; Remove from in-memory cache to prevent stale isStreaming
                                         (remove-agent-session! conversation-id))
                                     (throw err))))))))))))))))))))

(defn recovered-auth-context
  [session]
  {:orgId (:org_id session)
   :orgSlug (:org_slug session)
   :userId (:user_id session)
   :userEmail (:user_email session)
   :membershipId (:membership_id session)
   :roleSlugs (vec (or (:role_slugs session) []))
   :permissions (vec (or (:permissions session) []))
   :toolPolicies (vec (or (:tool_policies session) []))
   :membershipToolPolicies (vec (or (:membership_tool_policies session) []))
   :isSystemAdmin (boolean (:is_system_admin session))})

(defn recovered-agent-spec
  [session]
  (:agent_spec session))

(defn restore-recovered-conversation-access!
  [session]
  (let [conversation-id (str (or (:conversation_id session) ""))
        snapshot (select-keys session [:org_id
                                       :org_slug
                                       :user_id
                                       :user_email
                                       :membership_id
                                       :role_slugs
                                       :permissions
                                       :tool_policies
                                       :membership_tool_policies
                                       :is_system_admin])]
    (when (and (not (str/blank? conversation-id))
               (auth-snapshot-has-principal? snapshot))
      (swap! conversation-access* assoc conversation-id snapshot))))

(defn last-session-user-message
  [session]
  (some (fn [message]
          (let [role (some-> (:role message) str str/lower-case)
                content (some-> (:content message) str)]
            (when (and (= role "user")
                       (not (str/blank? content)))
              content)))
        (reverse (vec (or (:messages session) [])))))

(defn- wait-for-recovered-turn-kickoff!
  [conversation-id launch-promise]
  (if (some? (turn-control/active-turn conversation-id))
    (js/Promise.resolve true)
    (js/Promise.
     (fn [resolve reject]
       (let [done? (atom false)
             started-ms (.now js/Date)
             check! (fn check! []
                      (cond
                        @done? nil
                        (some? (turn-control/active-turn conversation-id))
                        (do
                          (reset! done? true)
                          (resolve true))

                        (> (- (.now js/Date) started-ms) RECOVERED-SESSION-KICKOFF-TIMEOUT-MS)
                        (do
                          (reset! done? true)
                          (reject (js/Error. (str "Timed out waiting for recovered session kickoff: " conversation-id))))

                        :else
                        (js/setTimeout check! RECOVERED-SESSION-KICKOFF-POLL-MS)))]
         (.catch launch-promise
                 (fn [err]
                   (when-not @done?
                     (reset! done? true)
                     (reject err))))
         (check!))))))

(defn resume-recovered-session!
  ([runtime config session]
   (resume-recovered-session! runtime config session nil))
  ([runtime config session opts]
   (let [conversation-id (str (or (:conversation_id session) ""))
         session-id (str (or (:session_id session) ""))
         run-id (or (:run_id session) nil)
         model-id (or (:model session) nil)
         mode (or (:mode session) "direct")
         wait-for (or (:wait-for opts) :completion)
         thinking-level (or (:thinking_level session)
                            (:agent-thinking-level config)
                            "off")
         auth-context (recovered-auth-context session)
         agent-spec (recovered-agent-spec session)
         message (last-session-user-message session)
         resume-failed! (fn [err]
                          (js/console.error "[knoxx] failed to resume recovered session"
                                            #js {:sessionId session-id
                                                 :conversationId conversation-id
                                                 :error (str err)})
                          (-> (session-store/complete-session! (redis/get-client)
                                                               session-id
                                                               conversation-id
                                                               {:status "failed"
                                                                :error (str "Session recovery failed: " err)
                                                                :messages (:messages session)})
                              (.then (fn [_]
                                       {:session_id session-id
                                        :conversation_id conversation-id
                                        :resumed false
                                        :error (str err)}))))]
     (restore-recovered-conversation-access! session)
     (cond
       (or (str/blank? conversation-id)
           (str/blank? session-id))
       (js/Promise.resolve {:session_id session-id
                            :conversation_id conversation-id
                            :resumed false
                            :reason "missing session or conversation id"})

       (str/blank? message)
       (-> (ensure-agent-session! runtime config conversation-id model-id auth-context thinking-level session-id agent-spec)
           (.then (fn [_]
                    (-> (session-store/update-session! (redis/get-client) session-id
                                                      {:status "waiting_input"
                                                       :has_active_stream false
                                                       :recovered_at (now-iso)})
                        (.then (fn [_]
                                 {:session_id session-id
                                  :conversation_id conversation-id
                                  :resumed false
                                  :reason "no pending user message to resume"}))))))

       :else
       (-> (session-store/update-session! (redis/get-client) session-id
                                          {:status "running"
                                           :has_active_stream false
                                           :recovered_at (now-iso)})
           (.then (fn [_]
                    (let [send-promise (send-agent-turn! runtime config {:conversation-id conversation-id
                                                                         :session-id session-id
                                                                         :run-id run-id
                                                                         :message message
                                                                         :model model-id
                                                                         :mode mode
                                                                         :thinking-level thinking-level
                                                                         :auth-context auth-context
                                                                         :agent-spec agent-spec})]
                      (if (= wait-for :kickoff)
                        (do
                          (.catch send-promise
                                  (fn [err]
                                    (js/console.error "[knoxx] recovered session failed after kickoff"
                                                      #js {:sessionId session-id
                                                           :conversationId conversation-id
                                                           :error (str err)})
                                    nil))
                          (-> (wait-for-recovered-turn-kickoff! conversation-id send-promise)
                              (.then (fn [_]
                                       {:session_id session-id
                                        :conversation_id conversation-id
                                        :resumed true
                                        :wait_for "kickoff"}))
                              (.catch resume-failed!)))
                        (-> send-promise
                            (.then (fn [_]
                                     {:session_id session-id
                                      :conversation_id conversation-id
                                      :resumed true}))
                            (.catch resume-failed!)))))))))))

(defn recover-active-agent-sessions!
  [runtime config redis-client]
  (-> (session-store/recover-sessions! redis-client)
      (.then (fn [sessions]
               (let [items (vec sessions)]
                 (if (seq items)
                   (-> (.all js/Promise (clj->js (mapv #(resume-recovered-session! runtime config %) items)))
                       (.then (fn [results]
                                (vec (js->clj results :keywordize-keys true)))))
                   (js/Promise.resolve [])))))))
