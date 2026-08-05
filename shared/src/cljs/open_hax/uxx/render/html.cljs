(ns open-hax.uxx.render.html
  "Deterministic, React-free HTML rendering for the portable UXX markup AST."
  (:require [clojure.string :as str]
            [open-hax.uxx.markup :as markup]))

(def ^:private void-elements
  #{"area" "base" "br" "col" "embed" "hr" "img" "input" "link" "meta"
    "param" "source" "track" "wbr"})

(defn escape-text
  [value]
  (-> (str (or value ""))
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn escape-attribute
  [value]
  (-> (escape-text value)
      (str/replace "\"" "&quot;")
      (str/replace "'" "&#39;")))

(defn- normalized-attribute
  [[attribute value :as entry]]
  (markup/validate-attribute! entry)
  (let [name (markup/attribute-name attribute)
        value (if (= "class" name)
                (markup/normalize-class-value value)
                value)]
    [name value]))

(defn- render-attribute
  [[name value]]
  (cond
    (= "key" name) ""
    (or (nil? value) (false? value) (= "" value)) ""
    (true? value) (str " " name)
    :else (str " " name "=\"" (escape-attribute value) "\"")))

(defn- render-attributes
  [attrs]
  (->> attrs
       (map normalized-attribute)
       (sort-by first)
       (map render-attribute)
       (apply str)))

(declare render-node)

(defn- render-children
  [children]
  (apply str (map render-node children)))

(defn- render-element
  [node]
  (let [tag (markup/tag-name (first node))
        attrs (second node)
        children (nnext node)
        attributes (render-attributes attrs)]
    (if (contains? void-elements tag)
      (do
        (when (seq (remove #(or (nil? %) (false? %)) children))
          (throw (ex-info "Void HTML elements cannot contain children"
                          {:tag tag :type :uxx/void-element-children})))
        (str "<" tag attributes ">"))
      (str "<" tag attributes ">"
           (render-children children)
           "</" tag ">"))))

(defn- render-node
  [node]
  (cond
    (or (nil? node) (false? node)) ""
    (string? node) (escape-text node)
    (number? node) (str node)
    (keyword? node) (escape-text (name node))

    (and (vector? node) (= :<> (first node)))
    (render-children (next node))

    (markup/raw-html-node? node)
    (markup/trusted-value (second node))

    (markup/element-node? node)
    (render-element node)

    (sequential? node)
    (render-children node)

    :else
    (throw (ex-info "Unsupported markup node during HTML rendering"
                    {:node node :type :uxx/invalid-node}))))

(defn render
  "Validate and render one portable markup tree to HTML."
  [node]
  (markup/validate-node! node)
  (render-node node))
