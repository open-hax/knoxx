(ns knoxx.frontend.components.ui-interaction-test
  "Interaction contract for the shared UI primitives — isolates the
  handler chain through the defnc wrappers (button on-click, input
  on-change) under jsdom + RTL."
  (:require [cljs.test :refer [deftest is use-fixtures]]
            ["@testing-library/react" :as rtl]
            [helix.core :refer [$]]
            [knoxx.frontend.components.ui :as ui]))

;; jsdom globals come from the :test build's :prepend-js (they must exist
;; before react-dom's module load — see shadow-cljs.edn).

(use-fixtures :each {:after rtl/cleanup})

(deftest button-on-click-fires
  (let [clicks (atom 0)
        r (rtl/render ($ ui/button {:on-click #(swap! clicks inc)} "Go"))]
    (.click rtl/fireEvent (.getByRole r "button" #js {:name "Go"}))
    (is (= 1 @clicks))))

(deftest input-on-change-receives-value
  (let [seen (atom nil)
        r (rtl/render ($ ui/input {:value ""
                                   :placeholder "p"
                                   :on-change #(reset! seen (.. % -target -value))}))]
    (.change rtl/fireEvent (.getByPlaceholderText r "p")
             #js {:target #js {:value "typed"}})
    (is (= "typed" @seen))))
