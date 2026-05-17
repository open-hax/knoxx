(ns open-hax.contracts.policy.gate
  "Flat policy gate evaluation for tool-call gating.

   Heritage: eta-mu.extensions.contract-runtime-v2.core

   Policy gate contracts use :policy/match maps to match tool calls
   and :policy/action to determine the outcome (:block, :warn, :note, :allow).

   This is simpler than the tree-shaped policy evaluator — it's designed
   for fast, flat lookups on every tool call."
  (:require [clojure.string :as str]))

;; ── Match evaluation ──────────────────────────────────────────────────────────

(defn- match-params?
  "Check if all expected param key-value pairs match the actual params.
   If a value is a fn, it's called with the actual value (predicate match).
   Otherwise, exact equality is required."
  [expected actual]
  (every? (fn [[k v]]
            (let [actual-val (get actual k)]
              (if (fn? v)
                (v actual-val)
                (= v actual-val))))
          expected))

(defn policy-matches?
  "Does a policy-gate contract match a given tool-call map?

   tool-call — {:tool/name string :tool/params map}

   Match keys:
     :tool/name   — exact string match
     :tool/params — all listed keys must match (fn predicates allowed)"
  [policy tool-call]
  (let [match (:policy/match policy {})]
    (every? (fn [[k v]]
              (cond
                (= k :tool/name)
                (= v (:tool/name tool-call))

                (= k :tool/params)
                (match-params v (:tool/params tool-call {}))

                :else
                false))
            match)))

;; ── Severity ranking ─────────────────────────────────────────────────────────

(def ^:private action-severity
  "Numeric severity for policy actions. Higher = stronger."
  {:block 3
   :warn  2
   :note  1
   :allow 0})

(defn strongest-action
  "Given a seq of action keywords, return the one with highest severity."
  [actions]
  (let [n (apply max 0 (map #(get action-severity % 0) actions))]
    (some (fn [[k v]] (when (= v n) k)) action-severity)))

;; ── TTL filtering ─────────────────────────────────────────────────────────────

(defn- policy-active?
  "Is this policy gate still within its TTL window?"
  [policy now-ms loaded-at]
  (let [ttl (:policy/ttl-ms policy)]
    (if (and ttl now-ms loaded-at)
      (< (- now-ms loaded-at) ttl)
      true)))

;; ── Public API ────────────────────────────────────────────────────────────────

(defn evaluate-gates
  "Evaluate all policy-gate contracts against a tool call.

   Returns:
     {:action  :block | :warn | :note | :allow
      :reason  string?
      :policy  matching-policy-or-nil
      :matches vector-of-all-matching-policies}

   When multiple policies match, the strongest action wins."
  ([gates tool-call]
   (evaluate-gates gates tool-call nil nil))
  ([gates tool-call now-ms loaded-at]
   (let [active  (filter #(policy-active? % now-ms loaded-at) gates)
         matches (filter #(policy-matches? % tool-call) active)]
     (if (empty? matches)
       {:action :allow :reason nil :policy nil :matches []}
       (let [action (strongest-action (map :policy/action matches))
             winner (first (filter #(= action (:policy/action %)) matches))]
         {:action  action
          :reason  (:policy/reason winner)
          :policy  winner
          :matches (vec matches)})))))
