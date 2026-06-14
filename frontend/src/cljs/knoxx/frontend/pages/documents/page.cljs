(ns knoxx.frontend.pages.documents.page
  "Documents/lakes page container. Helix port of src/pages/DocumentsPage.tsx.
   Exposed at window.knoxx.frontend.pages.documents.page.documents_page for
   the TS loader shim (OpsRoot is still TS-routed)."
  (:require [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.pages.documents.api :as api]
            [knoxx.frontend.pages.documents.logic :as logic]
            [knoxx.frontend.pages.documents.view :as view]))

(def ^:private blank-new-db
  {:name "" :use-local-docs true :forum-mode false
   :public-base-url "https://docs.example.com" :files []})

(defn- load-documents! [{:keys [set-documents!]}]
  (-> (api/fetch-documents)
      (.then #(set-documents! (vec (:documents %))))
      (.catch #(js/console.error %))))

(defn- load-databases! [{:keys [set-db-info! set-selected-db-id! set-edit-db!]}]
  (-> (api/list-databases)
      (.then (fn [data]
               (set-db-info! data)
               (set-selected-db-id! (:activeDatabaseId data))
               (let [active (first (filter #(= (:id %) (:activeDatabaseId data))
                                           (:databases data)))]
                 (set-edit-db! {:name (or (:name active) "")
                                :base-url (or (:publicDocsBaseUrl active) "")
                                :use-local-docs (boolean (:useLocalDocsBaseUrl active))
                                :forum-mode (boolean (:forumMode active))}))))
      (.catch #(js/console.error %))))

(defn- load-history! [{:keys [set-history!]}]
  (-> (api/ingestion-history)
      (.then #(set-history! (vec (:items %))))
      (.catch #(js/console.error %))))

(defn- poll-progress! [{:keys [set-ingesting! set-progress! set-samples!] :as setters}]
  (-> (api/ingestion-progress)
      (.then (fn [data]
               (if (or (:active data) (:canResumeForum data))
                 (do (set-ingesting! (boolean (:active data)))
                     (set-progress! (assoc (or (:progress data) {})
                                           :canResumeForum (boolean (:canResumeForum data))
                                           :stale (boolean (:stale data))))
                     (set-samples! (fn [prev]
                                     (logic/push-sample prev (.getTime (js/Date.))
                                                        (or (get-in data [:progress :processedChunks]) 0)))))
                 (do (set-ingesting! false)
                     (set-progress! nil)
                     (set-samples! [])
                     (load-history! setters)))))
      (.catch #(js/console.error %))))

(defn- create-database! [new-db {:keys [set-creating! set-new-db! set-selected-docs!] :as setters}]
  (let [nm (.trim (or (:name new-db) ""))]
    (when (seq nm)
      (set-creating! true)
      (-> (api/create-database
           (cond-> {:name nm
                    :activate true
                    :useLocalDocsBaseUrl (:use-local-docs new-db)
                    :forumMode (:forum-mode new-db)}
             (not (:use-local-docs new-db))
             (assoc :publicDocsBaseUrl (.trim (or (:public-base-url new-db) "")))))
          (.then (fn [_]
                   (if (seq (:files new-db))
                     (api/upload-documents (:files new-db) true)
                     (js/Promise.resolve nil))))
          (.then (fn [_]
                   (set-new-db! blank-new-db)
                   (set-selected-docs! #{})
                   (js/Promise.all #js [(load-databases! setters)
                                        (load-documents! setters)
                                        (load-history! setters)])))
          (.catch #(js/console.error "Create database failed:" %))
          (.finally #(set-creating! false))))))

(defn- restart-ingestion! [{:keys [set-restarting! set-message! set-ingesting!
                                   set-progress! set-samples! set-last-restart!]}]
  (set-restarting! true)
  (set-message! "")
  (-> (api/ingestion-progress)
      (.then (fn [before]
               (if (logic/no-active-run? before)
                 (do (set-ingesting! false)
                     (set-progress! nil)
                     (set-samples! [])
                     (set-message! "No active ingestion run to restart. Start a new ingest instead."))
                 (let [force-fresh? (logic/should-force-fresh? before)]
                   (-> (api/restart-ingestion force-fresh?)
                       (.then (fn [result]
                                (if (false? (:resumed result))
                                  (do (set-ingesting! false)
                                      (set-progress! nil)
                                      (set-samples! [])
                                      (set-message! (str (or (:message result)
                                                             "No active ingestion run to restart."))))
                                  (-> (api/ingestion-progress)
                                      (.then (fn [data]
                                               (set-ingesting! (boolean (:active data)))
                                               (set-progress! (when (:progress data)
                                                                (assoc (:progress data)
                                                                       :canResumeForum (boolean (:canResumeForum data))
                                                                       :stale (boolean (:stale data)))))
                                               (set-samples! [])
                                               (set-last-restart! (.getTime (js/Date.)))
                                               (set-message! (logic/restart-message force-fresh?)))))))))))))
      (.catch (fn [^js err]
                (if (logic/no-active-restart-error? (.-message err))
                  (do (set-ingesting! false)
                      (set-progress! nil)
                      (set-samples! [])
                      (set-message! "No active ingestion run to restart. Start a new ingest instead."))
                  (set-message! "Restart failed. Please try again or start a fresh ingest run."))))
      (.finally #(set-restarting! false))))

(defn- simple-action!
  "Runs `thunk` (a promise-returning fn) bracketed by a busy setter,
   reloading via `after!` on success."
  [busy-set! thunk after! label]
  (busy-set! true)
  (-> (thunk)
      (.then (fn [_] (after!)))
      (.catch #(js/console.error label %))
      (.finally #(busy-set! false))))

(defn- make-lake-handlers
  [{:keys [new-db selected-db-id db-info edit-db is-ingesting]}
   {:keys [set-selected-docs!] :as setters} flag! reload-all!]
  {:create! #(create-database! new-db setters)
   :activate! (fn []
                (when (and (not is-ingesting) (seq selected-db-id)
                           (not= selected-db-id (:activeDatabaseId db-info)))
                  (simple-action! (flag! :switching)
                                  #(api/activate-database selected-db-id)
                                  (fn [] (set-selected-docs! #{}) (reload-all!))
                                  "Switch database failed:")))
   :save! (fn []
            (when (seq selected-db-id)
              (simple-action! (flag! :saving)
                              #(api/update-database
                                selected-db-id
                                (cond-> {:useLocalDocsBaseUrl (:use-local-docs edit-db)
                                         :forumMode (:forum-mode edit-db)}
                                  (seq (.trim (or (:name edit-db) "")))
                                  (assoc :name (.trim (:name edit-db)))
                                  (and (not (:use-local-docs edit-db))
                                       (seq (.trim (or (:base-url edit-db) ""))))
                                  (assoc :publicDocsBaseUrl (.trim (:base-url edit-db)))))
                              #(load-databases! setters)
                              "Update database failed:")))
   :delete-db! (fn []
                 (when (and (seq selected-db-id)
                            (not= selected-db-id (:activeDatabaseId db-info))
                            (js/confirm "Delete this lake profile? This does not delete the underlying vector index, only the Knoxx lake profile."))
                   (simple-action! (flag! :deleting)
                                   #(api/delete-database selected-db-id)
                                   #(load-databases! setters)
                                   "Delete database failed:")))
   :make-private! (fn []
                    (when (and (seq selected-db-id)
                               (js/confirm "Make this lake profile private to your current browser session? Other sessions will no longer see it."))
                      (simple-action! (flag! :privatizing)
                                      #(api/make-database-private selected-db-id)
                                      #(load-databases! setters)
                                      "Make private failed:")))})

(defn- make-doc-handlers
  [{:keys [selected-docs documents]}
   {:keys [set-uploading! set-selected-docs! set-ingesting!] :as setters}]
  {:upload! (fn [^js event auto?]
              (let [files (vec (js/Array.from (or (.. event -target -files) #js [])))]
                (when (seq files)
                  (set-uploading! true)
                  (-> (api/upload-documents files auto?)
                      (.then (fn [_] (load-documents! setters)))
                      (.catch #(js/console.error "Upload failed:" %))
                      (.finally (fn []
                                  (set-uploading! false)
                                  (set! (.. event -target -value) "")))))))
   :ingest-selected! (fn []
                       (when (seq selected-docs)
                         (set-ingesting! true)
                         (-> (api/ingest-documents {:selectedFiles (vec selected-docs)})
                             (.catch (fn [err]
                                       (js/console.error "Ingest failed:" err)
                                       (set-ingesting! false))))))
   :ingest-all! (fn []
                  (set-ingesting! true)
                  (-> (api/ingest-documents {:full true})
                      (.catch (fn [err]
                                (js/console.error "Ingest failed:" err)
                                (set-ingesting! false)))))
   :restart! #(restart-ingestion! setters)
   :toggle-all! #(set-selected-docs! (logic/toggle-all selected-docs documents))
   :toggle-doc! #(set-selected-docs! (logic/toggle-doc selected-docs %))
   :delete-doc! (fn [path]
                  (when (js/confirm (str "Are you sure you want to delete " path "?"))
                    (-> (api/delete-document path)
                        (.then (fn [_]
                                 (load-documents! setters)
                                 (set-selected-docs! #(disj % path))))
                        (.catch #(js/console.error "Delete failed:" %)))))})

(defn- make-handlers [state setters flag! reload-all!]
  (merge (make-lake-handlers state setters flag! reload-all!)
         (make-doc-handlers state setters)))

(defnc lake-profiles-section
  [{:keys [state setters handlers can-access?]}]
  (let [{:keys [is-ingesting db-info selected-db-id new-db edit-db flags]} state
        {:keys [set-selected-db-id! set-new-db! set-edit-db!]} setters]
    (d/div {:class-name "rounded-md border border-slate-700 bg-slate-900 p-4 space-y-3"}
           (d/h2 {:class-name "text-sm font-semibold uppercase tracking-wide text-slate-300"}
                 "Lake Runtime Profiles")
           (d/div {:class-name "grid gap-3 md:grid-cols-3"}
                  ($ view/lake-select-card {:db-info db-info
                                            :selected-db-id selected-db-id
                                            :set-selected-db-id set-selected-db-id!
                                            :selected-db-can-access can-access?
                                            :is-ingesting is-ingesting
                                            :is-switching (:switching flags)
                                            :on-activate (:activate! handlers)})
                  ($ view/lake-create-card {:new-db new-db :set-new-db set-new-db!
                                            :is-creating (:creating flags)
                                            :on-create (:create! handlers)}))
           ($ view/lake-edit-card {:edit-db edit-db :set-edit-db set-edit-db!
                                   :selected-db-id selected-db-id
                                   :selected-db-can-access can-access?
                                   :db-info db-info
                                   :is-ingesting is-ingesting
                                   :is-saving (:saving flags)
                                   :is-deleting (:deleting flags)
                                   :is-privatizing (:privatizing flags)
                                   :on-save (:save! handlers)
                                   :on-delete (:delete-db! handlers)
                                   :on-make-private (:make-private! handlers)}))))

(defnc documents-page* [{:keys [state setters handlers]}]
  (let [{:keys [documents selected-docs is-uploading is-ingesting progress db-info
                selected-db-id message samples last-restart history]} state
        selected-db (first (filter #(= (:id %) selected-db-id) (:databases db-info)))
        can-access? (if selected-db (not (false? (:canAccess selected-db))) true)
        elapsed (if-let [started (:startedAt progress)]
                  (max 1 (/ (- (.getTime (js/Date.)) (.getTime (js/Date. started))) 1000))
                  0)
        rate (logic/chunks-per-sec samples progress elapsed)
        remaining (logic/remaining-chunks progress)
        flags (:flags state)]
    (d/div {:class-name "p-8 max-w-5xl mx-auto space-y-6 text-slate-100"}
           ($ view/page-header {:is-uploading is-uploading :on-upload (:upload! handlers)})
           ($ lake-profiles-section {:state state :setters setters
                                     :handlers handlers :can-access? can-access?})
           (when (and is-ingesting progress)
             ($ view/progress-banner {:progress progress :chunks-per-sec rate
                                      :remaining-chunks remaining
                                      :eta-seconds (logic/eta-seconds remaining rate)
                                      :last-restart-at last-restart}))
           (when (and (not is-ingesting) (:canResumeForum progress))
             ($ view/resume-banner {:progress progress}))
           ($ view/ingest-actions {:selected-count (count selected-docs)
                                   :is-ingesting is-ingesting
                                   :can-restart (or is-ingesting (boolean (:canResumeForum progress)))
                                   :is-restarting (:restarting flags)
                                   :stale? (boolean (:stale progress))
                                   :on-ingest-selected (:ingest-selected! handlers)
                                   :on-ingest-all (:ingest-all! handlers)
                                   :on-restart (:restart! handlers)})
           (when (seq (or message ""))
             (d/div {:class-name "rounded-md border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200"}
                    message))
           ($ view/documents-table {:documents documents :selected-docs selected-docs
                                    :on-toggle-all (:toggle-all! handlers)
                                    :on-toggle-doc (:toggle-doc! handlers)
                                    :on-delete-doc (:delete-doc! handlers)})
           ($ view/history-table {:items history}))))

(defnc ^:export documents-page []
  (let [[documents set-documents!] (hooks/use-state [])
        [selected-docs set-selected-docs!] (hooks/use-state #{})
        [is-uploading set-uploading!] (hooks/use-state false)
        [is-ingesting set-ingesting!] (hooks/use-state false)
        [progress set-progress!] (hooks/use-state nil)
        [db-info set-db-info!] (hooks/use-state nil)
        [selected-db-id set-selected-db-id!] (hooks/use-state "")
        [new-db set-new-db!] (hooks/use-state blank-new-db)
        [edit-db set-edit-db!] (hooks/use-state {:name "" :base-url "" :use-local-docs true :forum-mode false})
        [flags set-flags!] (hooks/use-state {})
        [message set-message!] (hooks/use-state "")
        [samples set-samples!] (hooks/use-state [])
        [last-restart set-last-restart!] (hooks/use-state nil)
        [history set-history!] (hooks/use-state [])
        flag! (fn [k] (fn [v] (set-flags! #(assoc % k v))))
        setters {:set-documents! set-documents! :set-selected-docs! set-selected-docs!
                 :set-db-info! set-db-info! :set-selected-db-id! set-selected-db-id!
                 :set-new-db! set-new-db! :set-edit-db! set-edit-db!
                 :set-ingesting! set-ingesting! :set-progress! set-progress!
                 :set-samples! set-samples! :set-history! set-history!
                 :set-message! set-message! :set-last-restart! set-last-restart!
                 :set-uploading! set-uploading!
                 :set-creating! (flag! :creating) :set-restarting! (flag! :restarting)}
        reload-all! (fn [] (js/Promise.all #js [(load-databases! setters)
                                                (load-documents! setters)
                                                (load-history! setters)]))
        handlers (make-handlers
                  {:new-db new-db :selected-docs selected-docs :documents documents
                   :selected-db-id selected-db-id :db-info db-info :edit-db edit-db
                   :is-ingesting is-ingesting}
                  setters flag! reload-all!)]
    (hooks/use-effect
     []
     (load-documents! setters)
     (load-databases! setters)
     (load-history! setters)
     (let [timer (js/setInterval #(poll-progress! setters) 2000)]
       (fn [] (js/clearInterval timer))))
    (hooks/use-effect
     [db-info selected-db-id]
     (when-let [selected (first (filter #(= (:id %) selected-db-id) (:databases db-info)))]
       (set-edit-db! {:name (or (:name selected) "")
                      :base-url (or (:publicDocsBaseUrl selected) "")
                      :use-local-docs (boolean (:useLocalDocsBaseUrl selected))
                      :forum-mode (boolean (:forumMode selected))}))
     nil)
    ($ documents-page* {:state {:documents documents :selected-docs selected-docs
                                :is-uploading is-uploading :is-ingesting is-ingesting
                                :progress progress :db-info db-info
                                :selected-db-id selected-db-id :new-db new-db
                                :edit-db edit-db :flags flags :message message
                                :samples samples :last-restart last-restart
                                :history history}
                        :setters setters
                        :handlers handlers})))
