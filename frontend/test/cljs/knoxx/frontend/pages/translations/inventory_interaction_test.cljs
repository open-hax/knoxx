(ns knoxx.frontend.pages.translations.inventory-interaction-test
  "Preserved translation workspace scenarios, using native async test control flow."
  (:require
    ["@testing-library/react" :as rtl]
    [cljs.test :as t]
    [helix.core :as hx]
    [knoxx.frontend.pages.translations.api :as api]
    [knoxx.frontend.pages.translations.page-fixtures :as fixtures]
    [knoxx.frontend.pages.translations.view :as view]))

(t/use-fixtures :each fixtures/api-fixture)

(t/deftest ^:async empty-resource-inventory-keeps-legacy-splits-reviewable
  (set! api/list-publication-reviews
    (fn [] (fixtures/record! :publication-reviews true) (js/Promise.resolve {:project "devel" :reviews []})))
  (let
    [r (rtl/render (hx/$ view/translation-review-page))]
    (try
      (await (fixtures/select-first-document r))
      (t/is (some? (.queryByText r "Hola mundo")) "legacy split annotations remain visible")
      (t/is
        (some? (.queryByRole r "button" #js {:name "Approve All"}))
        "the old document review flow remains interactive")
      (t/is (= [["docs/doc-1" "es" {:project "devel" :garden-id "gardens/sonic"}]] (:get @fixtures/calls)))
      (catch :default err (t/is false (str "unexpected: " err))))))

(t/deftest ^:async exact-legacy-splits-bridge-candidate-less-resource-work
  (set! api/list-publication-reviews
    (fn
      []
      (fixtures/record! :publication-reviews true)
      (js/Promise.resolve
        {:project "knoxx-session"
         :reviews [{:publication "publications/doc-1-es"
           :document "docs/doc-1"
           :garden "gardens/sonic"
           :source_locale "en"
           :locale "es"
           :title "Doc One"
           :project "knoxx-session"
           :revision "source-sha"
           :work_state "missing"
           :reviewable false
           :approved false
           :allowed_actions ["dispatch"]}]})))
  (let
    [r (rtl/render (hx/$ view/translation-review-page))]
    (try
      (await (fixtures/select-first-document r))
      (t/is (some? (.queryByText r "Hola mundo")) "the exact persisted split set renders over its CMS row")
      (t/is (some? (.queryByRole r "button" #js {:name "Approve All"})))
      (t/is (some? (.queryByRole r "button" #js {:name "Needs Edit"})))
      (t/is (some? (.queryByRole r "button" #js {:name "Reject All"})))
      (t/is
        (some? (.queryByRole r "button" #js {:name "Dispatch"}))
        "resource dispatch authority remains independently visible")
      (t/is
        (nil? (.queryByRole r "button" #js {:name "Approve for publication"}))
        "legacy splits never masquerade as whole-file publication evidence")
      (t/is (= {:project "knoxx-session" :target-lang ""} (first (:list @fixtures/calls))))
      (t/is (= {:project "knoxx-session" :garden-id "gardens/sonic"} (nth (first (:get @fixtures/calls)) 2)))
      (catch :default err (t/is false (str "unexpected: " err))))))

(t/deftest ^:async unhydrated-receipt-never-offers-publication-approval
  (let
    [r (rtl/render
       (hx/$ view/document-actions
         {:selected {:publication "publications/doc-1-es"
           :document_id "docs/doc-1"
           :garden_id "gardens/sonic"
           :target_lang "es"
           :translation_revision "translation-sha"
           :publication_review {:document "docs/doc-1"
            :garden "gardens/sonic"
            :locale "es"
            :revision "source-sha"
            :translation_revision "translation-sha"
            :reviewable false}}
          :saving false
          :on-review (fn [_])
          :on-publication-approval (fn [])
          :on-translation-dispatch (fn [])}))]
    (doseq
      [control ["Approve for publication" "Approve All" "Needs Edit" "Reject All"]]
      (t/is
        (nil? (.queryByRole r "button" #js {:name control}))
        (str control " is absent without explicit review/mutation authority")))))

(t/deftest ^:async unhydrated-resource-candidate-does-not-fall-through-to-legacy-detail
  (set! api/list-publication-reviews
    (fn
      []
      (fixtures/record! :publication-reviews true)
      (js/Promise.resolve
        {:reviews [(assoc fixtures/publication-review :source_locale
            "en"
            :title
            "Doc One"
            :work_state
            "ready"
            :contract_candidate
            true
            :reviewable
            false
            :hydration_state
            "content_missing")]})))
  (let
    [r (rtl/render (hx/$ view/translation-review-page))]
    (try
      (await (fixtures/wait-until "doc list" #(some? (.queryByText r "Doc One"))))
      (.click rtl/fireEvent (.getByText r "Doc One"))
      (await
        (fixtures/wait-until
          "missing candidate content"
          #(some? (.queryByText r "Translation candidate content unavailable"))))
      (t/is
        (empty? (:get @fixtures/calls))
        "a same-named legacy document is never fetched as substitute bytes")
      (t/is (nil? (.queryByText r "Hola mundo")))
      (t/is (nil? (.queryByRole r "button" #js {:name "Approve for publication"})))
      (doseq
        [control ["Approve All" "Needs Edit" "Reject All" "Submit review" "Submit as in review" "Mark rejected"]]
        (t/is
          (nil? (.queryByRole r "button" #js {:name control}))
          (str control " is absent while hydration is blocked")))
      (t/is
        (= 2
          (count
            (.queryAllByText r
              "This receipt names a resource-backed candidate, but its exact source or translated bytes could not be loaded. Review and publication approval are blocked; legacy content is not substituted.")))
        "both the document and review panes explain the closed gate")
      (catch :default err (t/is false (str "unexpected: " err))))))

(t/deftest ^:async publication-approval-uses-revision-bound-evidence
  (set! api/list-publication-reviews
    (fn
      []
      (fixtures/record! :publication-reviews true)
      (js/Promise.resolve
        {:project "devel"
         :reviews [(assoc fixtures/publication-review :reviewable
            true
            :content_source
            "agent"
            :source_text
            "Hello world\n\nBye"
            :translated_text
            "Hola mundo\n\nAdiós")]})))
  (let
    [r (rtl/render (hx/$ view/translation-review-page))]
    (try
      (await (fixtures/select-first-document r))
      (.click rtl/fireEvent (.getByRole r "button" #js {:name "Approve for publication"}))
      (await
        (fixtures/wait-until
          "publication approval notice"
          #(some? (.queryByText r "Translation approved; publication reconciliation: publication/materialized."))))
      (t/is
        (=
          [{:document "docs/doc-1"
            :garden "gardens/sonic"
            :locale "es"
            :revision "source-sha"
            :translation_revision "translation-sha"}]
          (:publication-approval @fixtures/calls)))
      (t/is (= ["publications/doc-1-es"] (:reconcile @fixtures/calls)))
      (catch :default err (t/is false (str "unexpected: " err))))))

(t/deftest ^:async candidate-less-resource-work-remains-visible-and-dispatchable
  (set! api/list-documents
    (fn [params] (fixtures/record! :list params) (js/Promise.resolve {:documents [] :total 0})))
  (let
    [inventory (atom
       [{:publication "publications/missing-es"
         :document "docs/missing"
         :garden "gardens/sonic"
         :source_locale "en"
         :locale "es"
         :title "Missing Doc"
         :revision "missing-source"
         :work_state "missing"
         :reviewable false
         :approved false
         :allowed_actions ["dispatch"]} {:publication "publications/failed-es"
         :document "docs/failed"
         :garden "gardens/sonic"
         :source_locale "en"
         :locale "es"
         :title "Failed Doc"
         :revision "failed-source"
         :work_state "failed"
         :reviewable false
         :approved false
         :allowed_actions ["retry"]}
        {:publication "publications/ready-es"
         :document "docs/ready"
         :garden "gardens/sonic"
         :source_locale "en"
         :locale "es"
         :title "Ready Doc"
         :revision "ready-source"
         :translation_revision "ready-target"
         :work_state "ready"
         :contract_candidate true
         :reviewable true
         :hydration_state "displayable"
         :approved false
         :allowed_actions []}])]
    (set! api/list-publication-reviews
      (fn [] (fixtures/record! :publication-reviews true) (js/Promise.resolve {:reviews @inventory})))
    (set! api/dispatch-publication-translation
      (fn
        [publication-id]
        (fixtures/record! :publication-dispatch publication-id)
        (swap! inventory update 0 assoc :work_state "in_flight" :allowed_actions [])
        (js/Promise.resolve {:dispatched [{:outcome "dispatch/accepted"}]})))
    (let
      [r (rtl/render (hx/$ view/translation-review-page))]
      (try
        (await
          (fixtures/wait-until
            "resource inventory"
            #(and (some? (.queryByText r "Missing Doc")) (some? (.queryByText r "Failed Doc")) (some? (.queryByText r "Ready Doc")))))
        (.click rtl/fireEvent (.getByText r "Ready Doc"))
        (await
          (fixtures/wait-until
            "receipt-shaped candidate without authenticated bytes"
            #(some? (.queryByText r "Translation candidate content unavailable"))))
        (t/is
          (nil? (.queryByRole r "button" #js {:name "Approve for publication"}))
          "a receipt and reviewable flag cannot authorize approval without authenticated content_source bytes")
        (t/is
          (empty? (:get @fixtures/calls))
          "no legacy worker document exists and none is fetched as substitute content")
        (.click rtl/fireEvent (.getByText r "Missing Doc"))
        (await
          (fixtures/wait-until
            "candidate-less detail"
            #(some? (.queryByText r "No translation candidate yet"))))
        (t/is
          (nil? (.queryByRole r "button" #js {:name "Approve for publication"}))
          "candidate-less work never exposes an approval control")
        (.click rtl/fireEvent (.getByRole r "button" #js {:name "Dispatch"}))
        (await
          (fixtures/wait-until
            "dispatch notice"
            #(some? (.queryByText r "Translation dispatch: dispatch/accepted."))))
        (await
          (fixtures/wait-until
            "selected row refresh"
            #(some? (.queryByText r (js/RegExp. "Resource work is in flight")))))
        (t/is
          (= ["publications/missing-es"] (:publication-dispatch @fixtures/calls))
          "the exact publication identity, not a client revision, is sent")
        (t/is (= 2 (count (:list @fixtures/calls))) "dispatch refreshes the resource inventory")
        (t/is
          (nil? (.queryByRole r "button" #js {:name "Dispatch"}))
          "the refreshed whole row removes stale server-authored actions")
        (catch :default err (t/is false (str "unexpected: " err)))))))

(t/deftest ^:async server-inventory-project-controls-legacy-list-and-selected-scope
  (set! api/list-publication-reviews
    (fn
      []
      (fixtures/record! :publication-reviews true)
      (js/Promise.resolve
        {:project "knoxx-session"
         :reviews [(-> fixtures/publication-review
            (dissoc :translation_revision)
            (assoc :project
              "knoxx-session"
              :work_state
              "missing"
              :reviewable
              false
              :allowed_actions
              ["dispatch"]))]})))
  (let
    [r (rtl/render (hx/$ view/translation-review-page))]
    (try
      (await (fixtures/select-first-document r))
      (t/is
        (every? #(= "knoxx-session" (:project %)) (:list @fixtures/calls))
        "reviews load first, so no legacy query escapes to devel")
      (t/is
        (= "knoxx-session" (.-value (.getByPlaceholderText r "devel")))
        "the visible project reflects server scope")
      (t/is
        (= {:project "knoxx-session" :garden-id "gardens/sonic"} (nth (last (:get @fixtures/calls)) 2))
        "selected detail prefers inventory project and keeps garden")
      (catch :default err (t/is false (str "unexpected: " err))))))

(t/deftest ^:async empty-inventory-envelope-project-controls-legacy-compatibility-flow
  (set! api/list-publication-reviews
    (fn
      []
      (fixtures/record! :publication-reviews true)
      (js/Promise.resolve {:project "knoxx-session" :reviews []})))
  (let
    [r (rtl/render (hx/$ view/translation-review-page))]
    (try
      (await (fixtures/select-first-document r))
      (t/is
        (= [{:project "knoxx-session" :target-lang ""}] (:list @fixtures/calls))
        "even an empty inventory scopes the compatibility query")
      (t/is (= "knoxx-session" (.-value (.getByPlaceholderText r "devel"))))
      (t/is (= {:project "knoxx-session" :garden-id "gardens/sonic"} (nth (first (:get @fixtures/calls)) 2)))
      (t/is (some? (.queryByRole r "button" #js {:name "Approve All"})))
      (catch :default err (t/is false (str "unexpected: " err))))))

(t/deftest ^:async unmatched-gardenless-worker-row-remains-in-legacy-compatibility
  (set! api/list-publication-reviews
    (fn
      []
      (fixtures/record! :publication-reviews true)
      (js/Promise.resolve {:project "knoxx-session" :reviews []})))
  (set! api/list-documents
    (fn
      [params]
      (fixtures/record! :list params)
      (js/Promise.resolve
        {:documents [{:document_id "docs/gardenless-worker"
           :target_lang "es"
           :source_lang "en"
           :project "knoxx-session"
           :title "Gardenless Worker"
           :total_segments 2
           :approved 1
           :overall_status "partial_review"}]
         :total 1})))
  (let
    [r (rtl/render (hx/$ view/translation-review-page))]
    (await (fixtures/wait-until "gardenless compatibility row" #(some? (.queryByText r "Gardenless Worker"))))
    (.click rtl/fireEvent (.getByText r "Gardenless Worker"))
    (await (fixtures/wait-until "gardenless legacy detail" #(some? (.queryByText r "Hola mundo"))))
    (t/is
      (= {:project "knoxx-session"} (nth (last (:get @fixtures/calls)) 2))
      "omitted garden remains one exact compatibility coordinate")
    (t/is (some? (.queryByRole r "button" #js {:name "Approve All"})))
    (t/is (some? (.queryByRole r "button" #js {:name "Needs Edit"})))
    (t/is (some? (.queryByRole r "button" #js {:name "Reject All"})))
    (t/is
      (nil? (.queryByRole r "button" #js {:name "Approve for publication"}))
      "worker evidence restores legacy review, never whole-publication approval")))

(t/deftest ^:async moved-source-candidate-fails-closed-in-both-review-panes
  (set! api/list-publication-reviews
    (fn
      []
      (fixtures/record! :publication-reviews true)
      (js/Promise.resolve
        {:project "devel"
         :reviews [(assoc fixtures/publication-review :work_state
            "stale"
            :contract_candidate
            true
            :reviewable
            false
            :hydration_state
            "source_moved")]})))
  (let
    [r (rtl/render (hx/$ view/translation-review-page))]
    (try
      (await (fixtures/wait-until "moved candidate row" #(some? (.queryByText r "Doc One"))))
      (.click rtl/fireEvent (.getByText r "Doc One"))
      (await
        (fixtures/wait-until
          "moved-source block"
          #(seq (.queryAllByText r "The source revision changed after this candidate was created. Review and publication approval are blocked until a candidate is produced for the current source."))))
      (t/is (empty? (:get @fixtures/calls)) "source movement cannot fall through to same-named legacy bytes")
      (await
        (doseq
          [control ["Approve for publication" "Approve All"
            "Needs Edit" "Reject All"
            "Submit review" "Submit as in review"
            "Mark rejected"]]
          (t/is
            (nil? (.queryByRole r "button" #js {:name control}))
            (str control " is absent for a moved source"))))
      (catch :default err (t/is false (str "unexpected: " err))))))
