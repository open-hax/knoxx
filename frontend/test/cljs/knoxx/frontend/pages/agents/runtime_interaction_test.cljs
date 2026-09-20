(ns knoxx.frontend.pages.agents.runtime-interaction-test
  "Check capability controls and visible runtime facts after the workbench split."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as t]
            [helix.core :as hx]
            [knoxx.frontend.pages.agents.fields :as fields]
            [knoxx.frontend.pages.agents.runtime-view :as view]))

(t/use-fixtures :each {:after #(rtl/cleanup)})

(t/deftest sidebar-count-remains-the-provided-data-value
  (let [^js rendered (rtl/render (hx/$ view/sidebar-section-toggle {:title "Contracts" :count 7 :open? true :on-toggle (fn [])}))]
    (t/is (some? (.getByText rendered "7")))
    (t/is (some? (.getByRole rendered "button" #js {:name "Contracts 7 ▾"})))))

(t/deftest runtime-card-refuses-disabled-run-and-preserves-inspection-target
  (let [calls (atom []) job {:id "trigger-1" :name "Daily research" :enabled true :contractSourceKind "agent" :contractSourceId "research"}
        ^js rendered (rtl/render (hx/$ view/runtime-job-card {:job job :runtime {} :can-control false
                                                              :on-run #(swap! calls conj [:run %]) :on-inspect #(swap! calls conj [:inspect %])}))]
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "▶ Run"}))
    (t/is (empty? @calls))
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "🔎 EDN"}))
    (t/is (= [[:inspect "research"]] @calls))))

(t/deftest role-picker-removes-only-the-selected-role
  (let [calls (atom []) ^js rendered (rtl/render (hx/$ fields/role-picker {:roles ["writer" "reviewer"] :selected ["writer" "reviewer"]
                                                                         :on-change #(swap! calls conj %) :disabled false}))]
    (.click rtl/fireEvent (aget (.getAllByRole rendered "button" #js {:name "×"}) 0))
    (t/is (= [["reviewer"]] @calls))))
