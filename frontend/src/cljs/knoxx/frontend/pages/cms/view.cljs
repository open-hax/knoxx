(ns knoxx.frontend.pages.cms.view
  "The native Wiki workspace for shared human and agent authoring commands."
  (:require [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.pages.cms.controller :as controller]
            [knoxx.frontend.pages.cms.logic :as logic]
            [knoxx.frontend.pages.cms.panels :as panels]
            [knoxx.frontend.pages.cms.publication-targets :as targets]))

(hx/defnc new-page-form
  "Collect actual source content and explicit publication topology before creating resources."
  [{:keys [creation set-creation! inventory create! cancel-creation! busy? can-create?]}]
  (let [change! #(set-creation! (assoc creation %1 %2))
        ready? (every? #(not (str/blank? (get creation %)))
                       [:title :content :source_locale :garden :target_locales])]
    (d/form {:class-name "panel mx-auto max-w-3xl space-y-5"
             :on-submit (fn [^js event] (.preventDefault event) (when ready? (create!)))}
            (d/h2 {:class-name "text-xl font-semibold"} "Create a wiki page")
            (panels/field "Page title" (:title creation) #(change! :title %) nil)
            (panels/field "Initial source content" (:content creation) #(change! :content %) 8)
            (d/div {:class-name "grid gap-4 sm:grid-cols-2"}
                   (panels/field "Source locale" (:source_locale creation) #(change! :source_locale %) nil)
                   (panels/field "Target locales" (:target_locales creation) #(change! :target_locales %) nil))
            (d/label {:class-name "block"}
                     (d/span {:class-name "field-label"} "Garden")
                     (d/select {:aria-label "Garden" :class-name "input" :value (:garden creation)
                                :on-change #(change! :garden (.. % -target -value))}
                               (d/option {:value ""} "Select a garden")
                               (for [garden (:gardens inventory)
                                     :let [id (logic/wire-id (or (:garden/id garden) (:id garden)))]]
                                 (d/option {:key id :value id} (or (:garden/title garden) (:title garden) id)))))
            (d/div {:class-name "flex gap-2"}
                   (d/button {:class-name "btn btn-primary" :type "submit"
                              :disabled (boolean (or busy? (not can-create?) (not ready?)))} "Create page")
                   (d/button {:class-name "btn btn-ghost" :type "button" :on-click cancel-creation!}
                             (if (logic/creation-dirty? creation) "Discard new page" "Cancel"))))))

(hx/defnc page-navigation
  "Navigate the actual resource inventory without discarding unfinished work."
  [{:keys [inventory selected select! unsaved?]}]
  (let [[query set-query!] (hooks/use-state "")]
    (d/nav {:class-name "panel space-y-4" :aria-label "Wiki pages"}
           (d/h2 {:class-name "panel-title"} "Pages")
           (d/input {:class-name "input" :aria-label "Find a page" :placeholder "Find a page..."
                     :value query :on-change #(set-query! (.. % -target -value))})
           (for [row (:documents inventory)
                 :let [id (logic/document-id row) title (logic/document-title row)]
                 :when (str/includes? (str/lower-case title) (str/lower-case query))]
             (d/button {:key id :type "button" :disabled (boolean unsaved?) :on-click #(select! id)
                        :class-name (str "block w-full rounded-lg px-3 py-3 text-left text-sm "
                                         (when (= selected id) "bg-cyan-950/50 text-cyan-100"))}
                       title))
           (d/p {:class-name "border-t border-white/10 pt-4 text-xs leading-5 opacity-60"}
                "Your roles determine the actions available here and to agents on your team."))))

(hx/defnc selected-page
  "Keep saved article, source editor and revision discussion on one resource identity."
  [{:keys [snapshot editor tab set-tab! discard-source!] :as props}]
  (d/main {:class-name "panel min-w-0 space-y-5 p-7"}
          (d/div {:class-name "space-y-2"}
                 (d/code {:class-name "block truncate text-[10px] opacity-50"} (:document snapshot))
                 (d/span {:class-name "rounded-full bg-white/10 px-2 py-0.5 text-[10px]"}
                         (if (logic/dirty? editor) "Unsaved draft" "Saved revision")))
          (d/div {:class-name "flex gap-5 border-b border-white/10" :role "tablist"}
                 (for [[id title] [[:read "Article"] [:edit "Edit"] [:review "Discussion & history"]]]
                   (d/button {:key (name id) :type "button" :role "tab" :aria-selected (= id tab)
                              :on-click #(set-tab! id)
                              :class-name (str "border-b-2 pb-3 text-sm "
                                               (if (= id tab) "border-cyan-300 text-cyan-300" "border-transparent opacity-60"))}
                             title)))
          (when (:conflict? editor)
            (d/div {:role "alert" :class-name "rounded-lg border border-amber-500/50 p-3 text-sm"}
                   (d/p "This source changed while you were editing. Your unsaved draft is preserved. Saving is blocked until you review the latest revision.")
                   (d/button {:class-name "btn mt-2" :on-click discard-source!} "Discard draft and load latest")))
          (case tab
            :edit (hx/$ panels/source-editor {& props})
            :review (hx/$ panels/source-review {& props})
            (hx/$ panels/article {& props}))))

(defn- wiki-header [{:keys [new! refresh! loading? busy? unsaved? can-create?]}]
  (d/header {:class-name "mb-6 flex items-center justify-between gap-5 border-b border-white/10 pb-5"}
                     (d/div (d/p {:class-name "text-[10px] font-bold uppercase tracking-[0.2em] text-cyan-300"}
                                 "Knoxx · Knowledge workspace")
                            (d/h1 {:class-name "mt-1 text-2xl font-bold"} "Team wiki")
                            (d/p {:class-name "mt-1 text-sm opacity-60"}
                                 "Write together. Review deliberately. Carry each lesson forward."))
                     (d/div {:class-name "flex gap-2"}
                       (d/button {:class-name "btn btn-ghost" :on-click refresh! :disabled busy?} "Refresh pages")
                       (d/button {:class-name "btn btn-primary" :on-click new!
                                :disabled (boolean (or loading? busy? unsaved? (not can-create?)))} "New page"))))

(hx/defnc cms-page
  "Mount the integrated Wiki over the existing authenticated command API."
  [{:keys [Markdown]}]
  (let [state (controller/use-source-controller)
        {:keys [loading? creation snapshot error notice]} state]
    (d/div {:class-name "min-h-0 flex-1 overflow-auto p-5 md:p-8"}
           (wiki-header state)
           (when error
             (d/div {:role "alert" :class-name "mb-4 rounded-lg border border-red-500/40 p-3 text-sm"}
                    error (d/button {:class-name "ml-3 underline" :on-click (:dismiss-error! state)} "Dismiss")))
           (when notice
             (d/p {:role "status" :class-name "mb-4 rounded-lg border border-emerald-500/30 p-3 text-sm"} notice))
           (if creation
             (hx/$ new-page-form {& state})
             (d/div {:class-name "grid items-start gap-5 lg:grid-cols-[210px_minmax(0,1fr)_300px]"}
                    (hx/$ page-navigation {& state})
                    (if snapshot
                      (hx/$ selected-page {& (assoc state :Markdown Markdown)})
                      (d/main {:class-name "panel py-16 text-center text-sm opacity-60"}
                              (if loading? "Loading wiki pages..." "Select a page or create a new one.")))
                    (when snapshot
                      (d/aside {:class-name "panel space-y-5 p-5"}
                               (hx/$ panels/workflow {& state})
                               (hx/$ panels/assistant-panel {& state})
                               (hx/$ targets/publication-targets {& state}))))))))
