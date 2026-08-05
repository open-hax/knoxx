(ns open-hax.uxx.markup
  "Portable, server-safe markup nodes shared by Knoxx renderers.

   This namespace owns the data contract and safety rules, not a rendering
   runtime. Components are ordinary pure functions that return nodes."
  (:require [clojure.string :as str]))

(def ^:private tag-pattern #"^[A-Za-z][A-Za-z0-9:-]*$")
(def ^:private attribute-pattern #"^[A-Za-z_:][A-Za-z0-9_.:-]*$")
(def ^:private url-attributes #{"action" "formaction" "href" "poster" "src"})
(def ^:private allowed-url-schemes #{"http" "https" "mailto" "tel"})

(deftype TrustedHtml [value])

(defn trusted-html
  "Mark a string as reviewed HTML. This is the only raw-markup escape hatch."
  [value]
  (TrustedHtml. (str (or value ""))))

(defn trusted-html?
  [value]
  (instance? TrustedHtml value))

(defn trusted-value
  [value]
  (if (trusted-html? value)
    (.-value value)
    (throw (ex-info "Raw HTML requires a trusted-html value"
                    {:value value :type :uxx/unsafe-raw-html}))))

(defn raw-html
  "Create an explicit raw HTML node from a trusted-html value."
  [value]
  (when-not (trusted-html? value)
    (throw (ex-info "raw-html accepts only trusted-html values"
                    {:value value :type :uxx/unsafe-raw-html})))
  [:raw-html value])

(defn element
  [tag attrs & children]
  (into [tag attrs] children))

(defn fragment
  [& children]
  (into [:<>] children))

(defn tag-name
  [tag]
  (let [value (cond
                (keyword? tag) (name tag)
                (string? tag) tag
                :else nil)]
    (when-not (and value
                   (not (#{{"<>" "raw-html"}} value))
                   (re-matches tag-pattern value))
      (throw (ex-info "Invalid markup tag"
                      {:tag tag :type :uxx/invalid-tag})))
    value))

(defn attribute-name
  [attribute]
  (let [value (cond
                (keyword? attribute) (name attribute)
                (string? attribute) attribute
                :else nil)]
    (when-not (and value (re-matches attribute-pattern value))
      (throw (ex-info "Invalid markup attribute name"
                      {:attribute attribute :type :uxx/invalid-attribute})))
    value))

(defn event-attribute?
  [attribute]
  (str/starts-with? (str/lower-case (attribute-name attribute)) "on"))

(defn url-attribute?
  [attribute]
  (contains? url-attributes (str/lower-case (attribute-name attribute))))

(defn safe-url?
  "Allow relative URLs and a deliberately small set of explicit schemes.

   Protocol-relative URLs are rejected because their destination depends on the
   surrounding document. ASCII controls and spaces are removed only for scheme
   detection so spellings such as `java\nscript:` cannot bypass the policy."
  [value]
  (let [candidate (str/trim (str (or value "")))
        scheme-probe (-> candidate
                         (str/replace #"[\u0000-\u0020]+" "")
                         str/lower-case)
        scheme (second (re-find #"^([a-z][a-z0-9+.-]*):" scheme-probe))]
    (and (not (str/starts-with? scheme-probe "//"))
         (or (nil? scheme) (contains? allowed-url-schemes scheme)))))

(declare normalize-class-value)

(defn- class-tokens
  [value]
  (cond
    (or (nil? value) (false? value)) []
    (string? value) (remove str/blank? (str/split value #"\s+"))
    (keyword? value) [(name value)]
    (map? value) (mapcat (fn [[token enabled?]]
                           (when enabled? (class-tokens token)))
                         (sort-by (comp str key) value))
    (set? value) (mapcat class-tokens (sort-by str value))
    (sequential? value) (mapcat class-tokens value)
    (number? value) [(str value)]
    :else (throw (ex-info "Unsupported class value"
                          {:value value :type :uxx/invalid-class}))))

(defn normalize-class-value
  [value]
  (str/join " " (distinct (class-tokens value))))

(defn validate-attribute!
  [[attribute value]]
  (let [name (attribute-name attribute)]
    (when (event-attribute? name)
      (throw (ex-info "Portable markup rejects browser event attributes"
                      {:attribute attribute :type :uxx/event-attribute})))
    (when (fn? value)
      (throw (ex-info "Portable markup rejects function-valued attributes"
                      {:attribute attribute :type :uxx/function-attribute})))
    (when (and (url-attribute? name)
               (some? value)
               (not (false? value))
               (not (safe-url? value)))
      (throw (ex-info "Unsafe URL scheme in markup attribute"
                      {:attribute attribute :value value :type :uxx/unsafe-url})))
    true))

(defn- node-vector?
  [value]
  (and (vector? value)
       (or (keyword? (first value)) (string? (first value)))))

(declare validate-node!)

(defn- validate-element!
  [node]
  (when (< (count node) 2)
    (throw (ex-info "Element nodes require an attribute map"
                    {:node node :type :uxx/malformed-element})))
  (tag-name (first node))
  (when-not (map? (second node))
    (throw (ex-info "Element node attributes must be a map"
                    {:node node :type :uxx/malformed-attributes})))
  (doseq [attribute (second node)] (validate-attribute! attribute))
  (doseq [child (nnext node)] (validate-node! child))
  node)

(defn validate-node!
  "Validate one node or nested child sequence and return it unchanged."
  [node]
  (cond
    (or (nil? node) (false? node) (string? node) (number? node) (keyword? node)) node

    (and (node-vector? node) (= :<> (first node)))
    (do (doseq [child (next node)] (validate-node! child)) node)

    (and (node-vector? node) (= :raw-html (first node)))
    (do
      (when-not (= 2 (count node))
        (throw (ex-info "Raw HTML nodes contain exactly one trusted value"
                        {:node node :type :uxx/malformed-raw-html})))
      (trusted-value (second node))
      node)

    (node-vector? node) (validate-element! node)
    (sequential? node) (do (doseq [child node] (validate-node! child)) node)

    :else
    (throw (ex-info "Unsupported markup node"
                    {:node node :type :uxx/invalid-node}))))

(defn valid-node?
  [node]
  (try
    (validate-node! node)
    true
    (catch :default _ false)))

(defn element-node?
  [node]
  (and (node-vector? node)
       (not (#{:<> :raw-html} (first node)))))

(defn raw-html-node?
  [node]
  (and (node-vector? node) (= :raw-html (first node))))
