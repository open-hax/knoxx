(ns knoxx.frontend.components.ui-render-test
  "Written FIRST (TDD) — render contract for the shared hand-rolled Helix
  UI primitives (button/card/input) that replace `@open-hax/uxx`'s
  Button/Card/Input in migrated pages until uxx-helix lands natively."
  (:require ["react-dom/server" :as rds]
            [cljs.test :as t]
            [clojure.string :as str]
            [helix.core :as hx]
            [knoxx.frontend.components.ui :as ui]))

(defn- render [el] (rds/renderToStaticMarkup el))

(t/deftest button-variants-and-label
  (let [primary (render (hx/$ ui/button {:variant :primary} "Save"))
        ghost (render (hx/$ ui/button {:variant :ghost} "Cancel"))
        secondary (render (hx/$ ui/button {} "Plain"))]
    (t/is (str/includes? primary "Save"))
    (t/is (str/includes? primary "bg-cyan-600") "primary is the accent button")
    (t/is (str/includes? ghost "Cancel"))
    (t/is (not (str/includes? ghost "bg-cyan-600")) "ghost has no accent fill")
    (t/is (str/includes? secondary "Plain") "variant defaults to secondary")))

(t/deftest button-loading-disables-and-marks-busy
  (let [html (render (hx/$ ui/button {:loading true} "Create"))]
    (t/is (str/includes? html "disabled"))
    (t/is (str/includes? html "aria-busy=\"true\""))
    (t/is (str/includes? html "Create") "label stays visible while loading"))
  (t/testing "disabled prop also disables"
    (t/is (str/includes? (render (hx/$ ui/button {:disabled true} "X")) "disabled"))))

(t/deftest button-sizes
  (t/is (str/includes? (render (hx/$ ui/button {:size :sm} "S")) "text-xs"))
  (t/is (str/includes? (render (hx/$ ui/button {} "M")) "text-sm")))

(t/deftest card-renders-children-and-variants
  (let [elevated (render (hx/$ ui/card {:variant :elevated :padding :lg}
                            (hx/$ :span "card body")))
        default (render (hx/$ ui/card {} (hx/$ :span "plain")))]
    (t/is (str/includes? elevated "card body"))
    (t/is (str/includes? elevated "shadow") "elevated cards cast a shadow")
    (t/is (str/includes? elevated "p-6") "lg padding")
    (t/is (str/includes? default "plain"))
    (t/is (str/includes? default "p-4") "default padding is md")))

(t/deftest input-passes-through-props
  (let [html (render (hx/$ ui/input {:value "abc"
                                  :placeholder "my-garden-id"
                                  :disabled true
                                  :on-change (fn [_])}))]
    (t/is (str/includes? html "value=\"abc\""))
    (t/is (str/includes? html "placeholder=\"my-garden-id\""))
    (t/is (str/includes? html "disabled"))))
