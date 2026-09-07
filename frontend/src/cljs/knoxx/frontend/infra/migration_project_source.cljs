(ns knoxx.frontend.infra.migration-project-source
  "Filesystem census of project namespace and route-location source facts."
  (:require ["node:fs" :as fs]
            ["node:path" :as node-path]
            [knoxx.frontend.infra.migration-router-source :as router]
            [knoxx.frontend.law.migration :as law]
            [knoxx.frontend.shape.migration :as shape]))

(defn- merge-source-facts [prior facts]
  (-> facts
      (update :dependencies into (:dependencies prior))
      (update :bridge-exports into (:bridge-exports prior))
      (update :unresolved-bridge? #(or % (:unresolved-bridge? prior)))))

(defn read-namespaces
  "Read governed project namespaces and validate the existing route-location boundary."
  [{:keys [root app-path walk-files read-forms route-forms inspect-nodes]}]
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
    (for [source-path ["frontend/src" "shared/src/cljs"]
          :let [source-root (node-path/join root source-path)]
          :when (fs/existsSync source-root)
          absolute-path (walk-files source-root)
          :when (re-find #"\.clj[sc]$" absolute-path)]
      absolute-path)))
