(ns knoxx.frontend.infra.migration-router-source
  "Reader-form inspection of React Router namespace and API syntax.")

(defn aliases
  "Read React Router module aliases from parsed namespace require forms."
  [forms]
  (->> forms
       (filter #(and (seq? %) (= 'ns (first %))))
       (mapcat #(drop 2 %))
       (filter #(and (seq? %) (= :require (first %))))
       (mapcat rest)
       (filter #(and (vector? %)
                     (contains? #{"react-router" "react-router-dom"} (first %))))
       (mapcat #(partition 2 (rest %)))
       (keep (fn [[option alias-name]]
               (when (and (= :as option) (symbol? alias-name)) alias-name)))
       set))

(defn api-reference?
  "Whether a symbol names an API that constructs routes."
  [value]
  (and (symbol? value)
       (re-matches #"(?:\.|-|\.-)?(?:Route|useRoutes|createBrowserRouter|createHashRouter|createMemoryRouter)"
                   (name value))))

(defn computed-access?
  "Recognize computed reads or unresolved threading of a declared router alias."
  [router-aliases form]
  (and (seq? form)
       (symbol? (first form))
       (contains? #{"aget" "get" "getValueByKeys" "Reflect.get"
                    "->" "->>" "some->" "some->>" "cond->" "cond->>" "as->"}
                  (name (first form)))
       (contains? router-aliases (second form))))

(defn element-components
  "Read component positions from Helix and direct React element-construction forms."
  [nodes]
  (->> nodes
       (filter #(and (seq? %) (symbol? (first %))
                     (contains? #{"$" "createElement" ".createElement"} (name (first %)))))
       (map #(nth % (if (= ".createElement" (name (first %))) 2 1) nil))))

(defn symbol-references
  "Decode source symbols into plain reference names and namespace facts."
  [nodes]
  (->> nodes
       (filter symbol?)
       (mapv (fn [reference] {:name (str reference) :namespace (namespace reference)}))))

(defn definition-references
  "Decode declarations and assignments under local and self-qualified names."
  [nodes inspect-nodes]
  (let [source-namespace (some #(when (and (seq? %) (= 'ns (first %))) (str (second %))) nodes)
        definitions (reduce (fn [result form]
                              (if (and (seq? form) (symbol? (first form)) (symbol? (second form))
                                       (contains? #{"def" "defonce" "defn" "defn-" "defnc" "set!"}
                                                  (name (first form))))
                                (let [binding-name (second form)
                                      local-name (if (= source-namespace (namespace binding-name))
                                                   (name binding-name) (str binding-name))]
                                  (update result local-name (fnil into [])
                                          (symbol-references (inspect-nodes (drop 2 form)))))
                                result))
                            {} nodes)]
    (if source-namespace
      (into definitions (map (fn [[local-name references]]
                               [(str source-namespace "/" local-name) references])) definitions)
      definitions)))
