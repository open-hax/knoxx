(ns knoxx.frontend.pages.documents.page-interaction-test
  "Written FIRST (TDD) — interaction flows for the Helix DocumentsPage
  (data lakes): initial load, selection→ingest, lake creation, restart
  with no active run, and confirmed document deletion. API ns mocked via
  set!; js/confirm stubbed."
  (:require [cljs.test :refer [deftest is async use-fixtures]]
            ["@testing-library/react" :as rtl]
            [helix.core :refer [$]]
            [knoxx.frontend.pages.documents.api :as api]
            [knoxx.frontend.pages.documents.page :refer [documents-page]]))

;; jsdom globals come from the :test build's :prepend-js.

(def docs
  [{:relativePath "a.md" :name "Alpha doc" :size 2048 :indexed true :chunkCount 4}
   {:relativePath "b.md" :name "Beta doc" :size 1024 :indexed false}])

(def db-info
  {:activeDatabaseId "lake-1"
   :databases [{:id "lake-1" :name "Primary" :qdrantCollection "col-1"
                :useLocalDocsBaseUrl true :forumMode false :canAccess true}
               {:id "lake-2" :name "Secondary" :qdrantCollection "col-2"
                :useLocalDocsBaseUrl true :forumMode false :canAccess true}]
   :activeRuntime {:docsPath "/lakes/primary"}})

(def calls (atom {}))
(defn- record! [k v] (swap! calls update k (fnil conj []) v))
(def progress-response (atom {:active false :canResumeForum false}))

(def ^:private originals
  {:docs api/fetch-documents :upload api/upload-documents :del api/delete-document
   :ingest api/ingest-documents :restart api/restart-ingestion
   :progress api/ingestion-progress :history api/ingestion-history
   :list-db api/list-databases :create-db api/create-database
   :activate api/activate-database :update-db api/update-database
   :delete-db api/delete-database :private api/make-database-private})

(use-fixtures :each
  {:before (fn []
             (reset! calls {})
             (reset! progress-response {:active false :canResumeForum false})
             (set! (.-confirm js/globalThis) (fn [_] true))
             (set! api/fetch-documents (fn [] (js/Promise.resolve {:documents docs})))
             (set! api/upload-documents (fn [files auto] (record! :upload [files auto]) (js/Promise.resolve {:ok true})))
             (set! api/delete-document (fn [path] (record! :del path) (js/Promise.resolve {:ok true})))
             (set! api/ingest-documents (fn [opts] (record! :ingest opts) (js/Promise.resolve {:ok true})))
             (set! api/restart-ingestion (fn [force] (record! :restart force) (js/Promise.resolve {:resumed true})))
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
  (rtl/render ($ documents-page)))

(deftest loads-documents-and-lakes
  (async done
    (let [r (render-page)]
      (-> (wait-until "doc row" #(some? (.queryByText r "Alpha doc")))
          (.then (fn []
                   (is (some? (.queryByText r "Indexed (4 chunks)")))
                   (is (some? (.queryByText r "Pending")))
                   (is (some? (.queryByText r "Mounted docs path: /lakes/primary")))
                   (is (= [true] (:list-db @calls)))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest select-and-ingest-selected
  (async done
    (let [r (render-page)]
      (-> (wait-until "doc row" #(some? (.queryByText r "Alpha doc")))
          (.then (fn []
                   ;; the Alpha row's checkbox lives inside its <tr>
                   (let [row (.closest (.getByText r "Alpha doc") "tr")
                         box (.querySelector row "input[type=checkbox]")]
                     (.click rtl/fireEvent box))
                   (wait-until "count updates" #(some? (.queryByText r "Ingest Selected (1)")))))
          (.then (fn []
                   (.click rtl/fireEvent (.getByText r "Ingest Selected (1)"))
                   (wait-until "ingest called" #(seq (:ingest @calls)))))
          (.then (fn []
                   (is (= [{:selectedFiles ["a.md"]}] (:ingest @calls)))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest creates-lake-profile
  (async done
    (let [r (render-page)]
      (-> (wait-until "create input" #(some? (.queryByPlaceholderText r "e.g. Engine Manuals")))
          (.then (fn []
                   (.change rtl/fireEvent (.getByPlaceholderText r "e.g. Engine Manuals")
                            #js {:target #js {:value "New Lake"}})
                   (.click rtl/fireEvent (.getByRole r "button" #js {:name "Create"}))
                   (wait-until "created" #(seq (:create-db @calls)))))
          (.then (fn []
                   (is (= [{:name "New Lake" :activate true
                            :useLocalDocsBaseUrl true :forumMode false}]
                          (:create-db @calls))
                       "local-docs default omits publicDocsBaseUrl")
                   (is (= 2 (count (:list-db @calls))) "lakes reloaded")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest restart-with-no-active-run-messages
  (async done
    (let [r (render-page)]
      (-> (wait-until "doc row" #(some? (.queryByText r "Alpha doc")))
          (.then (fn []
                   ;; restart button is disabled with no active run; force the
                   ;; handler path by simulating a resumable checkpoint first
                   (reset! progress-response {:active false :canResumeForum true
                                              :progress {:percent 10 :currentFile "x.md"}})
                   ;; the banner appears on the next 2s progress poll
                   (wait-until "resume banner"
                               #(some? (.queryByText r "Resumable Forum Ingestion Found"))
                               {:timeout 4000})))
          (.then (fn []
                   ;; now simulate the run disappearing before restart
                   (reset! progress-response {:active false :canResumeForum false})
                   (.click rtl/fireEvent (.getByRole r "button" #js {:name "Restart Ingestion (Resume)"}))
                   (wait-until "no-active message"
                               #(some? (.queryByText r "No active ingestion run to restart. Start a new ingest instead.")))))
          (.then (fn []
                   (is (empty? (:restart @calls)) "restart API not called without an active run")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest delete-doc-confirms-and-reloads
  (async done
    (let [r (render-page)]
      (-> (wait-until "doc row" #(some? (.queryByText r "Alpha doc")))
          (.then (fn []
                   (.click rtl/fireEvent (first (.getAllByText r "Delete")))
                   (wait-until "deleted" #(seq (:del @calls)))))
          (.then (fn []
                   (is (= ["a.md"] (:del @calls)))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))
