(ns knoxx.backend.infra.agent.stream.provider-events
  "Provider JS stream event normalization. Downstream stream semantics should
   consume the canonical CLJS map returned by `normalize` rather than aget-ing
   provider objects directly."
  (:require [knoxx.backend.domain.agent.content :refer [preview-text-nonblank tool-result-content-parts]]
            [knoxx.backend.infra.agent.tools :refer [tool-call-input-preview]]))

(defn- js-present?
  [value]
  (and (some? value)
       (not= value js/undefined)))

(defn- js->data
  [value]
  (cond
    (not (js-present? value)) nil
    (string? value) value
    :else (try
            (js->clj value :keywordize-keys true)
            (catch :default _
              (str value)))))

(defn- first-js
  [& values]
  (first (filter js-present? values)))

(defn- tool-input-preview
  [tool-name raw-args args-by-key]
  (or (tool-call-input-preview tool-name (:params args-by-key))
      (tool-call-input-preview tool-name (:toolArgs args-by-key))
      (tool-call-input-preview tool-name (:args args-by-key))
      (tool-call-input-preview tool-name (:arguments args-by-key))
      (tool-call-input-preview tool-name (:input args-by-key))
      (tool-call-input-preview tool-name (:parameters args-by-key))
      (tool-call-input-preview tool-name raw-args)))

(defn- normalize-message-update
  [event]
  (let [assistant-event (aget event "assistantMessageEvent")
        assistant-event-type (some-> (aget assistant-event "type") str)
        delta (or (aget assistant-event "delta")
                  (aget assistant-event "text")
                  (aget assistant-event "reasoning")
                  (aget assistant-event "thinking")
                  "")]
    {:type "message_update"
     :raw event
     :assistant-message-event assistant-event
     :assistant-event-type assistant-event-type
     :delta (str (or delta ""))
     :partial-message (aget assistant-event "partial")
     :tool-call (aget assistant-event "toolCall")
     :message (aget event "message")}))

(defn- normalize-message-end
  [event]
  {:type "message_end"
   :raw event
   :message (aget event "message")})

(defn- normalize-tool-start
  [event]
  (let [tool-name (or (aget event "toolName") "tool")
        args-by-key {:params (aget event "params")
                     :toolArgs (aget event "toolArgs")
                     :args (aget event "args")
                     :arguments (aget event "arguments")
                     :input (aget event "input")
                     :parameters (aget event "parameters")}
        raw-args (first-js (:params args-by-key)
                           (:toolArgs args-by-key)
                           (:args args-by-key)
                           (:arguments args-by-key)
                           (:input args-by-key)
                           (:parameters args-by-key))]
    {:type "tool_execution_start"
     :raw event
     :tool-name tool-name
     :tool-call-id (aget event "toolCallId")
     :raw-args raw-args
     :input-raw (js->data raw-args)
     :input-preview (tool-input-preview tool-name raw-args args-by-key)}))

(defn- normalize-tool-update
  [event]
  {:type "tool_execution_update"
   :raw event
   :tool-name (or (aget event "toolName") "tool")
   :tool-call-id (aget event "toolCallId")
   :preview (or (preview-text-nonblank (aget event "delta") 400)
                (preview-text-nonblank (aget event "update") 400)
                (preview-text-nonblank (aget event "message") 400)
                (preview-text-nonblank (aget event "statusMessage") 400))})

(defn- normalize-tool-end
  [event]
  (let [raw-result (first-js (aget event "result")
                             (aget event "toolResult")
                             (aget event "output"))]
    {:type "tool_execution_end"
     :raw event
     :tool-name (or (aget event "toolName") "tool")
     :tool-call-id (aget event "toolCallId")
     :is-error (boolean (aget event "isError"))
     :raw-result raw-result
     :result-raw (js->data raw-result)
     :content-parts (tool-result-content-parts raw-result)
     :result-preview (or (preview-text-nonblank (aget event "result") 20000)
                         (preview-text-nonblank (aget event "toolResult") 20000)
                         (preview-text-nonblank (aget event "output") 20000))}))

(defn- normalize-turn-end
  [event]
  (let [tool-results (or (aget event "toolResults") #js [])]
    {:type "turn_end"
     :raw event
     :tool-results tool-results
     :tool-result-count (or (.-length tool-results) 0)}))

(defn normalize
  [event]
  (case (some-> (aget event "type") str)
    "message_update" (normalize-message-update event)
    "message_end" (normalize-message-end event)
    "tool_execution_start" (normalize-tool-start event)
    "tool_execution_update" (normalize-tool-update event)
    "tool_execution_end" (normalize-tool-end event)
    "turn_end" (normalize-turn-end event)
    "agent_end" {:type "agent_end" :raw event}
    {:type nil :raw event}))
