(ns knoxx.frontend.pages.documents.page
  "Documents/lakes page container. Helix port of src/pages/DocumentsPage.tsx.
   Exposed at window.knoxx.frontend.pages.documents.page.documents_page for
   the TS loader shim (OpsRoot is still TS-routed)."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.pages.documents.api :as api]
            [knoxx.frontend.pages.documents.logic :as logic]
            [knoxx.frontend.pages.documents.view :as view]))

(def ^:private blank-new-db
  {:name "" :use-local-docs true :forum-mode false
   :public-base-url "https://docs.example.com" :files []})

(defn- ^:async load-documents! [{:keys [set-documents!]}]
  (try (set-documents! (vec (:documents (await (api/fetch-documents)))))
       (catch :default error (js/console.error error))))

(defn- editable-profile [profile]
  {:name (or (:name profile) "")
   :base-url (or (:publicDocsBaseUrl profile) "")
   :use-local-docs (boolean (:useLocalDocsBaseUrl profile))
   :forum-mode (boolean (:forumMode profile))})

(defn- ^:async load-databases! [{:keys [set-db-info! set-selected-db-id! set-edit-db!]}]
  (try
    (let [data (await (api/list-databases))]
      (set-db-info! data)
      (set-selected-db-id! (:activeDatabaseId data))
      (set-edit-db! (editable-profile
                    (first (filter #(= (:id %) (:activeDatabaseId data)) (:databases data))))))
    (catch :default error (js/console.error error))))

(defn- ^:async load-history! [{:keys [set-history!]}]
  (try (set-history! (vec (:items (await (api/ingestion-history)))))
       (catch :default error (js/console.error error))))

(defn- ^:async reload-all! [setters]
  (let [databases (load-databases! setters)
        documents (load-documents! setters)
        history (load-history! setters)]
    (await databases)
    (await documents)
    (await history)))

(defn- reset-ingestion! [{:keys [set-ingesting! set-progress! set-samples!]}]
  (set-ingesting! false)
  (set-progress! nil)
  (set-samples! []))

(defn- resumable-progress [data progress]
  (assoc progress :canResumeForum (boolean (:canResumeForum data))
                 :stale (boolean (:stale data))))

(defn- ^:async poll-progress! [{:keys [set-ingesting! set-progress! set-samples!] :as setters}]
  (try
    (let [data (await (api/ingestion-progress))]
      (if (or (:active data) (:canResumeForum data))
        (do (set-ingesting! (boolean (:active data)))
            (set-progress! (resumable-progress data (or (:progress data) {})))
            (set-samples! #(logic/push-sample % (.getTime (js/Date.))
                                            (or (get-in data [:progress :processedChunks]) 0))))
        (do (reset-ingestion! setters)
            (await (load-history! setters)))))
    (catch :default error (js/console.error error))))

(defn- ^:async create-database! [new-db {:keys [set-creating! set-new-db! set-selected-docs!] :as setters}]
  (let [profile-name (.trim (or (:name new-db) ""))]
    (when (seq profile-name)
      (set-creating! true)
      (try
        (await (api/create-database
                (cond-> {:name profile-name :activate true
                         :useLocalDocsBaseUrl (:use-local-docs new-db)
                         :forumMode (:forum-mode new-db)}
                  (not (:use-local-docs new-db))
                  (assoc :publicDocsBaseUrl (.trim (or (:public-base-url new-db) ""))))))
        (when (seq (:files new-db))
          (await (api/upload-documents (:files new-db) true)))
        (set-new-db! blank-new-db)
        (set-selected-docs! #{})
        (await (reload-all! setters))
        (catch :default error (js/console.error "Create database failed:" error))
        (finally (set-creating! false))))))

(def ^:private no-active-message
  "No active ingestion run to restart. Start a new ingest instead.")

(defn- no-active-restart! [{:keys [set-message!] :as setters} message]
  (reset-ingestion! setters)
  (set-message! message))

(defn- ^:async resume-ingestion! [before {:keys [set-ingesting! set-progress! set-samples!
                                               set-last-restart! set-message!] :as setters}]
  (let [force-fresh? (logic/should-force-fresh? before)
        result (await (api/restart-ingestion force-fresh?))]
    (if (false? (:resumed result))
      (no-active-restart! setters (str (or (:message result) "No active ingestion run to restart.")))
      (let [data (await (api/ingestion-progress))]
        (set-ingesting! (boolean (:active data)))
        (set-progress! (when (:progress data) (resumable-progress data (:progress data))))
        (set-samples! [])
        (set-last-restart! (.getTime (js/Date.)))
        (set-message! (logic/restart-message force-fresh?))))))

(defn- ^:async restart-ingestion! [{:keys [set-restarting! set-message!] :as setters}]
  (set-restarting! true)
  (set-message! "")
  (try
    (let [before (await (api/ingestion-progress))]
      (if (logic/no-active-run? before)
        (no-active-restart! setters no-active-message)
        (await (resume-ingestion! before setters))))
    (catch :default error
      (if (logic/no-active-restart-error? (.-message error))
        (no-active-restart! setters no-active-message)
        (set-message! "Restart failed. Please try again or start a fresh ingest run.")))
    (finally (set-restarting! false))))

(defn- ^:async simple-action!
  "Run an action and refresh before clearing its busy flag, reporting failures."
  [busy-set! thunk after! label]
  (busy-set! true)
  (try
    (await (thunk))
    (await (after!))
    (catch :default error (js/console.error label error))
    (finally (busy-set! false))))

(defn- edit-payload [edit-db]
  (cond-> {:useLocalDocsBaseUrl (:use-local-docs edit-db)
           :forumMode (:forum-mode edit-db)}
    (seq (.trim (or (:name edit-db) "")))
    (assoc :name (.trim (:name edit-db)))
    (and (not (:use-local-docs edit-db)) (seq (.trim (or (:base-url edit-db) ""))))
    (assoc :publicDocsBaseUrl (.trim (:base-url edit-db)))))

(defn- make-lake-removal-handlers
  [{:keys [selected-db-id db-info]} setters flag!]
  {:delete-db! (fn []
                 (when (and (seq selected-db-id)
                            (not= selected-db-id (:activeDatabaseId db-info))
                            (js/confirm "Delete this lake profile? This does not delete the underlying vector index, only the Knoxx lake profile."))
                   (simple-action! (flag! :deleting) #(api/delete-database selected-db-id)
                                   #(load-databases! setters) "Delete database failed:")))
   :make-private! (fn []
                    (when (and (seq selected-db-id)
                               (js/confirm "Make this lake profile private to your current browser session? Other sessions will no longer see it."))
                      (simple-action! (flag! :privatizing) #(api/make-database-private selected-db-id)
                                      #(load-databases! setters) "Make private failed:")))})

(defn- make-lake-handlers
  [{:keys [new-db selected-db-id db-info edit-db is-ingesting] :as state}
   {:keys [set-selected-docs!] :as setters} flag! reload!]
  (merge
   (make-lake-removal-handlers state setters flag!)
   {:create! #(create-database! new-db setters)
    :activate! (fn []
                 (when (and (not is-ingesting) (seq selected-db-id)
                            (not= selected-db-id (:activeDatabaseId db-info)))
                   (simple-action! (flag! :switching) #(api/activate-database selected-db-id)
                                   (fn [] (set-selected-docs! #{}) (reload!))
                                   "Switch database failed:")))
    :save! (fn []
             (when (seq selected-db-id)
               (simple-action! (flag! :saving) #(api/update-database selected-db-id (edit-payload edit-db))
                               #(load-databases! setters) "Update database failed:")))}))

(defn- ^:async upload! [^js event auto? {:keys [set-uploading!] :as setters}]
  (let [files (vec (js/Array.from (or (.. event -target -files) #js [])))]
    (when (seq files)
      (set-uploading! true)
      (try
        (await (api/upload-documents files auto?))
        (await (load-documents! setters))
        (catch :default error (js/console.error "Upload failed:" error))
        (finally
          (set-uploading! false)
          (set! (.. event -target -value) ""))))))

(defn- ^:async ingest! [options {:keys [set-ingesting!]}]
  (set-ingesting! true)
  (try (await (api/ingest-documents options))
       (catch :default error
         (js/console.error "Ingest failed:" error)
         (set-ingesting! false))))

(defn- ^:async delete-document! [path {:keys [set-selected-docs!] :as setters}]
  (when (js/confirm (str "Are you sure you want to delete " path "?"))
    (try
      (await (api/delete-document path))
      (load-documents! setters)
      (set-selected-docs! #(disj % path))
      (catch :default error (js/console.error "Delete failed:" error)))))

(defn- make-doc-handlers
  [{:keys [selected-docs documents]} {:keys [set-selected-docs!] :as setters}]
  {:upload! #(upload! %1 %2 setters)
   :ingest-selected! #(when (seq selected-docs) (ingest! {:selectedFiles (vec selected-docs)} setters))
   :ingest-all! #(ingest! {:full true} setters)
   :restart! #(restart-ingestion! setters)
   :toggle-all! #(set-selected-docs! (logic/toggle-all selected-docs documents))
   :toggle-doc! #(set-selected-docs! (logic/toggle-doc selected-docs %))
   :delete-doc! #(delete-document! % setters)})

(defn- make-handlers [state setters flag! reload!]
  (merge (make-lake-handlers state setters flag! reload!)
         (make-doc-handlers state setters)))

(hx/defnc lake-profiles-section
  "Render selection, creation, and editing of lake profiles."
  [{:keys [state setters handlers can-access?]}]
  (let [{:keys [is-ingesting db-info selected-db-id new-db edit-db flags]} state
        {:keys [set-selected-db-id! set-new-db! set-edit-db!]} setters]
    (d/div {:class-name "rounded-md border border-slate-700 bg-slate-900 p-4 space-y-3"}
           (d/h2 {:class-name "text-sm font-semibold uppercase tracking-wide text-slate-300"}
                 "Lake Runtime Profiles")
           (d/div {:class-name "grid gap-3 md:grid-cols-3"}
                  (hx/$ view/lake-select-card {:db-info db-info
                                            :selected-db-id selected-db-id
                                            :set-selected-db-id set-selected-db-id!
                                            :selected-db-can-access can-access?
                                            :is-ingesting is-ingesting
                                            :is-switching (:switching flags)
                                            :on-activate (:activate! handlers)})
                  (hx/$ view/lake-create-card {:new-db new-db :set-new-db set-new-db!
                                            :is-creating (:creating flags)
                                            :on-create (:create! handlers)}))
           (hx/$ view/lake-edit-card {:edit-db edit-db :set-edit-db set-edit-db!
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

(hx/defnc ^:private ingestion-section [{:keys [state handlers]}]
  (let [{:keys [is-ingesting progress samples last-restart selected-docs flags]} state
        elapsed (if-let [started (:startedAt progress)]
                  (max 1 (/ (- (.getTime (js/Date.)) (.getTime (js/Date. started))) 1000)) 0)
        rate (logic/chunks-per-sec samples progress elapsed)
        remaining (logic/remaining-chunks progress)]
    (hx/<>
     (when (and is-ingesting progress)
       (hx/$ view/progress-banner {:progress progress :chunks-per-sec rate
                                  :remaining-chunks remaining
                                  :eta-seconds (logic/eta-seconds remaining rate)
                                  :last-restart-at last-restart}))
     (when (and (not is-ingesting) (:canResumeForum progress))
       (hx/$ view/resume-banner {:progress progress}))
     (hx/$ view/ingest-actions {:selected-count (count selected-docs)
                               :is-ingesting is-ingesting
                               :can-restart (or is-ingesting (boolean (:canResumeForum progress)))
                               :is-restarting (:restarting flags)
                               :stale? (boolean (:stale progress))
                               :on-ingest-selected (:ingest-selected! handlers)
                               :on-ingest-all (:ingest-all! handlers)
                               :on-restart (:restart! handlers)}))))

(hx/defnc documents-page*
  "Render document actions and ingestion state from the page model."
  [{:keys [state setters handlers]}]
  (let [{:keys [documents selected-docs is-uploading db-info selected-db-id message history]} state
        selected-db (first (filter #(= (:id %) selected-db-id) (:databases db-info)))
        can-access? (if selected-db (not (false? (:canAccess selected-db))) true)]
    (d/div {:class-name "p-8 max-w-5xl mx-auto space-y-6 text-slate-100"}
           (hx/$ view/page-header {:is-uploading is-uploading :on-upload (:upload! handlers)})
           (hx/$ lake-profiles-section {:state state :setters setters
                                       :handlers handlers :can-access? can-access?})
           (hx/$ ingestion-section {:state state :handlers handlers})
           (when (seq (or message ""))
             (d/div {:class-name "rounded-md border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200"}
                    message))
           (hx/$ view/documents-table {:documents documents :selected-docs selected-docs
                                      :on-toggle-all (:toggle-all! handlers)
                                      :on-toggle-doc (:toggle-doc! handlers)
                                      :on-delete-doc (:delete-doc! handlers)})
           (hx/$ view/history-table {:items history}))))

(defn- use-document-state []
  (let [[documents set-documents!] (hooks/use-state [])
        [selected-docs set-selected-docs!] (hooks/use-state #{})
        [is-uploading set-uploading!] (hooks/use-state false)
        [is-ingesting set-ingesting!] (hooks/use-state false)
        [progress set-progress!] (hooks/use-state nil)
        [message set-message!] (hooks/use-state "")
        [samples set-samples!] (hooks/use-state [])
        [last-restart set-last-restart!] (hooks/use-state nil)
        [history set-history!] (hooks/use-state [])]
    {:state {:documents documents :selected-docs selected-docs
             :is-uploading is-uploading :is-ingesting is-ingesting
             :progress progress :message message :samples samples
             :last-restart last-restart :history history}
     :setters {:set-documents! set-documents! :set-selected-docs! set-selected-docs!
               :set-uploading! set-uploading! :set-ingesting! set-ingesting!
               :set-progress! set-progress! :set-message! set-message!
               :set-samples! set-samples! :set-last-restart! set-last-restart!
               :set-history! set-history!}}))

(defn- use-lake-state []
  (let [[db-info set-db-info!] (hooks/use-state nil)
        [selected-db-id set-selected-db-id!] (hooks/use-state "")
        [new-db set-new-db!] (hooks/use-state blank-new-db)
        [edit-db set-edit-db!] (hooks/use-state {:name "" :base-url "" :use-local-docs true :forum-mode false})
        [flags set-flags!] (hooks/use-state {})]
    {:state {:db-info db-info :selected-db-id selected-db-id :new-db new-db :edit-db edit-db :flags flags}
     :setters {:set-db-info! set-db-info! :set-selected-db-id! set-selected-db-id!
               :set-new-db! set-new-db! :set-edit-db! set-edit-db!}
     :flag! (fn [flag-key] (fn [value] (set-flags! #(assoc % flag-key value))))}))

(hx/defnc ^:export documents-page
  "Mount the document workspace and its refresh lifecycle."
  []
  (let [document-model (use-document-state)
        lake-model (use-lake-state)
        state (merge (:state document-model) (:state lake-model))
        {:keys [db-info selected-db-id]} state
        flag! (:flag! lake-model)
        setters (merge (:setters document-model) (:setters lake-model)
                       {:set-creating! (flag! :creating) :set-restarting! (flag! :restarting)})
        handlers (make-handlers state setters flag! #(reload-all! setters))]
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
       ((:set-edit-db! setters) (editable-profile selected)))
     nil)
    (hx/$ documents-page* {:state state :setters setters :handlers handlers})))
