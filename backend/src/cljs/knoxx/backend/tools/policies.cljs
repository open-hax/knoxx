(ns knoxx.backend.tools.policies
  "Policy and tool-call contract loading for tool access restriction.

   Policy contracts (contract/kind = :policy) narrow or remove tools.
   Tool-call contracts (contract/kind = :tool-call) declare an explicit tool surface.
   Neither contract class grants tools — only the capability/role path does.
   All functions are now async (return Promises)."
  (:require [clojure.set :as set]
            [knoxx.backend.contracts.loader :as loader]
            [knoxx.backend.tools.registry :as tool-registry]
            ["node:path" :as path]))

(defn- find-dir
  [config subdirs]
  (-> (loader/contract-root-paths config)
      (.then (fn [roots]
               (let [candidates (->> roots
                                (map #(path/join % subdirs))
                                distinct
                                vec)]
                 (-> (js/Promise.all
                      (clj->js
                       (mapv (fn [candidate]
                               (-> (loader/read-edn-file! (path/join candidate "dummy.edn"))
                                   (.then (fn [_] candidate))
                                   (.catch (fn [_] nil))))
                             candidates)))
                     (.then (fn [results]
                              (some identity (js->clj results)))))))))

(defn tool-call-dir
  [config]
  (find-dir config "tool_calls"))

(defn policy-dir
  [config]
  (find-dir config "policies"))

(defn load-tool-call-contract!
  [config contract-id]
  (-> (tool-call-dir config)
      (.then (fn [dir]
               (if-not dir
                 (js/Promise.resolve nil)
                 (let [file-path (path/join dir (str contract-id ".edn"))]
                   (-> (loader/read-edn-file! file-path)
                       (.then (fn [contract]
                                (when (and contract
                                           (= :tool-call (:contract/kind contract)))
                                  contract)))
                       (.catch (fn [_] nil))))))))

(defn load-policy-contract!
  [config contract-id]
  (-> (policy-dir config)
      (.then (fn [dir]
               (if-not dir
                 (js/Promise.resolve nil)
                 (let [file-path (path/join dir (str contract-id ".edn"))]
                   (-> (loader/read-edn-file! file-path)
                       (.then (fn [contract]
                                (when (and contract
                                           (= :policy (:contract/kind contract)))
                                  contract)))
                       (.catch (fn [_] nil))))))))

(defn load-tool-call-contracts!
  [config contract-ids]
  (-> (js/Promise.all
        (clj->js
         (mapv (fn [id]
                 (load-tool-call-contract! config id))
               contract-ids)))
      (.then (fn [results]
               (->> (js->clj results)
                    (remove nil?)
                    vec)))))

(defn load-policy-contracts!
  [config contract-ids]
  (-> (js/Promise.all
        (clj->js
         (mapv (fn [id]
                 (load-policy-contract! config id))
               contract-ids)))
      (.then (fn [results]
               (->> (js->clj results)
                    (remove nil?)
                    vec)))))

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
  [contract granted-tool-ids]
  (let [deny-set (->> (get-in contract [:policy/denied] [])
                       (map tool-registry/normalize-tool-id)
                       (remove str/blank?)
                       set)]
    (vec (set/intersection deny-set granted-tool-ids))))

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
          tool->deny-count (apply merge-with +
                                   (for [denied per-contract-denied]
                                     (into {} (map (fn [t] [t 1]) denied))))
          must-deny (into {} (filter (fn [[_ cnt]]
                                      (= cnt (count contracts)))
                                    tool->deny-count))]
      (vec (keys must-deny)))))
