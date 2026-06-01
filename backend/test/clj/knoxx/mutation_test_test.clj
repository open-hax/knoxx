(ns knoxx.mutation-test-test
  (:require [clojure.java.io :as io]
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

(deftest emit-mutant-source-creates-shadow-overlay-path
  (let [tmp-dir (.toFile (java.nio.file.Files/createTempDirectory "knoxx-mutation-test" (make-array java.nio.file.attribute.FileAttribute 0)))
        src-dir (io/file tmp-dir "src/cljs")
        source-file (io/file src-dir "demo/core.cljs")
        output-dir (io/file tmp-dir "target/mutation")
        source "(ns demo.core)\n(defn f [x] (if (= x 1) true false))\n"]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file source)
      (let [mutant (->> (mutation/discover-mutants {:src-dir (.getPath src-dir)
                                                    :include-regex "demo/core.cljs$"
                                                    :limit 1})
                        first)
            emitted (mutation/emit-mutant-source! {:src-dir (.getPath src-dir)
                                                   :output-dir (.getPath output-dir)}
                                                  mutant)]
        (is (.exists (io/file (:overlay-path emitted))))
        (is (str/ends-with? (:overlay-path emitted) "target/mutation/mutants/m0001/src/cljs/demo/core.cljs"))
        (is (= ["target/mutation/mutants/m0001/src/cljs" "src/cljs" "test/cljs"]
               (:source-paths (mutation/shadow-config-merge emitted)))))
      (finally
        (doseq [f (reverse (file-seq tmp-dir))]
          (.delete f))))))
