(ns knoxx.mutation-test
  "S-expression mutation harness for Knoxx ClojureScript backend sources.

  The harness mutates original CLJS forms with rewrite-clj, temporarily
  overwrites the real source file per mutant, and delegates compile/test
  execution to shadow-cljs. It intentionally stays in Clojure so mutation
  operators work on Lisp data, not generated JavaScript."
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.tools.reader :as reader]
            [clojure.tools.reader.reader-types :as reader-types]
            [rewrite-clj.zip :as z])
  (:import [java.io File]
           [java.security MessageDigest]
           [java.util.concurrent TimeUnit]))

(def default-src-dir "src/cljs")
(def default-output-dir "target/mutation")
(def default-shadow-build "test-ci")
(def default-timeout-ms 120000)
(def default-limit 100)

(def comparison-replacements
  {'< '<=
   '<= '<
   '> '>=
   '>= '>
   '= 'not=
   'not= '=})

(def arithmetic-replacements
  {'+ '-
   '- '+
   '* '/
   '/ '*})

(defn cljs-reader-data-readers
  "Data-reader bindings needed for CLJS literals when using tools.reader for
  sanity analysis. rewrite-clj owns source-preserving rewrites; tools.reader is
  used only to prove the source can be read as Lisp data."
  []
  (assoc reader/*data-readers* 'js identity))

(defn read-top-level-sexprs
  [source]
  (binding [reader/*data-readers* (cljs-reader-data-readers)]
    (let [rdr (reader-types/string-push-back-reader source)]
      (loop [forms []]
        (let [form (reader/read {:eof ::eof
                                 :read-cond :allow
                                 :features #{:cljs}}
                                rdr)]
          (if (= ::eof form)
            forms
            (recur (conj forms form))))))))

(defn list-form?
  [form]
  (or (list? form) (seq? form)))

(defn if-form?
  [form]
  (and (list-form? form)
       (= 'if (first form))
       (<= 3 (count form))))

(defn replace-list-head
  [form replacement]
  (apply list replacement (rest form)))

(defn negate-if-test
  [form]
  (let [[_ test & branches] form]
    (apply list 'if (list 'not test) branches)))

(defn literal-mutant
  [form]
  (cond
    (= true form) {:operator :boolean-literal-flip
                   :replacement false}
    (= false form) {:operator :boolean-literal-flip
                    :replacement true}
    (= 0 form) {:operator :numeric-literal-zero-to-one
                :replacement 1}
    (= 1 form) {:operator :numeric-literal-one-to-zero
                :replacement 0}
    :else nil))

(defn form-mutations
  "Pure mutation operators over one s-expression. Returns mutation descriptors
  with operator ids and replacement forms."
  [form]
  (cond-> []
    (if-form? form)
    (conj {:operator :if-test-negation
           :replacement (negate-if-test form)})

    (and (list-form? form) (contains? comparison-replacements (first form)))
    (conj {:operator :comparison-flip
           :replacement (replace-list-head form (comparison-replacements (first form)))})

    (and (list-form? form) (contains? arithmetic-replacements (first form)))
    (conj {:operator :arithmetic-operator-flip
           :replacement (replace-list-head form (arithmetic-replacements (first form)))})

    (some? (literal-mutant form))
    (conj (literal-mutant form))))

(defn safe-sexpr
  [loc]
  (try
    (z/sexpr loc)
    (catch Throwable _
      ::unreadable)))

(defn safe-position
  [loc]
  (try
    (let [[line column] (z/position loc)]
      {:line line :column column})
    (catch Throwable _
      {})))

(defn relative-path
  [root file]
  (str/replace (.toString (.relativize (.toPath (io/file root)) (.toPath (io/file file)))) #"\\\\" "/"))

(defn cljs-source-file?
  [^File file]
  (and (.isFile file)
       (some #(str/ends-with? (.getName file) %) [".cljs" ".cljc"])))

(defn active-cljs-location?
  "Keep source-preserving zipper locations inside the selected CLJS feature.
  Reader-conditional feature keys and inactive branches are not executable code."
  [loc]
  (loop [cursor loc]
    (if (= :uneval (z/tag cursor))
      false
      (if-let [parent (z/up cursor)]
      (let [owner (z/up parent)
            conditional? (and (= :list (z/tag parent)) owner
                              (= :reader-macro (z/tag owner))
                              (str/starts-with? (z/string owner) "#?"))]
        (if conditional?
          (let [pairs (partition 2 (take-while identity (iterate z/right (z/down parent))))
                selected (some (fn [[feature branch]]
                                 (when (contains? #{:cljs :default} (safe-sexpr feature)) branch)) pairs)]
            (and selected (= (z/position selected) (z/position cursor)) (recur parent)))
          (recur parent)))
        true))))

(defn source-files
  [src-dir]
  (->> (file-seq (io/file src-dir))
       (filter cljs-source-file?)
       (sort-by #(.getPath ^File %))))

(defn reader-conditional-free-location?
  "Do not reify a conditional-containing compound as rewrite-clj's read-string.
  Active descendant forms can still be mutated at their own exact locations."
  [loc]
  (not (some #(and (= :reader-macro (z/tag %)) (str/starts-with? (z/string %) "#?"))
             (tree-seq #(boolean (z/down %))
                       #(take-while identity (iterate z/right (z/down %))) loc))))

(defn loc-mutations
  [relative-path loc]
  (let [form (safe-sexpr loc)]
    (when (and (not= ::unreadable form) (reader-conditional-free-location? loc))
      (let [{:keys [line column]} (safe-position loc)]
        (mapv (fn [{:keys [operator replacement]}]
                {:relative-path relative-path
                 :line line
                 :column column
                 :operator operator
                 :original-form form
                 :replacement-form replacement
                 :original (pr-str form)
                 :replacement (pr-str replacement)})
              (form-mutations form))))))

(defn mutants-in-source
  [relative-path source]
  ;; tools.reader supplies a reader-level sanity pass; rewrite-clj supplies the
  ;; zipper we mutate so comments and surrounding source survive in temp output.
  (read-top-level-sexprs source)
  (loop [loc (z/of-string source {:track-position? true})
         acc []]
    (if (z/end? loc)
      acc
      (recur (z/next loc)
             (if (active-cljs-location? loc)
               (into acc (loc-mutations relative-path loc)) acc)))))

(defn mutants-in-file
  [src-dir file]
  (let [source (slurp file)
        rel (relative-path src-dir file)]
    (mutants-in-source rel source)))

(defn fingerprint
  "Stable source mutation identity, independent of batch size and enumeration limits."
  [mutant]
  (let [bytes (.digest (MessageDigest/getInstance "SHA-256")
                (.getBytes (pr-str (mapv mutant [:relative-path :line :column :operator :original :replacement])) "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 0xff %)) bytes))))

(defn assign-mutant-ids [mutants]
  (mapv (fn [idx mutant] (assoc mutant :id (format "m%04d" (inc idx)) :ordinal idx
                                      :fingerprint (fingerprint mutant)))
        (range) mutants))

(defn partition-mutants
  "Disjoint deterministic batches; limiting happens after partitioning."
  [mutants {:keys [batch-index batch-count limit equivalent-fingerprints]
            :or {batch-index 0 batch-count 1 limit default-limit equivalent-fingerprints #{}}}]
  (when-not (and (pos? batch-count) (<= 0 batch-index) (< batch-index batch-count))
    (throw (ex-info "Invalid mutation batch index/count" {})))
  (let [batch (filter #(and (= batch-index (mod (:ordinal %) batch-count))
                           (not (contains? equivalent-fingerprints (:fingerprint %)))) mutants)]
    (vec (if (pos? limit) (take limit batch) batch))))

(defn discover-candidates
  "Read all candidate identities before partitioning or reviewed exclusions."
  [{:keys [src-dir include-regex] :or {src-dir default-src-dir}}]
  (let [pattern (when-not (str/blank? include-regex) (re-pattern include-regex))]
    (->> (source-files src-dir)
         (filter #(or (nil? pattern) (re-find pattern (relative-path src-dir %))))
         (mapcat #(mutants-in-file src-dir %))
         assign-mutant-ids)))

(defn discover-mutants
  "Select one deterministic partition after any validated equivalent exclusions."
  [opts]
  (partition-mutants (discover-candidates opts) opts))

(defn sha256
  "Digest the exact UTF-8 source or captured output bytes."
  [text]
  (let [bytes (.digest (MessageDigest/getInstance "SHA-256") (.getBytes ^String text "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 0xff %)) bytes))))

(defn validate-equivalents!
  "Validate explicit semantic adjudications against current candidate/file custody.
  The committed manifest supplies review provenance; exclusions never earn kills."
  [manifest candidates file-hashes]
  (let [by-fingerprint (into {} (map (juxt :fingerprint identity) candidates))
        entries (:entries manifest)]
    (when-not (and (= 1 (:schema-version manifest)) (vector? entries)
                   (re-matches #"[0-9a-f]{40}" (or (:basis-source-sha manifest) ""))
                   (= (count entries) (count (set (map :fingerprint entries)))))
      (throw (ex-info "Invalid equivalent adjudication manifest" {})))
    (doseq [{:keys [fingerprint relative-path source-sha256 rationale review-reference]} entries]
      (let [candidate (get by-fingerprint fingerprint)]
        (when-not (and candidate (= relative-path (:relative-path candidate))
                       (= source-sha256 (get file-hashes relative-path))
                       (re-matches #"[0-9a-f]{64}" (or source-sha256 ""))
                       (string? rationale) (not (str/blank? rationale))
                       (string? review-reference)
                       (re-matches #"https://github\.com/open-hax/knoxx/pull/[1-9][0-9]*(?:#[A-Za-z0-9_-]+)?" review-reference))
          (throw (ex-info "Stale or unreviewed equivalent adjudication" {:fingerprint fingerprint})))))
    entries))

(defn load-equivalents!
  "Accept only a byte-exact tracked manifest and source unchanged since its basis.
  This checks custody; PR approval remains the owning repository's review policy."
  [{:keys [equivalents-file src-dir] :or {src-dir default-src-dir}} candidates]
  (if-not equivalents-file
    []
    (let [canonical (.toPath (.getCanonicalFile (io/file ".")))
          path (.toPath (.getCanonicalFile (io/file equivalents-file)))
          _ (when-not (.startsWith path canonical)
              (throw (ex-info "Equivalent manifest must be inside committed checkout" {})))
          rel (.toString (.relativize canonical path))
          content (slurp equivalents-file)
          tracked (shell/sh "git" "show" (str "HEAD:backend/" rel))
          _ (when-not (and (zero? (:exit tracked)) (= content (:out tracked)))
              (throw (ex-info "Equivalent manifest must match committed HEAD bytes" {})))
          manifest (edn/read-string content)
          basis (:basis-source-sha manifest)
          ancestor (shell/sh "git" "merge-base" "--is-ancestor" (str basis) "HEAD")
          file-hashes (into {} (map (fn [file] [(relative-path src-dir file) (sha256 (slurp file))]) (source-files src-dir)))
          entries (validate-equivalents! manifest candidates file-hashes)]
      (when-not (zero? (:exit ancestor))
        (throw (ex-info "Equivalent manifest basis must be an ancestor of source" {})))
      (doseq [{:keys [relative-path source-sha256]} entries]
        (let [original (shell/sh "git" "show" (str basis ":backend/" src-dir "/" relative-path))]
          (when-not (and (zero? (:exit original)) (= source-sha256 (sha256 (:out original))))
            (throw (ex-info "Equivalent candidate source changed since review basis" {:relative-path relative-path})))))
      (mapv #(assoc % :basis-source-sha basis :manifest-sha256 (sha256 content)
                     :manifest-path (str "backend/" rel)) entries))))

(defn same-position?
  [mutant loc]
  (let [{:keys [line column]} (safe-position loc)]
    (and (= line (:line mutant))
         (= column (:column mutant)))))

(defn matching-mutation?
  [mutant mutation]
  (and (= (:operator mutant) (:operator mutation))
       (= (:replacement-form mutant) (:replacement mutation))))

(defn apply-mutant-to-source
  [source mutant]
  (loop [loc (z/of-string source {:track-position? true})]
    (cond
      (z/end? loc)
      (throw (ex-info "Mutant location no longer matches source"
                      {:mutant (select-keys mutant [:id :relative-path :line :column :operator])}))

      (same-position? mutant loc)
      (if-let [mutation (some #(when (matching-mutation? mutant %) %)
                              (form-mutations (safe-sexpr loc)))]
        (z/root-string (z/replace loc (:replacement mutation)))
        (recur (z/next loc)))

      :else
      (recur (z/next loc)))))

(defn mutant-source-path
  [{:keys [src-dir] :or {src-dir default-src-dir}} mutant]
  (io/file src-dir (:relative-path mutant)))

(defn assert-clean-tree!
  "with-mutated-source! restores src-dir via try/finally plus a JVM shutdown
  hook, but neither runs on SIGKILL or power loss. Refusing to start against
  a dirty tree means that worst case is always safely recoverable with
  `git checkout -- <src-dir>`, since nothing uncommitted was ever at risk."
  [{:keys [src-dir] :or {src-dir default-src-dir}}]
  (when-not (= (.getCanonicalPath (io/file src-dir)) (.getCanonicalPath (io/file default-src-dir)))
    (throw (ex-info "Mutation execution must use the compiled src/cljs source tree" {:src-dir src-dir})))
  (let [result (shell/sh "git" "status" "--porcelain" "--untracked-files=all")]
    (when (not= 0 (:exit result))
      (throw (ex-info "git status failed while checking for uncommitted changes"
                      {:src-dir src-dir :result result})))
    (when-let [dirty (seq (remove str/blank? (str/split-lines (:out result))))]
      (throw (ex-info (str "Refusing to run mutation tests against a dirty committed checkout for " src-dir
                           ". Mutation runs temporarily overwrite real source files; "
                           "commit or stash first so a killed run can always recover "
                           "with `git checkout`.")
                      {:src-dir src-dir :dirty-paths dirty})))))

(defn with-mutated-source!
  "backend/shadow-cljs.edn runs shadow-cljs in :deps mode, which hands the
  entire classpath to tools.deps and ignores :source-paths outright (even via
  --config-merge), so a mutant can no longer be layered in ahead of src/cljs
  as an overlay path. Mutating the real file for the duration of one
  compile+test cycle sidesteps that restriction entirely. Mutants run
  strictly sequentially (see run-suite!), so there is never more than one
  file mutated at a time. The shutdown hook guards against the file being
  left mutated if the process is killed mid-mutation."
  [opts mutant f]
  (let [target (mutant-source-path opts mutant)
        original (slurp target)
        mutated (apply-mutant-to-source original mutant)
        restore! #(spit target original)
        hook (Thread. restore!)]
    (.addShutdownHook (Runtime/getRuntime) hook)
    (try
      (spit target mutated)
      (f)
      (finally
        (restore!)
        (.removeShutdownHook (Runtime/getRuntime) hook)))))

(defn process-result
  [exit-code timed-out? output]
  {:exit-code exit-code
   :timed-out? timed-out?
   :output output})

(defn run-process!
  [{:keys [cmd timeout-ms]
    :or {timeout-ms default-timeout-ms}}]
  (let [supervised (into ["python3" "scripts/run-mutation-process.py" "--timeout-ms" (str timeout-ms) "--"] cmd)
        builder (doto (ProcessBuilder. ^java.util.List supervised)
                  (.directory (io/file "."))
                  (.redirectErrorStream true))
        process (.start builder)
        output-future (future (slurp (.getInputStream process)))]
    (try
      (let [finished? (.waitFor process (+ timeout-ms 10000) TimeUnit/MILLISECONDS)
            output (if finished? (deref output-future 1000 ::capture-timeout) ::capture-timeout)]
        (when (or (= ::capture-timeout output) (not (zero? (.exitValue process))))
          (throw (ex-info "Mutation supervisor failed to finish safely" {})))
        (json/read-str output :key-fn keyword))
      (finally
        ;; SIGTERM lets the process-local supervisor reap its owned children.
        ;; Even an interrupted JVM wait must not bypass bounded source cleanup.
        (when (.isAlive process)
          (.destroy process)
          (when-not (.waitFor process 3000 TimeUnit/MILLISECONDS)
            (.destroyForcibly process)
            (.waitFor process 1000 TimeUnit/MILLISECONDS)))
        (.close (.getInputStream process))
        (future-cancel output-future)))))

(defn test-counters [output]
  (let [counters (re-seq #"(?m)\b(\d+) failures?,\s*(\d+) errors?\." output)
        totals (re-seq #"(?m)Ran (\d+) tests? containing (\d+) assertions?\." output)]
    (when (and (seq counters) (= (count counters) (count totals))
               (every? #(and (pos? (parse-long (nth % 1))) (pos? (parse-long (nth % 2)))) totals))
      {:failures (reduce + (map #(parse-long (nth % 1)) counters))
       :errors (reduce + (map #(parse-long (nth % 2)) counters))
       :tests (reduce + (map #(parse-long (nth % 1)) totals))
       :summaries (count totals)
       :assertions (reduce + (map #(parse-long (nth % 2)) totals))})))

(defn completed-execution?
  "Only a normally terminated, fully captured and cleaned subprocess is evidence."
  [{:keys [timed-out? termination cleanup-complete? capture-truncated? output]}]
  (and (false? timed-out?) (= "exited" termination) (true? cleanup-complete?)
       (false? capture-truncated?) (string? output)
       (not (str/includes? output "[shadow-test-guard] FATAL"))))

(defn killed?
  "Only completed test assertions can kill a source mutation."
  [{:keys [exit-code output] :as result}]
  (let [{:keys [failures errors assertions]} (test-counters output)]
    (boolean (and (completed-execution? result) (= 1 exit-code) (pos? (or assertions 0))
                  (or (pos? failures) (pos? errors))))))

(defn passed-tests? [{:keys [exit-code output] :as result}]
  (let [{:keys [failures errors assertions]} (test-counters output)]
    (boolean (and (completed-execution? result) (= 0 exit-code) (pos? (or assertions 0))
                  (zero? failures) (zero? errors)))))

(defn compiled? [{:keys [exit-code output artifact-sha256] :as result}]
  (and (= 0 exit-code) (completed-execution? result)
       (re-find #"\[:test-ci\] Build completed\. \([0-9]+ files, [0-9]+ compiled, 0 warnings," output)
       (re-matches #"[0-9a-f]{64}" (or artifact-sha256 ""))
       (not (re-find #"\b[1-9][0-9]* warnings" output))))

(defn compile-mutant!
  [{:keys [shadow-build timeout-ms] :or {shadow-build default-shadow-build timeout-ms default-timeout-ms}}]
  (when-not (= default-shadow-build shadow-build)
    (throw (ex-info "Mutation execution requires its exact non-autorun test-ci target" {:shadow-build shadow-build})))
  (let [result (run-process! {:cmd ["pnpm" "exec" "shadow-cljs" "--force-spawn" "compile" shadow-build]
                              :timeout-ms timeout-ms})
        artifact (io/file "target/test/test-ci.cjs")]
    (cond-> result
      (.isFile artifact) (assoc :artifact-path "target/test/test-ci.cjs"
                                :artifact-sha256 (sha256 (slurp artifact))))))

(defn run-mutant-tests!
  [{:keys [timeout-ms] :or {timeout-ms default-timeout-ms}}]
  (run-process! {:cmd ["node" "scripts/run-mutation-tests.mjs"]
                 :timeout-ms timeout-ms}))

(defn execution-evidence
  "Retain captured output alongside its hash and admission fields for verification."
  [result]
  (assoc result :output-sha256 (or (:output-sha256 result) (sha256 (:output result)))
                :fatal? (str/includes? (:output result) "[shadow-test-guard] FATAL")))

(defn evaluate-mutant!
  [opts mutant]
  (with-mutated-source! opts mutant
    (fn []
      (let [compile-result (compile-mutant! opts)]
        (if-not (compiled? compile-result)
          (assoc mutant
                 :status :invalid
                 :phase :compile
                 :result (execution-evidence compile-result))
          (let [test-result (run-mutant-tests! opts)]
            (assoc mutant
                   :status (cond (killed? test-result) :killed
                                 (passed-tests? test-result) :survived
                                 :else :invalid)
                   :phase :test
                   :compile (execution-evidence compile-result)
                   :result (merge (execution-evidence test-result)
                                  (or (test-counters (:output test-result)) {})))))))))

(defn report-summary
  [results]
  (let [total (count results)
        killed (count (filter #(= :killed (:status %)) results))
        survived (count (filter #(= :survived (:status %)) results))
        planned (count (filter #(= :planned (:status %)) results))
        evaluated (+ killed survived)]
    {:invalid (count (filter #(= :invalid (:status %)) results))
     :total total
     :planned planned
     :evaluated evaluated
     :killed killed
     :survived survived
     :score (when (pos? evaluated)
              (double (/ killed evaluated)))}))

(defn write-report!
  [{:keys [output-dir report-file baseline baseline-evidence source-sha batch-index min-evaluated run? excluded]
    :or {output-dir default-output-dir batch-index 0 min-evaluated 1}} results]
  (let [path (io/file (or report-file (str output-dir "/report.edn")))
        passed? (and run? (= :passed baseline) (>= (count results) min-evaluated)
                     (every? #(and (= :killed (:status %)) (= :test (:phase %))) results))
        report {:schema_version 2 :summary (assoc (report-summary results) :excluded (count excluded)) :mutants results
                :excluded (vec excluded) :baseline_evidence baseline-evidence
                :source_sha source-sha :batch_index batch-index :baseline baseline
                :status (if passed? :passed (if run? :failed :planned))}]
    (when-let [parent (.getParentFile path)] (.mkdirs parent))
    (spit path (with-out-str (pprint/pprint report)))
    (spit (str path ".json") (json/write-str (update report :mutants #(mapv (fn [mutant] (select-keys mutant [:id :fingerprint :status :phase :compile :result])) %))))
    report))

(defn parse-long-option
  [value default]
  (if (str/blank? (str value))
    default
    (Long/parseLong (str value))))

(defn parse-args
  [args]
  (loop [opts {:src-dir default-src-dir
               :output-dir default-output-dir
               :shadow-build default-shadow-build
               :timeout-ms default-timeout-ms
               :limit default-limit
               :batch-index 0 :batch-count 1 :min-evaluated 1
               :run? false
               :dry-run? false}
         remaining args]
    (let [[arg value & more] remaining]
      (case arg
        nil opts
        "--src-dir" (recur (assoc opts :src-dir value) more)
        "--output-dir" (recur (assoc opts :output-dir value) more)
        "--report" (recur (assoc opts :report-file value) more)
        "--include-regex" (recur (assoc opts :include-regex value) more)
        "--equivalents" (recur (assoc opts :equivalents-file value) more)
        "--shadow-build" (recur (assoc opts :shadow-build value) more)
        "--timeout-ms" (recur (assoc opts :timeout-ms (parse-long-option value default-timeout-ms)) more)
        "--limit" (recur (assoc opts :limit (parse-long-option value default-limit)) more)
        "--batch-index" (recur (assoc opts :batch-index (parse-long-option value 0)) more)
        "--batch-count" (recur (assoc opts :batch-count (parse-long-option value 1)) more)
        "--min-evaluated" (recur (assoc opts :min-evaluated (parse-long-option value 1)) more)
        "--run" (recur (assoc opts :run? true) (cons value more))
        "--dry-run" (recur (assoc opts :dry-run? true) (cons value more))
        "--" (recur opts (cons value more))
        "--help" (assoc opts :help? true)
        (throw (ex-info (str "Unknown option: " arg) {:arg arg}))))))

(def usage
  (str "Usage: clojure -M:mutation [--dry-run|--run] [options]\n\n"
       "Options:\n"
       "  --src-dir DIR           Source directory, default src/cljs\n"
       "  --include-regex REGEX   Restrict mutants by source-relative file path\n"
       "  --equivalents FILE      Explicit committed source-bound review adjudications; zero kill credit\n"
       "  --limit N               Mutant limit, 0 means no limit; default 100\n"
       "  --batch-index N         Zero-based deterministic partition index\n"
       "  --batch-count N         Number of disjoint partitions\n"
       "  --min-evaluated N       Minimum completed test-killed mutations to pass\n"
       "  --output-dir DIR        Mutation output dir, default target/mutation\n"
       "  --report FILE           Report EDN path, default target/mutation/report.edn\n"
       "  --shadow-build BUILD    Shadow build used for mutants, default test-ci\n"
       "  --timeout-ms MS         Compile/test timeout per phase, default 120000\n"
       "\n"
       "Examples:\n"
       "  clojure -M:mutation --dry-run --include-regex 'infra/config.cljs$'\n"
       "  clojure -M:mutation --run --limit 100\n"))

(defn dry-run-results
  [mutants]
  (mapv #(assoc % :status :planned) mutants))

(defn print-summary!
  [report]
  (let [{:keys [total planned evaluated killed survived score]} (:summary report)]
    (println (format "Mutation summary: total=%d planned=%d evaluated=%d killed=%d survived=%d score=%s"
                     total planned evaluated killed survived (if score (format "%.2f" score) "n/a")))
    (doseq [{:keys [id status relative-path line column operator original replacement]} (:mutants report)]
      (println (format "%s %-9s %s:%s:%s %s %s => %s"
                       id (name status) relative-path line column (name operator) original replacement)))))

(defn baseline!
  "An already-failing suite cannot be mutation evidence."
  [opts]
  (let [compile-result (compile-mutant! opts)
        test-result (when (compiled? compile-result) (run-mutant-tests! opts))]
    {:status (if (and test-result (passed-tests? test-result)) :passed :failed)
     :compile (execution-evidence compile-result)
     :test (when test-result (merge (execution-evidence test-result) (test-counters (:output test-result))))}))

(defn run-suite! [opts]
  (when (:run? opts) (assert-clean-tree! opts))
  (let [candidates (discover-candidates opts)
        equivalents (load-equivalents! opts candidates)
        excluded-keys (set (map :fingerprint equivalents))
        excluded (filterv (fn [entry] (let [candidate (some #(when (= (:fingerprint entry) (:fingerprint %)) %) candidates)]
                                       (= (:batch-index opts) (mod (:ordinal candidate) (:batch-count opts))))) equivalents)
        baseline-evidence (when (:run? opts) (baseline! opts))
        baseline (if (:run? opts) (:status baseline-evidence) :not-run)
        mutants (partition-mutants candidates (assoc opts :equivalent-fingerprints excluded-keys))
        results (cond (not (:run? opts)) (dry-run-results mutants)
                      (= :passed baseline) (mapv #(evaluate-mutant! opts %) mutants)
                      :else [])
        source-sha (str/trim (:out (shell/sh "git" "rev-parse" "HEAD")))
        report (write-report! (assoc opts :baseline baseline :baseline-evidence baseline-evidence
                                    :excluded excluded :source-sha source-sha) results)]
    (print-summary! report)
    (when (and (:run? opts) (not= :passed (:status report)))
      (throw (ex-info "Mutation qualification failed: require a green baseline and enough completed test kills" {:summary (:summary report)})))
    report))

(defn -main
  [& args]
  (let [opts (parse-args args)]
    (if (:help? opts)
      (println usage)
      (run-suite! opts))))
