---
title: "μ Malli Contract Schema Freeze"
category: contracts
created: 2026-04-22
original: 2026.04.22.10.48.07.md
status: note
---

Here’s the μ cut: a proposed Malli freeze for the **next** policy-contract layer, not a claim that Knoxx already enforces all of this at runtime.  It follows the direction we had reached: `contractkind` + `contractuses` stay as the composition spine, `policy` narrows behavior rather than granting it, `tool` access is really capability-shaped, and actor binding is the missing bridge between principal identity and contract execution.
## μ

```clojure
(ns knoxx.contract.schema
  (:require
   [malli.core :as m]))

(def NonEmptyString
  [:and string? [:fn {:error/message "must be non-empty"} seq]])

(def KeywordOrStringId
  [:or keyword? NonEmptyString])

(def Probability
  [:and number? [:fn {:error/message "must be between 0.0 and 1.0"}
                 #(<= 0.0 % 1.0)]])

(def Expr
  [:map
   [:expr any?]])

(def FnRef
  [:map
   [:fn-ref keyword?]])

(def EvalForm
  [:or Expr FnRef])

(def Decision
  [:map
   [:allowed boolean?]
   [:rationale {:optional true} string?]
   [:meta {:optional true} map?]])

(def FulfillmentResult
  [:map
   [:fulfilled boolean?]
   [:verdict {:optional true} [:enum :held :failed :partial]]
   [:rejection-reason {:optional true} string?]
   [:rationale {:optional true} string?]
   [:evidence-refs {:optional true} [:vector any?]]
   [:meta {:optional true} map?]])

(def RoleId keyword?)
(def CapabilityId keyword?)
(def ContractId NonEmptyString)
(def ActorId NonEmptyString)
(def ToolName NonEmptyString)
(def LakeName NonEmptyString)

(def BaseContract
  [:map
   [:contract/id ContractId]
   [:contract/kind
    [:enum :agent :policy :intent :fulfillment :trigger :tool-call :role]]
   [:contract/doc {:optional true} string?]
   [:contract/version {:optional true} pos-int?]
   [:contract/enabled {:optional true} boolean?]
   [:contract/tags {:optional true} [:vector keyword?]]
   [:contract/uses {:optional true} [:vector ContractId]]])

(def ToolOp
  [:map
   [:tool/name ToolName]
   [:tool/ops [:vector KeywordOrStringId]]
   [:tool/defaults {:optional true} map?]])

(def Capability
  [:merge
   BaseContract
   [:map
    [:contract/kind [:= :capability]]
    [:capability/id CapabilityId]
    [:capability/doc {:optional true} string?]
    [:capability/tools {:optional true} [:vector ToolOp]]
    [:capability/lakes {:optional true} [:vector LakeName]]
    [:capability/default-intent {:optional true} ContractId]
    [:capability/default-fulfillment {:optional true} ContractId]
    [:capability/config {:optional true} map?]]])

(def Role
  [:merge
   BaseContract
   [:map
    [:contract/kind [:= :role]]
    [:role/id RoleId]
    [:role/doc {:optional true} string?]
    [:role/system {:optional true} [:or string? EvalForm]]
    [:role/capabilities {:optional true} [:vector CapabilityId]]
    [:role/defaults {:optional true}
     [:map
      [:model {:optional true} NonEmptyString]
      [:thinking {:optional true} [:enum :off :minimal :on]]
      [:sinks {:optional true} [:vector keyword?]]
      [:lake {:optional true} LakeName]
      [:before/hooks {:optional true} map?]
      [:after/hooks {:optional true} map?]]]]])

(def Actor
  [:map
   [:actor/id ActorId]
   [:actor/status [:enum :active :disabled :draft]]
   [:actor/org-id {:optional true} NonEmptyString]
   [:actor/role {:optional true} RoleId]
   [:actor/roles {:optional true} [:vector RoleId]]
   [:actor/capabilities {:optional true} [:vector CapabilityId]]
   [:model {:optional true} NonEmptyString]
   [:thinking {:optional true} [:enum :off :minimal :on]]
   [:system {:optional true} [:or string? EvalForm]]
   [:sinks {:optional true} [:vector keyword?]]
   [:lake {:optional true} LakeName]
   [:before/hooks {:optional true} map?]
   [:after/hooks {:optional true} map?]])

(def TriggerContract
  [:merge
   BaseContract
   [:map
    [:contract/kind [:= :trigger]]
    [:trigger/match EvalForm]
    [:trigger/normalize {:optional true} EvalForm]
    [:trigger/emits {:optional true} [:vector keyword?]]]])

(def IntentContract
  [:merge
   BaseContract
   [:map
    [:contract/kind [:= :intent]]
    [:intent/mode {:optional true} [:enum :loose :strict :custom]]
    [:intent/check EvalForm]
    [:intent/default-allow? {:optional true} boolean?]]])

(def PolicyCheck
  [:map
   [:policy/check EvalForm]
   [:policy/rationale {:optional true} string?]])

(def PolicyContract
  [:merge
   BaseContract
   [:map
    [:contract/kind [:= :policy]]
    [:policy/checks [:vector PolicyCheck]]
    [:policy/default-intent {:optional true} ContractId]
    [:policy/default-fulfillment {:optional true} ContractId]
    [:policy/config {:optional true} map?]]])

(def ToolCallContract
  [:merge
   BaseContract
   [:map
    [:contract/kind [:= :tool-call]]
    [:tool-call/tools [:vector ToolOp]]
    [:tool-call/lakes {:optional true} [:vector LakeName]]
    [:tool-call/attests-as {:optional true} [:vector keyword?]]]])

(def FulfillmentContract
  [:merge
   BaseContract
   [:map
    [:contract/kind [:= :fulfillment]]
    [:fulfillment/mode [:enum :deterministic :judge :hybrid]]
    [:fulfillment/check EvalForm]]])

(def AgentContract
  [:merge
   BaseContract
   [:map
    [:contract/kind [:= :agent]]

    ;; principal binding
    [:actor-id ActorId]

    ;; actor-supplied baseline may be appended or replaced
    [:additional-roles {:optional true} [:vector RoleId]]
    [:roles-override {:optional true} [:vector RoleId]]

    ;; per-run overrides
    [:model {:optional true} NonEmptyString]
    [:thinking {:optional true} [:enum :off :minimal :on]]
    [:system {:optional true} [:or string? EvalForm]]
    [:task {:optional true} [:or string? EvalForm]]

    ;; data / destinations
    [:sinks {:optional true} [:vector keyword?]]
    [:lake {:optional true} LakeName]

    ;; hooks
    [:before/hooks {:optional true} map?]
    [:after/hooks {:optional true} map?]]])

(def Contract
  [:or
   TriggerContract
   IntentContract
   PolicyContract
   ToolCallContract
   FulfillmentContract
   Role
   AgentContract])

(def ActorBinding
  [:map
   [:actor/id ActorId]
   [:contract/id ContractId]
   [:org-id {:optional true} NonEmptyString]
   [:run-id {:optional true} uuid?]
   [:episode-id {:optional true} uuid?]
   [:causedby {:optional true} any?]])

(def EpistemicFact
  [:map
   [:ctx keyword?]
   [:claim any?]
   [:src any?]
   [:p Probability]
   [:time inst?]])

(def Obs
  [:map
   [:ctx keyword?]
   [:about any?]
   [:signal any?]
   [:p Probability]])

(def Inference
  [:map
   [:from [:vector any?]]
   [:rule keyword?]
   [:actor keyword?]
   [:claim any?]
   [:p Probability]])

(def Attestation
  [:map
   [:actor keyword?]
   [:did any?]
   [:run-id uuid?]
   [:causedby {:optional true} uuid?]
   [:p Probability]])

(def Judgment
  [:map
   [:of uuid?]
   [:verdict [:enum :held :failed :partial]]
   [:auditor keyword?]
   [:p Probability]])

(def contract-kind->epistemic-role
  {:trigger     :obs-promoter
   :policy      :side-condition
   :tool-call   :capability-grant
   :agent       :inference-rule
   :fulfillment :judgment-emitter
   :role        :actor-authority})

(def RuntimeInvariants
  [:map
   [:intent/result Decision]
   [:fulfillment/result FulfillmentResult]])
```

## Evidence

Facts: the schema above keeps `contractkind` and `contractuses` as the top-level composition model, because that was the design center already reached in the contract DSL discussion.  Facts: it also standardizes fulfillment around a normalized result map, because the earlier direction explicitly called for every fulfillment gate to return a stable shape rather than a loose boolean.  Facts: the separation between `Actor`, `Role`, `AgentContract`, and `ActorBinding` follows the same architectural pressure that had already surfaced in the notes: actors are principals, contracts are constitutions of behavior, and a binding layer is needed to connect identity, policy, execution, and audit.
## Next

My sharpest recommendation is to treat this as the **schema freeze candidate**, then implement only three validation rules first: policy never grants authority, roles/capabilities provide the ceiling, and agent contracts may only append or override actor defaults for a run.  After that, the clean follow-on μ would be executable validators for those invariants plus one deterministic `:intent` and one deterministic `:fulfillment` example contract.
