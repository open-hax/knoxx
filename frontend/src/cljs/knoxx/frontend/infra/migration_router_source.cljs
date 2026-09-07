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

(defn unresolved-module-value?
  "Recognize router module values escaping their statically named member access."
  [router-aliases form]
  (and (coll? form)
       (some router-aliases form)
       (not (and (seq? form) (= 2 (count form)) (symbol? (first form))
                 (re-matches #"\.-[A-Za-z_$][-A-Za-z0-9_$]*" (name (first form)))
                 (contains? router-aliases (second form))))))

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

(defn- binding-names [pattern]
  (cond
    (symbol? pattern) (if (or (namespace pattern) (= '& pattern)) #{} #{(str pattern)})
    (vector? pattern) (into #{} (mapcat binding-names) pattern)
    (map? pattern)
    (into #{} (mapcat (fn [[entry value]]
                       (cond
                         (= :or entry) []
                         (= :as entry) (binding-names value)
                         (and (keyword? entry) (contains? #{"keys" "syms" "strs"} (name entry)))
                         (map #(name %) value)
                         :else (binding-names entry)))) pattern)
    :else #{}))

(defn- binding-children [bindings outer-names]
  (reduce (fn [{:keys [children names]} [pattern initializer]]
            (if (= :let pattern)
              (let [nested (binding-children initializer names)]
                {:children (into children (:children nested)) :names (:names nested)})
              {:children (conj children [pattern names] [initializer names])
               :names (into names (binding-names pattern))}))
          {:children [] :names outer-names} (partition 2 bindings)))

(defn- function-children [declaration outer-names]
  (let [declaration (drop-while #(or (string? %) (map? %)) declaration)
        arities (if (vector? (first declaration)) [declaration] declaration)]
    (mapcat (fn [arity]
              (when (and (seq? arity) (vector? (first arity)))
                (let [names (into outer-names (binding-names (first arity)))]
                  (map #(vector % names) (rest arity))))) arities)))

(defn- scoped-binding-children [form outer-names]
  (let [{:keys [children names]} (binding-children (second form) outer-names)
        conditional? (contains? #{"if-let" "if-some"} (name (first form)))]
    (into children
          (map-indexed (fn [index body]
                         [body (if (and conditional? (pos? index)) outer-names names)]))
          (drop 2 form))))

(defn- scoped-function-children [form outer-names]
  (let [constructor (name (first form))
        anonymous? (contains? #{"fn" "fn*"} constructor)
        named? (symbol? (second form))
        local-name (when (and anonymous? named?) (str (second form)))]
    (function-children (drop (if (and anonymous? (not named?)) 1 2) form)
                       (cond-> outer-names local-name (conj local-name)))))

(defn- scoped-letfn-children [form outer-names]
  (let [declarations (second form)
        names (into outer-names (mapcat #(binding-names (first %))) declarations)]
    (concat (mapcat #(function-children (rest %) names) declarations)
            (map #(vector % names) (drop 2 form)))))

(defn- scoped-children [form outer-names]
  (let [constructor (when (and (seq? form) (symbol? (first form))) (name (first form)))]
    (cond
      (and (contains? #{"let" "let*" "loop" "loop*" "binding" "with-open"
                         "if-let" "when-let" "if-some" "when-some" "for" "doseq"} constructor)
           (vector? (second form))) (scoped-binding-children form outer-names)
      (contains? #{"fn" "fn*" "defn" "defn-" "defnc"} constructor)
      (scoped-function-children form outer-names)
      (= "letfn" constructor) (scoped-letfn-children form outer-names)
      (= "as->" constructor)
      (cons [(second form) outer-names]
            (map #(vector % (into outer-names (binding-names (nth form 2 nil)))) (drop 3 form)))
      (= "catch" constructor)
      (map #(vector % (into outer-names (binding-names (nth form 2 nil)))) (drop 3 form))
      :else (map #(vector % outer-names) form))))

(defn lexical-route-components
  "Read lexically bound component positions for each canonical route without resolving values."
  [forms ignored-form?]
  (let [components (atom {})]
    (letfn [(inspect [form names enclosing-route]
              (when (and (coll? form) (not (ignored-form? form)))
                (let [route (if (and (seq? form) (= '$ (first form)) (= 'Route (second form)))
                              form enclosing-route)]
                  (when route
                    (swap! components update route (fnil into #{})
                           (keep #(when (and (symbol? %) (nil? (namespace %))
                                              (contains? names (str %))) (str %))
                                 (element-components [form]))))
                  (doseq [[child child-names] (scoped-children form names)]
                    (inspect child child-names route)))))]
      (doseq [form forms] (inspect form #{} nil))
      @components)))

(defn- namespace-form? [form]
  (and (seq? form) (= 'ns (first form))))

(defn- require-bindings [forms]
  (for [form forms :when (namespace-form? form)
        clause (drop 2 form) :when (and (seq? clause) (= :require (first clause)))
        specification (rest clause)
        :let [specification (if (sequential? specification) specification [specification])
              module (str (first specification))
              options (into {} (map vec (partition 2 (rest specification))))]]
    {:module module
     :string-module? (string? (first specification))
     :alias (some-> (:as options) str)
     :referred (into {} (for [export (:refer options)]
                          [(str (get (:rename options) export export)) (str export)]))}))

(defn namespace-facts
  "Decode namespace imports and live source references without inferring route ownership."
  [forms inspect-nodes]
  (let [bindings (require-bindings forms)
        namespace-aliases (into {} (keep #(when (:alias %) [(:alias %) (:module %)])) bindings)
        referred (into {} (mapcat (fn [{:keys [module referred]}]
                                   (map (fn [[local-name export]]
                                          [local-name {:module module :export export}]) referred))) bindings)
        references (->> forms (remove namespace-form?) inspect-nodes (filter symbol?))
        module-for (fn [reference]
                     (or (get namespace-aliases (namespace reference))
                         (get namespace-aliases (str reference))
                         (:module (get referred (str reference)))
                         (namespace reference)))
        bridge-module "@open-hax/knoxx-app-bridge"]
    {:namespace (some #(when (namespace-form? %) (str (second %))) forms)
     :module-imports (mapv :module (filter :string-module? bindings))
     :aliases namespace-aliases
     :referred-names (into {} (map (fn [[local-name value]] [local-name (:module value)])) referred)
     :bridge-referred-names (into {} (keep (fn [[local-name {:keys [module export]}]]
                                            (when (= bridge-module module) [local-name export]))) referred)
     :dependencies (set (keep module-for references))
     :bridge-exports (set (keep (fn [reference]
                                 (when (= bridge-module (module-for reference))
                                   (if (namespace reference)
                                     (name reference)
                                     (:export (get referred (str reference)))))) references))
     :unresolved-bridge? (boolean (some #(= bridge-module (get namespace-aliases (str %))) references))}))

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
