(ns kms-ingestion.jobs.control-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [kms-ingestion.jobs.control :as control]))

;; ─────────────────────────────────────────────────────────────────────────────
;; control-delay-ms
;; ─────────────────────────────────────────────────────────────────────────────

(deftest control-delay-ms-ratios
  (testing "delay rises as cpu/target ratio increases"
    ;; max-load-per-core 1.0, host cores 4 → target = 4.0 cores
    ;; We pass cpu-cores directly; control-delay-ms uses available-cores internally.
    ;; The 2-arity overload accepts explicit max-load-per-core.
    ;; We exercise the ratio bands by choosing cpu values relative to known core count.
    ;; available-cores reads /proc/cpuinfo; in CI that's ≥1.
    ;; We normalise by using max-load-per-core = 999 to guarantee ratio < 0.25
    ;; for low cpu inputs, and max-load-per-core = 0.001 to guarantee ratio > 2.0
    ;; for any non-zero cpu.
    (let [very-low  (control/control-delay-ms 0.001 999)
          very-high (control/control-delay-ms 100.0 0.001)]
      (is (= 8    very-low)  "ratio < 0.25 → 8 ms")
      (is (= 2000 very-high) "ratio >> 2.0 → 2000 ms")))

  (testing "delay is monotonically non-decreasing with ratio"
    (let [ratios [0.1 0.3 0.6 0.8 0.95 1.1 1.4 1.8 2.5]
          ;; use fixed cores=100, max=1.0 so target=100
          delays (mapv #(control/control-delay-ms (* % 100) 1.0) ratios)]
      (is (= delays (sort delays))
          "delays must be non-decreasing as load ratio rises"))))

;; ─────────────────────────────────────────────────────────────────────────────
;; bounded-future-mapv
;; ─────────────────────────────────────────────────────────────────────────────

(deftest bounded-future-mapv-preserves-order
  (testing "output order matches input order regardless of parallelism"
    (let [input   (range 20)
          results (control/bounded-future-mapv 4 identity input)]
      (is (= (vec input) results))))
  (testing "parallelism 1 is equivalent to mapv"
    (let [input   (range 10)
          results (control/bounded-future-mapv 1 #(* % 2) input)]
      (is (= (mapv #(* % 2) input) results))))
  (testing "empty input returns empty"
    (is (= [] (control/bounded-future-mapv 4 identity [])))))

(deftest bounded-future-mapv-parallelism-clamped
  (testing "parallelism < 1 is treated as 1"
    (let [results (control/bounded-future-mapv 0 inc (range 5))]
      (is (= [1 2 3 4 5] results)))))

;; ─────────────────────────────────────────────────────────────────────────────
;; maybe-throttle! — contract-aware overload
;; ─────────────────────────────────────────────────────────────────────────────

(deftest maybe-throttle-disabled-does-not-sleep
  (testing "throttle-enabled? false never sleeps"
    (let [start (System/currentTimeMillis)]
      (control/maybe-throttle! "test-job" {:throttle-enabled? false
                                            :max-load-per-core 0.001})
      (let [elapsed (- (System/currentTimeMillis) start)]
        (is (< elapsed 200)
            "disabled throttle must return quickly")))))

;; ─────────────────────────────────────────────────────────────────────────────
;; openplanner backpressure state machine
;; ─────────────────────────────────────────────────────────────────────────────

(deftest backpressure-success-resets-state
  ;; Simulate a failure then a success; state should be cleared.
  (control/note-openplanner-failure! "t1" "timeout")
  (control/note-openplanner-success!)
  ;; After success, respect-openplanner-backpressure! should return immediately.
  (let [start (System/currentTimeMillis)]
    (control/respect-openplanner-backpressure! "t1")
    (is (< (- (System/currentTimeMillis) start) 200)
        "backpressure should be cleared after success")))

(deftest backpressure-streak-increases-delay
  ;; Each consecutive failure increases the streak counter.
  ;; We can't assert the actual sleep without a clock mock, but we can
  ;; assert the internal state transitions.
  (control/note-openplanner-success!) ; reset
  (dotimes [_ 3]
    (control/note-openplanner-failure! "t2" "err"))
  ;; After 3 failures the streak should be ≥ 3 and until-ms should be in the future.
  (let [state @(var-get (ns-resolve 'kms-ingestion.jobs.control 'openplanner-backpressure*))]
    (is (>= (:streak state) 3))
    (is (> (:until-ms state) (System/currentTimeMillis))))
  ;; Reset after test
  (control/note-openplanner-success!))
