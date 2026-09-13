(ns knoxx.frontend.pages.documents.page-interaction-test
  "Written FIRST (TDD) — interaction flows for the Helix DocumentsPage
  (data lakes): initial load, selection→ingest, lake creation, restart
  with no active run, and confirmed document deletion. API ns mocked via
  set!; js/confirm stubbed."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as test]
            [helix.core :as hx]
            [knoxx.frontend.pages.documents.api :as api]
            [knoxx.frontend.pages.documents.page :as page]))

;; jsdom globals come from the :test build's :prepend-js.

(def ^:private docs
  [{:relativePath "a.md" :name "Alpha doc" :size 2048 :indexed true :chunkCount 4}
   {:relativePath "b.md" :name "Beta doc" :size 1024 :indexed false}])

(def ^:private db-info
  {:activeDatabaseId "lake-1"
   :databases [{:id "lake-1" :name "Primary" :qdrantCollection "col-1"
                :useLocalDocsBaseUrl true :forumMode false :canAccess true}
               {:id "lake-2" :name "Secondary" :qdrantCollection "col-2"
                :useLocalDocsBaseUrl true :forumMode false :canAccess true}]
   :activeRuntime {:docsPath "/lakes/primary"}})

(def ^:private calls (atom {}))
(defn- record! [k v] (swap! calls update k (fnil conj []) v))
(def ^:private progress-response (atom {:active false :canResumeForum false}))

(def ^:private originals
  {:confirm (.-confirm js/globalThis) :docs api/fetch-documents :upload api/upload-documents :del api/delete-document
   :ingest api/ingest-documents :restart api/restart-ingestion
   :progress api/ingestion-progress :history api/ingestion-history
   :list-db api/list-databases :create-db api/create-database
   :activate api/activate-database :update-db api/update-database
   :delete-db api/delete-database :private api/make-database-private})

(test/use-fixtures :each
  {:before (fn []
             (reset! calls {})
             (reset! progress-response {:active false :canResumeForum false})
             (set! (.-confirm js/globalThis) (fn [_] true))
             (set! api/fetch-documents (fn [] (js/Promise.resolve {:documents docs})))
             (set! api/upload-documents (fn [files auto] (record! :upload [files auto]) (js/Promise.resolve {:ok true})))
             (set! api/delete-document (fn [path] (record! :del path) (js/Promise.resolve {:ok true})))
             (set! api/ingest-documents (fn [opts] (record! :ingest opts) (js/Promise.resolve {:ok true})))
             (set! api/restart-ingestion (fn [force-fresh?] (record! :restart force-fresh?) (js/Promise.resolve {:resumed true})))
             (set! api/ingestion-progress (fn [] (js/Promise.resolve @progress-response)))
             (set! api/ingestion-history (fn [] (js/Promise.resolve {:items []})))
             (set! api/list-databases (fn [] (record! :list-db true) (js/Promise.resolve db-info)))
             (set! api/create-database (fn [payload] (record! :create-db payload) (js/Promise.resolve {:ok true})))
             (set! api/activate-database (fn [id] (record! :activate id) (js/Promise.resolve {:ok true})))
             (set! api/update-database (fn [id payload] (record! :update-db [id payload]) (js/Promise.resolve {:ok true})))
             (set! api/delete-database (fn [id] (record! :delete-db id) (js/Promise.resolve {:ok true})))
             (set! api/make-database-private (fn [id] (record! :private id) (js/Promise.resolve {:ok true}))))
   :after (fn []
            (rtl/cleanup)
            (set! (.-confirm js/globalThis) (:confirm originals))
            (set! api/fetch-documents (:docs originals))
            (set! api/upload-documents (:upload originals))
            (set! api/delete-document (:del originals))
            (set! api/ingest-documents (:ingest originals))
            (set! api/restart-ingestion (:restart originals))
            (set! api/ingestion-progress (:progress originals))
            (set! api/ingestion-history (:history originals))
            (set! api/list-databases (:list-db originals))
            (set! api/create-database (:create-db originals))
            (set! api/activate-database (:activate originals))
            (set! api/update-database (:update-db originals))
            (set! api/delete-database (:delete-db originals))
            (set! api/make-database-private (:private originals)))})

(defn- wait-until
  ([msg pred] (wait-until msg pred nil))
  ([msg pred opts]
   (rtl/waitFor (fn [] (when-not (pred) (throw (js/Error. (str "still waiting: " msg)))))
                (clj->js (or opts {})))))

(defn- render-page []
  (rtl/render (hx/$ page/documents-page)))

(test/deftest ^:async loads-documents-and-lakes
  (let [r (render-page)]
    (await (wait-until "doc row" #(some? (.queryByText r "Alpha doc"))))
    (test/is (some? (.queryByText r "Indexed (4 chunks)")))
    (test/is (some? (.queryByText r "Pending")))
    (test/is (some? (.queryByText r "Mounted docs path: /lakes/primary")))
    (test/is (= [true] (:list-db @calls)))))

(test/deftest ^:async select-and-ingest-selected
  (let [r (render-page)]
    (await (wait-until "doc row" #(some? (.queryByText r "Alpha doc"))))
    (let [row (.closest (.getByText r "Alpha doc") "tr")
          box (.querySelector row "input[type=checkbox]")]
      (.click rtl/fireEvent box))
    (await (wait-until "count updates" #(some? (.queryByText r "Ingest Selected (1)"))))
    (.click rtl/fireEvent (.getByText r "Ingest Selected (1)"))
    (await (wait-until "ingest called" #(seq (:ingest @calls))))
    (test/is (= [{:selectedFiles ["a.md"]}] (:ingest @calls)))))

(test/deftest ^:async creates-lake-profile
  (let [r (render-page)]
    (await (wait-until "create input" #(some? (.queryByPlaceholderText r "e.g. Engine Manuals"))))
    (.change rtl/fireEvent (.getByPlaceholderText r "e.g. Engine Manuals")
             #js {:target #js {:value "New Lake"}})
    (.click rtl/fireEvent (.getByRole r "button" #js {:name "Create"}))
    (await (wait-until "created" #(seq (:create-db @calls))))
    (test/is (= [{:name "New Lake" :activate true
                 :useLocalDocsBaseUrl true :forumMode false}]
               (:create-db @calls))
             "local-docs default omits publicDocsBaseUrl")
    (test/is (= 2 (count (:list-db @calls))) "lakes reloaded")))

(test/deftest ^:async restart-with-no-active-run-messages
  (let [r (render-page)]
    (await (wait-until "doc row" #(some? (.queryByText r "Alpha doc"))))
    ;; Enable restart through a resumable checkpoint observed by the 2s poll.
    (reset! progress-response {:active false :canResumeForum true
                               :progress {:percent 10 :currentFile "x.md"}})
    (await (wait-until "resume banner"
                       #(some? (.queryByText r "Resumable Forum Ingestion Found"))
                       {:timeout 4000}))
    ;; The run disappears between the last poll and the restart request.
    (reset! progress-response {:active false :canResumeForum false})
    (.click rtl/fireEvent (.getByRole r "button" #js {:name "Restart Ingestion (Resume)"}))
    (await (wait-until "no-active message"
                       #(some? (.queryByText r "No active ingestion run to restart. Start a new ingest instead."))))
    (test/is (empty? (:restart @calls)) "restart API not called without an active run")))

(test/deftest ^:async delete-doc-confirms-and-reloads
  (let [r (render-page)]
    (await (wait-until "doc row" #(some? (.queryByText r "Alpha doc"))))
    (.click rtl/fireEvent (first (.getAllByText r "Delete")))
    (await (wait-until "deleted" #(seq (:del @calls))))
    (test/is (= ["a.md"] (:del @calls)))))
