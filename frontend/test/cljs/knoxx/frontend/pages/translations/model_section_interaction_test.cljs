(ns knoxx.frontend.pages.translations.model-section-interaction-test
  "Exercise model selection despite catalog failure, refused saves and read-only roles."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as t]
            [helix.core :as hx]
            [knoxx.frontend.pages.translations.api :as api]
            [knoxx.frontend.pages.translations.model-section :as model]))

(def ^:private previous-config api/pipeline-config)
(def ^:private previous-models api/list-proxx-models)
(def ^:private previous-update api/update-pipeline-config)
(def ^:private updates (atom []))
(def ^:private refuse-save? (atom false))

(t/use-fixtures :each
  {:before (fn []
             (reset! updates [])
             (reset! refuse-save? false)
             (set! api/pipeline-config #(js/Promise.resolve {:model "current-model"}))
             (set! api/list-proxx-models #(js/Promise.reject (js/Error. "Catalog unavailable")))
             (set! api/update-pipeline-config
                   (fn [selected]
                     (swap! updates conj selected)
                     (if @refuse-save?
                       (js/Promise.reject (js/Error. "Model change refused"))
                       (js/Promise.resolve {:model selected :updated_at "now"})))))
   :after (fn []
            (rtl/cleanup)
            (set! api/pipeline-config previous-config)
            (set! api/list-proxx-models previous-models)
            (set! api/update-pipeline-config previous-update))})

(t/deftest ^:async catalog-failure-keeps-current-model-editable
  (let [rendered (rtl/render (hx/$ model/model-section {:can-manage true}))
        input (await (.findByLabelText rendered "Translation model"))]
    (t/is (= "current-model" (.-value input)))
    (.change rtl/fireEvent input #js {:target #js {:value " selected-model "}})
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Save"}))
    (await (.findByText rendered "Translation model updated to selected-model."))
    (t/is (= ["selected-model"] @updates))
    (t/is (= "selected-model" (.-value input)))))

(t/deftest ^:async save-refusal-keeps-draft-and-releases-controls
  (reset! refuse-save? true)
  (let [rendered (rtl/render (hx/$ model/model-section {:can-manage true}))
        input (await (.findByLabelText rendered "Translation model"))]
    (.change rtl/fireEvent input #js {:target #js {:value "draft-model"}})
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Save"}))
    (await (.findByText rendered "Model change refused"))
    (t/is (= "draft-model" (.-value input)))
    (t/is (not (.-disabled (.getByRole rendered "button" #js {:name "Save"}))))
    (t/is (= ["draft-model"] @updates))))

(t/deftest ^:async read-only-role-cannot-edit-or-save
  (let [rendered (rtl/render (hx/$ model/model-section {:can-manage false}))
        input (await (.findByLabelText rendered "Translation model"))
        save (.getByRole rendered "button" #js {:name "Save"})]
    (t/is (.-disabled input))
    (t/is (.-disabled save))
    (.click rtl/fireEvent save)
    (t/is (empty? @updates))))
