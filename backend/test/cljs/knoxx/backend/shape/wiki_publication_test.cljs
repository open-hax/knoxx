(ns knoxx.backend.shape.wiki-publication-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.law.wiki-publication :as law]
            [knoxx.backend.shape.wiki-publication :as wire]))

(deftest correlation-preserves-receipt-identity
  (is (= {:type "publication/materialized"
          :publication "docs/actual"
          :revision "actual-revision"
          :correlation {:publication "docs/requested"
                        :revision "requested-revision"
                        :trigger "wiki/publish"
                        :origin "route"}}
         (wire/receipt->wire {:receipt/type :publication/materialized
                              :publication/id :docs/actual
                              :revision "actual-revision"
                              :correlation/publication :docs/requested
                              :correlation/revision "requested-revision"
                              :correlation/trigger :wiki/publish
                              :correlation/origin :route}))))

(deftest receipt-contract-rejects-invalid-correlation
  (is (thrown? js/Error
               (law/assert-receipt-wire! {:publication "docs/actual"
                                          :correlation {:publication 42}}))))
