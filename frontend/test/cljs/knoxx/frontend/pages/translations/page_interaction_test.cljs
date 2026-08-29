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
            [knoxx.frontend.pages.translations.view :refer [translation-review-page]]))

;; jsdom globals come from the :test build's :prepend-js.

(def doc-summary
  {:document_id "docs/doc-1" :target_lang "es" :title "Doc One"
   :overall_status "pending_review" :source_lang "en" :garden_id "gardens/sonic"
   :approved 1 :total_segments 2})

(def publication-review
  {:publication "publications/doc-1-es"
   :document "docs/doc-1" :garden "gardens/sonic" :locale "es"
   :revision "source-sha" :translation_revision "translation-sha"
   :translated_at "2026-08-26T10:00:00.000Z" :approved false})

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
   :approve-publication api/approve-publication-translation
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
                     (js/Promise.resolve {:documents [doc-summary] :total 1})))
             (set! api/get-document
                   (fn [id lang]
                     (record! :get [id lang])
                     (js/Promise.resolve doc-detail)))
             (set! api/list-publication-reviews
                   (fn []
                     (record! :publication-reviews true)
                     (js/Promise.resolve {:reviews [publication-review]})))
             (set! api/approve-publication-translation
                   (fn [payload]
                     (record! :publication-approval payload)
                     (js/Promise.resolve {:approved true :status "recorded"})))
             (set! api/reconcile-publication
                   (fn [publication-id]
                     (record! :reconcile publication-id)
                     (js/Promise.resolve {:type "publication/materialized"})))
             (set! api/review-document
                   (fn [id lang payload]
                     (record! :review [id lang payload])
                     (js/Promise.resolve {:ok true :segments_reviewed 2 :overall (:overall payload)})))
             (set! api/submit-label
                   (fn [seg-id payload]
                     (record! :label [seg-id payload])
                     (js/Promise.resolve {:ok true :new_status "approved"})))
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
            (set! api/approve-publication-translation (:approve-publication originals))
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

(defn- select-first-document [^js r]
  (-> (wait-until "doc list" #(some? (.queryByText r "Doc One")))
      (.then (fn [] (.click rtl/fireEvent (.getByText r "Doc One"))
               (wait-until "detail" #(some? (.queryByText r "Hola mundo")))))))

(deftest loads-documents-and-detail-on-selection
  (async done
    (let [r (rtl/render ($ translation-review-page))]
      (-> (select-first-document r)
          (.then (fn []
                   (is (= {:project "devel" :target-lang ""} (first (:list @calls))))
                   (is (= "devel" (first (:manifest @calls))))
                   (is (= [["docs/doc-1" "es"]] (:get @calls)))
                   (is (some? (.queryByText r "Hello world")) "segment source shown")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

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
                   (let [[seg-id payload] (first (:label @calls))]
                     (is (= "seg-a" seg-id))
                     (is (= "approve" (:overall payload)))
                     (is (not (contains? payload :corrected_text)) "blank correction omitted"))
                   (is (= 2 (count (:get @calls))) "detail reloaded after label")
                   (is (= 2 (count (:list @calls))) "list reloaded after label")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest document-level-review-approves-all
  (async done
    (let [r (rtl/render ($ translation-review-page))]
      (-> (select-first-document r)
          (.then (fn []
                   (.click rtl/fireEvent (.getByRole r "button" #js {:name "Approve All"}))
                   (wait-until "notice" #(some? (.queryByText r "Document review: approve (2 segments)")))))
          (.then (fn []
                   (is (= [["docs/doc-1" "es" {:overall "approve"}]] (:review @calls)))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest publication-approval-uses-revision-bound-evidence
  (async done
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
