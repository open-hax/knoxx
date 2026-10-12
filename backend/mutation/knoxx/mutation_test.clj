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
  (:import [java.io File IOException]
           [java.nio.file Files]
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

(defn anonymous-function-containing-location?
  "Find actual #() syntax nodes without interpreting or normalizing symbols."
  [loc]
  (boolean
   (some #(= :fn (z/tag %))
         (tree-seq #(boolean (z/down %))
                   #(take-while identity (iterate z/right (z/down %))) loc))))

(defn rewrite-anonymous-compound
  "Change only the selected operator/test; retain nested #() syntax verbatim."
  [loc operator]
  (case operator
    :if-test-negation
    (let [test-loc (z/right (z/down loc))
          negated (z/node (z/of-string (str "(not " (z/string test-loc) ")")))]
      (z/up (z/replace test-loc negated)))
    :comparison-flip
    (let [head (z/down loc)]
      (z/up (z/replace head (comparison-replacements (safe-sexpr head)))))
    :arithmetic-operator-flip
    (let [head (z/down loc)]
      (z/up (z/replace head (arithmetic-replacements (safe-sexpr head)))))
    (throw (ex-info "Unsupported anonymous compound mutation" {:operator operator}))))

(defn loc-mutations
  [relative-path loc]
  (let [form (safe-sexpr loc)]
    (when (and (not= ::unreadable form) (reader-conditional-free-location? loc))
      (let [{:keys [line column]} (safe-position loc)
            syntax? (anonymous-function-containing-location? loc)]
        (mapv (fn [{:keys [operator replacement]}]
                (merge {:relative-path relative-path
                        :line line :column column :operator operator}
                       (if syntax?
                         {:syntax-preserving? true
                          :original (z/string loc)
                          :replacement (z/string (rewrite-anonymous-compound loc operator))}
                         {:original-form form :replacement-form replacement
                          :original (pr-str form) :replacement (pr-str replacement)})))
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
      (if (:syntax-preserving? mutant)
        (let [descriptor-keys [:operator :syntax-preserving? :original :replacement]
              actual (some #(when (= (select-keys mutant descriptor-keys)
                                    (select-keys % descriptor-keys)) %)
                           (loc-mutations (:relative-path mutant) loc))]
          (if actual
            (z/root-string (rewrite-anonymous-compound loc (:operator actual)))
            (recur (z/next loc))))
        (if-let [mutation (some #(when (matching-mutation? mutant %) %)
                                (form-mutations (safe-sexpr loc)))]
          (z/root-string (z/replace loc (:replacement mutation)))
          (recur (z/next loc))))

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

(def payload-loader "target/test/test-ci.cjs")
(def payload-runtime ".shadow-cljs/builds/test-ci/dev/out/cljs-runtime")

(defn sha256-bytes
  "Digest copied artifacts without decoding or normalizing their bytes."
  [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (MessageDigest/getInstance "SHA-256") ^bytes bytes))))

(defn git-result!
  "Read immutable source identity and checkout custody at the backend boundary."
  [opts & args]
  (let [result (apply shell/sh (concat ["git"] args [:dir (or (:backend-dir opts) ".")]))]
    (when-not (zero? (:exit result)) (throw (ex-info "Source custody Git read failed" result)))
    (:out result)))

(defn bind-mutant-source
  "Use the canonical descriptor and rewrite to bind exact source bytes."
  [original mutant]
  (when-not (= (:fingerprint mutant) (fingerprint mutant))
    (throw (ex-info "Mutation fingerprint differs from canonical descriptor" {})))
  {:kind "mutant" :fingerprint (:fingerprint mutant)
   :path (str "src/cljs/" (:relative-path mutant))
   :original-sha256 (sha256 original)
   :mutated-sha256 (sha256 (apply-mutant-to-source original mutant))})

(defn source-binding!
  "Bind a mutation to its original committed bytes and exact expected rewrite."
  [opts mutant]
  (if-not mutant
    {:kind "baseline"}
    (let [path (str "src/cljs/" (:relative-path mutant))
          original (git-result! opts "show" (str (:source-sha opts) ":backend/" path))]
      (bind-mutant-source original mutant))))

(defn assert-source-custody!
  "Permit only the exact current source mutation; refuse HEAD, index or input drift."
  [{:keys [source-sha source-binding backend-dir] :as opts}]
  (let [binding (or source-binding {:kind "baseline"})
        mutant? (= "mutant" (:kind binding))
        dirty (remove str/blank? (str/split (git-result! opts "diff" "--name-only" "-z" "HEAD") #"\u0000"))
        expected-path (str "backend/" (:path binding))]
    (when-not (and (re-matches #"[0-9a-f]{40}" (or source-sha ""))
                   (= source-sha (str/trim (git-result! opts "rev-parse" "HEAD")))
                   (str/blank? (git-result! opts "diff" "--cached" "--name-only" "-z"))
                   (str/blank? (git-result! opts "ls-files" "--others" "--exclude-standard" "-z"))
                   (if mutant? (= [expected-path] (vec dirty)) (empty? dirty))
                   (if mutant?
                     (and (= (:original-sha256 binding)
                             (sha256 (git-result! opts "show" (str source-sha ":" expected-path))))
                          (not= (:original-sha256 binding) (:mutated-sha256 binding))
                          (= (:mutated-sha256 binding) (sha256 (slurp (io/file (or backend-dir ".") (:path binding))))))
                     (= {:kind "baseline"} binding)))
      (throw (ex-info "Immutable source custody changed" {:source-sha source-sha :source-binding binding :dirty-paths (vec dirty)})))
    true))

(defn payload-path!
  "Refuse symlink traversal or lexical escape before reading or copying artifacts."
  [root relative]
  (when (or (.isAbsolute (io/file relative))
            (some #{"" "." ".."} (str/split relative #"/" -1)))
    (throw (ex-info "Payload path is not backend-relative" {:path relative})))
  (let [root (.toFile (.normalize (.toPath (.getAbsoluteFile (io/file root)))))
        file (.toFile (.normalize (.toPath (.getAbsoluteFile (io/file root relative)))))]
    (when-not (.startsWith (.toPath file) (.toPath root))
      (throw (ex-info "Payload path escaped backend root" {:path relative})))
    (loop [part file]
      (when (Files/isSymbolicLink (.toPath part))
        (throw (ex-info "Payload path is a symlink" {:path relative})))
      (when-not (= root part)
        (if-let [parent (.getParentFile part)]
          (recur parent)
          (throw (ex-info "Payload path escaped backend root" {:path relative})))))
    file))

(defn regular-payload-file!
  "Reject missing and nonregular files after path custody has been established."
  [root relative]
  (let [file (payload-path! root relative)]
    (when-not (.isFile file) (throw (ex-info "Payload file missing or nonregular" {:path relative})))
    file))

(defn source-bindings-manifest!
  "Regenerate source identities for the verifier, without compiling or mutating.
  The checkout at S, not an uploaded report or manifest, supplies every descriptor."
  [{:keys [source-sha backend-dir] :or {backend-dir "."} :as opts}]
  (let [opts (assoc opts :source-binding {:kind "baseline"})
        src-dir (io/file backend-dir default-src-dir)]
    (assert-source-custody! opts)
    (let [originals (into {} (map (fn [file]
                                  (let [relative (relative-path src-dir file)
                                        path (str default-src-dir "/" relative)
                                        physical (regular-payload-file! backend-dir path)
                                        original (git-result! opts "show" (str source-sha ":backend/" path))]
                                    (when-not (= original (slurp physical))
                                      (throw (ex-info "Source bytes differ from immutable checkout" {:path path})))
                                    [relative original])) (source-files src-dir)))
          candidates (discover-candidates {:src-dir (.getPath src-dir)})
          bindings (mapv (fn [candidate]
                           {:ordinal (:ordinal candidate)
                            :source-binding (bind-mutant-source (get originals (:relative-path candidate)) candidate)}) candidates)]
      (when-not (= (count bindings) (count (set (map #(get-in % [:source-binding :fingerprint]) bindings))))
        (throw (ex-info "Canonical source inventory contains duplicate identities" {})))
      (assert-source-custody! opts)
      {:schema-version 1 :source-sha source-sha :candidates bindings})))

(defn payload-digest
  "Hash the portable sorted file inventory; maps have a separate code identity."
  [files]
  (sha256 (apply str (map (fn [{:keys [path kind bytes sha256]}]
                           (str path "\t" kind "\t" bytes "\t" sha256 "\n")) files))))

(defn payload-snapshot!
  "Read the complete nonbundled Shadow test loader and flat runtime JS/map output."
  [{:keys [backend-dir source-sha source-binding] :or {backend-dir "."}}]
  (let [loader (regular-payload-file! backend-dir payload-loader)
        runtime (io/file backend-dir payload-runtime)
        children (.listFiles runtime)
        lines (str/split-lines (slurp loader))
        import-lines (filter #(str/starts-with? (str/triml %) "SHADOW_IMPORT(") lines)
        imports (mapv second (re-seq #"(?m)^SHADOW_IMPORT\(\"([^\"\r\n]+)\"\);$" (slurp loader)))
        paths (sort (cons payload-loader (map #(str payload-runtime "/" (.getName ^File %)) children)))]
    (when-not (and (seq children) (seq imports) (= (count imports) (count (distinct imports)) (count import-lines))
                   (= [(str "var SHADOW_IMPORT_PATH = __dirname + '/../../" payload-runtime "';")]
                      (vec (filter #(str/starts-with? % "var SHADOW_IMPORT_PATH") lines)))
                   (not (some #{"goog.base.js"} imports))
                   (every? #(re-matches #"[A-Za-z0-9_$][A-Za-z0-9_.$-]*\.js" %) imports)
                   (every? #(re-matches #"[A-Za-z0-9_$][A-Za-z0-9_.$-]*\.js(?:\.map)?" (.getName ^File %)) children)
                   (= (set (map #(.getName ^File %) (filter #(str/ends-with? (.getName ^File %) ".js") children)))
                      (set (conj imports "goog.base.js")))
                   (every? #(let [name (.getName ^File %)]
                              (or (not (str/ends-with? name ".map"))
                                  (contains? (set (conj imports "goog.base.js")) (subs name 0 (- (count name) 4))))) children))
      (throw (ex-info "Unsupported or incomplete Shadow runtime payload" {})))
    (let [files (mapv (fn [path]
                        (let [bytes (Files/readAllBytes (.toPath (regular-payload-file! backend-dir path)))]
                          (when (zero? (alength bytes)) (throw (ex-info "Payload file is empty" {:path path})))
                          {:path path :kind (cond (= payload-loader path) "loader" (str/ends-with? path ".map") "source-map" :else "runtime-js")
                           :bytes (alength bytes) :sha256 (sha256-bytes bytes)})) paths)]
      {:schema-version 1 :source-sha source-sha :source-binding (or source-binding {:kind "baseline"})
       :loader-path payload-loader :runtime-root payload-runtime :imports imports :files files
       :sha256 (payload-digest files) :code-sha256 (payload-digest (remove #(= "source-map" (:kind %)) files))})))

(defn preserve-payload!
  "Store exact artifact bytes once by content hash for independent verification."
  [{:keys [backend-dir output-dir] :or {backend-dir "." output-dir default-output-dir}} payload]
  (let [store (payload-path! backend-dir (str output-dir "/payload-store"))]
    (.mkdirs store)
    (doseq [{:keys [path sha256 bytes]} (:files payload)]
      (let [blob (payload-path! backend-dir (str output-dir "/payload-store/" sha256))
            source (regular-payload-file! backend-dir path)]
        (when-not (.exists blob) (io/copy source blob))
        (regular-payload-file! (.getAbsolutePath store) sha256)
        (when-not (and (= bytes (.length blob))
                       (= sha256 (sha256-bytes (Files/readAllBytes (.toPath blob)))))
          (throw (ex-info "Copied payload blob changed" {:path path :sha256 sha256})))))
    payload))

(defn assert-payload-custody!
  "Compare actual complete runtime bytes with the compilation snapshot."
  [opts]
  (when-not (= (:payload opts) (payload-snapshot! opts))
    (throw (ex-info "Compiled runtime payload changed before or during tests" {})))
  true)

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
  [{:keys [cmd timeout-ms dir]
    :or {timeout-ms default-timeout-ms}}]
  (let [supervised (into ["python3" "scripts/run-mutation-process.py" "--timeout-ms" (str timeout-ms) "--"] cmd)
        builder (doto (ProcessBuilder. ^java.util.List supervised)
                  (.directory (io/file (or dir ".")))
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

(defn clean-compilation? [{:keys [exit-code output] :as result}]
  (and (= 0 exit-code) (completed-execution? result)
       (re-find #"\[:test-ci\] Build completed\. \([0-9]+ files, [0-9]+ compiled, 0 warnings," output)
       (not (re-find #"\b[1-9][0-9]* warnings" output))))

(defn compiled?
  "Clean compile output alone cannot establish the emitted runtime identity."
  [{:keys [artifact-path artifact-sha256 payload source-verified-before? source-verified-after?] :as result}]
  (and (clean-compilation? result) (true? source-verified-before?) (true? source-verified-after?)
       (= 1 (:schema-version payload)) (seq (:files payload))
       (= payload-loader artifact-path) (re-matches #"[0-9a-f]{64}" (or artifact-sha256 ""))
       (re-matches #"[0-9a-f]{40}" (or (:source-sha payload) ""))
       (= (:sha256 payload) (payload-digest (:files payload)))
       (= (:code-sha256 payload) (payload-digest (remove #(= "source-map" (:kind %)) (:files payload))))
       (= artifact-sha256 (:sha256 (some #(when (= payload-loader (:path %)) %) (:files payload))))))

(defn compile-mutant!
  [{:keys [shadow-build timeout-ms backend-dir] :or {shadow-build default-shadow-build timeout-ms default-timeout-ms} :as opts}]
  (when-not (= default-shadow-build shadow-build)
    (throw (ex-info "Mutation execution requires its exact non-autorun test-ci target" {:shadow-build shadow-build})))
  (assert-source-custody! opts)
  (let [result (run-process! {:cmd ["pnpm" "exec" "shadow-cljs" "--force-spawn" "compile" shadow-build]
                             :dir backend-dir :timeout-ms timeout-ms})]
    (try
      (assert-source-custody! opts)
      (if-not (clean-compilation? result)
        result
        (let [payload (preserve-payload! opts (payload-snapshot! opts))]
          (assert-payload-custody! (assoc opts :payload payload))
          (assoc result :artifact-path payload-loader
                        :artifact-sha256 (:sha256 (some #(when (= payload-loader (:path %)) %) (:files payload)))
                        :payload payload :source-verified-before? true :source-verified-after? true)))
      (catch clojure.lang.ExceptionInfo error
        (throw (ex-info (.getMessage error) (assoc (ex-data error) :compile-result result) error)))
      (catch IOException error
        (throw (ex-info (or (.getMessage error) "Custody I/O failed")
                        {:compile-result result
                         :custody-io-error {:class (.getName (class error)) :message (.getMessage error)}}
                        error))))))

(defn run-mutant-tests!
  [{:keys [timeout-ms backend-dir payload source-sha source-binding] :or {timeout-ms default-timeout-ms} :as opts}]
  (assert-source-custody! opts)
  (assert-payload-custody! opts)
  (let [result (run-process! {:cmd ["node" "scripts/run-mutation-tests.mjs"] :dir backend-dir :timeout-ms timeout-ms})]
    (try
      (assert-payload-custody! opts)
      (assert-source-custody! opts)
      (assoc result :input-payload-sha256 (:sha256 payload) :source-sha source-sha
                    :source-binding (or source-binding {:kind "baseline"})
                    :payload-verified-before? true :payload-verified-after? true
                    :source-verified-before? true :source-verified-after? true)
      (catch clojure.lang.ExceptionInfo error
        (throw (ex-info (.getMessage error) (assoc (ex-data error) :test-result result) error)))
      (catch IOException error
        (throw (ex-info (or (.getMessage error) "Custody I/O failed")
                        {:test-result result
                         :custody-io-error {:class (.getName (class error)) :message (.getMessage error)}}
                        error))))))

(defn run-compiled-tests!
  "Keep completed compile evidence when test custody yields no test result."
  [opts compile-result]
  (try
    (run-mutant-tests! (assoc opts :payload (:payload compile-result)))
    (catch clojure.lang.ExceptionInfo error
      (if (contains? (ex-data error) :compile-result)
        (throw error)
        (throw (ex-info (.getMessage error)
                        (assoc (ex-data error) :compile-result compile-result)
                        error))))
    (catch IOException error
      (throw (ex-info (or (.getMessage error) "Custody I/O failed")
                      {:compile-result compile-result
                       :custody-io-error {:class (.getName (class error)) :message (.getMessage error)}}
                      error)))))

(defn custody-failure
  "Retain a failed guard as invalid evidence, without inventing a child exit."
  [error]
  {:exit-code nil :timed-out? false :termination "custody-rejected" :cleanup-complete? true
   :capture-truncated? false :output (.getMessage ^Exception error) :custody-error (ex-data error)})

(defn execution-evidence
  "Retain captured output alongside its hash and admission fields for verification."
  [result]
  (assoc result :output-sha256 (or (:output-sha256 result) (sha256 (:output result)))
                :fatal? (str/includes? (:output result) "[shadow-test-guard] FATAL")))

(defn evaluate-mutant!
  [opts mutant]
  (let [binding (source-binding! opts mutant)
        opts (assoc opts :source-binding binding)]
    (with-mutated-source! opts mutant
      (fn []
        (try
          (let [compile-result (compile-mutant! opts)]
            (cond
              (not (compiled? compile-result))
              (assoc mutant :source-binding binding :status :invalid :phase :compile
                            :result (execution-evidence compile-result))

              (= (:baseline-code-sha256 opts) (get-in compile-result [:payload :code-sha256]))
              (assoc mutant :source-binding binding :status :invalid :phase :custody
                            :compile (execution-evidence compile-result)
                            :result (execution-evidence (custody-failure (ex-info "Mutation emitted unchanged runtime code" {}))))

              :else
              (let [test-result (run-compiled-tests! opts compile-result)]
                (assoc mutant :source-binding binding
                       :status (cond (killed? test-result) :killed
                                     (passed-tests? test-result) :survived
                                     :else :invalid)
                       :phase :test :compile (execution-evidence compile-result)
                       :result (merge (execution-evidence test-result) (or (test-counters (:output test-result)) {}))))))
          (catch clojure.lang.ExceptionInfo error
            (assoc mutant :source-binding binding :status :invalid :phase :custody
                          :result (execution-evidence (custody-failure error)))))))))

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
        report {:schema_version 3 :summary (assoc (report-summary results) :excluded (count excluded)) :mutants results
                :excluded (vec excluded) :baseline_evidence baseline-evidence
                :source_sha source-sha :batch_index batch-index :baseline baseline
                :status (if passed? :passed (if run? :failed :planned))}]
    (when-let [parent (.getParentFile path)] (.mkdirs parent))
    (spit path (with-out-str (pprint/pprint report)))
    (spit (str path ".json") (json/write-str (update report :mutants #(mapv (fn [mutant] (select-keys mutant [:id :fingerprint :source-binding :status :phase :compile :result])) %))))
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
       "  --source-bindings SHA   Read-only canonical bindings from a clean checkout at SHA; no other options\n"
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
  (try
    (let [opts (assoc opts :source-binding {:kind "baseline"})
          compile-result (compile-mutant! opts)
          test-result (when (compiled? compile-result) (run-compiled-tests! opts compile-result))]
      {:status (if (and test-result (passed-tests? test-result)) :passed :failed)
       :compile (execution-evidence compile-result)
       :test (when test-result (merge (execution-evidence test-result) (test-counters (:output test-result))))})
    (catch clojure.lang.ExceptionInfo error
      {:status :failed :compile (execution-evidence (custody-failure error)) :test nil})))

(defn run-suite! [opts]
  (when (:run? opts) (assert-clean-tree! opts))
  (when (and (:run? opts) (.exists (io/file (or (:output-dir opts) default-output-dir) "payload-store")))
    (throw (ex-info "Mutation payload evidence directory must be fresh; preserve prior runs" {})))
  (let [opts (assoc opts :source-sha (str/trim (git-result! opts "rev-parse" "HEAD")))
        candidates (discover-candidates opts)
        equivalents (load-equivalents! opts candidates)
        excluded-keys (set (map :fingerprint equivalents))
        mutants (partition-mutants candidates (assoc opts :equivalent-fingerprints excluded-keys))
        by-fingerprint (into {} (map (juxt :fingerprint identity) candidates))
        excluded (filterv (fn [entry]
                            (when-let [candidate (get by-fingerprint (:fingerprint entry))]
                              (= (:batch-index opts) (mod (:ordinal candidate) (:batch-count opts))))) equivalents)
        baseline-evidence (when (:run? opts) (baseline! opts))
        baseline (if (:run? opts) (:status baseline-evidence) :not-run)
        results (cond (not (:run? opts)) (dry-run-results mutants)
                      (= :passed baseline) (mapv #(evaluate-mutant! (assoc opts :baseline-code-sha256 (get-in baseline-evidence [:compile :payload :code-sha256])) %) mutants)
                      :else [])
        source-sha (:source-sha opts)
        report (write-report! (assoc opts :baseline baseline :baseline-evidence baseline-evidence
                                    :excluded excluded :source-sha source-sha) results)]
    (print-summary! report)
    (when (and (:run? opts) (not= :passed (:status report)))
      (throw (ex-info "Mutation qualification failed: require a green baseline and enough completed test kills" {:summary (:summary report)})))
    report))

(defn -main
  [& args]
  (try
    (if (= "--source-bindings" (first args))
      (do
        (when-not (= 2 (count args))
          (throw (ex-info "Source binding command requires only one immutable SHA" {})))
        (println (json/write-str (source-bindings-manifest! {:source-sha (second args)}))))
      (let [opts (parse-args args)]
        (if (:help? opts)
          (println usage)
          (run-suite! opts))))
    (finally
      ;; Only the terminal CLI owns the process-wide agent executors.
      (try
        (flush)
        (finally (shutdown-agents))))))
