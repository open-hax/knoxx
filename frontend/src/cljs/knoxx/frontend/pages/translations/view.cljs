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
            [knoxx.frontend.pages.translations.model-section :refer [model-section]]))

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

(defnc segment-detail-panel
  [{:keys [segment form saving on-change on-submit]}]
  (if-not segment
    (d/p {:class-name "text-sm text-slate-400"} "Click a segment annotation to review it.")
    (d/div {:class-name "space-y-4"}
           (d/div {:class-name "flex items-center justify-between"}
                  (d/h4 {:class-name "text-sm font-semibold text-slate-200"}
                        (str "Segment " (:segment_index segment)))
                  ($ status-badge {:status (:status segment)}))
           (d/div {:class-name "space-y-3"}
                  (segment-source-block (str "Source (" (logic/lang-name (:source_lang segment)) ")")
                                        (:source_text segment))
                  (segment-source-block (str "Translation (" (logic/lang-name (:target_lang segment)) ")")
                                        (:translated_text segment)))
           ($ label-score-fields {:form form :on-change on-change})
           (label-textarea "Corrected translation" (:corrected_text form)
                           "Optional. If you enter a correction and submit the review, this becomes the rendered translation."
                           4 #(on-change (assoc form :corrected_text (.. % -target -value))))
           (label-textarea "Editor notes" (:editor_notes form)
                           "Terminology caveats, tone issues, etc."
                           2 #(on-change (assoc form :editor_notes (.. % -target -value))))
           (d/div {:class-name "flex gap-2"}
                  ($ ui/button {:disabled saving :on-click #(on-submit "approve")} "Submit review")
                  ($ ui/button {:variant :secondary :disabled saving :on-click #(on-submit "needs_edit")} "Submit as in review")
                  ($ ui/button {:variant :ghost :disabled saving :on-click #(on-submit "reject")} "Mark rejected"))
           ($ previous-labels {:labels (:labels segment)}))))

;; ── async runners ────────────────────────────────────────────────────────────

(defn- run-load-documents!
  [project target-lang selected {:keys [set-loading! set-error! set-documents!
                                        set-selected! set-detail!]}]
  (set-loading! true)
  (set-error! nil)
  (-> (js/Promise.all
       #js [(api/list-documents {:project project :target-lang target-lang})
            (api/list-publication-reviews)])
      (.then (fn [results]
               (let [documents-response (aget results 0)
                     reviews-response (aget results 1)
                     reviews (cond->> (:reviews reviews-response)
                               (seq target-lang)
                               (filter #(= target-lang (:locale %))))
                     documents (logic/attach-publication-reviews
                                (:documents documents-response)
                                reviews)]
                 (set-documents! documents)
                 (when selected
                   (if (logic/still-listed? documents selected)
                     (let [updated (first
                                    (filter #(and (= (:document_id %) (:document_id selected))
                                                  (= (:target_lang %) (:target_lang selected)))
                                            documents))]
                       (when (not= (:publication_review updated)
                                   (:publication_review selected))
                         (set-selected! updated)))
                     (do
                       (set-selected! nil)
                       (set-detail! nil)))))))
      (.catch (fn [^js err]
                (set-error! (or (.-message err) (str err)))
                (set-documents! [])))
      (.finally #(set-loading! false))))

(defn- run-load-detail! [selected {:keys [set-detail-loading! set-detail!
                                          set-seg-idx! set-form! set-error!]}]
  (set-detail-loading! true)
  (-> (if (:authored_content selected)
        (js/Promise.resolve (logic/authored-detail selected))
        (api/get-document (:document_id selected) (:target_lang selected)))
      (.then (fn [detail]
               (set-detail! detail)
               (set-seg-idx! nil)
               (set-form! logic/default-label)))
      (.catch (fn [^js err]
                (set-error! (or (.-message err) (str err)))
                (set-detail! nil)))
      (.finally #(set-detail-loading! false))))

(defn- run-segment-submit!
  [segment form overall selected {:keys [set-saving! set-error! set-notice!
                                         set-detail! set-form!]} reload-docs!]
  (set-saving! true)
  (set-error! nil)
  (-> (api/submit-label (:id segment) (logic/prepare-label-payload form overall))
      (.then (fn [_]
               (set-notice! (str "Segment " (:segment_index segment) ": " overall))
               (-> (api/get-document (:document_id selected) (:target_lang selected))
                   (.then set-detail!))))
      (.then (fn [_]
               (set-form! logic/default-label)
               (reload-docs!)))
      (.catch (fn [^js err] (set-error! (or (.-message err) (str err)))))
      (.finally #(set-saving! false))))

(defn- run-document-review!
  [selected overall {:keys [set-saving! set-error! set-notice! set-detail!]} reload-docs!]
  (set-saving! true)
  (set-error! nil)
  (-> (api/review-document (:document_id selected) (:target_lang selected) {:overall overall})
      (.then (fn [result]
               (set-notice! (str "Document review: " overall
                                 " (" (:segments_reviewed result) " segments)"))
               (reload-docs!)
               (-> (api/get-document (:document_id selected) (:target_lang selected))
                   (.then set-detail!))))
      (.catch (fn [^js err] (set-error! (or (.-message err) (str err)))))
      (.finally #(set-saving! false))))

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

(defnc document-pane
  [{:keys [selected detail detail-loading saving seg-idx set-seg-idx
           on-review on-publication-approval]}]
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
                   (d/div {:class-name "flex gap-2"}
                          (when-not (:authored_content selected)
                            ($ ui/button {:size :sm :disabled saving :on-click #(on-review "approve")} "Approve All"))
                          (when-let [review (:publication_review selected)]
                            (if (:approved review)
                              (d/span {:class-name "rounded bg-emerald-900/30 px-3 py-2 text-xs font-medium text-emerald-300"}
                                      "Approved for publication")
                              ($ ui/button {:size :sm :variant :primary :disabled saving
                                            :on-click on-publication-approval}
                                 "Approve for publication")))
                          (when-not (:authored_content selected)
                            ($ ui/button {:size :sm :variant :secondary :disabled saving :on-click #(on-review "needs_edit")} "Needs Edit"))
                          (when-not (:authored_content selected)
                            ($ ui/button {:size :sm :variant :ghost :disabled saving :on-click #(on-review "reject")} "Reject All"))))
            ($ progress-bar {:approved (get-in detail [:summary :approved])
                             :total (get-in detail [:summary :total_segments])}))
     (d/div {:class-name "min-h-0 flex-1 overflow-auto p-4"}
            (d/div {:class-name "space-y-2"}
                   (for [seg (:segments detail)]
                     ($ segment-annotation {:key (:id seg)
                                            :segment seg
                                            :selected? (= seg-idx (:segment_index seg))
                                            :on-select #(set-seg-idx (:segment_index seg))})))))))

(defnc document-list-pane
  [{:keys [loading documents selected set-selected]}]
  (d/aside {:class-name "flex w-72 shrink-0 flex-col overflow-auto border-r border-slate-800 p-3"}
           (cond
             loading (d/p {:class-name "text-sm text-slate-400"} "Loading documents…")
             (empty? documents) (d/p {:class-name "text-sm text-slate-400"} "No translated documents found.")
             :else (d/div {:class-name "space-y-2"}
                          (for [doc documents]
                            ($ document-card {:key (str (:document_id doc) "-" (:target_lang doc))
                                              :doc doc
                                              :selected? (and selected
                                                              (= (:document_id doc) (:document_id selected))
                                                              (= (:target_lang doc) (:target_lang selected)))
                                              :on-select #(set-selected doc)}))))))

(defnc segment-review-pane
  [{:keys [segment form saving set-form on-submit]}]
  (d/aside {:class-name "flex w-[440px] shrink-0 flex-col overflow-hidden"}
           (d/div {:class-name "shrink-0 border-b border-slate-800 px-4 py-3"}
                  (d/h3 {:class-name "text-sm font-semibold text-slate-200"} "Segment Review"))
           (d/div {:class-name "min-h-0 flex-1 overflow-auto p-4"}
                  ($ segment-detail-panel {:segment segment :form form :saving saving
                                           :on-change set-form :on-submit on-submit}))))

;; ── page ─────────────────────────────────────────────────────────────────────

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
        setters {:set-loading! set-loading! :set-error! set-error!
                 :set-documents! set-documents! :set-selected! set-selected!
                 :set-detail! set-detail! :set-detail-loading! set-detail-loading!
                 :set-seg-idx! set-seg-idx! :set-form! set-form!
                 :set-saving! set-saving! :set-notice! set-notice!}
        reload-docs! #(run-load-documents! project target-lang selected setters)
        selected-segment (logic/find-segment detail seg-idx)]
    (hooks/use-effect
     [project target-lang]
     (run-load-documents! project target-lang selected setters)
     (-> (api/get-manifest project)
         (.then set-manifest!)
         (.catch (fn [_] (set-manifest! nil))))
     nil)
    (hooks/use-effect
     [selected]
     (if selected
       (run-load-detail! selected setters)
       (do (set-detail! nil) (set-seg-idx! nil)))
     nil)
    (d/div {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden"}
           ($ review-header {:project project :set-project set-project!
                             :target-lang target-lang :set-target-lang set-target-lang!
                             :manifest manifest
                             :show-config? show-config?
                             :toggle-config #(set-show-config! not)
                             :on-export #(run-export! project target-lang setters)})
           (when notice (dismissable-banner notice :success #(set-notice! nil)))
           (when error (dismissable-banner error :error #(set-error! nil)))
           (d/div {:class-name "flex min-h-0 flex-1 overflow-hidden"}
                  ($ document-list-pane {:loading loading :documents documents
                                         :selected selected :set-selected set-selected!})
                  (d/main {:class-name "flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden border-r border-slate-800"}
                          ($ document-pane {:selected selected :detail detail
                                            :detail-loading detail-loading :saving saving
                                            :seg-idx seg-idx :set-seg-idx set-seg-idx!
                                            :on-review (fn [overall]
                                                         (run-document-review! selected overall setters reload-docs!))
                                            :on-publication-approval
                                            #(run-publication-approval! selected setters reload-docs!)}))
                  (when-not (:authored_content selected)
                    ($ segment-review-pane {:segment selected-segment :form form :saving saving
                                            :set-form set-form!
                                            :on-submit (fn [overall]
                                                         (run-segment-submit! selected-segment form overall
                                                                              selected setters reload-docs!))}))))))
