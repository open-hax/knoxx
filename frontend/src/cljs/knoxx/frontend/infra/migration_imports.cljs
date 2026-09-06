(ns knoxx.frontend.infra.migration-imports
  "TypeScript dependency syntax inspection for the governed source boundary."
  (:require ["node:path" :as node-path]
            ["typescript" :as ts]
            [clojure.string :as str]))

(defn- assert-target! [source-root path specifier target]
  (let [relative (node-path/relative source-root target)]
    (when (or (node-path/isAbsolute relative)
              (= relative "..")
              (str/starts-with? relative (str ".." node-path/sep)))
      (throw (ex-info "Local import leaves governed frontend source tree"
                      {:path path :source specifier :target target}))))
  target)

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

(defn- resolved-target [source-root options path specifier]
  (let [^js result (ts/resolveModuleName specifier path options ts/sys)
        ^js resolved (.-resolvedModule result)]
    (when (and resolved (not (.-isExternalLibraryImport resolved)))
      (assert-target! source-root path specifier (.-resolvedFileName resolved)))))

(defn resolve-target
  "Resolve and validate a local module or project alias; packages return nil."
  [{:keys [source-root options aliases]} path specifier]
  (let [local? (or (str/starts-with? specifier ".") (node-path/isAbsolute specifier))
        lexical-target (when local? (node-path/resolve (node-path/dirname path) specifier))
        vite-target (alias-target source-root aliases path specifier)
        typescript-target (resolved-target source-root options path specifier)]
    (when local?
      (assert-target! source-root path specifier lexical-target))
    (if vite-target
      (or (resolved-target source-root options path vite-target) vite-target)
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
  (when (ts/isNewExpression node)
    (let [^js constructor (.-expression node)]
      (when (ts/isIdentifier constructor)
        (.-text constructor)))))

(defn- assert-worker-url! [{:keys [root source-root] :as resolution} path ^js node]
  (when (contains? #{"Worker" "SharedWorker"} (new-constructor node))
    (let [^js url (first (array-seq (.-arguments node)))]
      (when (and url (= "URL" (new-constructor url)))
        (let [[^js specifier base] (array-seq (.-arguments url))]
          (when (= "url" (import-meta-property base))
            (when-not (and specifier
                           (or (ts/isStringLiteral specifier)
                               (ts/isNoSubstitutionTemplateLiteral specifier)))
              (throw (ex-info "Unsupported dynamic worker URL in migration source"
                              {:path path})))
            (let [source (.-text specifier)
                  module-source (if (str/starts-with? source "/")
                                  (node-path/join root "frontend" source)
                                  source)
                  target (or (resolve-target resolution path module-source)
                             (node-path/resolve (node-path/dirname path) module-source))]
              (assert-target! source-root path source target))))))))

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
  (let [module (ts/createSourceFile path source (.-Latest ts/ScriptTarget) true)]
    (letfn [(inspect [node]
              (when (contains? #{"glob" "globEager"} (import-meta-property node))
                (throw (ex-info "Vite glob imports are outside the supported migration grammar"
                                {:path path})))
              (assert-worker-url! resolution path node)
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
