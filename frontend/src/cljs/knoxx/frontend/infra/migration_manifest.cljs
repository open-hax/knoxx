(ns knoxx.frontend.infra.migration-manifest
  "Node filesystem and Git adapters for the generated migration ledger."
  (:require ["node:fs" :as fs]
            ["node:path" :as node-path]
            ["typescript" :as ts]
            [cljs.tools.reader :as reader]
            [cljs.tools.reader.edn :as edn]
            [cljs.tools.reader.reader-types :as reader-types]
            [clojure.string :as str]
            [knoxx.frontend.domain.migration :as domain]
            [knoxx.frontend.infra.migration-build :as build]
            [knoxx.frontend.infra.migration-git :as git]
            [knoxx.frontend.infra.migration-imports :as imports]
            [knoxx.frontend.law.migration :as law]
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

(defn- legacy-sources [root resolution]
  (let [source-root (node-path/join root "frontend" "src")]
    (->> (walk-files source-root)
         (map law/assert-governed-extension!)
         (map (fn [absolute-path]
                {:path (repository-path root absolute-path)
                 :absolute-path absolute-path}))
         (filter (comp #(re-find law/legacy-source-pattern %) :path))
         (mapv (fn [{:keys [path absolute-path]}]
                 (let [source (fs/readFileSync absolute-path "utf8")]
                   (imports/assert-contained! resolution path source)
                   {:path path :source source}))))))

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

(defn- resolve-local-export [resolution bridge-path source]
  (some-> (imports/resolve-target resolution bridge-path source)
          shape/normalize-path))

(defn- bridge-records [root resolution]
  (->> [{:bridge :frontend :path "frontend/src/bridge/index.ts"}
        {:bridge :app :path "frontend/src/bridge/app.ts"}]
       (filter #(fs/existsSync (node-path/join root (:path %))))
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
                                   (resolve-local-export resolution absolute-path source-path))))
                        (export-symbols path source)))))
       vec))

(defn- strip-extension [path]
  (str/replace path #"\.(?:tsx?|mts|cts)$" ""))

(defn- direct-bridge-index [root records]
  (->> records
       (keep (fn [{:keys [bridge resolved-source]}]
               (when resolved-source
                 [(strip-extension (repository-path root resolved-source)) bridge])))
       (into {})))

(defn- attach-direct-bridges [sources bridge-index]
  (mapv (fn [{:keys [path] :as source}]
          (if-let [bridge (get bridge-index (strip-extension path))]
            (assoc source :bridge bridge)
            source))
        sources))

;; Explicit non-evaluated bodies cannot establish routes or implementation ownership.
(defn- route-inspection-nodes [form]
  (tree-seq (fn [node]
              (and (coll? node)
                   (not (and (seq? node)
                             (contains? '#{comment quote
                                           cljs.core/comment cljs.core/quote
                                           clojure.core/comment clojure.core/quote}
                                        (first node))))))
            seq
            form))

(defn- parse-route-form [block]
  (try
    (edn/read-string block)
    (catch :default _
      (throw (ex-info "Unsupported Shadow route implementation" {})))))

(defn- route-implementation [bridge-alias route-form]
  (let [components (->> (route-inspection-nodes route-form)
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
    (or (when bridge-alias
          (some #(when (= bridge-alias (namespace %)) (str %)) symbols))
        (some #(when (namespace %) (str %)) symbols)
        (some #(when (contains? #{"LegacyOpsRedirect" "Navigate" "PlaceholderPage"} (str %))
                 (str %)) symbols)
        (throw (ex-info "Unsupported Shadow route implementation" {})))))

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

(defn app-bridge-alias
  "Read a declared bridge alias, or nil after its namespace require is retired."
  [source]
  (let [namespaces (->> (source-forms "frontend/src/cljs/knoxx/frontend/app.cljs" source)
                        (filter #(and (seq? %) (= 'ns (first %)))))
        specifications (->> (drop 2 (first namespaces))
                             (filter #(and (seq? %) (= :require (first %))))
                             (mapcat rest)
                             (filter #(= "@open-hax/knoxx-app-bridge"
                                         (if (sequential? %) (first %) %))))]
    (when-not (and (= 1 (count namespaces)) (<= (count specifications) 1))
      (throw (ex-info "Expected exactly one application bridge alias" {})))
    (when-let [specification (first specifications)]
      (when-not (vector? specification)
        (throw (ex-info "Expected exactly one application bridge alias" {})))
      (let [options (rest specification)
            pairs (partition 2 options)
            aliases (map second (filter #(= :as (first %)) pairs))
            alias (first aliases)]
        (when-not (and (even? (count options))
                       (every? (comp keyword? first) pairs)
                       (= 1 (count aliases))
                       (symbol? alias)
                       (nil? (namespace alias)))
          (throw (ex-info "Expected exactly one application bridge alias"
                          {:aliases (vec aliases)})))
        (str alias)))))

(defn- route-identity [path source]
  (let [route (str/trim source)]
    (when-not (= route (pr-str (edn/read-string route)))
      (throw (ex-info "Unsupported Shadow route syntax"
                      {:path path :route route})))
    route))

;; Census Helix and direct React route creation independently of supported grammar.
;; Shared :id/:children props do not identify routes; route markers identify aliases.
(defn- route-forms [path source]
  (->> (source-forms path source)
       route-inspection-nodes
       (filter (fn [form]
                 (and (seq? form)
                      (symbol? (first form))
                      (contains? #{"$" "createElement"} (name (first form)))
                      (let [component (second form)
                            props (nth form 2 nil)]
                        (or (and (symbol? component)
                                 (= "Route" (name component)))
                            (and (map? props)
                                 (some #(contains? props %)
                                       [:path :element :index :Component])))))))))

(defn- checked-route-forms [path source]
  (let [forms (vec (route-forms path source))]
    (doseq [form forms]
      (when-not (and (= '$ (first form)) (= 'Route (second form)))
        (throw (ex-info "Unsupported Shadow route syntax"
                        {:path path :constructor (second form)}))))
    forms))

(defn- assert-route-locations! [root app-path]
  (doseq [source-path ["frontend/src" "shared/src/cljs"]
          :let [source-root (node-path/join root source-path)]
          :when (fs/existsSync source-root)
          absolute-path (walk-files source-root)
          :when (re-find #"\.clj[sc]$" absolute-path)
          :let [path (repository-path root absolute-path)]
          :when (not= app-path path)]
    (when (seq (route-forms path (fs/readFileSync absolute-path "utf8")))
      (throw (ex-info "Unsupported Shadow route location"
                      {:path path :supported-path app-path})))))

(defn- extract-route-forms [source matches]
  (mapv (fn [position next-position]
          (parse-route-form
            (subs source (:index position)
                  (or (:index next-position) (count source)))))
        matches (concat (rest matches) [nil])))

(defn- route-records [root]
  (let [path "frontend/src/cljs/knoxx/frontend/app.cljs"
        source (fs/readFileSync (node-path/join root path) "utf8")
        bridge-alias (app-bridge-alias source)
        pattern (js/RegExp. "\\(\\$ Route \\{:path\\s+([^\\n]+)" "g")
        route-count (count (re-seq #"\(\s*\$\s+Route(?=\s|\))" source))
        declared-route-forms (checked-route-forms path source)]
    (assert-route-locations! root path)
    (loop [matches []]
      (if-let [match (.exec pattern source)]
        (recur (conj matches {:index (.-index match)
                              :route (route-identity path (aget match 1))}))
        (let [extracted-route-forms (extract-route-forms source matches)]
          (when-not (and (= route-count (count declared-route-forms) (count matches))
                         (= declared-route-forms extracted-route-forms))
            (throw (ex-info "Unsupported Shadow route syntax"
                            {:path path
                             :route-forms route-count
                             :declared-routes (count declared-route-forms)
                             :parsed-routes (count matches)})))
          (mapv (fn [position route-form]
                  {:path path
                   :route (:route position)
                   :implementation (route-implementation bridge-alias route-form)
                   :bridge-alias bridge-alias})
                matches
                extracted-route-forms))))))

(defn current-records
  "Read the repository and return the canonical generated records."
  []
  (let [root (repository-root)
        resolution (imports/resolver root (build/assert-configs! root))
        bridge-records* (bridge-records root resolution)
        bridge-index (direct-bridge-index root bridge-records*)
        sources (attach-direct-bridges (legacy-sources root resolution) bridge-index)
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

(defn base-manifest
  "Read and validate a baseline; only an absent ledger or omitted revision returns nil."
  [sha]
  (when-let [text (git/baseline-text (repository-root) manifest-relative-path sha)]
    (-> text parse-records law/assert-manifest!)))

(defn changed-paths
  "Return repository paths changed between a Git revision and HEAD."
  [sha]
  (git/changed-paths (repository-root) sha))
