(ns knoxx.mutation-test-test
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [knoxx.mutation-test :as mutation]))

(deftest form-mutations-operate-on-s-expressions
  (testing "if, comparison, arithmetic, and literal operators are data rewrites"
    (is (= [{:operator :if-test-negation
             :replacement '(if (not (= x 1)) :yes :no)}]
           (mutation/form-mutations '(if (= x 1) :yes :no))))
    (is (= [{:operator :comparison-flip
             :replacement '(<= x 10)}]
           (mutation/form-mutations '(< x 10))))
    (is (= [{:operator :arithmetic-operator-flip
             :replacement '(- x 1)}]
           (mutation/form-mutations '(+ x 1))))
    (is (= [{:operator :boolean-literal-flip
             :replacement false}]
           (mutation/form-mutations true)))))

(deftest discover-mutants-attaches-source-locations
  (let [source "(ns demo.core)\n(defn f [x]\n  (if (= x 1)\n    (+ x 1)\n    false))\n"
        mutants (mutation/assign-mutant-ids
                 (mutation/mutants-in-source "demo/core.cljs" source))]
    (is (= "m0001" (:id (first mutants))))
    (is (some #(and (= :if-test-negation (:operator %))
                    (= 3 (:line %))
                    (= 3 (:column %)))
              mutants))
    (is (some #(= :comparison-flip (:operator %)) mutants))
    (is (some #(= :arithmetic-operator-flip (:operator %)) mutants))
    (is (some #(= :boolean-literal-flip (:operator %)) mutants))))

(deftest apply-mutant-to-source-rewrites-only-the-target-form
  (let [source "(ns demo.core)\n(defn f [x]\n  (if (= x 1)\n    (+ x 1)\n    false))\n"
        mutant (->> (mutation/mutants-in-source "demo/core.cljs" source)
                    mutation/assign-mutant-ids
                    (filter #(= :if-test-negation (:operator %)))
                    first)
        mutated (mutation/apply-mutant-to-source source mutant)]
    (is (str/includes? mutated "(if (not (= x 1)) (+ x 1) false)"))
    (is (str/includes? mutated "(ns demo.core)"))))

(deftest apply-mutant-to-source-matches-anonymous-functions-after-rereading
  (let [neighbor "(defn untouched [ready values] (if ready (map #(str %) values) []))\n"
        source (str "(defn decide [ready values] (if ready (map #(str %) values) []))\n" neighbor)
        mutant (->> (mutation/mutants-in-source "demo/core.cljs" source)
                    mutation/assign-mutant-ids
                    (filter #(= :if-test-negation (:operator %)))
                    first)
        changed-source (str/replace-first source "#(str %)" "#(name %)")
        mutated (mutation/apply-mutant-to-source source mutant)]
    (is (str/includes? mutated "(if (not ready)"))
    (is (str/ends-with? mutated neighbor))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Mutant location no longer matches source"
                          (mutation/apply-mutant-to-source changed-source mutant)))))

(deftest with-mutated-source-mutates-in-place-then-restores
  (let [tmp-dir (.toFile (java.nio.file.Files/createTempDirectory "knoxx-mutation-test" (make-array java.nio.file.attribute.FileAttribute 0)))
        src-dir (io/file tmp-dir "src/cljs")
        source-file (io/file src-dir "demo/core.cljs")
        source "(ns demo.core)\n(defn f [x] (if (= x 1) true false))\n"
        opts {:src-dir (.getPath src-dir)}]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file source)
      (let [mutant (->> (mutation/discover-mutants {:src-dir (.getPath src-dir)
                                                    :include-regex "demo/core.cljs$"
                                                    :limit 0})
                        (filter #(= :if-test-negation (:operator %)))
                        first)
            during (atom nil)]
        (is (= (.getPath source-file)
               (.getPath (mutation/mutant-source-path opts mutant))))

        ;; The mutant is only in the tracked file while the body runs.
        (mutation/with-mutated-source! opts mutant #(reset! during (slurp source-file)))
        (is (str/includes? @during "(not (= x 1))"))
        (is (= source (slurp source-file)))

        ;; A throwing body must still restore, or the run leaves the tree dirty.
        (is (thrown? clojure.lang.ExceptionInfo
                     (mutation/with-mutated-source! opts mutant
                       #(throw (ex-info "boom" {})))))
        (is (= source (slurp source-file))))
      (finally
        (doseq [f (reverse (file-seq tmp-dir))]
          (.delete f))))))

(deftest campaign-partitions-are-disjoint-and-stable
  (let [mutants (mutation/assign-mutant-ids
                 (mapv #(hash-map :relative-path "a.cljs" :line % :column 1 :operator :boolean-literal-flip :original "true" :replacement "false") (range 1000)))
        batches (mapv #(mutation/partition-mutants mutants {:batch-index % :batch-count 4 :limit 250}) (range 4))]
    (is (every? #(= 250 (count %)) batches))
    (is (= 1000 (count (set (map :fingerprint (mapcat identity batches))))))
    (is (= (first (nth batches 2)) (first (mutation/partition-mutants mutants {:batch-index 2 :batch-count 4 :limit 1}))))))

(deftest invalid-execution-never-counts-as-test-kill
  (let [green "Ran 10 tests containing 40 assertions.\n0 failures, 0 errors."
        red "Ran 10 tests containing 40 assertions.\n1 failures, 0 errors."]
    (is (mutation/passed-tests? {:exit-code 0 :timed-out? false :output green}))
    (is (mutation/killed? {:exit-code 1 :timed-out? false :output red}))
    (doseq [result [{:exit-code 124 :timed-out? true :output red}
                    {:exit-code 1 :timed-out? false :output "Compiler error"}
                    {:exit-code 0 :timed-out? false :output "0 failures, 0 errors."}]]
      (is (not (mutation/killed? result)))
      (is (not (mutation/passed-tests? result)))))
  (is (not (mutation/compiled? {:exit-code 0 :timed-out? false :output "Build completed. 2 warnings"}))))

(deftest baseline-check-preserves-phase-and-safe-counters
  (let [green-build (mutation/process-result 0 false "Build completed. 0 warnings.")
        green-tests (mutation/process-result 0 false "Ran 10 tests containing 40 assertions.\n0 failures, 0 errors.")
        red-tests (mutation/process-result 1 false "Ran 10 tests containing 40 assertions.\n1 failures, 0 errors.")]
    (with-redefs [mutation/compile-mutant! (constantly (mutation/process-result 0 false "Build completed. 2 warnings."))
                  mutation/run-mutant-tests! (fn [_] (throw (ex-info "Tests must not run after a red compile" {})))]
      (is (= {:status :failed
              :failure {:phase :compile :reason :compiler-warnings :exit-code 0 :timed-out? false :warning-count 2}}
             (mutation/baseline! {}))))
    (with-redefs [mutation/compile-mutant! (constantly green-build)]
      (doseq [[result expected] [[green-tests {:status :passed}]
                                 [red-tests {:status :failed
                                             :failure {:phase :test :reason :test-failures :exit-code 1
                                                       :timed-out? false :test-counters {:assertions 40 :failures 1 :errors 0}}}]
                                 [(mutation/process-result 0 false "Missing native summary")
                                  {:status :failed
                                   :failure {:phase :test :reason :missing-test-assertions :exit-code 0 :timed-out? false}}]
                                 [(mutation/process-result 124 true "Synthetic private value")
                                  {:status :failed
                                   :failure {:phase :test :reason :timeout :exit-code 124 :timed-out? true}}]]]
        (with-redefs [mutation/run-mutant-tests! (constantly result)]
          (is (= expected (mutation/baseline! {}))))))))

(deftest failed-baseline-report-stays-red-without-child-output
  (let [directory (.toFile (java.nio.file.Files/createTempDirectory "knoxx-baseline-report" (make-array java.nio.file.attribute.FileAttribute 0)))
        report-file (io/file directory "batch-2.edn")
        json-file (io/file directory "batch-2.edn.json")
        sentinel "SYNTHETIC_PRIVATE_CHILD_OUTPUT"
        compile-result (mutation/process-result 1 false (str "Failed to read artifact descriptor " sentinel))]
    (try
      (with-redefs [mutation/assert-clean-tree! (constantly nil)
                    mutation/discover-mutants (constantly [{:id "must-not-run"}])
                    mutation/compile-mutant! (constantly compile-result)
                    mutation/evaluate-mutant! (fn [_ _] (throw (ex-info "Mutation unexpectedly ran" {})))]
        (let [output (with-out-str
                       (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Mutation qualification failed"
                                             (mutation/run-suite! {:run? true :report-file (.getPath report-file)
                                                                   :batch-index 2 :min-evaluated 250}))))
              json-text (slurp json-file)
              report (json/read-str json-text)
              failure (get report "baseline_failure")]
          (is (= ["failed" "failed" 2 []] (mapv report ["status" "baseline" "batch_index" "mutants"])))
          (is (= 0 (get-in report ["summary" "evaluated"])))
          (is (= {"phase" "compile" "reason" "nonzero-exit" "exit-code" 1 "timed-out?" false
                  "signals" ["dependency-resolution"]}
                 failure))
          (is (str/includes? output "Mutation baseline failed:"))
          (is (not-any? #(str/includes? % sentinel) [output json-text (slurp report-file)]))))
      (finally
        (doseq [file (reverse (file-seq directory))]
          (.delete file))))))
