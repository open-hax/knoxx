(ns knoxx.mutation-test
  "S-expression mutation harness for Knoxx ClojureScript backend sources.

  The harness mutates original CLJS forms with rewrite-clj, temporarily
  overwrites the real source file per mutant, and delegates compile/test
  execution to shadow-cljs. It intentionally stays in Clojure so mutation
  operators work on Lisp data, not generated JavaScript."
  (:require [clojure.data.json :as json]
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
       (str/ends-with? (.getName file) ".cljs")))

(defn source-files
  [src-dir]
  (->> (file-seq (io/file src-dir))
       (filter cljs-source-file?)
       (sort-by #(.getPath ^File %))))

(defn loc-mutations
  [relative-path loc]
  (let [form (safe-sexpr loc)]
    (when-not (= ::unreadable form)
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
             (into acc (loc-mutations relative-path loc))))))

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
  [mutants {:keys [batch-index batch-count limit] :or {batch-index 0 batch-count 1 limit default-limit}}]
  (when-not (and (pos? batch-count) (<= 0 batch-index) (< batch-index batch-count))
    (throw (ex-info "Invalid mutation batch index/count" {})))
  (let [batch (filter #(= batch-index (mod (:ordinal %) batch-count)) mutants)]
    (vec (if (pos? limit) (take limit batch) batch))))

(defn discover-mutants
  [{:keys [src-dir include-regex] :or {src-dir default-src-dir} :as opts}]
  (let [pattern (when-not (str/blank? include-regex) (re-pattern include-regex))]
    (->> (source-files src-dir)
         (filter #(or (nil? pattern) (re-find pattern (relative-path src-dir %))))
         (mapcat #(mutants-in-file src-dir %))
         assign-mutant-ids
         (#(partition-mutants % opts)))))

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
  (let [result (shell/sh "git" "status" "--porcelain" "--" src-dir)]
    (when (not= 0 (:exit result))
      (throw (ex-info "git status failed while checking for uncommitted changes"
                      {:src-dir src-dir :result result})))
    (when-let [dirty (seq (remove str/blank? (str/split-lines (:out result))))]
      (throw (ex-info (str "Refusing to run mutation tests against a dirty " src-dir
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
  (let [builder (doto (ProcessBuilder. ^java.util.List cmd)
                  (.directory (io/file "."))
                  (.redirectErrorStream true))
        process (.start builder)
        output-future (future (slurp (.getInputStream process)))
        finished? (.waitFor process timeout-ms TimeUnit/MILLISECONDS)]
    (if finished?
      (process-result (.exitValue process) false @output-future)
      (do
        (.destroyForcibly process)
        (process-result 124 true @output-future)))))

(defn test-counters [output]
  (let [counters (re-seq #"(?m)\b(\d+) failures?,\s*(\d+) errors?\." output)
        totals (re-seq #"(?m)Ran (\d+) tests containing (\d+) assertions\." output)]
    (when (and (seq counters) (seq totals))
      {:failures (reduce + (map #(parse-long (nth % 1)) counters))
       :errors (reduce + (map #(parse-long (nth % 2)) counters))
       :assertions (reduce + (map #(parse-long (nth % 2)) totals))})))

(defn killed?
  "Only completed test assertions can kill a source mutation."
  [{:keys [timed-out? output]}]
  (let [{:keys [failures errors assertions]} (test-counters output)]
    (boolean (and (not timed-out?) (pos? (or assertions 0))
                  (or (pos? failures) (pos? errors))))))

(defn passed-tests? [{:keys [exit-code timed-out? output]}]
  (let [{:keys [failures errors assertions]} (test-counters output)]
    (boolean (and (zero? exit-code) (not timed-out?) (pos? (or assertions 0))
                  (zero? failures) (zero? errors)))))

(defn compiled? [{:keys [exit-code timed-out? output]}]
  (and (zero? exit-code) (not timed-out?)
       (not (re-find #"\b[1-9][0-9]* warnings" output))))

(defn compile-mutant!
  [{:keys [shadow-build timeout-ms] :or {shadow-build default-shadow-build timeout-ms default-timeout-ms}}]
  (run-process! {:cmd ["pnpm" "exec" "shadow-cljs" "--force-spawn" "compile" shadow-build]
                 :timeout-ms timeout-ms}))

(defn run-mutant-tests!
  [{:keys [timeout-ms] :or {timeout-ms default-timeout-ms}}]
  (run-process! {:cmd ["node" "target/test/test-ci.cjs"]
                 :timeout-ms timeout-ms}))

(defn evaluate-mutant!
  [opts mutant]
  (with-mutated-source! opts mutant
    (fn []
      (let [compile-result (compile-mutant! opts)]
        (if-not (compiled? compile-result)
          (assoc mutant
                 :status :invalid
                 :phase :compile
                 :result (select-keys compile-result [:exit-code :timed-out?]))
          (let [test-result (run-mutant-tests! opts)]
            (assoc mutant
                   :status (cond (killed? test-result) :killed
                                 (passed-tests? test-result) :survived
                                 :else :invalid)
                   :phase :test
                   :result (merge (select-keys test-result [:exit-code :timed-out?])
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
  [{:keys [output-dir report-file baseline source-sha batch-index min-evaluated run?]
    :or {output-dir default-output-dir batch-index 0 min-evaluated 1}} results]
  (let [path (io/file (or report-file (str output-dir "/report.edn")))
        passed? (and run? (= :passed baseline) (>= (count results) min-evaluated)
                     (every? #(and (= :killed (:status %)) (= :test (:phase %))) results))
        report {:summary (report-summary results) :mutants results
                :source_sha source-sha :batch_index batch-index :baseline baseline
                :status (if passed? :passed (if run? :failed :planned))}]
    (when-let [parent (.getParentFile path)] (.mkdirs parent))
    (spit path (with-out-str (pprint/pprint report)))
    (spit (str path ".json") (json/write-str (update report :mutants #(mapv (fn [mutant] (select-keys mutant [:id :fingerprint :status :phase :result])) %))))
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
  (if (and (compiled? (compile-mutant! opts)) (passed-tests? (run-mutant-tests! opts)))
    :passed :failed))

(defn run-suite! [opts]
  (when (:run? opts) (assert-clean-tree! opts))
  (let [baseline (if (:run? opts) (baseline! opts) :not-run)
        mutants (discover-mutants opts)
        results (cond (not (:run? opts)) (dry-run-results mutants)
                      (= :passed baseline) (mapv #(evaluate-mutant! opts %) mutants)
                      :else [])
        source-sha (str/trim (:out (shell/sh "git" "rev-parse" "HEAD")))
        report (write-report! (assoc opts :baseline baseline :source-sha source-sha) results)]
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
