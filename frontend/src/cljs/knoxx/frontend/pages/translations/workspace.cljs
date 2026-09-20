(ns knoxx.frontend.pages.translations.workspace
  "Translation inventory, publication actions, and review workspace layout."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.components.ui :as ui]
            [knoxx.frontend.pages.translations.logic :as logic]
            [knoxx.frontend.pages.translations.model-section :as model-section]
            [knoxx.frontend.pages.translations.review-panels :as panels]
            [knoxx.frontend.pages.translations.split-review :as split-review]
            [knoxx.frontend.pages.translations.split-review-panel :as split-review-panel]))

(defn- review-filters [project set-project target-lang set-target-lang manifest]
  (d/div {:class-name "mt-2 flex gap-3"}
                (d/label {:class-name "block text-sm"}
                         (d/span {:class-name "mb-1 block text-xs font-medium text-slate-400"} "Project")
                         (d/div {:class-name "w-28"}
                                (hx/$ ui/input {:value project
                                             :on-change #(set-project (.. % -target -value))
                                             :placeholder "devel"})))
                (d/label {:class-name "block text-sm"}
                         (d/span {:class-name "mb-1 block text-xs font-medium text-slate-400"} "Target Lang")
                         (d/select {:value target-lang
                                    :on-change #(set-target-lang (.. % -target -value))
                                    :class-name "rounded-md border border-slate-600 bg-slate-800 px-3 py-2 text-sm text-slate-100"}
                                   (d/option {:value ""} "All")
                                   (for [l (logic/available-langs manifest)]
                                     (d/option {:key l :value l} (logic/lang-name l)))))
                (when manifest
                  (d/div {:class-name "flex items-end gap-3 text-xs text-slate-400"}
                         (for [[lang stats] (:languages manifest)]
                           (d/span {:key (name lang)}
                                   (str (logic/lang-name (name lang)) ": "
                                        (:approved stats) "/" (:total_segments stats)
                                        " approved")))))))

(hx/defnc review-header
  "Choose project and locale and expose pipeline configuration."
  [{:keys [project set-project target-lang set-target-lang manifest
           show-config? toggle-config on-export]}]
  (d/div {:class-name "shrink-0 border-b border-slate-800 bg-slate-950/90 px-4 py-3"}
         (d/div {:class-name "flex items-center justify-between"}
                (d/h1 {:class-name "text-lg font-bold text-slate-100"} "Translation Review")
                (d/div {:class-name "flex items-center gap-2"}
                       (hx/$ ui/button {:variant :ghost :on-click toggle-config}
                          (if show-config? "Hide Config" "⚙ Pipeline"))
                       (hx/$ ui/button {:variant :ghost :on-click on-export} "Export SFT")))
         (review-filters project set-project target-lang set-target-lang manifest)
         (when show-config?
           (d/div {:class-name "mt-3"}
                  (hx/$ model-section/model-section {:can-manage true})))))

(defn- dismissable-banner [text tone on-dismiss]
  (d/div {:class-name (str "shrink-0 border-b px-4 py-2 text-sm "
                           (if (= tone :success)
                             "border-emerald-500/40 bg-emerald-500/10 text-emerald-300"
                             "border-rose-500/40 bg-rose-500/10 text-rose-300"))}
         text
         (d/button {:class-name "ml-2 underline" :on-click on-dismiss} "dismiss")))

(defn- whole-approval-disabled?
  [selected review saving]
  (or saving
      (if (split-review/review selected)
        (not (split-review/all-approved? selected))
        (not (true? (:reviewable review))))))

(hx/defnc publication-approval-action
  "Approve only a reviewable revision whose splits are accepted."
  [{:keys [selected saving on-publication-approval]}]
  (when-let [review (:publication_review selected)]
    (let [split-review? (some? (split-review/review selected))]
      (when (and (:contract_content selected)
                 (or split-review? (true? (:reviewable review)))
                 (not (logic/blocked-resource-candidate? selected)))
        (if (:approved review)
          (d/span {:class-name "rounded bg-emerald-900/30 px-3 py-2 text-xs font-medium text-emerald-300"}
                  (if split-review? "Whole output approved" "Approved for publication"))
          (hx/$ ui/button
             {:size :sm
              :variant :primary
              :disabled (whole-approval-disabled? selected review saving)
              :title (when (and split-review?
                                (not (split-review/all-approved? selected)))
                       "Approve every split before approving the whole output")
             :on-click on-publication-approval}
             (if split-review? "Approve whole output" "Approve for publication")))))))

(hx/defnc review-fast-path-actions
  "Apply the admitted document or candidate-wide review command."
  [{:keys [selected project saving on-review on-resource-review]}]
  (let [resource? (some? (split-review/review selected))
        legacy? (logic/legacy-mutation-admitted? selected project)]
    (when (or resource? legacy?)
      (let [submit (if resource? on-resource-review on-review)
            [approve-status edit-status reject-status]
            (if resource?
              ["approved" "in-review" "rejected"]
              ["approve" "needs_edit" "reject"])]
        (hx/<>
         (hx/$ ui/button {:size :sm :disabled saving
                       :on-click #(submit approve-status)}
            "Approve All")
         (hx/$ ui/button {:size :sm :variant :secondary :disabled saving
                       :on-click #(submit edit-status)}
            "Needs Edit")
         (hx/$ ui/button {:size :sm :variant :ghost :disabled saving
                       :on-click #(submit reject-status)}
            "Reject All"))))))

(hx/defnc document-actions
  "Offer the actions admitted for the selected translation work."
  [{:keys [selected project saving on-review on-publication-approval
           on-resource-review on-translation-dispatch]}]
  (d/div {:class-name "flex gap-2"}
         (when (logic/allowed-action? selected "dispatch")
           (hx/$ ui/button {:size :sm :variant :primary :disabled saving
                         :on-click on-translation-dispatch}
              "Dispatch"))
         (when (logic/allowed-action? selected "retry")
           (hx/$ ui/button {:size :sm :variant :primary :disabled saving
                         :on-click on-translation-dispatch}
              "Retry"))
         (hx/$ review-fast-path-actions
            {:selected selected :project project :saving saving
             :on-review on-review :on-resource-review on-resource-review})
         (hx/$ publication-approval-action
            {:selected selected :saving saving
             :on-publication-approval on-publication-approval})))

(defn- blocked-candidate-copy
  [row]
  (case (if (keyword? (:hydration_state row))
          (name (:hydration_state row))
          (:hydration_state row))
    "source_moved"
    "The source revision changed after this candidate was created. Review and publication approval are blocked until a candidate is produced for the current source."

    "content_moved"
    "The translated bytes no longer match the completed candidate receipt. Review and publication approval are blocked until those exact bytes are restored or a new candidate is produced."

    "content_missing"
    "This receipt names a resource-backed candidate, but its exact source or translated bytes could not be loaded. Review and publication approval are blocked; legacy content is not substituted."

    "This resource-backed candidate did not pass exact-byte hydration. Review and publication approval are blocked; legacy content is not substituted."))

(defn- blocked-candidate [selected]
  (d/div {:class-name "rounded-lg border border-rose-900/50 bg-rose-950/20 p-5"}
                  (d/h3 {:class-name "text-sm font-semibold text-rose-200"}
                        "Translation candidate content unavailable")
                  (d/p {:class-name "mt-2 text-sm text-rose-200/80"}
                       (blocked-candidate-copy selected))))

(defn- missing-candidate [selected]
  (d/div {:class-name "rounded-lg border border-slate-700 bg-slate-900/40 p-5"}
                  (d/h3 {:class-name "text-sm font-semibold text-slate-100"}
                        "No translation candidate yet")
                  (d/p {:class-name "mt-2 text-sm text-slate-400"}
                       (str "Resource work is " (logic/status-label (:work_state selected))
                            ". Its row remains visible because the resource graph, not completed receipts, owns this inventory."))))

(hx/defnc candidate-or-work-state
  "Display hydrated candidate bytes or the exact blocking reason."
  [{:keys [selected detail seg-idx set-seg-idx]}]
  (d/div {:class-name "min-h-0 flex-1 overflow-auto p-4"}
         (cond
           (logic/blocked-resource-candidate? selected)
           (blocked-candidate selected)

           (and (logic/work-row? selected)
                (logic/candidate-present? selected)
                (not (logic/legacy-candidate? selected))
                (not (:contract_content selected)))
           (d/div {:class-name "rounded-lg border border-amber-900/50 bg-amber-950/20 p-5"}
                  (d/h3 {:class-name "text-sm font-semibold text-amber-200"}
                        "Translation candidate is not split-reviewable")
                  (d/p {:class-name "mt-2 text-sm text-amber-200/80"}
                       "This resource candidate has no admitted persisted split set. Legacy review controls are hidden so they cannot mutate a same-named older document."))

           (logic/candidate-present? selected)
           (d/div {:class-name "space-y-2"}
                  (for [seg (:segments detail)]
                    (hx/$ panels/segment-annotation {:key (:id seg)
                                           :segment seg
                                           :selected? (= seg-idx (:segment_index seg))
                                           :on-select #(set-seg-idx (:segment_index seg))})))

           :else
           (missing-candidate selected))))

(defn- selected-document-header [selected project detail saving on-review on-resource-review on-publication-approval on-translation-dispatch]
  (d/div {:class-name "shrink-0 border-b border-slate-800 bg-slate-950/90 px-4 py-3"}
            (d/div {:class-name "flex items-center justify-between"}
                   (d/div
                    (d/h2 {:class-name "text-base font-semibold text-slate-100"}
                          (get-in detail [:document :title]))
                    (d/div {:class-name "mt-0.5 flex items-center gap-3 text-xs text-slate-400"}
                           (d/span (str (logic/lang-name (get-in detail [:document :source_lang]))
                                        " → " (logic/lang-name (:target_lang selected))))
                           (d/span (str "· " (get-in detail [:summary :total_segments]) " segments"))
                           (hx/$ panels/status-badge {:status (get-in detail [:summary :overall_status])})))
                   (hx/$ document-actions {:selected selected :project project :saving saving
                                        :on-review on-review
                                        :on-resource-review on-resource-review
                                        :on-publication-approval on-publication-approval
                                        :on-translation-dispatch on-translation-dispatch}))
            (hx/$ panels/progress-bar {:approved (get-in detail [:summary :approved])
                             :total (get-in detail [:summary :total_segments])})))

(hx/defnc document-pane
  "Show the selected document and its live review progress."
  [{:keys [selected project detail detail-loading saving seg-idx set-seg-idx
           on-review on-resource-review on-publication-approval on-translation-dispatch]}]
  (cond
    (nil? selected)
    (d/div {:class-name "flex flex-1 items-center justify-center text-sm text-slate-500"}
           "Select a document to review")

    detail-loading
    (d/div {:class-name "flex flex-1 items-center justify-center text-sm text-slate-400"} "Loading…")

    (nil? detail)
    (d/div {:class-name "flex flex-1 items-center justify-center text-sm text-rose-400"}
           "Failed to load document")

    :else
    (hx/<>
     (selected-document-header selected project detail saving on-review on-resource-review on-publication-approval on-translation-dispatch)
     (hx/$ candidate-or-work-state {:selected selected :detail detail
                                 :seg-idx seg-idx :set-seg-idx set-seg-idx}))))

(hx/defnc document-list-pane
  "List the current authorized translation work inventory."
  [{:keys [loading documents selected set-selected]}]
  (d/aside {:class-name "flex w-72 shrink-0 flex-col overflow-auto border-r border-slate-800 p-3"}
           (cond
             loading (d/p {:class-name "text-sm text-slate-400"} "Loading documents…")
             (empty? documents) (d/p {:class-name "text-sm text-slate-400"} "No translated documents found.")
             :else (d/div {:class-name "space-y-2"}
                          (for [doc documents]
                            (hx/$ panels/document-card {:key (pr-str (logic/work-row-id doc))
                                              :doc doc
                                              :selected? (and selected (logic/same-work? doc selected))
                                              :on-select #(set-selected doc)}))))))

(hx/defnc segment-review-content
  "Select the resource or legacy review control for this candidate."
  [{:keys [work-row segment form saving set-form on-submit on-skip
           read-only? content-source]}]
  (cond
    (logic/blocked-resource-candidate? work-row)
    (d/p {:class-name "text-sm text-rose-300"}
         (blocked-candidate-copy work-row))

    (split-review/review work-row)
    (hx/$ split-review-panel/split-review-panel {:split segment :form form :saving saving
                           :on-change set-form :on-submit on-submit
                           :on-skip on-skip})

    (and (logic/work-row? work-row) (not (logic/legacy-candidate? work-row)))
    (d/p {:class-name "text-sm text-slate-400"}
         (if (logic/candidate-present? work-row)
           "This resource candidate has no admitted persisted split set. Legacy mutation controls remain unavailable."
           "Dispatch this work item to create a candidate. Real persisted splits will appear here for review."))

    :else
    (hx/$ panels/segment-detail-panel {:segment segment :form form :saving saving
                             :on-change set-form :on-submit on-submit
                             :read-only? read-only? :content-source content-source})))

(hx/defnc segment-review-pane
  "Keep the active review input visible while guarding stale commands."
  [{:keys [work-row] :as props}]
  (d/aside {:class-name "flex w-[440px] shrink-0 flex-col overflow-hidden"}
           (d/div {:class-name "shrink-0 border-b border-slate-800 px-4 py-3"}
                  (d/h3 {:class-name "text-sm font-semibold text-slate-200"}
                        (if (split-review/review work-row)
                          "Split Review"
                          "Segment Review")))
           (d/div {:class-name "min-h-0 flex-1 overflow-auto p-4"}
                  (hx/$ segment-review-content {& props}))))

(defn- review-columns
  [{:keys [loading documents selected set-selected detail detail-loading saving project
           seg-idx set-seg-idx form set-form selected-segment on-review on-resource-review
           on-publication-approval on-translation-dispatch on-segment-submit on-segment-skip review-row]}]
  (d/div {:class-name "flex min-h-0 flex-1 overflow-hidden"}
                (hx/$ document-list-pane {:loading loading :documents documents
                                       :selected selected :set-selected set-selected})
                (d/main {:class-name "flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden border-r border-slate-800"}
                (hx/$ document-pane {:selected selected :detail detail :project project
                                  :detail-loading detail-loading :saving saving
                                  :seg-idx seg-idx :set-seg-idx set-seg-idx
                                  :on-review on-review :on-resource-review on-resource-review
                                  :on-publication-approval on-publication-approval :on-translation-dispatch on-translation-dispatch}))
                (hx/$ segment-review-pane {:work-row (or review-row selected) :segment selected-segment
                                        :form form :saving saving :set-form set-form
                                        :read-only? (not (logic/legacy-mutation-admitted?
                                                          selected project))
                                        :content-source (:content_source selected)
                                        :on-submit on-segment-submit
                                        :on-skip on-segment-skip})))

(hx/defnc translation-review-layout
  "Arrange inventory, candidate text, and reviewer input in one workspace."
  [{:keys [notice dismiss-notice error dismiss-error draft-stale? discard-draft] :as props}]
  (d/div {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden"}
         (hx/$ review-header {& props})
         (when notice (dismissable-banner notice :success dismiss-notice))
         (when error (dismissable-banner error :error dismiss-error))
         (when draft-stale?
           (d/div {:role "alert" :class-name "space-y-2 border-b border-amber-800 bg-amber-950/30 p-3 text-sm text-amber-200"}
                  (d/p "The candidate or its review changed. Your local correction and notes are preserved; review the latest state before submitting.")
                  (hx/$ ui/button {:on-click discard-draft} "Discard local review and load latest")))
         (review-columns props)))
