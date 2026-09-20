(ns knoxx.frontend.pages.documents.logic-test
  "Written FIRST (TDD) — pure-logic contract for the DocumentsPage port
  (ingestion rate/ETA math, progress-sample windowing, selection toggles,
  restart decision helpers from src/pages/DocumentsPage.tsx)."
  (:require [cljs.test :as test]
            [knoxx.frontend.pages.documents.logic :as logic]))

(test/deftest format-eta-contract
  (test/is (= "Estimating..." (logic/format-eta 0)))
  (test/is (= "Estimating..." (logic/format-eta js/Infinity)))
  (test/is (= "45s" (logic/format-eta 45)))
  (test/is (= "2m 5s" (logic/format-eta 125))))

(test/deftest chunks-per-sec-from-samples
  (test/testing "two or more samples → rate over the window"
    (test/is (= 5 (logic/chunks-per-sec
              [{:ts 0 :processed 0} {:ts 10000 :processed 50}]
              nil 0))))
  (test/testing "fewer than two samples falls back to processed/elapsed"
    (test/is (= 10 (logic/chunks-per-sec [] {:processedChunks 100} 10)))
    (test/is (= 0 (logic/chunks-per-sec [] nil 10)))
    (test/is (= 0 (logic/chunks-per-sec [{:ts 0 :processed 1}] nil 0))))
  (test/testing "regressing counts clamp to zero"
    (test/is (= 0 (logic/chunks-per-sec
              [{:ts 0 :processed 50} {:ts 10000 :processed 10}]
              nil 0)))))

(test/deftest remaining-and-eta
  (test/is (= 40 (logic/remaining-chunks {:totalChunks 100 :processedChunks 60})))
  (test/is (= 0 (logic/remaining-chunks {:totalChunks 50 :processedChunks 80})) "clamped")
  (test/is (= 0 (logic/remaining-chunks nil)))
  (test/is (= 20 (logic/eta-seconds 100 5)))
  (test/is (= 0 (logic/eta-seconds 100 0)) "no rate → 0 (renders Estimating...)"))

(test/deftest progress-sample-windowing
  (let [now 100000
        samples [{:ts (- now 70000) :processed 1}
                 {:ts (- now 30000) :processed 5}]
        next-samples (logic/push-sample samples now 9)]
    (test/is (= [{:ts (- now 30000) :processed 5} {:ts now :processed 9}]
           next-samples)
        "older-than-60s samples dropped, new appended"))
  (test/testing "capped at 120 samples"
    (let [many (vec (for [i (range 150)] {:ts 1000000 :processed i}))]
      (test/is (= 120 (count (logic/push-sample many 1000000 999)))))))

(test/deftest selection-toggles
  (test/is (= #{"a"} (logic/toggle-doc #{} "a")))
  (test/is (= #{} (logic/toggle-doc #{"a"} "a")))
  (let [docs [{:relativePath "a"} {:relativePath "b"}]]
    (test/is (= #{"a" "b"} (logic/toggle-all #{} docs)) "select all when not all selected")
    (test/is (= #{"a" "b"} (logic/toggle-all #{"a"} docs)))
    (test/is (= #{} (logic/toggle-all #{"a" "b"} docs)) "clear when all selected")))

(test/deftest restart-decision-helpers
  (test/is (logic/should-force-fresh? {:stale true :canResumeForum true}))
  (test/is (not (logic/should-force-fresh? {:stale false :canResumeForum true})))
  (test/is (logic/no-active-run? {:active false :canResumeForum false}))
  (test/is (not (logic/no-active-run? {:active true :canResumeForum false})))
  (test/is (not (logic/no-active-run? {:active false :canResumeForum true})))
  (test/is (= "Ingestion was stalled; started fresh forum ingestion from scratch."
         (logic/restart-message true)))
  (test/is (= "Ingestion restart requested. Resuming from saved progress..."
         (logic/restart-message false))))

(test/deftest no-active-restart-error?
  (test/is (logic/no-active-restart-error? "No active ingestion to restart"))
  (test/is (logic/no-active-restart-error? "Error: No active ingestion to restart (400)"))
  (test/is (not (logic/no-active-restart-error? "boom"))))
