(ns knoxx.backend.law.publication-reconciler-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.law.publication-reconciler :as law]))

(def ^:private valid-trigger
  {:trigger/id :test/reconcile
   :trigger/origin :route
   :publication/id :knoxx.docs/probe-es})

(deftest trigger-contract-admits-one-lawful-demand
  (is (= valid-trigger (law/assert-trigger! valid-trigger))))

(deftest trigger-contract-refuses-undeclared-or-missing-data
  (doseq [[label trigger]
          [["missing publication" (dissoc valid-trigger :publication/id)]
           ["missing origin" (dissoc valid-trigger :trigger/origin)]
           ["undeclared field" (assoc valid-trigger :trigger/note "hi")]
           ["unknown origin" (assoc valid-trigger :trigger/origin :carrier-pigeon)]
           ["unqualified publication id"
            (assoc valid-trigger :publication/id :probe-es)]
           ["unqualified trigger id"
            (assoc valid-trigger :trigger/id "test/reconcile")]]]
    (testing label
      (is (thrown? js/Error (law/assert-trigger! trigger))))))

(deftest correlation-carries-trigger-publication-and-revision
  (is (= {:correlation/trigger :test/reconcile
          :correlation/origin :route
          :correlation/publication :knoxx.docs/probe-es
          :correlation/revision "rev-1"}
         (law/correlation valid-trigger "rev-1")))
  (testing "a plan that never resolved a revision records nil, not an invention"
    (is (nil? (:correlation/revision (law/correlation valid-trigger nil)))))
  (testing "a blank revision is not a revision"
    (is (thrown? js/Error (law/correlation valid-trigger "  ")))))

(deftest correlate-stamps-and-validates
  (let [receipt {:receipt/type :publication/noop :reason :publication-not-public}
        correlation (law/correlation valid-trigger "rev-1")
        stamped (law/correlate receipt correlation)]
    (is (= :publication/noop (:receipt/type stamped)))
    (is (= "rev-1" (:correlation/revision stamped)))
    (is (= :knoxx.docs/probe-es (:correlation/publication stamped)))
    (is (thrown? js/Error
                 (law/correlate receipt (dissoc correlation :correlation/trigger))))))
