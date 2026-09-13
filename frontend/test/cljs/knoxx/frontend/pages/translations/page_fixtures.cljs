(ns knoxx.frontend.pages.translations.page-fixtures
  "Shared deterministic boundaries for translation workspace interaction scenarios."
  (:require ["@testing-library/react" :as rtl] [knoxx.frontend.pages.translations.api :as api]))

(def doc-summary
  "doc summary fixture."
  {:document_id "docs/doc-1"
   :target_lang "es"
   :title "Doc One"
   :overall_status "pending_review"
   :source_lang "en"
   :garden_id "gardens/sonic"
   :project "devel"
   :approved 1
   :total_segments 2})

(def publication-review
  "publication review fixture."
  {:publication "publications/doc-1-es"
   :document "docs/doc-1"
   :garden "gardens/sonic"
   :locale "es"
   :project "devel"
   :source_locale "en"
   :title "Doc One"
   :revision "source-sha"
   :translation_revision "translation-sha"
   :translated_at "2026-08-26T10:00:00.000Z"
   :approved false})

(def resource-split-review
  "resource split review fixture."
  {:candidate_set_id "candidate-set/doc-1-es"
   :manifest_id "manifest/doc-1-es"
   :status "partial-review"
   :splits [{:split_id "split/doc-1/0"
     :split_index 0
     :source_text "Hello world"
     :candidate_text "Hola mundo"
     :review_status "in-review"
     :adequacy "adequate"
     :fluency "good"
     :terminology "minor_errors"
     :risk "sensitive"
     :editor_notes "Confirm product terminology"} {:split_id "split/doc-1/1"
     :split_index 1
     :source_text "Bye"
     :candidate_text "Adiós"
     :review_status "approved"}]})

(def doc-detail
  "doc detail fixture."
  {:document {:title "Doc One" :source_lang "en"}
   :summary {:total_segments 2 :approved 1 :overall_status "partial_review"}
   :segments [{:id "seg-a"
     :segment_index 0
     :status "pending"
     :source_lang "en"
     :target_lang "es"
     :source_text "Hello world"
     :translated_text "Hola mundo"
     :label_count 0} {:id "seg-b"
     :segment_index 1
     :status "approved"
     :source_lang "en"
     :target_lang "es"
     :source_text "Bye"
     :translated_text "Adiós"
     :label_count 1}]})

(def calls "calls fixture." (atom {}))

(defn record!
  "Capture one boundary call for exact request assertions."
  [k v]
  (swap! calls update k (fnil conj []) v))

(def ^:private originals
  {:list api/list-documents
   :get api/get-document
   :review api/review-document
   :list-publication-reviews api/list-publication-reviews
   :dispatch-publication api/dispatch-publication-translation
   :approve-publication api/approve-publication-translation
   :split-review api/submit-publication-split-review
   :bulk-review api/submit-publication-bulk-review
   :reconcile-publication api/reconcile-publication
   :label api/submit-label
   :manifest api/get-manifest
   :sft api/sft-export
   :config api/pipeline-config
   :update-config api/update-pipeline-config
   :models api/list-proxx-models})

(def api-fixture
  "Reset and restore the authenticated translation HTTP boundaries."
  {:before (fn
     []
     (reset! calls {})
     (set! api/list-documents
       (fn
         [params]
         (record! :list params)
         (js/Promise.resolve {:documents [(assoc doc-summary :project (:project params))] :total 1})))
     (set! api/get-document
       (fn
         ([id lang] (record! :get [id lang nil]) (js/Promise.resolve doc-detail))
         ([id lang scope] (record! :get [id lang scope]) (js/Promise.resolve doc-detail))))
     (set! api/list-publication-reviews
       (fn [] (record! :publication-reviews true) (js/Promise.resolve {:project "devel" :reviews []})))
     (set! api/dispatch-publication-translation
       (fn
         [publication-id]
         (record! :publication-dispatch publication-id)
         (js/Promise.resolve {:dispatched [{:outcome "dispatch/accepted"}]})))
     (set! api/approve-publication-translation
       (fn
         [payload]
         (record! :publication-approval payload)
         (js/Promise.resolve {:approved true :status "recorded"})))
     (set! api/submit-publication-split-review
       (fn [payload] (record! :publication-split-review payload) (js/Promise.resolve {:status "recorded"})))
     (set! api/submit-publication-bulk-review
       (fn [payload] (record! :publication-bulk-review payload) (js/Promise.resolve {:status "recorded"})))
     (set! api/reconcile-publication
       (fn
         [publication-id]
         (record! :reconcile publication-id)
         (js/Promise.resolve {:type "publication/materialized"})))
     (set! api/review-document
       (fn
         ([id lang payload]
           (record! :review [id lang nil payload])
           (js/Promise.resolve {:ok true :segments_reviewed 2 :overall (:overall payload)}))
         ([id lang scope payload]
           (record! :review [id lang scope payload])
           (js/Promise.resolve {:ok true :segments_reviewed 2 :overall (:overall payload)}))))
     (set! api/submit-label
       (fn
         ([seg-id payload]
           (record! :label [seg-id nil payload])
           (js/Promise.resolve {:ok true :new_status "approved"}))
         ([seg-id scope payload]
           (record! :label [seg-id scope payload])
           (js/Promise.resolve {:ok true :new_status "approved"}))))
     (set! api/get-manifest
       (fn
         [project]
         (record! :manifest project)
         (js/Promise.resolve {:languages {:es {:approved 1 :total_segments 2}}})))
     (set! api/pipeline-config
       (fn [] (record! :config true) (js/Promise.resolve {:model "glm-5" :updated_at nil})))
     (set! api/update-pipeline-config
       (fn [model] (record! :update-config model) (js/Promise.resolve {:model model :updated_at "2026-06-11"})))
     (set! api/list-proxx-models
       (fn [] (record! :models true) (js/Promise.resolve [{:id "glm-5"} {:id "gpt-5.4"}]))))
   :after (fn
     []
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

(defn wait-until
  "Wait for an observable UI or request boundary to settle."
  [msg pred]
  (rtl/waitFor (fn [] (when-not (pred) (throw (js/Error. (str "still waiting: " msg)))))))

(defn deferred
  "Create a controllable Promise for out-of-order response scenarios."
  []
  (let
    [resolve! (atom nil)
     reject! (atom nil)
     promise (js/Promise. (fn [complete reject] (reset! resolve! complete) (reset! reject! reject)))]
    {:promise promise :resolve #(@resolve! %) :reject #(@reject! %)}))

(defn ^:async select-first-document
  "Open the first real fixture document after its inventory arrives."
  [^js r]
  (await (wait-until "doc list" #(some? (.queryByText r "Doc One"))))
  (.click rtl/fireEvent (.getByText r "Doc One"))
  (await (wait-until "detail" #(some? (.queryByText r "Hola mundo")))))

(defn install-resource-split-inventory!
  "Install an evolving resource candidate inventory."
  [inventory]
  (set! api/list-publication-reviews
    (fn [] (record! :publication-reviews true) (js/Promise.resolve {:project "devel" :reviews @inventory}))))

(defn resource-review-row
  "Build one hydrated resource candidate from its persisted split set."
  [aggregate]
  (assoc publication-review :work_state "ready" :split_review aggregate))
