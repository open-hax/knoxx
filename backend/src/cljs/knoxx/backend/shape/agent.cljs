(ns knoxx.backend.shape.agent
  "Protocol and schemas for agent request/session state.
   Implementations live under extern/ — no JS interop here."
  (:require [knoxx.backend.shape.agent.message :as message]
            [malli.core :as m]
            [malli.error :as me]))

(def ToolPolicy
  [:map {:closed false}
   [:toolId :string]
   [:effect [:enum "allow" "deny"]]])

(def AgentRequestSpec
  [:map {:closed false}
   [:contract-id {:optional true} [:maybe :string]]
   [:actor-id {:optional true} [:maybe :string]]
   [:contract-actors {:optional true} [:maybe [:vector [:map {:closed false}]]]]
   [:role {:optional true} [:maybe :string]]
   [:system-prompt {:optional true} [:maybe :string]]
   [:task-prompt {:optional true} [:maybe :string]]
   [:model {:optional true} [:maybe :string]]
   [:thinking-level {:optional true} [:maybe :string]]
   [:tool-policies {:optional true} [:maybe [:vector ToolPolicy]]]
   [:resource-policies {:optional true} any?]
   [:sources {:optional true} any?]
   [:memory-hydration {:optional true} any?]
   [:context-policy {:optional true} [:maybe [:map {:closed false}]]]
   [:sub-agent-id {:optional true} [:maybe :string]]
   [:parent-agent-id {:optional true} [:maybe :string]]
   [:parent-run-id {:optional true} [:maybe :string]]
   [:spawn-kind {:optional true} [:maybe :string]]])

(def AgentSpec
  "Canonical agent specification shape used by agent runtime requests. Kept as
   an alias during migration so older call sites can continue to refer to
   AgentRequestSpec."
  AgentRequestSpec)

(def ChatBody
  [:map {:closed false}
   [:message :string]
   [:conversation-id {:optional true} [:maybe :string]]
   [:session-id {:optional true} [:maybe :string]]
   [:run-id {:optional true} [:maybe :string]]
   [:model {:optional true} [:maybe :string]]
   [:thinking-level {:optional true} [:maybe :string]]
   [:content-parts {:optional true} [:vector message/ContentPart]]
   [:template-context {:optional true} [:maybe message/TemplateContext]]
   [:mode :string]
   [:agent-spec {:optional true} [:maybe AgentRequestSpec]]
   [:auth-context {:optional true} [:maybe [:map {:closed false}]]]])

(def ControlBody
  [:map {:closed false}
   [:message :string]
   [:conversation-id {:optional true} [:maybe :string]]
   [:session-id {:optional true} [:maybe :string]]
   [:run-id {:optional true} [:maybe :string]]
   [:actor-id {:optional true} [:maybe :string]]
   [:metadata {:optional true} [:map {:closed false}]]])

(defn valid-agent-request-spec?
  [value]
  (m/validate AgentRequestSpec value))

(defn valid-chat-body?
  [value]
  (m/validate ChatBody value))

(defn valid-control-body?
  [value]
  (m/validate ControlBody value))

(defn explain-agent-request-spec
  [value]
  (me/humanize (m/explain AgentRequestSpec value)))

(defprotocol IAgentSession
  (streaming? [s]
    "True when the session has an active streaming turn.")
  (current-turn [s]
    "The current running turn object (opaque), or nil if idle.")
  (messages [s]
    "A seq of the session's message history as JS objects.
     Individual message structure is eta-mu's internal format;
     use infra.agent.transcript functions to convert.")
  (subscribe! [s handler]
    "Subscribe to provider stream events. Returns an unsubscribe function.")
  (send-user-message! [s content]
    "Send a user prompt/content payload to the provider session. Returns a Promise.")
  (follow-up! [s message]
    "Queue a follow-up message into the running turn. Returns a Promise.")
  (steer! [s message]
    "Steer the current generation. Returns a Promise.")
  (set-thinking-level! [s level]
    "Set the thinking/reasoning level on the session."))
