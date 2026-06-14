(ns knoxx.frontend.components.ui-render-test
  "Written FIRST (TDD) — render contract for the shared hand-rolled Helix
  UI primitives (button/card/input) that replace `@open-hax/uxx`'s
  Button/Card/Input in migrated pages until uxx-helix lands natively."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            ["react-dom/server" :as rds]
            [helix.core :refer [$]]
            [knoxx.frontend.components.ui :as ui]))

(defn render [el] (rds/renderToStaticMarkup el))

(deftest button-variants-and-label
  (let [primary (render ($ ui/button {:variant :primary} "Save"))
        ghost (render ($ ui/button {:variant :ghost} "Cancel"))
        secondary (render ($ ui/button {} "Plain"))]
    (is (str/includes? primary "Save"))
    (is (str/includes? primary "bg-cyan-600") "primary is the accent button")
    (is (str/includes? ghost "Cancel"))
    (is (not (str/includes? ghost "bg-cyan-600")) "ghost has no accent fill")
    (is (str/includes? secondary "Plain") "variant defaults to secondary")))

(deftest button-loading-disables-and-marks-busy
  (let [html (render ($ ui/button {:loading true} "Create"))]
    (is (str/includes? html "disabled"))
    (is (str/includes? html "aria-busy=\"true\""))
    (is (str/includes? html "Create") "label stays visible while loading"))
  (testing "disabled prop also disables"
    (is (str/includes? (render ($ ui/button {:disabled true} "X")) "disabled"))))

(deftest button-sizes
  (is (str/includes? (render ($ ui/button {:size :sm} "S")) "text-xs"))
  (is (str/includes? (render ($ ui/button {} "M")) "text-sm")))

(deftest card-renders-children-and-variants
  (let [elevated (render ($ ui/card {:variant :elevated :padding :lg}
                            ($ :span "card body")))
        default (render ($ ui/card {} ($ :span "plain")))]
    (is (str/includes? elevated "card body"))
    (is (str/includes? elevated "shadow") "elevated cards cast a shadow")
    (is (str/includes? elevated "p-6") "lg padding")
    (is (str/includes? default "plain"))
    (is (str/includes? default "p-4") "default padding is md")))

(deftest input-passes-through-props
  (let [html (render ($ ui/input {:value "abc"
                                  :placeholder "my-garden-id"
                                  :disabled true
                                  :on-change (fn [_])}))]
    (is (str/includes? html "value=\"abc\""))
    (is (str/includes? html "placeholder=\"my-garden-id\""))
    (is (str/includes? html "disabled"))))
