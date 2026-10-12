(ns knoxx.mutation-test-test
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [knoxx.mutation-test :as mutation]))

(defn payload-fixture!
  "Make a private committed source and two generated runtime files."
  []
  (let [root (.toFile (java.nio.file.Files/createTempDirectory "mutation-payload" (make-array java.nio.file.attribute.FileAttribute 0)))
        backend (io/file root "backend")
        loader "target/test/test-ci.cjs"
        runtime ".shadow-cljs/builds/test-ci/dev/out/cljs-runtime"
        texts {loader (str "var SHADOW_IMPORT_PATH = __dirname + '/../../" runtime "';\nSHADOW_IMPORT(\"demo.js\");\n")
               (str runtime "/demo.js") "global.demo = true;\n"
               (str runtime "/goog.base.js") "global.goog = {};\n"
               (str runtime "/demo.js.map") "{}\n"}]
    (.mkdirs backend)
    (spit (io/file root ".gitignore") "backend/target/\nbackend/.shadow-cljs/\n")
    (.mkdirs (io/file backend "src/cljs"))
    (spit (io/file backend "src/cljs/demo.cljs") "(def value true)\n")
    (doseq [cmd [["git" "init" "--quiet"] ["git" "add" "."]
                 ["git" "-c" "user.name=Fixture" "-c" "user.email=fixture@example.invalid" "commit" "--quiet" "-m" "fixture"]]]
      (let [result (apply shell/sh (concat cmd [:dir (.getPath root)]))]
        (when-not (zero? (:exit result)) (throw (ex-info "Fixture Git command failed" result)))))
    (doseq [[path content] texts]
      (.mkdirs (.getParentFile (io/file backend path)))
      (spit (io/file backend path) content))
    (let [files (mapv (fn [[path content]]
                        {:path path :kind (cond (= loader path) "loader" (str/ends-with? path ".map") "source-map" :else "runtime-js")
                         :bytes (alength (.getBytes ^String content "UTF-8")) :sha256 (mutation/sha256 content)}) (sort-by key texts))
          digest #(mutation/sha256 (apply str (map (fn [{:keys [path kind bytes sha256]}] (str path "\t" kind "\t" bytes "\t" sha256 "\n")) %)))
          source-sha (str/trim (:out (shell/sh "git" "rev-parse" "HEAD" :dir (.getPath root))))]
      {:root root :backend-dir (.getPath backend) :source-sha source-sha
       :payload {:schema-version 1 :source-sha source-sha :source-binding {:kind "baseline"}
                 :loader-path loader :runtime-root runtime :imports ["demo.js"] :files files
                 :sha256 (digest files) :code-sha256 (digest (remove #(= "source-map" (:kind %)) files))}})))

(defn delete-fixture!
  "Remove only the test's private directory."
  [root]
  (doseq [file (reverse (file-seq root))] (.delete ^java.io.File file)))

(deftest canonical-source-binding-refuses-fabricated-fingerprint
  (let [{:keys [root backend-dir] :as fixture} (payload-fixture!)
        candidate (first (mutation/discover-candidates {:src-dir (.getPath (io/file backend-dir "src/cljs"))}))]
    (try
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"fingerprint differs"
                           (mutation/source-binding! fixture (assoc candidate :fingerprint (apply str (repeat 64 "f"))))))
      (finally (delete-fixture! root)))))

(deftest canonical-source-manifest-is-reconstructed-from-immutable-checkout
  (let [{:keys [root backend-dir source-sha] :as fixture} (payload-fixture!)
        file (io/file backend-dir "src/cljs/demo.cljs")
        original (slurp file)
        candidate (first (mutation/discover-candidates {:src-dir (.getPath (io/file backend-dir "src/cljs"))}))]
    (try
      (with-redefs [mutation/run-process! (fn [_] (throw (AssertionError. "Binding regeneration must not execute a child")))]
        (let [manifest (mutation/source-bindings-manifest! fixture)
              binding (get-in manifest [:candidates 0 :source-binding])]
          (is (= {:schema-version 1 :source-sha source-sha} (dissoc manifest :candidates)))
          (is (= 1 (count (:candidates manifest))))
          (is (= 0 (get-in manifest [:candidates 0 :ordinal])))
          (is (= (mutation/source-binding! fixture candidate) binding))
          (is (= "src/cljs/demo.cljs" (:path binding)))
          (is (= (mutation/sha256 "(def value true)\n") (:original-sha256 binding)))
          (is (= (mutation/sha256 "(def value false)\n") (:mutated-sha256 binding)))
          (is (= original (slurp file))))
        (is (thrown? clojure.lang.ExceptionInfo
                     (mutation/source-bindings-manifest! (assoc fixture :source-sha (apply str (repeat 40 "f"))))))
        (spit file "(def value false)\n")
        (is (thrown? clojure.lang.ExceptionInfo (mutation/source-bindings-manifest! fixture)))
        (spit file original)
        (let [calls (atom 0)
              original-discover mutation/discover-candidates]
          (with-redefs [mutation/discover-candidates (fn [opts]
                                                     (swap! calls inc)
                                                     (let [result (original-discover opts)]
                                                       (spit file "(def value false)\n")
                                                       result))]
            (is (thrown? clojure.lang.ExceptionInfo (mutation/source-bindings-manifest! fixture)))
            (is (= 1 @calls))))
        (spit file original)
        (is (true? (mutation/assert-source-custody! fixture))))
      (finally (delete-fixture! root)))))

(deftest canonical-fingerprint-matches-observed-pilot-descriptor
  ;; Actual historical pilot c533; no equivalent adjudication or kill credit.
  (is (= "c5331e130ef389a0c7c8f22f68ef799fcea6c121f355c70c564643050a068081"
         (mutation/fingerprint {:relative-path "knoxx/backend/bootstrap.cljs" :line 71 :column 9
                                :operator :boolean-literal-flip :original "true" :replacement "false"}))))

(deftest exclusion-partition-lookup-does-not-promote-a-missing-candidate
  (let [{:keys [root backend-dir] :as fixture} (payload-fixture!)
        report (io/file backend-dir "target/mutation/exclusion-lookup.edn")]
    (try
      ;; This inert dependency fault tests the defensive lookup only;
      ;; real load-equivalents! still rejects missing candidates first.
      (with-redefs [mutation/load-equivalents! (fn [_ _] [{:fingerprint (apply str (repeat 64 "f"))}])]
        (let [result (mutation/run-suite! (assoc fixture :src-dir (.getPath (io/file backend-dir "src/cljs"))
                                               :report-file (.getPath report) :batch-index 0 :batch-count 4 :run? false))]
          (is (empty? (:excluded result)))
          (is (= :planned (:status result)))
          (is (= 0 (get-in result [:summary :evaluated])))
          (is (= 0 (get-in result [:summary :killed])))))
      (finally (delete-fixture! root)))))

;; These controls call the real canonical partition and exclusion arithmetic.
;; The nonempty equivalent list is an inert ordering seam, not adjudication.
(deftest invalid-batch-count-precedes-equivalent-modulo
  (doseq [run? [false true]]
    (let [{:keys [root backend-dir] :as fixture} (payload-fixture!)
          source-dir (.getPath (io/file backend-dir "src/cljs"))
          source-file (io/file source-dir "demo.cljs")
          source-before (slurp source-file)
          output-dir (io/file backend-dir "target/invalid-count")
          payload-store (io/file output-dir "payload-store")
          candidate (first (mutation/discover-candidates {:src-dir source-dir}))
          baseline-calls (atom 0)
          evaluation-calls (atom 0)
          report-calls (atom 0)]
      (try
        ;; The private fixture is not the product compiled cwd; replace only
        ;; its outer clean-tree preflight to reach the real ordering boundary.
        (with-redefs [mutation/assert-clean-tree! (fn [_] nil)
                      mutation/load-equivalents! (fn [_ _] [{:fingerprint (:fingerprint candidate)}])
                      mutation/baseline! (fn [_]
                                           (swap! baseline-calls inc)
                                           (.mkdirs payload-store)
                                           {:status :passed})
                      mutation/evaluate-mutant! (fn [_ _] (swap! evaluation-calls inc) {})
                      mutation/write-report! (fn [_ _] (swap! report-calls inc) {})]
          (let [caught (try (mutation/run-suite! (assoc fixture :src-dir source-dir
                                                      :output-dir (.getPath output-dir)
                                                      :batch-index 0 :batch-count 0 :run? run?))
                            (catch Exception error error))]
            (is (instance? clojure.lang.ExceptionInfo caught))
            (is (= "Invalid mutation batch index/count"
                   (when (instance? Exception caught) (.getMessage ^Exception caught))))
            (is (zero? @baseline-calls))
            (is (zero? @evaluation-calls))
            (is (zero? @report-calls))
            (is (not (.exists payload-store)))
            (is (= source-before (slurp source-file)))))
        (finally (delete-fixture! root))))))

(deftest invalid-batch-index-precedes-baseline-and-report
  (doseq [run? [false true]
          batch-index [-1 4]]
    (let [{:keys [root backend-dir] :as fixture} (payload-fixture!)
          source-dir (.getPath (io/file backend-dir "src/cljs"))
          source-file (io/file source-dir "demo.cljs")
          source-before (slurp source-file)
          output-dir (io/file backend-dir "target/invalid-index")
          payload-store (io/file output-dir "payload-store")
          baseline-calls (atom 0)
          evaluation-calls (atom 0)
          report-calls (atom 0)]
      (try
        ;; Same private-fixture preflight seam; partition and baseline order
        ;; remain the actual run-suite! implementation under test.
        (with-redefs [mutation/assert-clean-tree! (fn [_] nil)
                      mutation/baseline! (fn [_]
                                         (swap! baseline-calls inc)
                                         (.mkdirs payload-store)
                                         {:status :passed})
                      mutation/evaluate-mutant! (fn [_ _] (swap! evaluation-calls inc) {})
                      mutation/write-report! (fn [_ _] (swap! report-calls inc) {})]
          (let [caught (try (mutation/run-suite! (assoc fixture :src-dir source-dir
                                                      :output-dir (.getPath output-dir)
                                                      :batch-index batch-index :batch-count 4 :run? run?))
                            (catch Exception error error))]
            (is (instance? clojure.lang.ExceptionInfo caught))
            (is (= "Invalid mutation batch index/count"
                   (when (instance? Exception caught) (.getMessage ^Exception caught))))
            (is (zero? @baseline-calls))
            (is (zero? @evaluation-calls))
            (is (zero? @report-calls))
            (is (not (.exists payload-store)))
            (is (= source-before (slurp source-file)))))
        (finally (delete-fixture! root))))))

(deftest completed-child-evidence-survives-custody-io-failures
  (let [{:keys [root backend-dir payload] :as fixture} (payload-fixture!)
        source-dir (io/file backend-dir "src/cljs")
        source-file (io/file source-dir "demo.cljs")
        original-source (slurp source-file)
        mutant (first (mutation/discover-mutants {:src-dir (.getPath source-dir)}))
        opts (assoc fixture :src-dir (.getPath source-dir))
        complete {:timed-out? false :termination "exited" :cleanup-complete? true :capture-truncated? false}
        compile-result (assoc complete :exit-code 0 :output "[:test-ci] Build completed. (2 files, 1 compiled, 0 warnings, 0.1s)")
        test-result (assoc complete :exit-code 1 :output "Ran 1 tests containing 1 assertions.\n1 failures, 0 errors.")
        test-runs (atom 0)
        original-payload-guard mutation/assert-payload-custody!]
    (try
      (doseq [phase [:compile :test]]
        (let [io-error (java.io.IOException. (str "injected completed-" (name phase) " custody I/O failure"))
              result-key (if (= :compile phase) :compile-result :test-result)
              child-result (if (= :compile phase) compile-result test-result)]
          (with-redefs [mutation/run-process! (fn [{:keys [cmd]}]
                                               (if (= "pnpm" (first cmd)) compile-result
                                                   (do (swap! test-runs inc) test-result)))
                        mutation/preserve-payload! (fn [_ snapshot]
                                                     (if (= :compile phase) (throw io-error) snapshot))
                        mutation/assert-payload-custody! (fn [guard-opts]
                                                          (if (and (= :test phase) (pos? @test-runs))
                                                            (throw io-error)
                                                            (original-payload-guard guard-opts)))]
            (reset! test-runs 0)
            (let [error (try
                          (if (= :compile phase) (mutation/compile-mutant! opts)
                              (mutation/run-mutant-tests! (assoc opts :payload payload)))
                          (catch Exception failure failure))]
              (is (instance? clojure.lang.ExceptionInfo error))
              (is (identical? io-error (.getCause ^Exception error)))
              (is (= child-result (get (ex-data error) result-key)))
              (is (= {:class "java.io.IOException" :message (.getMessage io-error)}
                     (:custody-io-error (ex-data error)))))
            (reset! test-runs 0)
            (let [baseline (try (mutation/baseline! opts) (catch Exception error error))]
              (is (= :failed (:status baseline)))
              (is (= "custody-rejected" (get-in baseline [:compile :termination])))
              (is (= child-result (get-in baseline [:compile :custody-error result-key])))
              (is (= "java.io.IOException" (get-in baseline [:compile :custody-error :custody-io-error :class]))))
            (reset! test-runs 0)
            (let [evaluated (try (mutation/evaluate-mutant! opts mutant) (catch Exception error error))
                  report-file (io/file backend-dir "target" (str "io-" (name phase) ".edn"))
                  report (when (map? evaluated)
                           (mutation/write-report! {:run? true :baseline :passed :source-sha (:source-sha fixture)
                                                    :report-file (.getPath report-file)} [evaluated]))
                  native (when report (json/read-str (slurp (str report-file ".json")) :key-fn keyword))]
              (is (= :invalid (:status evaluated)))
              (is (= :custody (:phase evaluated)))
              (is (= child-result (get-in evaluated [:result :custody-error result-key])))
              (is (= (.getMessage io-error) (get-in evaluated [:result :custody-error :custody-io-error :message])))
              (is (= 3 (:schema_version native)))
              (is (= "failed" (:status native)))
              (is (= 1 (get-in native [:summary :invalid])))
              (is (= child-result (get-in native [:mutants 0 :result :custody-error result-key])))
              (is (= original-source (slurp source-file)))))))
      ;; Fatal JVM errors are outside the expected I/O custody boundary.
      (doseq [phase [:compile :test]]
        (let [fatal (AssertionError. "fatal fixture")
              calls (atom 0)]
          (with-redefs [mutation/assert-source-custody! (constantly true)
                        mutation/run-process! (fn [_] (swap! calls inc) (if (= :compile phase) compile-result test-result))
                        mutation/preserve-payload! (fn [_ _] (throw fatal))
                        mutation/payload-snapshot! (constantly payload)
                        mutation/assert-payload-custody! (fn [_] (when (pos? @calls) (throw fatal)) true)]
            (is (identical? fatal (try
                                   (if (= :compile phase) (mutation/compile-mutant! opts)
                                       (mutation/run-mutant-tests! (assoc opts :payload payload)))
                                   (catch AssertionError error error)))))))
      (finally (delete-fixture! root)))))

(deftest pretest-custody-io-failure-retains-compile-without-running-tests
  (let [{:keys [root backend-dir payload] :as fixture} (payload-fixture!)
        source-dir (io/file backend-dir "src/cljs")
        source-file (io/file source-dir "demo.cljs")
        original-source (slurp source-file)
        mutant (first (mutation/discover-mutants {:src-dir (.getPath source-dir)}))
        opts (assoc fixture :src-dir (.getPath source-dir))
        complete {:exit-code 0 :timed-out? false :termination "exited" :cleanup-complete? true
                  :capture-truncated? false :source-verified-before? true :source-verified-after? true
                  :artifact-path mutation/payload-loader
                  :artifact-sha256 (:sha256 (some #(when (= "loader" (:kind %)) %) (:files payload)))
                  :output "[:test-ci] Build completed. (2 files, 1 compiled, 0 warnings, 0.1s)"}
        original-source-guard mutation/assert-source-custody!
        original-payload-guard mutation/assert-payload-custody!
        original-custody-failure mutation/custody-failure]
    (try
      (doseq [guard [:source :payload]
              caller [:baseline :mutant]]
        (let [io-error (java.io.IOException. (str "injected pretest " (name guard) " read failure"))
              compiled (atom nil)
              caught (atom nil)
              test-runs (atom 0)]
          (with-redefs [mutation/compile-mutant! (fn [compile-opts]
                                                 (let [result (assoc complete :payload
                                                                     (assoc payload :source-binding (:source-binding compile-opts)))]
                                                   (reset! compiled result)
                                                   result))
                        mutation/assert-source-custody! (fn [guard-opts]
                                                          (if (= :source guard) (throw io-error)
                                                              (original-source-guard guard-opts)))
                        mutation/assert-payload-custody! (fn [guard-opts]
                                                           (if (= :payload guard) (throw io-error)
                                                               (original-payload-guard guard-opts)))
                        mutation/run-process! (fn [_] (swap! test-runs inc) (throw (AssertionError. "test child must remain uninvoked")))
                        mutation/custody-failure (fn [error] (reset! caught error) (original-custody-failure error))]
            (let [result (try (if (= :baseline caller) (mutation/baseline! opts)
                                 (mutation/evaluate-mutant! opts mutant))
                              (catch Exception error error))
                  failure (if (= :baseline caller) (:compile result) (:result result))
                  report-file (io/file backend-dir "target" (str "pretest-" (name caller) "-" (name guard) ".edn"))
                  report (when (map? result)
                           (mutation/write-report! {:run? true :source-sha (:source-sha fixture)
                                                    :baseline (if (= :baseline caller) (:status result) :passed)
                                                    :baseline-evidence (when (= :baseline caller) result)
                                                    :report-file (.getPath report-file)}
                                                   (if (= :baseline caller) [] [result])))
                  native (when report (json/read-str (slurp (str report-file ".json")) :key-fn keyword))
                  native-failure (if (= :baseline caller) (get-in native [:baseline_evidence :compile])
                                     (get-in native [:mutants 0 :result]))]
              (is (zero? @test-runs))
              (is (mutation/compiled? @compiled))
              (is (= (if (= :baseline caller) :failed :invalid) (:status result)))
              (is (= "custody-rejected" (:termination failure)))
              (is (nil? (:exit-code failure)))
              (is (= @compiled (get-in failure [:custody-error :compile-result])))
              (is (= {:class "java.io.IOException" :message (.getMessage io-error)}
                     (get-in failure [:custody-error :custody-io-error])))
              (is (identical? io-error (some-> @caught .getCause)))
              (is (not (contains? (:custody-error failure) :test-result)))
              (is (not-any? #(contains? failure %) [:tests :assertions :failures :errors]))
              (is (= 3 (:schema_version native)))
              (is (= "failed" (:status native)))
              (is (= 0 (get-in native [:summary :killed])))
              (is (= 0 (get-in native [:summary :evaluated])))
              (is (= @compiled (get-in native-failure [:custody-error :compile-result])))
              (is (nil? (get-in native [:baseline_evidence :test])))
              (is (= original-source (slurp source-file)))))))
      (doseq [caller [:baseline :mutant]]
        (let [fatal (AssertionError. "fatal pretest fixture")]
          (with-redefs [mutation/compile-mutant! (constantly (assoc complete :payload payload))
                        mutation/assert-source-custody! (fn [_] (throw fatal))]
            (is (identical? fatal (try
                                   (if (= :baseline caller) (mutation/baseline! opts)
                                       (mutation/evaluate-mutant! opts mutant))
                                   (catch AssertionError error error))))
            (is (= original-source (slurp source-file))))))
      (finally (delete-fixture! root)))))

(deftest pretest-custody-ex-info-retains-compile-without-running-tests
  (let [{:keys [root backend-dir payload] :as fixture} (payload-fixture!)
        source-dir (io/file backend-dir "src/cljs")
        source-file (io/file source-dir "demo.cljs")
        original-source (slurp source-file)
        mutant (first (mutation/discover-mutants {:src-dir (.getPath source-dir)}))
        opts (assoc fixture :src-dir (.getPath source-dir))
        complete {:exit-code 0 :timed-out? false :termination "exited" :cleanup-complete? true
                  :capture-truncated? false :source-verified-before? true :source-verified-after? true
                  :artifact-path mutation/payload-loader
                  :artifact-sha256 (:sha256 (some #(when (= "loader" (:kind %)) %) (:files payload)))
                  :output "[:test-ci] Build completed. (2 files, 1 compiled, 0 warnings, 0.1s)"}
        original-source-guard mutation/assert-source-custody!
        original-payload-guard mutation/assert-payload-custody!
        original-custody-failure mutation/custody-failure]
    (try
      (doseq [guard [:source :payload]
              caller [:baseline :mutant]]
        (let [guard-error (ex-info (str "injected pretest " (name guard) " custody failure") {:guard guard :tampered true})
              compiled (atom nil)
              caught (atom nil)
              test-runs (atom 0)]
          (with-redefs [mutation/compile-mutant! (fn [compile-opts]
                                                 (let [result (assoc complete :payload
                                                                     (assoc payload :source-binding (:source-binding compile-opts)))]
                                                   (reset! compiled result)
                                                   result))
                        mutation/assert-source-custody! (fn [guard-opts]
                                                          (if (= :source guard) (throw guard-error)
                                                              (original-source-guard guard-opts)))
                        mutation/assert-payload-custody! (fn [guard-opts]
                                                           (if (= :payload guard) (throw guard-error)
                                                               (original-payload-guard guard-opts)))
                        mutation/run-process! (fn [_] (swap! test-runs inc) (throw (AssertionError. "test child must remain uninvoked")))
                        mutation/custody-failure (fn [error] (reset! caught error) (original-custody-failure error))]
            (let [result (try (if (= :baseline caller) (mutation/baseline! opts)
                                 (mutation/evaluate-mutant! opts mutant))
                              (catch Exception error error))
                  failure (if (= :baseline caller) (:compile result) (:result result))
                  report-file (io/file backend-dir "target" (str "pretest-info-" (name caller) "-" (name guard) ".edn"))
                  report (when (map? result)
                           (mutation/write-report! {:run? true :source-sha (:source-sha fixture)
                                                    :baseline (if (= :baseline caller) (:status result) :passed)
                                                    :baseline-evidence (when (= :baseline caller) result)
                                                    :report-file (.getPath report-file)}
                                                   (if (= :baseline caller) [] [result])))
                  native (when report (json/read-str (slurp (str report-file ".json")) :key-fn keyword))
                  native-failure (if (= :baseline caller) (get-in native [:baseline_evidence :compile])
                                     (get-in native [:mutants 0 :result]))]
              (is (zero? @test-runs))
              (is (mutation/compiled? @compiled))
              (is (= (if (= :baseline caller) :failed :invalid) (:status result)))
              (is (= "custody-rejected" (:termination failure)))
              (is (nil? (:exit-code failure)))
              (is (= @compiled (get-in failure [:custody-error :compile-result])))
              (is (= {:guard guard :tampered true}
                     (dissoc (:custody-error failure) :compile-result)))
              (is (= (.getMessage guard-error) (:output failure)))
              (is (identical? guard-error (some-> @caught .getCause)))
              (is (not (contains? (:custody-error failure) :test-result)))
              (is (not-any? #(contains? failure %) [:tests :assertions :failures :errors]))
              (is (= 3 (:schema_version native)))
              (is (= "failed" (:status native)))
              (is (= 0 (get-in native [:summary :killed])))
              (is (= 0 (get-in native [:summary :evaluated])))
              (is (= @compiled (get-in native-failure [:custody-error :compile-result])))
              (is (nil? (get-in native [:baseline_evidence :test])))
              (is (= original-source (slurp source-file)))))))
      (finally (delete-fixture! root)))))

(deftest existing-compile-custody-is-not-replaced
  (let [current {:output "current completed compile" :payload {:sha256 "current"}}]
    (doseq [attached [{:output "previously attached compile"} nil]]
      (let [error (ex-info "existing custody failure" {:compile-result attached :guard :payload})]
        (with-redefs [mutation/run-mutant-tests! (fn [_] (throw error))]
          (is (identical? error (try (mutation/run-compiled-tests! {} current)
                                    (catch clojure.lang.ExceptionInfo caught caught))))
          (is (= attached (:compile-result (ex-data error)))))))))

(deftest actual-compiler-inventory-digest-has-cross-runtime-golden-vector
  ;; Three historical compiler entries are a conformance vector, not a full payload.
  (let [files [
               {:path ".shadow-cljs/builds/test-ci/dev/out/cljs-runtime/borkdude.dynaload.js" :kind "runtime-js"
                :bytes 15632 :sha256 "01056d679bdde793374719dc7299db423b8301bd163fcc8251b78fdccf673f7c"}
               {:path ".shadow-cljs/builds/test-ci/dev/out/cljs-runtime/borkdude.dynaload.js.map" :kind "source-map"
                :bytes 21877 :sha256 "c99be5cf888716d794dfce5282d343ca10397fd017e2f96ad0559ab0581512f7"}
               {:path "target/test/test-ci.cjs" :kind "loader"
                :bytes 98437 :sha256 "037202de10342bd13a49ba4cc4c09f978287091ff23e974d3753be452a0c11d4"}]
        code-files (remove #(= "source-map" (:kind %)) files)]
    (is (= "fdea337fc410ac8f655a4c458204beab2e8922d71cdec834d6edc05500fed7a4" (mutation/payload-digest files)))
    (is (= "5a9a3c92dc82b9c895c2686b3b9c2a0bb88c333bb1e0744205abfcd78fdcbc5e" (mutation/payload-digest code-files)))))

(deftest actual-emitted-loader-fragment-is-a-layout-control
  ;; Root plus first two imports are an actual historical fragment, not a full loader.
  (let [{:keys [root backend-dir] :as fixture} (payload-fixture!)
        runtime (io/file backend-dir mutation/payload-runtime)
        loader (io/file backend-dir mutation/payload-loader)
        fragment (str "var SHADOW_IMPORT_PATH = __dirname + '/../../.shadow-cljs/builds/test-ci/dev/out/cljs-runtime';\n"
                      "SHADOW_IMPORT(\"goog.debug.error.js\");\n"
                      "SHADOW_IMPORT(\"goog.dom.nodetype.js\");\n")]
    (try
      (.delete (io/file runtime "demo.js"))
      (.delete (io/file runtime "demo.js.map"))
      (spit loader fragment)
      (spit (io/file runtime "goog.debug.error.js") "global.debug_fixture = true;\n")
      (spit (io/file runtime "goog.dom.nodetype.js") "global.dom_fixture = true;\n")
      (let [snapshot (mutation/payload-snapshot! fixture)]
        (is (= ["goog.debug.error.js" "goog.dom.nodetype.js"] (:imports snapshot)))
        (is (= mutation/payload-runtime (:runtime-root snapshot)))
        (is (= "f6eac5a90eae5e7786018901b7c590100742b15ec3f3a6e1913f8f5462ae1671"
               (:sha256 (first (filter #(= "loader" (:kind %)) (:files snapshot)))))))
      (spit loader (str/replace fragment "/../../.shadow-cljs/" "/../.shadow-cljs/"))
      (is (thrown? clojure.lang.ExceptionInfo (mutation/payload-snapshot! fixture)))
      (finally (delete-fixture! root)))))

(deftest unchanged-loader-cannot-hide-changed-runtime-test-input
  (let [{:keys [root backend-dir payload] :as fixture} (payload-fixture!)
        runtime-file (io/file backend-dir (:runtime-root payload) "demo.js")
        original (slurp runtime-file)
        green {:exit-code 0 :timed-out? false :termination "exited" :cleanup-complete? true
               :capture-truncated? false :output "Ran 1 tests containing 1 assertions.\n0 failures, 0 errors."}]
    (try
      (with-redefs [mutation/run-process! (constantly green)]
        (spit runtime-file "global.demo = false;\n")
        (is (thrown? clojure.lang.ExceptionInfo (mutation/run-mutant-tests! fixture)))
        (spit runtime-file original)
        (.delete runtime-file)
        (is (thrown? clojure.lang.ExceptionInfo (mutation/run-mutant-tests! fixture)))
        (spit runtime-file original)
        (let [result (mutation/run-mutant-tests! fixture)]
          (is (= (:sha256 payload) (:input-payload-sha256 result)))
          (is (true? (:payload-verified-before? result)))
          (is (true? (:payload-verified-after? result)))))
      (with-redefs [mutation/run-process! (fn [_] (spit runtime-file "global.demo = false;\n") green)]
        (is (thrown? clojure.lang.ExceptionInfo (mutation/run-mutant-tests! fixture))))
      (finally (delete-fixture! root)))))

(deftest payload-inventory-and-source-identity-have-independent-guards
  (let [{:keys [root backend-dir payload] :as fixture} (payload-fixture!)
        imported (io/file backend-dir (:runtime-root payload) "demo.js")
        extra (io/file backend-dir (:runtime-root payload) "extra.js")
        source (io/file backend-dir "src/cljs/demo.cljs")
        original-source (slurp source)]
    (try
      (is (= payload (mutation/payload-snapshot! fixture)))
      (is (thrown? clojure.lang.ExceptionInfo (mutation/preserve-payload! (assoc fixture :output-dir "../outside") payload)))
      (is (not (.exists (io/file root "outside"))))
      (is (thrown? clojure.lang.ExceptionInfo (mutation/assert-source-custody! (assoc fixture :source-sha (apply str (repeat 40 "0"))))))
      (spit extra "global.extra = 1;\n")
      (is (thrown? clojure.lang.ExceptionInfo (mutation/payload-snapshot! fixture)))
      (.delete extra)
      (.delete imported)
      (java.nio.file.Files/createSymbolicLink (.toPath imported) (.toPath source) (make-array java.nio.file.attribute.FileAttribute 0))
      (is (thrown? clojure.lang.ExceptionInfo (mutation/payload-snapshot! fixture)))
      (.delete imported)
      (spit imported "global.demo = true;\n")
      (with-redefs [mutation/run-process! (fn [_] (spit source "(def value false)\n") {:output "source changed"})]
        (is (thrown? clojure.lang.ExceptionInfo (mutation/run-mutant-tests! fixture))))
      (spit source original-source)
      (is (true? (mutation/assert-source-custody! fixture)))
      (let [before (mutation/payload-snapshot! fixture)]
        (spit (io/file backend-dir (:runtime-root payload) "demo.js.map") "{\"changed\":true}\n")
        (let [after-map (mutation/payload-snapshot! fixture)]
          (is (not= (:sha256 before) (:sha256 after-map)))
          (is (= (:code-sha256 before) (:code-sha256 after-map))))
        (spit imported "global.demo = false;\n")
        (let [after-js (mutation/payload-snapshot! fixture)]
          (is (= (first (filter #(= "loader" (:kind %)) (:files before)))
                 (first (filter #(= "loader" (:kind %)) (:files after-js)))))
          (is (not= (:code-sha256 before) (:code-sha256 after-js)))))
      (finally (delete-fixture! root)))))

(deftest actual-shadow-map-layout-has-optional-debug-companions
  (let [{:keys [root backend-dir] :as fixture} (payload-fixture!)
        runtime (io/file backend-dir mutation/payload-runtime)]
    (try
      (spit (io/file backend-dir mutation/payload-loader) "SHADOW_IMPORT(\"shadow.module.main.append.js\");\n" :append true)
      (spit (io/file runtime "shadow.module.main.append.js") "global.shadow_append = true;\n")
      (spit (io/file runtime "goog.base.js.map") "{}\n")
      (let [snapshot (try (mutation/payload-snapshot! fixture) (catch clojure.lang.ExceptionInfo _ ::refused))]
        (is (not= ::refused snapshot))
        (is (= 2 (count (filter #(= "source-map" (:kind %)) (:files snapshot))))))
      (spit (io/file runtime "unknown.js.map") "{}\n")
      (is (thrown? clojure.lang.ExceptionInfo (mutation/payload-snapshot! fixture)))
      (finally (delete-fixture! root)))))

(deftest unchanged-code-is-invalid-and-actual-test-input-is-source-bound
  (let [{:keys [root backend-dir payload] :as fixture} (payload-fixture!)
        src (io/file backend-dir "src/cljs")
        source (io/file src "demo.cljs")
        original (slurp source)
        mutant (first (mutation/discover-mutants {:src-dir (.getPath src)}))
        opts (assoc fixture :src-dir (.getPath src) :baseline-code-sha256 (:code-sha256 payload))
        tests (atom 0)
        completed {:timed-out? false :termination "exited" :cleanup-complete? true :capture-truncated? false}
        compile-result (assoc completed :exit-code 0 :output "[:test-ci] Build completed. (2 files, 1 compiled, 0 warnings, 0.1s)")]
    (try
      (with-redefs [mutation/run-process! (fn [{:keys [cmd]}]
                                           (if (= "pnpm" (first cmd)) compile-result
                                               (do (swap! tests inc) (assoc completed :exit-code 1 :output "Ran 1 tests containing 1 assertions.\n1 failures, 0 errors."))))]
        (let [result (mutation/evaluate-mutant! opts mutant)]
          (is (= :invalid (:status result)))
          (is (= :custody (:phase result)))
          (is (zero? @tests))
          (is (= original (slurp source)))))
      (with-redefs [mutation/run-process! (fn [{:keys [cmd]}]
                                           (if (= "pnpm" (first cmd))
                                             (do (spit (io/file backend-dir (:runtime-root payload) "demo.js") "global.demo = false;\n") compile-result)
                                             (do (swap! tests inc) (assoc completed :exit-code 1 :output "Ran 1 tests containing 1 assertions.\n1 failures, 0 errors."))))]
        (let [result (mutation/evaluate-mutant! opts mutant)]
          (is (= :killed (:status result)))
          (is (= 1 @tests))
          (is (= original (slurp source)))
          (is (= (:source-binding result) (get-in result [:compile :payload :source-binding]) (get-in result [:result :source-binding])))
          (is (= (get-in result [:compile :payload :sha256]) (get-in result [:result :input-payload-sha256])))
          (is (true? (get-in result [:result :source-verified-after?])))))
      (finally (delete-fixture! root)))))

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
  (let [{:keys [root payload]} (payload-fixture!)
        complete {:exit-code 0 :timed-out? false :termination "exited" :cleanup-complete? true
                  :capture-truncated? false :payload payload :source-verified-before? true :source-verified-after? true
                  :artifact-path "target/test/test-ci.cjs"
                  :artifact-sha256 (:sha256 (some #(when (= "loader" (:kind %)) %) (:files payload)))
                  :output "[:test-ci] Build completed. (10 files, 1 compiled, 0 warnings, 0.1s)"}]
    (try
    (is (mutation/compiled? complete))
    (is (not (mutation/compiled? (assoc complete :output ""))))
    (is (not (mutation/compiled? (dissoc complete :artifact-sha256))))
    (is (not (mutation/compiled? (assoc complete :output "[:server] Build completed. (10 files, 1 compiled, 0 warnings, 0.1s)"))))
    (is (not (mutation/compiled? (dissoc complete :payload))))
    (is (not (mutation/compiled? (assoc complete :source-verified-after? false))))
    (finally (delete-fixture! root)))))

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

;; Terminal cleanup is characterized with spies; the parent test JVM is never
;; shut down. Real exit-time observation belongs to a separate owned subprocess.
(deftest cli-main-finalizes-source-bindings-and-suite-success
  (let [events (atom [])
        sha (apply str (repeat 40 "a"))
        manifest {:schema-version 1 :source-sha sha :candidates []}
        report {:status :planned}]
    (with-redefs [mutation/source-bindings-manifest! (fn [opts]
                                                     (swap! events conj [:bindings opts]) manifest)
                  mutation/run-suite! (fn [opts] (swap! events conj [:suite opts]) report)
                  clojure.core/flush (fn [] (swap! events conj :flush))
                  clojure.core/shutdown-agents (fn [] (swap! events conj :shutdown))]
      (let [output (with-out-str (is (nil? (mutation/-main "--source-bindings" sha))))]
        (is (= manifest (json/read-str output :key-fn keyword)))
        (is (= [[:bindings {:source-sha sha}] :flush :shutdown] @events)))
      (reset! events [])
      (is (identical? report (mutation/-main "--dry-run")))
      (is (= [:flush :shutdown] (vec (rest @events))))
      (is (= :suite (ffirst @events)))
      (is (true? (get-in @events [0 1 :dry-run?]))))))

(deftest cli-main-finalizes-help-without-running-suite
  (let [events (atom [])]
    (with-redefs [mutation/run-suite! (fn [_] (throw (AssertionError. "Help must not run a suite")))
                  clojure.core/flush (fn [] (swap! events conj :flush))
                  clojure.core/shutdown-agents (fn [] (swap! events conj :shutdown))]
      (is (= (str mutation/usage "\n") (with-out-str (mutation/-main "--help"))))
      (is (= [:flush :shutdown] @events)))))

(deftest cli-main-finalizes-refusals-and-propagates-fatal-errors
  (doseq [phase [:source-arity :source :parse :suite :fatal]]
    (let [events (atom [])
          failure (if (= :fatal phase) (AssertionError. "Fatal CLI fixture") (ex-info "CLI fixture failure" {:phase phase}))
          args (case phase
                 :source-arity ["--source-bindings"]
                 :source ["--source-bindings" (apply str (repeat 40 "a"))]
                 ["--dry-run"])]
      (with-redefs [mutation/source-bindings-manifest! (fn [_] (throw failure))
                    mutation/parse-args (fn [_] (if (= :parse phase) (throw failure) {:dry-run? true}))
                    mutation/run-suite! (fn [_] (throw failure))
                    clojure.core/flush (fn [] (swap! events conj :flush))
                    clojure.core/shutdown-agents (fn [] (swap! events conj :shutdown))]
        (let [caught (try (apply mutation/-main args)
                          (catch clojure.lang.ExceptionInfo error error)
                          (catch AssertionError error error))]
          (if (= :source-arity phase)
            (do (is (instance? clojure.lang.ExceptionInfo caught))
                (is (= "Source binding command requires only one immutable SHA" (.getMessage ^Exception caught))))
            (is (identical? failure caught)))
          (is (= [:flush :shutdown] @events)))))))

(deftest cli-main-shuts-down-agents-even-if-output-flush-fails
  (let [events (atom [])
        failure (java.io.IOException. "Injected flush failure")]
    (with-redefs [mutation/run-suite! (constantly {:status :planned})
                  clojure.core/flush (fn [] (swap! events conj :flush) (throw failure))
                  clojure.core/shutdown-agents (fn [] (swap! events conj :shutdown))]
      (is (identical? failure (try (mutation/-main "--dry-run") (catch java.io.IOException error error))))
      (is (= [:flush :shutdown] @events)))))


;; Actual repeated-discovery regression: rewrite-clj FnNode sexpr expands fresh
;; argument gensyms. Source syntax, not a regex over user symbols, supplies identity.
(deftest anonymous-compound-discovery-and-source-rewrites-are-stable
  (doseq [[source operator expected]
          [["(if ready #(+ % 1) p1__5438#)" :if-test-negation
            "(if (not ready) #(+ % 1) p1__5438#)"]
           ["(if (some #(= (:root %) (:root entry)) acc) acc (conj acc entry))" :if-test-negation
            "(if (not (some #(= (:root %) (:root entry)) acc)) acc (conj acc entry))"]
           ["(if ok [#(+ %1 1) #(vector %1 %2 %&)] \"p1__5438#\")" :if-test-negation
            "(if (not ok) [#(+ %1 1) #(vector %1 %2 %&)] \"p1__5438#\")"]
           ["(< (count (filter #(true? %) xs)) 1)" :comparison-flip
            "(<= (count (filter #(true? %) xs)) 1)"]
           ["(+ (count (filter #(true? %) xs)) 1)" :arithmetic-operator-flip
            "(- (count (filter #(true? %) xs)) 1)"]
           ["(if ready\n ; keep this comment\n #(+ % 1) p1__5438#)" :if-test-negation
            "(if (not ready)\n ; keep this comment\n #(+ % 1) p1__5438#)"]]]
    (let [discover #(mutation/assign-mutant-ids (mutation/mutants-in-source "anonymous.cljs" source))
          first-read (discover)
          second-read (discover)
          mutant (first (filter #(= operator (:operator %)) first-read))
          second-mutant (first (filter #(= operator (:operator %)) second-read))
          attempt (fn [f] (try (f) (catch Exception error error)))
          rewritten (attempt #(mutation/apply-mutant-to-source source mutant))
          first-binding (attempt #(mutation/bind-mutant-source source mutant))
          second-binding (attempt #(mutation/bind-mutant-source source second-mutant))]
      (is (= first-read second-read))
      (is (= (:fingerprint mutant) (:fingerprint second-mutant)))
      (is (= expected rewritten))
      (is (map? first-binding))
      (is (= first-binding second-binding))
      (is (= (mutation/sha256 expected) (:mutated-sha256 first-binding)))
      (is (thrown? clojure.lang.ExceptionInfo
                   (mutation/apply-mutant-to-source source (assoc mutant :replacement "false")))))))

(deftest anonymous-compound-immutable-manifest-reconstruction-is-stable
  (let [{:keys [root backend-dir] :as fixture} (payload-fixture!)
        file (io/file backend-dir "src/cljs/demo.cljs")
        source "(defn f [ready] (if ready #(+ % 1) false))\n"]
    (try
      (spit file source)
      (doseq [args [["git" "add" "backend/src/cljs/demo.cljs"]
                    ["git" "-c" "user.name=Fixture" "-c" "user.email=fixture@example.invalid"
                     "commit" "--quiet" "-m" "inert anonymous source fixture"]]]
        (let [result (apply shell/sh (concat args [:dir (.getPath root)]))]
          (when-not (zero? (:exit result)) (throw (ex-info "Fixture commit failed" result)))))
      (let [source-sha (str/trim (:out (shell/sh "git" "rev-parse" "HEAD" :dir (.getPath root))))
            opts (assoc fixture :source-sha source-sha)
            attempt (fn [f] (try (f) (catch Exception error error)))]
        (with-redefs [mutation/run-process! (fn [_] (throw (AssertionError. "Identity reconstruction cannot compile or test")))]
          (let [first-manifest (attempt #(mutation/source-bindings-manifest! opts))
                second-manifest (attempt #(mutation/source-bindings-manifest! opts))]
            (is (map? first-manifest))
            (is (= first-manifest second-manifest))
            (is (seq (:candidates first-manifest)))
            (is (= source (slurp file)))
            (is (true? (mutation/assert-source-custody! opts))))))
      (finally (delete-fixture! root)))))

(deftest actual-boolean-c533-syntax-identity-remains-unchanged
  (let [source (str (apply str (repeat 70 "\n")) "        true\n")
        first-read (mutation/assign-mutant-ids (mutation/mutants-in-source "knoxx/backend/bootstrap.cljs" source))
        second-read (mutation/assign-mutant-ids (mutation/mutants-in-source "knoxx/backend/bootstrap.cljs" source))
        candidate (first first-read)]
    (is (= first-read second-read))
    (is (= 71 (:line candidate)))
    (is (= 9 (:column candidate)))
    (is (= "c5331e130ef389a0c7c8f22f68ef799fcea6c121f355c70c564643050a068081" (:fingerprint candidate)))
    (is (= true (:original-form candidate)))
    (is (= false (:replacement-form candidate)))
    (is (not (contains? candidate :syntax-preserving?)))))
