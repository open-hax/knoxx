(ns knoxx.frontend.pages.translations.review-form-sync-test
  "Same-split form synchronization follows immutable review identity."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as t]
            [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.pages.translations.logic :as logic]
            [knoxx.frontend.pages.translations.review-form-sync :as sync]))

(t/use-fixtures :each
  {:after rtl/cleanup})

(hx/defnc form-sync-harness
  "Expose form state after immutable review identity changes."
  [{:keys [split]}]
  (let [[form set-form!] (hooks/use-state logic/default-label)]
    (sync/use-review-form-sync! split set-form!)
    (d/div
     (d/output {:data-testid "adequacy"} (:adequacy form))
     (d/output {:data-testid "fluency"} (:fluency form))
     (d/output {:data-testid "terminology"} (:terminology form))
     (d/output {:data-testid "risk"} (:risk form))
     (d/output {:data-testid "correction"} (:corrected_text form))
     (d/output {:data-testid "notes"} (:editor_notes form)))))

(defn- wait-for-value
  [^js rendered test-id expected]
  (rtl/waitFor
   (fn []
     (when-not (= expected (.-textContent (.getByTestId rendered test-id)))
       (throw (js/Error. (str "still waiting for " test-id " = " expected)))))))

(t/deftest ^:async same-split-external-review-rehydrates-correction-and-scores
  (let [initial {:id "split/0"
                 :split_id "split/0"
                 :resource_split true
                 :candidate_set_id "candidate-set/7"
                 :review_id "review/initial"
                 :adequacy "adequate"
                 :fluency "good"
                 :terminology "minor_errors"
                 :risk "sensitive"
                 :corrected_text "Initial correction"
                 :editor_notes "Initial note"}
        remote (merge initial
                      {:review_id "review/remote"
                       :adequacy "poor"
                       :fluency "adequate"
                       :terminology "major_errors"
                       :risk "safe"
                       :corrected_text "Remote correction"
                       :editor_notes "Remote note"})
        rendered (rtl/render (hx/$ form-sync-harness {:split initial}))]
    (await (wait-for-value rendered "correction" "Initial correction"))
    (.rerender rendered (hx/$ form-sync-harness {:split remote}))
    (await (wait-for-value rendered "correction" "Remote correction"))
    (t/is (= "poor" (.-textContent (.getByTestId rendered "adequacy"))))
    (t/is (= "adequate" (.-textContent (.getByTestId rendered "fluency"))))
    (t/is (= "major_errors"
           (.-textContent (.getByTestId rendered "terminology"))))
    (t/is (= "safe" (.-textContent (.getByTestId rendered "risk"))))
    (t/is (= "Remote note" (.-textContent (.getByTestId rendered "notes"))))))
