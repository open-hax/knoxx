(ns knoxx.mutation-test-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [knoxx.mutation-test :as mutation])
  (:import [java.lang ProcessHandle]))

(defn- process-alive? [^ProcessHandle handle]
  (.isAlive handle))

(deftest timed-out-process-returns-without-waiting-for-its-child
  (let [started (System/nanoTime)
        result (mutation/run-process!
                 {:cmd ["sh" "-c" "sleep 6 & printf '%s' \"$!\"; wait"]
                  :timeout-ms 500})
        elapsed-ms (quot (- (System/nanoTime) started) 1000000)
        child (ProcessHandle/of (parse-long (:output result)))]
    (is (= {:exit-code 124 :timed-out? true}
           (select-keys result [:exit-code :timed-out?])))
    (is (< elapsed-ms 2500) (str "Timed-out subprocess returned after " elapsed-ms " ms"))
    (is (or (.isEmpty child) (not (process-alive? (.get child)))))))

(deftest completed-process-retains-merged-output-and-exit-code
  (is (= {:exit-code 7 :timed-out? false :output "successfailure"}
         (mutation/run-process!
          {:cmd ["sh" "-c" "printf success; printf failure >&2; exit 7"]
           :timeout-ms 2000}))))

(deftest unconfirmed-cleanup-does-not-produce-a-process-result
  (with-redefs-fn {#'mutation/stop-process-tree! (fn [^Process process]
                                                 (.destroyForcibly process)
                                                 (throw (ex-info "Synthetic cleanup failure" {})))}
    #(is (thrown-with-msg? clojure.lang.ExceptionInfo #"Synthetic cleanup failure"
                           (mutation/run-process! {:cmd ["sleep" "5"] :timeout-ms 50})))))

(defn- wait-for-processes [file]
  (let [deadline (+ (System/nanoTime) 2000000000)]
    (loop []
      (let [ids (re-matches #"(\d+) (\d+)\n" (slurp file))]
        (cond
          ids (mapv #(.orElse (ProcessHandle/of (parse-long %)) nil) (rest ids))
          (< (System/nanoTime) deadline) (do (Thread/sleep 10) (recur))
          :else (throw (ex-info "Synthetic subprocesses did not start" {})))))))

(deftest interrupt-stops-the-owned-processes-and-preserves-interruption
  (let [pids (java.io.File/createTempFile "knoxx-mutation-test-" ".pids")
        handles (atom [])
        result (promise)
        worker (Thread.
                ^Runnable
                (fn []
                  (try
                    (deliver result
                             (mutation/run-process!
                              {:cmd ["sh" "-c" "sleep 8 & printf '%s %s\n' \"$$\" \"$!\" > \"$1\"; wait"
                                     "mutation-interrupt-test" (.getPath pids)]
                               :timeout-ms 5000}))
                    (catch InterruptedException interrupted
                      (deliver result {:interrupted? (.isInterrupted (Thread/currentThread))
                                       :cleanup-failures (count (.getSuppressed interrupted))})))))]
    (try
      (.start worker)
      (reset! handles (wait-for-processes pids))
      (is (every? some? @handles) "both fixture process handles were captured before interruption")
      (.interrupt worker)
      (is (= {:interrupted? true :cleanup-failures 0} (deref result 2500 nil)))
      (is (not-any? process-alive? (remove nil? @handles)))
      (finally
        (doseq [^ProcessHandle handle (remove nil? @handles)]
          (.destroyForcibly handle))
        (.delete pids)))))

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
