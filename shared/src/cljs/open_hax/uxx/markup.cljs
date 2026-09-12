(ns open-hax.uxx.markup
  "Portable markup nodes and shared safety rules."
  (:require [clojure.string :as str]))

(def ^:private tag-pattern #"^[A-Za-z][A-Za-z0-9:-]*$")
(def ^:private attribute-pattern #"^[A-Za-z_:][A-Za-z0-9_.:-]*$")
(def ^:private reserved-tags #{"<>" "raw-html"})
(def ^:private url-attributes #{"action" "formaction" "href" "poster" "src"})
(def ^:private allowed-url-schemes #{"http" "https" "mailto" "tel"})

(deftype TrustedHtml [value])

(defn trusted-html
  "Mark caller-reviewed text as trusted raw markup; this does not sanitize it."
  [value]
  (TrustedHtml. (str (or value ""))))

(defn trusted-html?
  "Check whether a value explicitly carries the trusted-markup wrapper."
  [value]
  (instance? TrustedHtml value))

(defn trusted-value
  "Read explicitly trusted text or refuse unwrapped raw markup."
  [value]
  (if (trusted-html? value)
    (.-value value)
    (throw (ex-info "Raw markup requires an explicit trusted value"
                    {:type :uxx/unsafe-raw-markup}))))

(defn raw-html
  "Construct a raw node only from an explicitly trusted value."
  [value]
  (when-not (trusted-html? value)
    (throw (ex-info "raw-html accepts only trusted values"
                    {:type :uxx/unsafe-raw-markup})))
  [:raw-html value])

(defn element
  "Construct a portable element node with its attributes and children."
  [tag attrs & children]
  (into [tag attrs] children))

(defn fragment
  "Group portable children without adding an element wrapper."
  [& children]
  (into [:<>] children))

(defn tag-name
  "Validate and normalize a tag, rejecting reserved portable node markers."
  [tag]
  (let [value (cond
                (keyword? tag) (name tag)
                (string? tag) tag
                :else nil)]
    (when-not (and value
                   (not (contains? reserved-tags value))
                   (re-matches tag-pattern value))
      (throw (ex-info "Invalid markup tag"
                      {:tag tag :type :uxx/invalid-tag})))
    value))

(defn attribute-name
  "Validate and normalize a portable attribute identifier."
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
  "Identify case-insensitive event handler attribute names."
  [attribute]
  (str/starts-with? (str/lower-case (attribute-name attribute)) "on"))

(defn url-attribute?
  "Identify attributes whose values must satisfy the URL safety contract."
  [attribute]
  (contains? url-attributes (str/lower-case (attribute-name attribute))))

(defn safe-url?
  "Accept relative URLs or approved schemes, rejecting protocol-relative URLs."
  [value]
  (let [candidate (str/trim (str (or value "")))
        scheme-probe (-> candidate
                         (str/replace #"[\u0000-\u0020]+" "")
                         str/lower-case)
        scheme (second (re-find #"^([a-z][a-z0-9+.-]*):" scheme-probe))]
    (and (not (str/starts-with? scheme-probe "//"))
         (or (nil? scheme) (contains? allowed-url-schemes scheme)))))

(defn- class-tokens [value]
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
  "Flatten supported class values into stable, deduplicated tokens."
  [value]
  (str/join " " (distinct (class-tokens value))))

(defn validate-attribute!
  "Reject event handlers, functions and unsafe URL attribute values."
  [[attribute value]]
  (let [attr-name (attribute-name attribute)]
    (when (event-attribute? attr-name)
      (throw (ex-info "Portable markup rejects event attributes"
                      {:attribute attribute :type :uxx/event-attribute})))
    (when (fn? value)
      (throw (ex-info "Portable markup rejects function attributes"
                      {:attribute attribute :type :uxx/function-attribute})))
    (when (and (url-attribute? attr-name)
               (some? value)
               (not (false? value))
               (not (safe-url? value)))
      (throw (ex-info "Unsafe URL scheme"
                      {:attribute attribute :type :uxx/unsafe-url})))
    true))

(defn- node-vector? [value]
  (and (vector? value)
       (or (keyword? (first value)) (string? (first value)))))

(declare validate-node!)

(defn- validate-element! [node]
  (when (< (count node) 2)
    (throw (ex-info "Element nodes require attributes"
                    {:node node :type :uxx/malformed-element})))
  (tag-name (first node))
  (when-not (map? (second node))
    (throw (ex-info "Element attributes must be a map"
                    {:node node :type :uxx/malformed-attributes})))
  (doseq [attribute (second node)] (validate-attribute! attribute))
  (doseq [child (nnext node)] (validate-node! child))
  node)

(defn validate-node!
  "Validate a complete portable tree, returning the original valid node."
  [node]
  (cond
    (or (nil? node) (false? node) (string? node) (number? node) (keyword? node)) node
    (and (node-vector? node) (= :<> (first node)))
    (do (doseq [child (next node)] (validate-node! child)) node)
    (and (node-vector? node) (= :raw-html (first node)))
    (do
      (when-not (= 2 (count node))
        (throw (ex-info "Raw nodes contain one trusted value"
                        {:node node :type :uxx/malformed-raw-markup})))
      (trusted-value (second node))
      node)
    (node-vector? node) (validate-element! node)
    (sequential? node) (do (doseq [child node] (validate-node! child)) node)
    :else (throw (ex-info "Unsupported markup node"
                          {:node node :type :uxx/invalid-node}))))

(defn valid-node?
  "Check portable node validity without exposing validation exceptions."
  [node]
  (try
    (validate-node! node)
    true
    (catch :default _ false)))

(defn element-node?
  "Identify a tagged element rather than a fragment or raw node."
  [node]
  (and (node-vector? node)
       (not (#{:<> :raw-html} (first node)))))

(defn raw-html-node?
  "Identify a raw-markup node requiring an explicit trusted value."
  [node]
  (and (node-vector? node) (= :raw-html (first node))))
