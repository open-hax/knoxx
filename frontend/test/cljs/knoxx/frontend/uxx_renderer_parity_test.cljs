(ns knoxx.frontend.uxx-renderer-parity-test
  (:require ["react-dom/server" :as rds]
            [cljs.test :as t]
            [open-hax.uxx.markup :as markup]
            [open-hax.uxx.render.helix :as helix]
            [open-hax.uxx.render.html :as html]))

(def ^:private fixture
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

(t/deftest portable-fixture-has-HTML-React-parity
  (let [direct (html-shape (html/render fixture))
        react (html-shape (rds/renderToStaticMarkup (helix/render fixture)))]
    (t/is (= direct react))))

(t/deftest React-adapter-keeps-portable-safety-boundary
  (t/testing "browser event handlers are not smuggled through the portable AST"
    (t/is (thrown? js/Error (helix/render [:button {:onClick (fn [])} "bad"]))))
  (t/testing "URL policy is shared with the server renderer"
    (t/is (thrown? js/Error (helix/render [:a {:href "javascript:alert(1)"} "bad"]))))
  (t/testing "reviewed raw HTML is explicit and parent-owned"
    (let [node [:div {} (markup/raw-html (markup/trusted-html "<em>ok</em>"))]]
      (t/is (= "<div><em>ok</em></div>"
             (rds/renderToStaticMarkup (helix/render node)))))))
