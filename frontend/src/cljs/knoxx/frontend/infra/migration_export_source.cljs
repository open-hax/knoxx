(ns knoxx.frontend.infra.migration-export-source
  "TypeScript symbol decoding for compatibility bridge export provenance."
  (:require ["node:path" :as node-path]
            ["typescript" :as ts]
            [knoxx.frontend.infra.migration-imports :as imports]
            [knoxx.frontend.shape.migration :as shape]))

(defn- compiler-host [resolution ^js options]
  (let [host (ts/createCompilerHost options)]
    (set! (.-resolveTypeReferenceDirectives host)
          (fn [references] (js/Array. (alength references))))
    (set! (.-resolveModuleNames host)
          (fn [module-names containing-file]
            (into-array
              (map (fn [specifier]
                     (if-let [target (imports/resolve-target resolution containing-file specifier)]
                       (if (and (re-find #"\.(?:tsx?|jsx?|[cm][jt]s|json)$" target)
                                (or (not (re-find #"\.json$" target)) (.-resolveJsonModule options)))
                         #js {:resolvedFileName target :isExternalLibraryImport false}
                         js/undefined)
                       js/undefined))
                   (array-seq module-names)))))
    host))

(defn- export-origins [root ^js checker ^js source-file export-name]
  (let [module-symbol (.getSymbolAtLocation checker source-file)
        ^js exported (some #(when (= export-name (.-name ^js %)) %)
                           (array-seq (.getExportsOfModule checker module-symbol)))
        ^js origin (when exported
                     (if (pos? (bit-and (.-flags exported) (.-Alias ts/SymbolFlags)))
                       (.getAliasedSymbol checker exported)
                       exported))
        paths (->> (some-> origin .-declarations array-seq)
                   (map (fn [^js declaration]
                          (->> (.-fileName (.getSourceFile declaration))
                               (node-path/relative root)
                               shape/normalize-path)))
                   distinct vec)]
    paths))

(defn with-provenance
  "Read app export declaration paths; empty paths retain unresolved symbol evidence."
  [root resolution records]
  (let [app-records (filter #(= :app (:bridge %)) records)]
    (if (empty? app-records)
      records
      (let [options (js/Object.assign #js {} (:options resolution)
                                      #js {:noLib true :noEmit true :types #js []})
            roots (->> app-records (map #(node-path/join root (:path %))) distinct into-array)
            ^js program (ts/createProgram roots options (compiler-host resolution options))
            checker (.getTypeChecker program)]
        (mapv (fn [{:keys [bridge path] export-name :symbol :as record}]
                (if (= :app bridge)
                  (assoc record :origin-paths
                         (export-origins root checker
                                         (.getSourceFile program (node-path/join root path)) export-name))
                  record))
              records)))))
