(ns knoxx.backend.uxx-html-renderer-test
  (:require [cljs.test :refer [deftest is testing]]
            [open-hax.uxx.markup :as markup]
            [open-hax.uxx.render.html :as html]))

(deftest renders-deterministic-safe-html
  (is (= (str "<div class=\"beta alpha gamma\" disabled id=\"root\""
              " title=\"&quot;&amp;&lt;\">&lt;&amp;&gt;</div>")
         (html/render
          [:div {:title "\"&<"
                 :id "root"
                 :disabled true
                 :class ["beta" {:alpha true :hidden false} :gamma "beta"]}
           "<&>"]))))

(deftest renders-fragments-nested-sequences-and-void-elements
  (is (= "a<span>b</span><br><input name=\"empty\" value=\"\">"
         (html/render
          [:<> "a"
           [[:span {} "b"] nil false]
           [:br {}]
           [:input {:value "" :name "empty"}]]))))

(deftest rejects-children-on-void-elements
  (is (thrown? js/Error (html/render [:input {} "not allowed"]))))

(deftest URL-policy-is-explicit
  (testing "relative and reviewed external schemes render"
    (is (= "<a href=\"/inside\">inside</a>"
           (html/render [:a {:href "/inside"} "inside"])))
    (is (= "<a href=\"https://example.test/x\">outside</a>"
           (html/render [:a {:href "https://example.test/x"} "outside"])))
    (is (= "<a href=\"mailto:user@example.test\">mail</a>"
           (html/render [:a {:href "mailto:user@example.test"} "mail"]))))
  (testing "active-content and origin-relative spellings are refused"
    (is (thrown? js/Error (html/render [:a {:href "javascript:alert(1)"} "bad"])))
    (is (thrown? js/Error (html/render [:a {:href "java\nscript:alert(1)"} "bad"])))
    (is (thrown? js/Error (html/render [:a {:href "//other.example"} "bad"])))))

(deftest portable-attributes-cannot-smuggle-browser-code
  (is (thrown? js/Error (html/render [:button {:onClick "alert(1)"} "bad"])))
  (is (thrown? js/Error (html/render [:button {:on-click (fn [] nil)} "bad"]))))

(deftest raw-html-requires-an-explicit-trusted-value
  (is (= "<em>reviewed</em>"
         (html/render
          (markup/raw-html (markup/trusted-html "<em>reviewed</em>")))))
  (is (thrown? js/Error (html/render [:raw-html "<em>untrusted</em>"])))
  (is (thrown? js/Error (markup/raw-html "<em>untrusted</em>"))))

(deftest malformed-nodes-fail-loudly
  (is (thrown? js/Error (html/render [:div])))
  (is (thrown? js/Error (html/render [:div "not-attrs" "child"])))
  (is (thrown? js/Error (html/render [:raw-html])))
  (is (false? (markup/valid-node? #js {}))))
