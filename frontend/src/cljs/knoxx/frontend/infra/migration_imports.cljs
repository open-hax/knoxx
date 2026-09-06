(ns knoxx.frontend.infra.migration-imports
  "TypeScript dependency syntax inspection for the governed source boundary."
  (:require ["node:path" :as node-path]
            ["typescript" :as ts]
            [clojure.string :as str]))

(defn- assert-local-target! [source-root path specifier]
  (let [target (node-path/resolve (node-path/dirname path) specifier)
        relative (node-path/relative source-root target)]
    (when (or (node-path/isAbsolute relative)
              (= relative "..")
              (str/starts-with? relative (str ".." node-path/sep)))
      (throw (ex-info "Local import leaves governed frontend source tree"
                      {:path path :source specifier :target target})))))

(defn assert-contained!
  "Reject local module imports and file references outside the governed tree."
  [root path source]
  (let [source-root (node-path/join root "frontend" "src")
        absolute-path (node-path/join root path)
        ^js information (ts/preProcessFile source true true)]
    (doseq [^js reference (array-seq (.-importedFiles information))
            :let [specifier (.-fileName reference)]
            :when (or (str/starts-with? specifier ".") (node-path/isAbsolute specifier))]
      (assert-local-target! source-root absolute-path specifier))
    (doseq [^js reference (array-seq (.-referencedFiles information))]
      (assert-local-target! source-root absolute-path (.-fileName reference)))))
