(ns knoxx.frontend.pages.settings.view
  "Settings page. Helix port of src/pages/SettingsPage.tsx. Exposed at
   window.knoxx.frontend.pages.settings.view.settings_page for the TS
   loader shim (OpsRoot is still TS-routed)."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.pages.settings.api :as api]))

(defn- ^:async check-status! [url cancelled set-status!]
  (let [ok? (await (api/ping url))]
    (when-not @cancelled
      (set-status! (if ok? :ok :error)))))

(hx/defnc status-row
  "Display one endpoint status and ignore completion after its URL changes."
  [{:keys [label url]}]
  (let [[status set-status!] (hooks/use-state :checking)]
    (hooks/use-effect
     [url]
     (let [cancelled (atom false)]
       (check-status! url cancelled set-status!)
       (fn [] (reset! cancelled true))))
    (d/div {:class-name "flex items-center justify-between py-1.5"}
           (d/span {:class-name "text-slate-400"} label)
           (d/span {:class-name (case status
                                  :checking "text-slate-500"
                                  :ok "text-emerald-400"
                                  "text-rose-400")}
                   (case status
                     :checking "…"
                     :ok "● OK"
                     "✕ Unavailable")))))

(defn- config-item [label value]
  (d/div
   (d/span {:class-name "text-slate-500"} label)
   (d/div {:class-name "font-medium text-slate-200 mt-0.5"} value)))

(defn- flag-item [label enabled? yes-text no-text no-class]
  (d/div
   (d/span {:class-name "text-slate-500"} label)
   (d/div {:class-name "font-medium mt-0.5"}
          (if enabled?
            (d/span {:class-name "text-emerald-400"} yes-text)
            (d/span {:class-name no-class} no-text)))))

(hx/defnc instance-card
  "Render the runtime identity flags and version provided by frontend config."
  [{:keys [config]}]
  (d/div {:class-name "rounded-xl border border-slate-800 bg-slate-900/80 p-5 space-y-4"}
         (d/h2 {:class-name "text-sm font-semibold text-slate-200 border-b border-slate-800 pb-2"}
               "Instance")
         (d/div {:class-name "grid gap-3 sm:grid-cols-2 text-sm"}
                (config-item "Environment" (or (:env config) "—"))
                (config-item "Version" (or (:version config) "—"))
                (flag-item "GitHub OAuth" (:github_enabled config) "Enabled" "Disabled" "text-rose-400")
                (flag-item "Auth Required" (:auth_required config) "Yes" "No" "text-slate-400"))))

(defn- ^:async load-config! [set-config! set-loaded!]
  (try
    (set-config! (await (api/frontend-config)))
    (catch :default _error nil)
    (finally (set-loaded! true))))

(hx/defnc ^:export settings-page
  "Load runtime configuration and render independently checked service statuses."
  []
  (let [[config set-config!] (hooks/use-state nil)
        [loaded set-loaded!] (hooks/use-state false)]
    (hooks/use-effect
     []
     (load-config! set-config! set-loaded!)
     nil)
    (if-not loaded
      (d/div {:class-name "p-6 text-sm text-slate-400"} "Loading…")
      (d/div {:data-page "settings"
              :class-name "p-6 max-w-3xl mx-auto space-y-6"}
             (d/div
              (d/h1 {:class-name "text-xl font-bold text-slate-100"} "Settings")
              (d/p {:class-name "mt-1 text-sm text-slate-400"} "Knoxx runtime configuration."))
             (hx/$ instance-card {:config config})
             (d/div {:class-name "rounded-xl border border-slate-800 bg-slate-900/80 p-5 space-y-4"}
                    (d/h2 {:class-name "text-sm font-semibold text-slate-200 border-b border-slate-800 pb-2"}
                          "Runtime Status")
                    (d/div {:class-name "space-y-2 text-sm"}
                           (hx/$ status-row {:label "Backend API" :url "/api/config"})
                           (hx/$ status-row {:label "Proxx Health" :url "/api/proxx/health"})
                           (hx/$ status-row {:label "Events" :url "/api/admin/config/events"})
                           (hx/$ status-row {:label "Agents" :url "/api/admin/agents/active?limit=1"})))
             (d/div {:class-name "rounded-xl border border-amber-500/20 bg-amber-500/5 p-4 text-xs text-amber-300"}
                    "Legacy settings (model selection, RAG config, forum mode) have been moved to the admin control plane or environment configuration.")))))
