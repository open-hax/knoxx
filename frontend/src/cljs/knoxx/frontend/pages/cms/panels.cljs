(ns knoxx.frontend.pages.cms.panels
  "Wiki article, source editing, shared review commands and remembered writing lessons."
  (:require [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.pages.cms.logic :as logic]))

(defn field
  "Render a label tied to a controlled authoring input."
  [label value on-change rows]
  (d/label {:class-name "block text-sm"}
           (d/span {:class-name "field-label"} label)
           (if rows
             (d/textarea {:aria-label label :class-name "input" :rows rows :value (or value "")
                          :on-change #(on-change (.. % -target -value))})
             (d/input {:aria-label label :class-name "input" :value (or value "")
                       :on-change #(on-change (.. % -target -value))}))))

(hx/defnc article
  "Render current saved source through the existing Markdown bridge."
  [{:keys [snapshot Markdown]}]
  (d/article {:class-name "prose prose-invert max-w-none"}
             (if Markdown (hx/$ Markdown {:content (:content snapshot)})
                 (d/pre {:class-name "whitespace-pre-wrap"} (:content snapshot)))))

(hx/defnc source-editor
  "Show immutable title metadata and a source draft bound to its saved revision."
  [{:keys [editor set-content! save! can-write? busy?]}]
  (d/section {:class-name "space-y-5" :aria-label "Source editor"}
             (d/label {:class-name "block text-sm"}
                      (d/span {:class-name "field-label"} "Page title")
                      (d/input {:class-name "input" :aria-label "Page title"
                                :value (:title editor) :read-only true}))
             (d/p {:class-name "text-xs opacity-60"} "Page titles are set when the resource is created.")
             (d/label {:class-name "block text-sm"}
                      (d/span {:class-name "field-label"} "Source content")
                      (d/textarea {:aria-label "Source content" :class-name "input min-h-[24rem]"
                                   :rows 16 :value (:content editor) :read-only (not can-write?)
                                   :on-change #(set-content! (.. % -target -value))}))
             (d/div {:class-name "flex items-center justify-between gap-4"}
                    (d/p {:class-name "text-xs opacity-60"}
                         "Saving creates a source revision. Existing acceptance applies only to its reviewed revision.")
                    (d/button {:class-name "btn btn-primary" :type "button" :on-click save!
                               :disabled (boolean (or busy? (not can-write?) (not (logic/writable-draft? editor))))}
                              "Save revision"))))

(defn- history-entry [event]
  (d/li {:key (:id event) :class-name "rounded-lg border border-white/10 p-3 text-sm"}
        (d/div {:class-name "flex justify-between gap-3"}
               (d/span (get-in event [:actor :id]))
               (d/time {:class-name "text-xs opacity-60"} (str (:recorded_at event))))
        (d/p {:class-name "text-xs opacity-70"} (:action event))
        (when-not (str/blank? (:notes event))
          (d/p {:class-name "mt-2 whitespace-pre-wrap"} (:notes event)))
        (for [[idx correction] (map-indexed vector (:corrections event))]
          (d/div {:key idx :class-name "mt-2 text-xs"}
                 (d/del (:before correction)) " → " (d/ins (:after correction))
                 (d/p (:reason correction))))
        (for [lesson (:lessons event)] (d/p {:key lesson :class-name "mt-2 text-xs"} lesson))
        (d/code {:class-name "mt-2 block truncate text-[10px] opacity-50"} (:revision event))))

(hx/defnc review-history
  "Keep human and agent events visible in the same revision discussion."
  [{:keys [snapshot]}]
  (d/section {:class-name "space-y-2" :aria-label "Revision discussion"}
             (d/h3 {:class-name "text-sm font-semibold"} "Revision discussion")
             (if (seq (:history snapshot))
               (d/ol {:class-name "space-y-2"} (map history-entry (reverse (:history snapshot))))
               (d/p {:class-name "text-sm opacity-60"} "No discussion yet. Submit this source for review when it is ready."))))

(defn- review-button [props action label]
  (let [{:keys [snapshot editor review-form busy? can-comment? can-review? review!]} props
        reviewer? (contains? #{"accept" "request_changes"} action)
        feedback? (or (not (str/blank? (:notes review-form))) (seq (:before review-form))
                      (seq (:after review-form)))
        corrections? (or (seq (:before review-form)) (seq (:after review-form)))
        disabled? (or busy? (logic/dirty? editor)
                      (not (logic/review-form-current? review-form snapshot))
                      (not (logic/review-action-available? snapshot action))
                      (not (if reviewer? can-review? can-comment?))
                      (and (= action "request_changes") (not feedback?))
                      (and (= action "accept") corrections?))]
    (d/button {:type "button" :class-name (str "btn " (when (= action "accept") "btn-primary"))
               :disabled (boolean disabled?) :on-click #(review! action)} label)))

(defn- proposed-correction [review-form change!]
  (d/details {:class-name "rounded-lg border border-white/10 p-3"}
                          (d/summary {:class-name "cursor-pointer text-sm"} "Propose a correction")
                          (d/div {:class-name "mt-3 space-y-3"}
                                 (field "Before" (:before review-form) #(change! :before %) 2)
                                 (field "After" (:after review-form) #(change! :after %) 2)
                                 (field "Reason" (:reason review-form) #(change! :reason %) 2)
                                 (d/p {:class-name "text-xs opacity-60"}
                                      "Apply proposed corrections in the source editor and save a new revision before acceptance."))))

(hx/defnc source-review
  "Discuss, revise and accept the exact source before translation work is admitted."
  [{:keys [snapshot review-form set-review-form! discard-review! acknowledge-review!] :as props}]
  (let [change! #(set-review-form! (assoc review-form %1 %2))]
    (d/section {:class-name "space-y-5" :aria-label "Source review"}
               (d/header (d/h2 {:class-name "text-xl font-semibold"} "Review the source")
                         (d/p {:class-name "mt-1 text-sm opacity-60"}
                              "Discuss the current revision, record a correction, and retain what the team learned."))
               (when-not (logic/review-form-current? review-form snapshot)
                 (d/div {:role "alert" :class-name "rounded-lg border border-amber-500/50 p-3 text-sm"}
                        (d/p "The source or discussion changed. Your notes are preserved against the earlier review.")
                        (d/button {:class-name "btn mt-2" :on-click acknowledge-review!} "Review latest revision")))
               (field "Review notes" (:notes review-form) #(change! :notes %) 3)
               (proposed-correction review-form change!)
               (field "Lessons for future drafts" (:lessons review-form) #(change! :lessons %) 3)
               (d/p {:class-name "text-xs opacity-60"}
                    "One reusable lesson per line. Lessons become writing memory when the source is accepted.")
               (d/div {:class-name "flex flex-wrap gap-2"}
                      (review-button props "comment" "Add comment")
                      (review-button props "submit" "Submit for content review")
                      (review-button props "request_changes" "Request revision")
                      (review-button props "accept" "Accept source content")
                      (when (logic/review-dirty? review-form)
                        (d/button {:class-name "btn btn-ghost" :on-click discard-review!} "Discard review notes")))
               (hx/$ review-history {:snapshot snapshot}))))

(hx/defnc workflow
  "Make source acceptance and later translation acceptance separate visible decisions."
  [{:keys [snapshot]}]
  (d/section {:class-name "space-y-3"}
             (d/h2 {:class-name "font-semibold"} "Publication workflow")
             (d/span {:class-name "rounded-full border border-cyan-500/30 px-2 py-0.5 text-xs text-cyan-200"}
                     (logic/status-label (:status snapshot)))
             (d/ol {:class-name "space-y-2 text-xs opacity-70"}
                   (d/li "1. Draft and improve source") (d/li "2. Review and accept source")
                   (d/li "3. Translate, review, and learn") (d/li "4. Accept translations and publish"))
             (when (:accepted snapshot)
               (d/a {:href "/translations" :class-name "block text-sm text-cyan-300"}
                    "Open translation reviews →"))))

(hx/defnc assistant-panel
  "Offer real model suggestions and accepted memory without implicitly writing source."
  [{:keys [snapshot instruction set-instruction! suggestion ask! adopt! editor busy? can-assist? can-write?]}]
  (d/section {:class-name "space-y-3 border-t border-white/10 pt-5" :aria-label "Writing assistant"}
             (d/h2 {:class-name "font-semibold"} "Writing assistant")
             (d/p {:class-name "text-xs opacity-60"}
                  "Brainstorm, draft, or improve this page using its prior review lessons.")
             (field "Assistant instruction" instruction set-instruction! 4)
             (d/button {:class-name "btn btn-primary" :on-click ask!
                        :disabled (boolean (or busy? (not can-assist?) (str/blank? instruction)))}
                       "Ask writing assistant")
             (when suggestion
               (d/div {:class-name "space-y-3 rounded-lg border border-white/10 p-3"}
                      (d/p {:class-name "text-[10px] font-bold uppercase tracking-wider text-cyan-300"}
                           "Suggestion · review before using")
                      (d/pre {:class-name "whitespace-pre-wrap text-xs leading-6"}
                             (or (:content suggestion) (:text suggestion) (:suggestion suggestion)))
                      (d/button {:class-name "btn" :on-click adopt!
                                 :disabled (boolean (or busy? (not can-write?) (logic/dirty? editor)
                                                       (not= (:revision snapshot) (:basis suggestion))))}
                                "Use suggestion in draft")))
             (d/div {:class-name "space-y-2"}
                    (d/h3 {:class-name "text-sm font-semibold"} "Remembered writing lessons")
                    (if (seq (:lessons snapshot))
                      (d/ul {:class-name "list-inside list-disc space-y-2 text-xs opacity-70"}
                            (for [lesson (:lessons snapshot)]
                              (d/li {:key (str (:review lesson) (:text lesson))} (:text lesson))))
                      (d/p {:class-name "text-xs opacity-60"}
                           "Accepted review lessons will appear here and inform the next draft.")))))
