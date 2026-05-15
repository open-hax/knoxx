(ns knoxx.frontend.admin.event-agent-editor
  "Job editor form components.

   Operates on CLJS data (keyword keys)."
  (:require [helix.core :as hx :refer [$ defnc]]
            [helix.dom :as d]
            [knoxx.frontend.admin.event-agent-utils :as u]
            [knoxx.frontend.admin.event-agent-components :as c]))

(defnc text-field
  "Generic text input field."
  [{:keys [label value on-change disabled type placeholder]}]
  (d/label {:class-name "space-y-0.5 block"}
           (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-400"}
                  label)
           (d/input {:type (or type "text")
                     :value value
                     :on-change on-change
                     :disabled disabled
                     :placeholder placeholder
                     :class-name (str "w-full rounded-lg border border-slate-800 bg-slate-950/70 "
                                      "px-2.5 py-1.5 text-xs text-slate-100 outline-none "
                                      "focus:border-sky-500 disabled:opacity-60")})))

(defnc select-field
  "Generic select dropdown."
  [{:keys [label value on-change disabled options]}]
  (d/label {:class-name "space-y-0.5 block"}
           (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-400"}
                  label)
           (d/select {:value value
                      :on-change on-change
                      :disabled disabled
                      :class-name (str "w-full rounded-lg border border-slate-800 bg-slate-950/70 "
                                       "px-2.5 py-1.5 text-xs text-slate-100 outline-none "
                                       "focus:border-sky-500 disabled:opacity-60")}
                     (for [opt options]
                       (d/option {:key opt :value opt} opt)))))

(defnc checkbox-field
  "Generic checkbox field."
  [{:keys [label checked on-change disabled]}]
  (d/div {:class-name "space-y-0.5"}
         (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-400"}
                label)
         (d/label {:class-name "inline-flex w-full items-center gap-2 rounded-lg border border-slate-800 bg-slate-950/70 px-2.5 py-1.5 text-xs text-slate-200"}
                  (d/input {:type "checkbox"
                            :checked checked
                            :on-change on-change
                            :disabled disabled})
                  "Active")))

(defnc job-form
  "The full job editing form."
  [{:keys [job on-update can-manage saving-control]}]
  (let [disabled (or (not can-manage) saving-control)
        trigger (:trigger job)
        source (:source job)
        spec (:agentSpec job)]
    (d/div {:class-name "space-y-3"}
           (d/div {:class-name "grid gap-2 md:grid-cols-3"}
                  ($ checkbox-field {:label "Enabled"
                                     :checked (:enabled job)
                                     :on-change #(on-update :enabled (.. % -target -checked))
                                     :disabled disabled})
                  ($ select-field {:label "Trigger kind"
                                   :value (:kind trigger)
                                   :on-change #(on-update :trigger-kind (.. % -target -value))
                                   :disabled disabled
                                   :options ["cron" "event"]})
                  ($ select-field {:label "Source kind"
                                   :value (:kind source)
                                   :on-change #(on-update :source-kind (.. % -target -value))
                                   :disabled disabled
                                   :options ["discord" "github" "webhook"]}))
           (d/div {:class-name "grid gap-2 md:grid-cols-3"}
                  ($ text-field {:label "Source mode"
                                 :value (:mode source)
                                 :on-change #(on-update :source-mode (.. % -target -value))
                                 :disabled disabled})
                  ($ text-field {:label "Cadence (min)"
                                 :type "number"
                                 :value (:cadenceMinutes trigger)
                                 :on-change #(on-update :cadence (js/parseInt (.. % -target -value) 10))
                                 :disabled (or disabled (not= (:kind trigger) "cron"))})
                  ($ text-field {:label "Event kinds"
                                 :value (u/join-csv (:eventKinds trigger))
                                 :on-change #(on-update :event-kinds (u/split-csv (.. % -target -value)))
                                 :disabled (or disabled (not= (:kind trigger) "event"))
                                 :placeholder "mention, issues.opened"}))
           (d/div {:class-name "grid gap-2 md:grid-cols-3"}
                  ($ select-field {:label "Role"
                                   :value (:role spec)
                                   :on-change #(on-update :role (.. % -target -value))
                                   :disabled disabled
                                   :options ["agent" "reviewer" "dispatcher"]})
                  ($ text-field {:label "Model"
                                 :value (:model spec)
                                 :on-change #(on-update :model (.. % -target -value))
                                 :disabled disabled})
                  ($ select-field {:label "Thinking"
                                   :value (:thinkingLevel spec)
                                   :on-change #(on-update :thinking-level (.. % -target -value))
                                   :disabled disabled
                                   :options ["off" "minimal" "low" "medium" "high" "xhigh"]}))
           ($ text-field {:label "Job description"
                          :value (or (:description job) "")
                          :on-change #(on-update :description (.. % -target -value))
                          :disabled disabled})
           (d/label {:class-name "space-y-0.5 block"}
                    (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-400"}
                           "System prompt")
                    (d/textarea {:value (:systemPrompt spec)
                                 :on-change #(on-update :system-prompt (.. % -target -value))
                                 :disabled disabled
                                 :rows 3
                                 :class-name (str "w-full rounded-lg border border-slate-800 bg-slate-950/70 "
                                                  "px-2.5 py-1.5 text-xs text-slate-100 outline-none "
                                                  "focus:border-sky-500 disabled:opacity-60")})))))
