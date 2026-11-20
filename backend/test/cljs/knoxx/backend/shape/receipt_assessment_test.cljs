(ns knoxx.backend.shape.receipt-assessment-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.law.publication-receipts :as receipts]
            [knoxx.backend.law.publication-reconciler :as correlation]
            [knoxx.backend.law.wiki-publication :as law]
            [knoxx.backend.shape.wiki-publication :as wire]))

(deftest absent-primary-identity-is-not-asserted-from-correlation
  (doseq [receipt [{:receipt/type :publication/noop}
                   {:receipt/type :publication/blocked :blockers []}
                   {:receipt/type :publication/failed :failure/reason "failure" :failure/drift? true}
                   {:receipt/type :publication/materialized :materialized/revision "r1"
                    :materialized/path "/a" :idempotency/key "k"}]]
    (let [correlated (correlation/correlate (receipts/assert-receipt! receipt)
                                            (correlation/correlation {:trigger/id :wiki/publish :trigger/origin :route
                                                                      :publication/id :docs/a} "r1"))
          projection (law/assert-receipt-wire! (wire/receipt->wire correlated))]
      (is (not (contains? projection :publication)))
      (is (= "docs/a" (get-in projection [:correlation :publication]))))))

(deftest empty-wire-map-is-accepted
  (is (= {} (law/assert-receipt-wire! {}))))
