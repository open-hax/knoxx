(ns knoxx.backend.infra.event-runtime-disable-test
  "A disabled process must not report that it started or reset anything.

   `reload!` used to return a hardcoded `{:ok true :action \"reload\"}`, so the
   admin reset route answered 200 while `start!` had already declined to arm
   anything. The outcome was computed correctly one level down and then thrown
   away — which is the shape of bug worth a regression test, because everything
   involved was individually behaving."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.event-runtime :as event-runtime]))

(def ^:private disabled-config {:event-runtimes-disabled? true})
(def ^:private enabled-config {:event-runtimes-disabled? false})

(deftest disabled?-reads-the-resolved-config
  (is (true? (event-runtime/disabled? disabled-config)))
  (is (false? (event-runtime/disabled? enabled-config)))
  (testing "and an absent key means enabled — the flag never defaults on"
    (is (false? (event-runtime/disabled? {})))))

(deftest start-refuses-and-says-so
  (is (= :disabled (event-runtime/start! disabled-config))))

(deftest ^:async reload-refuses-rather-than-reporting-success
  (let [summary (await (event-runtime/reload! disabled-config))]
    (testing "the caller can tell nothing was started"
      (is (= :disabled (:status summary)))
      (is (false? (:ok summary))))))

(deftest ^:async reset-runtime-inherits-the-refusal
  (testing "reset-runtime! delegates to reload!, so the admin reset route sees it"
    (let [summary (await (event-runtime/reset-runtime! disabled-config))]
      (is (= :disabled (:status summary)))
      (is (false? (:ok summary))))))
