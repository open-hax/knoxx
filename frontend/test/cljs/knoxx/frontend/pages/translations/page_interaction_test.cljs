(ns knoxx.frontend.pages.translations.page-interaction-test
  "Preserved translation workspace scenarios, using native async test control flow."
  (:require
    ["@testing-library/react" :as rtl]
    [cljs.test :as t]
    [helix.core :as hx]
    [knoxx.frontend.pages.translations.api :as api]
    [knoxx.frontend.pages.translations.page-fixtures :as fixtures]
    [knoxx.frontend.pages.translations.view :as view]))

(t/use-fixtures :each fixtures/api-fixture)

(t/deftest ^:async loads-documents-and-detail-on-selection
  (let
    [r (rtl/render (hx/$ view/translation-review-page))]
    (try
      (await (fixtures/select-first-document r))
      (t/is (= {:project "devel" :target-lang ""} (first (:list @fixtures/calls))))
      (t/is (= "devel" (first (:manifest @fixtures/calls))))
      (t/is (= [["docs/doc-1" "es" {:project "devel" :garden-id "gardens/sonic"}]] (:get @fixtures/calls)))
      (t/is (some? (.queryByText r "Hello world")) "segment source shown")
      (catch :default err (t/is false (str "unexpected: " err))))))

(t/deftest ^:async segment-label-submit-posts-payload-and-reloads
  (let
    [r (rtl/render (hx/$ view/translation-review-page))]
    (try
      (await (fixtures/select-first-document r))
      (.click rtl/fireEvent (.getByText r "Hola mundo"))
      (await (fixtures/wait-until "panel" #(some? (.queryByText r "Segment 0"))))
      (.click rtl/fireEvent (.getByRole r "button" #js {:name "Submit review"}))
      (await (fixtures/wait-until "notice" #(some? (.queryByText r "Segment 0: approve"))))
      (let
        [[seg-id scope payload] (first (:label @fixtures/calls))]
        (t/is (= "seg-a" seg-id))
        (t/is (= {:project "devel" :garden-id "gardens/sonic"} scope))
        (t/is (= "approve" (:overall payload)))
        (t/is (not (contains? payload :corrected_text)) "blank correction omitted"))
      (t/is (= 2 (count (:get @fixtures/calls))) "detail reloaded after label")
      (t/is (= 2 (count (:list @fixtures/calls))) "list reloaded after label")
      (catch :default err (t/is false (str "unexpected: " err))))))

(t/deftest ^:async changing-segments-hydrates-form-without-correction-leakage
  (let
    [detail (-> fixtures/doc-detail
       (assoc-in [:segments 0 :labels] [])
       (assoc-in
         [:segments 1 :labels]
         [{:id "label-b"
           :adequacy "adequate"
           :overall "needs_edit"
           :corrected_text "Persisted B correction"
           :editor_notes "Persisted B note"}]))]
    (set! api/get-document (fn ([_ _] (js/Promise.resolve detail)) ([_ _ _] (js/Promise.resolve detail))))
    (let
      [r (rtl/render (hx/$ view/translation-review-page))]
      (try
        (await (fixtures/select-first-document r))
        (.click rtl/fireEvent (.getByText r "Hola mundo"))
        (await
          (fixtures/wait-until "first segment form" #(some? (.queryByLabelText r "Corrected translation"))))
        (.change rtl/fireEvent
          (.getByLabelText r "Corrected translation")
          #js
          {:target #js {:value "Draft for segment A"}})
        (t/is (= "Draft for segment A" (.-value (.getByLabelText r "Corrected translation"))))
        (let
          [confirm (.-confirm js/window)]
          (set! (.-confirm js/window) (constantly true))
          (try (.click rtl/fireEvent (.getByText r "Adiós")) (finally (set! (.-confirm js/window) confirm))))
        (await
          (fixtures/wait-until
            "second segment form hydration"
            #(= "Persisted B correction" (.-value (.getByLabelText r "Corrected translation")))))
        (t/is (= "Persisted B note" (.-value (.getByLabelText r "Editor notes"))))
        (t/is (= "adequate" (.-value (.getByLabelText r "adequacy"))))
        (catch :default err (t/is false (str "unexpected: " err)))))))

(t/deftest ^:async document-level-review-approves-all
  (let
    [r (rtl/render (hx/$ view/translation-review-page))]
    (try
      (await (fixtures/select-first-document r))
      (.click rtl/fireEvent (.getByRole r "button" #js {:name "Approve All"}))
      (await (fixtures/wait-until "notice" #(some? (.queryByText r "Document review: approve (2 segments)"))))
      (t/is
        (=
          [["docs/doc-1" "es" {:project "devel" :garden-id "gardens/sonic"} {:overall "approve"}]]
          (:review @fixtures/calls)))
      (catch :default err (t/is false (str "unexpected: " err))))))

(t/deftest ^:async pipeline-model-config-saves-patch
  (let
    [r (rtl/render (hx/$ view/translation-review-page))]
    (try
      (await (fixtures/wait-until "page" #(some? (.queryByText r "Translation Review"))))
      (.click rtl/fireEvent (.getByRole r "button" #js {:name "⚙ Pipeline"}))
      (await (fixtures/wait-until "config loaded" #(seq (:config @fixtures/calls))))
      (await (fixtures/wait-until "input ready" #(some? (.queryByPlaceholderText r "glm-5"))))
      (.change rtl/fireEvent (.getByPlaceholderText r "glm-5") #js {:target #js {:value "gpt-5.4"}})
      (.click rtl/fireEvent (.getByRole r "button" #js {:name "Save"}))
      (await
        (fixtures/wait-until "saved notice" #(some? (.queryByText r "Translation model updated to gpt-5.4."))))
      (t/is (= ["gpt-5.4"] (:update-config @fixtures/calls)))
      (t/is (= [true] (:models @fixtures/calls)) "proxx models listed for datalist")
      (catch :default err (t/is false (str "unexpected: " err))))))
