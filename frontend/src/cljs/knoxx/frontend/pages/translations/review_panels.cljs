(ns knoxx.frontend.pages.translations.review-panels
  "Translation annotations and granular review presentation."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.components.ui :as ui]
            [knoxx.frontend.pages.translations.logic :as logic]
            [knoxx.frontend.pages.translations.review-controls :as review-controls]
            [knoxx.frontend.pages.translations.review-history :as review-history]))

(hx/defnc status-badge
  "Display the actual translation review status."
  [{:keys [status]}]
  (d/span {:class-name (str "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium "
                            (logic/status-class status))}
          (str (logic/status-icon status) " " (logic/status-label status))))

(hx/defnc progress-bar
  "Show approved segment count against the complete candidate."
  [{:keys [approved total]}]
  (d/div {:class-name "flex items-center gap-2"}
         (d/div {:class-name "h-1.5 flex-1 rounded-full bg-slate-700"}
                (d/div {:class-name "h-1.5 rounded-full bg-emerald-500 transition-all"
                        :style #js {:width (str (logic/progress-pct approved total) "%")}}))
         (d/span {:class-name "text-xs text-slate-400"}
                 (str approved "/" total))))

(hx/defnc document-card
  "Select one desired translation work item."
  [{:keys [doc selected? on-select]}]
  (d/button {:type "button"
             :on-click on-select
             :class-name (str "w-full rounded-lg border p-3 text-left transition "
                              (if selected?
                                "border-blue-400 bg-blue-500/10"
                                "border-slate-700 bg-slate-900/50 hover:border-slate-600"))}
            (d/div {:class-name "mb-1.5 flex items-start justify-between gap-2"}
                   (d/span {:class-name "line-clamp-1 text-sm font-semibold text-slate-100"}
                           (:title doc))
                   (hx/$ status-badge {:status (:overall_status doc)}))
            (d/div {:class-name "mb-2 text-xs text-slate-400"}
                   (str (logic/lang-name (:source_lang doc)) " → " (logic/lang-name (:target_lang doc)))
                   (when (:garden_id doc)
                     (d/span {:class-name "ml-2"} (str "· " (:garden_id doc)))))
            (hx/$ progress-bar {:approved (:approved doc) :total (:total_segments doc)})))

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

(hx/defnc segment-annotation
  "Show exact source and translated text for one segment."
  [{:keys [segment selected? on-select]}]
  (let [{:keys [segment_index status label_count source_lang target_lang
                source_text translated_text]} segment]
    (d/button {:type "button"
               :on-click on-select
               :aria-pressed (boolean selected?)
               :style #js {:cursor "pointer"
                           :borderRadius 6
                           :borderLeft (str "4px solid " (segment-border selected? status))
                           :padding "8px 12px"}
               :class-name "w-full bg-slate-900/40 text-left transition"}
           (d/div {:class-name "mb-2 flex items-center gap-2 text-xs"}
                  (d/span {:class-name "text-slate-500"} (str "seg " segment_index))
                  (d/span (logic/status-icon status))
                  (d/span {:class-name "text-slate-400"} status)
                  (when (and (some? label_count) (pos? label_count))
                    (d/span {:class-name "text-slate-500"}
                            (str label_count " review" (when (not= 1 label_count) "s"))))
                  (when (:corrected_text segment)
                    (d/span {:class-name "rounded bg-blue-500/10 px-1.5 py-0.5 text-blue-300"}
                            "corrected")))
           (d/div {:class-name "grid grid-cols-2 gap-3"}
                  (segment-text-column (str "Source (" (logic/lang-name source_lang) ")") source_text)
                  (segment-text-column (str "Translation (" (logic/lang-name target_lang) ")") translated_text)))))

;; ── segment review panel ─────────────────────────────────────────────────────

(defn- segment-source-block [title text]
  (d/div {:class-name "rounded-lg border border-slate-700 bg-slate-800/50 p-3"}
         (d/h5 {:class-name "mb-2 text-xs font-semibold text-slate-400"} title)
         (d/pre {:class-name "whitespace-pre-wrap break-words text-sm text-slate-200"} text)))

(hx/defnc previous-labels
  "Display persisted labels without inventing reviewer scores."
  [{:keys [labels]}]
  (hx/$ review-history/review-history {:labels labels :title "Previous labels"}))

(hx/defnc authored-review-notice
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

(defn- legacy-review-fields [form on-change]
  (hx/<>
              (hx/$ review-controls/score-fields {:form form :on-change on-change})
              (review-controls/textarea
               "Corrected translation" (:corrected_text form)
               "Optional. If you enter a correction and submit the review, this becomes the rendered translation."
               4 #(on-change (assoc form :corrected_text
                                    (.. % -target -value))))
              (review-controls/textarea
               "Editor notes" (:editor_notes form)
               "Terminology caveats, tone issues, etc."
               2 #(on-change (assoc form :editor_notes
                                    (.. % -target -value))))))

(hx/defnc segment-detail-panel
  "Review one legacy segment or explain why it is read-only."
  [{:keys [segment form saving on-change on-submit read-only? content-source]}]
  (if-not segment
    (d/p {:class-name "text-sm text-slate-400"} "Click a segment annotation to review it.")
    (d/div {:class-name "space-y-4"}
           (d/div {:class-name "flex items-center justify-between"}
                  (d/h4 {:class-name "text-sm font-semibold text-slate-200"}
                        (str "Segment " (:segment_index segment)))
                  (hx/$ status-badge {:status (:status segment)}))
           (when read-only? (hx/$ authored-review-notice {:content-source content-source}))
           (d/div {:class-name "space-y-3"}
                  (segment-source-block (str "Source (" (logic/lang-name (:source_lang segment)) ")")
                                        (:source_text segment))
                  (segment-source-block (str "Translation (" (logic/lang-name (:target_lang segment)) ")")
                                        (:translated_text segment)))
           (when-not read-only?
             (legacy-review-fields form on-change))
           (when-not read-only?
             (d/div {:class-name "flex gap-2"}
                    (hx/$ ui/button {:disabled saving :on-click #(on-submit "approve")} "Submit review")
                    (hx/$ ui/button {:variant :secondary :disabled saving :on-click #(on-submit "needs_edit")} "Submit as in review")
                    (hx/$ ui/button {:variant :ghost :disabled saving :on-click #(on-submit "reject")} "Mark rejected")))
           (hx/$ previous-labels {:labels (:labels segment)}))))

;; ── mutation runners ─────────────────────────────────────────────────────────

