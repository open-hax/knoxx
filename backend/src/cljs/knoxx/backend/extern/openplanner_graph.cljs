(ns knoxx.backend.extern.openplanner-graph
  "Mongo cursor boundary for read-only OpenPlanner graph-node events and edges."
  (:require [knoxx.backend.domain.graph-query :as graph]
            [knoxx.backend.extern.json :as xjson]))

(defn- ^:async find-rows!
  "Read a bounded cursor and decode native rows at this boundary."
  [collection selector projection sort-fields limit]
  (xjson/to-cljs
   (await (-> (.find collection (clj->js selector)
                      (clj->js {:projection projection :maxTimeMS 5000}))
              (.sort (clj->js sort-fields))
              (.limit limit)
              (.toArray)))))

(defn ^:async query!
  "Query canonical graph-node events with optional lake/type scope and bounded edges."
  [mongo payload]
  (let [plan (graph/query-plan payload)
        projection {"_id" 0 "project" 1 "extra" 1 "text" 1}
        seeds (await (find-rows! (.-events mongo) (:seed-filter plan) projection {"ts" -1 "id" 1} (:limit plan)))
        ids (vec (keep graph/node-id seeds))
        edges (if (and (seq ids) (pos? (:edge-limit plan)))
                (await (find-rows! (.-graphEdges mongo)
                                  (merge (:scope plan)
                                         {"$or" [{"source_node_id" {"$in" ids}}
                                                  {"target_node_id" {"$in" ids}}]})
                                  {"_id" 0 "source_node_id" 1 "target_node_id" 1
                                   "edge_kind" 1 "data" 1 "layer" 1}
                                  {"updated_at" -1 "id" 1}
                                  (:edge-limit plan))) [])
        neighbor-ids (vec (remove (set ids) (distinct (mapcat (juxt :source_node_id :target_node_id) edges))))
        neighbors (if (and (seq neighbor-ids) (< (count seeds) (:limit plan)))
                    (await (find-rows! (.-events mongo)
                                      (merge (:scope plan) {"kind" "graph.node"
                                                           "extra.node_id" {"$in" neighbor-ids}})
                                      projection {"ts" -1 "id" 1} (- (:limit plan) (count seeds)))) [])]
    (graph/query-result plan seeds neighbors edges)))
