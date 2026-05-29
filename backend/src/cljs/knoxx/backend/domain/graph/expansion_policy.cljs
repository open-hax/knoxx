(ns knoxx.backend.domain.graph.expansion-policy
  "Graph expansion policy seam.

   Defines the smallest internal seam needed to swap graph expansion
   behavior without changing the agent-facing graph query contract.

   A policy decides the bounded parameters for the four graph access
   shapes Knoxx exercises today:

   - search    — vector similarity memory search (`k` / `fetch-k`)
   - expand    — graph traversal queries (`limit` / `edge-limit` cost)
   - preview   — read-only session row fetches (`limit`)
   - writeback — indexing fan-out (`batch-size`)

   Each method takes a plain params map and returns a plain map of the
   bounded values. Policies are pure: no I/O, no domain side effects.
   This is the Category — the space of lawful expansion moves — leaving
   admissibility (contracts) and transport (infra) to other layers."
  (:require [clojure.string :as str]))

(defprotocol IGraphExpansionPolicy
  "Bounds the parameters of graph expansion access shapes.

   Implementations clamp caller-supplied parameters into the lawful
   range for each shape and return a plain map of bounded values."
  (bounded-search-params [policy params]
    "Bound vector memory search params. Returns {:k :fetch-k}.")
  (bounded-expand-params [policy params]
    "Bound graph traversal params. Returns {:limit :edge-limit :max-cost}.")
  (bounded-preview-params [policy params]
    "Bound read-only preview/row-fetch params. Returns {:limit}.")
  (bounded-writeback-params [policy params]
    "Bound indexing/writeback fan-out params. Returns {:batch-size}."))

(defn- clamp
  "Clamp n into [lo hi], falling back to fallback when n is not a number."
  [n lo hi fallback]
  (let [v (if (number? n) n fallback)]
    (max lo (min hi v))))

;; ---------------------------------------------------------------------------
;; Default policy — preserves the behavior baked into the query handlers
;; before this seam existed.
;; ---------------------------------------------------------------------------

(defrecord DefaultGraphExpansionPolicy []
  IGraphExpansionPolicy
  (bounded-search-params [_ {:keys [k]}]
    (let [k (clamp k 1 12 7)
          fetch-k (max k (min 36 (* k 3)))]
      {:k k
       :fetch-k fetch-k}))

  (bounded-expand-params [_ {:keys [limit edge-limit]}]
    (let [limit (clamp limit 1 60 15)
          edge-limit (when (some? edge-limit) edge-limit)
          max-cost (when edge-limit (/ 1.0 (max 0.01 edge-limit)))]
      (cond-> {:limit limit
               :edge-limit edge-limit}
        max-cost (assoc :max-cost max-cost))))

  (bounded-preview-params [_ {:keys [limit]}]
    {:limit (clamp limit 1 12 12)})

  (bounded-writeback-params [_ {:keys [batch-size]}]
    {:batch-size (clamp batch-size 1 16 3)}))

(defn default-expansion-policy
  "Construct the default graph expansion policy.

   Matches the legacy in-handler bounds: search `k` in 1..12 (default 7)
   with `fetch-k` up to 3x capped at 36; expand `limit` in 1..60
   (default 15) with edge cost derived as 1/edge-limit; preview `limit`
   in 1..12; writeback `batch-size` in 1..16 (default 3)."
  []
  (->DefaultGraphExpansionPolicy))

(defn expansion-policy?
  "Return true if x satisfies the graph expansion policy protocol."
  [x]
  (satisfies? IGraphExpansionPolicy x))

(defn normalize-policy-id
  "Normalize a policy id into a non-blank keyword, or nil."
  [id]
  (cond
    (keyword? id) id
    (and (string? id) (not (str/blank? id))) (keyword (str/trim id))
    :else nil))
