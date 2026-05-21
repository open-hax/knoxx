(ns knoxx.backend.domain.policy.tools
  "Policy and tool-call contract loading for tool access restriction.

   Policy contracts (contract/kind = :policy) narrow or remove tools.
   Tool-call contracts (contract/kind = :tool-call) declare an explicit tool surface.
   Neither contract class grants tools — only the capability/role path does.
   All functions are pure and sync."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.domain.contracts.loader :as contracts-loader]
            [knoxx.backend.infra.registry.tools :as tool-registry]
            ["node:fs" :as node-fs]
            ["node:path" :as path]))

(defn read-edn-sync
  [file-path]
  (try
    (some-> (.readFileSync node-fs file-path "utf8")
            cljs.reader/read-string)
    (catch :default _
      nil)))

(defn tool-call-dir
  [config]
  (let [cwd (if (exists? js/process) (.cwd js/process) "")
        candidates (->> [(or (:contracts-dir config) "contracts")
                         "../contracts"
                         "packages/agents/knoxx/contracts"
                         "orgs/open-hax/openplanner/packages/agents/knoxx/contracts"]
                        (map #(path/join cwd % "tool_calls"))
                        distinct)]
    (some #(when (.existsSync node-fs %) %) candidates)))

(defn policy-dir
  [config]
  (let [cwd (if (exists? js/process) (.cwd js/process) "")
        candidates (->> [(or (:contracts-dir config) "contracts")
                         "../contracts"
                         "packages/agents/knoxx/contracts"
                         "orgs/open-hax/openplanner/packages/agents/knoxx/contracts"]
                        (map #(path/join cwd % "policies"))
                        distinct)]
    (some #(when (.existsSync node-fs %) %) candidates)))

(defn load-tool-call-contract!
  [config contract-id]
  (when-let [dir (tool-call-dir config)]
    (let [file-path (path/join dir (str contract-id ".edn"))]
      (when (.existsSync node-fs file-path)
        (read-edn-sync file-path)))))

(defn load-policy-contract!
  [config contract-id]
  (some-> (contracts-loader/find-contract-record-sync config "policies" contract-id)
          :contract))

(defn load-tool-call-contracts!
  [config contract-ids]
  (keep (fn [id]
          (let [contract (load-tool-call-contract! config id)]
            (when (and contract
                       (= :tool-call (:contract/kind contract)))
              contract)))
        contract-ids))

(defn load-policy-contracts!
  [config contract-ids]
  (keep (fn [id]
          (let [contract (load-policy-contract! config id)]
            (when (and contract
                       (= :policy (:contract/kind contract)))
              contract)))
        contract-ids))

(defn- tool-call-contract-tools
  [contract]
  (->> (get-in contract [:tools/allowed] [])
       (map tool-registry/normalize-tool-id)
       (remove str/blank?)
       set))

(defn- tool-call-contract-call-shape
  [contract tool-id]
  (get-in contract [:tools/call-shape tool-id]))

(defn tool-call-contract-denied
  "Return tools from granted-tool-ids that are NOT in the contract's :tools/allowed.
   Empty :tools/allowed means no restriction (passthrough)."
  [contract granted-tool-ids]
  (let [allowed (tool-call-contract-tools contract)]
    (if (empty? allowed)
      []
      (vec (set/difference granted-tool-ids allowed)))))

(defn policy-contract-denied
  [contract _granted-tool-ids]
  (->> (get-in contract [:policy/denied] [])
       (map tool-registry/normalize-tool-id)
       (remove str/blank?)
       vec))

(defn policy-contract-reason
  [contract tool-id]
  (or (get-in contract [:policy/reasons tool-id])
      (str "Denied by policy contract " (:contract/id contract))))

(defn tool-call-contract-reason
  [contract tool-id]
  (str "Denied by tool-call contract " (:contract/id contract)
       " — not declared in :tools/allowed"))

(defn merge-tool-call-contracts
  "Union all allowed sets from multiple tool-call contracts.
   Tools not in any contract are removed."
  [contracts granted-tool-ids]
  (let [allowed-all (->> contracts
                         (mapcat tool-call-contract-tools)
                         set)
        denied (if (empty? allowed-all)
                 #{}
                 (set/difference granted-tool-ids allowed-all))]
    (vec denied)))

(defn merge-policy-contracts
  "Intersect deny sets from multiple policy contracts.
   A tool must be denied by every policy contract that covers it."
  [contracts granted-tool-ids]
  (if (empty? contracts)
    []
    (let [per-contract-denied (map #(policy-contract-denied % granted-tool-ids) contracts)
          tool->deny-count (apply merge-with + {}
                                  (for [denied per-contract-denied]
                                    (into {} (map (fn [t] [t 1]) denied))))
          must-deny (into {} (filter (fn [[_ cnt]]
                                       (= cnt (count contracts)))
                                     tool->deny-count))]
      (vec (keys must-deny)))))