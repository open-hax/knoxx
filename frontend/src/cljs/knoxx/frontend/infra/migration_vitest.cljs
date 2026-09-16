(ns knoxx.frontend.infra.migration-vitest
  "Static Vitest runner and source-scope inspection for the migration inventory."
  (:require ["node:fs" :as fs]
            ["node:path" :as node-path]
            ["typescript" :as ts]
            [knoxx.frontend.infra.migration-config-source :as config-source]
            [knoxx.frontend.law.migration :as law]
            [knoxx.frontend.law.migration-vitest-modules :as module-law]))

(defn- unsupported! [file detail]
  (throw (ex-info "Unsupported Vitest migration configuration" {:path file :detail detail})))

(defn- object-properties [file ^js node]
  (when-not (and node (ts/isObjectLiteralExpression node))
    (unsupported! file "Expected a static object literal"))
  (reduce (fn [properties ^js property]
            (when-not (ts/isPropertyAssignment property)
              (unsupported! file "Object spreads, methods and shorthand are not supported"))
            (let [^js property-name (.-name property)]
              (when-not (or (ts/isIdentifier property-name) (ts/isStringLiteral property-name))
                (unsupported! file "Computed property names are not supported"))
              (let [property-key (.-text property-name)]
                (when (contains? properties property-key)
                  (unsupported! file (str "Duplicate property: " property-key)))
                (assoc properties property-key (.-initializer property)))))
          {} (array-seq (.-properties node))))

(defn- config-imports [^js source]
  (->> (array-seq (.-statements source))
       (filter ts/isImportDeclaration)
       (filter #(contains? #{"vitest/config" "vite"} (.-text ^js (.-moduleSpecifier ^js %))))
       (mapcat (fn [^js declaration]
                 (let [^js clause (.-importClause declaration)
                       ^js bindings (when clause (.-namedBindings clause))]
                   (when (and bindings (ts/isNamedImports bindings))
                     (array-seq (.-elements bindings))))))
       (keep (fn [^js imported]
               (when (= "defineConfig" (.-text ^js (or (.-propertyName imported) (.-name imported))))
                 (.-text ^js (.-name imported)))))
       set))

(defn- exported-configuration [file ^js source]
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
                         (contains? (config-imports source) (.-text callee))
                         (= 1 (count arguments)))
            (unsupported! file "Expected defineConfig with a static object"))
          (object-properties file (first arguments)))
        (object-properties file expression)))))

(defn- literal-scopes [file ^js node scalar?]
  (let [entries (cond
                  (and node (ts/isArrayLiteralExpression node)) (array-seq (.-elements node))
                  (and scalar? node (ts/isStringLiteral node)) [node]
                  :else (unsupported! file "Source scopes must be literal strings in a static array"))]
    (mapv (fn [^js entry]
            (when-not (ts/isStringLiteral entry)
              (unsupported! file "Source scopes must contain only literal strings"))
            (.-text entry))
          entries)))

(defn- config-value [file ^js node]
  (cond
    (or (ts/isStringLiteral node) (ts/isNoSubstitutionTemplateLiteral node)) (.-text node)
    (ts/isNumericLiteral node) (js/Number (.-text node))
    (= (.-NullKeyword ts/SyntaxKind) (.-kind node)) nil
    (= (.-TrueKeyword ts/SyntaxKind) (.-kind node)) true
    (= (.-FalseKeyword ts/SyntaxKind) (.-kind node)) false
    (ts/isArrayLiteralExpression node) (mapv #(config-value file %) (array-seq (.-elements node)))
    (ts/isObjectLiteralExpression node)
    (into {} (map (fn [[property value]] [property (config-value file value)]))
          (object-properties file node))
    :else :dynamic))

(defn- assert-test-scopes! [file configuration]
  (let [test-settings (object-properties file (get configuration "test"))]
    (law/assert-vitest-config-admission! {:path file
                                         :root-fields (set (keys configuration))
                                         :test-fields (set (keys test-settings))})
    (module-law/assert-config!
      {:path file :settings (into {} (map (fn [[field node]] [field (config-value file node)]))
                                 test-settings)})
    (doseq [scope (literal-scopes file (get test-settings "include") false)]
      (law/assert-vitest-scope! file "include" scope))
    (doseq [field ["includeSource" "setupFiles" "globalSetup"]
            :let [node (get test-settings field)]
            :when node
            scope (literal-scopes file node (not= field "includeSource"))]
      (law/assert-vitest-scope! file field scope))))

(defn- assert-static-config! [file]
  (let [^js source (ts/createSourceFile file (fs/readFileSync file "utf8")
                                      (.-Latest ts/ScriptTarget) true)]
    (when (seq (array-seq (.-parseDiagnostics source)))
      (unsupported! file "Configuration syntax could not be parsed"))
    (config-source/assert-source! file :vitest source)
    (assert-test-scopes! file (exported-configuration file source))))

(defn- package-scripts [file]
  (if (fs/existsSync file)
    (let [^js configuration (js/JSON.parse (fs/readFileSync file "utf8"))
          scripts (js->clj (.-scripts configuration))]
      (when-not (or (nil? scripts) (and (map? scripts) (every? string? (vals scripts))))
        (unsupported! file "Package scripts must be command strings"))
      (or scripts {}))
    {}))

(defn- runner-facts [frontend-root file command]
  (let [invocation (re-matches #"(?:NODE_ENV=test\s+)?(?:(?:pnpm(?:\s+exec)?|npx)\s+)?(?:\./node_modules/\.bin/)?vitest(?:\s+(?:run|watch))?\s+(?:--config(?:=|\s+)|-c\s+)([^\s\"';&|]+)(?:\s+--coverage)?\s*" command)]
    {:mentions-vitest? (boolean (re-find #"\bvitest\b" command))
     :direct? (boolean (and invocation
                            (= file (node-path/resolve frontend-root (second invocation)))))}))

(defn- assert-runner-scripts! [frontend-root file]
  (let [package-path (node-path/join frontend-root "package.json")
        runners (into {} (map (fn [[script-name command]]
                                [script-name (runner-facts frontend-root file command)]))
                      (package-scripts package-path))]
    (law/assert-vitest-runners! {:path package-path
                                :config-present? (fs/existsSync file)
                                :runners runners})))

(defn- assert-no-workspaces! [frontend-root]
  (when (fs/existsSync frontend-root)
    (doseq [file-name (array-seq (fs/readdirSync frontend-root))
            :when (re-matches #"^vitest\.(?:workspace|projects)\.(?:[cm]?[jt]s|json)$" file-name)]
      (unsupported! (node-path/join frontend-root file-name)
                    "Automatically selected Vitest workspace/project scopes are not supported"))))

(defn assert-config!
  "Require the active Vitest runner's statically inspectable scopes to stay in frontend/src."
  [root]
  (let [frontend-root (node-path/resolve root "frontend")
        file (node-path/join frontend-root "vitest.config.ts")]
    (assert-runner-scripts! frontend-root file)
    (assert-no-workspaces! frontend-root)
    (when (fs/existsSync file)
      (assert-static-config! file))))
