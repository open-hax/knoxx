(ns knoxx.frontend.pages.translations.page-interaction-test
  "Written FIRST (TDD) — interaction flows for the Helix translation
  review page: document list → detail → segment label submit, document-
  level review, and the pipeline model config save (ports the contract of
  TranslationModelSection.test.tsx; the review page itself had NO vitest
  coverage — these are net-new). API ns mocked via set!."
  (:require [cljs.test :refer [deftest is async use-fixtures]]
            ["@testing-library/react" :as rtl]
            [helix.core :refer [$]]
            [knoxx.frontend.pages.translations.api :as api]
            [knoxx.frontend.pages.translations.view :refer [document-actions
                                                            translation-review-page]]))

;; jsdom globals come from the :test build's :prepend-js.

(def doc-summary
  {:document_id "docs/doc-1" :target_lang "es" :title "Doc One"
   :overall_status "pending_review" :source_lang "en" :garden_id "gardens/sonic"
   :project "devel" :approved 1 :total_segments 2})

(def publication-review
  {:publication "publications/doc-1-es"
   :document "docs/doc-1" :garden "gardens/sonic" :locale "es"
   :project "devel" :source_locale "en" :title "Doc One"
   :revision "source-sha" :translation_revision "translation-sha"
   :translated_at "2026-08-26T10:00:00.000Z" :approved false})

(def resource-split-review
  {:candidate_set_id "candidate-set/doc-1-es"
   :manifest_id "manifest/doc-1-es"
   :status "partial-review"
   :splits [{:split_id "split/doc-1/0" :split_index 0
             :source_text "Hello world" :candidate_text "Hola mundo"
             :review_status "in-review"
             :adequacy "adequate" :fluency "good"
             :terminology "minor_errors" :risk "sensitive"
             :editor_notes "Confirm product terminology"}
            {:split_id "split/doc-1/1" :split_index 1
             :source_text "Bye" :candidate_text "Adiós"
             :review_status "approved"}]})

(def doc-detail
  {:document {:title "Doc One" :source_lang "en"}
   :summary {:total_segments 2 :approved 1 :overall_status "partial_review"}
   :segments [{:id "seg-a" :segment_index 0 :status "pending"
               :source_lang "en" :target_lang "es"
               :source_text "Hello world" :translated_text "Hola mundo"
               :label_count 0}
              {:id "seg-b" :segment_index 1 :status "approved"
               :source_lang "en" :target_lang "es"
               :source_text "Bye" :translated_text "Adiós"
               :label_count 1}]})

(def calls (atom {}))
(defn- record! [k v] (swap! calls update k (fnil conj []) v))

(def ^:private originals
  {:list api/list-documents :get api/get-document :review api/review-document
   :list-publication-reviews api/list-publication-reviews
   :dispatch-publication api/dispatch-publication-translation
   :approve-publication api/approve-publication-translation
   :split-review api/submit-publication-split-review
   :bulk-review api/submit-publication-bulk-review
   :reconcile-publication api/reconcile-publication
   :label api/submit-label :manifest api/get-manifest :sft api/sft-export
   :config api/pipeline-config :update-config api/update-pipeline-config
   :models api/list-proxx-models})

(use-fixtures :each
  {:before (fn []
             (reset! calls {})
             (set! api/list-documents
                   (fn [params]
                     (record! :list params)
                     (js/Promise.resolve
                      {:documents [(assoc doc-summary :project (:project params))]
                       :total 1})))
             (set! api/get-document
                   (fn
                     ([id lang]
                      (record! :get [id lang nil])
                      (js/Promise.resolve doc-detail))
                     ([id lang scope]
                      (record! :get [id lang scope])
                      (js/Promise.resolve doc-detail))))
             (set! api/list-publication-reviews
                   (fn []
                     (record! :publication-reviews true)
                     (js/Promise.resolve {:project "devel" :reviews []})))
             (set! api/dispatch-publication-translation
                   (fn [publication-id]
                     (record! :publication-dispatch publication-id)
                     (js/Promise.resolve
                      {:dispatched [{:outcome "dispatch/accepted"}]})))
             (set! api/approve-publication-translation
                   (fn [payload]
                     (record! :publication-approval payload)
                     (js/Promise.resolve {:approved true :status "recorded"})))
             (set! api/submit-publication-split-review
                   (fn [payload]
                     (record! :publication-split-review payload)
                     (js/Promise.resolve {:status "recorded"})))
             (set! api/submit-publication-bulk-review
                   (fn [payload]
                     (record! :publication-bulk-review payload)
                     (js/Promise.resolve {:status "recorded"})))
             (set! api/reconcile-publication
                   (fn [publication-id]
                     (record! :reconcile publication-id)
                     (js/Promise.resolve {:type "publication/materialized"})))
             (set! api/review-document
                   (fn
                     ([id lang payload]
                      (record! :review [id lang nil payload])
                      (js/Promise.resolve
                       {:ok true :segments_reviewed 2 :overall (:overall payload)}))
                     ([id lang scope payload]
                      (record! :review [id lang scope payload])
                      (js/Promise.resolve
                       {:ok true :segments_reviewed 2 :overall (:overall payload)}))))
             (set! api/submit-label
                   (fn
                     ([seg-id payload]
                      (record! :label [seg-id nil payload])
                      (js/Promise.resolve {:ok true :new_status "approved"}))
                     ([seg-id scope payload]
                      (record! :label [seg-id scope payload])
                      (js/Promise.resolve {:ok true :new_status "approved"}))))
             (set! api/get-manifest
                   (fn [project]
                     (record! :manifest project)
                     (js/Promise.resolve {:languages {:es {:approved 1 :total_segments 2}}})))
             (set! api/pipeline-config
                   (fn []
                     (record! :config true)
                     (js/Promise.resolve {:model "glm-5" :updated_at nil})))
             (set! api/update-pipeline-config
                   (fn [model]
                     (record! :update-config model)
                     (js/Promise.resolve {:model model :updated_at "2026-06-11"})))
             (set! api/list-proxx-models
                   (fn []
                     (record! :models true)
                     (js/Promise.resolve [{:id "glm-5"} {:id "gpt-5.4"}]))))
   :after (fn []
            (rtl/cleanup)
            (set! api/list-documents (:list originals))
            (set! api/get-document (:get originals))
            (set! api/list-publication-reviews (:list-publication-reviews originals))
            (set! api/dispatch-publication-translation (:dispatch-publication originals))
            (set! api/approve-publication-translation (:approve-publication originals))
            (set! api/submit-publication-split-review (:split-review originals))
            (set! api/submit-publication-bulk-review (:bulk-review originals))
            (set! api/reconcile-publication (:reconcile-publication originals))
            (set! api/review-document (:review originals))
            (set! api/submit-label (:label originals))
            (set! api/get-manifest (:manifest originals))
            (set! api/sft-export (:sft originals))
            (set! api/pipeline-config (:config originals))
            (set! api/update-pipeline-config (:update-config originals))
            (set! api/list-proxx-models (:models originals)))})

(defn- wait-until [msg pred]
  (rtl/waitFor (fn [] (when-not (pred) (throw (js/Error. (str "still waiting: " msg)))))))

(defn- deferred []
  (let [resolve! (atom nil)
        reject! (atom nil)
        promise (js/Promise. (fn [resolve reject]
                               (reset! resolve! resolve)
                               (reset! reject! reject)))]
    {:promise promise
     :resolve #(@resolve! %)
     :reject #(@reject! %)}))

(defn- select-first-document [^js r]
  (-> (wait-until "doc list" #(some? (.queryByText r "Doc One")))
      (.then (fn [] (.click rtl/fireEvent (.getByText r "Doc One"))
               (wait-until "detail" #(some? (.queryByText r "Hola mundo")))))))

(defn- install-resource-split-inventory!
  [inventory]
  (set! api/list-publication-reviews
        (fn []
          (record! :publication-reviews true)
          (js/Promise.resolve {:project "devel" :reviews @inventory}))))

(defn- resource-review-row
  [aggregate]
  (assoc publication-review
         :work_state "ready"
         :split_review aggregate))

(deftest loads-documents-and-detail-on-selection
  (async done
    (let [r (rtl/render ($ translation-review-page))]
      (-> (select-first-document r)
          (.then (fn []
                   (is (= {:project "devel" :target-lang ""} (first (:list @calls))))
                   (is (= "devel" (first (:manifest @calls))))
                   (is (= [["docs/doc-1" "es"
                            {:project "devel" :garden-id "gardens/sonic"}]]
                          (:get @calls)))
                   (is (some? (.queryByText r "Hello world")) "segment source shown")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest empty-resource-inventory-keeps-legacy-splits-reviewable
  (async done
    (set! api/list-publication-reviews
          (fn []
            (record! :publication-reviews true)
            (js/Promise.resolve {:project "devel" :reviews []})))
    (let [r (rtl/render ($ translation-review-page))]
      (-> (select-first-document r)
          (.then (fn []
                   (is (some? (.queryByText r "Hola mundo"))
                       "legacy split annotations remain visible")
                   (is (some? (.queryByRole r "button" #js {:name "Approve All"}))
                       "the old document review flow remains interactive")
                   (is (= [["docs/doc-1" "es"
                            {:project "devel" :garden-id "gardens/sonic"}]]
                          (:get @calls)))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest exact-legacy-splits-bridge-candidate-less-resource-work
  (async done
    (set! api/list-publication-reviews
          (fn []
            (record! :publication-reviews true)
            (js/Promise.resolve
             {:project "knoxx-session"
              :reviews [{:publication "publications/doc-1-es"
                         :document "docs/doc-1" :garden "gardens/sonic"
                         :source_locale "en" :locale "es" :title "Doc One"
                         :project "knoxx-session" :revision "source-sha"
                         :work_state "missing" :reviewable false
                         :approved false :allowed_actions ["dispatch"]}]})))
    (let [r (rtl/render ($ translation-review-page))]
      (-> (select-first-document r)
          (.then (fn []
                   (is (some? (.queryByText r "Hola mundo"))
                       "the exact persisted split set renders over its CMS row")
                   (is (some? (.queryByRole r "button" #js {:name "Approve All"})))
                   (is (some? (.queryByRole r "button" #js {:name "Needs Edit"})))
                   (is (some? (.queryByRole r "button" #js {:name "Reject All"})))
                   (is (some? (.queryByRole r "button" #js {:name "Dispatch"}))
                       "resource dispatch authority remains independently visible")
                   (is (nil? (.queryByRole r "button"
                                           #js {:name "Approve for publication"}))
                       "legacy splits never masquerade as whole-file publication evidence")
                   (is (= {:project "knoxx-session" :target-lang ""}
                          (first (:list @calls))))
                   (is (= {:project "knoxx-session"
                           :garden-id "gardens/sonic"}
                          (nth (first (:get @calls)) 2)))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest unhydrated-receipt-never-offers-publication-approval
  (let [r (rtl/render
           ($ document-actions
              {:selected {:publication "publications/doc-1-es"
                          :document_id "docs/doc-1" :garden_id "gardens/sonic"
                          :target_lang "es" :translation_revision "translation-sha"
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
    (doseq [control ["Approve for publication" "Approve All" "Needs Edit" "Reject All"]]
      (is (nil? (.queryByRole r "button" #js {:name control}))
          (str control " is absent without explicit review/mutation authority")))))

(deftest unhydrated-resource-candidate-does-not-fall-through-to-legacy-detail
  (async done
    (set! api/list-publication-reviews
          (fn []
            (record! :publication-reviews true)
            (js/Promise.resolve
             {:reviews [(assoc publication-review
                               :source_locale "en"
                               :title "Doc One"
                               :work_state "ready"
                               :contract_candidate true
                               :reviewable false
                               :hydration_state "content_missing")]})))
    (let [r (rtl/render ($ translation-review-page))]
      (-> (wait-until "doc list" #(some? (.queryByText r "Doc One")))
          (.then (fn []
                   (.click rtl/fireEvent (.getByText r "Doc One"))
                   (wait-until
                    "missing candidate content"
                    #(some? (.queryByText r "Translation candidate content unavailable")))))
          (.then (fn []
                   (is (empty? (:get @calls))
                       "a same-named legacy document is never fetched as substitute bytes")
                   (is (nil? (.queryByText r "Hola mundo")))
                   (is (nil? (.queryByRole r "button"
                                           #js {:name "Approve for publication"})))
                   (doseq [control ["Approve All" "Needs Edit" "Reject All"
                                    "Submit review" "Submit as in review"
                                    "Mark rejected"]]
                     (is (nil? (.queryByRole r "button" #js {:name control}))
                         (str control " is absent while hydration is blocked")))
                   (is (= 2 (count (.queryAllByText
                                    r
                                    "This receipt names a resource-backed candidate, but its exact source or translated bytes could not be loaded. Review and publication approval are blocked; legacy content is not substituted.")))
                       "both the document and review panes explain the closed gate")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest ^:async resource-split-corrected-approval-posts-and-unlocks-whole-output
  (let [inventory (atom [(resource-review-row resource-split-review)])]
    (install-resource-split-inventory! inventory)
    (set! api/submit-publication-split-review
          (fn [payload]
            (record! :publication-split-review payload)
            (swap! inventory update-in [0 :split_review :splits 0]
                   assoc
                   :review_status (:status payload)
                   :corrected_text (:corrected_text payload))
            (js/Promise.resolve {:status "recorded"})))
    (let [r (rtl/render ($ translation-review-page))]
      (await (select-first-document r))
      (let [whole-button
            (.getByRole r "button" #js {:name "Approve whole output"})]
        (is (.-disabled whole-button)
            "whole-output approval waits for every split"))
      (.click rtl/fireEvent (.getByText r "Hola mundo"))
      (await
       (wait-until "resource split editor"
                   #(some? (.queryByRole r "button" #js {:name "Approve split"}))))
      (is (some? (.queryByRole r "button" #js {:name "Submit review"}))
          "the split can remain explicitly in review")
      (is (= "adequate" (.-value (.getByLabelText r "adequacy"))))
      (is (= "good" (.-value (.getByLabelText r "fluency"))))
      (is (= "minor_errors" (.-value (.getByLabelText r "terminology"))))
      (is (= "sensitive" (.-value (.getByLabelText r "risk"))))
      (is (= "Confirm product terminology"
             (.-value (.getByLabelText r "Editor notes")))
          "the latest granular review hydrates the resource split card")
      (.change rtl/fireEvent
               (.getByLabelText r "adequacy")
               #js {:target #js {:value "excellent"}})
      (.change rtl/fireEvent
               (.getByLabelText r "Corrected translation")
               #js {:target #js {:value "  Hola, mundo corregido.  "}})
      (.change rtl/fireEvent
               (.getByLabelText r "Editor notes")
               #js {:target #js {:value "  Approved after terminology pass.  "}})
      (.click rtl/fireEvent (.getByRole r "button" #js {:name "Approve split"}))
      (await
       (wait-until "approved notice"
                   #(some? (.queryByText r "Split 0 approved."))))
      (is (= [{:candidate_set_id "candidate-set/doc-1-es"
               :split_id "split/doc-1/0"
               :status "approved"
               :adequacy "excellent"
               :fluency "good"
               :terminology "minor_errors"
               :risk "sensitive"
               :corrected_text "  Hola, mundo corregido.  "
               :editor_notes "Approved after terminology pass."}]
             (:publication-split-review @calls)))
      (await
       (wait-until "all-split progress" #(seq (.queryAllByText r "2/2"))))
      (is (not (.-disabled
                (.getByRole r "button" #js {:name "Approve whole output"})))
          "whole-output action becomes available only after 2/2")
      (is (empty? (:get @calls))
          "resource splits never fall through to legacy Mongo detail"))))

(deftest ^:async resource-split-skip-advances-without-writing-review-evidence
  (let [inventory (atom [(resource-review-row resource-split-review)])]
    (install-resource-split-inventory! inventory)
    (let [r (rtl/render ($ translation-review-page))]
      (await (select-first-document r))
      (.click rtl/fireEvent (.getByText r "Hola mundo"))
      (await
       (wait-until "first resource split"
                   #(some? (.queryByText r "Split 0"))))
      (.click rtl/fireEvent (.getByRole r "button" #js {:name "Skip"}))
      (await
       (wait-until "next resource split"
                   #(some? (.queryByText r "Split 1"))))
      (is (empty? (:publication-split-review @calls))
          "Skip changes only local selection")
      (is (empty? (:publication-bulk-review @calls))
          "Skip never substitutes a bulk verdict")
      (is (.-disabled (.getByRole r "button" #js {:name "Skip"}))
          "the final split does not wrap back to the beginning"))))

(deftest ^:async resource-document-review-fast-path-posts-one-candidate-set
  (let [inventory (atom [(resource-review-row resource-split-review)])]
    (install-resource-split-inventory! inventory)
    (set! api/submit-publication-bulk-review
          (fn [payload]
            (record! :publication-bulk-review payload)
            (swap! inventory update-in [0 :split_review :splits]
                   #(mapv (fn [split]
                            (assoc split :review_status (:status payload)))
                          %))
            (js/Promise.resolve {:status "recorded"})))
    (let [r (rtl/render ($ translation-review-page))]
      (await (select-first-document r))
      (doseq [action ["Approve All" "Needs Edit" "Reject All"]]
        (is (some? (.queryByRole r "button" #js {:name action}))))
      (is (some? (.queryByRole r "button"
                               #js {:name "Approve whole output"}))
          "publication approval remains a separate, gated action")
      (.click rtl/fireEvent (.getByText r "Hola mundo"))
      (await
       (wait-until "resource evaluation form"
                   #(some? (.queryByLabelText r "fluency"))))
      (.change rtl/fireEvent
               (.getByLabelText r "fluency")
               #js {:target #js {:value "poor"}})
      (.change rtl/fireEvent
               (.getByLabelText r "Corrected translation")
               #js {:target #js {:value "Must remain split-local"}})
      (.change rtl/fireEvent
               (.getByLabelText r "Editor notes")
               #js {:target #js {:value "  Apply to every persisted split.  "}})
      (.click rtl/fireEvent (.getByRole r "button" #js {:name "Needs Edit"}))
      (await
       (wait-until "bulk review notice"
                   #(some? (.queryByText r "All splits in-review."))))
      (is (= [{:candidate_set_id "candidate-set/doc-1-es"
               :status "in-review"
               :adequacy "adequate"
               :fluency "poor"
               :terminology "minor_errors"
               :risk "sensitive"
               :editor_notes "Apply to every persisted split."}]
             (:publication-bulk-review @calls))
          "the server owns split enumeration and document scope cannot carry a correction")
      (is (empty? (:publication-split-review @calls))))))

(deftest ^:async resource-split-rejection-is-granular-and-relocks-whole-output
  (let [approved-aggregate
        (update resource-split-review :splits
                #(mapv (fn [split] (assoc split :review_status "approved")) %))
        inventory (atom [(resource-review-row approved-aggregate)])]
    (install-resource-split-inventory! inventory)
    (set! api/submit-publication-split-review
          (fn [payload]
            (record! :publication-split-review payload)
            (swap! inventory update-in [0 :split_review :splits 0]
                   assoc :review_status (:status payload))
            (js/Promise.resolve {:status "recorded"})))
    (let [r (rtl/render ($ translation-review-page))]
      (await (select-first-document r))
      (is (not (.-disabled
                (.getByRole r "button" #js {:name "Approve whole output"}))))
      (.click rtl/fireEvent (.getByText r "Hola mundo"))
      (await
       (wait-until "reject control"
                   #(some? (.queryByRole r "button" #js {:name "Reject split"}))))
      (.click rtl/fireEvent (.getByRole r "button" #js {:name "Reject split"}))
      (await
       (wait-until "rejected notice"
                   #(some? (.queryByText r "Split 0 rejected."))))
      (is (= [{:candidate_set_id "candidate-set/doc-1-es"
               :split_id "split/doc-1/0"
               :status "rejected"
               :adequacy "adequate"
               :fluency "good"
               :terminology "minor_errors"
               :risk "sensitive"
               :editor_notes "Confirm product terminology"}]
             (:publication-split-review @calls)))
      (await
       (wait-until "partial review projection"
                   #(seq (.queryAllByText r "1/2"))))
      (is (.-disabled
           (.getByRole r "button" #js {:name "Approve whole output"}))
          "a rejected split relocks whole-output approval"))))

(deftest segment-label-submit-posts-payload-and-reloads
  (async done
    (let [r (rtl/render ($ translation-review-page))]
      (-> (select-first-document r)
          (.then (fn []
                   (.click rtl/fireEvent (.getByText r "Hola mundo"))
                   (wait-until "panel" #(some? (.queryByText r "Segment 0")))))
          (.then (fn []
                   (.click rtl/fireEvent (.getByRole r "button" #js {:name "Submit review"}))
                   (wait-until "notice" #(some? (.queryByText r "Segment 0: approve")))))
          (.then (fn []
                   (let [[seg-id scope payload] (first (:label @calls))]
                     (is (= "seg-a" seg-id))
                     (is (= {:project "devel" :garden-id "gardens/sonic"}
                            scope))
                     (is (= "approve" (:overall payload)))
                     (is (not (contains? payload :corrected_text)) "blank correction omitted"))
                   (is (= 2 (count (:get @calls))) "detail reloaded after label")
                   (is (= 2 (count (:list @calls))) "list reloaded after label")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest changing-segments-hydrates-form-without-correction-leakage
  (async done
    (let [detail (-> doc-detail
                     (assoc-in [:segments 0 :labels] [])
                     (assoc-in [:segments 1 :labels]
                               [{:id "label-b" :adequacy "adequate"
                                 :overall "needs_edit"
                                 :corrected_text "Persisted B correction"
                                 :editor_notes "Persisted B note"}]))]
      (set! api/get-document
            (fn
              ([_ _] (js/Promise.resolve detail))
              ([_ _ _] (js/Promise.resolve detail))))
      (let [r (rtl/render ($ translation-review-page))]
        (-> (select-first-document r)
            (.then (fn []
                     (.click rtl/fireEvent (.getByText r "Hola mundo"))
                     (wait-until
                      "first segment form"
                      #(some? (.queryByLabelText r "Corrected translation")))))
            (.then (fn []
                     (.change rtl/fireEvent
                              (.getByLabelText r "Corrected translation")
                              #js {:target #js {:value "Draft for segment A"}})
                     (is (= "Draft for segment A"
                            (.-value (.getByLabelText r "Corrected translation"))))
                     (.click rtl/fireEvent (.getByText r "Adiós"))
                     (wait-until
                      "second segment form hydration"
                      #(= "Persisted B correction"
                          (.-value (.getByLabelText r "Corrected translation"))))))
            (.then (fn []
                     (is (= "Persisted B note"
                            (.-value (.getByLabelText r "Editor notes"))))
                     (is (= "adequate"
                            (.-value (.getByLabelText r "adequacy"))))
                     (done)))
            (.catch (fn [err] (is false (str "unexpected: " err)) (done))))))))

(deftest document-level-review-approves-all
  (async done
    (let [r (rtl/render ($ translation-review-page))]
      (-> (select-first-document r)
          (.then (fn []
                   (.click rtl/fireEvent (.getByRole r "button" #js {:name "Approve All"}))
                   (wait-until "notice" #(some? (.queryByText r "Document review: approve (2 segments)")))))
          (.then (fn []
                   (is (= [["docs/doc-1" "es"
                            {:project "devel" :garden-id "gardens/sonic"}
                            {:overall "approve"}]]
                          (:review @calls)))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest publication-approval-uses-revision-bound-evidence
  (async done
    (set! api/list-publication-reviews
          (fn []
            (record! :publication-reviews true)
            (js/Promise.resolve
             {:project "devel"
              :reviews [(assoc publication-review
                               :reviewable true
                               :content_source "agent"
                               :source_text "Hello world\n\nBye"
                               :translated_text "Hola mundo\n\nAdiós")]})))
    (let [r (rtl/render ($ translation-review-page))]
      (-> (select-first-document r)
          (.then (fn []
                   (.click rtl/fireEvent
                           (.getByRole r "button" #js {:name "Approve for publication"}))
                   (wait-until "publication approval notice"
                               #(some? (.queryByText
                                        r
                                        "Translation approved; publication reconciliation: publication/materialized.")))))
          (.then (fn []
                   (is (= [{:document "docs/doc-1" :garden "gardens/sonic"
                            :locale "es" :revision "source-sha"
                            :translation_revision "translation-sha"}]
                          (:publication-approval @calls)))
                   (is (= ["publications/doc-1-es"] (:reconcile @calls)))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest candidate-less-resource-work-remains-visible-and-dispatchable
  (async done
    (set! api/list-documents
          (fn [params]
            (record! :list params)
            (js/Promise.resolve {:documents [] :total 0})))
    (let [inventory (atom
                     [{:publication "publications/missing-es"
                       :document "docs/missing" :garden "gardens/sonic"
                       :source_locale "en" :locale "es" :title "Missing Doc"
                       :revision "missing-source" :work_state "missing"
                       :reviewable false :approved false
                       :allowed_actions ["dispatch"]}
                      {:publication "publications/failed-es"
                       :document "docs/failed" :garden "gardens/sonic"
                       :source_locale "en" :locale "es" :title "Failed Doc"
                       :revision "failed-source" :work_state "failed"
                       :reviewable false :approved false
                       :allowed_actions ["retry"]}
                      {:publication "publications/ready-es"
                       :document "docs/ready" :garden "gardens/sonic"
                       :source_locale "en" :locale "es" :title "Ready Doc"
                       :revision "ready-source" :translation_revision "ready-target"
                       :work_state "ready" :contract_candidate true
                       :reviewable true :hydration_state "displayable"
                       :approved false
                       :allowed_actions []}])]
      (set! api/list-publication-reviews
            (fn []
              (record! :publication-reviews true)
              (js/Promise.resolve {:reviews @inventory})))
      (set! api/dispatch-publication-translation
            (fn [publication-id]
              (record! :publication-dispatch publication-id)
              (swap! inventory update 0 assoc
                     :work_state "in_flight" :allowed_actions [])
              (js/Promise.resolve
               {:dispatched [{:outcome "dispatch/accepted"}]})))
      (let [r (rtl/render ($ translation-review-page))]
      (-> (wait-until "resource inventory"
                      #(and (some? (.queryByText r "Missing Doc"))
                            (some? (.queryByText r "Failed Doc"))
                            (some? (.queryByText r "Ready Doc"))))
          (.then (fn []
                   (.click rtl/fireEvent (.getByText r "Ready Doc"))
                   (wait-until
                    "receipt-shaped candidate without authenticated bytes"
                    #(some? (.queryByText
                             r "Translation candidate content unavailable")))))
          (.then (fn []
                   (is (nil? (.queryByRole
                              r "button" #js {:name "Approve for publication"}))
                       "a receipt and reviewable flag cannot authorize approval without authenticated content_source bytes")
                   (is (empty? (:get @calls))
                       "no legacy worker document exists and none is fetched as substitute content")
                   (.click rtl/fireEvent (.getByText r "Missing Doc"))
                   (wait-until "candidate-less detail"
                               #(some? (.queryByText r "No translation candidate yet")))))
          (.then (fn []
                   (is (nil? (.queryByRole r "button"
                                           #js {:name "Approve for publication"}))
                       "candidate-less work never exposes an approval control")
                   (.click rtl/fireEvent (.getByRole r "button" #js {:name "Dispatch"}))
                   (wait-until "dispatch notice"
                               #(some? (.queryByText r "Translation dispatch: dispatch/accepted.")))))
          (.then (fn []
                   (wait-until "selected row refresh"
                               #(some? (.queryByText
                                        r
                                        (js/RegExp. "Resource work is in flight"))))))
          (.then (fn []
                   (is (= ["publications/missing-es"]
                          (:publication-dispatch @calls))
                       "the exact publication identity, not a client revision, is sent")
                   (is (= 2 (count (:list @calls)))
                       "dispatch refreshes the resource inventory")
                   (is (nil? (.queryByRole r "button" #js {:name "Dispatch"}))
                       "the refreshed whole row removes stale server-authored actions")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done))))))))

(deftest server-inventory-project-controls-legacy-list-and-selected-scope
  (async done
    (set! api/list-publication-reviews
          (fn []
            (record! :publication-reviews true)
            (js/Promise.resolve
             {:project "knoxx-session"
              :reviews [(-> publication-review
                            (dissoc :translation_revision)
                            (assoc :project "knoxx-session"
                                   :work_state "missing"
                                   :reviewable false
                                   :allowed_actions ["dispatch"]))]})))
    (let [r (rtl/render ($ translation-review-page))]
      (-> (select-first-document r)
          (.then (fn []
                   (is (every? #(= "knoxx-session" (:project %)) (:list @calls))
                       "reviews load first, so no legacy query escapes to devel")
                   (is (= "knoxx-session"
                          (.-value (.getByPlaceholderText r "devel")))
                       "the visible project reflects server scope")
                   (is (= {:project "knoxx-session"
                           :garden-id "gardens/sonic"}
                          (nth (last (:get @calls)) 2))
                       "selected detail prefers inventory project and keeps garden")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest empty-inventory-envelope-project-controls-legacy-compatibility-flow
  (async done
    (set! api/list-publication-reviews
          (fn []
            (record! :publication-reviews true)
            (js/Promise.resolve {:project "knoxx-session" :reviews []})))
    (let [r (rtl/render ($ translation-review-page))]
      (-> (select-first-document r)
          (.then (fn []
                   (is (= [{:project "knoxx-session" :target-lang ""}]
                          (:list @calls))
                       "even an empty inventory scopes the compatibility query")
                   (is (= "knoxx-session"
                          (.-value (.getByPlaceholderText r "devel"))))
                   (is (= {:project "knoxx-session"
                           :garden-id "gardens/sonic"}
                          (nth (first (:get @calls)) 2)))
                   (is (some? (.queryByRole r "button" #js {:name "Approve All"})))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest ^:async unmatched-gardenless-worker-row-remains-in-legacy-compatibility
  (set! api/list-publication-reviews
        (fn []
          (record! :publication-reviews true)
          (js/Promise.resolve {:project "knoxx-session" :reviews []})))
  (set! api/list-documents
        (fn [params]
          (record! :list params)
          (js/Promise.resolve
           {:documents [{:document_id "docs/gardenless-worker"
                         :target_lang "es" :source_lang "en"
                         :project "knoxx-session" :title "Gardenless Worker"
                         :total_segments 2 :approved 1
                         :overall_status "partial_review"}]
            :total 1})))
  (let [r (rtl/render ($ translation-review-page))]
    (await
     (wait-until "gardenless compatibility row"
                 #(some? (.queryByText r "Gardenless Worker"))))
    (.click rtl/fireEvent (.getByText r "Gardenless Worker"))
    (await
     (wait-until "gardenless legacy detail"
                 #(some? (.queryByText r "Hola mundo"))))
    (is (= {:project "knoxx-session"}
           (nth (last (:get @calls)) 2))
        "omitted garden remains one exact compatibility coordinate")
    (is (some? (.queryByRole r "button" #js {:name "Approve All"})))
    (is (some? (.queryByRole r "button" #js {:name "Needs Edit"})))
    (is (some? (.queryByRole r "button" #js {:name "Reject All"})))
    (is (nil? (.queryByRole r "button"
                            #js {:name "Approve for publication"}))
        "worker evidence restores legacy review, never whole-publication approval")))

(deftest moved-source-candidate-fails-closed-in-both-review-panes
  (async done
    (set! api/list-publication-reviews
          (fn []
            (record! :publication-reviews true)
            (js/Promise.resolve
             {:project "devel"
              :reviews [(assoc publication-review
                               :work_state "stale"
                               :contract_candidate true
                               :reviewable false
                               :hydration_state "source_moved")]})))
    (let [r (rtl/render ($ translation-review-page))]
      (-> (wait-until "moved candidate row" #(some? (.queryByText r "Doc One")))
          (.then (fn []
                   (.click rtl/fireEvent (.getByText r "Doc One"))
                   (wait-until
                    "moved-source block"
                    #(seq (.queryAllByText
                           r
                           "The source revision changed after this candidate was created. Review and publication approval are blocked until a candidate is produced for the current source.")))))
          (.then (fn []
                   (is (empty? (:get @calls))
                       "source movement cannot fall through to same-named legacy bytes")
                   (doseq [control ["Approve for publication" "Approve All"
                                    "Needs Edit" "Reject All" "Submit review"
                                    "Submit as in review" "Mark rejected"]]
                     (is (nil? (.queryByRole r "button" #js {:name control}))
                         (str control " is absent for a moved source")))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest stale-project-list-and-manifest-responses-cannot-overwrite-newer-state
  (async done
    (let [old-documents (deferred)
          fresh-documents (deferred)
          old-manifest (deferred)
          fresh-manifest (deferred)]
      (set! api/list-publication-reviews
            (fn [] (js/Promise.resolve {:reviews []})))
      (set! api/list-documents
            (fn [{:keys [project] :as params}]
              (record! :list params)
              (let [pending (if (= "fresh" project)
                              fresh-documents
                              old-documents)]
                (-> (:promise pending)
                    (.then (fn [response]
                             (record! :document-response project)
                             response))))))
      (set! api/get-manifest
            (fn [project]
              (record! :manifest project)
              (let [pending (if (= "fresh" project) fresh-manifest old-manifest)]
                (-> (:promise pending)
                    (.then (fn [response]
                             (record! :manifest-response project)
                             response))))))
      (let [r (rtl/render ($ translation-review-page))]
        (-> (wait-until "initial project requests"
                        #(and (= 1 (count (:list @calls)))
                              (= 1 (count (:manifest @calls)))))
            (.then (fn []
                     (.change rtl/fireEvent (.getByPlaceholderText r "devel")
                              #js {:target #js {:value "fresh"}})
                     (wait-until
                      "fresh project requests"
                      #(and (some (fn [params] (= "fresh" (:project params)))
                                  (:list @calls))
                            (some #{"fresh"} (:manifest @calls))))))
            (.then (fn []
                     ((:resolve fresh-documents)
                      {:documents [{:document_id "docs/fresh" :target_lang "fr"
                                    :source_lang "en" :garden_id "gardens/sonic"
                                    :project "fresh" :title "Fresh Project Doc"
                                    :total_segments 1 :approved 0
                                    :overall_status "pending_review"}]
                       :total 1})
                     ((:resolve fresh-manifest)
                      {:languages {:fr {:approved 0 :total_segments 1}}})
                     (wait-until
                      "fresh project state"
                      #(and (some? (.queryByText r "Fresh Project Doc"))
                            (some? (.queryByRole r "option" #js {:name "Français"}))))))
            (.then (fn []
                     ((:resolve old-documents)
                      {:documents [{:document_id "docs/stale" :target_lang "es"
                                    :source_lang "en" :garden_id "gardens/sonic"
                                    :project "devel" :title "Stale Project Doc"
                                    :total_segments 1 :approved 0
                                    :overall_status "pending_review"}]
                       :total 1})
                     ((:resolve old-manifest)
                      {:languages {:es {:approved 1 :total_segments 1}}})
                     (wait-until
                      "stale responses settled"
                      #(and (= #{"devel" "fresh"}
                               (set (:document-response @calls)))
                            (= #{"devel" "fresh"}
                               (set (:manifest-response @calls)))))))
            (.then (fn []
                     (is (some? (.queryByText r "Fresh Project Doc")))
                     (is (nil? (.queryByText r "Stale Project Doc")))
                     (is (some? (.queryByRole r "option" #js {:name "Français"})))
                     (is (nil? (.queryByRole r "option" #js {:name "Español"})))
                     (done)))
            (.catch (fn [err] (is false (str "unexpected: " err)) (done))))))))

(deftest stale-language-list-response-cannot-repopulate-the-old-filter
  (async done
    (let [all-documents (deferred)
          french-documents (deferred)]
      (set! api/list-publication-reviews
            (fn [] (js/Promise.resolve {:project "devel" :reviews []})))
      (set! api/list-documents
            (fn [{:keys [target-lang] :as params}]
              (record! :list params)
              (let [pending (if (= "fr" target-lang)
                              french-documents
                              all-documents)]
                (-> (:promise pending)
                    (.then (fn [response]
                             (record! :language-response target-lang)
                             response))))))
      (set! api/get-manifest
            (fn [_]
              (js/Promise.resolve
               {:languages {:es {:approved 0 :total_segments 1}
                            :fr {:approved 0 :total_segments 1}}})))
      (let [r (rtl/render ($ translation-review-page))]
        (-> (wait-until "all-language request" #(= 1 (count (:list @calls))))
            (.then (fn []
                     (.change rtl/fireEvent (.getByLabelText r "Target Lang")
                              #js {:target #js {:value "fr"}})
                     (wait-until
                      "French request"
                      #(some (fn [params] (= "fr" (:target-lang params)))
                             (:list @calls)))))
            (.then (fn []
                     ((:resolve french-documents)
                      {:documents [{:document_id "docs/fr" :target_lang "fr"
                                    :source_lang "en" :garden_id "gardens/sonic"
                                    :project "devel" :title "French Filter Doc"
                                    :total_segments 1 :approved 0
                                    :overall_status "pending_review"}]
                       :total 1})
                     (wait-until "French result"
                                 #(some? (.queryByText r "French Filter Doc")))))
            (.then (fn []
                     ((:resolve all-documents)
                      {:documents [{:document_id "docs/all" :target_lang "es"
                                    :source_lang "en" :garden_id "gardens/sonic"
                                    :project "devel" :title "Stale All-Language Doc"
                                    :total_segments 1 :approved 0
                                    :overall_status "pending_review"}]
                       :total 1})
                     (wait-until "old filter response settled"
                                 #(= 2 (count (:language-response @calls))))))
            (.then (fn []
                     (is (some? (.queryByText r "French Filter Doc")))
                     (is (nil? (.queryByText r "Stale All-Language Doc")))
                     (done)))
            (.catch (fn [err] (is false (str "unexpected: " err)) (done))))))))

(deftest stale-detail-response-cannot-overwrite-a-newer-selection
  (async done
    (let [detail-a (deferred)
          detail-b (deferred)
          documents [{:document_id "docs/a" :target_lang "es"
                      :source_lang "en" :garden_id "gardens/sonic"
                      :project "devel" :title "Document A"
                      :total_segments 1 :approved 0
                      :overall_status "pending_review"}
                     {:document_id "docs/b" :target_lang "es"
                      :source_lang "en" :garden_id "gardens/sonic"
                      :project "devel" :title "Document B"
                      :total_segments 1 :approved 0
                      :overall_status "pending_review"}]]
      (set! api/list-publication-reviews
            (fn [] (js/Promise.resolve {:project "devel" :reviews []})))
      (set! api/list-documents
            (fn [_] (js/Promise.resolve {:documents documents :total 2})))
      (set! api/get-document
            (fn
              ([document-id _]
               (record! :get document-id)
               (:promise (if (= "docs/a" document-id) detail-a detail-b)))
              ([document-id _ _]
               (record! :get document-id)
               (let [pending (if (= "docs/a" document-id) detail-a detail-b)]
                 (-> (:promise pending)
                     (.then (fn [response]
                              (record! :detail-response document-id)
                              response)))))))
      (let [r (rtl/render ($ translation-review-page))
            detail-for (fn [title target]
                         {:document {:title title :source_lang "en"}
                          :summary {:total_segments 1 :approved 0
                                    :overall_status "pending_review"}
                          :segments [{:id (str title "/segment") :segment_index 0
                                      :status "pending" :source_lang "en"
                                      :target_lang "es" :source_text "Source"
                                      :translated_text target}]})]
        (-> (wait-until "both document cards"
                        #(and (some? (.queryByText r "Document A"))
                              (some? (.queryByText r "Document B"))))
            (.then (fn []
                     (.click rtl/fireEvent (.getByText r "Document A"))
                     (wait-until "A detail request" #(some #{"docs/a"} (:get @calls)))))
            (.then (fn []
                     (.click rtl/fireEvent (.getByText r "Document B"))
                     (wait-until "B detail request" #(some #{"docs/b"} (:get @calls)))))
            (.then (fn []
                     ((:resolve detail-b) (detail-for "Document B" "Fresh B target"))
                     (wait-until "B detail" #(some? (.queryByText r "Fresh B target")))))
            (.then (fn []
                     ((:resolve detail-a) (detail-for "Document A" "Stale A target"))
                     (wait-until "A response settled"
                                 #(some #{"docs/a"} (:detail-response @calls)))))
            (.then (fn []
                     (is (some? (.queryByText r "Fresh B target")))
                     (is (nil? (.queryByText r "Stale A target")))
                     (done)))
            (.catch (fn [err] (is false (str "unexpected: " err)) (done))))))))

(deftest async-refresh-preserves-the-reviewers-current-selection
  (async done
    (let [dispatch (deferred)
          inventory (atom
                     [{:publication "publications/a-es"
                       :document "docs/a" :garden "gardens/sonic"
                       :project "devel" :source_locale "en" :locale "es"
                       :title "Resource Row A" :revision "source-a"
                       :work_state "missing" :reviewable false
                       :allowed_actions ["dispatch"]}
                      {:publication "publications/b-es"
                       :document "docs/b" :garden "gardens/sonic"
                       :project "devel" :source_locale "en" :locale "es"
                       :title "Resource Row B" :revision "source-b"
                       :work_state "missing" :reviewable false
                       :allowed_actions []}])]
      (set! api/list-publication-reviews
            (fn []
              (record! :publication-reviews true)
              (js/Promise.resolve {:project "devel" :reviews @inventory})))
      (set! api/list-documents
            (fn [params]
              (record! :list params)
              (js/Promise.resolve {:documents [] :total 0})))
      (set! api/dispatch-publication-translation
            (fn [publication-id]
              (record! :publication-dispatch publication-id)
              (:promise dispatch)))
      (let [r (rtl/render ($ translation-review-page))]
        (-> (wait-until "resource rows"
                        #(and (some? (.queryByText r "Resource Row A"))
                              (some? (.queryByText r "Resource Row B"))))
            (.then (fn []
                     (.click rtl/fireEvent (.getByText r "Resource Row A"))
                     (wait-until "A selected"
                                 #(= 2 (count (.queryAllByText r "Resource Row A"))))))
            (.then (fn []
                     (.click rtl/fireEvent (.getByRole r "button" #js {:name "Dispatch"}))
                     (.click rtl/fireEvent (.getByText r "Resource Row B"))
                     (wait-until "B selected"
                                 #(= 2 (count (.queryAllByText r "Resource Row B"))))))
            (.then (fn []
                     (swap! inventory update 0 assoc
                            :work_state "in_flight" :allowed_actions [])
                     ((:resolve dispatch)
                      {:dispatched [{:outcome "dispatch/accepted"}]})
                     (wait-until "post-dispatch refresh"
                                 #(= 2 (count (:publication-reviews @calls))))))
            (.then (fn []
                     (wait-until "B remains selected"
                                 #(= 2 (count (.queryAllByText r "Resource Row B"))))))
            (.then (fn []
                     (is (= 1 (count (.queryAllByText r "Resource Row A")))
                         "the completed request does not restore its captured row A")
                     (is (= ["publications/a-es"] (:publication-dispatch @calls)))
                     (done)))
            (.catch (fn [err] (is false (str "unexpected: " err)) (done))))))))

(deftest pipeline-model-config-saves-patch
  (async done
    (let [r (rtl/render ($ translation-review-page))]
      (-> (wait-until "page" #(some? (.queryByText r "Translation Review")))
          (.then (fn []
                   (.click rtl/fireEvent (.getByRole r "button" #js {:name "⚙ Pipeline"}))
                   (wait-until "config loaded" #(seq (:config @calls)))))
          (.then (fn []
                   (wait-until "input ready" #(some? (.queryByPlaceholderText r "glm-5")))))
          (.then (fn []
                   (.change rtl/fireEvent (.getByPlaceholderText r "glm-5")
                            #js {:target #js {:value "gpt-5.4"}})
                   (.click rtl/fireEvent (.getByRole r "button" #js {:name "Save"}))
                   (wait-until "saved notice"
                               #(some? (.queryByText r "Translation model updated to gpt-5.4.")))))
          (.then (fn []
                   (is (= ["gpt-5.4"] (:update-config @calls)))
                   (is (= [true] (:models @calls)) "proxx models listed for datalist")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))
