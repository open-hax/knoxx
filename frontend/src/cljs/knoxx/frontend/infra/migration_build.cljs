(ns knoxx.frontend.infra.migration-build
  "Static Vite configuration inspection for the governed frontend inventory."
  (:require ["node:fs" :as fs]
            ["node:path" :as node-path]
            ["typescript" :as ts]
            [cljs.tools.reader.edn :as edn]
            [clojure.string :as str]
            [knoxx.frontend.infra.migration-config-source :as config-source]
            [knoxx.frontend.law.migration :as law]
            [knoxx.frontend.law.migration-shadow :as shadow-law]))

(defn- unsupported! [file detail]
  (throw (ex-info "Unsupported Vite migration configuration"
                  {:path file :detail detail})))

(defn- property-name [file ^js node]
  (if (or (ts/isIdentifier node) (ts/isStringLiteral node))
    (.-text node)
    (unsupported! file "Computed property names are not supported")))

(defn- object-properties [file ^js node]
  (when-not (and node (ts/isObjectLiteralExpression node))
    (unsupported! file "Expected a static object literal"))
  (reduce (fn [properties ^js property]
            (when-not (ts/isPropertyAssignment property)
              (unsupported! file "Object spreads, methods and shorthand are not supported"))
            (let [property-key (property-name file (.-name property))]
              (when (contains? properties property-key)
                (unsupported! file (str "Duplicate property: " property-key)))
              (assoc properties property-key (.-initializer property))))
          {} (array-seq (.-properties node))))

(defn- imported-names [^js source]
  (reduce
    (fn [bindings ^js statement]
      (if (ts/isImportDeclaration statement)
        (let [module (.-text ^js (.-moduleSpecifier statement))
              ^js clause (.-importClause statement)
              ^js named (when clause (.-namedBindings clause))]
          (cond
            (and (= module "vite") named (ts/isNamedImports named))
            (reduce (fn [result ^js imported]
                      (let [original (or (.-propertyName imported) (.-name imported))]
                        (if (= "defineConfig" (.-text ^js original))
                          (update result :define-config conj (.-text ^js (.-name imported)))
                          result)))
                    bindings (array-seq (.-elements named)))
            (and (contains? #{"path" "node:path"} module) clause)
            (update bindings :path into
                    (keep #(some-> ^js % .-text)
                          [(.-name clause) (when named (.-name named))]))
            (and clause (.-name clause))
            (assoc-in bindings [:plugin-factories (.-text ^js (.-name clause))] module)
            :else bindings))
        bindings))
    {:define-config #{} :path #{} :plugin-factories {}} (array-seq (.-statements source))))

(defn- plugin-facts [bindings ^js node]
  (when (ts/isCallExpression node)
    (let [^js callee (.-expression node)]
      {:factory-module (when (ts/isIdentifier callee)
                         (get-in bindings [:plugin-factories (.-text callee)]))
       :argument-count (count (array-seq (.-arguments node)))})))

(defn- plugin-list-facts [bindings placement ^js node]
  (let [array-node? (and node (ts/isArrayLiteralExpression node))]
    {:placement placement :static? (or (nil? node) array-node?)
     :plugins (if array-node? (mapv #(plugin-facts bindings %) (array-seq (.-elements node))) [])}))

(defn- scalar-value [^js node]
  (cond
    (ts/isStringLiteral node) (.-text node)
    (= (.-TrueKeyword ts/SyntaxKind) (.-kind node)) true
    (= (.-FalseKeyword ts/SyntaxKind) (.-kind node)) false
    :else :dynamic))

(defn- option-values [properties]
  (reduce-kv (fn [result field node] (assoc result field (scalar-value node))) {} properties))

(defn- output-values [file properties]
  (let [^js globals (get properties "globals")]
    (cond-> (option-values properties)
      (and globals (ts/isObjectLiteralExpression globals))
      (assoc "globals" (option-values (object-properties file globals))))))

(defn- configuration-facts [file bindings configuration]
  (let [build-settings (when-let [node (get configuration "build")] (object-properties file node))
        rollup (when-let [node (get build-settings "rollupOptions")] (object-properties file node))
        output (when-let [node (get rollup "output")] (object-properties file node))
        worker (when-let [node (get configuration "worker")] (object-properties file node))
        esbuild (when-let [node (get configuration "esbuild")] (object-properties file node))]
    {:path file :root-override? (contains? configuration "root")
     :options {:output (output-values file output) :worker (option-values worker)
               :esbuild (option-values esbuild)}
     :plugin-lists [(plugin-list-facts bindings :vite (get configuration "plugins"))
                    (plugin-list-facts bindings :rollup (get rollup "plugins"))
                    (plugin-list-facts bindings :output (get output "plugins"))
                    (plugin-list-facts bindings :worker (get worker "plugins"))]}))

(defn- exported-config [file ^js source bindings]
  (let [exports (filter ts/isExportAssignment (array-seq (.-statements source)))]
    (when-not (= 1 (count exports))
      (unsupported! file "Expected one default configuration export"))
    (let [^js assignment (first exports)
          ^js expression (.-expression assignment)]
      (when (.-isExportEquals assignment)
        (unsupported! file "CommonJS configuration exports are not supported"))
      (if (ts/isCallExpression expression)
        (let [^js callee (.-expression expression)
              arguments (array-seq (.-arguments expression))]
          (when-not (and (ts/isIdentifier callee)
                         (contains? (:define-config bindings) (.-text callee))
                         (= 1 (count arguments)))
            (unsupported! file "Expected defineConfig with a static object"))
          (object-properties file (first arguments)))
        (object-properties file expression)))))

(defn- dirname-path [file frontend-root bindings ^js expression]
  (let [^js callee (.-expression expression)
        arguments (array-seq (.-arguments expression))
        ^js base (first arguments)]
    (when-not (and (ts/isPropertyAccessExpression callee)
                   (ts/isIdentifier (.-expression callee))
                   (contains? (:path bindings) (.-text ^js (.-expression callee)))
                   (= "resolve" (.-text ^js (.-name callee)))
                   base (ts/isIdentifier base) (= "__dirname" (.-text base))
                   (seq (rest arguments))
                   (every? ts/isStringLiteral (rest arguments)))
      (unsupported! file "Expected path.resolve(__dirname, literal path segments)"))
    (apply node-path/resolve frontend-root (map #(.-text ^js %) (rest arguments)))))

(defn- static-path [file frontend-root bindings ^js expression]
  (cond
    (and expression (ts/isStringLiteral expression))
    (.-text expression)
    (and expression (ts/isCallExpression expression))
    (dirname-path file frontend-root bindings expression)
    :else (unsupported! file "Expected a literal path or path.resolve call")))

(defn- containment-facts [source-root target]
  (let [relative (node-path/relative source-root target)]
    {:relative-path relative :absolute? (node-path/isAbsolute relative) :separator node-path/sep}))

(defn- assert-alias-target! [file frontend-root target]
  (when-not (node-path/isAbsolute target)
    (unsupported! file "Alias replacements must be absolute filesystem paths"))
  (let [source-root (node-path/join frontend-root "src")]
    (law/assert-vite-alias-target!
      {:path file :target target
       :lexical (containment-facts source-root target)
       :canonical (when (and (fs/existsSync source-root) (fs/existsSync target))
                    (containment-facts (fs/realpathSync source-root) (fs/realpathSync target)))})))

(defn- alias-entries [file ^js alias]
  (if (ts/isArrayLiteralExpression alias)
    (mapv (fn [element]
            (let [properties (object-properties file element)
                  ^js find-node (get properties "find")]
              (when-not (and (= #{"find" "replacement"} (set (keys properties)))
                             find-node (ts/isStringLiteral find-node))
                (unsupported! file "Alias entries require literal find and replacement only"))
              [(.-text find-node) (get properties "replacement")]))
          (array-seq (.-elements alias)))
    (seq (object-properties file alias))))

(defn- merge-alias! [file aliases alias-name target]
  (when (str/blank? alias-name)
    (unsupported! file "Alias names must be nonempty strings"))
  (when (and (contains? aliases alias-name) (not= (get aliases alias-name) target))
    (unsupported! file (str "Conflicting alias replacements: " alias-name)))
  (assoc aliases alias-name target))

(defn- assert-aliases! [file frontend-root bindings configuration]
  (let [resolve-node (get configuration "resolve")
        alias (when resolve-node (get (object-properties file resolve-node) "alias"))]
    (if alias
      (reduce (fn [aliases [alias-name replacement]]
                (let [target (static-path file frontend-root bindings replacement)]
                  (assert-alias-target! file frontend-root target)
                  (merge-alias! file aliases alias-name (node-path/normalize target))))
              {} (alias-entries file alias))
      {})))

(defn- assert-entry! [file frontend-root bindings expected ^js entry]
  (law/assert-vite-entry!
    {:path file :expected expected
     :expected-target (node-path/resolve frontend-root expected)
     :actual (node-path/resolve frontend-root (static-path file frontend-root bindings entry))}))

(defn- assert-bridge! [file frontend-root bindings expected configuration]
  (let [build (object-properties file (get configuration "build"))
        library (object-properties file (get build "lib"))]
    (assert-entry! file frontend-root bindings expected (get library "entry"))
    (when-let [rollup-node (get build "rollupOptions")]
      (when-let [input (get (object-properties file rollup-node) "input")]
        (assert-entry! file frontend-root bindings expected input)))))

(defn- assert-configuration! [frontend-root config-name expected]
  (let [file (node-path/join frontend-root config-name)]
    (if (fs/existsSync file)
      (let [^js source (ts/createSourceFile file (fs/readFileSync file "utf8")
                                          (.-Latest ts/ScriptTarget) true)
            bindings (imported-names source)]
        (when (seq (array-seq (.-parseDiagnostics source)))
          (unsupported! file "Configuration syntax could not be parsed"))
        (config-source/assert-source! file :vite source)
        (let [configuration (exported-config file source bindings)]
          (law/assert-vite-config-admission!
            (configuration-facts file bindings configuration))
          (let [aliases (assert-aliases! file frontend-root bindings configuration)]
            (when expected
              (assert-bridge! file frontend-root bindings expected configuration))
            aliases)))
      {})))

(defn- package-scripts [file]
  (if (fs/existsSync file)
    (let [^js package-data (js/JSON.parse (fs/readFileSync file "utf8"))
          scripts (js->clj (.-scripts package-data))]
      (when-not (or (nil? scripts)
                    (and (map? scripts) (every? string? (vals scripts))))
        (unsupported! file "Package scripts must be command strings"))
      (or scripts {}))
    {}))

(defn- script-configs [file script-name command]
  (let [invocations (re-seq #"(?:^|[\s\"';&|])(?:[^\s\"';&|]*/)?vite[\"']?\s+build(?=$|[\s\"';&|])" command)
        configured (re-seq #"(?:^|[\s\"';&|])(?:[^\s\"';&|]*/)?vite[\"']?\s+build\s+(?:--config(?:=|\s+)|-c\s+)([^\s\"';&|]+)(?:[ \t]+--watch)?[ \t]*(?=$|[\"';&|\r\n])"
                           command)]
    (when-not (= (count invocations) (count configured))
      (unsupported! file (str "Vite builds require an explicit static config: " script-name)))
    (mapv second configured)))

(defn- shadow-configuration-facts [file]
  (if (fs/existsSync file)
    (let [configuration (edn/read-string (fs/readFileSync file "utf8"))]
      (when-not (map? configuration)
        (unsupported! file "Shadow configuration must be an EDN map"))
      (let [configurations (filter map? (tree-seq coll? seq configuration))]
        {:path file
         :resolutions (vec (mapcat seq (keep :resolve configurations)))
         :build-hooks (mapv :build-hooks (filter #(contains? % :build-hooks) configurations))}))
    {:path file :resolutions [] :build-hooks []}))

(defn- production-phases [file command]
  (when-not (string? command)
    (unsupported! file "Active Shadow bridges require a production build command"))
  (let [known {"vite build --config vite.bridge.config.ts" "build:bridge"
               "vite build --config vite.app-bridge.config.ts" "build:app-bridge"
               "shadow-cljs release app" :shadow
               "tailwindcss -c tailwind.config.ts -i src/index.css -o dist/app.css" :css}]
    (mapv (fn [phase]
            (let [normalized (-> (str/join " " (str/split (str/trim phase) #"\s+"))
                                 (str/replace #"^(?:\./)?node_modules/\.bin/vite(?=\s)" "vite"))]
              (or (get known normalized)
                  (unsupported! file (str "Unsupported production build phase: " phase)))))
          (str/split command #"&&" -1))))

(defn- assert-shadow-bridge-builds! [frontend-root scripts]
  (let [shadow-file (node-path/join frontend-root "shadow-cljs.edn")
        facts (-> (shadow-configuration-facts shadow-file)
                  law/assert-shadow-bridge-resolutions!
                  shadow-law/assert-file-resolutions!
                  shadow-law/assert-build-hooks!)
        required-bridges (law/required-bridge-builds (set (map first (:resolutions facts))))]
    (when (or (seq required-bridges) (contains? scripts "build"))
      (let [file (node-path/join frontend-root "package.json")]
        (law/assert-production-build!
          {:path file
           :available-scripts (set (keys scripts))
           :required-bridges required-bridges
           :phases (production-phases file (get scripts "build"))})))))

(defn- assert-active-configs! [frontend-root]
  (let [file (node-path/join frontend-root "package.json")
        scripts (package-scripts file)
        bridges {"build:bridge" "vite.bridge.config.ts"
                 "build:app-bridge" "vite.app-bridge.config.ts"}
        allowed (set (map #(node-path/resolve frontend-root %) (vals bridges)))]
    (assert-shadow-bridge-builds! frontend-root scripts)
    (doseq [[script-name command] scripts]
      (let [configs (mapv #(node-path/resolve frontend-root %)
                          (script-configs file script-name command))]
        (law/assert-vite-script-configs!
          {:path file :script-name script-name :allowed-configs allowed
           :expected-config (when-let [expected (get bridges script-name)]
                              (node-path/resolve frontend-root expected))
           :configs (mapv (fn [config] {:config-path config :exists? (fs/existsSync config)}) configs)})))))

(defn assert-configs!
  "Validate static Vite entries and return contained alias mappings without execution."
  [root]
  (let [frontend-root (node-path/resolve root "frontend")]
    (assert-active-configs! frontend-root)
    (reduce (fn [aliases [config-name expected]]
              (reduce-kv (partial merge-alias! config-name) aliases
                         (assert-configuration! frontend-root config-name expected)))
            {} [["vite.config.ts" nil]
                ["vite.bridge.config.ts" "src/bridge/index.ts"]
                ["vite.app-bridge.config.ts" "src/bridge/app.ts"]])))
