(ns knoxx.backend.law.contracts
  (:require [knoxx.backend.domain.actor.scope :as actor-scope]
            [malli.core :as m]
            [malli.error :as me]))

(def ContractId
  [:or string? keyword?])

(def UserSurface
  [:map {:closed false}
   [:surface/id keyword?]
   [:surface/label string?]
   [:surface/kind {:optional true} keyword?]
   [:surface/routes {:optional true} [:vector string?]]
   [:surface/endpoints {:optional true} [:vector string?]]
   [:surface/description {:optional true} string?]])

(def PolicyCheck
  [:map {:closed false}
   [:id ContractId]
   [:severity [:enum :block :warn :note]]
   [:message string?]
   [:check [:map {:closed false}
            [:expr {:optional true} any?]
            [:rule {:optional true} keyword?]]]])

(def AgentSpec
  [:map {:closed false}
   [:role {:optional true} [:or keyword? string?]]
   [:roles {:optional true} [:sequential [:or keyword? string?]]]
   [:model {:optional true} [:maybe string?]]
   [:thinking {:optional true} [:or keyword? string?]]])

(def ActorCapSpec
  [:map {:closed false}
   [:capabilities {:optional true} [:sequential keyword?]]])

(def HookMap
  [:map {:closed false}
   [:before {:optional true} [:map {:closed false}]]
   [:after  {:optional true} [:map {:closed false}]]])

(def ContextPolicy
  [:map {:closed false}
   [:max-messages {:optional true} int?]
   [:max-chars {:optional true} int?]
   [:preserve-system {:optional true} boolean?]])

(def RuntimeSourceRef
  [:or keyword? string?
   [:map {:closed false}
    [:source/ref {:optional true} [:or keyword? string?]]
    [:source/id {:optional true} [:or keyword? string?]]
    [:filters {:optional true} [:map {:closed false}]]
    [:hydration {:optional true} [:map {:closed false}]]
    [:source/hydration {:optional true} [:map {:closed false}]]]])

(def UiAction
  [:map {:closed false}
   [:id string?]
   [:label string?]
   [:kind {:optional true} [:or keyword? string?]]
   [:surface {:optional true} [:or keyword? string?]]
   [:surfaces {:optional true} [:sequential [:or keyword? string?]]]
   [:icon {:optional true} string?]
   [:intent {:optional true} [:or keyword? string?]]
   [:agent/contract {:optional true} string?]
   [:agent/actor {:optional true} string?]
   [:tool/id {:optional true} string?]
   [:media/from {:optional true} [:or keyword? string?]]
   [:requires {:optional true} [:sequential [:or keyword? string?]]]
   [:mode {:optional true} [:or keyword? string?]]
   [:confirm? {:optional true} boolean?]
   [:enabled? {:optional true} boolean?]])

(def AgentContract
  "Agent contract schema — all disk fields tolerated."
  [:map {:closed false}
   [:contract/id string?]
   [:contract/kind keyword?]
   [:contract/version {:optional true} int?]
   [:enabled {:optional true} boolean?]
   [:trigger-kind {:optional true} keyword?]
   [:source-kind {:optional true} keyword?]
   [:source-mode {:optional true} keyword?]
   [:hooks {:optional true} HookMap]
   [:contract/actor {:optional true} string?]
   [:contract/actors {:optional true} [:or
                                        [:set [:or string? [:= actor-scope/wildcard-actor]]]
                                        [:sequential [:or string? [:= actor-scope/wildcard-actor]]]]]
   [:actor/id {:optional true} string?]
   [:actor/roles {:optional true} [:sequential keyword?]]
   [:actor/capabilities {:optional true} [:sequential keyword?]]
   [:actor {:optional true} ActorCapSpec]
   [:agent {:optional true} AgentSpec]
   [:prompts {:optional true} [:map {:closed false}
                                [:system {:optional true} any?]
                                [:task {:optional true} any?]]]
   [:sources {:optional true} [:sequential RuntimeSourceRef]]
   [:context {:optional true} ContextPolicy]
   [:context-policy {:optional true} ContextPolicy]
   [:ui/actions {:optional true} [:vector UiAction]]
   [:data {:optional true} [:map {:closed false}]]])

(def ActorContract
  [:map {:closed false}
   [:actor/id string?]
   [:actor/kind [:enum :agent :user :page]]
   [:actor/email {:optional true} string?]
   [:actor/username {:optional true} string?]
   [:actor/org {:optional true} string?]
   [:actor/label {:optional true} string?]
   [:actor/contract {:optional true} string?]
   [:actor/default-agent {:optional true} string?]
   [:actor/roles {:optional true} [:sequential keyword?]]
   [:actor/capabilities {:optional true} [:sequential keyword?]]
   [:actor/sources {:optional true} [:sequential RuntimeSourceRef]]
   [:sources {:optional true} [:sequential RuntimeSourceRef]]
   [:ui/actions {:optional true} [:vector UiAction]]])

(def RoleContract
  [:map {:closed false}
   [:role/id keyword?]
   [:role/name {:optional true} string?]
   [:role/description {:optional true} string?]
   [:role/capabilities {:optional true} [:sequential keyword?]]
   [:role/permissions {:optional true} [:sequential string?]]
   [:role/sources {:optional true} [:sequential RuntimeSourceRef]]
   [:sources {:optional true} [:sequential RuntimeSourceRef]]
   [:prompts {:optional true}
    [:map {:closed false}
     [:system {:optional true} any?]
     [:task {:optional true} any?]]]
   [:role/system-prompt {:optional true} any?]])

(def CapabilityContract
  [:map {:closed false}
   [:cap/id keyword?]
   [:cap/tools {:optional true} [:sequential any?]]
   [:cap/user-surfaces {:optional true} [:vector UserSurface]]])

(def PolicyContract
  [:map {:closed false}
   [:contract/id string?]
   [:contract/kind [:= :policy]]
   [:contract/doc {:optional true} string?]
   [:contract/scope {:optional true} string?]
   [:contract/uses {:optional true} [:vector ContractId]]
   [:policy/invariants {:optional true} [:vector PolicyCheck]]
   [:policy/required {:optional true} [:vector PolicyCheck]]
   [:policy/checked-by {:optional true} keyword?]])

(def ModelFamilyContract
  [:map {:closed false}
   [:model-family/id string?]
   [:model-family/provider {:optional true} keyword?]
   [:model-family/api {:optional true} [:or string? keyword?]]
   [:model-family/compat {:optional true} :map]
   [:model-family/prefixes [:sequential string?]]
   [:model-family/allowlisted {:optional true} boolean?]
   [:model-family/reasoning {:optional true} boolean?]
   [:model-family/default-thinking {:optional true} keyword?]
   [:model-family/thinking-levels {:optional true} [:sequential keyword?]]
   [:model-family/context-window {:optional true} int?]
   [:model-family/max-tokens {:optional true} int?]
   [:model-family/input {:optional true} [:sequential keyword?]]])

(def ModelContract
  [:map {:closed false}
   [:model/id string?]
   [:model-family/id {:optional true} string?]
   [:model/provider {:optional true} keyword?]
   [:model/api {:optional true} [:or string? keyword?]]
   [:model/compat {:optional true} :map]
   [:model/label {:optional true} string?]
   [:model/default {:optional true} boolean?]
   [:model/allowlisted {:optional true} boolean?]
   [:model/reasoning {:optional true} boolean?]
   [:model/default-thinking {:optional true} keyword?]
   [:model/thinking-levels {:optional true} [:sequential keyword?]]
   [:model/context-window {:optional true} int?]
    [:model/max-tokens {:optional true} int?]
    [:model/input {:optional true} [:sequential keyword?]]])

(def SourceModeContract
  "Source-mode contracts document how event runtime source modes transform upstream
   source records into template context and runtime dispatch behavior."
  [:map {:closed false}
   [:contract/kind [:= :source-mode]]
   [:contract/id ContractId]
   [:source-mode/id {:optional true} keyword?]
   [:source/kind {:optional true} [:or keyword? string?]]
   [:source/mode {:optional true} [:or keyword? string?]]
   [:prompts {:optional true} [:map {:closed false}
                                [:system {:optional true} any?]
                                [:task {:optional true} any?]]]
   [:data {:optional true} [:map {:closed false}]]])

(def GeneratorContract
  "Generator contracts declare event-producing actors, adapters, or processes.
   Schedules are generators for synthetic future events."
  [:map {:closed false}
   [:contract/kind [:= :generator]]
   [:contract/id ContractId]
   [:generator/id {:optional true} [:or string? keyword?]]
   [:generator/kind {:optional true} [:or string? keyword?]]
   [:generator/driver {:optional true} [:or string? keyword?]]
   [:generator/actor {:optional true} [:or string? keyword?]]
   [:generator/emits {:optional true} [:sequential keyword?]]
   [:generator/policy {:optional true} [:map {:closed false}]]
   [:data {:optional true} [:map {:closed false}]]])

(def ScheduleContract
  "Schedule contracts map a temporal rule to a synthetic event emission."
  [:map {:closed false}
   [:contract/kind [:= :schedule]]
   [:contract/id ContractId]
   [:schedule/id {:optional true} [:or string? keyword?]]
   [:schedule/rule {:optional true} [:or string? keyword?]]
   [:schedule/cron {:optional true} string?]
   [:schedule/at {:optional true} string?]
   [:schedule/generator {:optional true} ContractId]
   [:schedule/event {:optional true} [:map {:closed false}]]
   [:schedule/policy {:optional true} [:map {:closed false}]]
   [:data {:optional true} [:map {:closed false}]]])

(def SourceEmission
  [:or keyword?
   [:map {:closed false}
    [:event/type keyword?]
    [:event/shape {:optional true} [:map {:closed false}]]
    [:event/payload-schema {:optional true} any?]
    [:description {:optional true} string?]]])

(def SourceListener
  [:or keyword?
   [:map {:closed false}
    [:event/type keyword?]
    [:description {:optional true} string?]]])

(def RuntimeSourceContract
  "Source resources declare driver-backed event listeners or context providers.
   Event sources name the driver implemented in code, the actor identity that
   owns credentials, and the driver events this source cares about. Event shapes
   belong to driver implementation code, not source consumers. They are intentionally
   separate from :ingest_source contracts, which describe indexing and discovery
   jobs."
  [:map {:closed false}
   [:contract/kind [:= :source]]
   [:contract/id ContractId]
   [:contract/type {:optional true} [:or string? keyword?]]
   [:contract/version {:optional true} int?]
   [:source/id [:or string? keyword?]]
   [:source/type {:optional true} [:or string? keyword?]]
   [:source/name {:optional true} string?]
   [:source/enabled? {:optional true} boolean?]
   [:source/driver {:optional true} [:or string? keyword?]]
   [:source/actor {:optional true} [:or string? keyword?]]
   [:source/listens {:optional true} [:sequential SourceListener]]
   [:source/emits {:optional true} [:sequential SourceEmission]]
   [:source/protocol {:optional true} [:map {:closed false}]]
   [:source/provider {:optional true} [:or string? keyword?]]
   [:source/hydration {:optional true} [:map {:closed false}]]
   [:source/render {:optional true} [:map {:closed false}]]
   [:source/filters {:optional true} [:map {:closed false}]]
   [:source/tools {:optional true} [:sequential [:or string? keyword?]]]])

(def RuntimeFeatureContract
  "Runtime feature contracts let Knoxx manage non-agent runtime toggles such as
   eta-mu extensions without promoting those toggles into ad-hoc JSON state."
  [:map {:closed false}
   [:contract/kind [:= :runtime-feature]]
   [:contract/id string?]
   [:runtime-feature/id {:optional true} string?]
   [:runtime/owner {:optional true} [:or keyword? string?]]
   [:runtime/feature {:optional true} [:or keyword? string?]]
   [:eta-mu/extension {:optional true} [:or keyword? string?]]
   [:enabled {:optional true} boolean?]
   [:runtime/enabled {:optional true} boolean?]
   [:runtime/default-enabled {:optional true} boolean?]
   [:runtime/applies-to {:optional true} [:sequential [:map {:closed false}]]]
   [:runtime/config {:optional true} [:map {:closed false}]]])

(def IngestSourceContract
   "Ingestion source contracts live under contracts/ and are also discovered by
    the backend contract loader. They are not used by runtime.models, but they
    must validate to avoid noisy startup logs when contract discovery runs."
   [:map {:closed false}
    ;; Most existing ingestion contracts use :contract/type :ingest/source, but
    ;; the loader requires :contract/kind for identity.
    [:contract/kind [:= :ingest_source]]
    [:contract/id ContractId]
    [:contract/type {:optional true} [:or string? keyword?]]
    [:contract/version {:optional true} int?]
    [:tenant/id {:optional true} string?]
    [:source/id {:optional true} [:or string? keyword?]]
    [:source/name {:optional true} string?]
    [:source/enabled? {:optional true} boolean?]
    [:source/driver {:optional true} [:or string? keyword?]]
    [:source/config {:optional true} [:map {:closed false}]]
    [:source/discovery {:optional true} [:map {:closed false}]]
    [:source/schedule {:optional true} [:map {:closed false}]]
    [:source/ingest {:optional true} [:map {:closed false}]]
    [:source/sink {:optional true} [:map {:closed false}]]
    [:source/semantic {:optional true} [:map {:closed false}]]
    [:source/translation {:optional true} [:map {:closed false}]]
    [:source/projection {:optional true} [:map {:closed false}]]
    [:source/backpressure {:optional true} [:map {:closed false}]]])

(def SubAgentContract
  "Sub-agent contracts define lightweight agents that can be spawned by parent agents.
   They inherit conversation lineage and may restrict or inherit capabilities."
  [:map {:closed false}
   [:contract/kind [:= :sub-agent]]
   [:contract/id string?]
   [:contract/version {:optional true} int?]
   [:enabled {:optional true} boolean?]
   [:sub-agent/parent-capabilities {:optional true} [:enum :inherit :restrict :none]]
   [:sub-agent/capabilities {:optional true} [:vector any?]]
   [:sub-agent/role {:optional true} string?]
   [:sub-agent/model {:optional true} [:maybe string?]]
   [:sub-agent/thinking {:optional true} string?]
   [:sub-agent/timeout-ms {:optional true} int?]
   [:sub-agent/mode {:optional true} [:enum :fire-and-forget :await :collect]]
   [:agent {:optional true} AgentSpec]
   [:prompts {:optional true} [:map {:closed false}
                                [:system {:optional true} any?]
                                [:task {:optional true} any?]]]
    [:context {:optional true} [:map {:closed false}]]
    [:data {:optional true} [:map {:closed false}]]])

(def ActionContract
  [:map {:closed false}
   [:contract/kind [:= :action]]
   [:contract/id ContractId]
   [:contract/version {:optional true} int?]
   [:enabled {:optional true} boolean?]
   [:action/id {:optional true} ContractId]
   [:action/kind {:optional true} keyword?]
   [:action/handler string?]
   [:action/responds-to {:optional true} [:sequential keyword?]]
   [:action/result {:optional true} keyword?]
   [:action/scope {:optional true} [:map {:closed false}]]
   [:action/params {:optional true} [:map {:closed false}]]
   [:data {:optional true} [:map {:closed false}]]])

(def PipelineStep
  [:map {:closed false}
   [:step/id string?]
   [:step/contract ContractId]
   [:step/depends-on {:optional true} [:vector string?]]
   [:step/condition {:optional true} string?]
   [:step/data {:optional true} [:map {:closed false}]]])

(def PipelineContract
  [:map {:closed false}
   [:contract/kind [:= :pipeline]]
   [:contract/id ContractId]
   [:contract/version {:optional true} int?]
   [:enabled {:optional true} boolean?]
   [:pipeline/steps [:vector PipelineStep]]
   [:data {:optional true} [:map {:closed false}]]])

(def TriggerContract
  [:map {:closed false}
   [:contract/kind [:= :trigger]]
   [:contract/id ContractId]
   [:contract/version {:optional true} int?]
   [:enabled {:optional true} boolean?]
   [:trigger/kind [:enum :event]]
   [:trigger/events [:vector [:or string? keyword?]]]
   [:trigger/action {:optional true} ContractId]
   [:trigger/agent {:optional true} ContractId]
   [:trigger/actor {:optional true} ContractId]
   [:trigger/emitter {:optional true} ContractId]
   [:trigger/listener {:optional true} ContractId]
   [:trigger/domain {:optional true} [:map {:closed false}]]
   [:trigger/condition {:optional true} any?]
   [:trigger/predicate {:optional true} [:map {:closed false}]]
   [:trigger/with {:optional true} [:map {:closed false}]]
   [:data {:optional true} [:map {:closed false}]]])

(def CmsContract
  "CMS editor contracts are EDN records under contracts/ used by the folder-backed
   visual CMS. They keep their domain payload (:blocks or :templates) at the
   top-level so existing CMS editor file readers remain compatible."
  [:map {:closed false}
   [:contract/id string?]
   [:contract/kind [:enum :cms-block-registry :cms-templates :cms-template-registry]]
   [:contract/version {:optional true} int?]
   [:enabled {:optional true} boolean?]
   [:blocks {:optional true} [:map {:closed false}]]
   [:templates {:optional true} [:map {:closed false}]]])

(defn- contract-kind-class
  [value]
  (case (:contract/kind value)
    :policy "policies"
    :sub-agent "sub_agents"
    :action "actions"
    :pipeline "pipelines"
    :trigger "triggers"
    :generator "generators"
    :schedule "schedules"
    :source-mode "source_modes"
    :source "sources"
    :runtime-feature "runtime_features"
    :ingest_source "ingest_sources"
    nil))

(defn- structural-contract-class
  [value]
  (cond
    (contains? value :actor/id) "actors"
    (contains? value :role/id) "roles"
    (contains? value :cap/id) "capabilities"
    (contains? value :model/id) "models"
    (contains? value :model-family/id) "model_families"
    (contains? value :generator/id) "generators"
    (contains? value :schedule/id) "schedules"
    (contains? value :source-mode/id) "source_modes"
    (contains? value :runtime-feature/id) "runtime_features"
    :else nil))

(defn- infer-contract-class
  [value]
  (or (structural-contract-class value)
      (when (contains? #{:cms-block-registry :cms-templates :cms-template-registry}
                       (:contract/kind value))
        "cms")
      (contract-kind-class value)
      (when (contains? value :contract/id) "agents")
      "agents"))

(defn- schema-for
  [contract-class value]
  (case (or contract-class (infer-contract-class value))
    "agents" AgentContract
    "actors" ActorContract
    "roles" RoleContract
    "capabilities" CapabilityContract
    "policies" PolicyContract
    "generators" GeneratorContract
    "schedules" ScheduleContract
    "source_modes" SourceModeContract
    "sources" RuntimeSourceContract
    "model_families" ModelFamilyContract
    "models" ModelContract
    "runtime_features" RuntimeFeatureContract
    "ingest_sources" IngestSourceContract
    "actions" ActionContract
    "pipelines" PipelineContract
    "triggers" TriggerContract
    "sub_agents" SubAgentContract
    "cms" CmsContract
    AgentContract))

(defn- collect-humanized-errors
  [prefix value]
  (cond
    (nil? value) []
    (string? value) [{:path prefix :message value}]
    (vector? value) (mapcat #(collect-humanized-errors prefix %) value)
    (sequential? value) (mapcat #(collect-humanized-errors prefix %) value)
    (map? value) (mapcat (fn [[k v]]
                           (collect-humanized-errors (conj prefix (name k)) v))
                         value)
    :else [{:path prefix :message (pr-str value)}]))

(defn validate
  "Validate a parsed contract-like map.

   Returns:
   - {:ok true :value value :errors []}
   - {:ok false :value value :errors [{:path [...] :message <text>} ...]}"
  ([value]
   (validate nil value))
  ([contract-class value]
   (let [schema (schema-for contract-class value)
         ok? (m/validate schema value)
         explained (when-not ok? (m/explain schema value))
         errors (if explained
                  (->> (collect-humanized-errors [] (me/humanize explained))
                       (map (fn [err]
                              (update err :path (fn [p] (mapv str p)))))
                       vec)
                  [])]
     {:ok ok?
      :value value
      :errors errors})))
