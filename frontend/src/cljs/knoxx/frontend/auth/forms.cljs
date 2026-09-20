(ns knoxx.frontend.auth.forms
  "Shared accessible authentication form controls."
  (:require [helix.dom :as d]))
(def input-class "Form input styling." "w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100")
(def submit-class "Form action styling." "rounded-lg bg-cyan-700 px-4 py-2 text-sm font-medium text-white disabled:opacity-50")
(defn field "Render an explicitly named controlled field." [{:keys [id label value on-change placeholder auto-complete required] field-type :type}]
  (d/label {:html-for id :class-name "block space-y-2 text-sm text-slate-300"}
           (d/span label)
           (d/input {:id id :aria-label label :type (or field-type "text") :value value :class-name input-class
                     :placeholder placeholder :auto-complete auto-complete :required required
                     :on-change #(on-change (.. % -target -value))})))
(defn error-box "Keep failed actions visible." [message]
  (when (seq message) (d/p {:role "alert" :class-name "rounded-lg border border-red-800 bg-red-950/30 p-3 text-sm text-red-200"} message)))
(defn success-box "Report confirmed success." [message]
  (when (seq message) (d/p {:role "status" :class-name "rounded-lg border border-emerald-800 p-3 text-sm text-emerald-200"} message)))
