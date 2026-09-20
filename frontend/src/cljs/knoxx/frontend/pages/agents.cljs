(ns knoxx.frontend.pages.agents
  "Agent contracts and audit views composed from bounded workbench responsibilities."
  (:require ["@open-hax/knoxx-app-bridge" :as app]
            [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.components.layout.workbench :as layout]
            [knoxx.frontend.pages.agents.controller :as controller]
            [knoxx.frontend.pages.agents.editor :as editor]
            [knoxx.frontend.pages.agents.runtime-model :as runtime-model]
            [knoxx.frontend.pages.agents.runtime-view :as runtime-view]))

(hx/defnc agent-chat-panel
  "Keep the librarian conversation beside the selected contract."
  [{:keys [chat label storage-key]}]
  (hx/$ layout/WorkbenchPanel
    {:edge "right" :label label :storage-key storage-key :default-width 420 :min-width 360 :max-width 640
     :header (d/span {:class-name "text-xs font-semibold text-slate-100"} label)}
    (hx/$ app/ChatWorkspacePane {:controller chat :showFiles false :showFilesToggle false :showCanvasToggle false :onShowFiles #()})))

(hx/defnc tab-buttons
  "Select contract editing or historical agent audit."
  [{:keys [tab select-tab!]}]
  (d/div {:class-name "flex items-center gap-2"}
    (for [[value label] [["contracts" "🧠 Contracts"] ["audit" "📜 Audit"]]]
      (d/button {:key value :type "button" :on-click #(select-tab! value)
                 :class-name (str "rounded-md px-2 py-1 text-xs font-medium transition "
                                  (if (= tab value) "bg-sky-600 text-slate-50 hover:bg-sky-500" "bg-transparent text-slate-300 hover:bg-slate-800"))}
        label))))

(hx/defnc library-header
  "Expose local creation and library refresh commands."
  [{:keys [agents handle-new! load-agent-library! loading-library]}]
  (d/div {:class-name "flex min-w-0 flex-1 items-center gap-2"}
    (d/div {:class-name "flex min-w-0 items-center gap-2"}
      (d/span {:class-name "text-xs font-semibold text-slate-100"} "Agents")
      (d/span {:class-name "rounded bg-slate-900 px-1.5 py-0.5 text-[10px] text-slate-400"} (str (count agents))))
    (d/div {:class-name "ml-auto flex items-center gap-1"}
      (d/button {:type "button" :on-click handle-new! :title "New agent" :class-name (runtime-model/runtime-button-class :default)} "➕")
      (d/button {:type "button" :on-click load-agent-library! :disabled loading-library
                 :title "Refresh contracts and triggers" :class-name (runtime-model/runtime-button-class :default)}
        (if loading-library "⏳" "↻")))))

(defn- sidebar-props [state]
  (merge (select-keys state [:agents :triggers :pipelines :selected-id :selected-contract-id :runtime-status :runtime-error :runtime-notice
                             :running-job-id :toggling-runtime :resetting-runtime :chat])
         {:on-select (:set-selected-id state) :on-new (:handle-new! state) :loading-agents (:loading-library state)
          :loading-runtime (:runtime-loading state) :can-control (:can-control-runtime? state)
          :on-refresh-runtime (:load-runtime! state) :on-start-runtime (:handle-start-runtime! state)
          :on-stop-runtime (:handle-stop-runtime! state) :on-reset-runtime (:handle-reset-runtime! state)
          :on-run (:handle-run-job! state) :on-inspect-contract (:set-selected-id state) :audit? (= "audit" (:tab state)) :hide-header true}))

(hx/defnc library-panel
  "Render the agent library and its selected runtime projection."
  [{:keys [state]}]
  (hx/$ layout/WorkbenchPanel
    {:edge "left" :label "Agents" :storage-key "knoxx_agents_left" :default-width 320 :min-width 260 :max-width 520
     :header (hx/$ library-header {& state})}
    (hx/$ runtime-view/agent-workbench-sidebar {& (sidebar-props state)})))

(hx/defnc editor-header
  "Keep selected contract context visible above the active editor or audit."
  [{:keys [tab selected-id selected-contract-id select-tab!]}]
  (d/header {:class-name "flex shrink-0 items-center justify-between gap-3 border-b border-slate-800 bg-slate-950 px-4 py-2"}
    (d/div {:class-name "min-w-0"}
      (d/h1 {:class-name "truncate text-lg font-semibold text-slate-100"} (if (= tab "audit") "Agent Audit" "Agents"))
      (d/div {:class-name "truncate text-xs text-slate-500"}
        (if (= tab "audit") (str "Active and historical runs for " (or selected-contract-id "selected agent")) (or selected-id "New agent contract"))))
    (hx/$ tab-buttons {:tab tab :select-tab! select-tab!})))

(defn- editor-props [state]
  (merge (select-keys state [:draft :edn-text :parse-error :role-options :saving :validating :notice :error])
         {:can-save (:can-save-contracts? state) :on-update (:update-draft! state) :on-raw-change (:handle-raw-change state)
          :on-save (:handle-save! state) :on-validate (:handle-validate! state)}))

(hx/defnc workbench-content
  "Display the selected contract editor or interactive session audit."
  [{:keys [state]}]
  (let [{:keys [tab chat loading-contract]} state]
    (hx/$ layout/WorkbenchMain
      (d/div {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden"}
        (hx/$ editor-header {& state})
        (cond
          (= tab "audit")
          (d/div {:class-name "min-h-0 flex-1 overflow-hidden bg-slate-950"}
            (hx/$ app/ChatWorkspacePane {:controller chat :showFiles false :showFilesToggle false :showCanvasToggle true :onShowFiles #()}))
          loading-contract
          (d/div {:class-name "flex min-w-0 flex-1 items-center justify-center bg-slate-950/40 text-sm text-slate-400"} "Loading contract…")
          :else (hx/$ editor/contract-editor {& (editor-props state)}))))))

(hx/defnc AgentsPage
  "Compose the contract library, editor and role-aware agent conversation."
  []
  (let [state (controller/use-controller)]
    (d/div {:data-page "agents" :class-name "flex h-full min-h-0 min-w-0 flex-1 overflow-hidden bg-slate-950 text-slate-100"}
      (hx/$ layout/WorkbenchShell
        (hx/$ library-panel {:state state}) (hx/$ workbench-content {:state state})
        (when-not (= "audit" (:tab state))
          (hx/$ agent-chat-panel {:chat (:chat state) :label "Agent Chat" :storage-key "knoxx_agents_chat"}))))))
