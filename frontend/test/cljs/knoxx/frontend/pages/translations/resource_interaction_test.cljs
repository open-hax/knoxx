(ns knoxx.frontend.pages.translations.resource-interaction-test
  "Preserved translation workspace scenarios, using native async test control flow."
  (:require
    ["@testing-library/react" :as rtl]
    [cljs.test :as t]
    [helix.core :as hx]
    [knoxx.frontend.pages.translations.api :as api]
    [knoxx.frontend.pages.translations.page-fixtures :as fixtures]
    [knoxx.frontend.pages.translations.view :as view]))

(t/use-fixtures :each fixtures/api-fixture)

(t/deftest ^:async resource-split-corrected-approval-posts-and-unlocks-whole-output
  (let
    [inventory (atom [(fixtures/resource-review-row fixtures/resource-split-review)])]
    (fixtures/install-resource-split-inventory! inventory)
    (set! api/submit-publication-split-review
      (fn
        [payload]
        (fixtures/record! :publication-split-review payload)
        (swap! inventory update-in
          [0 :split_review :splits 0]
          assoc
          :review_status
          (:status payload)
          :corrected_text
          (:corrected_text payload))
        (js/Promise.resolve {:status "recorded"})))
    (let
      [r (rtl/render (hx/$ view/translation-review-page))]
      (await (fixtures/select-first-document r))
      (let
        [whole-button (.getByRole r "button" #js {:name "Approve whole output"})]
        (t/is (.-disabled whole-button) "whole-output approval waits for every split"))
      (.click rtl/fireEvent (.getByText r "Hola mundo"))
      (await
        (fixtures/wait-until
          "resource split editor"
          #(some? (.queryByRole r "button" #js {:name "Approve split"}))))
      (t/is
        (some? (.queryByRole r "button" #js {:name "Submit review"}))
        "the split can remain explicitly in review")
      (t/is (= "adequate" (.-value (.getByLabelText r "adequacy"))))
      (t/is (= "good" (.-value (.getByLabelText r "fluency"))))
      (t/is (= "minor_errors" (.-value (.getByLabelText r "terminology"))))
      (t/is (= "sensitive" (.-value (.getByLabelText r "risk"))))
      (t/is
        (= "Confirm product terminology" (.-value (.getByLabelText r "Editor notes")))
        "the latest granular review hydrates the resource split card")
      (.change rtl/fireEvent (.getByLabelText r "adequacy") #js {:target #js {:value "excellent"}})
      (.change rtl/fireEvent
        (.getByLabelText r "Corrected translation")
        #js
        {:target #js {:value "  Hola, mundo corregido.  "}})
      (.change rtl/fireEvent
        (.getByLabelText r "Editor notes")
        #js
        {:target #js {:value "  Approved after terminology pass.  "}})
      (.click rtl/fireEvent (.getByRole r "button" #js {:name "Approve split"}))
      (await (fixtures/wait-until "approved notice" #(some? (.queryByText r "Split 0 approved."))))
      (t/is
        (=
          [{:candidate_set_id "candidate-set/doc-1-es"
            :split_id "split/doc-1/0"
            :status "approved"
            :adequacy "excellent"
            :fluency "good"
            :terminology "minor_errors"
            :risk "sensitive"
            :corrected_text "  Hola, mundo corregido.  "
            :editor_notes "Approved after terminology pass."}]
          (:publication-split-review @fixtures/calls)))
      (await (fixtures/wait-until "all-split progress" #(seq (.queryAllByText r "2/2"))))
      (t/is
        (not (.-disabled (.getByRole r "button" #js {:name "Approve whole output"})))
        "whole-output action becomes available only after 2/2")
      (t/is (empty? (:get @fixtures/calls)) "resource splits never fall through to legacy Mongo detail"))))

(t/deftest ^:async resource-split-skip-advances-without-writing-review-evidence
  (let
    [inventory (atom [(fixtures/resource-review-row fixtures/resource-split-review)])]
    (fixtures/install-resource-split-inventory! inventory)
    (let
      [r (rtl/render (hx/$ view/translation-review-page))]
      (await (fixtures/select-first-document r))
      (.click rtl/fireEvent (.getByText r "Hola mundo"))
      (await (fixtures/wait-until "first resource split" #(some? (.queryByText r "Split 0"))))
      (.click rtl/fireEvent (.getByRole r "button" #js {:name "Skip"}))
      (await (fixtures/wait-until "next resource split" #(some? (.queryByText r "Split 1"))))
      (t/is (empty? (:publication-split-review @fixtures/calls)) "Skip changes only local selection")
      (t/is (empty? (:publication-bulk-review @fixtures/calls)) "Skip never substitutes a bulk verdict")
      (t/is
        (.-disabled (.getByRole r "button" #js {:name "Skip"}))
        "the final split does not wrap back to the beginning"))))

(t/deftest ^:async resource-document-review-fast-path-posts-one-candidate-set
  (let
    [inventory (atom [(fixtures/resource-review-row fixtures/resource-split-review)])]
    (fixtures/install-resource-split-inventory! inventory)
    (set! api/submit-publication-bulk-review
      (fn
        [payload]
        (fixtures/record! :publication-bulk-review payload)
        (swap! inventory update-in
          [0 :split_review :splits]
          #(mapv (fn [split] (assoc split :review_status (:status payload))) %))
        (js/Promise.resolve {:status "recorded"})))
    (let
      [r (rtl/render (hx/$ view/translation-review-page))]
      (await (fixtures/select-first-document r))
      (doseq
        [action ["Approve All" "Needs Edit" "Reject All"]]
        (t/is (some? (.queryByRole r "button" #js {:name action}))))
      (t/is
        (some? (.queryByRole r "button" #js {:name "Approve whole output"}))
        "publication approval remains a separate, gated action")
      (.click rtl/fireEvent (.getByText r "Hola mundo"))
      (await (fixtures/wait-until "resource evaluation form" #(some? (.queryByLabelText r "fluency"))))
      (.change rtl/fireEvent (.getByLabelText r "fluency") #js {:target #js {:value "poor"}})
      (.change rtl/fireEvent
        (.getByLabelText r "Corrected translation")
        #js
        {:target #js {:value "Must remain split-local"}})
      (.change rtl/fireEvent
        (.getByLabelText r "Editor notes")
        #js
        {:target #js {:value "  Apply to every persisted split.  "}})
      (.click rtl/fireEvent (.getByRole r "button" #js {:name "Needs Edit"}))
      (await (fixtures/wait-until "bulk review notice" #(some? (.queryByText r "All splits in-review."))))
      (t/is
        (=
          [{:candidate_set_id "candidate-set/doc-1-es"
            :status "in-review"
            :adequacy "adequate"
            :fluency "poor"
            :terminology "minor_errors"
            :risk "sensitive"
            :editor_notes "Apply to every persisted split."}]
          (:publication-bulk-review @fixtures/calls))
        "the server owns split enumeration and document scope cannot carry a correction")
      (t/is (empty? (:publication-split-review @fixtures/calls))))))

(t/deftest ^:async resource-split-rejection-is-granular-and-relocks-whole-output
  (let
    [approved-aggregate (update fixtures/resource-split-review :splits
       #(mapv (fn [split] (assoc split :review_status "approved")) %))
     inventory (atom [(fixtures/resource-review-row approved-aggregate)])]
    (fixtures/install-resource-split-inventory! inventory)
    (set! api/submit-publication-split-review
      (fn
        [payload]
        (fixtures/record! :publication-split-review payload)
        (swap! inventory update-in [0 :split_review :splits 0] assoc :review_status (:status payload))
        (js/Promise.resolve {:status "recorded"})))
    (let
      [r (rtl/render (hx/$ view/translation-review-page))]
      (await (fixtures/select-first-document r))
      (t/is (not (.-disabled (.getByRole r "button" #js {:name "Approve whole output"}))))
      (.click rtl/fireEvent (.getByText r "Hola mundo"))
      (await
        (fixtures/wait-until "reject control" #(some? (.queryByRole r "button" #js {:name "Reject split"}))))
      (.click rtl/fireEvent (.getByRole r "button" #js {:name "Reject split"}))
      (await (fixtures/wait-until "rejected notice" #(some? (.queryByText r "Split 0 rejected."))))
      (t/is
        (=
          [{:candidate_set_id "candidate-set/doc-1-es"
            :split_id "split/doc-1/0"
            :status "rejected"
            :adequacy "adequate"
            :fluency "good"
            :terminology "minor_errors"
            :risk "sensitive"
            :editor_notes "Confirm product terminology"}]
          (:publication-split-review @fixtures/calls)))
      (await (fixtures/wait-until "partial review projection" #(seq (.queryAllByText r "1/2"))))
      (t/is
        (.-disabled (.getByRole r "button" #js {:name "Approve whole output"}))
        "a rejected split relocks whole-output approval"))))
