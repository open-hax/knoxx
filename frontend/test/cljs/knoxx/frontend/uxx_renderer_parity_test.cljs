(ns knoxx.frontend.uxx-renderer-parity-test
  (:require [cljs.test :refer [deftest is testing]]
            [open-hax.uxx.markup :as markup]
            [open-hax.uxx.render.helix :as helix]
            [open-hax.uxx.render.html :as html]
            ["react-dom/server" :as rds]))

(def fixture
  [:section {:id "fixture" :class ["shell" {:active true}]}
   [:h1 {} "Shared <markup>"]
   [:<> [:p {:key "first"} "one"]
    [[:p {:key "second"} "two"]]]
   [:label {:for "query"} "Query"]
   [:input {:id "query" :name "query" :type "text" :disabled true}]
   [:a {:href "/inside"} "Continue"]])

(defn- attributes-map
  [node]
  (into (sorted-map)
        (map (fn [attribute]
               [(.-name attribute) (.-value attribute)]))
        (array-seq (.-attributes node))))

(declare node-shape)

(defn- child-shapes
  [node]
  (->> (array-seq (.-childNodes node))
       (map node-shape)
       (remove nil?)
       vec))

(defn- node-shape
  [node]
  (case (.-nodeType node)
    1 {:tag (.-localName node)
       :attrs (attributes-map node)
       :children (child-shapes node)}
    3 (.-nodeValue node)
    11 (child-shapes node)
    nil))

(defn- html-shape
  [source]
  (let [template (.createElement js/document "template")]
    (set! (.-innerHTML template) source)
    (node-shape (.-content template))))

(deftest portable-fixture-has-HTML-React-parity
  (let [direct (html-shape (html/render fixture))
        react (html-shape (rds/renderToStaticMarkup (helix/render fixture)))]
    (is (= direct react))))

(deftest React-adapter-keeps-portable-safety-boundary
  (testing "browser event handlers are not smuggled through the portable AST"
    (is (thrown? js/Error (helix/render [:button {:onClick (fn [])} "bad"]))))
  (testing "URL policy is shared with the server renderer"
    (is (thrown? js/Error (helix/render [:a {:href "javascript:alert(1)"} "bad"]))))
  (testing "reviewed raw HTML is explicit and parent-owned"
    (let [node [:div {} (markup/raw-html (markup/trusted-html "<em>ok</em>"))]]
      (is (= "<div><em>ok</em></div>"
             (rds/renderToStaticMarkup (helix/render node)))))))
