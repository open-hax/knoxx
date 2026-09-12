(ns knoxx.frontend.pages.translations.concurrency-interaction-test
  "Preserved translation workspace scenarios, using native async test control flow."
  (:require
    ["@testing-library/react" :as rtl]
    [cljs.test :as t]
    [helix.core :as hx]
    [knoxx.frontend.pages.translations.api :as api]
    [knoxx.frontend.pages.translations.page-fixtures :as fixtures]
    [knoxx.frontend.pages.translations.view :as view]))

(t/use-fixtures :each fixtures/api-fixture)

(t/deftest ^:async stale-project-list-and-manifest-responses-cannot-overwrite-newer-state
  (let
    [old-documents (fixtures/deferred)
     fresh-documents (fixtures/deferred)
     old-manifest (fixtures/deferred)
     fresh-manifest (fixtures/deferred)]
    (set! api/list-publication-reviews (fn [] (js/Promise.resolve {:reviews []})))
    (set! api/list-documents
      (fn ^:async respond
        [{:keys [project] :as params}]
        (fixtures/record! :list params)
        (let
          [pending (if (= "fresh" project) fresh-documents old-documents) response (await (:promise pending))]
          (fixtures/record! :document-response project)
          response)))
    (set! api/get-manifest
      (fn ^:async respond
        [project]
        (fixtures/record! :manifest project)
        (let
          [pending (if (= "fresh" project) fresh-manifest old-manifest) response (await (:promise pending))]
          (fixtures/record! :manifest-response project)
          response)))
    (let
      [r (rtl/render (hx/$ view/translation-review-page))]
      (try
        (await
          (fixtures/wait-until
            "initial project requests"
            #(and (= 1 (count (:list @fixtures/calls))) (= 1 (count (:manifest @fixtures/calls))))))
        (.change rtl/fireEvent (.getByPlaceholderText r "devel") #js {:target #js {:value "fresh"}})
        (await
          (fixtures/wait-until
            "fresh project requests"
            #(and (some (fn [params] (= "fresh" (:project params))) (:list @fixtures/calls)) (some #{"fresh"} (:manifest @fixtures/calls)))))
        ((:resolve fresh-documents)
          {:documents [{:document_id "docs/fresh"
             :target_lang "fr"
             :source_lang "en"
             :garden_id "gardens/sonic"
             :project "fresh"
             :title "Fresh Project Doc"
             :total_segments 1
             :approved 0
             :overall_status "pending_review"}]
           :total 1})
        ((:resolve fresh-manifest) {:languages {:fr {:approved 0 :total_segments 1}}})
        (await
          (fixtures/wait-until
            "fresh project state"
            #(and (some? (.queryByText r "Fresh Project Doc")) (some? (.queryByRole r "option" #js {:name "Français"})))))
        ((:resolve old-documents)
          {:documents [{:document_id "docs/stale"
             :target_lang "es"
             :source_lang "en"
             :garden_id "gardens/sonic"
             :project "devel"
             :title "Stale Project Doc"
             :total_segments 1
             :approved 0
             :overall_status "pending_review"}]
           :total 1})
        ((:resolve old-manifest) {:languages {:es {:approved 1 :total_segments 1}}})
        (await
          (fixtures/wait-until
            "stale responses settled"
            #(and (= #{"devel" "fresh"} (set (:document-response @fixtures/calls))) (= #{"devel" "fresh"} (set (:manifest-response @fixtures/calls))))))
        (t/is (some? (.queryByText r "Fresh Project Doc")))
        (t/is (nil? (.queryByText r "Stale Project Doc")))
        (t/is (some? (.queryByRole r "option" #js {:name "Français"})))
        (t/is (nil? (.queryByRole r "option" #js {:name "Español"})))
        (catch :default err (t/is false (str "unexpected: " err)))))))

(t/deftest ^:async stale-language-list-response-cannot-repopulate-the-old-filter
  (let
    [all-documents (fixtures/deferred) french-documents (fixtures/deferred)]
    (set! api/list-publication-reviews (fn [] (js/Promise.resolve {:project "devel" :reviews []})))
    (set! api/list-documents
      (fn ^:async respond
        [{:keys [target-lang] :as params}]
        (fixtures/record! :list params)
        (let
          [pending (if (= "fr" target-lang) french-documents all-documents)
           response (await (:promise pending))]
          (fixtures/record! :language-response target-lang)
          response)))
    (set! api/get-manifest
      (fn
        [_]
        (js/Promise.resolve
          {:languages {:es {:approved 0 :total_segments 1} :fr {:approved 0 :total_segments 1}}})))
    (let
      [r (rtl/render (hx/$ view/translation-review-page))]
      (try
        (await (fixtures/wait-until "all-language request" #(= 1 (count (:list @fixtures/calls)))))
        (.change rtl/fireEvent (.getByLabelText r "Target Lang") #js {:target #js {:value "fr"}})
        (await
          (fixtures/wait-until
            "French request"
            #(some (fn [params] (= "fr" (:target-lang params))) (:list @fixtures/calls))))
        ((:resolve french-documents)
          {:documents [{:document_id "docs/fr"
             :target_lang "fr"
             :source_lang "en"
             :garden_id "gardens/sonic"
             :project "devel"
             :title "French Filter Doc"
             :total_segments 1
             :approved 0
             :overall_status "pending_review"}]
           :total 1})
        (await (fixtures/wait-until "French result" #(some? (.queryByText r "French Filter Doc"))))
        ((:resolve all-documents)
          {:documents [{:document_id "docs/all"
             :target_lang "es"
             :source_lang "en"
             :garden_id "gardens/sonic"
             :project "devel"
             :title "Stale All-Language Doc"
             :total_segments 1
             :approved 0
             :overall_status "pending_review"}]
           :total 1})
        (await
          (fixtures/wait-until
            "old filter response settled"
            #(= 2 (count (:language-response @fixtures/calls)))))
        (t/is (some? (.queryByText r "French Filter Doc")))
        (t/is (nil? (.queryByText r "Stale All-Language Doc")))
        (catch :default err (t/is false (str "unexpected: " err)))))))

(t/deftest ^:async stale-detail-response-cannot-overwrite-a-newer-selection
  (let
    [detail-a (fixtures/deferred)
     detail-b (fixtures/deferred)
     documents [{:document_id "docs/a"
       :target_lang "es"
       :source_lang "en"
       :garden_id "gardens/sonic"
       :project "devel"
       :title "Document A"
       :total_segments 1
       :approved 0
       :overall_status "pending_review"} {:document_id "docs/b"
       :target_lang "es"
       :source_lang "en"
       :garden_id "gardens/sonic"
       :project "devel"
       :title "Document B"
       :total_segments 1
       :approved 0
       :overall_status "pending_review"}]]
    (set! api/list-publication-reviews (fn [] (js/Promise.resolve {:project "devel" :reviews []})))
    (set! api/list-documents (fn [_] (js/Promise.resolve {:documents documents :total 2})))
    (set! api/get-document
      (fn ^:async respond
        ([document-id _]
          (fixtures/record! :get document-id)
          (:promise (if (= "docs/a" document-id) detail-a detail-b)))
        ([document-id _ _]
          (fixtures/record! :get document-id)
          (let
            [pending (if (= "docs/a" document-id) detail-a detail-b) response (await (:promise pending))]
            (fixtures/record! :detail-response document-id)
            response))))
    (let
      [r (rtl/render (hx/$ view/translation-review-page))
       detail-for (fn
         [title target]
         {:document {:title title :source_lang "en"}
          :summary {:total_segments 1 :approved 0 :overall_status "pending_review"}
          :segments [{:id (str title "/segment")
            :segment_index 0
            :status "pending"
            :source_lang "en"
            :target_lang "es"
            :source_text "Source"
            :translated_text target}]})]
      (try
        (await
          (fixtures/wait-until
            "both document cards"
            #(and (some? (.queryByText r "Document A")) (some? (.queryByText r "Document B")))))
        (.click rtl/fireEvent (.getByText r "Document A"))
        (await (fixtures/wait-until "A detail request" #(some #{"docs/a"} (:get @fixtures/calls))))
        (.click rtl/fireEvent (.getByText r "Document B"))
        (await (fixtures/wait-until "B detail request" #(some #{"docs/b"} (:get @fixtures/calls))))
        ((:resolve detail-b) (detail-for "Document B" "Fresh B target"))
        (await (fixtures/wait-until "B detail" #(some? (.queryByText r "Fresh B target"))))
        ((:resolve detail-a) (detail-for "Document A" "Stale A target"))
        (await
          (fixtures/wait-until "A response settled" #(some #{"docs/a"} (:detail-response @fixtures/calls))))
        (t/is (some? (.queryByText r "Fresh B target")))
        (t/is (nil? (.queryByText r "Stale A target")))
        (catch :default err (t/is false (str "unexpected: " err)))))))

(t/deftest ^:async async-refresh-preserves-the-reviewers-current-selection
  (let
    [dispatch (fixtures/deferred)
     inventory (atom
       [{:publication "publications/a-es"
         :document "docs/a"
         :garden "gardens/sonic"
         :project "devel"
         :source_locale "en"
         :locale "es"
         :title "Resource Row A"
         :revision "source-a"
         :work_state "missing"
         :reviewable false
         :allowed_actions ["dispatch"]} {:publication "publications/b-es"
         :document "docs/b"
         :garden "gardens/sonic"
         :project "devel"
         :source_locale "en"
         :locale "es"
         :title "Resource Row B"
         :revision "source-b"
         :work_state "missing"
         :reviewable false
         :allowed_actions []}])]
    (set! api/list-publication-reviews
      (fn
        []
        (fixtures/record! :publication-reviews true)
        (js/Promise.resolve {:project "devel" :reviews @inventory})))
    (set! api/list-documents
      (fn [params] (fixtures/record! :list params) (js/Promise.resolve {:documents [] :total 0})))
    (set! api/dispatch-publication-translation
      (fn [publication-id] (fixtures/record! :publication-dispatch publication-id) (:promise dispatch)))
    (let
      [r (rtl/render (hx/$ view/translation-review-page))]
      (try
        (await
          (fixtures/wait-until
            "resource rows"
            #(and (some? (.queryByText r "Resource Row A")) (some? (.queryByText r "Resource Row B")))))
        (.click rtl/fireEvent (.getByText r "Resource Row A"))
        (await (fixtures/wait-until "A selected" #(= 2 (count (.queryAllByText r "Resource Row A")))))
        (.click rtl/fireEvent (.getByRole r "button" #js {:name "Dispatch"}))
        (.click rtl/fireEvent (.getByText r "Resource Row B"))
        (await (fixtures/wait-until "B selected" #(= 2 (count (.queryAllByText r "Resource Row B")))))
        (swap! inventory update 0 assoc :work_state "in_flight" :allowed_actions [])
        ((:resolve dispatch) {:dispatched [{:outcome "dispatch/accepted"}]})
        (await
          (fixtures/wait-until "post-dispatch refresh" #(= 2 (count (:publication-reviews @fixtures/calls)))))
        (await (fixtures/wait-until "B remains selected" #(= 2 (count (.queryAllByText r "Resource Row B")))))
        (t/is
          (= 1 (count (.queryAllByText r "Resource Row A")))
          "the completed request does not restore its captured row A")
        (t/is (= ["publications/a-es"] (:publication-dispatch @fixtures/calls)))
        (catch :default err (t/is false (str "unexpected: " err)))))))
