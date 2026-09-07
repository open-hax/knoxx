(ns knoxx.frontend.infra.migration-html
  "Read authored browser documents without executing scripts or loading resources."
  (:require ["jsdom" :as jsdom]
            ["node:fs" :as fs]
            ["node:path" :as node-path]
            [clojure.string :as str]
            [knoxx.frontend.law.migration-html :as law]))

(def ^:private document-types
  {".html" "text/html" ".htm" "text/html" ".svg" "image/svg+xml"
   ".xml" "application/xml" ".xhtml" "application/xhtml+xml"})

(defn- browser-element? [^js element]
  (contains? #{"http://www.w3.org/1999/xhtml" "http://www.w3.org/2000/svg"
               "http://www.w3.org/1998/Math/MathML"}
             (.-namespaceURI element)))

(defn- browser-attribute-name [^js attribute]
  (case (.-namespaceURI attribute)
    nil (.-localName attribute)
    "http://www.w3.org/1999/xlink" (str "xlink:" (.-localName attribute))
    "http://www.w3.org/XML/1998/namespace" (str "xml:" (.-localName attribute))
    nil))

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
    (or (= "http://www.w3.org/2000/svg" (.-namespaceURI element))
        (and (= "http://www.w3.org/1999/xhtml" (.-namespaceURI element))
             (contains? javascript-types (str/lower-case effective-type))))))

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

(defn- refresh-target [content]
  ;; Shared declarative refresh grammar: decimal delay, optional URL=, and quotes.
  (when-let [[_ tail] (re-matches #"^[\t\n\f\r ]*[0-9.]+(?:[\t\n\f\r ]*[;,][\t\n\f\r ]*|[\t\n\f\r ]+)([\s\S]*)$"
                                 (or content ""))]
    (let [target (str/replace tail #"^[uU][rR][lL][\t\n\f\r ]*=[\t\n\f\r ]*" "")
          quote-delimiter (subs target 0 (min 1 (count target)))]
      (if (contains? #{"'" "\""} quote-delimiter)
        (subs target 1 (or (str/index-of target quote-delimiter 1) (count target)))
        target))))

(defn- refresh-data-url-facts [^js element]
  (when (and (= "http://www.w3.org/1999/xhtml" (.-namespaceURI element))
             (= "meta" (.-localName element))
             (= "refresh" (some-> (.getAttribute element "http-equiv") str/lower-case)))
    (when-let [target (refresh-target (.getAttribute element "content"))]
      (data-url-facts {:element "meta" :name :refresh-target :value target}))))

(defn- html-facts [path elements attributes]
  {:path path
   :scripts (vec (for [^js element elements
                       :when (and (= "script" (.-localName element))
                                  (executable-script? element))]
                   {:source (.getAttribute element "src")
                    :html? (= "http://www.w3.org/1999/xhtml" (.-namespaceURI element))
                    :inline-code? (not (str/blank? (.-textContent element)))}))
   :handlers (vec (filter #(str/starts-with? (:name %) "on") attributes))
   :data-urls (vec (concat (keep data-url-facts attributes)
                           (keep refresh-data-url-facts elements)))
   :script-urls (vec (filter #(and (or (contains? #{"href" "src" "action" "formaction" "xlink:href"}
                                                 (:name %))
                                      (and (= "object" (:element %)) (= "data" (:name %))))
                                  (javascript-url? (:value %))) attributes))
   :base-hrefs (vec (filter #(or (= "xml:base" (:name %))
                                (and (= "base" (:element %)) (= "href" (:name %))))
                            attributes))})

(defn assert-html!
  "Parse HTML or XML without script execution or resource loading."
  ([path source] (assert-html! path source "text/html"))
  ([path source content-type]
  (let [dom (jsdom/JSDOM. source #js {:contentType content-type})
        window (.-window dom)]
    (try
      (let [elements (filter browser-element?
                             (array-seq (.querySelectorAll (.-document window) "*")))
            attributes (for [^js element elements
                             ^js attribute (array-seq (.-attributes element))
                             :let [attribute-key (browser-attribute-name attribute)]
                             :when attribute-key]
                         {:element (.-localName element)
                          :name attribute-key
                          :value (.-value attribute)})]
        (law/assert-entrypoint! (html-facts path elements attributes))
        (doseq [{:keys [element value] attribute-name :name} attributes
                :when (and (= "iframe" element) (= "srcdoc" attribute-name))]
          (assert-html! (str path "#srcdoc") value)))
      (finally (.close window))))))

(defn- public-document-files [root seen]
  (let [canonical (fs/realpathSync root)]
    (when-not (contains? seen canonical)
      (let [seen (conj seen canonical)]
        (mapcat (fn [entry]
                  (let [path (node-path/join root entry)]
                    (cond
                      (.isDirectory (fs/statSync path)) (public-document-files path seen)
                      (contains? document-types (str/lower-case (node-path/extname path))) [path]
                      :else [])))
                (sort (array-seq (fs/readdirSync root))))))))

(defn assert-entrypoints!
  "Inspect the HTML entry and copied public browser documents, preserving generated JS."
  [root]
  (let [index (node-path/join root "frontend/index.html")
        public (node-path/join root "frontend/public")
        paths (concat (when (fs/existsSync index) [index])
                      (when (fs/existsSync public) (public-document-files public #{})))]
    (doseq [path paths]
      (assert-html! (node-path/relative root path) (fs/readFileSync path "utf8")
                    (get document-types (str/lower-case (node-path/extname path)))))))
