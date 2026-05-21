(ns knoxx.backend.shape.agent.turn
  "Schemas for Knoxx agent turn lifecycle state."
  (:require [knoxx.backend.shape.agent.message :as message]
            [malli.core :as m]
            [malli.error :as me]))

(def TurnStatus
  [:enum "queued" "running" "completed" "failed" "waiting_input" "cancelled"])

(def StreamEventKind
  [:enum :text :reasoning :tool_call :tool_result :error :done :status])

(def StreamEvent
  [:map {:closed false}
   [:id {:optional true} :string]
   [:kind StreamEventKind]
   [:status {:optional true} TurnStatus]
   [:delta {:optional true} :string]
   [:text {:optional true} :string]
   [:reasoning {:optional true} :string]
   [:tool_name {:optional true} :string]
   [:tool_call_id {:optional true} :string]
   [:data {:optional true} [:map {:closed false}]]
   [:error {:optional true} [:maybe :string]]
   [:at {:optional true} [:or :string int?]]])

(def AgentTurnInput
  [:map {:closed false}
   [:message {:optional true} :string]
   [:messages {:optional true} [:vector message/AgentMessage]]
   [:conversation-id {:optional true} [:maybe :string]]
   [:session-id {:optional true} [:maybe :string]]
   [:run-id {:optional true} [:maybe :string]]
   [:model {:optional true} [:maybe :string]]
   [:thinking-level {:optional true} [:maybe :string]]
   [:content-parts {:optional true} [:vector message/ContentPart]]
   [:template-context {:optional true} message/TemplateContext]
   [:mode {:optional true} :string]
   [:agent-spec {:optional true} [:maybe [:map {:closed false}]]]
   [:auth-context {:optional true} [:maybe [:map {:closed false}]]]])

(def AgentTurnState
  [:map {:closed false}
   [:run-id {:optional true} :string]
   [:session-id :string]
   [:conversation-id {:optional true} :string]
   [:status TurnStatus]
   [:has-active-stream {:optional true} :boolean]
   [:messages {:optional true} [:vector message/AgentMessage]]
   [:started-at {:optional true} [:or :string int?]]
   [:updated-at {:optional true} [:or :string int?]]
   [:trace-blocks {:optional true} [:vector [:map {:closed false}]]]
   [:tool-receipts {:optional true} [:vector [:map {:closed false}]]]
   [:error {:optional true} [:maybe :string]]])

(def AgentTurnResult
  [:map {:closed false}
   [:status TurnStatus]
   [:answer {:optional true} [:maybe :string]]
   [:reasoning {:optional true} [:maybe :string]]
   [:messages {:optional true} [:vector message/AgentMessage]]
   [:trace-blocks {:optional true} [:vector [:map {:closed false}]]]
   [:tool-receipts {:optional true} [:vector [:map {:closed false}]]]
   [:error {:optional true} [:maybe :string]]])

(defn valid-turn-input?
  [value]
  (m/validate AgentTurnInput value))

(defn explain-turn-input
  [value]
  (me/humanize (m/explain AgentTurnInput value)))
