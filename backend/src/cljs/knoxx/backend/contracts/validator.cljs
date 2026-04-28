(ns knoxx.backend.contracts.validator
  (:require [knoxx.backend.contracts.actor-scope :as actor-scope]
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
   [:role {:optional true} keyword?]
   [:roles {:optional true} [:sequential keyword?]]
   [:model {:optional true} string?]
   [:thinking {:optional true} keyword?]])

(def ActorCapSpec
  [:map {:closed false}
   [:capabilities {:optional true} [:sequential keyword?]]])

(def HookMap
  [:map {:closed false}
   [:before {:optional true} [:map {:closed false}]]
   [:after  {:optional true} [:map {:closed false}]]])

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
                                [:system {:optional true} string?]
                                [:task {:optional true} string?]]]
   [:data {:optional true} [:map {:closed false}]]])

(def ActorContract
  [:map {:closed false}
   [:actor/id string?]
   [:actor/kind [:enum :agent :user]]
   [:actor/email {:optional true} string?]
   [:actor/username {:optional true} string?]
   [:actor/org {:optional true} string?]
   [:actor/label {:optional true} string?]
   [:actor/contract {:optional true} string?]
   [:actor/default-agent {:optional true} string?]
   [:actor/roles {:optional true} [:sequential keyword?]]
   [:actor/capabilities {:optional true} [:sequential keyword?]]])

(def RoleContract
  [:map {:closed false}
   [:role/id keyword?]
   [:role/name {:optional true} string?]
   [:role/description {:optional true} string?]
   [:role/capabilities {:optional true} [:sequential keyword?]]
   [:role/permissions {:optional true} [:sequential string?]]
   [:prompts {:optional true}
    [:map {:closed false}
     [:system {:optional true} string?]
     [:task {:optional true} string?]]]
   [:role/system-prompt {:optional true} string?]])

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
   [:model/label {:optional true} string?]
   [:model/default {:optional true} boolean?]
   [:model/allowlisted {:optional true} boolean?]
   [:model/reasoning {:optional true} boolean?]
   [:model/default-thinking {:optional true} keyword?]
   [:model/thinking-levels {:optional true} [:sequential keyword?]]
   [:model/context-window {:optional true} int?]
    [:model/max-tokens {:optional true} int?]
    [:model/input {:optional true} [:sequential keyword?]]])

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

(defn- infer-contract-class
  [value]
  (cond
    (and (contains? value :contract/id)
         (= :policy (:contract/kind value))) "policies"
    (contains? value :contract/id) "agents"
    (contains? value :actor/id) "actors"
    (contains? value :role/id) "roles"
    (contains? value :cap/id) "capabilities"
    (contains? value :model-family/id) "model_families"
    (contains? value :model/id) "models"
    (= :ingest_source (:contract/kind value)) "ingest_sources"
    :else "agents"))

(defn- schema-for
  [contract-class value]
  (case (or contract-class (infer-contract-class value))
    "agents" AgentContract
    "actors" ActorContract
    "roles" RoleContract
    "capabilities" CapabilityContract
    "policies" PolicyContract
    "model_families" ModelFamilyContract
    "models" ModelContract
    "ingest_sources" IngestSourceContract
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
