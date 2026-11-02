(ns knoxx.backend.domain.graph-query
  "Pure query planning and structural graph response shaping."
  (:require [clojure.string :as str]
            [knoxx.backend.law.graph-query :as law]))

(defn query-plan
  "Plan literal metadata search and a bounded one-hop neighborhood, without vectors."
  [payload]
  (law/request! payload)
  (let [{:keys [q k lakes nodeTypes edgeLimit includeText]} payload
        pattern (str/escape (str/trim q)
                            (zipmap "\\^$.*+?()[]{}|"
                                    (map #(str "\\" %) "\\^$.*+?()[]{}|")))
        scope (if (seq lakes) {"project" {"$in" lakes}} {})
        fields ["extra.node_id" "extra.label" "extra.preview" "extra.path"
                "extra.harness_name" "extra.executable_path"]]
    {:query (str/trim q) :pattern pattern :scope scope :limit (or k 15)
     :edge-limit (if (some? edgeLimit) edgeLimit 30)
     :include-text? (not= false includeText)
     :seed-filter (cond-> (merge scope {"kind" "graph.node"
                                       "$or" (mapv #(hash-map % {"$regex" pattern "$options" "i"}) fields)})
                    (seq nodeTypes) (assoc "extra.node_kind" {"$in" nodeTypes}))}))

(defn node-id
  "Return the canonical identity of a graph-node event."
  [row]
  (get-in row [:extra :node_id]))

(defn- node-view
  "Shape an admitted graph node without claiming a vector similarity score."
  [plan row]
  (let [extra (:extra row)]
    (cond-> {:id (node-id row) :label (or (:label extra) (node-id row))
             :nodeType (:node_kind extra) :lake (or (:lake extra) (:project row))
             :data extra}
      (:include-text? plan) (assoc :text (or (:preview extra) (:text row) "")))))

(defn query-result
  "Return only edges whose endpoints are in the bounded hydrated result."
  [plan seeds neighbors edges]
  (let [rows (->> (concat seeds neighbors)
                  (filter #(string? (node-id %)))
                  (reduce (fn [acc row]
                            (if (some #(= (node-id row) (node-id %)) acc) acc (conj acc row))) [])
                  (take (:limit plan)) vec)
        ids (set (map node-id rows))
        included-edges (->> edges
                            (filter #(and (ids (:source_node_id %)) (ids (:target_node_id %)))))
        edge-views (mapv (fn [edge] {:source (:source_node_id edge) :target (:target_node_id edge)
                                     :edgeType (:edge_kind edge) :data (:data edge) :layer (:layer edge)})
                         (take (:edge-limit plan) included-edges))]
    (law/result! {:query (:query plan) :mode "structural" :clusters []
                  :nodes (mapv #(node-view plan %) rows) :edges edge-views
                  :stats {:vectorHits 0 :seeds (count seeds) :visited (count rows)
                          :edges (count edge-views) :traversal "one-hop"}})))
