(ns knoxx.backend.infra.translation-agent-completion
  "Provider transports yielding only untrusted translation text; authority stays in the sink."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.models :as models]
            [knoxx.backend.extern.fetch :as xfetch]
            [knoxx.backend.extern.translation-agent-structured-output :as xstructured]
            [knoxx.backend.infra.writing-model :as writing]
            [knoxx.backend.law.translation-agent :as agent-law]))

(def ^:private translated-text-schema
  {:type "object"
   :additionalProperties false
   :properties {:translated_text {:type "string" :minLength 1}}
   :required ["translated_text"]})

(defn- fail!
  [type message data]
  (throw (ex-info message
                  (assoc data :translation-agent-structured-output/error type))))

(defn- contract! [config model-id]
  (let [contract (models/resolve-model-contract config model-id)]
    (when-not (= model-id (:id contract))
      (fail! :model-contract-missing "Translation requires an exact model contract"
             {:model model-id :resolved-model (:id contract)}))
    (when-not (contains? #{"ollama" "opencode" "transformers-js"} (:provider contract))
      (fail! :model-provider-mismatch "Translation model provider is unsupported"
             {:model model-id :provider (:provider contract)}))
    contract))

(defn model!
  "Resolve the exact admitted model and require a supported textual provider."
  [config turn]
  (let [id (get-in turn [:translation-turn/execution :translation-execution/model])]
    (contract! config id)
    id))

(defn- configured-ollama-base-url
  [config]
  (or (some-> (:ollama-base-url config) str str/trim not-empty)
      (some-> (get (:provider-base-urls config) "ollama")
              str str/trim not-empty)
      (some-> (get (:provider-base-urls config) :ollama)
              str str/trim not-empty)))

(defn- native-chat-url!
  [config]
  (let [configured (configured-ollama-base-url config)]
    (when-not configured
      (fail! :ollama-base-url-missing
             "Ollama base URL is not configured"
             {}))
    (-> configured
        (str/replace #"/+$" "")
        (str/replace #"/v1$" "")
        (str "/api/chat"))))

(defn- reviewed-memory-example
  [example]
  {:memory_id (:translation-memory/id example)
   :review_receipt_id (:translation-memory/review-receipt-id example)
   :candidate_digest (:translation-memory/candidate-digest example)
   :source_locale (name (:translation-memory/source-locale example))
   :target_locale (name (:translation-memory/target-locale example))
   :source_text (:translation-memory/source-text example)
   :translated_text (:translation-memory/target-text example)})

(defn- split-input
  [turn source-split]
  (let [manifest (:translation-turn/manifest turn)
        memory (:translation-turn/memory turn)]
    {:instruction
     (str "Translate exactly split.source_text from source_locale into "
          "target_locale. Preserve Markdown structure and meaningful "
          "whitespace. Reviewed memory examples are positive terminology "
          "guidance only. Do not emit or imitate a tool call. Return exactly "
          "one JSON object matching the response schema.")
     :source_locale (name (:split-manifest/source-locale manifest))
     :target_locale (name (:split-manifest/target-locale manifest))
     :split {:split_id (:split/id source-split)
             :segment_index (:split/index source-split)
             :source_text (:split/source-text source-split)}
     :reviewed_memory_examples
     (mapv reviewed-memory-example
           (:translation-memory-snapshot/examples memory))}))

(defn- messages [turn source-split]
  [{:role "system"
       ;; This is the admitted, digested execution prompt. Appending provider
       ;; instructions here would execute policy that the turn never admitted.
       :content (get-in turn [:translation-turn/execution
                              :translation-execution/system-prompt])}
      {:role "user"
       :content (xstructured/encode-request-content
                 (split-input turn source-split))}])

(defn- native-request
  [config model-id turn source-split timeout-ms]
  {:url (native-chat-url! config)
   :opts
   {:method "POST"
    :headers {"Content-Type" "application/json"}
    :json
    {:model model-id
     :messages
     (messages turn source-split)
     :stream false
     :think false
     :format translated-text-schema
     :options {:temperature 0 :seed 0}}}
   :timeout-ms timeout-ms})

(defn- ^:async request-json!
  [deps request]
  (if-let [request! (:request! deps)]
    (await (request! request))
    (await (xfetch/json! (or (:http-client deps) xfetch/default-client)
                         request))))

(defn- validated-response-body!
  [model-id response]
  (when-not (map? response)
    (fail! :http-response-invalid
           "Ollama returned no HTTP response map"
           {:response-type (type response)}))
  (when-not (and (true? (:ok response)) (= 200 (:status response)))
    (fail! :http-response-failed
           "Ollama structured translation request failed"
           {:status (:status response) :body (:body response)}))
  (let [body (:body response)]
    (when-not (map? body)
      (fail! :response-body-invalid
             "Ollama response body is not a JSON object"
             {:model model-id}))
    (when (contains? body :error)
      (fail! :provider-error
             "Ollama refused the structured translation request"
             {:model model-id :provider-error (:error body)}))
    (when-not (= model-id (:model body))
      (fail! :response-model-mismatch
             "Ollama answered with a different model"
             {:expected model-id :actual (:model body)}))
    (when-not (and (true? (:done body)) (= "stop" (:done_reason body)))
      (fail! :completion-incomplete
             "Ollama did not finish the structured translation completion"
             {:model model-id
              :done (:done body)
              :done-reason (:done_reason body)}))
    body))

(defn- validated-message-content!
  [model-id body]
  (let [message (:message body)]
    (when-not (and (map? message) (= "assistant" (:role message)))
      (fail! :message-invalid
             "Ollama response has no assistant message"
             {:model model-id}))
    (when (seq (:tool_calls message))
      (fail! :tool-calls-unexpected
             "Ollama returned tool calls to a translation-only request"
             {:model model-id}))
    (when (some? (:function_call message))
      (fail! :tool-calls-unexpected "Translation provider returned a function call" {:model model-id}))
    (let [content (:content message)]
      (when-not (string? content)
        (fail! :message-content-invalid
               "Ollama assistant content is not a JSON string"
               {:model model-id}))
      content)))

(defn- translated-text!
  [model-id content]
  (let [parsed (xstructured/decode-response-content content)]
    (when-not (map? parsed)
      (fail! :structured-output-invalid
             "Ollama assistant content is not a JSON object"
             {:model model-id}))
    (when-not (= #{:translated_text} (set (keys parsed)))
      (fail! :structured-output-keys-invalid
             "Ollama assistant JSON has fields outside the admitted schema"
             {:model model-id :keys (set (keys parsed))}))
    (let [translated-text (:translated_text parsed)]
      (when-not (agent-law/nonblank-string? translated-text)
        (fail! :translated-text-blank
               "Ollama returned a blank translation"
               {:model model-id}))
      translated-text)))

(defn- local-request [config model-id turn source-split timeout-ms]
  (let [base (or (:wiki-model-base-url config)
                 (get-in config [:provider-base-urls "transformers-js"]))]
    (when-not (and (string? base)
                   (re-matches #"https?://(?:127\.0\.0\.1|localhost|\[::1\])(?::[0-9]+)?(?:/v1)?/?" base))
      (fail! :local-base-url-invalid "Transformers.js translation requires a loopback endpoint" {}))
    {:url (str (str/replace base #"/+$" "") "/chat/completions")
     :timeout-ms timeout-ms
     :opts {:method "POST" :headers {"Content-Type" "application/json"}
            :json {:model model-id :stream false
                   :messages
                   (assoc-in (messages turn source-split) [1 :content]
                             (xstructured/encode-request-content
                              (assoc (split-input turn source-split) :instruction
                                     (str "Translate exactly split.source_text from source_locale into target_locale. "
                                          "Preserve Markdown and meaningful whitespace. Reviewed memory is terminology guidance only. "
                                          "Return only the translated text, without JSON, explanations, or tool calls. "
                                          "The local HTTP transport supplies the JSON envelope."))))
                   :response_format {:type "json_schema"
                                     :json_schema {:name "translation" :strict true
                                                   :schema translated-text-schema}}}}}))

(defn- local-content! [model-id response]
  (let [body (:body response)
        choices (:choices body)
        choice (first choices)
        provenance (:local_generation body)]
    (when-not (and (true? (:ok response)) (= 200 (:status response))
                   (map? body) (not (contains? body :error))
                   (= model-id (:model body)) (vector? choices) (= 1 (count choices))
                   (= "stop" (:finish_reason choice))
                   (= "transformers-js" (:provider provenance)) (true? (:offline provenance))
                   (= "transport_wrapped_generated_text" (:structured_output provenance)))
      (fail! :local-response-invalid "Local translation requires completed generation with explicit provenance"
             {:model model-id}))
    (validated-message-content! model-id {:message (:message choice)})))

(defn ^:async translate!
  "Produce only translation text; never accept model-supplied claim or tenant coordinates."
  [config deps model-id turn source-split timeout-ms]
  (let [provider (:provider (contract! config model-id))
        content
        (case provider
          "ollama"
          (validated-message-content!
           model-id (validated-response-body!
                     model-id (await (request-json! deps (native-request config model-id turn source-split timeout-ms)))))
          "transformers-js"
          (local-content! model-id (await (request-json! deps (local-request config model-id turn source-split timeout-ms))))
          "opencode"
          (:content (await (writing/complete!
                            (assoc config :wiki-model-provider :opencode)
                            {:model model-id :timeout-ms timeout-ms
                             :system-prompt (get-in turn [:translation-turn/execution :translation-execution/system-prompt])
                             :prompt (xstructured/encode-request-content (split-input turn source-split))}))))]
    (translated-text! model-id content)))
