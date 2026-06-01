(ns open-hax.contracts.policy.fulfillment
  "Fulfillment evaluation for post-tool-call notification.

   Heritage: eta-mu.extensions.contract-runtime-v2.core

   Fulfillment contracts match tool results and emit notification/audit actions.
   All matching fulfillments fire — there is no strongest-wins reduction."
  (:require [clojure.string :as str]))

;; ── Match evaluation ──────────────────────────────────────────────────────────

(defn- match-params?
  [expected actual]
  (every? (fn [[k v]]
            (let [actual-val (get actual k)]
              (if (fn? v)
                (v actual-val)
                (= v actual-val))))
          expected))

(defn fulfillment-matches?
  "Does a fulfillment contract match a given tool-result map?

   tool-result — {:tool/name string
                  :tool/params map
                  :tool/output any
                  :tool/error  any
                  :tool/status any
                  :tool/code   any
                  :tool/message string}

   Match keys:
     :tool/name    — exact string match
     :tool/params  — all listed keys must match (fn predicates allowed)
     :tool/output  — exact match or fn predicate
     :tool/error?  — boolean match on presence of error"
  [fulfill tool-result]
  (let [match (:fulfillment/match fulfill {})]
    (every? (fn [[k v]]
              (cond
                (= k :tool/name)
                (= v (:tool/name tool-result))

                (= k :tool/params)
                (match-params? v (:tool/params tool-result {}))

                (= k :tool/output)
                (if (fn? v)
                  (v (:tool/output tool-result))
                  (= v (:tool/output tool-result)))

                (= k :tool/error?)
                (= v (boolean (:tool/error tool-result)))

                :else
                false))
            match)))

;; ── Message interpolation ─────────────────────────────────────────────────────

(defn interpolate-message
  "Replace {key} tokens in a template string with values from tool-result.

   Lookup order:
     1. :tool/params (keyword key)
     2. :tool/params (string key)
     3. tool-result top-level (keyword key)
     4. tool-result top-level (string key)
     5. Leave {key} as-is if not found"
  [template tool-result]
  (if (str/blank? template)
    template
    (str/replace template #"\{([^{}]+)\}"
                 (fn [[_ k]]
                   (cond
                     (contains? (:tool/params tool-result) (keyword k))
                     (str (get-in tool-result [:tool/params (keyword k)]))

                     (contains? (:tool/params tool-result) k)
                     (str (get-in tool-result [:tool/params k]))

                     (contains? tool-result (keyword k))
                     (str (get tool-result (keyword k)))

                     (contains? tool-result k)
                     (str (get tool-result k))

                     :else
                     (str "{" k "}"))))))

;; ── Public API ────────────────────────────────────────────────────────────────

(defn evaluate-fulfillments
  "Evaluate all fulfillment contracts against a tool result.

   Returns a vector of action maps for all matching fulfillments.
   All matches fire — there is no strongest-wins reduction.

   Each action:
     {:mode    :notify | :audit
      :message string (interpolated)
      :level   :info | :warn | :error
      :fulfill the-matching-contract}"
  [fulfills tool-result]
  (->> fulfills
       (filter #(fulfillment-matches? % tool-result))
       (map (fn [f]
              (let [template (or (:fulfillment/message f)
                                 (str (:tool/name tool-result) " completed"))
                    message  (interpolate-message template tool-result)]
                {:mode    (or (:fulfillment/mode f) :notify)
                 :message message
                 :level   (or (:fulfillment/level f) :info)
                 :fulfill f})))
       vec))
