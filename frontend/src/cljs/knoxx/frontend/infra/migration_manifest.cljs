(ns knoxx.frontend.infra.migration-manifest
  "Node filesystem and Git adapters for the generated migration ledger."
  (:require ["node:child_process" :as child-process]
            ["node:fs" :as fs]
            ["node:path" :as node-path]
            [cljs.tools.reader.edn :as edn]
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
  (walk-files-under (fs/realpathSync root) root))

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

(defn- export-symbols [source]
  (let [pattern (js/RegExp. "export\\s*\\{([\\s\\S]*?)\\}\\s*from\\s*[\\\"']([^\\\"']+)[\\\"'];?" "g")]
    (loop [exports []]
      (if-let [match (.exec pattern source)]
        (let [body (-> (aget match 1)
                       (str/replace #"//[^\n]*" ""))
              source-path (aget match 2)
              symbols (->> (str/split body #",")
                           (map str/trim)
                           (remove str/blank?)
                           (map #(last (str/split % #"\s+as\s+"))))]
          (recur (into exports (map (fn [export-name]
                                      {:symbol export-name :source source-path})
                                    symbols))))
        exports))))

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
                   (when-not (= (count (re-seq #"(?m)^export\s+" source))
                                (count (re-seq #"(?m)^export\s*\{" source)))
                     (throw (ex-info "Unsupported bridge export syntax"
                                     {:path path})))
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
                        (export-symbols source)))))
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

(defn- route-implementation [block]
  (or (second (re-find #"\(\$\s+(app/[A-Za-z0-9_-]+)" block))
      (second (re-find #"\(\$\s+([A-Za-z0-9_-]+/[A-Za-z0-9_-]+)" block))
      (second (re-find #"\(\$\s+(LegacyOpsRedirect|Navigate|PlaceholderPage)" block))
      "inline"))

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

(defn- route-records [root]
  (let [path "frontend/src/cljs/knoxx/frontend/app.cljs"
        source (fs/readFileSync (node-path/join root path) "utf8")
        bridge-alias (app-bridge-alias source)
        pattern (js/RegExp. "\\(\\$ Route \\{:path\\s+([^\\n]+)" "g")
        route-count (count (re-seq #"\(\$ Route \{:path" source))]
    (loop [matches []]
      (if-let [match (.exec pattern source)]
        (recur (conj matches {:index (.-index match)
                              :route (str/trim (aget match 1))}))
        (do
          (when-not (= route-count (count matches))
            (throw (ex-info "Unsupported Shadow route syntax"
                            {:path path
                             :route-forms route-count
                             :parsed-routes (count matches)})))
          (mapv (fn [position next-position]
                  (let [block (subs source (:index position)
                                    (or (:index next-position) (count source)))
                        implementation (route-implementation block)]
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
