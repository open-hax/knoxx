(ns knoxx.frontend.infra.migration-imports
  "TypeScript dependency syntax inspection for the governed source boundary."
  (:require ["node:path" :as node-path]
            ["typescript" :as ts]
            [clojure.string :as str]
            [knoxx.frontend.infra.migration-packages :as packages]
            [knoxx.frontend.law.migration :as law]
            [knoxx.frontend.law.migration-worker :as worker-law]))

(defn- assert-target! [source-root path specifier target]
  (let [relative (node-path/relative source-root target)]
    (law/assert-import-target!
      {:path path :source specifier :target target
       :relative-path relative
       :absolute? (node-path/isAbsolute relative)
       :separator node-path/sep})))

(defn- compiler-options [root]
  (let [directory (node-path/join root "frontend")
        config-path (node-path/join directory "tsconfig.json")]
    (if ((.-fileExists ts/sys) config-path)
      (let [^js config (ts/readConfigFile config-path (.-readFile ts/sys))]
        (when (.-error config)
          (throw (ex-info "Unsupported TypeScript module configuration" {:path config-path})))
        (let [^js parsed (ts/parseJsonConfigFileContent (.-config config) ts/sys directory)
              errors (remove #(contains? #{18002 18003} (.-code ^js %))
                             (array-seq (.-errors parsed)))]
          (when (seq errors)
            (throw (ex-info "Unsupported TypeScript module configuration" {:path config-path})))
          (.-options parsed)))
      #js {:moduleResolution (.-Bundler ts/ModuleResolutionKind)})))

(defn resolver
  "Load the project's TypeScript module-resolution configuration once per inventory."
  [root aliases]
  {:root root
   :source-root (node-path/join root "frontend" "src")
   :aliases aliases
   :packages (packages/resolver root)
   :options (compiler-options root)})

(defn- alias-target [source-root aliases path specifier]
  (let [targets (->> aliases
                     (keep (fn [[prefix replacement]]
                             (when (or (= prefix specifier)
                                       (str/starts-with? specifier (str prefix "/")))
                               (node-path/resolve (str replacement (subs specifier (count prefix)))))))
                     distinct vec)]
    (when (> (count targets) 1)
      (throw (ex-info "Ambiguous Vite migration alias" {:path path :source specifier})))
    (when-let [target (first targets)]
      (assert-target! source-root path specifier target))))

(defn- resolved-target [source-root options package-context path specifier]
  (let [^js result (ts/resolveModuleName specifier path options ts/sys)
        ^js resolved (.-resolvedModule result)]
    (when resolved
      (when-let [target (packages/resolved-target package-context path specifier
                                                  (.-resolvedFileName resolved)
                                                  (.-isExternalLibraryImport resolved))]
        (assert-target! source-root path specifier target)))))

(defn resolve-target
  "Resolve and validate a local module or project alias; packages return nil."
  [{:keys [source-root options aliases packages]} path specifier]
  (let [local? (or (str/starts-with? specifier ".") (node-path/isAbsolute specifier))
        lexical-target (when local? (node-path/resolve (node-path/dirname path) specifier))
        vite-target (alias-target source-root aliases path specifier)
        typescript-target (resolved-target source-root options packages path specifier)]
    (when-let [target (packages/declared-target packages path specifier)]
      (assert-target! source-root path specifier target))
    (when local?
      (assert-target! source-root path specifier lexical-target))
    (if vite-target
      (or (resolved-target source-root options packages path vite-target) vite-target)
      (or typescript-target lexical-target))))

(defn- import-meta-property [^js node]
  (when (and node (or (ts/isPropertyAccessExpression node) (ts/isElementAccessExpression node)))
    (let [^js receiver (.-expression node)
          ^js property (if (ts/isPropertyAccessExpression node)
                         (.-name node)
                         (.-argumentExpression node))]
      (when (and (ts/isMetaProperty receiver)
                  (= (.-ImportKeyword ts/SyntaxKind) (.-keywordToken receiver))
                  (= "meta" (.-text ^js (.-name receiver)))
                  property (or (ts/isIdentifier property) (ts/isStringLiteral property)))
        (.-text property)))))

(defn- new-constructor [^js node]
  (when (and node (ts/isNewExpression node))
    (let [^js constructor (.-expression node)]
      (when (ts/isIdentifier constructor)
        (.-text constructor)))))

(defn- member-path [^js node]
  (cond
    (ts/isIdentifier node) [(.-text node)]
    (or (ts/isPropertyAccessExpression node) (ts/isElementAccessExpression node))
    (let [^js property (if (ts/isPropertyAccessExpression node)
                         (.-name node) (.-argumentExpression node))]
      (when (and property (if (ts/isPropertyAccessExpression node)
                            (ts/isIdentifier property)
                            (or (ts/isStringLiteral property)
                                (ts/isNoSubstitutionTemplateLiteral property))))
        (when-let [receiver (member-path (.-expression node))]
          (conj receiver (.-text property)))))
    :else nil))

(defn- imported-bindings [^js module]
  (let [bindings (atom #{})]
    (letfn [(inspect [^js node type-only?]
              (let [type-only? (or type-only? (.-isTypeOnly node))]
                (when (and (not type-only?)
                           (or (ts/isImportClause node) (ts/isImportSpecifier node)
                               (ts/isNamespaceImport node)) (.-name node))
                  (swap! bindings conj (.-text ^js (.-name node))))
                (ts/forEachChild node #(inspect % type-only?))
                nil))]
      (inspect module false)
      @bindings)))

(defn- browser-worker-reference [bindings node]
  (let [members (member-path node)
        root (first members)
        members (if (contains? #{"window" "globalThis" "self"} root)
                  (subvec members 1) members)]
    (when-not (contains? bindings root)
      (cond
        (contains? #{["Worker"] ["SharedWorker"]} members) :worker
        (= ["navigator" "serviceWorker"] members) :service-worker
        (= ["navigator" "serviceWorker" "register"] members) :registration
        :else nil))))

(defn- worker-call [bindings ^js node]
  (when (or (ts/isNewExpression node) (ts/isCallExpression node))
    (let [kind (browser-worker-reference bindings (.-expression node))]
      (cond
        (and (= :worker kind) (ts/isNewExpression node)) :worker
        (and (= :registration kind) (ts/isCallExpression node)) :service-worker
        :else nil))))

(defn- runtime-reference? [^js node]
  (let [^js parent (.-parent node)]
    (and parent
         (or (ts/isExpressionNode node) (ts/isShorthandPropertyAssignment parent))
         (not (and (ts/isPropertyAccessExpression parent) (identical? node (.-name parent))))
         (not (loop [^js ancestor parent]
                (when ancestor
                  (or (ts/isTypeNode ancestor) (recur (.-parent ancestor)))))))))

(defn- container-usage [^js node ^js parent]
  (let [^js call (.-parent parent)
        method (last (member-path parent))]
    (when (and (or (ts/isPropertyAccessExpression parent) (ts/isElementAccessExpression parent))
               (identical? node (.-expression parent)) call)
      (cond
        (ts/isTypeOfExpression call) :type-query
        (and (ts/isCallExpression call) (identical? parent (.-expression call)))
        (cond
          (= "register" method) :registration-call
          (contains? #{"getRegistration" "getRegistrations" "startMessages"} method) :query-call
          :else nil)
        :else nil))))

(defn- assert-worker-reference! [bindings path ^js node]
  (when-let [kind (browser-worker-reference bindings node)]
    (when (runtime-reference? node)
      (let [^js parent (.-parent node)
            usage (cond
                    (ts/isTypeOfExpression parent) :type-query
                    (and (= :worker kind) (ts/isNewExpression parent)
                         (identical? node (.-expression parent))) :constructor-call
                    (and (= :registration kind) (ts/isCallExpression parent)
                         (identical? node (.-expression parent))) :registration-call
                    (= :service-worker kind) (container-usage node parent)
                    :else nil)]
        (worker-law/assert-reference! {:path path :kind kind :usage usage})))))

(defn- assert-worker-source! [bindings path ^js node]
  (when-let [kind (worker-call bindings node)]
    (let [^js source (first (array-seq (.-arguments node)))
          base (when (= "URL" (new-constructor source))
                 (second (array-seq (.-arguments source))))]
      (worker-law/assert-source! {:path path :kind kind
                                 :governed-url? (= "url" (import-meta-property base))}))))

(defn- assert-asset-url! [{:keys [root source-root] :as resolution} path ^js node]
  (when (= "URL" (new-constructor node))
    (let [[^js specifier base] (array-seq (.-arguments node))]
      (when (= "url" (import-meta-property base))
        (when-not (and specifier
                       (or (ts/isStringLiteral specifier)
                           (ts/isNoSubstitutionTemplateLiteral specifier)))
          (throw (ex-info "Unsupported dynamic Vite asset URL in migration source"
                          {:path path})))
        (let [source (.-text specifier)
              module-source (if (str/starts-with? source "/")
                              (node-path/join root "frontend" source)
                              source)
              target (or (resolve-target resolution path module-source)
                         (node-path/resolve (node-path/dirname path) module-source))]
          (assert-target! source-root path source target))))))

(defn- assert-dynamic-import! [resolution path ^js node]
  (when (and (ts/isCallExpression node)
             (= (.-ImportKeyword ts/SyntaxKind) (.-kind ^js (.-expression node))))
    (let [^js specifier (first (array-seq (.-arguments node)))]
      (when-not (and specifier
                     (or (ts/isStringLiteral specifier)
                         (ts/isNoSubstitutionTemplateLiteral specifier)))
        (throw (ex-info "Non-literal dynamic imports are outside the supported migration grammar"
                        {:path path})))
      (resolve-target resolution path (.-text specifier)))))

(defn- assert-vite-dependencies! [resolution path source]
  (let [module (ts/createSourceFile path source (.-Latest ts/ScriptTarget) true)
        bindings (imported-bindings module)]
    (letfn [(inspect [node]
              (when (contains? #{"glob" "globEager"} (import-meta-property node))
                (throw (ex-info "Vite glob imports are outside the supported migration grammar"
                                {:path path})))
              (assert-asset-url! resolution path node)
              (assert-worker-source! bindings path node)
              (assert-worker-reference! bindings path node)
              (assert-dynamic-import! resolution path node)
              (ts/forEachChild node inspect)
              nil)]
      (inspect module))))

(defn assert-contained!
  "Reject local module imports and file references outside the governed tree."
  [{:keys [root source-root] :as resolution} path source]
  (let [absolute-path (node-path/join root path)
        ^js information (ts/preProcessFile source true true)]
    (assert-vite-dependencies! resolution absolute-path source)
    (doseq [^js reference (array-seq (.-importedFiles information))]
      (resolve-target resolution absolute-path (.-fileName reference)))
    (doseq [^js reference (array-seq (.-referencedFiles information))]
      (let [specifier (.-fileName reference)]
        (assert-target! source-root absolute-path specifier
                        (node-path/resolve (node-path/dirname absolute-path) specifier))))))
