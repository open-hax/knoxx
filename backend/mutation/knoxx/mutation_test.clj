(ns knoxx.mutation-test
  "S-expression mutation harness for Knoxx ClojureScript backend sources.

  The harness mutates original CLJS forms with rewrite-clj, emits a temporary
  source overlay per mutant, and delegates compile/test execution to shadow-cljs.
  It intentionally stays in Clojure so mutation operators work on Lisp data, not
  generated JavaScript."
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.tools.reader :as reader]
            [clojure.tools.reader.reader-types :as reader-types]
            [rewrite-clj.zip :as z])
  (:import [java.io File]
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

(defn assign-mutant-ids
  [mutants]
  (mapv (fn [idx mutant]
          (assoc mutant :id (format "m%04d" (inc idx))))
        (range)
        mutants))

(defn discover-mutants
  [{:keys [src-dir include-regex limit]
    :or {src-dir default-src-dir
         limit default-limit}}]
  (let [include-pattern (when-not (str/blank? include-regex)
                          (re-pattern include-regex))
        candidates (->> (source-files src-dir)
                        (filter (fn [file]
                                  (or (nil? include-pattern)
                                      (re-find include-pattern (relative-path src-dir file)))))
                        (mapcat #(mutants-in-file src-dir %)))
        limited (if (and limit (pos? limit))
                  (take limit candidates)
                  candidates)]
    (assign-mutant-ids limited)))

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

(defn mutant-overlay-source-path
  [output-dir mutant]
  (.getPath (io/file output-dir "mutants" (:id mutant) "src" "cljs" (:relative-path mutant))))

(defn emit-mutant-source!
  [{:keys [src-dir output-dir] :or {src-dir default-src-dir output-dir default-output-dir}}
   mutant]
  (let [original-path (io/file src-dir (:relative-path mutant))
        mutated-path (io/file (mutant-overlay-source-path output-dir mutant))
        mutated-source (apply-mutant-to-source (slurp original-path) mutant)]
    (.mkdirs (.getParentFile mutated-path))
    (spit mutated-path mutated-source)
    (assoc mutant
           :overlay-path (.getPath mutated-path))))

(defn shadow-config-merge
  [mutant]
  {:source-paths [(str "target/mutation/mutants/" (:id mutant) "/src/cljs")
                  "src/cljs"
                  "test/cljs"]})

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

(defn test-counters
  [output]
  (when-let [[_ failures errors] (re-find #"(?m)\b(\d+) failures?,\s*(\d+) errors?\." output)]
    {:failures (parse-long failures)
     :errors (parse-long errors)}))

(defn killed?
  [{:keys [exit-code timed-out? output]}]
  (let [{:keys [failures errors]} (or (test-counters output) {})]
    (or timed-out?
        (not= 0 exit-code)
        (pos? (or failures 0))
        (pos? (or errors 0)))))

(defn compile-mutant!
  [{:keys [shadow-build timeout-ms] :or {shadow-build default-shadow-build timeout-ms default-timeout-ms}}
   mutant]
  (run-process! {:cmd ["pnpm" "exec" "shadow-cljs" "--force-spawn"
                       "--config-merge" (pr-str (shadow-config-merge mutant))
                       "compile" shadow-build]
                 :timeout-ms timeout-ms}))

(defn run-mutant-tests!
  [{:keys [timeout-ms] :or {timeout-ms default-timeout-ms}}]
  (run-process! {:cmd ["node" "target/test/test-ci.cjs"]
                 :timeout-ms timeout-ms}))

(defn evaluate-mutant!
  [opts mutant]
  (let [emitted (emit-mutant-source! opts mutant)
        compile-result (compile-mutant! opts emitted)]
    (if (killed? compile-result)
      (assoc emitted
             :status :killed
             :phase :compile
             :result (select-keys compile-result [:exit-code :timed-out?]))
      (let [test-result (run-mutant-tests! opts)]
        (assoc emitted
               :status (if (killed? test-result) :killed :survived)
               :phase :test
               :result (merge (select-keys test-result [:exit-code :timed-out?])
                              (or (test-counters (:output test-result)) {})))))))

(defn report-summary
  [results]
  (let [total (count results)
        killed (count (filter #(= :killed (:status %)) results))
        survived (count (filter #(= :survived (:status %)) results))
        planned (count (filter #(= :planned (:status %)) results))
        evaluated (+ killed survived)]
    {:total total
     :planned planned
     :evaluated evaluated
     :killed killed
     :survived survived
     :score (when (pos? evaluated)
              (double (/ killed evaluated)))}))

(defn write-report!
  [{:keys [output-dir report-file] :or {output-dir default-output-dir}}
   results]
  (let [report-file (or report-file (str output-dir "/report.edn"))
        report {:summary (report-summary results)
                :mutants results}]
    (.mkdirs (.getParentFile (io/file report-file)))
    (spit report-file (with-out-str (pprint/pprint report)))
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

(defn run-suite!
  [opts]
  (let [mutants (discover-mutants opts)
        results (if (:run? opts)
                  (mapv #(evaluate-mutant! opts %) mutants)
                  (dry-run-results mutants))
        report (write-report! opts results)]
    (print-summary! report)
    (when (and (:run? opts)
               (some #(= :survived (:status %)) results))
      (System/exit 2))))

(defn -main
  [& args]
  (let [opts (parse-args args)]
    (if (:help? opts)
      (println usage)
      (run-suite! opts))))
