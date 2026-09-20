(ns knoxx.frontend.pages.translations.live-interaction-test
  "Open translation reviews follow agent updates without losing unfinished human corrections."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as t]
            [helix.core :as hx]
            [knoxx.frontend.pages.translations.page-fixtures :as fixtures]
            [knoxx.frontend.pages.translations.view :as view]))

(t/use-fixtures :each fixtures/api-fixture)

(defn- changed! [document]
  (rtl/fireEvent js/window
                  (js/window.CustomEvent. "knoxx:publication-changed"
                                          #js {:detail #js {:document document}})))

(defn- change-text! [element value]
  (.change rtl/fireEvent element #js {:target #js {:value value}}))

(defn- ^:async open-editor! [^js rendered]
  (await (fixtures/select-first-document rendered))
  (.click rtl/fireEvent (.getByText rendered "Hola mundo"))
  (await (fixtures/wait-until "selected split" #(some? (.queryByText rendered "Split 0")))))

(defn- updated-inventory! [inventory]
  (swap! inventory update-in [0 :split_review :splits 0]
         assoc :review_id "review/agent-2" :corrected_text "Corrección del agente"
         :editor_notes "Agent terminology review"))

(t/deftest ^:async clean-open-review-follows-global-publication-refresh
  (let [inventory (atom [(fixtures/resource-review-row fixtures/resource-split-review)])]
    (fixtures/install-resource-split-inventory! inventory)
    (let [^js rendered (rtl/render (hx/$ view/translation-review-page))]
      (await (open-editor! rendered))
      (updated-inventory! inventory)
      (changed! nil)
      (await (fixtures/wait-until "agent correction hydrated"
                                 #(= "Corrección del agente" (.-value (.getByLabelText rendered "Corrected translation")))))
      (t/is (= "Agent terminology review" (.-value (.getByLabelText rendered "Editor notes"))))
      (t/is (nil? (.queryByRole rendered "button" #js {:name "Discard local review and load latest"}))))))

(t/deftest ^:async dirty-open-review-retains-original-input-and-rejects-stale-mutation
  (let [inventory (atom [(fixtures/resource-review-row fixtures/resource-split-review)])]
    (fixtures/install-resource-split-inventory! inventory)
    (let [^js rendered (rtl/render (hx/$ view/translation-review-page))]
      (await (open-editor! rendered))
      (change-text! (.getByLabelText rendered "Corrected translation") "Unfinished human correction")
      (updated-inventory! inventory)
      (changed! "docs/doc-1")
      (await (.findByRole rendered "button" #js {:name "Discard local review and load latest"}))
      (t/is (= "Unfinished human correction" (.-value (.getByLabelText rendered "Corrected translation"))))
      (let [approve (.getByRole rendered "button" #js {:name "Approve split"})]
        (t/is (.-disabled approve))
        (.click rtl/fireEvent approve)
        (t/is (empty? (:publication-split-review @fixtures/calls))))
      (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Discard local review and load latest"}))
      (await (fixtures/wait-until "latest correction deliberately loaded"
                                 #(= "Corrección del agente" (.-value (.getByLabelText rendered "Corrected translation")))))
      (t/is (not (.-disabled (.getByRole rendered "button" #js {:name "Approve split"})))))))
