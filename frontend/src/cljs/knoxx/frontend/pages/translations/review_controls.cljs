(ns knoxx.frontend.pages.translations.review-controls
  "Reusable controls for the legacy and resource granular review cards."
  (:require [helix.core :refer [defnc]]
            [helix.dom :as d]
            [knoxx.frontend.pages.translations.review-contract :as review-contract]))

(defnc score-fields
  "Render the closed adequacy, fluency, terminology, and risk selectors."
  [{:keys [form on-change]}]
  (d/div {:class-name "grid gap-3"}
         (for [field review-contract/evaluation-fields]
           (d/label {:key (name field) :class-name "block text-sm"}
                    (d/span {:class-name
                             "mb-1 block font-medium capitalize text-slate-200"}
                            (name field))
                    (d/select
                     {:value (get form field)
                      :on-change #(on-change
                                   (assoc form field (.. % -target -value)))
                      :class-name
                      "w-full rounded-md border border-slate-600 bg-slate-800 px-3 py-2 text-sm text-slate-100"}
                     (for [value (review-contract/field-options field)]
                       (d/option {:key value :value value} value)))))))

(defn textarea
  "Render a labeled controlled textarea used by both granular review cards."
  [label value placeholder rows on-change]
  (d/label {:class-name "block text-sm"}
           (d/span {:class-name "mb-1 block font-medium text-slate-200"}
                   label)
           (d/textarea
            {:value (or value "")
             :on-change on-change
             :rows rows
             :placeholder placeholder
             :class-name
             "w-full rounded-md border border-slate-600 bg-slate-800 px-3 py-2 text-sm text-slate-100"})))
