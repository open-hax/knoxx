(ns knoxx.frontend.pages.documents.view
  "Presentational pieces for the documents/lakes page. Helix port of
   src/pages/documents-page/DocumentsPageView.tsx, split into cards."
  (:require [helix.core :as hx :refer [$ defnc]]
            [helix.dom :as d]
            [knoxx.frontend.pages.documents.logic :as logic]))

(def ^:private field-input
  "w-full rounded border border-slate-700 bg-slate-800 px-2 py-1.5 text-sm text-slate-100")

(defn- action-button [{:keys [on-click disabled class-name label]}]
  (d/button {:on-click on-click
             :disabled disabled
             :class-name (str "px-3 py-2 rounded text-white disabled:opacity-50 " class-name)}
            label))

(defnc page-header [{:keys [is-uploading on-upload]}]
  (d/div {:class-name "flex justify-between items-center"}
         (d/div
          (d/h1 {:class-name "text-2xl font-bold"} "Data Lakes")
          (d/p {:class-name "mt-1 text-sm text-slate-400"}
               "Knoxx now treats lakes as the primary document boundary. This page manages the active runtime lake profile that ingestion and retrieval are using right now."))
         (d/label {:class-name "px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 cursor-pointer"}
                  (if is-uploading "Uploading..." "Upload & Auto-Ingest")
                  (d/input {:type "file" :multiple true :class-name "hidden"
                            :disabled is-uploading
                            :on-change #(on-upload % true)}))))

(defnc lake-select-card
  [{:keys [db-info selected-db-id set-selected-db-id selected-db-can-access
           is-ingesting is-switching on-activate]}]
  (d/div {:class-name "md:col-span-2"}
         (d/label {:class-name "text-xs text-slate-400"} "Active Lake Profile")
         (d/div {:class-name "mt-1 flex gap-2"}
                (d/select {:class-name field-input
                           :value selected-db-id
                           :on-change #(set-selected-db-id (.. % -target -value))}
                          (for [{:keys [id name qdrantCollection privateToSession canAccess]} (:databases db-info)]
                            (d/option {:key id :value id}
                                      (str name " · index " qdrantCollection
                                           (when privateToSession
                                             (if (false? canAccess) " [private: other session]" " [private]"))))))
                (action-button {:on-click on-activate
                                :disabled (or is-ingesting is-switching
                                              (empty? (or selected-db-id ""))
                                              (= selected-db-id (:activeDatabaseId db-info))
                                              (not selected-db-can-access))
                                :class-name "bg-cyan-600 hover:bg-cyan-500"
                                :label (if is-switching "Activating..." "Activate")}))
         (d/p {:class-name "text-xs text-slate-400 mt-1"}
              (str "Mounted docs path: " (or (get-in db-info [:activeRuntime :docsPath]) "N/A")))
         (when is-ingesting
           (d/p {:class-name "text-xs text-amber-300 mt-1"}
                "Lake switching is disabled while ingestion is active."))
         (when-not selected-db-can-access
           (d/p {:class-name "text-xs text-rose-300 mt-1"}
                "This lake profile is private to another session. You can view it but cannot activate or edit it."))))

(defnc lake-create-card
  [{:keys [new-db set-new-db is-creating on-create]}]
  (let [update! (fn [k v] (set-new-db (assoc new-db k v)))]
    (d/div
     (d/label {:class-name "text-xs text-slate-400"} "Create New Lake Profile")
     (d/div {:class-name "mt-1 flex gap-2"}
            (d/input {:value (:name new-db)
                      :on-change #(update! :name (.. % -target -value))
                      :class-name field-input
                      :placeholder "e.g. Engine Manuals"})
            (action-button {:on-click on-create
                            :disabled (or is-creating (empty? (.trim (or (:name new-db) ""))))
                            :class-name "bg-emerald-600 hover:bg-emerald-500"
                            :label (cond is-creating "Creating..."
                                         (seq (:files new-db)) "Create + Upload"
                                         :else "Create")}))
     (d/div {:class-name "mt-2 space-y-2"}
            (d/label {:class-name "flex items-center gap-2 text-xs text-slate-300"}
                     (d/input {:type "checkbox" :checked (:forum-mode new-db)
                               :on-change #(update! :forum-mode (.. % -target -checked))})
                     "Forum mode for this lake")
            (d/label {:class-name "flex items-center gap-2 text-xs text-slate-300"}
                     (d/input {:type "checkbox" :checked (:use-local-docs new-db)
                               :on-change #(update! :use-local-docs (.. % -target -checked))})
                     "Use local docs viewer links")
            (when-not (:use-local-docs new-db)
              (d/input {:class-name field-input
                        :placeholder "https://docs.example.com"
                        :value (:public-base-url new-db)
                        :on-change #(update! :public-base-url (.. % -target -value))}))
            (d/input {:type "file" :multiple true
                      :on-change #(update! :files (vec (js/Array.from (or (.. % -target -files) #js []))))
                      :class-name "block w-full text-xs text-slate-300"})
            (d/p {:class-name "text-[11px] text-slate-400"}
                 "Optional bootstrap upload (.zip or files) after creating the lake profile.")))))

(defnc lake-edit-card
  [{:keys [edit-db set-edit-db selected-db-id selected-db-can-access db-info
           is-ingesting is-saving is-deleting is-privatizing
           on-save on-delete on-make-private]}]
  (let [update! (fn [k v] (set-edit-db (assoc edit-db k v)))]
    (hx/<>
     (d/div {:class-name "grid gap-3 md:grid-cols-3"}
            (d/div
             (d/label {:class-name "text-xs text-slate-400"} "Display Name")
             (d/input {:class-name (str field-input " mt-1")
                       :value (:name edit-db)
                       :on-change #(update! :name (.. % -target -value))}))
            (d/div {:class-name "md:col-span-2"}
                   (d/label {:class-name "text-xs text-slate-400"} "Public Docs Base URL")
                   (d/input {:class-name (str field-input " mt-1")
                             :value (:base-url edit-db)
                             :disabled (:use-local-docs edit-db)
                             :on-change #(update! :base-url (.. % -target -value))})))
     (d/div {:class-name "flex flex-wrap gap-4 text-xs text-slate-300"}
            (d/label {:class-name "flex items-center gap-2"}
                     (d/input {:type "checkbox" :checked (:forum-mode edit-db)
                               :on-change #(update! :forum-mode (.. % -target -checked))})
                     "Forum mode")
            (d/label {:class-name "flex items-center gap-2"}
                     (d/input {:type "checkbox" :checked (:use-local-docs edit-db)
                               :on-change #(update! :use-local-docs (.. % -target -checked))})
                     "Use local docs viewer links"))
     (d/div {:class-name "flex gap-2"}
            (action-button {:on-click on-save
                            :disabled (or is-saving (empty? (or selected-db-id "")) (not selected-db-can-access))
                            :class-name "bg-indigo-600 hover:bg-indigo-500"
                            :label (if is-saving "Saving..." "Save Lake Profile")})
            (action-button {:on-click on-delete
                            :disabled (or is-deleting (empty? (or selected-db-id ""))
                                          (= selected-db-id (:activeDatabaseId db-info))
                                          is-ingesting (not selected-db-can-access))
                            :class-name "bg-rose-700 hover:bg-rose-600"
                            :label (if is-deleting "Deleting..." "Delete Lake Profile")})
            (action-button {:on-click on-make-private
                            :disabled (or is-privatizing (empty? (or selected-db-id "")) (not selected-db-can-access))
                            :class-name "bg-amber-700 hover:bg-amber-600"
                            :label (if is-privatizing "Applying..." "Make Session-Private")})))))

(defnc progress-banner
  [{:keys [progress chunks-per-sec remaining-chunks eta-seconds last-restart-at]}]
  (d/div {:class-name "bg-cyan-500/10 border border-cyan-500/30 p-4 rounded-md"}
         (d/h3 {:class-name "font-semibold text-cyan-200"} "Ingestion in Progress")
         (d/div {:class-name "w-full bg-slate-800 rounded-full h-2.5 mt-2"}
                (d/div {:class-name "bg-cyan-400 h-2.5 rounded-full"
                        :style #js {:width (str (or (:percent progress) 0) "%")}}))
         (d/p {:class-name "text-sm mt-2 text-cyan-100"}
              (str (:processedChunks progress) " / " (:totalChunks progress)
                   " chunks processed ("
                   (.toFixed (js/Number. (or (:percentPrecise progress) (:percent progress) 0)) 2)
                   "%)"))
         (d/p {:class-name "text-xs text-cyan-200/90 mt-1"}
              (str "Throughput: " (.toFixed (js/Number. chunks-per-sec) 2)
                   " chunks/s | Remaining: " remaining-chunks
                   " chunks | ETA: " (logic/format-eta eta-seconds)))
         (when last-restart-at
           (d/p {:class-name "text-xs text-cyan-200/80"}
                (str "Last restart requested at " (.toLocaleTimeString (js/Date. last-restart-at)))))
         (d/p {:class-name "text-xs text-cyan-200/80 truncate"} (:currentFile progress))
         (when (:stale progress)
           (d/p {:class-name "text-xs text-amber-300 mt-1"}
                "Progress appears stalled. Use restart resume."))))

(defnc resume-banner [{:keys [progress]}]
  (d/div {:class-name "bg-amber-500/10 border border-amber-500/30 p-4 rounded-md"}
         (d/h3 {:class-name "font-semibold text-amber-200"} "Resumable Forum Ingestion Found")
         (d/p {:class-name "text-xs text-amber-100 mt-1"}
              (str "Last checkpoint: " (or (:currentFile progress) "N/A") " ("
                   (.toFixed (js/Number. (or (:percentPrecise progress) (:percent progress) 0)) 2)
                   "%)."))
         (d/p {:class-name "text-xs text-amber-200/80 mt-1"} "Press restart to resume from checkpoint.")))

(defnc ingest-actions
  [{:keys [selected-count is-ingesting can-restart is-restarting stale?
           on-ingest-selected on-ingest-all on-restart]}]
  (d/div {:class-name "flex items-center gap-4 bg-slate-900 p-4 rounded-md border border-slate-700"}
         (action-button {:on-click on-ingest-selected
                         :disabled (or (zero? selected-count) is-ingesting)
                         :class-name "bg-green-600 hover:bg-green-700"
                         :label (str "Ingest Selected (" selected-count ")")})
         (action-button {:on-click on-ingest-all
                         :disabled is-ingesting
                         :class-name "bg-gray-600 hover:bg-gray-700"
                         :label "Ingest All Missing"})
         (action-button {:on-click on-restart
                         :disabled (or (not can-restart) is-restarting)
                         :class-name "bg-amber-600 hover:bg-amber-500"
                         :label (cond is-restarting "Restarting..."
                                      stale? "Restart Ingestion (Force Fresh)"
                                      :else "Restart Ingestion (Resume)")})))

(defnc documents-table
  [{:keys [documents selected-docs on-toggle-all on-toggle-doc on-delete-doc]}]
  (d/div {:class-name "overflow-x-auto border border-slate-700 rounded-md bg-slate-900"}
         (d/table {:class-name "w-full text-left border-collapse"}
                  (d/thead
                   (d/tr {:class-name "bg-slate-800 border-b border-slate-700"}
                         (d/th {:class-name "p-3 w-12 text-center"}
                               (d/input {:type "checkbox"
                                         :checked (and (pos? (count documents))
                                                       (= (count selected-docs) (count documents)))
                                         :on-change on-toggle-all}))
                         (d/th {:class-name "p-3 font-semibold"} "Name")
                         (d/th {:class-name "p-3 font-semibold"} "Size")
                         (d/th {:class-name "p-3 font-semibold"} "Status")
                         (d/th {:class-name "p-3 font-semibold text-right"} "Actions")))
                  (d/tbody
                   (if (empty? documents)
                     (d/tr (d/td {:col-span 5 :class-name "p-8 text-center text-slate-400"}
                                 "No documents found."))
                     (for [{:keys [relativePath name size indexed chunkCount]} documents]
                       (d/tr {:key relativePath
                              :class-name "border-b border-slate-800 hover:bg-slate-800/60"}
                             (d/td {:class-name "p-3 text-center"}
                                   (d/input {:type "checkbox"
                                             :checked (contains? selected-docs relativePath)
                                             :on-change #(on-toggle-doc relativePath)}))
                             (d/td {:class-name "p-3"}
                                   (d/div {:class-name "font-medium"} name)
                                   (d/div {:class-name "text-xs text-slate-400"} relativePath))
                             (d/td {:class-name "p-3 text-sm text-slate-300"}
                                   (str (.toFixed (/ size 1024) 1) " KB"))
                             (d/td {:class-name "p-3"}
                                   (if indexed
                                     (d/span {:class-name "inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-emerald-500/20 text-emerald-300 border border-emerald-500/30"}
                                             (str "Indexed (" chunkCount " chunks)"))
                                     (d/span {:class-name "inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-amber-500/20 text-amber-300 border border-amber-500/30"}
                                             "Pending")))
                             (d/td {:class-name "p-3 text-right"}
                                   (d/button {:on-click #(on-delete-doc relativePath)
                                              :class-name "text-red-600 hover:text-red-800 text-sm font-medium"}
                                             "Delete")))))))))

(defnc history-table [{:keys [items]}]
  (d/div {:class-name "rounded-md border border-slate-700 bg-slate-900 p-4"}
         (d/h2 {:class-name "text-sm font-semibold uppercase tracking-wide text-slate-300 mb-3"}
               "Ingestion History (Current Lake)")
         (d/div {:class-name "overflow-x-auto"}
                (d/table {:class-name "w-full text-left border-collapse text-sm"}
                         (d/thead
                          (d/tr {:class-name "border-b border-slate-700 text-slate-300"}
                                (for [h ["Completed" "Mode" "Chunks Upserted" "Files Updated" "Duration" "Errors"]]
                                  (d/th {:key h :class-name "p-2"} h))))
                         (d/tbody
                          (if (empty? items)
                            (d/tr (d/td {:col-span 6 :class-name "p-3 text-slate-400"}
                                        "No ingestion runs yet for this lake."))
                            (for [{:keys [id completedAt mode chunksUpserted processedChunks
                                          filesUpdated durationSeconds errors]} items]
                              (d/tr {:key id :class-name "border-b border-slate-800"}
                                    (d/td {:class-name "p-2 text-slate-200"}
                                          (.toLocaleString (js/Date. completedAt)))
                                    (d/td {:class-name "p-2 text-slate-300"} mode)
                                    (d/td {:class-name "p-2 text-slate-300"} (or chunksUpserted processedChunks 0))
                                    (d/td {:class-name "p-2 text-slate-300"} (or filesUpdated 0))
                                    (d/td {:class-name "p-2 text-slate-300"} (str (or durationSeconds 0) "s"))
                                    (d/td {:class-name "p-2 text-slate-300"} (or errors 0))))))))))
