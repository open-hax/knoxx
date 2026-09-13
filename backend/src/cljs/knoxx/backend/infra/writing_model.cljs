(ns knoxx.backend.infra.writing-model
  "Replaceable writing transports returning proposals without publication authority."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [knoxx.backend.extern.opencode-writing :as opencode]
            [knoxx.backend.extern.promise :as xpromise]
            [knoxx.backend.law.writing-model :as law]))

(defn prompt
  "Render only caller-supplied instruction, draft and accepted lessons."
  [{:keys [instruction content lessons] :as input}]
  (law/require-input! input)
  (str "Instruction:\n" instruction "\n\nDraft:\n" content
       "\n\nAccepted lessons:\n" (pr-str (or lessons []))))

(defn- provider [config]
  (keyword (or (:wiki-model-provider config) :openai-compatible)))

(defmulti complete!
  "Complete a tool-free textual request through the explicitly selected provider."
  (fn [config _request] (provider config)))

(defmethod complete! :opencode [config request]
  (opencode/complete! config request))

(defn- openai-result! [model response]
  (let [body (:body response)
        choices (:choices body)
        choice (first choices)
        message (:message choice)]
    (when-not (and (true? (:ok response)) (= 200 (:status response))
                  (map? body) (not (contains? body :error))
                  (= model (:model body)) (= 1 (count choices))
                  (= "stop" (:finish_reason choice))
                  (= "assistant" (:role message))
                  (not (seq (:tool_calls message))) (nil? (:function_call message)))
      (throw (law/failure "writing_model_invalid_output" "Writing provider returned an incomplete or invalid response")))
    {:content (law/require-content! (:content message)) :model model
     :provider :openai-compatible :usage (:usage body)}))

(defn- ^:async request-openai! [model request]
  (try
    (openai-result! model
                    (await (xpromise/with-timeout-error
                            (xfetch/json! xfetch/default-client request)
                            (:timeout-ms request)
                            (ex-info "Writing model completion timed out"
                                     {:status 504 :code "writing_model_timeout"}))))
    (catch :default error
      (if (ex-data error) (throw error)
          (throw (law/failure "writing_model_transport_failed" "Writing model request failed"))))))

(defn- timeout! [config request-timeout]
  (let [timeout (or request-timeout (:wiki-model-timeout-ms config) 180000)]
    (when-not (and (integer? timeout) (pos? timeout) (<= timeout 180000))
      (throw (law/failure "writing_model_configuration_invalid" "Writing timeout must be between 1 and 180000 milliseconds")))
    timeout))

(defmethod complete! :openai-compatible
  [config {:keys [model system-prompt prompt timeout-ms]}]
  (let [base (:wiki-model-base-url config)]
    (when-not (and (string? base) (not (str/blank? base))
                  (string? model) (not (str/blank? model)))
      (throw (ex-info "Writing model endpoint and model are required"
                      {:status 503 :code "writing_model_unconfigured"})))
    (let [request {:url (str (str/replace base #"/+$" "") "/chat/completions")
                   :timeout-ms (timeout! config timeout-ms)
                   :opts {:method "POST"
                          :headers (cond-> {"Content-Type" "application/json"}
                                     (seq (:wiki-model-api-key config))
                                     (assoc "Authorization" (str "Bearer " (:wiki-model-api-key config))))
                          :json {:model model :stream false
                                 :messages [{:role "system" :content system-prompt}
                                            {:role "user" :content prompt}]}}}]
      (request-openai! model request))))

(defmethod complete! :default [_config _request]
  (throw (ex-info "Writing model provider is not supported"
                  {:status 503 :code "writing_model_provider_unsupported"})))

(defn generate!
  "Generate a bounded writing proposal from explicit draft context."
  [config input]
  (complete! config {:model (:wiki-model config)
                     :system-prompt "Work only with the supplied draft and accepted lessons. Return a writing proposal. Do not use tools or claim to save or publish content."
                     :prompt (prompt input)
                     :timeout-ms (:wiki-model-timeout-ms config)}))
