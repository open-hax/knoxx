(ns knoxx.backend.shape.agent.message
  "Schemas for agent-facing messages and prompt template context. These shapes
   describe Knoxx-normalized maps, not provider-specific wire payloads."
  (:require [malli.core :as m]
            [malli.error :as me]))

(def MessageRole
  [:enum "system" "user" "assistant" "tool" "thinking" "compactionSummary"])

(def ContentPartType
  [:enum :text :image :audio :video :document
   "text" "image" "image_url" "audio" "video" "document" "file"])

(def ContentPart
  [:map {:closed false}
   [:type ContentPartType]
   [:text {:optional true} :string]
   [:url {:optional true} :string]
   [:data {:optional true} :string]
   [:mimeType {:optional true} :string]
   [:filename {:optional true} :string]
   [:size {:optional true} number?]])

(def EtaMuAttachment
  [:map {:closed false}
   [:type [:or :string ContentPartType]]
   [:data :string]
   [:mimeType :string]
   [:filename {:optional true} :string]])

(def MessageUsage
  [:map {:closed false}
   [:input-tokens {:optional true} int?]
   [:output-tokens {:optional true} int?]
   [:total-tokens {:optional true} int?]])

(def StoredMessage
  [:map {:closed false}
   [:role MessageRole]
   [:content {:optional true} [:maybe :string]]
   [:summary {:optional true} [:maybe :string]]
   [:content-parts {:optional true} [:vector ContentPart]]
   [:contentParts {:optional true} [:vector ContentPart]]
   [:usage {:optional true} MessageUsage]
   [:tokensBefore {:optional true} int?]])

(def TemplateContext
  [:map {:closed false}])

(def AgentMessage
  [:map {:closed false}
   [:role MessageRole]
   [:content {:optional true} [:or :string [:vector ContentPart]]]
   [:content-parts {:optional true} [:vector ContentPart]]
   [:name {:optional true} :string]
   [:tool_call_id {:optional true} :string]
   [:metadata {:optional true} [:map {:closed false}]]])

(def ProviderAgentMessage
  [:or
   AgentMessage
   [:map {:closed false}
    ["role" {:optional true} :string]
    ["content" {:optional true} [:or :string [:vector ContentPart] [:vector EtaMuAttachment]]]
    ["text" {:optional true} [:maybe :string]]
    ["summary" {:optional true} [:maybe :string]]
    ["usage" {:optional true} [:map {:closed false}]]
    ["reasoning" {:optional true} [:maybe :string]]]])

(def TranscriptMessage
  [:map {:closed false}
   [:role MessageRole]
   [:content :string]
   [:at {:optional true} [:or :string int?]]
   [:id {:optional true} :string]
   [:name {:optional true} :string]
   [:metadata {:optional true} [:map {:closed false}]]])

(defn valid-agent-message?
  [value]
  (m/validate AgentMessage value))

(defn explain-agent-message
  [value]
  (me/humanize (m/explain AgentMessage value)))
