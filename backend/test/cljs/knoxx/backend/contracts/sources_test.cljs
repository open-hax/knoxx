(ns knoxx.backend.contracts.sources-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as loader]
            [knoxx.backend.domain.contracts.sources :as sut]))

(def openplanner-source-contract
  {:contract/id "openplanner-memory"
   :contract/kind :source
   :source/id :source/openplanner-memory
   :source/provider :openplanner
   :source/hydration {:strategy :memory-search
                      :mode :triggered
                      :k 6}})

(deftest normalize-source-id-test
  (is (= :source/openplanner-memory (sut/normalize-source-id :source/openplanner-memory)))
  (is (= :source/openplanner-memory (sut/normalize-source-id :openplanner_memory)))
  (is (= :source/openplanner-memory (sut/normalize-source-id "source/openplanner-memory")))
  (is (= :source/openplanner-memory (sut/normalize-source-id "openplanner_memory"))))

(deftest compose-source-refs-dedupes-and-deep-merges-by-source-id
  (with-redefs [loader/contract-sync (fn [_ contract-class contract-id]
                                       (when (and (= "sources" contract-class)
                                                  (= "openplanner-memory" contract-id))
                                         openplanner-source-contract))
                loader/load-all-contracts-sync (fn [_] [])]
    (let [resolved (sut/compose-source-refs
                    {}
                    [:source/openplanner-memory]
                    [{:source/ref :source/openplanner-memory
                      :hydration {:k 10}
                      :filters {:session "session-a"}}])
          source (first resolved)]
      (testing "one source survives duplicate refs"
        (is (= 1 (count resolved)))
        (is (= :source/openplanner-memory (:source/id source))))
      (testing "contract defaults and later overrides merge"
        (is (= {:strategy :memory-search
                :mode :triggered
                :k 10}
               (:source/hydration source)))
        (is (= {:session "session-a"}
               (:source/filters source)))))))
