(ns knoxx.backend.quality-labels-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [knoxx.backend.quality-labels :as quality-labels]))

(deftest crossed-records-are-hard-excluded
  (testing "bad quality labels are detected across legacy label shapes"
    (doseq [record [{:openplannerLabels {:quality "bad"}}
                   {:extra {:openplanner_labels {:reaction_emojis ["❌"]}}}
                   {:metadata {:quality_label "bad"}}
                   {:openplannerLabels {:labels ["emoji:✖️"]}}
                   {:openplannerLabels {:history [{:emoji "❎"}]}}]]
      (is (quality-labels/bad? record))
      (is (not (quality-labels/not-bad? record))))))

(deftest bad-dominates-good
  (testing "a crossed record never passes even if it also has a checkmark"
    (let [record {:openplannerLabels {:reaction_emojis ["✅" "❌"]}}]
      (is (= "bad" (quality-labels/quality-label record)))
      (is (= [] (quality-labels/drop-bad [record]))))))

(deftest checkmarked-records-sort-before-unlabeled
  (testing "context hydration order prefers good labels, then non-bad records"
    (let [records [{:id "plain-1"}
                   {:id "bad" :openplannerLabels {:quality "bad"}}
                   {:id "good-1" :openplannerLabels {:quality "good"}}
                   {:id "plain-2"}
                   {:id "good-2" :openplannerLabels {:reaction_emojis ["✔️"]}}]]
      (is (= ["good-1" "good-2" "plain-1" "plain-2"]
             (mapv :id (quality-labels/good-first-then-not-bad records)))))))
