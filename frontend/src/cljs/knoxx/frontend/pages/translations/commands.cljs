(ns knoxx.frontend.pages.translations.commands
  "Native async mutation runners for existing translation command boundaries."
  (:require [knoxx.frontend.pages.translations.api :as api]
            [knoxx.frontend.pages.translations.logic :as logic]
            [knoxx.frontend.pages.translations.split-review :as split-review]))

(defn- ^:async saving! [{:keys [set-saving! set-error!]} command]
  (set-saving! true)
  (set-error! nil)
  (try (await (command)) true
       (catch :default error (set-error! (or (.-message ^js error) (str error))) false)
       (finally (set-saving! false))))

(defn ^:async run-segment-submit!
  "Record and reload a legacy segment review under the selected resource scope."
  [segment form overall selected project
   {:keys [set-notice! set-detail! set-form!] :as setters} reload-docs!]
  (await
   (saving! setters
            (fn ^:async perform []
              (let [scope (logic/legacy-review-scope selected project)]
                (await (api/submit-label (:id segment) scope (logic/prepare-label-payload form overall)))
                (set-notice! (str "Segment " (:segment_index segment) ": " overall))
                (set-detail! (await (api/get-document (:document_id selected) (:target_lang selected) scope)))
                (set-form! logic/default-label)
                (reload-docs!))))))

(defn ^:async run-resource-split-submit!
  "Record a candidate-set-bound split verdict and refresh its actual inventory."
  [selected segment form status {:keys [set-notice!] :as setters} reload-docs!]
  (await (saving! setters
                  (fn ^:async perform []
                    (await (api/submit-publication-split-review
                            (split-review/review-payload selected segment status form)))
                    (set-notice! (str "Split " (:segment_index segment) " " status "."))
                    (reload-docs!)))))

(defn ^:async run-resource-bulk-submit!
  "Apply one exact evaluation and verdict to the selected persisted candidate set."
  [selected form status {:keys [set-notice!] :as setters} reload-docs!]
  (await (saving! setters
                  (fn ^:async perform []
                    (await (api/submit-publication-bulk-review
                            (split-review/bulk-review-payload selected status form)))
                    (set-notice! (str "All splits " status "."))
                    (reload-docs!)))))

(defn ^:async run-document-review!
  "Review and reload the selected legacy document without widening its scope."
  [selected overall project {:keys [set-notice! set-detail!] :as setters} reload-docs!]
  (await
   (saving! setters
            (fn ^:async perform []
              (let [scope (logic/legacy-review-scope selected project)
                    result (await (api/review-document (:document_id selected) (:target_lang selected)
                                                       scope {:overall overall}))]
                (set-notice! (str "Document review: " overall " (" (:segments_reviewed result) " segments)"))
                (reload-docs!)
                (set-detail! (await (api/get-document (:document_id selected) (:target_lang selected) scope))))))))

(defn ^:async run-publication-approval!
  "Approve the exact translation revision, then expose its reconciliation result."
  [selected {:keys [set-notice!] :as setters} reload-docs!]
  (await
   (saving! setters
            (fn ^:async perform []
              (let [review (:publication_review selected)]
                (await (api/approve-publication-translation (logic/approval-request review)))
                (let [receipt (await (api/reconcile-publication (:publication review)))]
                  (set-notice! (str "Translation approved; publication reconciliation: "
                                   (or (:type receipt) "recorded") ".")))
                (reload-docs!))))))

(defn ^:async run-translation-dispatch!
  "Dispatch the selected real publication intent through the existing server command."
  [selected {:keys [set-notice!] :as setters} reload-docs!]
  (await
   (saving! setters
            (fn ^:async perform []
              (let [result (await (api/dispatch-publication-translation (:publication selected)))
                    outcome (or (some-> result :dispatched first :dispatch/outcome name)
                                (some-> result :dispatched first :outcome name) "recorded")]
                (set-notice! (str "Translation dispatch: " outcome "."))
                (reload-docs!))))))

(defn ^:async run-export!
  "Download the real reviewed translation export and release its object URL."
  [project target-lang {:keys [set-notice! set-error!]}]
  (try
    (let [text (await (api/sft-export {:project project :target-lang target-lang}))
          blob (js/Blob. #js [text] #js {:type "application/x-ndjson"})
          url (js/URL.createObjectURL blob)
          anchor (.createElement js/document "a")]
      (try (set! (.-href anchor) url)
           (set! (.-download anchor) (logic/sft-filename project target-lang))
           (.click anchor) (set-notice! "SFT export downloaded.")
           (finally (js/URL.revokeObjectURL url))))
    (catch :default error (set-error! (or (.-message ^js error) (str error))))))
