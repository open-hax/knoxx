(ns knoxx.backend.isolated-social-knowledge-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.openplanner-sdk :as sdk]
            [knoxx.backend.infra.clients.openplanner :as planner]
            [knoxx.backend.infra.openplanner.knowledge :as knowledge]
            [knoxx.backend.infra.openplanner.knowledge-scope :as identity]))

(def ^:private target
  {:social-knowledge-mongo-uri "mongodb://private.invalid:27017"
   :social-knowledge-mongo-db "knoxx_private_test"})

(defn- fixture-sdk
  [calls]
  (let [cursor #js {:toArray (constantly #js [])}]
    (doseq [method ["sort" "skip" "limit"]]
      (aset cursor method (constantly cursor)))
    #js {:mongo #js {:db #js {:collection (fn [name]
                                            (swap! calls conj [:query name])
                                            #js {:countDocuments (constantly 0)
                                                 :find (constantly cursor)})}}
         :searchVector (fn [body]
                         (swap! calls conj [:vector (aget body "project")])
                         #js {:result #js {:hits #js []}})
         :ingestEvents (fn [rows]
                         (swap! calls conj [:ingest (alength rows)])
                         #js {:ok true})
         :close (fn [] (swap! calls conj :closed))}))

(defn- ^:async refusal-code
  [invoke]
  (try
    (await (invoke))
    nil
    (catch :default error (:code (ex-data error)))))

(deftest ^:async isolated-knowledge-uses-only-its-explicit-mongo-sdk
  (let [open-store! sdk/isolated-knowledge-store!
        calls (atom [])
        open! (fn [destination]
                (swap! calls conj [:open destination])
                (fixture-sdk calls))
        generic! (fn [& _] (throw (js/Error. "Shared planner accessed")))
        search! (knowledge/search-execute nil)
        current-db (or (aget js/process.env "MONGODB_DB")
                       (aget js/process.env "OPENPLANNER_MONGODB_DB")
                       "openplanner")]
    (try
      (with-redefs [sdk/isolated-knowledge-store! (fn
                                                  ([config] (open-store! config open!))
                                                  ([config opener] (open-store! config opener)))
                    sdk/mongo-query generic!
                    sdk/vector-search! generic!
                    planner/client generic!
                    planner/mongo-query! generic!
                    identity/principal (constantly {:actor-id "duck" :org-slug "open-hax"})]
        (is (= "social_knowledge_unavailable"
               (await (refusal-code #(search! nil {} "absent" {:query "fixture"} nil nil nil)))))
        (is (= "social_knowledge_database_overlap"
               (await (refusal-code #(search! nil (assoc target :social-knowledge-mongo-db current-db)
                                              "overlap" {:query "fixture"} nil nil nil)))))
        (is (empty? @calls))
        (let [result (await (search! nil target "search" {:query "fixture"} nil nil nil))
              store (await (sdk/isolated-knowledge-store! target))]
          (is (zero? (alength (aget result "details" "entries"))))
          (is (:ok (await ((:events! store) [{:id "fixture"}]))))
          (is (= [[:open {:uri "mongodb://private.invalid:27017" :db-name "knoxx_private_test"}]
                  [:query "events"] [:vector "knoxx-knowledge"] [:ingest 1]]
                 @calls))))
      (finally
        (await (sdk/close-isolated-sdk!))))
    (is (= :closed (last @calls)))))
