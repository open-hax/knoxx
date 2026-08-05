(ns open-hax.uxx.render.helix
  "React/Helix adapter for the portable UXX markup AST.

   Interactive components may wrap this output, but event handlers are not part
   of the portable subset and are rejected by the shared attribute contract."
  (:require [clojure.string :as str]
            [open-hax.uxx.markup :as markup]
            ["react" :as react]))

(def ^:private react-attribute-names
  {"accept-charset" "acceptCharset"
   "autocomplete" "autoComplete"
   "autofocus" "autoFocus"
   "charset" "charSet"
   "class" "className"
   "colspan" "colSpan"
   "crossorigin" "crossOrigin"
   "for" "htmlFor"
   "formaction" "formAction"
   "http-equiv" "httpEquiv"
   "maxlength" "maxLength"
   "readonly" "readOnly"
   "referrerpolicy" "referrerPolicy"
   "rowspan" "rowSpan"
   "srcset" "srcSet"
   "tabindex" "tabIndex"})

(defn- react-attribute-name
  [name]
  (or (get react-attribute-names name) name))

(defn- normalized-value
  [name value]
  (cond
    (= "class" name) (markup/normalize-class-value value)
    (and (= "style" name) (map? value)) (clj->js value)
    :else value))

(defn- props-js
  [attrs]
  (let [props (js-obj)]
    (doseq [[attribute value :as entry] attrs]
      (markup/validate-attribute! entry)
      (let [name (markup/attribute-name attribute)
            value (normalized-value name value)]
        (when-not (or (nil? value)
                      (and (= "class" name) (str/blank? value)))
          (aset props (react-attribute-name name) value))))
    props))

(defn- explicit-key?
  [node]
  (and (markup/element-node? node)
       (let [attrs (second node)]
         (or (contains? attrs :key) (contains? attrs "key")))))

(declare render-node)

(defn- render-sequence
  [nodes]
  (let [nodes (vec nodes)]
    (doseq [node nodes
            :when (and (markup/element-node? node) (not (explicit-key? node)))]
      (.warn js/console "Portable markup sibling element has no :key" node))
    (to-array (map render-node nodes))))

(defn- create-element
  [tag props children]
  (.apply (.-createElement react)
          react
          (to-array (concat [tag props] children))))

(defn- render-element
  [node]
  (let [tag (markup/tag-name (first node))
        attrs (second node)
        children (vec (nnext node))
        raw-child (when (and (= 1 (count children))
                             (markup/raw-html-node? (first children)))
                    (first children))
        props (props-js attrs)]
    (if raw-child
      (do
        (aset props "dangerouslySetInnerHTML"
              #js {:__html (markup/trusted-value (second raw-child))})
        (create-element tag props []))
      (create-element tag props (map render-node children)))))

(defn- render-node
  [node]
  (cond
    (or (nil? node) (false? node)) nil
    (string? node) node
    (number? node) node
    (keyword? node) (name node)

    (and (vector? node) (= :<> (first node)))
    (create-element (.-Fragment react) nil (map render-node (next node)))

    (markup/raw-html-node? node)
    (throw (ex-info "React raw HTML must be the sole child of an element"
                    {:node node :type :uxx/raw-html-needs-parent}))

    (markup/element-node? node)
    (render-element node)

    (sequential? node)
    (render-sequence node)

    :else
    (throw (ex-info "Unsupported markup node during React rendering"
                    {:node node :type :uxx/invalid-node}))))

(defn render
  "Validate and convert one portable markup tree into React elements."
  [node]
  (markup/validate-node! node)
  (render-node node))
