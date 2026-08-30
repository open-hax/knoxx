(ns knoxx.frontend.pages.translations.view
  "Translation review page. Helix port of src/pages/TranslationReviewPage.tsx.
   No router/auth dependencies — node-testable end to end; pure logic in
   translations.logic, REST in translations.api."
  (:require [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.components.ui :as ui]
            [knoxx.frontend.pages.translations.api :as api]
            [knoxx.frontend.pages.translations.logic :as logic]
            [knoxx.frontend.pages.translations.model-section :refer [model-section]]
            [knoxx.frontend.pages.translations.review-controller :as controller]))

;; ── small presentational pieces ──────────────────────────────────────────────

(defnc status-badge [{:keys [status]}]
  (d/span {:class-name (str "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium "
                            (logic/status-class status))}
          (str (logic/status-icon status) " " (logic/status-label status))))

(defnc progress-bar [{:keys [approved total]}]
  (d/div {:class-name "flex items-center gap-2"}
         (d/div {:class-name "h-1.5 flex-1 rounded-full bg-slate-700"}
                (d/div {:class-name "h-1.5 rounded-full bg-emerald-500 transition-all"
                        :style #js {:width (str (logic/progress-pct approved total) "%")}}))
         (d/span {:class-name "text-xs text-slate-400"}
                 (str approved "/" total))))

(defnc document-card [{:keys [doc selected? on-select]}]
  (d/button {:type "button"
             :on-click on-select
             :class-name (str "w-full rounded-lg border p-3 text-left transition "
                              (if selected?
                                "border-blue-400 bg-blue-500/10"
                                "border-slate-700 bg-slate-900/50 hover:border-slate-600"))}
            (d/div {:class-name "mb-1.5 flex items-start justify-between gap-2"}
                   (d/span {:class-name "line-clamp-1 text-sm font-semibold text-slate-100"}
                           (:title doc))
                   ($ status-badge {:status (:overall_status doc)}))
            (d/div {:class-name "mb-2 text-xs text-slate-400"}
                   (str (logic/lang-name (:source_lang doc)) " → " (logic/lang-name (:target_lang doc)))
                   (when (:garden_id doc)
                     (d/span {:class-name "ml-2"} (str "· " (:garden_id doc)))))
            ($ progress-bar {:approved (:approved doc) :total (:total_segments doc)})))

(defn- segment-border [selected? status]
  (cond
    selected? "var(--token-colors-border-focus, #3b82f6)"
    (= status "approved") "var(--token-colors-border-success, #10b981)"
    (= status "rejected") "var(--token-colors-border-danger, #ef4444)"
    :else "var(--token-colors-border-warning, #f59e0b)"))

(defn- segment-text-column [label text]
  (d/div
   (d/div {:class-name "mb-1 text-[10px] font-medium text-slate-400"} label)
   (d/div {:class-name "whitespace-pre-wrap break-words text-[13px] leading-relaxed text-slate-100"}
          text)))

(defnc segment-annotation [{:keys [segment selected? on-select]}]
  (let [{:keys [segment_index status label_count source_lang target_lang
                source_text translated_text]} segment]
    (d/div {:on-click on-select
            :style #js {:cursor "pointer"
                        :borderRadius 6
                        :borderLeft (str "4px solid " (segment-border selected? status))
                        :padding "8px 12px"}
            :class-name "bg-slate-900/40 transition"}
           (d/div {:class-name "mb-2 flex items-center gap-2 text-xs"}
                  (d/span {:class-name "text-slate-500"} (str "seg " segment_index))
                  (d/span (logic/status-icon status))
                  (d/span {:class-name "text-slate-400"} status)
                  (when (and (some? label_count) (pos? label_count))
                    (d/span {:class-name "text-slate-500"}
                            (str label_count " review" (when (not= 1 label_count) "s")))))
           (d/div {:class-name "grid grid-cols-2 gap-3"}
                  (segment-text-column (str "Source (" (logic/lang-name source_lang) ")") source_text)
                  (segment-text-column (str "Translation (" (logic/lang-name target_lang) ")") translated_text)))))

;; ── segment review panel ─────────────────────────────────────────────────────

(defnc label-score-fields [{:keys [form on-change]}]
  (d/div {:class-name "grid gap-3"}
         (for [field [:adequacy :fluency :terminology :risk]]
           (d/label {:key (name field) :class-name "block text-sm"}
                    (d/span {:class-name "mb-1 block font-medium capitalize text-slate-200"}
                            (name field))
                    (d/select {:value (get form field)
                               :on-change #(on-change (assoc form field (.. % -target -value)))
                               :class-name "w-full rounded-md border border-slate-600 bg-slate-800 px-3 py-2 text-sm text-slate-100"}
                              (for [v (logic/field-options field)]
                                (d/option {:key v :value v} v)))))))

(defn- label-textarea [label value placeholder rows on-change]
  (d/label {:class-name "block text-sm"}
           (d/span {:class-name "mb-1 block font-medium text-slate-200"} label)
           (d/textarea {:value (or value "")
                        :on-change on-change
                        :rows rows
                        :placeholder placeholder
                        :class-name "w-full rounded-md border border-slate-600 bg-slate-800 px-3 py-2 text-sm text-slate-100"})))

(defn- segment-source-block [title text]
  (d/div {:class-name "rounded-lg border border-slate-700 bg-slate-800/50 p-3"}
         (d/h5 {:class-name "mb-2 text-xs font-semibold text-slate-400"} title)
         (d/pre {:class-name "whitespace-pre-wrap break-words text-sm text-slate-200"} text)))

(defnc previous-labels [{:keys [labels]}]
  (when (seq labels)
    (d/div {:class-name "rounded-lg border border-slate-700 bg-slate-900/30 p-3"}
           (d/h5 {:class-name "mb-2 text-xs font-semibold text-slate-300"} "Previous labels")
           (d/div {:class-name "space-y-1"}
                  (for [label labels]
                    (d/div {:key (:id label) :class-name "text-xs text-slate-400"}
                           (d/span {:class-name "font-medium"} (:labeler_email label))
                           (str " · " (:overall label) " · " (:adequacy label) "/" (:fluency label))
                           (when (:corrected_text label)
                             (d/span {:class-name "ml-1"} "· corrected"))))))))

(defnc authored-review-notice
  "Why this translation cannot be scored here, and what it is.

   Two separate facts, deliberately not merged. The read-only REASON is that a
   contract-backed translation has no persisted segment for a label to attach
   to — identically true of authored and agent content. What the reviewer is
   LOOKING AT is the other fact, and it matters: approving text a person wrote
   and approving text a model produced are different acts."
  [{:keys [content-source]}]
  (d/div {:class-name "rounded-lg border border-amber-900/40 bg-amber-950/20 p-3 text-xs text-amber-200"}
         (d/p {:class-name "font-medium"}
              (case content-source
                "agent" "Read-only: produced by the translation agent."
                "authored-contract" "Read-only: authored by hand, not produced."
                "Read-only: contract-backed translation."))
         (d/p {:class-name "mt-1 text-amber-200/80"}
              "Its unit of content is a whole file, so there is no persisted segment behind
               it to label \u2014 agent submissions are refused a segment index other than 0,
               and authored locale files were never segmented at all. Scoring writes to
               /api/translations/segments/:id/labels, which resolves :id as a Mongo ObjectId
               and refuses one that names no stored segment \u2014 so a submit here would fail
               rather than record anything.")
         (d/p {:class-name "mt-1 text-amber-200/80"}
              "Scoring, corrections and notes are not shown because there is nothing stored to
               show \u2014 no label can exist without a segment to attach it to, so any values
               here would be invented rather than recalled.")
         (d/p {:class-name "mt-1 text-amber-200/80"}
              "The text itself is real and is what would be published. Approve the whole
               revision above.")))

(defnc segment-detail-panel
  [{:keys [segment form saving on-change on-submit read-only? content-source]}]
  (if-not segment
    (d/p {:class-name "text-sm text-slate-400"} "Click a segment annotation to review it.")
    (d/div {:class-name "space-y-4"}
           (d/div {:class-name "flex items-center justify-between"}
                  (d/h4 {:class-name "text-sm font-semibold text-slate-200"}
                        (str "Segment " (:segment_index segment)))
                  ($ status-badge {:status (:status segment)}))
           (when read-only? ($ authored-review-notice {:content-source content-source}))
           (d/div {:class-name "space-y-3"}
                  (segment-source-block (str "Source (" (logic/lang-name (:source_lang segment)) ")")
                                        (:source_text segment))
                  (segment-source-block (str "Translation (" (logic/lang-name (:target_lang segment)) ")")
                                        (:translated_text segment)))
           ;; Omitted entirely when read-only, not rendered disabled.
           ;;
           ;; `form` holds `logic/default-label` — good / good / correct / safe —
           ;; and an authored translation has no stored label to load into it,
           ;; because it has no persisted segment for a label to hang off. A
           ;; disabled control showing those defaults presents invented scores as
           ;; though they were a reviewer's, which is worse than showing nothing:
           ;; hiding the apparatus loses information, fabricating its contents
           ;; manufactures it.
           (when-not read-only?
             (hx/<>
              ($ label-score-fields {:form form :on-change on-change})
              (label-textarea "Corrected translation" (:corrected_text form)
                              "Optional. If you enter a correction and submit the review, this becomes the rendered translation."
                              4 #(on-change (assoc form :corrected_text (.. % -target -value))))
              (label-textarea "Editor notes" (:editor_notes form)
                              "Terminology caveats, tone issues, etc."
                              2 #(on-change (assoc form :editor_notes (.. % -target -value))))))
           ;; Omitted rather than disabled when read-only. A disabled Submit
           ;; still asserts that submitting is the thing to do here and that
           ;; something is temporarily in the way; neither is true.
           (when-not read-only?
             (d/div {:class-name "flex gap-2"}
                    ($ ui/button {:disabled saving :on-click #(on-submit "approve")} "Submit review")
                    ($ ui/button {:variant :secondary :disabled saving :on-click #(on-submit "needs_edit")} "Submit as in review")
                    ($ ui/button {:variant :ghost :disabled saving :on-click #(on-submit "reject")} "Mark rejected")))
           ($ previous-labels {:labels (:labels segment)}))))

;; ── mutation runners ─────────────────────────────────────────────────────────

(defn- run-segment-submit!
  [segment form overall selected project
   {:keys [set-saving! set-error! set-notice! set-detail! set-form!]}
   reload-docs!]
  (set-saving! true)
  (set-error! nil)
  (let [scope (logic/legacy-review-scope selected project)]
    (-> (api/submit-label (:id segment)
                          scope
                          (logic/prepare-label-payload form overall))
      (.then (fn [_]
               (set-notice! (str "Segment " (:segment_index segment) ": " overall))
               (-> (api/get-document (:document_id selected)
                                     (:target_lang selected)
                                     scope)
                   (.then set-detail!))))
      (.then (fn [_]
               (set-form! logic/default-label)
               (reload-docs!)))
      (.catch (fn [^js err] (set-error! (or (.-message err) (str err)))))
      (.finally #(set-saving! false)))))

(defn- run-document-review!
  [selected overall project
   {:keys [set-saving! set-error! set-notice! set-detail!]}
   reload-docs!]
  (set-saving! true)
  (set-error! nil)
  (let [scope (logic/legacy-review-scope selected project)]
    (-> (api/review-document (:document_id selected)
                             (:target_lang selected)
                             scope
                             {:overall overall})
      (.then (fn [result]
               (set-notice! (str "Document review: " overall
                                 " (" (:segments_reviewed result) " segments)"))
               (reload-docs!)
               (-> (api/get-document (:document_id selected)
                                     (:target_lang selected)
                                     scope)
                   (.then set-detail!))))
      (.catch (fn [^js err] (set-error! (or (.-message err) (str err)))))
      (.finally #(set-saving! false)))))

(defn- run-publication-approval!
  [selected {:keys [set-saving! set-error! set-notice!]} reload-docs!]
  (set-saving! true)
  (set-error! nil)
  (let [review (:publication_review selected)]
    (-> (api/approve-publication-translation
         (logic/approval-request review))
      (.then (fn [_] (api/reconcile-publication (:publication review))))
      (.then (fn [receipt]
               (set-notice! (str "Translation approved; publication reconciliation: "
                                 (or (:type receipt) "recorded") "."))
               (reload-docs!)))
      (.catch (fn [^js err] (set-error! (or (.-message err) (str err)))))
      (.finally #(set-saving! false)))))

(defn- run-translation-dispatch!
  [selected {:keys [set-saving! set-error! set-notice!]} reload-docs!]
  (set-saving! true)
  (set-error! nil)
  (-> (api/dispatch-publication-translation (:publication selected))
      (.then (fn [result]
               (let [outcome (or (some-> result :dispatched first :dispatch/outcome name)
                                 (some-> result :dispatched first :outcome name)
                                 "recorded")]
                 (set-notice! (str "Translation dispatch: " outcome ".")))
               (reload-docs!)))
      (.catch (fn [^js err] (set-error! (or (.-message err) (str err)))))
      (.finally #(set-saving! false))))

(defn- run-export! [project target-lang {:keys [set-notice! set-error!]}]
  (-> (api/sft-export {:project project :target-lang target-lang})
      (.then (fn [text]
               (let [blob (js/Blob. #js [text] #js {:type "application/x-ndjson"})
                     url (js/URL.createObjectURL blob)
                     a (.createElement js/document "a")]
                 (set! (.-href a) url)
                 (set! (.-download a) (logic/sft-filename project target-lang))
                 (.click a)
                 (js/URL.revokeObjectURL url)
                 (set-notice! "SFT export downloaded."))))
      (.catch (fn [^js err] (set-error! (or (.-message err) (str err)))))))

;; ── header + panes ───────────────────────────────────────────────────────────

(defnc review-header
  [{:keys [project set-project target-lang set-target-lang manifest
           show-config? toggle-config on-export]}]
  (d/div {:class-name "shrink-0 border-b border-slate-800 bg-slate-950/90 px-4 py-3"}
         (d/div {:class-name "flex items-center justify-between"}
                (d/h1 {:class-name "text-lg font-bold text-slate-100"} "Translation Review")
                (d/div {:class-name "flex items-center gap-2"}
                       ($ ui/button {:variant :ghost :on-click toggle-config}
                          (if show-config? "Hide Config" "⚙ Pipeline"))
                       ($ ui/button {:variant :ghost :on-click on-export} "Export SFT")))
         (d/div {:class-name "mt-2 flex gap-3"}
                (d/label {:class-name "block text-sm"}
                         (d/span {:class-name "mb-1 block text-xs font-medium text-slate-400"} "Project")
                         (d/div {:class-name "w-28"}
                                ($ ui/input {:value project
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
                                        " approved"))))))
         (when show-config?
           (d/div {:class-name "mt-3"}
                  ($ model-section {:can-manage true})))))

(defn- dismissable-banner [text tone on-dismiss]
  (d/div {:class-name (str "shrink-0 border-b px-4 py-2 text-sm "
                           (if (= tone :success)
                             "border-emerald-500/40 bg-emerald-500/10 text-emerald-300"
                             "border-rose-500/40 bg-rose-500/10 text-rose-300"))}
         text
         (d/button {:class-name "ml-2 underline" :on-click on-dismiss} "dismiss")))

(defnc document-actions
  [{:keys [selected project saving on-review on-publication-approval
           on-translation-dispatch]}]
  (let [legacy-mutation? (logic/legacy-mutation-admitted? selected project)]
    (d/div {:class-name "flex gap-2"}
           (when (logic/allowed-action? selected "dispatch")
             ($ ui/button {:size :sm :variant :primary :disabled saving
                           :on-click on-translation-dispatch}
                "Dispatch"))
           (when (logic/allowed-action? selected "retry")
             ($ ui/button {:size :sm :variant :primary :disabled saving
                           :on-click on-translation-dispatch}
                "Retry"))
           (when legacy-mutation?
             ($ ui/button {:size :sm :disabled saving
                           :on-click #(on-review "approve")}
                "Approve All"))
           (when-let [review (:publication_review selected)]
             (when (and (true? (:reviewable review))
                        (:contract_content selected)
                        (not (logic/blocked-resource-candidate? selected)))
               (if (:approved review)
                 (d/span {:class-name "rounded bg-emerald-900/30 px-3 py-2 text-xs font-medium text-emerald-300"}
                         "Approved for publication")
                 ($ ui/button {:size :sm :variant :primary :disabled saving
                               :on-click on-publication-approval}
                    "Approve for publication"))))
           (when legacy-mutation?
             ($ ui/button {:size :sm :variant :secondary :disabled saving
                           :on-click #(on-review "needs_edit")}
                "Needs Edit"))
           (when legacy-mutation?
             ($ ui/button {:size :sm :variant :ghost :disabled saving
                           :on-click #(on-review "reject")}
                "Reject All")))))

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

(defnc candidate-or-work-state
  [{:keys [selected detail seg-idx set-seg-idx]}]
  (d/div {:class-name "min-h-0 flex-1 overflow-auto p-4"}
         (cond
           (logic/blocked-resource-candidate? selected)
           (d/div {:class-name "rounded-lg border border-rose-900/50 bg-rose-950/20 p-5"}
                  (d/h3 {:class-name "text-sm font-semibold text-rose-200"}
                        "Translation candidate content unavailable")
                  (d/p {:class-name "mt-2 text-sm text-rose-200/80"}
                       (blocked-candidate-copy selected)))

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
                    ($ segment-annotation {:key (:id seg)
                                           :segment seg
                                           :selected? (= seg-idx (:segment_index seg))
                                           :on-select #(set-seg-idx (:segment_index seg))})))

           :else
           (d/div {:class-name "rounded-lg border border-slate-700 bg-slate-900/40 p-5"}
                  (d/h3 {:class-name "text-sm font-semibold text-slate-100"}
                        "No translation candidate yet")
                  (d/p {:class-name "mt-2 text-sm text-slate-400"}
                       (str "Resource work is " (logic/status-label (:work_state selected))
                            ". Its row remains visible because the resource graph, not completed receipts, owns this inventory."))))))

(defnc document-pane
  [{:keys [selected project detail detail-loading saving seg-idx set-seg-idx
           on-review on-publication-approval on-translation-dispatch]}]
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
     (d/div {:class-name "shrink-0 border-b border-slate-800 bg-slate-950/90 px-4 py-3"}
            (d/div {:class-name "flex items-center justify-between"}
                   (d/div
                    (d/h2 {:class-name "text-base font-semibold text-slate-100"}
                          (get-in detail [:document :title]))
                    (d/div {:class-name "mt-0.5 flex items-center gap-3 text-xs text-slate-400"}
                           (d/span (str (logic/lang-name (get-in detail [:document :source_lang]))
                                        " → " (logic/lang-name (:target_lang selected))))
                           (d/span (str "· " (get-in detail [:summary :total_segments]) " segments"))
                           ($ status-badge {:status (get-in detail [:summary :overall_status])})))
                   ($ document-actions {:selected selected :project project :saving saving
                                        :on-review on-review
                                        :on-publication-approval on-publication-approval
                                        :on-translation-dispatch on-translation-dispatch}))
            ($ progress-bar {:approved (get-in detail [:summary :approved])
                             :total (get-in detail [:summary :total_segments])}))
     ($ candidate-or-work-state {:selected selected :detail detail
                                 :seg-idx seg-idx :set-seg-idx set-seg-idx}))))

(defnc document-list-pane
  [{:keys [loading documents selected set-selected]}]
  (d/aside {:class-name "flex w-72 shrink-0 flex-col overflow-auto border-r border-slate-800 p-3"}
           (cond
             loading (d/p {:class-name "text-sm text-slate-400"} "Loading documents…")
             (empty? documents) (d/p {:class-name "text-sm text-slate-400"} "No translated documents found.")
             :else (d/div {:class-name "space-y-2"}
                          (for [doc documents]
                            ($ document-card {:key (pr-str (logic/work-row-id doc))
                                              :doc doc
                                              :selected? (and selected (logic/same-work? doc selected))
                                              :on-select #(set-selected doc)}))))))

(defnc segment-review-pane
  [{:keys [work-row segment form saving set-form on-submit read-only? content-source]}]
  (d/aside {:class-name "flex w-[440px] shrink-0 flex-col overflow-hidden"}
           (d/div {:class-name "shrink-0 border-b border-slate-800 px-4 py-3"}
                  (d/h3 {:class-name "text-sm font-semibold text-slate-200"} "Segment Review"))
           (d/div {:class-name "min-h-0 flex-1 overflow-auto p-4"}
                  (cond
                    (logic/blocked-resource-candidate? work-row)
                    (d/p {:class-name "text-sm text-rose-300"}
                         (blocked-candidate-copy work-row))

                    (and (logic/work-row? work-row)
                         (not (logic/legacy-candidate? work-row)))
                    (d/p {:class-name "text-sm text-slate-400"}
                         (if (logic/candidate-present? work-row)
                           "This resource candidate has no admitted persisted split set. Legacy mutation controls remain unavailable."
                           "Dispatch this work item to create a candidate. Real persisted splits will appear here for review."))

                    :else
                    ($ segment-detail-panel {:segment segment :form form :saving saving
                                             :on-change set-form :on-submit on-submit
                                             :read-only? read-only?
                                             :content-source content-source})))))

(defnc translation-review-layout
  [{:keys [project set-project target-lang set-target-lang manifest show-config?
           toggle-config on-export notice dismiss-notice error dismiss-error
           loading documents selected set-selected detail detail-loading saving
           seg-idx set-seg-idx form set-form selected-segment on-review
           on-publication-approval on-translation-dispatch on-segment-submit]}]
  (d/div {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden"}
         ($ review-header {:project project :set-project set-project
                           :target-lang target-lang :set-target-lang set-target-lang
                           :manifest manifest :show-config? show-config?
                           :toggle-config toggle-config :on-export on-export})
         (when notice (dismissable-banner notice :success dismiss-notice))
         (when error (dismissable-banner error :error dismiss-error))
         (d/div {:class-name "flex min-h-0 flex-1 overflow-hidden"}
                ($ document-list-pane {:loading loading :documents documents
                                       :selected selected :set-selected set-selected})
                (d/main {:class-name "flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden border-r border-slate-800"}
                ($ document-pane {:selected selected :detail detail
                                          :project project
                                          :detail-loading detail-loading :saving saving
                                          :seg-idx seg-idx :set-seg-idx set-seg-idx
                                          :on-review on-review
                                          :on-publication-approval on-publication-approval
                                          :on-translation-dispatch on-translation-dispatch}))
                ($ segment-review-pane {:work-row selected :segment selected-segment
                                        :form form :saving saving :set-form set-form
                                        :read-only? (not (logic/legacy-mutation-admitted?
                                                          selected project))
                                        :content-source (:content_source selected)
                                        :on-submit on-segment-submit}))))

;; ── page ─────────────────────────────────────────────────────────────────────

(defn- use-review-loads!
  [{:keys [project target-lang selected selected-segment setters
           document-load-seq manifest-load-seq detail-load-seq
           set-manifest! set-detail! set-seg-idx! set-form!]}]
  (hooks/use-effect
   [project target-lang]
   (let [load-id (controller/load-documents! project target-lang
                                             document-load-seq setters)]
     (fn [] (controller/cancel! document-load-seq load-id))))
  (hooks/use-effect
   [project]
   (let [load-id (controller/load-manifest! project manifest-load-seq set-manifest!)]
     (fn [] (controller/cancel! manifest-load-seq load-id))))
  (hooks/use-effect
   [selected project]
   (if selected
     (let [load-id (controller/load-detail! selected project detail-load-seq setters)]
       (fn [] (controller/cancel! detail-load-seq load-id)))
     (do
       (controller/cancel! detail-load-seq (.-current ^js detail-load-seq))
       (set-detail! nil)
       (set-seg-idx! nil)
       nil)))
  (hooks/use-effect
   [(:id selected-segment)]
   (set-form! (logic/segment-review-form selected-segment))
   nil))

(defnc translation-review-page []
  (let [[project set-project!] (hooks/use-state "devel")
        [target-lang set-target-lang!] (hooks/use-state "")
        [documents set-documents!] (hooks/use-state [])
        [selected set-selected!] (hooks/use-state nil)
        [detail set-detail!] (hooks/use-state nil)
        [seg-idx set-seg-idx!] (hooks/use-state nil)
        [form set-form!] (hooks/use-state logic/default-label)
        [manifest set-manifest!] (hooks/use-state nil)
        [loading set-loading!] (hooks/use-state true)
        [detail-loading set-detail-loading!] (hooks/use-state false)
        [saving set-saving!] (hooks/use-state false)
        [notice set-notice!] (hooks/use-state nil)
        [error set-error!] (hooks/use-state nil)
        [show-config? set-show-config!] (hooks/use-state false)
        document-load-seq (hooks/use-ref 0)
        manifest-load-seq (hooks/use-ref 0)
        detail-load-seq (hooks/use-ref 0)
        setters {:set-loading! set-loading! :set-error! set-error!
                 :set-documents! set-documents! :set-selected! set-selected!
                 :set-project! set-project!
                 :set-detail! set-detail! :set-detail-loading! set-detail-loading!
                 :set-seg-idx! set-seg-idx! :set-form! set-form!
                 :set-saving! set-saving! :set-notice! set-notice!}
        reload-docs! #(controller/load-documents! project target-lang
                                                   document-load-seq setters)
        selected-segment (logic/find-segment detail seg-idx)]
    (use-review-loads!
     {:project project :target-lang target-lang :selected selected
      :selected-segment selected-segment :setters setters
      :document-load-seq document-load-seq
      :manifest-load-seq manifest-load-seq :detail-load-seq detail-load-seq
      :set-manifest! set-manifest! :set-detail! set-detail!
      :set-seg-idx! set-seg-idx! :set-form! set-form!})
    ($ translation-review-layout
       {:project project :set-project set-project!
        :target-lang target-lang :set-target-lang set-target-lang!
        :manifest manifest :show-config? show-config?
        :toggle-config #(set-show-config! not)
        :on-export #(run-export! project target-lang setters)
        :notice notice :dismiss-notice #(set-notice! nil)
        :error error :dismiss-error #(set-error! nil)
        :loading loading :documents documents :selected selected
        :set-selected set-selected! :detail detail :detail-loading detail-loading
        :saving saving :seg-idx seg-idx :set-seg-idx set-seg-idx!
        :form form :set-form set-form! :selected-segment selected-segment
        :on-review #(run-document-review! selected % project setters reload-docs!)
        :on-publication-approval
        #(run-publication-approval! selected setters reload-docs!)
        :on-translation-dispatch
        #(run-translation-dispatch! selected setters reload-docs!)
        :on-segment-submit
        #(run-segment-submit! selected-segment form % selected project
                              setters reload-docs!)})))
