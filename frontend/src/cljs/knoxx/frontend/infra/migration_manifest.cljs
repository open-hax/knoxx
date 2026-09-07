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
            [knoxx.frontend.infra.migration-export-source :as export-source]
            [knoxx.frontend.infra.migration-git :as git]
            [knoxx.frontend.infra.migration-html :as html]
            [knoxx.frontend.infra.migration-imports :as imports]
            [knoxx.frontend.infra.migration-project-source :as project-source]
            [knoxx.frontend.infra.migration-router-source :as router]
            [knoxx.frontend.infra.migration-vitest :as vitest]
            [knoxx.frontend.law.migration :as law]
            [knoxx.frontend.shape.migration :as shape]))

(def manifest-relative-path
  "Repository-relative path to the generated migration ledger."
  "frontend/migration/manifest.ndedn")

(defn- relative-path-facts
  "Read platform-relative path facts for the source boundary contract."
  [root target]
  (let [relative (node-path/relative root target)]
    {:relative-path relative
     :absolute? (node-path/isAbsolute relative)
     :separator node-path/sep}))

(defn- symlink-facts
  "Read a symbolic link's target facts without admitting it into the inventory."
  [root path]
  (try
    (let [target (fs/realpathSync path)]
      {:path path :target (relative-path-facts root target)
       :file? (.isFile (fs/statSync target))})
    (catch :default _ {:path path :file? false})))

(defn- walk-files-under
  "Return file paths under directory without leaving the canonical root."
  [root directory]
  (->> (fs/readdirSync directory)
       (mapcat (fn [entry-name]
                 (let [path (node-path/join directory entry-name)
                       stat (fs/lstatSync path)]
                   (cond
                     (.isSymbolicLink stat)
                     (do (law/assert-source-symlink! (symlink-facts root path))
                         [path])

                     (.isDirectory stat) (walk-files-under root path)
                     :else [path]))))
       sort))

(defn walk-files
  "Return sorted absolute file paths without following unsafe symbolic links."
  [root]
  (let [stat (fs/lstatSync root)]
    (law/assert-source-root! {:path root
                              :symbolic-link? (.isSymbolicLink stat)
                              :directory? (.isDirectory stat)}))
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
(defn- non-evaluated-form? [node]
  (and (seq? node)
       (contains? '#{comment quote cljs.core/comment cljs.core/quote
                     clojure.core/comment clojure.core/quote}
                  (first node))))

(defn- canonical-route-binding? [form]
  (and (seq? form) (= 3 (count form))
       (= 'def (first form)) (= 'Route (second form))
       (let [initializer (nth form 2)]
         (and (seq? initializer) (= 2 (count initializer))
              (= '.-Route (first initializer)) (symbol? (second initializer))))))

(defn- route-inspection-nodes [form]
  (tree-seq #(and (coll? %) (not (non-evaluated-form? %))
                  (not (canonical-route-binding? %))) seq form))

(defn- parse-route-form [block]
  (try
    (edn/read-string block)
    (catch :default _
      (throw (ex-info "Unsupported Shadow route implementation" {})))))

(defn- route-implementation [ownership-facts route-form]
  (let [nodes (route-inspection-nodes route-form)
        components (router/element-components nodes)
        unknown (remove #(or (keyword? %)
                             (and (symbol? %)
                                  (or (namespace %)
                                      (contains? #{"Route" "ProtectedSurface" "LegacyOpsRedirect"
                                                   "Navigate" "PlaceholderPage"} (name %)))))
                        components)]
    (when (seq unknown)
      (throw (ex-info "Unsupported Shadow route implementation"
                      {:components (vec unknown)})))
    (or (domain/route-implementation
          (assoc ownership-facts :live-references (router/symbol-references nodes)
                 :lexically-bound-components (get (:lexical-route-components ownership-facts) route-form)
                 :rendered-components (router/symbol-references components)))
        (throw (ex-info "Unsupported Shadow route implementation" {})))))

(defn- source-forms [path source & [features]]
  (let [input (reader-types/string-push-back-reader source)
        eof (js/Object.)]
    (try
      (binding [reader/*data-readers* {'js identity}]
        (loop [forms []]
          (let [form (reader/read {:eof eof :read-cond :allow :features (or features #{:cljs})} input)]
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

;; Route values may not escape through aliases in definitions, bindings or calls.
;; Only the canonical Route binding is exempt; alias dataflow is not interpreted.
(defn- route-candidate? [router-aliases router-apis form]
  (and (coll? form)
       (not (non-evaluated-form? form))
       (not (canonical-route-binding? form))
       (or (some router/api-reference? form)
           (some router-apis form)
           (router/computed-access? router-aliases form)
           (router/unresolved-module-value? router-aliases form)
           (and (symbol? (first form))
                (contains? #{"$" "createElement"} (name (first form)))
                (let [props (nth form 2 nil)]
                  (and (map? props)
                       (some #(contains? props %) [:path :element :index :Component])))))))

;; Shared :id/:children props do not identify routes; route markers identify aliases.
(defn- route-forms [path source]
  (let [forms (source-forms path source)
        router-aliases (router/aliases forms)
        router-apis (router/api-bindings forms)]
    (->> forms
         (remove #(and (seq? %) (= 'ns (first %))))
         route-inspection-nodes
         (filter (partial route-candidate? router-aliases router-apis)))))

(defn- checked-route-forms [path source]
  (let [forms (vec (route-forms path source))]
    (doseq [form forms]
      (when-not (and (= '$ (first form)) (= 'Route (second form)))
        (throw (ex-info "Unsupported Shadow route syntax"
                        {:path path :constructor (second form)}))))
    forms))

(defn- extract-route-forms [source matches]
  (mapv (fn [position next-position]
          (parse-route-form
            (subs source (:index position)
                  (or (:index next-position) (count source)))))
        matches (concat (rest matches) [nil])))

(defn- route-ownership-facts [root path source route-exports]
  (let [forms (source-forms path source)]
    {:bridge-alias (app-bridge-alias source)
     :local-definitions (router/definition-references (route-inspection-nodes forms) route-inspection-nodes)
     :lexical-route-components (router/lexical-route-components forms non-evaluated-form?)
     :source-namespace (:namespace (router/namespace-facts forms route-inspection-nodes))
     :project-namespaces (project-source/read-namespaces
                           {:root root :app-path path :walk-files walk-files :read-forms source-forms
                            :route-forms route-forms :inspect-nodes route-inspection-nodes})
     :legacy-route-exports route-exports}))

(defn- route-records [root route-exports]
  (let [path "frontend/src/cljs/knoxx/frontend/app.cljs"
        source (fs/readFileSync (node-path/join root path) "utf8")
        ownership-facts (route-ownership-facts root path source route-exports)
        pattern (js/RegExp. "\\(\\$ Route \\{:path\\s+([^\\n]+)" "g")
        route-count (count (re-seq #"\(\s*\$\s+Route(?=\s|\))" source))
        declared-route-forms (checked-route-forms path source)]
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
                   :implementation (route-implementation ownership-facts route-form)
                   :bridge-alias (:bridge-alias ownership-facts)})
                matches
                extracted-route-forms))))))

(defn current-records
  "Read the repository and return the canonical generated records."
  []
  (let [root (repository-root)
        _ (html/assert-entrypoints! root)
        _ (vitest/assert-config! root)
        resolution (imports/resolver root (build/assert-configs! root))
        bridge-records* (export-source/with-provenance root resolution (bridge-records root resolution))
        bridge-index (direct-bridge-index root bridge-records*)
        sources (attach-direct-bridges (legacy-sources root resolution) bridge-index)
        exports (mapv shape/bridge-export-record bridge-records*)]
    (domain/assemble-records {:sources sources
                              :bridge-exports exports
                              :routes (route-records root (domain/legacy-route-exports bridge-records*))})))

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
  (let [records (-> text parse-records law/assert-manifest!)
        canonical (->> records (sort-by :record/id) render-records)
        root (repository-root)
        path (node-path/join root manifest-relative-path)]
    (when-not (= text canonical)
      (throw (ex-info "Manifest text is not canonical" {})))
    (fs/mkdirSync (node-path/dirname path) #js {:recursive true})
    (fs/writeFileSync path canonical "utf8")))

(defn base-manifest
  "Read and validate a baseline; only an absent ledger or omitted revision returns nil."
  [sha]
  (when-let [text (git/baseline-text (repository-root) manifest-relative-path sha)]
    (-> text parse-records law/assert-manifest!)))

(defn changed-paths
  "Return repository paths changed between a Git revision and HEAD."
  [sha]
  (git/changed-paths (repository-root) sha))

(defn native-source-counts
  "Read base and live frontend CLJS counts through the existing safe file walker."
  [sha]
  (let [root (repository-root)]
    {:before (git/native-source-count root sha)
     :after (->> (walk-files (node-path/join root "frontend" "src"))
                 (filter #(str/ends-with? % ".cljs"))
                 count)}))
