(ns knoxx.frontend.infra.migration-html
  "Read authored HTML through a nonexecuting parser before admitting runtime inputs."
  (:require ["jsdom" :as jsdom]
            ["node:fs" :as fs]
            ["node:path" :as node-path]
            [clojure.string :as str]
            [knoxx.frontend.law.migration-html :as law]))

(def ^:private javascript-types
  #{"application/ecmascript" "application/javascript"
    "application/x-ecmascript" "application/x-javascript"
    "text/ecmascript" "text/javascript" "text/javascript1.0"
    "text/javascript1.1" "text/javascript1.2" "text/javascript1.3"
    "text/javascript1.4" "text/javascript1.5" "text/jscript"
    "text/livescript" "text/x-ecmascript" "text/x-javascript" "module"})

(defn- executable-script? [^js element]
  (let [script-type (.getAttribute element "type")
        language (.getAttribute element "language")
        effective-type (cond
                         (= "" script-type) "text/javascript"
                         (some? script-type) (str/trim script-type)
                         (str/blank? language) "text/javascript"
                         :else (str "text/" language))]
    (or (not= "http://www.w3.org/1999/xhtml" (.-namespaceURI element))
        (contains? javascript-types (str/lower-case effective-type)))))

(defn- normalized-url [value]
  (-> value
      (str/replace #"[\t\r\n]" "")
      (str/replace #"^[\x00-\x20]+" "")
      str/lower-case))

(defn- javascript-url? [value]
  (str/starts-with? (normalized-url value) "javascript:"))

(defn- data-url-facts [{:keys [value] :as attribute}]
  (when-let [[_ metadata] (re-find #"^data:([^,]*)," (normalized-url value))]
    (assoc attribute :mime-type (str/replace (first (str/split metadata #";" -1))
                                              #"^ +| +$" ""))))

(defn- html-facts [path elements attributes]
  {:path path
   :scripts (vec (for [^js element elements
                       :when (and (= "script" (str/lower-case (.-localName element)))
                                  (executable-script? element))]
                   {:source (.getAttribute element "src")
                    :html? (= "http://www.w3.org/1999/xhtml" (.-namespaceURI element))
                    :inline-code? (not (str/blank? (.-textContent element)))}))
   :handlers (vec (filter #(str/starts-with? (:name %) "on") attributes))
   :data-urls (vec (keep data-url-facts attributes))
   :script-urls (vec (filter #(and (or (contains? #{"href" "src" "action" "formaction" "xlink:href"}
                                                 (:name %))
                                      (and (= "object" (:element %)) (= "data" (:name %))))
                                  (javascript-url? (:value %))) attributes))
   :base-hrefs (vec (filter #(and (= "base" (:element %)) (= "href" (:name %)))
                            attributes))})

(defn assert-html!
  "Parse HTML with jsdom defaults: no script execution or resource loading."
  [path source]
  (let [dom (jsdom/JSDOM. source)
        window (.-window dom)]
    (try
      (let [elements (array-seq (.querySelectorAll (.-document window) "*"))
            attributes (for [^js element elements
                             ^js attribute (array-seq (.-attributes element))]
                         {:element (.-localName element)
                          :name (str/lower-case (.-name attribute))
                          :value (.-value attribute)})]
        (law/assert-entrypoint! (html-facts path elements attributes))
        (doseq [{:keys [element value] attribute-name :name} attributes
                :when (and (= "iframe" element) (= "srcdoc" attribute-name))]
          (assert-html! (str path "#srcdoc") value)))
      (finally (.close window)))))

(defn- public-html-files [root seen]
  (let [canonical (fs/realpathSync root)]
    (when-not (contains? seen canonical)
      (let [seen (conj seen canonical)]
        (mapcat (fn [entry]
                  (let [path (node-path/join root entry)]
                    (cond
                      (.isDirectory (fs/statSync path)) (public-html-files path seen)
                      (re-find #"\.html?$" (str/lower-case path)) [path]
                      :else [])))
                (sort (array-seq (fs/readdirSync root))))))))

(defn assert-entrypoints!
  "Inspect the Shadow/Vite HTML entry and copied public HTML, preserving generated JS."
  [root]
  (let [index (node-path/join root "frontend/index.html")
        public (node-path/join root "frontend/public")
        paths (concat (when (fs/existsSync index) [index])
                      (when (fs/existsSync public) (public-html-files public #{})))]
    (doseq [path paths]
      (assert-html! (node-path/relative root path) (fs/readFileSync path "utf8")))))
