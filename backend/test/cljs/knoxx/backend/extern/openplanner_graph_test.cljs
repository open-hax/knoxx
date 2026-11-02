(ns knoxx.backend.extern.openplanner-graph-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.openplanner-graph :as graph]
            [knoxx.backend.extern.openplanner-sdk :as sdk]
            [knoxx.backend.infra.clients.openplanner :as client]
            [knoxx.backend.infra.clients.openplanner-mongo :as mongo-client]))

(defn- collection
  "Fake only the Mongo read cursor; capture every boundary selector and bound."
  [calls rows-for]
  #js {:find (fn [selector options]
               (let [request (js->clj selector :keywordize-keys true)
                     limit* (atom nil)
                     cursor #js {}]
                 (swap! calls conj {:selector request :options (js->clj options :keywordize-keys true)})
                 (aset cursor "sort" (fn [_sort] cursor))
                 (aset cursor "limit" (fn [limit] (reset! limit* limit) cursor))
                 (aset cursor "toArray" (fn []
                                          (swap! calls conj {:limit @limit*})
                                          (js/Promise.resolve (clj->js (rows-for request)))))
                 cursor))})

(deftest ^:async scoped-read-hydrates-incident-entities-without-rest-or-embeddings
  (let [calls (atom [])
        events (collection calls (fn [selector]
                                   (if (:extra.node_id selector)
                                     [{:project "linux-audit" :extra {:node_id "uid" :node_kind "audit.linux_principal" :label "UID 1002"}}]
                                     [{:project "linux-audit" :extra {:node_id "codex" :node_kind "audit.harness" :label "codex"}}])))
        edges (collection calls (fn [_] [{:source_node_id "uid" :target_node_id "codex" :edge_kind "mapped_to_harness"}]))
        result (await (graph/query! #js {:events events :graphEdges edges}
                                   {:q "codex" :lakes ["linux-audit"] :k 2 :edgeLimit 1}))]
    (is (= 2 (count (:nodes result))))
    (is (= 1 (count (:edges result))))
    (is (= "structural" (:mode result)))
    (doseq [call (filter :selector @calls)]
      (is (= {:$in ["linux-audit"]} (get-in call [:selector :project])))
      (is (= 5000 (get-in call [:options :maxTimeMS]))))
    (is (= [2 1 1] (mapv :limit (filter :limit @calls))))))

(deftest ^:async zero-edge-budget-does-not-touch-edge-collection
  (let [calls (atom [])
        events (collection calls (constantly []))
        result (await (graph/query! #js {:events events}
                                   {:q "nothing" :edgeLimit 0}))]
    (is (empty? (:nodes result)))
    (is (empty? (:edges result)))
    (is (= 1 (count (filter :selector @calls))))))

(deftest ^:async direct-client-dispatches-to-mongo-graph-boundary
  (with-redefs [sdk/graph-memory (fn [payload] (js/Promise.resolve {:query (:q payload) :mode "structural"}))]
    (is (= {:query "codex" :mode "structural"}
           (await (client/graph-memory! (mongo-client/client {} nil) {:q "codex"}))))))
