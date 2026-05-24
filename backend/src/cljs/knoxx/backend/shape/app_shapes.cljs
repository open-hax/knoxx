(ns knoxx.backend.shape.app-shapes
  (:require [clojure.string :as str]))

(def ^:private media-extension-pattern #".*\.(?:png|jpg|jpeg|gif|webp|mp4|webm|mp3|wav|ogg|m4a|flac|pdf)(?:\?.*)?$")

(defn- body-value
  [body & names]
  (some (fn [field-name]
          (if (map? body)
            (or (get body (keyword field-name))
                (get body field-name))
            (aget body field-name)))
        names))

(defn- maybe-cljs
  [value]
  (cond
    (nil? value) nil
    (map? value) value
    (vector? value) value
    :else (js->clj value :keywordize-keys true)))

(defn- maybe-cljs-map
  [value]
  (when-let [value (maybe-cljs value)]
    (when (map? value) value)))

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

(defn- memory-hydration-spec
  [spec]
  (or (:memory_hydration spec)
      (:memory-hydration spec)
      (:memoryHydration spec)
      (get-in spec [:memory :passive-hydration])
      (get-in spec [:memory :passiveHydration])))

(defn- context-policy-spec
  [spec]
  (or (:context_policy spec)
      (:context-policy spec)
      (:contextPolicy spec)
      (:context spec)))

(defn- normalize-agent-spec
  [value]
  (let [spec (maybe-cljs-map value)
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
                              (:resourcePolicies spec))
        sources (or (:sources spec)
                    (:runtime_sources spec)
                    (:runtime-sources spec)
                    (:runtimeSources spec))
        memory-hydration (memory-hydration-spec spec)
        context-policy (context-policy-spec spec)]
    (when (or contract-id actor-id role system-prompt task-prompt model thinking-level (seq tool-policies) resource-policies (seq sources) memory-hydration context-policy)
      {:contract-id contract-id
       :actor-id actor-id
       :role role
       :system-prompt system-prompt
       :task-prompt task-prompt
       :model model
       :thinking-level thinking-level
       :tool-policies tool-policies
       :resource-policies resource-policies
       :sources sources
       :memory-hydration memory-hydration
       :context-policy context-policy})))

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
  (let [parts (maybe-cljs value)]
    (cond
      (sequential? parts) (vec (keep normalize-content-part parts))
      (map? parts) (vec (keep normalize-content-part [parts]))
      :else [])))

(defn normalize-chat-body
  [body]
  (let [message (or (body-value body "message") "")
        raw-content-parts (normalize-content-parts (body-value body "contentParts"
                                                               "content_parts"
                                                               "content-parts"))
        content-parts raw-content-parts]
    {:message message
     :conversation-id (body-value body "conversationId"
                                  "conversation_id"
                                  "conversation-id")
     :session-id (body-value body "sessionId"
                             "session_id"
                             "session-id")
     :run-id (body-value body "runId"
                         "run_id"
                         "run-id")
     :model (body-value body "model")
     :thinking-level (body-value body "thinkingLevel"
                                 "thinking_level"
                                 "thinking-level"
                                 "reasoningEffort"
                                 "reasoning_effort"
                                 "reasoning-effort")
     :content-parts content-parts
     :template-context (maybe-cljs-map (body-value body "templateContext"
                                                   "template_context"
                                                   "template-context"))
     :mode (or (body-value body "mode") "direct")
     :agent-spec (normalize-agent-spec (body-value body "agentSpec"
                                                   "agent_spec"
                                                   "agent-spec"))
     :auth-context (maybe-cljs-map (body-value body "authContext"
                                               "auth_context"
                                               "auth-context"))}))

(defn normalize-control-body
  [body]
  (let [metadata (or (body-value body "metadata")
                     (body-value body "lineage")
                     {})]
    {:message (or (body-value body "message") "")
     :conversation-id (body-value body "conversationId"
                                  "conversation_id"
                                  "conversation-id")
     :session-id (body-value body "sessionId"
                             "session_id"
                             "session-id")
     :run-id (body-value body "runId"
                         "run_id"
                         "run-id")
     :actor-id (some-> (body-value body "actorId"
                                   "actor_id"
                                   "actor-id")
                       str
                       str/trim
                       not-empty)
     :metadata (or (maybe-cljs-map metadata) {})}))

(defn route!
  "Register a Fastify route. handler-or-opts may be either:
   - a plain function  → classic-mode (no preHandlers)
   - a JS options obj  → preHandler-mode from defroute macro;
     its keys are merged into the base route options so that
     @fastify/websocket receives a proper :handler fn, not a
     nested object (which causes `handler.call is not a function`)."
  [app method url handler-or-opts]
  (if (fn? handler-or-opts)
    (.route app #js {:method method
                     :url    url
                     :handler handler-or-opts})
    (.route app (.assign js/Object
                         #js {:method method :url url}
                         handler-or-opts))))
