(ns knoxx.mutation-test-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [knoxx.mutation-test :as mutation]))

(deftest fatal-and-incomplete-test-output-is-not-qualification
  (let [green "Ran 10 tests containing 40 assertions.\n0 failures, 0 errors."
        red "Ran 10 tests containing 40 assertions.\n1 failures, 0 errors."
        fatal "\n[shadow-test-guard] FATAL unhandled rejection: fixture"
        completed {:termination "exited" :cleanup-complete? true
                   :capture-truncated? false :timed-out? false}]
    (is (not (mutation/passed-tests? (merge completed {:exit-code 0 :output (str green fatal)}))))
    (is (not (mutation/killed? (merge completed {:exit-code 137 :output (str red fatal)}))))
    (is (not (mutation/killed? (merge completed {:exit-code 137 :output red}))))
    (is (not (mutation/passed-tests? (merge completed {:exit-code 0 :output (str green "\n0 failures, 0 errors.")}))))
    (is (not (mutation/passed-tests? (merge completed {:exit-code 0 :output "Ran 0 tests containing 40 assertions.\n0 failures, 0 errors."}))))
    (is (not (mutation/passed-tests? (merge completed {:exit-code 0 :output green :capture-truncated? true}))))
    (is (not (mutation/passed-tests? (merge completed {:exit-code 0 :output green :cleanup-complete? false}))))))

(deftest portable-source-inventory-admits-only-active-cljs-branches
  (let [directory (.toFile (java.nio.file.Files/createTempDirectory "mutation-portable" (make-array java.nio.file.attribute.FileAttribute 0)))
        source-file (io/file directory "portable.cljc")]
    (try
      (spit source-file "(ns portable)\n(def value #?(:clj (+ 1 0) :cljs (- 1 0)))\n(def fallback #?(:clj true :default false))\n#_(* 1 0)\n")
      (is (mutation/cljs-source-file? source-file))
      (let [mutants (mutation/mutants-in-file directory source-file)]
        (is (not-any? #(= "(+ 1 0)" (:original %)) mutants))
        (is (some #(= "(- 1 0)" (:original %)) mutants))
        (is (not-any? #(= "(* 1 0)" (:original %)) mutants))
        (is (not-any? #(= "true" (:original %)) mutants))
        (is (some #(= "false" (:original %)) mutants)))
      (finally (.delete source-file) (.delete directory)))))

(deftest explicit-equivalent-exclusion-replenishes-without-kill-credit
  (let [mutants (mutation/assign-mutant-ids
                 (mapv #(hash-map :relative-path "a.cljs" :line % :column 1
                                 :operator :boolean-literal-flip :original "true" :replacement "false")
                       (range 12)))
        excluded (set (map :fingerprint (take 2 mutants)))
        batches (mapv #(mutation/partition-mutants mutants {:batch-index % :batch-count 4 :limit 2
                                                           :equivalent-fingerprints excluded}) (range 4))]
    (is (every? #(= 2 (count %)) batches))
    (is (not-any? #(contains? excluded (:fingerprint %)) (mapcat identity batches)))
    (is (= [4 8] (mapv :ordinal (first batches))))
    (is (= [5 9] (mapv :ordinal (second batches))))
    (is (= 8 (count (set (map :fingerprint (mapcat identity batches))))))))

(deftest reader-conditionals-are-never-promoted-to-runtime-read-string
  (let [mutants (mutation/mutants-in-source "conditional.cljc" "(if #?(:clj true :cljs false) 1 0)")
        spliced (mutation/mutants-in-source "spliced.cljc" "(def value [#?@(:clj [(+ 1 0)] :cljs [(- 1 0)])])")]
    (is (not-any? #(str/includes? (:replacement %) "read-string") mutants))
    (is (some #(= "false" (:original %)) mutants))
    (is (not-any? #(= "(+ 1 0)" (:original %)) spliced))
    (is (some #(= "(- 1 0)" (:original %)) spliced))))

(deftest equivalent-adjudications-require-source-and-external-review-provenance
  (let [candidate (first (mutation/assign-mutant-ids [{:relative-path "a.cljc" :line 1 :column 1
                                                     :operator :boolean-literal-flip :original "true" :replacement "false"}]))
        source-hash (apply str (repeat 64 "a"))
        entry {:fingerprint (:fingerprint candidate) :relative-path "a.cljc" :source-sha256 source-hash
               :rationale "Synthetic fixture: ignored return has no observable consumer effect"
               :review-reference "https://github.com/open-hax/knoxx/pull/1#discussion_r1"}
        manifest {:schema-version 1 :basis-source-sha (apply str (repeat 40 "b")) :entries [entry]}]
    ;; This only exercises provenance grammar. No actual equivalent is adopted.
    (is (= [entry] (mutation/validate-equivalents! manifest [candidate] {"a.cljc" source-hash})))
    (doseq [bad-entry [(dissoc entry :review-reference) (assoc entry :review-reference "agent-approved")
                       (assoc entry :rationale "") (assoc entry :source-sha256 (apply str (repeat 64 "c")))
                       (assoc entry :fingerprint (apply str (repeat 64 "0")))]]
      (is (thrown? clojure.lang.ExceptionInfo
                   (mutation/validate-equivalents! (assoc manifest :entries [bad-entry]) [candidate] {"a.cljc" source-hash}))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (mutation/validate-equivalents! (assoc manifest :entries [entry entry]) [candidate] {"a.cljc" source-hash})))
    (is (= [] (mutation/load-equivalents! {} [candidate])))
    (is (thrown? clojure.lang.ExceptionInfo
                 (mutation/load-equivalents! {:equivalents-file "../AGENTS.md"} [candidate])))))

(deftest compiler-completion-and-artifact-custody-are-required
  (let [complete {:exit-code 0 :timed-out? false :termination "exited" :cleanup-complete? true
                  :capture-truncated? false :artifact-sha256 (apply str (repeat 64 "a"))
                  :output "[:test-ci] Build completed. (10 files, 1 compiled, 0 warnings, 0.1s)"}]
    (is (mutation/compiled? complete))
    (is (not (mutation/compiled? (assoc complete :output ""))))
    (is (not (mutation/compiled? (dissoc complete :artifact-sha256))))
    (is (not (mutation/compiled? (assoc complete :output "[:server] Build completed. (10 files, 1 compiled, 0 warnings, 0.1s)"))))))

(deftest timed-out-descendants-do-not-delay-original-source-restoration
  (let [directory (.toFile (java.nio.file.Files/createTempDirectory "mutation-restore" (make-array java.nio.file.attribute.FileAttribute 0)))
        file (io/file directory "demo.cljs")
        source "(ns demo)\n(def value true)\n"]
    (try
      (spit file source)
      (let [mutant (first (mutation/discover-mutants {:src-dir (.getPath directory)}))
            started (System/nanoTime)
            result (mutation/with-mutated-source! {:src-dir (.getPath directory)} mutant
                     #(mutation/run-process! {:cmd ["python3" "-c" "import subprocess,time; subprocess.Popen(['python3','-c','import time; time.sleep(60)']); print('owned-child-started',flush=True); time.sleep(60)"]
                                              :timeout-ms 150}))]
        (is (true? (:timed-out? result)))
        (is (true? (:cleanup-complete? result)))
        (is (not (mutation/killed? result)))
        (is (= source (slurp file)))
        (is (< (/ (- (System/nanoTime) started) 1e9) 5)))
      (finally (.delete file) (.delete directory)))))

(deftest interrupted-jvm-wait-restores-source-and-cleans-its-owned-child
  (let [directory (.toFile (java.nio.file.Files/createTempDirectory "mutation-cancel" (make-array java.nio.file.attribute.FileAttribute 0)))
        file (io/file directory "demo.cljs")
        ready (io/file directory "child.pid")
        source "(ns demo)\n(def value true)\n"]
    (try
      (spit file source)
      (let [mutant (first (mutation/discover-mutants {:src-dir (.getPath directory)}))
            command (str "import os,pathlib,subprocess,time; subprocess.Popen(['python3','-c','import time; time.sleep(60)']); pathlib.Path("
                         (pr-str (.getPath ready)) ").write_text(str(os.getpid())); time.sleep(60)")
            task (future (mutation/with-mutated-source! {:src-dir (.getPath directory)} mutant
                           #(mutation/run-process! {:cmd ["python3" "-c" command] :timeout-ms 60000})))
            ready-deadline (+ (System/nanoTime) 5000000000)]
        (try
          (loop []
            (when (and (not (.isFile ready)) (< (System/nanoTime) ready-deadline))
              (Thread/sleep 10) (recur)))
          (is (.isFile ready))
          (future-cancel task)
          (let [deadline (+ (System/nanoTime) 5000000000)
                pid (Long/parseLong (slurp ready))]
            (loop []
              (when (and (not= source (slurp file)) (< (System/nanoTime) deadline))
                (Thread/sleep 10) (recur)))
            (is (= source (slurp file)))
            (is (not (.isPresent (java.lang.ProcessHandle/of pid)))))
          (finally (future-cancel task))))
      (finally (.delete ready) (.delete file) (.delete directory)))))

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
        red "Ran 10 tests containing 40 assertions.\n1 failures, 0 errors."
        completed {:termination "exited" :cleanup-complete? true :capture-truncated? false}]
    (is (mutation/passed-tests? (merge completed {:exit-code 0 :timed-out? false :output green})))
    (is (mutation/killed? (merge completed {:exit-code 1 :timed-out? false :output red})))
    (doseq [result [{:exit-code 124 :timed-out? true :output red}
                    {:exit-code 1 :timed-out? false :output "Compiler error"}
                    {:exit-code 0 :timed-out? false :output "0 failures, 0 errors."}]]
      (is (not (mutation/killed? result)))
      (is (not (mutation/passed-tests? result)))))
  (is (not (mutation/compiled? {:exit-code 0 :timed-out? false :output "Build completed. 2 warnings"}))))
