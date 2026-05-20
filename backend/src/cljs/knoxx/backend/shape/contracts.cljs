(ns knoxx.backend.shape.contracts
  "Malli schemas for :action, :pipeline, and :trigger contract kinds."
  (:require [malli.core :as m]))

;; ─── Shared primitives ────────────────────────────────────────────────────────

(def ContractId :string)
(def ToolId     :string)
(def ISODuration :string)

(def ToolPolicy
  [:map
   [:toolId ToolId]
   [:effect [:enum "allow" "deny"]]])

(def DataShape
  "The :data field base. Extended by each contract kind."
  [:map
   [:tools    {:optional true} [:vector ToolId]]
   [:filters  {:optional true}
    [:map
     [:channels     {:optional true} [:vector :string]]
     [:keywords     {:optional true} [:vector :string]]
     [:guildIds     {:optional true} [:vector :string]]
     [:repositories {:optional true} [:vector :string]]]]
   [:source   {:optional true} :map]
   [:context  {:optional true} :map]
   [:output   {:optional true} :map]])

;; ─── :action contract ─────────────────────────────────────────────────────────
;; Non-LLM, deterministic executor (pure fn / tool call).

(def ActionContract
  [:map
   [:contract/kind   [:= :action]]
   [:contract/id     ContractId]
   [:action/handler  :string]
   [:action/params   {:optional true} :map]
   [:enabled         {:optional true} :boolean]
   [:data
    [:map
     [:context {:optional true} :map]
     [:output  {:optional true}
      [:map
       [:key   :string]
       [:ttl   {:optional true} ISODuration]
       [:merge {:optional true} :boolean]]]]]])

;; ─── :pipeline contract ───────────────────────────────────────────────────────
;; Ordered sequence of steps. Each step refs a :action or :agent contract.

(def PipelineStep
  [:map
   [:step/id          :string]
   [:step/contract    ContractId]
   [:step/depends-on  {:optional true} [:vector :string]]
   [:step/condition   {:optional true} :string]
   [:step/data        {:optional true}
    [:map
     [:context {:optional true} :map]
     [:output  {:optional true}
      [:map
       [:key :string]
       [:ttl {:optional true} ISODuration]]]]]])

(def PipelineContract
  [:map
   [:contract/kind   [:= :pipeline]]
   [:contract/id     ContractId]
   [:pipeline/steps  [:vector PipelineStep]]
   [:enabled         {:optional true} :boolean]
   [:data
    [:map
     [:context {:optional true} :map]
     [:output  {:optional true}
      [:map [:key :string] [:ttl {:optional true} ISODuration]]]]]])

;; ─── :trigger contract ────────────────────────────────────────────────────────
;; Fires a pipeline (or single agent/action) on schedule or event.

(def TriggerContract
  [:map
   [:contract/kind     [:= :trigger]]
   [:contract/id       ContractId]
   [:trigger/kind      [:enum :cron :event :webhook :manual]]
   [:trigger/action    {:optional true} ContractId]
   [:trigger/agent     {:optional true} ContractId]
   [:trigger/with      {:optional true} [:map {:closed false}]]
   [:trigger/schedule  {:optional true} :string]
   [:trigger/source    {:optional true}
    [:map
     [:kind   [:enum :discord :github :http :manual]]
     [:config {:optional true} :map]]]
   [:enabled           {:optional true} :boolean]
   [:data
    [:map
     [:context {:optional true} :map]
     [:filters {:optional true} :map]]]])

;; ─── :sub-agent contract ─────────────────────────────────────────────────────
;; A lightweight agent that can be spawned by a parent agent.
;; Sub-agents inherit conversation lineage and may restrict capabilities.

(def SubAgentContract
  [:map
   [:contract/kind   [:= :sub-agent]]
   [:contract/id     ContractId]
   [:enabled         {:optional true} :boolean]
   [:sub-agent/parent-capabilities {:optional true}
    [:enum :inherit :restrict :none]]
   [:sub-agent/capabilities {:optional true} [:vector ToolId]]
   [:sub-agent/role   {:optional true} :string]
   [:sub-agent/model  {:optional true} [:maybe :string]]
   [:sub-agent/thinking {:optional true} :string]
   [:sub-agent/timeout-ms {:optional true} int?]
   [:sub-agent/mode   {:optional true} [:enum :fire-and-forget :await :collect]]
   [:agent {:optional true}
    [:map
     [:role {:optional true} :string]
     [:model {:optional true} :string]
     [:thinking {:optional true} :string]]]
   [:prompts {:optional true}
    [:map
     [:system {:optional true} :any]
     [:task {:optional true} :any]]]
   [:context {:optional true} :map]
   [:data {:optional true} :map]])
