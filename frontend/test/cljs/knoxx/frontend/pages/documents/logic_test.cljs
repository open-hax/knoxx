(ns knoxx.frontend.pages.documents.logic-test
  "Written FIRST (TDD) — pure-logic contract for the DocumentsPage port
  (ingestion rate/ETA math, progress-sample windowing, selection toggles,
  restart decision helpers from src/pages/DocumentsPage.tsx)."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.frontend.pages.documents.logic :as logic]))

(deftest format-eta-contract
  (is (= "Estimating..." (logic/format-eta 0)))
  (is (= "Estimating..." (logic/format-eta js/Infinity)))
  (is (= "45s" (logic/format-eta 45)))
  (is (= "2m 5s" (logic/format-eta 125))))

(deftest chunks-per-sec-from-samples
  (testing "two or more samples → rate over the window"
    (is (= 5 (logic/chunks-per-sec
              [{:ts 0 :processed 0} {:ts 10000 :processed 50}]
              nil 0))))
  (testing "fewer than two samples falls back to processed/elapsed"
    (is (= 10 (logic/chunks-per-sec [] {:processedChunks 100} 10)))
    (is (= 0 (logic/chunks-per-sec [] nil 10)))
    (is (= 0 (logic/chunks-per-sec [{:ts 0 :processed 1}] nil 0))))
  (testing "regressing counts clamp to zero"
    (is (= 0 (logic/chunks-per-sec
              [{:ts 0 :processed 50} {:ts 10000 :processed 10}]
              nil 0)))))

(deftest remaining-and-eta
  (is (= 40 (logic/remaining-chunks {:totalChunks 100 :processedChunks 60})))
  (is (= 0 (logic/remaining-chunks {:totalChunks 50 :processedChunks 80})) "clamped")
  (is (= 0 (logic/remaining-chunks nil)))
  (is (= 20 (logic/eta-seconds 100 5)))
  (is (= 0 (logic/eta-seconds 100 0)) "no rate → 0 (renders Estimating...)"))

(deftest progress-sample-windowing
  (let [now 100000
        samples [{:ts (- now 70000) :processed 1}
                 {:ts (- now 30000) :processed 5}]
        next-samples (logic/push-sample samples now 9)]
    (is (= [{:ts (- now 30000) :processed 5} {:ts now :processed 9}]
           next-samples)
        "older-than-60s samples dropped, new appended"))
  (testing "capped at 120 samples"
    (let [many (vec (for [i (range 150)] {:ts 1000000 :processed i}))]
      (is (= 120 (count (logic/push-sample many 1000000 999)))))))

(deftest selection-toggles
  (is (= #{"a"} (logic/toggle-doc #{} "a")))
  (is (= #{} (logic/toggle-doc #{"a"} "a")))
  (let [docs [{:relativePath "a"} {:relativePath "b"}]]
    (is (= #{"a" "b"} (logic/toggle-all #{} docs)) "select all when not all selected")
    (is (= #{"a" "b"} (logic/toggle-all #{"a"} docs)))
    (is (= #{} (logic/toggle-all #{"a" "b"} docs)) "clear when all selected")))

(deftest restart-decision-helpers
  (is (logic/should-force-fresh? {:stale true :canResumeForum true}))
  (is (not (logic/should-force-fresh? {:stale false :canResumeForum true})))
  (is (logic/no-active-run? {:active false :canResumeForum false}))
  (is (not (logic/no-active-run? {:active true :canResumeForum false})))
  (is (not (logic/no-active-run? {:active false :canResumeForum true})))
  (is (= "Ingestion was stalled; started fresh forum ingestion from scratch."
         (logic/restart-message true)))
  (is (= "Ingestion restart requested. Resuming from saved progress..."
         (logic/restart-message false))))

(deftest no-active-restart-error?
  (is (logic/no-active-restart-error? "No active ingestion to restart"))
  (is (logic/no-active-restart-error? "Error: No active ingestion to restart (400)"))
  (is (not (logic/no-active-restart-error? "boom"))))
