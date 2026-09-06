(ns knoxx.frontend.infra.migration-build
  "Static Vite configuration inspection for the governed frontend inventory."
  (:require ["node:fs" :as fs]
            ["node:path" :as node-path]
            ["typescript" :as ts]
            [clojure.string :as str]))

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
            :else bindings))
        bindings))
    {:define-config #{} :path #{}} (array-seq (.-statements source))))

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

(defn- outside? [source-root target]
  (let [relative (node-path/relative source-root target)]
    (or (node-path/isAbsolute relative) (= relative "..")
        (str/starts-with? relative (str ".." node-path/sep)))))

(defn- assert-alias-target! [file frontend-root target]
  (when-not (node-path/isAbsolute target)
    (unsupported! file "Alias replacements must be absolute filesystem paths"))
  (let [source-root (node-path/join frontend-root "src")]
    (when (or (outside? source-root target)
              (and (fs/existsSync target)
                   (outside? (fs/realpathSync source-root) (fs/realpathSync target))))
      (throw (ex-info "Vite alias leaves governed frontend source tree"
                      {:path file :target target})))))

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
  (let [actual (node-path/resolve frontend-root
                                 (static-path file frontend-root bindings entry))]
    (when-not (= (node-path/resolve frontend-root expected) actual)
      (throw (ex-info "Vite bridge entry leaves governed inventory"
                      {:path file :expected expected :actual actual})))))

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
        (let [configuration (exported-config file source bindings)]
          (when (contains? configuration "root")
            (unsupported! file "Vite root overrides are not supported"))
          (let [aliases (assert-aliases! file frontend-root bindings configuration)]
            (when expected
              (assert-bridge! file frontend-root bindings expected configuration))
            aliases)))
      {})))

(defn assert-configs!
  "Validate static Vite entries and return contained alias mappings without execution."
  [root]
  (let [frontend-root (node-path/resolve root "frontend")]
    (reduce (fn [aliases [config-name expected]]
              (reduce-kv (partial merge-alias! config-name) aliases
                         (assert-configuration! frontend-root config-name expected)))
            {} [["vite.config.ts" nil]
                ["vite.bridge.config.ts" "src/bridge/index.ts"]
                ["vite.app-bridge.config.ts" "src/bridge/app.ts"]])))
