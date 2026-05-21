(ns knoxx.backend.shape.agent.runtime
  "Canonical schemas for agent runtime maps that cross route, session,
   provider, stream, sink, and persistence boundaries. Schemas are permissive at
   external edges and stricter for fields Knoxx consumes."
  (:require [knoxx.backend.shape.agent :as agent]
            [knoxx.backend.shape.agent.message :as message]
            [knoxx.backend.shape.agent.recovery :as recovery]
            [knoxx.backend.shape.agent.turn :as turn]
            [knoxx.backend.shape.session-persistence :as persistence]
            [malli.core :as m]
            [malli.error :as me]))

(def IsoOrMillis
  [:or :string int?])

(def AgentSpec agent/AgentSpec)
(def TurnRequest turn/AgentTurnInput)
(def ControlRequest agent/ControlBody)
(def RecoveryRequest recovery/RecoveryRequest)
(def RecoveryResult recovery/RecoveryResult)
(def ContentPart message/ContentPart)
(def StoredMessage message/StoredMessage)
(def ProviderAgentMessage message/ProviderAgentMessage)
(def EtaMuAttachment message/EtaMuAttachment)
(def ToolReceipt persistence/ToolReceipt)
(def TraceBlock persistence/TraceBlock)
(def KnoxxRun persistence/KnoxxRun)

(def DirectStartPayload
  [:map {:closed false}
   [:message {:optional true} [:maybe :string]]
   [:content-parts {:optional true} [:vector ContentPart]]
   [:contentParts {:optional true} [:vector ContentPart]]
   [:conversation-id {:optional true} [:maybe :string]]
   [:conversationId {:optional true} [:maybe :string]]
   [:session-id {:optional true} [:maybe :string]]
   [:sessionId {:optional true} [:maybe :string]]
   [:run-id {:optional true} [:maybe :string]]
   [:runId {:optional true} [:maybe :string]]
   [:model {:optional true} [:maybe :string]]
   [:mode {:optional true} [:maybe :string]]
   [:thinking-level {:optional true} [:maybe :string]]
   [:thinkingLevel {:optional true} [:maybe :string]]
   [:template-context {:optional true} [:map {:closed false}]]
   [:templateContext {:optional true} [:map {:closed false}]]
   [:agent-spec {:optional true} [:maybe AgentSpec]]
   [:agentSpec {:optional true} [:maybe [:map {:closed false}]]]
   [:auth-context {:optional true} [:maybe [:map {:closed false}]]]
   [:authContext {:optional true} [:maybe [:map {:closed false}]]]])

(def AuthContext
  [:map {:closed false}
   [:orgId {:optional true} [:maybe :string]]
   [:orgSlug {:optional true} [:maybe :string]]
   [:userId {:optional true} [:maybe :string]]
   [:userEmail {:optional true} [:maybe :string]]
   [:membershipId {:optional true} [:maybe :string]]
   [:actorId {:optional true} [:maybe :string]]
   [:roleSlugs {:optional true} [:vector :string]]
   [:permissions {:optional true} [:vector :string]]
   [:toolPolicies {:optional true} [:vector agent/ToolPolicy]]
   [:membershipToolPolicies {:optional true} [:vector agent/ToolPolicy]]
   [:isSystemAdmin {:optional true} :boolean]
   [:user {:optional true} [:map {:closed false}]]
   [:org {:optional true} [:map {:closed false}]]
   [:membership {:optional true} [:map {:closed false}]]])

(def ToolPolicy agent/ToolPolicy)

(def ContextPolicy
  [:map {:closed false}
   [:max-messages {:optional true} int?]
   [:maxMessages {:optional true} int?]
   [:max_messages {:optional true} int?]
   [:max-chars {:optional true} int?]
   [:maxChars {:optional true} int?]
   [:max_chars {:optional true} int?]
   [:preserve-system {:optional true} :boolean]
   [:preserveSystem {:optional true} :boolean]
   [:preserve_system {:optional true} :boolean]])

(def PolicyConstraints
  [:map {:closed false}
   [:allowedModels {:optional true} [:vector :string]]
   [:allowed-models {:optional true} [:vector :string]]
   [:models {:optional true} [:vector :string]]
   [:maxRequests {:optional true} int?]
   [:max-requests {:optional true} int?]
   [:windowSeconds {:optional true} int?]
   [:window-seconds {:optional true} int?]])

(def TemplateContext message/TemplateContext)

(def ActiveSessionEntry
  [:map {:closed false}
   [:session any?]
   [:model-id {:optional true} [:maybe :string]]
   [:tool-signature {:optional true} [:maybe :string]]
   [:session-id {:optional true} [:maybe :string]]
   [:actor-id {:optional true} [:maybe :string]]
   [:last-accessed int?]])

(def RuntimeSetup
  [:map {:closed false}
   [:auth-storage {:optional true} any?]
   [:model-registry {:optional true} any?]
   [:settings-manager {:optional true} any?]
   [:loader {:optional true} any?]
   [:runtime-dir {:optional true} [:maybe :string]]])

(def VisibleSessionSignature
  [:or
   :string
   [:map {:closed false}
    [:tools {:optional true} [:vector :string]]
    [:contract-id {:optional true} [:maybe :string]]
    [:actor-id {:optional true} [:maybe :string]]
    [:role {:optional true} [:maybe :string]]
    [:system-prompt {:optional true} [:maybe :string]]
    [:task-prompt {:optional true} [:maybe :string]]]])

(def StreamState
  [:map {:closed false}
   [:run-id :string]
   [:conversation-id :string]
   [:session-id :string]
   [:started-at IsoOrMillis]
   [:started-ms number?]
   [:random-uuid! fn?]
   [:chunks any?]
   [:reasoning-chunks any?]
   [:ttft-recorded? any?]
   [:last-assistant-text* any?]
   [:last-reasoning-text* any?]
   [:replay-suppression* any?]
   [:think-tag-mode* any?]
   [:aborting? any?]
   [:abort-reason* any?]
   [:tool-loop* any?]
   [:seen-tool-lifecycle-events* any?]])

(def ProviderStreamEventType
  [:enum "message_update" "message_end" "tool_execution_start"
   "tool_execution_update" "tool_execution_end" "turn_end" "agent_end"])

(def ProviderStreamEvent
  [:map {:closed false}
   [:type ProviderStreamEventType]
   [:assistantMessageEvent {:optional true} any?]
   [:message {:optional true} any?]
   [:toolName {:optional true} [:maybe :string]]
   [:toolCallId {:optional true} [:maybe :string]]
   [:isError {:optional true} :boolean]
   [:result {:optional true} any?]
   [:toolResult {:optional true} any?]
   [:output {:optional true} any?]
   [:toolResults {:optional true} any?]])

(def CanonicalRunEvent
  [:map {:closed false}
   [:run_id :string]
   [:conversation_id :string]
   [:session_id :string]
   [:type :string]
   [:at :string]
   [:status {:optional true} :string]
   [:tool_name {:optional true} [:maybe :string]]
   [:tool_call_id {:optional true} [:maybe :string]]
   [:preview {:optional true} [:maybe :string]]])

(def TokenEvent
  [:map {:closed false}
   [:run_id :string]
   [:conversation_id :string]
   [:session_id :string]
   [:kind [:enum "assistant_message" "reasoning"]]
   [:token :string]])

(def TurnResponse
  [:map {:closed false}
   [:answer {:optional true} [:maybe :string]]
   [:run_id {:optional true} [:maybe :string]]
   [:runId {:optional true} [:maybe :string]]
   [:conversation_id {:optional true} [:maybe :string]]
   [:conversationId {:optional true} [:maybe :string]]
   [:session_id {:optional true} [:maybe :string]]
   [:sessionId {:optional true} [:maybe :string]]
   [:model {:optional true} [:maybe :string]]
   [:content_parts {:optional true} [:vector ContentPart]]
   [:sources {:optional true} [:vector [:map {:closed false}]]]
   [:message_parts {:optional true} [:vector [:map {:closed false}]]]
   [:compare {:optional true} any?]])

(def AcceptedResponse
  [:map {:closed false}
   [:ok :boolean]
   [:queued :boolean]
   [:run_id {:optional true} [:maybe :string]]
   [:runId {:optional true} [:maybe :string]]
   [:conversation_id {:optional true} [:maybe :string]]
   [:conversationId {:optional true} [:maybe :string]]
   [:session_id {:optional true} [:maybe :string]]
   [:sessionId {:optional true} [:maybe :string]]
   [:model {:optional true} [:maybe :string]]])

(def SessionStoreRecord
  [:map {:closed false}
   [:session_id :string]
   [:conversation_id :string]
   [:run_id {:optional true} [:maybe :string]]
   [:status persistence/RunStatus]
   [:model {:optional true} [:maybe :string]]
   [:mode {:optional true} [:maybe :string]]
   [:thinking_level {:optional true} [:maybe :string]]
   [:created_at {:optional true} :string]
   [:updated_at {:optional true} :string]
   [:has_active_stream {:optional true} :boolean]
   [:messages {:optional true} [:vector StoredMessage]]
   [:auth_snapshot {:optional true} [:map {:closed false}]]
   [:agent_spec {:optional true} [:map {:closed false}]]
   [:answer {:optional true} [:maybe :string]]
   [:error {:optional true} [:maybe :string]]])

(defn valid?
  [schema value]
  (m/validate schema value))

(defn explain
  [schema value]
  (me/humanize (m/explain schema value)))
