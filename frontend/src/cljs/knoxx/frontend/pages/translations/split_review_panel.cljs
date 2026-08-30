(ns knoxx.frontend.pages.translations.split-review-panel
  "Reusable editor for one authenticated resource translation split."
  (:require [helix.core :refer [$ defnc]]
            [helix.dom :as d]
            [knoxx.frontend.components.ui :as ui]
            [knoxx.frontend.pages.translations.logic :as logic]
            [knoxx.frontend.pages.translations.review-controls :as review-controls]
            [knoxx.frontend.pages.translations.review-history
             :refer [review-history]]))

(defn- status-badge
  [status]
  (d/span {:class-name
           (str "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium "
                (logic/status-class status))}
          (str (logic/status-icon status) " " (logic/status-label status))))

(defn- text-block
  [title text]
  (d/div {:class-name "rounded-lg border border-slate-700 bg-slate-800/50 p-3"}
         (d/h5 {:class-name "mb-2 text-xs font-semibold text-slate-400"} title)
         (d/pre {:class-name "whitespace-pre-wrap break-words text-sm text-slate-200"}
                text)))

(defn- review-actions
  [saving on-submit on-skip]
  (d/div {:class-name "flex flex-wrap gap-2"}
         ($ ui/button {:disabled saving :on-click #(on-submit "approved")}
            "Approve split")
         ($ ui/button {:variant :secondary :disabled saving
                       :on-click #(on-submit "in-review")}
            "Submit review")
         ($ ui/button {:variant :ghost :disabled saving
                       :on-click #(on-submit "rejected")}
            "Reject split")
         ($ ui/button {:variant :ghost :disabled (or saving (nil? on-skip))
                       :on-click on-skip}
            "Skip")))

(defn- granular-review-controls
  [form on-change]
  (d/div
   {:class-name "space-y-4"}
   ($ review-controls/score-fields {:form form :on-change on-change})
   (review-controls/textarea
    "Corrected translation" (:corrected_text form)
    "Optional. An approved correction becomes this split's effective target and future translation memory."
    7 #(on-change (assoc form :corrected_text (.. % -target -value))))
   (review-controls/textarea
    "Editor notes" (:editor_notes form)
    "Terminology caveats, tone issues, reviewer rationale, etc."
    3 #(on-change (assoc form :editor_notes (.. % -target -value))))))

(defnc split-review-panel
  "Render the historical granular review card for one resource split."
  [{:keys [split form saving on-change on-submit on-skip]}]
  (if-not split
    (d/p {:class-name "text-sm text-slate-400"}
         "Choose a split to review its source and candidate translation.")
    (d/div {:class-name "space-y-4"}
           (d/div {:class-name "flex items-center justify-between"}
                  (d/h4 {:class-name "text-sm font-semibold text-slate-200"}
                        (str "Split " (:segment_index split)))
                  (status-badge (:status split)))
           (d/div {:class-name "space-y-3"}
                  (text-block
                   (str "Source (" (logic/lang-name (:source_lang split)) ")")
                   (:source_text split))
                  (text-block
                   (str "Candidate (" (logic/lang-name (:target_lang split)) ")")
                   (:candidate_text split)))
           (granular-review-controls form on-change)
           (d/p {:class-name "text-xs text-slate-500"}
                "Submit review keeps this split in review. Approve selects the correction above, or the candidate when it is blank; scores and notes remain attached to this split's review history.")
           (review-actions saving on-submit on-skip)
           ($ review-history {:labels (:labels split)
                              :title "Existing labels"
                              :empty-copy "No labels yet."}))))
