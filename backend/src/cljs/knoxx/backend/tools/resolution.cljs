(ns knoxx.backend.tools.resolution
  "Canonical ResolvedToolSuite resolution per #mu spec.

   Produces an immutable tool suite with full provenance traces, replacing
   all scattered tool-id-set + auth-context logic in the runtime.

   Inputs (ResolutionInputs) must NOT contain FORBIDDEN_INPUTS.
   Outputs (ResolvedToolSuite) carry provenance on every tool entry."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [knoxx.backend.contracts.loader :as loader]
            [knoxx.backend.contracts.roles :as roles]
            [knoxx.backend.tools.policies :as policies]
            [knoxx.backend.tools.registry :as tool-registry]))

(def FORBIDDEN_INPUTS
  #{:toolPolicies
    :agentSpec
    :source-kind
    :trigger-kind
    :event-kind})

(defn- forbidden-inputs-present?
  [m]
  (not (empty? (set/intersection (set (keys m)) FORBIDDEN_INPUTS))))

(defn- now-inst
  []
  (js/Date.))

(declare uuid-js-when-available)

(defn- build-actor-baseline
  [actor-defaults]
  (when (map? actor-defaults)
    (->> (get actor-defaults :tools [])
         (map tool-registry/normalize-tool-id)
         (remove str/blank?)
         set)))

(defn- capability-tool-ids-with-ref
  [config cap-ids]
  (->> cap-ids
       (mapcat (fn [cap-id]
                 (let [ids (roles/capability-tool-ids config cap-id)]
                   (map #(hash-map :tool-id % :cap-id cap-id) ids))))
       vec))

(defn- role-tool-ids-with-ref
  [config role-slugs]
  (->> role-slugs
       (mapcat (fn [role-slug]
                 (let [cap-ids (roles/role-capability-ids config role-slug)
                       tools (capability-tool-ids-with-ref config cap-ids)]
                   (map #(assoc % :role-slug role-slug) tools))))
       distinct
       vec))

(defn- normalize-cap-id
  [v]
  (cond
    (keyword? v) (str (namespace v) "/" (name v))
    (string? v) (some-> v str str/trim not-empty)
    :else nil))

(defn- resolve-tool-suite*
  [{:keys [actor/id actor-defaults roles capabilities contract/uses policy/contracts] :as inputs}]
  (if (forbidden-inputs-present? inputs)
    {:ok false
     :error/kind :error/legacy-input
     :error/msg "Legacy input key present; use contract/capability surface instead."
     :error/data (set/intersection (set (keys inputs)) FORBIDDEN_INPUTS)}
    (let [run-id (str (uuid-js-when-available))
          normalized-roles (->> (or roles [])
                                (map (fn [v]
                                       (let [s (cond (keyword? v) (name v)
                                                     (string? v) v
                                                     :else nil)]
                                         (some-> s str str/trim not-empty))))
                                (remove nil?)
                                distinct
                                vec)
          normalized-caps (->> (or capabilities [])
                                (map normalize-cap-id)
                                (remove nil?)
                                distinct
                                vec)
          actor-baseline (build-actor-baseline defaults)
          config {:contracts-dir (or (:contracts-dir inputs) "contracts")}
          role-tools (role-tool-ids-with-ref config normalized-roles)
          cap-tools (capability-tool-ids-with-ref config normalized-caps)
          role-tool-ids-set (->> role-tools (map :tool-id) set)
          cap-tool-ids-set (->> cap-tools (map :tool-id) set)
          explicit-from-actor (or actor-baseline #{})
          all-granted-tool-ids (set/union role-tool-ids-set cap-tool-ids-set explicit-from-actor)
          loaded-tc-contracts (policies/load-tool-call-contracts! config (or uses []))
          loaded-pol-contracts (policies/load-policy-contracts! config (or contracts []))
          tc-denied (policies/merge-tool-call-contracts loaded-tc-contracts all-granted-tool-ids)
          pol-denied (policies/merge-policy-contracts loaded-pol-contracts all-granted-tool-ids)
          all-denied (->> (concat tc-denied pol-denied)
                          (map tool-registry/normalize-tool-id)
                          set)
          granted-and-not-denied (set/difference all-granted-tool-ids all-denied)
          final-tool-ids (vec (sort granted-and-not-denied))
          tool-entries (into {} (map (fn [tool-id]
                                       (let [call-shape (some #(policies/tool-call-contract-call-shape % tool-id)
                                                              loaded-tc-contracts)
                                             prov {:granted-by (cond
                                                                (contains? role-tool-ids-set tool-id) :capability/explicit
                                                                (contains? cap-tool-ids-set tool-id)  :capability/explicit
                                                                (contains? explicit-from-actor tool-id) :actor/baseline
                                                                :else :capability/explicit)
                                                   :grant-ref (or (some (fn [t]
                                                                          (when (= (:tool-id t) tool-id)
                                                                            (:cap-id t)))
                                                                  cap-tools)
                                                                 (some (fn [t]
                                                                         (when (= (:tool-id t) tool-id)
                                                                           (:role-slug t)))
                                                                        role-tools)
                                                                 (str actor/id))}]
                                        [tool-id {:tool/id tool-id
                                                  :call-shape call-shape
                                                  :provenance prov}]))
                                  final-tool-ids))
          denied-reasons (into {} (comp
                                   (map tool-registry/normalize-tool-id)
                                   (remove nil?)
                                   (map (fn [tool-id]
                                          (let [tc-reason (some #(when (some #{tool-id}
                                                                              (policies/tool-call-contract-tools %))
                                                                    (policies/tool-call-contract-reason % tool-id))
                                                                 loaded-tc-contracts)
                                                pol-reason (some #(when (some #{tool-id}
                                                                             (policies/policy-contract-denied % all-granted-tool-ids))
                                                                   (policies/policy-contract-reason % tool-id))
                                                                 loaded-pol-contracts)]
                                            [tool-id (or tc-reason pol-reason
                                                         (str "Tool not granted: " tool-id))]))))
                                  all-denied)]
      {:ok true
       :suite {:run/id run-id
               :contract/id (or (some :contract/id inputs) "")
               :actor/id (str actor/id)
               :resolved-at (now-inst)
               :tools tool-entries
               :denied (vec (sort all-denied))
               :denied-reasons denied-reasons}})))

(defn- uuid-js-when-available
  []
  (if (exists? js/crypto)
    (let [buf (js/Uint8Array 16)]
      (js/crypto.getRandomValues buf)
      (apply str (map (fn [b]
                        (-> b
                            (bit-and 0xFF)
                            (.toString 16)
                            (str/replace #"^(.)$" "0$1")))
                      (array-seq buf))))
    (str "r" (.getTime (js/Date.)))))

(defn resolve-tool-suite
  "Resolve a ResolvedToolSuite from ResolutionInputs.

   Returns ResolutionResult:
     {:ok true :suite <ResolvedToolSuite>}
     {:ok false :error/kind <kind> :error/msg <msg> :error/data <data>}"
  [inputs]
  (resolve-tool-suite* inputs))
