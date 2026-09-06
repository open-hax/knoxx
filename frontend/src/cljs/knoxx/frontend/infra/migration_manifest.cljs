(ns knoxx.frontend.infra.migration-manifest
  "Node filesystem and Git adapters for the generated migration ledger."
  (:require ["node:child_process" :as child-process]
            ["node:fs" :as fs]
            ["node:path" :as node-path]
            ["typescript" :as ts]
            [cljs.tools.reader :as reader]
            [cljs.tools.reader.edn :as edn]
            [cljs.tools.reader.reader-types :as reader-types]
            [clojure.string :as str]
            [knoxx.frontend.domain.migration :as domain]
            [knoxx.frontend.shape.migration :as shape]))

(def manifest-relative-path
  "Repository-relative path to the generated migration ledger."
  "frontend/migration/manifest.ndedn")

(defn- path-inside-root?
  "Whether target resolves within the canonical root directory."
  [root target]
  (let [relative (node-path/relative root target)]
    (and (not (node-path/isAbsolute relative))
         (not= relative "..")
         (not (str/starts-with? relative (str ".." node-path/sep))))))

(defn- safe-file-symlink?
  "Whether path resolves to a regular file contained by root."
  [root path]
  (try
    (let [target (fs/realpathSync path)]
      (and (path-inside-root? root target)
           (.isFile (fs/statSync target))))
    (catch :default _ false)))

(defn- walk-files-under
  "Return file paths under directory without leaving the canonical root."
  [root directory]
  (->> (fs/readdirSync directory)
       (mapcat (fn [entry-name]
                 (let [path (node-path/join directory entry-name)
                       stat (fs/lstatSync path)]
                   (cond
                     (.isSymbolicLink stat)
                     (if (safe-file-symlink? root path)
                       [path]
                       (throw (ex-info "Unsafe symbolic link in migration source tree"
                                       {:path path})))

                     (.isDirectory stat) (walk-files-under root path)
                     :else [path]))))
       sort))

(defn walk-files
  "Return sorted absolute file paths without following unsafe symbolic links."
  [root]
  (let [stat (fs/lstatSync root)]
    (when (or (.isSymbolicLink stat) (not (.isDirectory stat)))
      (throw (ex-info "Unsafe migration source root" {:path root}))))
  (let [canonical-root (fs/realpathSync root)]
    (walk-files-under canonical-root canonical-root)))

(defn- repository-root []
  (let [cwd (.cwd js/process)]
    (if (fs/existsSync (node-path/join cwd "frontend" "package.json"))
      cwd
      (node-path/resolve cwd ".."))))

(defn- repository-path [root absolute-path]
  (shape/normalize-path (node-path/relative root absolute-path)))

(defn- legacy-sources [root]
  (let [source-root (node-path/join root "frontend" "src")]
    (->> (walk-files source-root)
         (map (fn [absolute-path]
                {:path (repository-path root absolute-path)
                 :absolute-path absolute-path}))
         (filter (comp #(re-find shape/legacy-source-pattern %) :path))
         (mapv (fn [{:keys [path absolute-path]}]
                 {:path path
                  :source (fs/readFileSync absolute-path "utf8")})))))

(defn- export-statement? [^js statement]
  (or (ts/isExportDeclaration statement)
      (ts/isExportAssignment statement)
      (some #(= (.-kind %) (.-ExportKeyword ts/SyntaxKind))
            (some-> statement .-modifiers array-seq))))

(defn- export-symbols [path source]
  (let [^js module (ts/createSourceFile path source (.-Latest ts/ScriptTarget))]
    (when (seq (array-seq (.-parseDiagnostics module)))
      (throw (ex-info "Unsupported bridge export syntax" {:path path})))
    (->> (array-seq (.-statements module))
         (filter export-statement?)
         (mapcat (fn [^js statement]
                   (let [^js clause (.-exportClause statement)
                         ^js module-specifier (.-moduleSpecifier statement)]
                     (when-not (and (ts/isExportDeclaration statement)
                                    (not (.-isTypeOnly statement))
                                    clause
                                    (ts/isNamedExports clause)
                                    module-specifier
                                    (ts/isStringLiteral module-specifier))
                       (throw (ex-info "Unsupported bridge export syntax" {:path path})))
                     (mapv (fn [^js specifier]
                             (when (.-isTypeOnly specifier)
                               (throw (ex-info "Unsupported bridge export syntax" {:path path})))
                             (let [^js exported-name (.-name specifier)]
                               {:symbol (.-text exported-name)
                                :source (.-text module-specifier)}))
                           (array-seq (.-elements clause))))))
         vec)))

(defn- resolve-local-export [bridge-path source]
  (when (str/starts-with? source ".")
    (-> (node-path/resolve (node-path/dirname bridge-path) source)
        shape/normalize-path)))

(defn- bridge-records [root]
  (->> [{:bridge :frontend :path "frontend/src/bridge/index.ts"}
        {:bridge :app :path "frontend/src/bridge/app.ts"}]
       (mapcat (fn [{:keys [bridge path]}]
                 (let [absolute-path (node-path/join root path)
                       source (fs/readFileSync absolute-path "utf8")]
                   (map (fn [export-entry]
                          (let [source-path (:source export-entry)
                                export-name (:symbol export-entry)]
                            (assoc (shape/bridge-export-record
                                    {:bridge bridge
                                     :path path
                                     :source source-path
                                     :symbol export-name})
                                   :resolved-source
                                   (resolve-local-export absolute-path source-path))))
                        (export-symbols path source)))))
       vec))

(defn- direct-bridge-index [root records]
  (->> records
       (keep (fn [{:keys [bridge resolved-source]}]
               (when resolved-source
                 [(repository-path root resolved-source) bridge])))
       (into {})))

(defn- strip-extension [path]
  (str/replace path #"\.tsx?$" ""))

(defn- attach-direct-bridges [sources bridge-index]
  (mapv (fn [{:keys [path] :as source}]
          (if-let [bridge (get bridge-index (strip-extension path))]
            (assoc source :bridge bridge)
            source))
        sources))

(defn- route-implementation [bridge-alias block]
  (let [route-form (try
                     (edn/read-string block)
                     (catch :default _
                       (throw (ex-info "Unsupported Shadow route implementation" {}))))
        components (->> (tree-seq coll? seq route-form)
                        (filter #(and (seq? %) (= '$ (first %))))
                        (map second))
        unknown (remove #(or (keyword? %)
                             (and (symbol? %)
                                  (or (namespace %)
                                      (contains? #{"Route" "ProtectedSurface" "LegacyOpsRedirect"
                                                   "Navigate" "PlaceholderPage"} (name %)))))
                        components)
        symbols (filter symbol? components)]
    (when (seq unknown)
      (throw (ex-info "Unsupported Shadow route implementation"
                      {:components (vec unknown)})))
    (or (some #(when (= bridge-alias (namespace %)) (str %)) symbols)
        (some #(when (namespace %) (str %)) symbols)
        (some #(when (contains? #{"LegacyOpsRedirect" "Navigate" "PlaceholderPage"} (str %))
                 (str %)) symbols)
        (throw (ex-info "Unsupported Shadow route implementation" {})))))

(defn app-bridge-alias
  "Read the local alias bound to the application compatibility bridge."
  [source]
  (let [aliases (map second
                     (re-seq #"\[\"@open-hax/knoxx-app-bridge\"\s+:as\s+([A-Za-z0-9_-]+)"
                             source))]
    (when-not (= 1 (count aliases))
      (throw (ex-info "Expected exactly one application bridge alias"
                      {:aliases (vec aliases)})))
    (first aliases)))

(defn bridge-owned-implementation?
  "Whether a parsed route implementation is owned by the bridge alias."
  [bridge-alias implementation]
  (str/starts-with? implementation (str bridge-alias "/")))

(defn- route-identity [path source]
  (let [route (str/trim source)]
    (when-not (= route (pr-str (edn/read-string route)))
      (throw (ex-info "Unsupported Shadow route syntax"
                      {:path path :route route})))
    route))

(defn- source-forms [path source]
  (let [input (reader-types/string-push-back-reader source)
        eof (js/Object.)]
    (try
      (binding [reader/*data-readers* {'js identity}]
        (loop [forms []]
          (let [form (reader/read {:eof eof :read-cond :allow :features #{:cljs}} input)]
            (if (identical? eof form)
              forms
              (recur (conj forms form))))))
      (catch :default _
        (throw (ex-info "Unsupported Shadow route syntax" {:path path}))))))

;; Census literal route props independently of constructor spelling so the
;; narrow extractor rejects unsupported forms instead of silently omitting them.
(defn- route-form-count [path source]
  (let [route-forms (->> (source-forms path source)
                         (tree-seq coll? seq)
                         (filter (fn [form]
                                   (and (seq? form)
                                        (symbol? (first form))
                                        (= "$" (name (first form)))
                                        (let [props (nth form 2 nil)]
                                          (and (map? props)
                                               (or (contains? props :path)
                                                   (contains? props :element))))))))]
    (doseq [form route-forms]
      (when-not (and (= '$ (first form)) (= 'Route (second form)))
        (throw (ex-info "Unsupported Shadow route syntax"
                        {:path path :constructor (second form)}))))
    (count route-forms)))

(defn- route-records [root]
  (let [path "frontend/src/cljs/knoxx/frontend/app.cljs"
        source (fs/readFileSync (node-path/join root path) "utf8")
        bridge-alias (app-bridge-alias source)
        pattern (js/RegExp. "\\(\\$ Route \\{:path\\s+([^\\n]+)" "g")
        route-count (count (re-seq #"\(\s*\$\s+Route(?=\s|\))" source))
        declared-route-count (route-form-count path source)]
    (loop [matches []]
      (if-let [match (.exec pattern source)]
        (recur (conj matches {:index (.-index match)
                              :route (route-identity path (aget match 1))}))
        (do
          (when-not (= route-count declared-route-count (count matches))
            (throw (ex-info "Unsupported Shadow route syntax"
                            {:path path
                             :route-forms route-count
                             :declared-routes declared-route-count
                             :parsed-routes (count matches)})))
          (mapv (fn [position next-position]
                  (let [block (subs source (:index position)
                                    (or (:index next-position) (count source)))
                        implementation (route-implementation bridge-alias block)]
                    (shape/route-record {:path path
                                         :route (:route position)
                                         :implementation implementation
                                         :legacy? (bridge-owned-implementation?
                                                   bridge-alias implementation)})))
                matches
                (concat (rest matches) [nil])))))))

(defn current-records
  "Read the repository and return the canonical generated records."
  []
  (let [root (repository-root)
        bridge-records* (bridge-records root)
        bridge-index (direct-bridge-index root bridge-records*)
        sources (attach-direct-bridges (legacy-sources root) bridge-index)
        exports (mapv #(dissoc % :resolved-source) bridge-records*)]
    (domain/assemble-records {:sources sources
                              :bridge-exports exports
                              :routes (route-records root)})))

(defn render-records
  "Render one EDN map per line with a terminal newline."
  [records]
  (str (str/join "\n" (map pr-str records)) "\n"))

(defn parse-records
  "Read canonical newline-delimited EDN; reject trailing or rewritten forms."
  [text]
  (->> (str/split-lines text)
       (remove str/blank?)
       (mapv (fn [line]
               (let [record (edn/read-string line)]
                 (when-not (= line (pr-str record))
                   (throw (ex-info "Manifest line is not canonical single-form EDN"
                                   {:line line})))
                 record)))))

(defn read-manifest
  "Read the checked-in migration ledger as text."
  []
  (let [root (repository-root)]
    (fs/readFileSync (node-path/join root manifest-relative-path) "utf8")))

(defn write-manifest!
  "Replace the checked-in migration ledger with canonical text."
  [text]
  (let [root (repository-root)
        path (node-path/join root manifest-relative-path)]
    (fs/mkdirSync (node-path/dirname path) #js {:recursive true})
    (fs/writeFileSync path text "utf8")))

(defn- git-commit-exists?
  "Whether Git can resolve a revision to a commit in the local checkout."
  [sha]
  (try
    (child-process/execFileSync
     "git" #js ["cat-file" "-e" (str sha "^{commit}")]
     #js {:cwd (repository-root)
          :stdio #js ["ignore" "ignore" "ignore"]})
    true
    (catch :default _ false)))

(defn base-manifest
  "Read and parse the migration ledger at a Git revision, or return nil."
  [sha]
  (when (seq sha)
    (when-not (git-commit-exists? sha)
      (throw (ex-info "Git cannot resolve the migration baseline revision"
                      {:base-sha sha})))
    (try
      (-> (child-process/execFileSync
           "git" #js ["show" (str sha ":" manifest-relative-path)]
           #js {:cwd (repository-root)
                :encoding "utf8"
                :stdio #js ["ignore" "pipe" "pipe"]})
          parse-records)
      (catch :default _ nil))))

(defn changed-paths
  "Return repository paths changed between a Git revision and HEAD."
  [sha]
  (if (seq sha)
    (-> (child-process/execFileSync
         "git" #js ["diff" "--name-only" (str sha "...HEAD")]
         #js {:cwd (repository-root) :encoding "utf8"})
        str/split-lines
        (->> (remove str/blank?) vec))
    []))
