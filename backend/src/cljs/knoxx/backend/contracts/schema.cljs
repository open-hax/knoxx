(ns knoxx.backend.contracts.schema
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
   [:trigger/target    ContractId]
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
