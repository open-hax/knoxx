(ns open-hax.contracts.schema
  "Unified resource boundary schema registry.

   Merges schemas from:
   - proxx.schema (provider/policy/strategy)
   - knoxx.backend resource definitions (agent/actor/role/capability/action/pipeline/trigger/schedule/generator)
   - eta-mu.contract-runtime-v2.core (policy-gate/fulfillment match maps)

   EDN files describe resources. Contracts are the schema/policy boundaries
   those resources must satisfy. The :contract/kind key remains the migration
   discriminator. Maps use {:closed false} to tolerate dialect-specific fields."
  (:require [malli.core :as m]
            [malli.error :as me]
            [malli.transform :as mt]))

;; ── Primitives ────────────────────────────────────────────────────────────────

(def ContractId
  [:or string? keyword?])

(def ToolId :string)

(def ISODuration :string)

(def EvalOp
  [:enum :all :some :none :not :assert])

(def PolicyOutcome
  [:enum :apply :try :next :reduce :block :warn :note :allow])

(def Severity
  [:enum :block :warn :note])

;; ── Eval nodes (proxx heritage) ───────────────────────────────────────────────

(def EvalNode
  [:map
   [:eval/op EvalOp]
   [:eval/target {:optional true} :keyword]
   [:eval/forms [:vector :any]]])

;; ── Policy match (eta-mu v2 heritage) ─────────────────────────────────────────

(def PolicyMatch
  [:map
   [:tool/name   {:optional true} :string]
   [:tool/params {:optional true} [:map-of :keyword :any]]])

;; ── Fulfillment match (eta-mu v2 heritage) ────────────────────────────────────

(def FulfillmentMatch
  [:map
   [:tool/name   {:optional true} :string]
   [:tool/params {:optional true} [:map-of :keyword :any]]
   [:tool/output {:optional true} :any]
   [:tool/error? {:optional true} :boolean]])

;; ── Agent contract (knoxx heritage) ───────────────────────────────────────────

(def AgentSpec
  [:map {:closed false}
   [:role     {:optional true} keyword?]
   [:roles    {:optional true} [:sequential keyword?]]
   [:model    {:optional true} string?]
   [:thinking {:optional true} keyword?]])

(def ActorCapSpec
  [:map {:closed false}
   [:capabilities {:optional true} [:sequential keyword?]]])

(def HookMap
  [:map {:closed false}
   [:before {:optional true} [:map {:closed false}]]
   [:after  {:optional true} [:map {:closed false}]]])

(def ContextPolicy
  [:map {:closed false}
   [:max-messages    {:optional true} int?]
   [:max-chars       {:optional true} int?]
   [:preserve-system {:optional true} boolean?]])

(def RuntimeSourceRef
  [:or keyword? string?
   [:map {:closed false}
    [:source/ref {:optional true} [:or keyword? string?]]
    [:source/id  {:optional true} [:or keyword? string?]]
    [:filters {:optional true} [:map {:closed false}]]
    [:hydration {:optional true} [:map {:closed false}]]
    [:source/hydration {:optional true} [:map {:closed false}]]]])

(def SubAgentConfig
  "Configuration for how a sub-agent relates to its parent."
  [:map {:closed false}
   [:mode         {:optional true} [:enum :fire-and-forget :await :collect]]
   [:timeout-ms   {:optional true} int?]
   [:inherit-role {:optional true} boolean?]
   [:restrict-capabilities {:optional true} [:vector keyword?]]
   [:shared-context {:optional true} [:map {:closed false}]]
   [:result-key  {:optional true} string?]])

(def AgentContract
  [:map {:closed false}
   [:contract/id      string?]
   [:contract/kind    [:= :agent]]
   [:contract/version {:optional true} int?]
   [:contract/actors  {:optional true} [:vector string?]]
   [:enabled          {:optional true} :boolean]
   [:trigger-kind     {:optional true} keyword?]
   [:source-kind      {:optional true} keyword?]
   [:source-mode      {:optional true} keyword?]
   [:hooks            {:optional true} HookMap]
   [:agent            {:optional true} AgentSpec]
   [:actor            {:optional true} ActorCapSpec]
   [:prompts          {:optional true}
    [:map {:closed false}
     [:system {:optional true} string?]
     [:task   {:optional true} string?]]]
   [:memory           {:optional true} [:map {:closed false}]]
   [:sources          {:optional true} [:sequential RuntimeSourceRef]]
   [:context          {:optional true} ContextPolicy]
   [:context-policy   {:optional true} ContextPolicy]
   [:sub-agents       {:optional true} [:vector string?]]
   [:data             {:optional true} [:map {:closed false}]]])

(def SubAgentContract
  "A sub-agent contract: a child agent spawned by a parent.
   Links to its parent via :parent-agent and declares delegation config."
  [:map {:closed false}
   [:contract/id      string?]
   [:contract/kind    [:= :sub-agent]]
   [:contract/version {:optional true} int?]
   [:contract/actors  {:optional true} [:vector string?]]
   [:parent-agent     string?]
   [:sub-agent/config {:optional true} SubAgentConfig]
   [:enabled          {:optional true} :boolean]
   [:agent            {:optional true} AgentSpec]
   [:actor            {:optional true} ActorCapSpec]
   [:prompts          {:optional true}
    [:map {:closed false}
     [:system {:optional true} string?]
     [:task   {:optional true} string?]]]
   [:memory           {:optional true} [:map {:closed false}]]
   [:sources          {:optional true} [:sequential RuntimeSourceRef]]
   [:context          {:optional true} ContextPolicy]
   [:context-policy   {:optional true} ContextPolicy]
   [:data             {:optional true} [:map {:closed false}]]])

;; ── Actor contract (knoxx heritage) ───────────────────────────────────────────

(def ActorContract
  [:map {:closed false}
   [:actor/id            string?]
   [:actor/kind          [:enum :agent :user]]
   [:actor/email         {:optional true} string?]
   [:actor/username      {:optional true} string?]
   [:actor/accounts      {:optional true}
    [:map {:closed false}
     [:discord {:optional true} [:map {:closed false}
                                [:username {:optional true} string?]
                                [:user-id {:optional true} string?]
                                [:userid {:optional true} string?]]]
     [:bluesky {:optional true} [:map {:closed false}
                                [:handle {:optional true} string?]
                                [:did {:optional true} string?]]]
     [:twitch {:optional true} [:map {:closed false}
                               [:username {:optional true} string?]
                               [:user-id {:optional true} string?]
                               [:userid {:optional true} string?]]]]]
   [:actor/org           {:optional true} string?]
   [:actor/label         {:optional true} string?]
   [:actor/contract      {:optional true} string?]
   [:actor/default-agent {:optional true} string?]
   [:actor/roles         {:optional true} [:sequential keyword?]]
   [:actor/capabilities  {:optional true} [:sequential keyword?]]
   [:actor/sources       {:optional true} [:sequential RuntimeSourceRef]]
   [:sources             {:optional true} [:sequential RuntimeSourceRef]]])

;; ── Role contract (knoxx heritage) ────────────────────────────────────────────

(def RoleContract
  [:map {:closed false}
   [:role/id           keyword?]
   [:role/name         {:optional true} string?]
   [:role/description  {:optional true} string?]
   [:role/capabilities {:optional true} [:sequential keyword?]]
   [:role/permissions  {:optional true} [:sequential string?]]
   [:role/sources      {:optional true} [:sequential RuntimeSourceRef]]
   [:sources           {:optional true} [:sequential RuntimeSourceRef]]
   [:prompts           {:optional true}
    [:map {:closed false}
     [:system {:optional true} string?]
     [:task   {:optional true} string?]]]
   [:role/system-prompt {:optional true} string?]])

;; ── Capability contract (knoxx heritage) ──────────────────────────────────────

(def UserSurface
  [:map {:closed false}
   [:surface/id          keyword?]
   [:surface/label       string?]
   [:surface/kind        {:optional true} keyword?]
   [:surface/routes      {:optional true} [:vector string?]]
   [:surface/endpoints   {:optional true} [:vector string?]]
   [:surface/description {:optional true} string?]])

(def CapabilityContract
  [:map {:closed false}
   [:cap/id           keyword?]
   [:cap/tools        {:optional true} [:vector any?]]
   [:cap/user-surfaces {:optional true} [:vector UserSurface]]])

;; ── Policy contract (proxx heritage — tree-shaped) ────────────────────────────

(declare PolicyContract*)

(def PolicyContract
  "Tree-shaped policy with conditions, filters, outcomes, children."
  [:map {:closed false}
   [:contract/id      :keyword]
   [:contract/kind    [:= :policy]]
   [:contract/doc     {:optional true} :string]
   [:contract/scope   {:optional true} :string]
   [:contract/uses    {:optional true} [:vector ContractId]]
   [:policy/condition {:optional true} EvalNode]
   [:policy/filters   {:optional true} [:vector EvalNode]]
   [:policy/outcome   PolicyOutcome]
   [:policy/strategy  {:optional true} :symbol]
   [:policy/children  {:optional true} [:vector [:ref :unified/policy]]]
   [:policy/sort      {:optional true} EvalNode]
   [:policy/project   {:optional true} [:vector :map]]
   [:policy/invariants {:optional true} [:vector :map]]
   [:policy/required  {:optional true} [:vector :map]]
   [:enabled          {:optional true} :boolean]])

;; ── Policy gate contract (eta-mu v2 heritage — flat tool-call gating) ─────────

(def PolicyGateContract
  [:map {:closed false}
   [:contract/id    :string]
   [:contract/kind  [:= :policy-gate]]
   [:contract/doc   {:optional true} :string]
   [:policy/match   PolicyMatch]
   [:policy/action  Severity]
   [:policy/reason  {:optional true} :string]
   [:policy/ttl-ms  {:optional true} int?]
   [:enabled        {:optional true} :boolean]])

;; ── Fulfillment contract (eta-mu v2 heritage) ─────────────────────────────────

(def FulfillmentContract
  [:map {:closed false}
   [:contract/id         :string]
   [:contract/kind       [:= :fulfillment]]
   [:contract/doc        {:optional true} :string]
   [:fulfillment/match   FulfillmentMatch]
   [:fulfillment/mode    [:enum :notify :audit]]
   [:fulfillment/message :string]
   [:fulfillment/level   {:optional true} [:enum :info :warn :error]]
   [:enabled             {:optional true} :boolean]])

;; ── Strategy contract (proxx heritage) ────────────────────────────────────────

(def StrategyContract
  [:map {:closed false}
   [:contract/id     :keyword]
   [:contract/kind   [:= :strategy]]
   [:policy/strategy :symbol]
   [:policy/outcome  [:= :try]]])

;; ── Action contract (knoxx heritage) ──────────────────────────────────────────

(def ActionContract
  [:map {:closed false}
   [:contract/kind   [:= :action]]
   [:contract/id     ContractId]
   [:contract/version {:optional true} int?]
   [:action/id       {:optional true} ContractId]
   [:action/kind     {:optional true} keyword?]
   [:action/handler  :string]
   [:action/responds-to {:optional true} [:sequential keyword?]]
   [:action/result   {:optional true} keyword?]
   [:action/scope    {:optional true} :map]
   [:action/params   {:optional true} :map]
   [:enabled         {:optional true} :boolean]
   [:data            {:optional true}
    [:map {:closed false}
     [:context {:optional true} :map]
     [:output  {:optional true} :map]]]])

;; ── Pipeline contract (knoxx heritage) ────────────────────────────────────────

(def PipelineStep
  [:map {:closed false}
   [:step/id         :string]
   [:step/contract   ContractId]
   [:step/depends-on {:optional true} [:vector :string]]
   [:step/condition  {:optional true} :string]
   [:step/data       {:optional true} :map]])

(def PipelineContract
  [:map {:closed false}
   [:contract/kind   [:= :pipeline]]
   [:contract/id     ContractId]
   [:contract/version {:optional true} int?]
   [:pipeline/steps  [:vector PipelineStep]]
   [:enabled         {:optional true} :boolean]
   [:data            {:optional true} :map]])

;; ── Trigger contract (actor-first) ────────────────────────────────────────────

(def TriggerContract
  [:map {:closed false}
   [:contract/kind     [:= :trigger]]
   [:contract/id       ContractId]
   [:contract/version  {:optional true} int?]
   [:trigger/kind      [:enum :event]]
   [:trigger/events    [:sequential [:or string? keyword?]]]
   [:trigger/action    {:optional true} ContractId]
   [:trigger/agent     {:optional true} ContractId]
   [:trigger/actor     {:optional true} ContractId]
   [:trigger/emitter   {:optional true} ContractId]
   [:trigger/listener  {:optional true} ContractId]
   [:trigger/domain    {:optional true} :map]
   [:trigger/condition {:optional true} :any]
   [:trigger/predicate {:optional true} :map]
   [:trigger/with      {:optional true} [:map {:closed false}]]
   [:enabled           {:optional true} :boolean]
   [:data              {:optional true} :map]])

(def GeneratorContract
  [:map {:closed false}
   [:contract/kind     [:= :generator]]
   [:contract/id       ContractId]
   [:contract/version  {:optional true} int?]
   [:generator/id      {:optional true} ContractId]
   [:generator/kind    {:optional true} [:or string? keyword?]]
   [:generator/driver  {:optional true} [:or string? keyword?]]
   [:generator/actor   {:optional true} ContractId]
   [:generator/emits   {:optional true} [:sequential keyword?]]
   [:generator/policy  {:optional true} :map]
   [:enabled           {:optional true} :boolean]
   [:data              {:optional true} :map]])

(def ScheduleContract
  [:map {:closed false}
   [:contract/kind     [:= :schedule]]
   [:contract/id       ContractId]
   [:contract/version  {:optional true} int?]
   [:schedule/id       {:optional true} ContractId]
   [:schedule/rule     {:optional true} [:or string? keyword?]]
   [:schedule/cron     {:optional true} string?]
   [:schedule/at       {:optional true} string?]
   [:schedule/generator {:optional true} ContractId]
   [:schedule/event    {:optional true} [:map {:closed false}]]
   [:schedule/policy   {:optional true} :map]
   [:enabled           {:optional true} :boolean]
   [:data              {:optional true} :map]])

;; ── Runtime source contract (knoxx) ──────────────────────────────────────────

(def SourceEmission
  [:or keyword?
   [:map {:closed false}
    [:event/type keyword?]
    [:event/shape {:optional true} [:map {:closed false}]]
    [:event/payload-schema {:optional true} :any]
    [:description {:optional true} string?]]])

(def SourceListener
  [:or keyword?
   [:map {:closed false}
    [:event/type keyword?]
    [:description {:optional true} string?]]])

(def RuntimeSourceContract
  "Source resource provider. Event sources are driver-backed listeners: they
   name the source driver implemented in code, the actor identity that owns
   credentials, and driver events they listen to. Context sources may still
   hydrate context before a turn. This is distinct from :ingest_source, which
   indexes data."
  [:map {:closed false}
   [:contract/kind    [:= :source]]
   [:contract/id      ContractId]
   [:contract/type    {:optional true} [:or string? keyword?]]
   [:contract/version {:optional true} int?]
   [:source/id        [:or string? keyword?]]
   [:source/type      {:optional true} [:or string? keyword?]]
   [:source/name      {:optional true} string?]
   [:source/enabled?  {:optional true} :boolean]
   [:source/driver    {:optional true} [:or string? keyword?]]
   [:source/actor     {:optional true} [:or string? keyword?]]
   [:source/listens   {:optional true} [:sequential SourceListener]]
   [:source/emits     {:optional true} [:sequential SourceEmission]]
   [:source/protocol  {:optional true} [:map {:closed false}]]
   [:source/provider  {:optional true} [:or string? keyword?]]
   [:source/hydration {:optional true} [:map {:closed false}]]
   [:source/render    {:optional true} [:map {:closed false}]]
   [:source/filters   {:optional true} [:map {:closed false}]]
   [:source/tools     {:optional true} [:sequential [:or string? keyword?]]]])

;; ── Model family contract (merged proxx + knoxx) ──────────────────────────────

(def ModelFamilyContract
  [:map {:closed false}
   [:model-family/id             string?]
   [:model-family/provider       {:optional true} keyword?]
   [:model-family/api            {:optional true} [:or string? keyword?]]
   [:model-family/compat         {:optional true} :map]
   [:model-family/prefixes       [:sequential string?]]
   [:model-family/allowlisted    {:optional true} :boolean]
   [:model-family/reasoning      {:optional true} :boolean]
   [:model-family/default-thinking {:optional true} keyword?]
   [:model-family/thinking-levels {:optional true} [:sequential keyword?]]
   [:model-family/context-window {:optional true} int?]
   [:model-family/max-tokens     {:optional true} int?]
   [:model-family/input          {:optional true} [:sequential keyword?]]])

;; ── Model contract (merged proxx + knoxx) ─────────────────────────────────────

(def ModelContract
  [:map {:closed false}
   [:model/id              string?]
   [:model-family/id       {:optional true} string?]
   [:model/provider        {:optional true} keyword?]
   [:model/api             {:optional true} [:or string? keyword?]]
   [:model/compat          {:optional true} :map]
   [:model/label           {:optional true} string?]
   [:model/default         {:optional true} :boolean]
   [:model/allowlisted     {:optional true} :boolean]
   [:model/reasoning       {:optional true} :boolean]
   [:model/default-thinking {:optional true} keyword?]
   [:model/thinking-levels {:optional true} [:sequential keyword?]]
   [:model/context-window  {:optional true} int?]
   [:model/max-tokens      {:optional true} int?]
   [:model/input           {:optional true} [:sequential keyword?]]])

;; ── Ingest source contract (knoxx heritage) ───────────────────────────────────

(def IngestSourceContract
  [:map {:closed false}
   [:contract/kind     [:= :ingest_source]]
   [:contract/id       ContractId]
   [:contract/type     {:optional true} [:or string? keyword?]]
   [:contract/version  {:optional true} int?]
   [:tenant/id         {:optional true} string?]
   [:source/id         {:optional true} [:or string? keyword?]]
   [:source/name       {:optional true} string?]
   [:source/enabled?   {:optional true} :boolean]
   [:source/driver     {:optional true} [:or string? keyword?]]
   [:source/config     {:optional true} [:map {:closed false}]]
   [:source/discovery  {:optional true} [:map {:closed false}]]
   [:source/schedule   {:optional true} [:map {:closed false}]]
   [:source/ingest     {:optional true} [:map {:closed false}]]
   [:source/sink       {:optional true} [:map {:closed false}]]
   [:source/semantic   {:optional true} [:map {:closed false}]]
   [:source/translation {:optional true} [:map {:closed false}]]
   [:source/projection {:optional true} [:map {:closed false}]]
   [:source/backpressure {:optional true} [:map {:closed false}]]])

;; ── Registry ──────────────────────────────────────────────────────────────────

(def registry
  "Complete schema registry. Keys match :contract/kind values."
  {;; Eval primitives
   :unified/eval-node          EvalNode
   :unified/policy             PolicyContract
   :unified/policy-match       PolicyMatch
   :unified/fulfillment-match  FulfillmentMatch

   ;; Agent runtime (knoxx)
   :agent        AgentContract
   :sub-agent    SubAgentContract
   :actor        ActorContract
   :role         RoleContract
   :capability   CapabilityContract

   ;; Policy engine (proxx + eta-mu)
   :policy       PolicyContract
   :policy-gate  PolicyGateContract
   :fulfillment  FulfillmentContract
   :strategy     StrategyContract

   ;; Orchestration (knoxx)
   :action       ActionContract
   :pipeline     PipelineContract
   :trigger      TriggerContract
   :generator    GeneratorContract
   :schedule     ScheduleContract
   :source       RuntimeSourceContract

   ;; Model catalog (merged)
   :model-family ModelFamilyContract
   :model        ModelContract

   ;; Data ingestion (knoxx)
   :ingest_source IngestSourceContract})

;; ── Kind inference ────────────────────────────────────────────────────────────

(defn infer-contract-class
  "Infer the contract kind from a parsed EDN map.
   Returns a keyword matching a registry key, or :agent as fallback."
  [value]
  (cond
    (not (map? value))                        :agent
    (contains? value :contract/kind)          (let [k (:contract/kind value)]
                                                (cond
                                                  (keyword? k) k
                                                  (string? k)  (keyword k)
                                                  :else        :agent))
    (contains? value :actor/id)               :actor
    (contains? value :role/id)                :role
    (contains? value :cap/id)                 :capability
    (contains? value :model/id)               :model
    (contains? value :model-family/id)        :model-family
    (contains? value :generator/id)           :generator
    (contains? value :schedule/id)            :schedule
    (contains? value :parent-agent)           :sub-agent
    (contains? value :contract/id)            :agent
    :else                                     :agent))

;; ── Public API ────────────────────────────────────────────────────────────────

(declare collect-humanized-errors)

(defn schema-for
  "Look up the Malli schema for a contract kind.
   Throws if the kind is unknown."
  [kind]
  (let [k (if (string? kind) (keyword kind) kind)]
    (or (get registry k)
        (throw (ex-info "Unknown contract kind"
                        {:kind kind :known (sort (keys registry))})))))

(defn validate
  "Validate a parsed contract map against its kind-specific schema.

   Returns:
   - {:ok true  :value value :errors []}
   - {:ok false :value value :errors [{:path [...] :message <text>} ...]}

   When contract-class is nil, the kind is inferred from the map."
  ([value]
   (validate nil value))
  ([contract-class value]
   (let [kind   (or contract-class (infer-contract-class value))
         schema (schema-for kind)
         ok?    (m/validate schema value)]
     (if ok?
       {:ok true :value value :errors []}
       (let [explained (m/explain schema value)
             errors    (->> (me/humanize explained)
                            (collect-humanized-errors [])
                            (mapv (fn [err] (update err :path #(mapv str %)))))]
         {:ok false :value value :errors errors})))))

(defn- collect-humanized-errors
  [prefix value]
  (cond
    (nil? value)       []
    (string? value)    [{:path prefix :message value}]
    (vector? value)    (mapcat #(collect-humanized-errors prefix %) value)
    (sequential? value) (mapcat #(collect-humanized-errors prefix %) value)
    (map? value)       (mapcat (fn [[k v]]
                                 (collect-humanized-errors (conj prefix (name k)) v))
                               value)
    :else              [{:path prefix :message (pr-str value)}]))

(defn assert!
  "Validate and throw on failure. Use at ingest boundaries."
  [contract-class value]
  (let [{:keys [ok errors]} (validate contract-class value)]
    (if ok
      value
      (throw (ex-info "Contract validation failed"
                      {:contract-class contract-class
                       :errors errors
                       :input value})))))

(defn coerce
  "Attempt to coerce a value through its schema's default-value-transformer.
   Returns the coerced value or nil on failure."
  [contract-class value]
  (let [schema (schema-for contract-class)]
    (try
      (let [coerced (m/coerce schema value (mt/default-value-transformer))]
        (when (m/validate schema coerced) coerced))
      (catch :default _ nil))))
