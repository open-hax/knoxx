(ns knoxx.frontend.pages.agents.fields
  "Accessible controls for structured contract editing."
  (:require [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]))

(hx/defnc text-field
  "Edit one textual contract property with its accessible field label."
  [{:keys [label value on-change disabled placeholder] input-type :type}]
  (d/label {:class-name "block space-y-1"}
           (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-400"}
                  label)
           (d/input {:type (or input-type "text")
                     :value (or value "")
                     :on-change #(on-change (.. % -target -value))
                     :disabled disabled
                     :placeholder placeholder
                     :class-name (str "w-full rounded-md border border-slate-800 bg-slate-950/70 px-2 py-1 "
                                      "text-xs text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60")})))

(hx/defnc textarea-field
  "Edit a multiline prompt or raw contract value."
  [{:keys [label value on-change disabled rows help]}]
  (d/label {:class-name "block space-y-1"}
           (d/div {:class-name "flex items-baseline justify-between gap-2"}
                  (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-400"}
                         label)
                  (when help
                    (d/div {:class-name "text-[10px] text-slate-500"} help)))
           (d/textarea {:value (or value "")
                        :on-change #(on-change (.. % -target -value))
                        :disabled disabled
                        :rows (or rows 4)
                        :class-name (str "w-full rounded-md border border-slate-800 bg-slate-950/70 px-2 py-1 "
                                         "font-mono text-xs leading-5 text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60")})))

(hx/defnc select-field
  "Choose one supported contract setting."
  [{:keys [label value on-change disabled options]}]
  (d/label {:class-name "block space-y-1"}
           (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-400"}
                  label)
           (d/select {:value (or value "")
                      :on-change #(on-change (.. % -target -value))
                      :disabled disabled
                      :class-name (str "w-full rounded-md border border-slate-800 bg-slate-950/70 px-2 py-1 "
                                       "text-xs text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60")}
                     (for [opt options]
                       (d/option {:key opt :value opt} opt)))))

(hx/defnc checkbox-field
  "Toggle a boolean contract property."
  [{:keys [label checked on-change disabled]}]
  (d/label {:class-name (str "inline-flex items-center gap-2 rounded-md border border-slate-800 bg-slate-950/70 "
                             "px-2 py-1 text-xs text-slate-200")}
           (d/input {:type "checkbox"
                     :checked (boolean checked)
                     :on-change #(on-change (.. % -target -checked))
                     :disabled disabled})
           label))

(hx/defnc selected-roles
  "Remove individual assigned roles while retaining the remaining order."
  [{:keys [selected on-change disabled]}]
(d/div {:class-name "flex min-h-8 flex-wrap gap-1 rounded-md border border-slate-800 bg-slate-950/70 p-1.5"}
                  (if (seq selected)
                    (for [role selected]
                      (d/span {:key role
                               :class-name "inline-flex items-center gap-1 rounded-full border border-sky-500/30 bg-sky-500/10 px-1.5 py-0.5 text-[10px] text-sky-100"}
                              role
                              (d/button {:type "button"
                                         :on-click #(on-change (vec (remove #{role} selected)))
                                         :disabled disabled
                                         :class-name "text-sky-200 hover:text-white disabled:opacity-50"}
                                        "×")))
                    (d/span {:class-name "text-xs text-slate-500"}
                            "No roles assigned."))))

(defn- available-roles [roles selected]
  (->> (concat roles selected) (remove str/blank?) distinct sort (remove (set selected))))

(hx/defnc role-picker
  "Add unselected role contracts while preserving assigned role order."
  [{:keys [roles selected on-change disabled]}]
  (let [[candidate set-candidate] (hooks/use-state "")
        available (available-roles roles selected)]
    (d/div {:class-name "space-y-2"}
           (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-400"}
                  "Roles from role contracts")
           (hx/$ selected-roles {:selected selected :on-change on-change :disabled disabled})
           (d/div {:class-name "flex gap-2"}
                  (d/select {:value candidate
                             :on-change #(set-candidate (.. % -target -value))
                             :disabled (or disabled (empty? available))
                             :class-name (str "min-w-0 flex-1 rounded-md border border-slate-800 bg-slate-950/70 px-2 py-1 "
                                              "text-xs text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60")}
                            (d/option {:value ""} "Add role…")
                            (for [role available]
                              (d/option {:key role :value role} role)))
                  (d/button {:type "button"
                             :disabled (or disabled (str/blank? candidate))
                             :on-click #(when-not (str/blank? candidate)
                                          (on-change (conj (vec selected) candidate))
                                          (set-candidate ""))
                             :class-name (str "rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-[11px] "
                                              "font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60")}
                            "➕")))))
