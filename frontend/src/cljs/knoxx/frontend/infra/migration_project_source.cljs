(ns knoxx.frontend.infra.migration-project-source
  "Filesystem census of project namespace and route-location source facts."
  (:require ["node:fs" :as fs]
            ["node:path" :as node-path]
            [cljs.tools.reader.edn :as edn]
            [knoxx.frontend.infra.migration-router-source :as router]
            [knoxx.frontend.law.migration :as law]
            [knoxx.frontend.shape.migration :as shape]))

(defn- macro-declaration? [node]
  (when (and (seq? node) (symbol? (first node)))
    (let [constructor (name (first node))
          declaration (drop 2 node)
          declaration (if (string? (first declaration)) (rest declaration) declaration)
          attributes (when (map? (first declaration)) (first declaration))
          bodies (if attributes (rest declaration) declaration)
          trailing (when (and (seq? (first bodies)) (map? (last bodies))) (last bodies))]
      (or (= "defmacro" constructor)
          (and (contains? #{"def" "defn" "defn-"} constructor)
               (:macro (meta (second node))))
          (and (contains? #{"defn" "defn-"} constructor)
               (or (:macro attributes) (:macro trailing)))))))

(defn- macro-facts [forms inspect-nodes]
  (let [nodes (inspect-nodes forms)
        namespaces (filter #(and (seq? %) (= 'ns (first %))) forms)]
    {:definitions (vec (keep (fn [node]
                               (when (macro-declaration? node)
                                 (str (second node)))) nodes))
     :imports (vec (for [form namespaces clause (drop 2 form)
                         :when (seq? clause)
                         option (if (= :require-macros (first clause))
                                  [:require-macros]
                                  (for [specification (rest clause) :when (vector? specification)
                                        [option value] (partition 2 (rest specification))
                                        :when (and (contains? #{:refer-macros :include-macros} option) value)]
                                    option))]
                     option))}))

(defn- configured-source-roots [root]
  (let [frontend-root (node-path/join root "frontend")
        config-path (node-path/join frontend-root "shadow-cljs.edn")
        paths (when (fs/existsSync config-path)
                (:source-paths (edn/read-string (fs/readFileSync config-path "utf8"))))]
    (when-not (or (nil? paths) (and (sequential? paths) (every? string? paths)))
      (throw (ex-info "Unsupported Shadow source path syntax" {:path config-path})))
    (distinct (concat [(node-path/join root "frontend/src") (node-path/join root "shared/src/cljs")]
                      (map #(node-path/resolve frontend-root %) paths)))))

(defn- assert-project-macros! [{:keys [root walk-files read-forms inspect-nodes]}]
  (doseq [source-root (configured-source-roots root) :when (fs/existsSync source-root)
          absolute-path (walk-files source-root) :when (re-find #"\.clj[sc]?$" absolute-path)
          :let [path (shape/normalize-path (node-path/relative root absolute-path))
                source (fs/readFileSync absolute-path "utf8")
                forms (read-forms path source (if (re-find #"\.cljs$" path) #{:cljs} #{:clj}))
                forms (if (re-find #"\.cljc$" path)
                        (concat forms (read-forms path source #{:cljs})) forms)]]
    (law/assert-project-macros! (assoc (macro-facts forms inspect-nodes) :path path))))

(defn- merge-source-facts [prior facts]
  (-> facts
      (update :dependencies into (:dependencies prior))
      (update :bridge-exports into (:bridge-exports prior))
      (update :unresolved-bridge? #(or % (:unresolved-bridge? prior)))))

(defn read-namespaces
  "Read governed project namespaces and validate the existing route-location boundary."
  [{:keys [root app-path walk-files read-forms route-forms inspect-nodes] :as options}]
  (assert-project-macros! options)
  (reduce
    (fn [result absolute-path]
      (let [path (shape/normalize-path (node-path/relative root absolute-path))
            source (fs/readFileSync absolute-path "utf8")
            facts (router/namespace-facts (read-forms path source) inspect-nodes)]
        (law/assert-route-location!
          {:path path :supported-path app-path
           :route-count (count (route-forms path source))})
        (update result (:namespace facts) merge-source-facts facts)))
    {}
    (for [source-root (configured-source-roots root)
          :when (fs/existsSync source-root)
          absolute-path (walk-files source-root)
          :when (re-find #"\.clj[sc]$" absolute-path)]
      absolute-path)))
