(ns knoxx.frontend.pages.translations.view
  "Live translation inventory and revision-bound human review orchestration."
  (:require [helix.core :as hx]
            [helix.hooks :as hooks]
            [knoxx.frontend.pages.translations.commands :as commands]
            [knoxx.frontend.pages.translations.live-review :as live]
            [knoxx.frontend.pages.translations.logic :as logic]
            [knoxx.frontend.pages.translations.review-controller :as controller]
            [knoxx.frontend.pages.translations.split-review :as split-review]
            [knoxx.frontend.pages.translations.workspace :as workspace]))

(def document-actions "Preserve the existing public action component boundary." workspace/document-actions)

(defn- use-review-loads!
  [{:keys [project target-lang selected refresh setters
           document-load-seq manifest-load-seq detail-load-seq
           set-manifest! set-detail! set-seg-idx!]}]
  (hooks/use-effect
   [project target-lang]
   (let [load-id (controller/load-documents! project target-lang
                                             document-load-seq setters)]
     (fn [] (controller/cancel! document-load-seq load-id))))
  (hooks/use-effect
   [project]
   (let [load-id (controller/load-manifest! project manifest-load-seq set-manifest!)]
     (fn [] (controller/cancel! manifest-load-seq load-id))))
  (let [^js previous-work (hooks/use-ref nil)]
    (hooks/use-effect
   [selected project refresh]
   (if selected
     (let [same-work? (= (.-current previous-work) (logic/work-row-id selected))
           preserved (if same-work?
                       (assoc setters :set-seg-idx! (fn [_] nil) :set-form! (fn [_] nil))
                       setters)
           load-id (controller/load-detail! selected project detail-load-seq preserved)]
       (set! (.-current previous-work) (logic/work-row-id selected))
       (fn [] (controller/cancel! detail-load-seq load-id)))
     (do
       (controller/cancel! detail-load-seq (.-current ^js detail-load-seq))
       (set-detail! nil)
       (set-seg-idx! nil)
       nil)))))

(defn- use-workspace-state []
  (let [
        [project set-project!] (hooks/use-state "devel")
        [refresh set-refresh!] (hooks/use-state 0)
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
        ^js document-load-seq (hooks/use-ref 0)
        ^js manifest-load-seq (hooks/use-ref 0)
        ^js detail-load-seq (hooks/use-ref 0)
        setters {:set-loading! set-loading! :set-error! set-error! :set-documents! set-documents! :set-selected! set-selected! :set-project! set-project! :set-detail! set-detail! :set-detail-loading! set-detail-loading! :set-seg-idx! set-seg-idx! :set-form! set-form! :set-saving! set-saving! :set-notice! set-notice!}]
    {:refresh refresh :set-refresh! set-refresh! :project project :set-project! set-project! :target-lang target-lang :set-target-lang! set-target-lang! :documents documents :set-documents! set-documents! :selected selected :set-selected! set-selected! :detail detail :set-detail! set-detail! :seg-idx seg-idx :set-seg-idx! set-seg-idx! :form form :set-form! set-form! :manifest manifest :set-manifest! set-manifest! :loading loading :set-loading! set-loading! :detail-loading detail-loading :set-detail-loading! set-detail-loading! :saving saving :set-saving! set-saving! :notice notice :set-notice! set-notice! :error error :set-error! set-error! :show-config? show-config? :set-show-config! set-show-config! :document-load-seq document-load-seq :manifest-load-seq manifest-load-seq :detail-load-seq detail-load-seq :setters setters}))

(defn- mutation-props [{:keys [project target-lang selected set-seg-idx! form setters reload-docs! next-split-index guard-selection review-row review-segment submit!]}]
  {:on-export #(commands/run-export! project target-lang setters)
   :on-review #(commands/run-document-review! selected % project setters reload-docs!)
   :on-resource-review (fn [status] (submit! #(commands/run-resource-bulk-submit! review-row form status setters reload-docs!)))
   :on-publication-approval #(commands/run-publication-approval! selected setters reload-docs!)
   :on-translation-dispatch #(commands/run-translation-dispatch! selected setters reload-docs!)
   :on-segment-submit (fn
     [status]
     (submit!
       #(if (:resource_split review-segment) (commands/run-resource-split-submit! review-row review-segment form status setters reload-docs!) (commands/run-segment-submit! review-segment form status review-row project setters reload-docs!))))
   :on-segment-skip (when (some? next-split-index) #((guard-selection set-seg-idx!) next-split-index))})

(defn- layout-props [{:keys [project set-project! target-lang set-target-lang! documents selected set-selected! detail seg-idx set-seg-idx! form set-form! manifest loading detail-loading saving notice set-notice! error set-error! show-config? set-show-config! live-state guard-selection review-row review-segment]}]
  {:project project
   :set-project (guard-selection set-project!)
   :target-lang target-lang
   :set-target-lang (guard-selection set-target-lang!)
   :manifest manifest
   :show-config? show-config?
   :toggle-config #(set-show-config! not)
   :notice notice
   :dismiss-notice #(set-notice! nil)
   :error error
   :dismiss-error #(set-error! nil)
   :loading loading
   :documents documents
   :selected selected
   :set-selected (guard-selection set-selected!)
   :detail detail
   :detail-loading detail-loading
   :saving (or saving (:draft-stale? live-state))
   :seg-idx seg-idx
   :set-seg-idx (guard-selection set-seg-idx!)
   :draft-stale? (:draft-stale? live-state)
   :discard-draft (:discard-draft live-state)
   :review-row review-row
   :form form
   :set-form set-form!
   :selected-segment review-segment})

(hx/defnc translation-review-page
  "Connect live projections to candidate-bound reviewer input."
  []
  (let [state (use-workspace-state)
        {:keys [refresh set-refresh! project target-lang selected detail set-detail! seg-idx set-seg-idx! form set-form! set-manifest! document-load-seq manifest-load-seq detail-load-seq setters]} state
        reload-docs! #(controller/load-documents! project target-lang document-load-seq setters)
        selected-segment (logic/find-segment detail seg-idx)
        next-split-index (split-review/next-split-index (:segments detail) seg-idx)
        live-state (live/use-live-review! {:selected selected :selected-segment selected-segment :form form :set-form! set-form!})
        guard-selection (:guard-selection live-state)
        review-row (:review-row live-state)
        review-segment (:review-segment live-state)
        submit! (fn ^:async submit-command [command] (when-not (:draft-stale? live-state) (when (await (command)) ((:clear-draft! live-state)))))
        context (merge state {:reload-docs! reload-docs! :selected-segment selected-segment :next-split-index next-split-index :live-state live-state :guard-selection guard-selection :review-row review-row :review-segment review-segment :submit! submit!})]
    (live/use-changes! #(do (reload-docs!) (set-refresh! inc)))
    (use-review-loads!
      {:refresh refresh :project project
       :target-lang target-lang
       :selected selected
       :selected-segment selected-segment
       :setters setters
       :document-load-seq document-load-seq
       :manifest-load-seq manifest-load-seq
       :detail-load-seq detail-load-seq
       :set-manifest! set-manifest!
       :set-detail! set-detail!
       :set-seg-idx! set-seg-idx!
       :set-form! set-form!})
    (hx/$ workspace/translation-review-layout
          {& (merge (layout-props context) (mutation-props context))})))
