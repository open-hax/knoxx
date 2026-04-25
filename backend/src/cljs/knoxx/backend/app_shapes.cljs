(ns knoxx.backend.app-shapes
  (:require [clojure.string :as str]))

(def ^:private media-extension-pattern #".*\.(?:png|jpg|jpeg|gif|webp|mp4|webm|mp3|wav|ogg|m4a|flac|pdf)(?:\?.*)?$")

(defn- extract-media-urls
  "Extract media URLs from text content."
  [text]
  (when (string? text)
    (->> (str/split text #"\s+")
         (keep (fn [token]
                 (let [lower (str/lower-case token)]
                   (when (or (re-matches media-extension-pattern lower)
                             (some #(str/includes? lower %) [".png" ".jpg" ".jpeg" ".gif" ".webp" ".mp4" ".webm" ".mp3" ".wav" ".ogg" ".m4a" ".flac" ".pdf"])
                             (str/includes? token "cdn.discordapp.com"))
                     {:url token
                      :type (cond
                              (some #(str/includes? lower %) [".png" ".jpg" ".jpeg" ".gif" ".webp"]) "image"
                              (some #(str/includes? lower %) [".mp4" ".webm" ".mov"]) "video"
                              (some #(str/includes? lower %) [".mp3" ".wav" ".ogg" ".m4a" ".flac"]) "audio"
                              (some #(str/includes? lower %) [".pdf"]) "document"
                              :else "image")}))))
         vec)))

(defn- auto-detect-content-parts
  "Auto-detect media URLs in message text and add them as content parts."
  [message content-parts]
  (let [extracted (when (string? message) (extract-media-urls message))
        existing-urls (->> (or content-parts [])
                          (map :url)
                          (remove nil?)
                          set)
        new-parts (->> extracted
                       (remove #(existing-urls (:url %)))
                       vec)]
    (if (seq new-parts)
      (vec (concat content-parts new-parts))
      content-parts)))

(defn- normalize-tool-policy
  [policy]
  (let [tool-id (some-> (or (:toolId policy)
                            (:tool-id policy)
                            (:tool_id policy))
                        str
                        str/trim
                        not-empty)
        effect (some-> (or (:effect policy) "allow")
                       str
                       str/trim
                       str/lower-case
                       not-empty)]
    (when tool-id
      {:toolId tool-id
       :effect (if (#{"allow" "deny"} effect)
                 effect
                 "allow")})))

(defn- normalize-tool-policies
  [policies]
  (vec (keep normalize-tool-policy (or policies []))))

(defn- normalize-agent-spec
  [value]
  (let [spec (some-> value (js->clj :keywordize-keys true))
        contract-id (some-> (or (:contract_id spec)
                                (:contract-id spec)
                                (:contractId spec)
                                (:agent_id spec)
                                (:agent-id spec)
                                (:agentId spec))
                            str
                            str/trim
                            not-empty)
        actor-id (some-> (or (:actor_id spec)
                             (:actor-id spec)
                             (:actorId spec))
                         str
                         str/trim
                         not-empty)
        role (some-> (or (:role spec) (:role_slug spec) (:role-slug spec)) str str/trim not-empty)
        system-prompt (some-> (or (:system_prompt spec)
                                  (:system-prompt spec)
                                  (:systemPrompt spec))
                              str
                              not-empty)
        task-prompt (some-> (or (:task_prompt spec)
                                (:task-prompt spec)
                                (:taskPrompt spec))
                            str
                            not-empty)
        model (some-> (:model spec) str str/trim not-empty)
        thinking-level (some-> (or (:thinking_level spec)
                                   (:thinking-level spec)
                                   (:thinkingLevel spec)
                                   (:reasoning_effort spec)
                                   (:reasoning-effort spec)
                                   (:reasoningEffort spec))
                               str
                               str/trim
                               not-empty)
        tool-policies (normalize-tool-policies (or (:tool_policies spec)
                                                   (:tool-policies spec)
                                                   (:toolPolicies spec)))
        resource-policies (or (:resource_policies spec)
                              (:resource-policies spec)
                              (:resourcePolicies spec))]
    (when (or contract-id actor-id role system-prompt task-prompt model thinking-level (seq tool-policies) resource-policies)
      {:contract-id contract-id
       :actor-id actor-id
       :role role
       :system-prompt system-prompt
       :task-prompt task-prompt
       :model model
       :thinking-level thinking-level
       :tool-policies tool-policies
       :resource-policies resource-policies})))

(defn- normalize-content-part-type
  [value]
  (case (some-> value str str/trim str/lower-case)
    ("text" "input_text" "output_text" "refusal") :text
    ("image" "image_url" "input_image" "output_image") :image
    ("audio" "audio_url" "input_audio" "output_audio") :audio
    ("video" "video_url" "input_video" "output_video") :video
    ("document" "file" "input_file" "output_file") :document
    nil))

(defn- normalize-content-part
  [part]
  (cond
    (string? part)
    {:type :text
     :text part}

    (map? part)
    (let [type (normalize-content-part-type (or (:type part)
                                                (:partType part)
                                                (:part-type part)
                                                (:part_type part)))
          text (some-> (or (:text part)
                           (:refusal part))
                       str)
          url (some-> (or (:url part)
                          (:file_url part)
                          (:file-url part)
                          (:fileUrl part))
                      str)
          data (some-> (:data part) str)
          mime-type (some-> (or (:mimeType part)
                                (:mime_type part)
                                (:mime-type part)
                                (:mediaType part)
                                (:media_type part)
                                (:media-type part))
                          str)
          filename (some-> (:filename part) str)
          size (let [value (or (:size part)
                               (:bytes part)
                               (:byteSize part)
                               (:byte_size part)
                               (:byte-size part))]
                 (when (number? value) value))]
      (when (or type text url data filename)
        (cond-> {:type (or type :text)}
          (and (= (or type :text) :text) (some? text)) (assoc :text text)
          url (assoc :url url)
          data (assoc :data data)
          mime-type (assoc :mimeType mime-type)
          filename (assoc :filename filename)
          size (assoc :size size))))

    :else nil))

(defn- normalize-content-parts
  [value]
  (let [parts (some-> value (js->clj :keywordize-keys true))]
    (cond
      (sequential? parts) (vec (keep normalize-content-part parts))
      (map? parts) (vec (keep normalize-content-part [parts]))
      :else [])))

(defn normalize-chat-body
  [body]
  (let [message (or (aget body "message") "")
        raw-content-parts (normalize-content-parts (or (aget body "contentParts")
                                                      (aget body "content_parts")
                                                      (aget body "content-parts")))
        content-parts (auto-detect-content-parts message raw-content-parts)]
    {:message message
     :conversation-id (or (aget body "conversationId")
                         (aget body "conversation_id"))
     :session-id (or (aget body "sessionId")
                     (aget body "session_id"))
     :run-id (or (aget body "runId")
                 (aget body "run_id"))
     :model (or (aget body "model") nil)
     :thinking-level (or (aget body "thinkingLevel")
                         (aget body "thinking_level")
                         (aget body "reasoningEffort")
                         (aget body "reasoning_effort"))
     :content-parts content-parts
     :mode (or (aget body "mode") "direct")
     :agent-spec (normalize-agent-spec (or (aget body "agentSpec")
                                           (aget body "agent_spec")))
     :auth-context (some-> (or (aget body "authContext")
                               (aget body "auth_context"))
                           (js->clj :keywordize-keys true))}))

(defn normalize-control-body
  [body]
  {:message (or (aget body "message") "")
   :conversation-id (or (aget body "conversationId")
                        (aget body "conversation_id"))
   :session-id (or (aget body "sessionId")
                   (aget body "session_id"))
   :run-id (or (aget body "runId")
               (aget body "run_id"))})

(defn route!
  [app method url handler]
  (.route app #js {:method method
                   :url url
                   :handler handler}))
