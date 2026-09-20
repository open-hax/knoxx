(ns knoxx.frontend.pages.agents.editor
  "Structured and raw EDN views of an agent contract."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.pages.agents.fields :as fields]
            [knoxx.frontend.pages.agents.model :as model]
            [knoxx.frontend.pages.agents.runtime-model :as runtime-model]))

(hx/defnc contract-editor-header
  "Expose validation and capability-checked save actions."
  [{:keys [draft can-save saving validating parse-error on-save on-validate]}]
  (d/div {:class-name "shrink-0 border-b border-slate-800 px-3 py-2"}
         (d/div {:class-name "flex flex-col gap-2 lg:flex-row lg:items-center lg:justify-between"}
                (d/div {:class-name "min-w-0"}
                       (d/div {:class-name "flex flex-wrap items-center gap-2"}
                              (d/h2 {:class-name "truncate text-base font-semibold text-slate-100"} (or (:contract/id draft) "New agent"))
                              (d/span {:class-name (str "rounded-full px-2 py-0.5 text-[11px] "
                                                        (if (:enabled draft) "bg-emerald-500/10 text-emerald-300" "bg-slate-700/40 text-slate-400"))}
                                      (if (:enabled draft) "on" "off"))))
                (d/div {:class-name "flex flex-wrap gap-2"}
                       (d/button {:type "button" :on-click on-validate :disabled validating :title "Validate" :class-name (runtime-model/runtime-button-class :success)}
                                 (if validating "⏳" "✅ Check"))
                       (d/button {:type "button" :on-click on-save :disabled (or saving parse-error (not can-save))
                                  :title (when-not can-save "Requires platform.org.create permission")
                                  :class-name (runtime-model/runtime-button-class :primary)}
                                 (if saving "⏳ Save" "💾 Save"))))))

(hx/defnc contract-identity-section
  "Edit contract identity, version and enabled state."
  [{:keys [draft saving on-update]}]
  (d/section {:class-name "space-y-2 rounded-lg border border-slate-800 bg-slate-950/50 p-2"}
             (d/div {:class-name "text-sm font-semibold text-slate-100"} "Contract identity")
             (d/div {:class-name "grid gap-2 md:grid-cols-2"}
                    (hx/$ fields/text-field {:label "Contract id" :value (:contract/id draft) :on-change #(on-update assoc :contract/id %) :disabled saving})
                    (hx/$ fields/text-field {:label "Version" :type "number" :value (str (or (:contract/version draft) 1))
                                   :on-change #(on-update assoc :contract/version (model/parse-int-or % 1)) :disabled saving}))
             (hx/$ fields/checkbox-field {:label "Enabled" :checked (:enabled draft) :on-change #(on-update assoc :enabled %) :disabled saving})))

(hx/defnc contract-source-section
  "Edit the trigger and source declaration."
  [{:keys [draft saving on-update]}]
  (d/section {:class-name "space-y-2 rounded-lg border border-slate-800 bg-slate-950/50 p-2"}
             (d/div {:class-name "text-sm font-semibold text-slate-100"} "Activation and source")
             (d/div {:class-name "grid gap-2 md:grid-cols-3"}
                    (hx/$ fields/select-field {:label "Trigger kind" :value (model/keywordish->plain (:trigger-kind draft)) :options model/trigger-kind-options
                                     :on-change #(on-update assoc :trigger-kind (keyword %)) :disabled saving})
                    (hx/$ fields/select-field {:label "Source kind" :value (model/keywordish->plain (:source-kind draft)) :options model/source-kind-options
                                     :on-change #(on-update assoc :source-kind (keyword %)) :disabled saving})
                    (hx/$ fields/text-field {:label "Source mode" :value (model/keywordish->plain (:source-mode draft))
                                   :on-change #(on-update assoc :source-mode (keyword %)) :disabled saving}))
             (hx/$ fields/text-field {:label "Cadence minutes (:cadence-min)" :type "number" :value (str (or (:cadence-min draft) 5))
                            :on-change #(on-update assoc :cadence-min (model/parse-int-or % 5)) :disabled saving})))

(hx/defnc agent-spec-section
  "Edit roles, model and reasoning settings."
  [{:keys [agent role-options selected-roles saving on-update]}]
  (d/section {:class-name "space-y-2 rounded-lg border border-slate-800 bg-slate-950/50 p-2"}
             (d/div {:class-name "text-sm font-semibold text-slate-100"} "Agent spec")
             (hx/$ fields/role-picker {:roles role-options :selected selected-roles :on-change #(on-update model/with-selected-role-ids %) :disabled saving})
             (d/div {:class-name "grid gap-2 md:grid-cols-2"}
                    (hx/$ fields/text-field {:label "Model" :value (:model agent) :on-change #(on-update assoc-in [:agent :model] %) :disabled saving})
                    (hx/$ fields/select-field {:label "Thinking" :value (model/keywordish->plain (:thinking agent)) :options model/thinking-options
                                     :on-change #(on-update assoc-in [:agent :thinking] (keyword %)) :disabled saving}))))

(hx/defnc prompts-section
  "Edit system and task prompts without coercing structured values."
  [{:keys [prompts saving on-update]}]
  (d/section {:class-name "space-y-2 rounded-lg border border-slate-800 bg-slate-950/50 p-2"}
             (d/div {:class-name "text-sm font-semibold text-slate-100"} "Direct prompts")
             (hx/$ fields/textarea-field {:label "System prompt (:prompts :system)" :help "direct only" :value (model/prompt-display (:system prompts))
                                :on-change #(on-update assoc-in [:prompts :system] %) :disabled saving :rows 7})
             (hx/$ fields/textarea-field {:label "Task prompt (:prompts :task)" :help "direct only" :value (model/prompt-display (:task prompts))
                                :on-change #(on-update assoc-in [:prompts :task] %) :disabled saving :rows 5})))

(hx/defnc contract-edn-section
  "Display raw EDN and parse errors beside structured controls."
  [{:keys [edn-text parse-error saving on-raw-change]}]
  (d/section {:class-name "space-y-2 rounded-lg border border-slate-800 bg-slate-950/50 p-2"}
             (d/div {:class-name "flex items-center justify-between gap-2"}
                    (d/div {:class-name "text-sm font-semibold text-slate-100"} "Full contract EDN")
                    (d/div {:class-name "text-[11px] text-slate-500"} "escape hatch for every schema field"))
             (hx/$ fields/textarea-field {:label "EDN" :value edn-text :on-change on-raw-change :disabled saving :rows 18})
             (when parse-error
               (d/div {:class-name "rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-xs text-rose-200"} parse-error))))

(hx/defnc contract-editor-notices
  "Show loading, validation and persistence errors."
  [{:keys [notice error]}]
  (d/div
   (when notice
     (d/div {:class-name (str "mt-4 rounded-lg border px-3 py-2 text-sm "
                              (case (:tone notice) :success "border-emerald-500/30 bg-emerald-500/10 text-emerald-200" "border-rose-500/30 bg-rose-500/10 text-rose-200"))}
            (:text notice)))
   (when (seq error)
     (d/div {:class-name "mt-4 rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200"} error))))

(hx/defnc contract-editor
  "Compose identity, source, role, prompt and raw EDN controls."
  [{:keys [draft edn-text parse-error role-options can-save saving validating notice error
           on-update on-raw-change on-save on-validate]}]
  (let [agent (:agent draft)
        prompts (:prompts draft)
        selected-roles (model/selected-role-ids draft)]
    (d/div {:class-name "flex min-h-0 flex-1 flex-col bg-slate-950/40"}
           (hx/$ contract-editor-header {:draft draft :can-save can-save :saving saving :validating validating
                                      :parse-error parse-error :on-save on-save :on-validate on-validate})
           (d/div {:class-name "min-h-0 flex-1 overflow-y-auto p-2"}
                  (d/div {:class-name "grid gap-2 xl:grid-cols-[minmax(0,0.85fr)_minmax(0,1.15fr)]"}
                         (d/div {:class-name "space-y-2"}
                                (hx/$ contract-identity-section {:draft draft :saving saving :on-update on-update})
                                (hx/$ contract-source-section {:draft draft :saving saving :on-update on-update})
                                (hx/$ agent-spec-section {:agent agent :role-options role-options :selected-roles selected-roles
                                                       :saving saving :on-update on-update}))
                         (d/div {:class-name "space-y-2"}
                                (hx/$ prompts-section {:prompts prompts :saving saving :on-update on-update})
                                (hx/$ contract-edn-section {:edn-text edn-text :parse-error parse-error
                                                         :saving saving :on-raw-change on-raw-change})))
                  (hx/$ contract-editor-notices {:notice notice :error error})))))
