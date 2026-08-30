(ns knoxx.frontend.pages.translations.review-history
  "Shared immutable-label history for legacy segments and resource splits."
  (:require [clojure.string :as str]
            [helix.core :refer [defnc]]
            [helix.dom :as d]))

(defn- wire-text
  [value]
  (cond
    (keyword? value) (name value)
    (some? value) (str value)
    :else nil))

(defn- reviewer-name
  [label]
  (or (some-> (:labeler_email label) wire-text not-empty)
      (some-> (:labeler_id label) wire-text not-empty)
      "Unknown reviewer"))

(defn- reviewed-at-text
  [label]
  (when-let [value (some-> (or (:reviewed_at label) (:ts label))
                            wire-text
                            not-empty)]
    (let [instant (js/Date. value)]
      (if (js/isNaN (.getTime instant))
        value
        (.toLocaleString instant)))))

(defn- score-text
  [label]
  (->> [(or (wire-text (:overall label))
            (wire-text (:review_status label))
            (wire-text (:status label)))
        (some-> (:adequacy label) wire-text (str " adequacy"))
        (some-> (:fluency label) wire-text (str " fluency"))
        (some-> (:terminology label) wire-text (str " terminology"))
        (some-> (:risk label) wire-text (str " risk"))]
       (remove str/blank?)
       (str/join " · ")))

(defn- authored-text
  [title text pre?]
  (when-let [value (some-> text wire-text not-empty)]
    (d/div {:class-name "mt-2 rounded bg-slate-900/40 px-2 py-1.5"}
           (d/p {:class-name "mb-1 text-[11px] font-medium uppercase tracking-wide text-slate-500"}
                title)
           (if pre?
             (d/pre {:class-name "whitespace-pre-wrap break-words text-xs text-slate-300"}
                    value)
             (d/p {:class-name "whitespace-pre-wrap break-words text-xs text-slate-300"}
                  value)))))

(defn- history-entry
  [label index]
  (d/div {:key (or (:review_id label)
                   (:id label)
                   (str "translation-review-history/" index))
          :class-name "rounded-md border border-slate-700/70 bg-slate-800/70 px-3 py-2"}
         (d/div {:class-name "mb-1 flex items-center justify-between gap-2"}
                (d/span {:class-name "text-xs font-medium text-slate-200"}
                        (reviewer-name label))
                (when-let [reviewed-at (reviewed-at-text label)]
                  (d/span {:class-name "text-[11px] text-slate-500"}
                          reviewed-at)))
         (when-let [summary (not-empty (score-text label))]
           (d/p {:class-name "text-xs text-slate-400"} summary))
         (authored-text "Correction" (:corrected_text label) true)
         (authored-text "Notes" (:editor_notes label) false)))

(defnc review-history
  "Render ordered append-only review facts without treating them as form state."
  [{:keys [labels title empty-copy]
    :or {title "Existing labels"}}]
  (let [entries (vec (or labels []))]
    (when (or (seq entries) (some? empty-copy))
      (d/div {:class-name "rounded-lg border border-slate-700 bg-slate-900/30 p-3"}
             (d/h5 {:class-name "mb-2 text-xs font-semibold text-slate-300"}
                   title)
             (if (seq entries)
               (d/div {:class-name "space-y-2"}
                      (for [[index label] (map-indexed vector entries)]
                        (history-entry label index)))
               (d/p {:class-name "text-xs text-slate-500"}
                    empty-copy))))))
