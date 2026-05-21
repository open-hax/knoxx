(ns knoxx.backend.tools.dispatch
  "Call routing against a pre-resolved ResolvedToolSuite.

   The dispatch layer is stateless and pure: it only reads from the suite
   attached to run-state at turn start. It never re-resolves or re-reads contracts."
  (:require [knoxx.backend.tools.registry :as tool-registry]))

(defn tool-allowed?
  "True when tool-id is in the resolved suite."
  [suite tool-id]
  (let [id (tool-registry/normalize-tool-id tool-id)]
    (contains? (:tools suite) id)))

(defn allowed-tool-ids
  "All granted tool IDs from the resolved suite."
  [suite]
  (vec (sort (keys (:tools suite)))))

(defn tool-call-shape
  "Return call-shape map for tool-id from the suite, or nil."
  [suite tool-id]
  (let [id (tool-registry/normalize-tool-id tool-id)]
    (get-in suite [:tools id :call-shape])))

(defn tool-provenance
  "Return provenance map for tool-id from the suite, or nil."
  [suite tool-id]
  (let [id (tool-registry/normalize-tool-id tool-id)]
    (get-in suite [:tools id :provenance])))

(defn denied-reason
  "Return the denial reason string for tool-id, or nil if not denied."
  [suite tool-id]
  (let [id (tool-registry/normalize-tool-id tool-id)]
    (get (:denied-reasons suite) id)))

(defn denied-tool?
  "True when tool-id is in the denied list."
  [suite tool-id]
  (let [id (tool-registry/normalize-tool-id tool-id)]
    (contains? (set (:denied suite)) id)))

(defn filter-tool-ids
  "Return only tool-ids that are granted (not denied) by the suite."
  [suite tool-ids]
  (let [allowed-set (set (keys (:tools suite)))]
    (vec (filter allowed-set tool-ids))))

(defn validate-call
  "Validate a tool call against the resolved suite.
   Returns {:ok true} or {:ok false :error <string>}."
  [suite tool-id]
  (let [id (tool-registry/normalize-tool-id tool-id)]
    (cond
      (contains? (:tools suite) id) {:ok true}
      (contains? (set (:denied suite)) id) {:ok false :error (str "Tool denied: " (denied-reason suite id))}
      :else {:ok false :error (str "Tool unknown: " id)})))