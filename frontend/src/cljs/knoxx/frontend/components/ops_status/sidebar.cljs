(ns knoxx.frontend.components.ops-status.sidebar
  "Sidebar system-usage + ingestion status. Helix port of
   src/components/SidebarOpsStatus.tsx. Exposed at
   window.knoxx.frontend.components.ops_status.sidebar.sidebar_ops_status
   for the TS loader shim (OpsRoot is still TS-routed)."
  (:require [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.components.ops-status.logic :as logic]
            [knoxx.frontend.lib.ws :as ws]
            [knoxx.frontend.pages.documents.api :as documents-api]))

(defn- metric-row [label value color]
  (d/div {:class-name "flex items-center justify-between rounded border border-slate-800 bg-slate-900/60 px-2 py-1"}
         (d/span {:class-name "text-slate-400"} label)
         (d/span {:class-name color} value)))

(defn- pct [v] (str (.toFixed (js/Number (or v 0)) 1) "%"))

(defnc usage-section [{:keys [samples]}]
  (let [latest (peek (vec samples))]
    (d/div
     (d/p {:class-name "text-[11px] uppercase tracking-wide text-slate-400"} "System Usage")
     (d/div {:class-name "mt-2 space-y-1 text-xs text-slate-300"}
            (metric-row "CPU" (pct (:cpu latest)) "text-cyan-300")
            (metric-row "RAM" (pct (:ram latest)) "text-amber-300")
            (metric-row "GPU" (pct (:gpu latest)) "text-violet-300"))
     (d/svg {:view-box "0 0 220 56"
             :class-name "mt-2 h-16 w-full rounded bg-slate-900/70"}
            (d/path {:d (logic/sparkline-path (mapv :cpu samples) 220 56)
                     :fill "none" :stroke "var(--token-colors-accent-blue)" :stroke-width "2"})
            (d/path {:d (logic/sparkline-path (mapv :ram samples) 220 56)
                     :fill "none" :stroke "var(--token-colors-accent-orange)" :stroke-width "1.8"})
            (d/path {:d (logic/sparkline-path (mapv :gpu samples) 220 56)
                     :fill "none" :stroke "var(--token-colors-accent-magenta)" :stroke-width "1.8"})))))

(defn- ingestion-pct [progress]
  (js/Number (or (:percentPrecise progress) (:percent progress) 0)))

(defnc ingestion-section [{:keys [ingestion]}]
  (let [progress (:progress ingestion)]
    (d/div
     (d/p {:class-name "text-[11px] uppercase tracking-wide text-slate-400"} "Ingestion")
     (cond
       (and (:active ingestion) progress)
       (hx/<>
        (d/p {:class-name "mt-1 text-xs text-slate-200"}
             (str (:processedChunks progress) " / " (:totalChunks progress)
                  " (" (.toFixed (ingestion-pct progress) 2) "%)"))
        (d/div {:class-name "mt-1 h-1.5 w-full rounded bg-slate-800"}
               (d/div {:class-name "h-1.5 rounded bg-cyan-400"
                       :style #js {:width (str (max 0 (min 100 (ingestion-pct progress))) "%")}}))
        (d/p {:class-name "mt-1 truncate text-[11px] text-slate-400"}
             (or (not-empty (:currentFile progress)) "Working...")))

       (and (:canResumeForum ingestion) progress)
       (d/p {:class-name "mt-1 text-xs text-amber-300"}
            (str "Paused at " (.toFixed (ingestion-pct progress) 2) "% (resumable)"))

       :else
       (d/p {:class-name "mt-1 text-xs text-slate-400"} "No active ingestion")))))

(defnc ^:export sidebar-ops-status []
  (let [[samples set-samples!] (hooks/use-state [])
        [ingestion set-ingestion!] (hooks/use-state nil)]
    (hooks/use-effect
     []
     (let [conn (ws/connect-stream
                 {:on-stats (fn [payload]
                              (set-samples!
                               #(logic/push-sample % (logic/stats->sample payload (.getTime (js/Date.))))))}
                 nil
                 nil)]
       (fn [] ((:disconnect conn)))))
    (hooks/use-effect
     []
     (let [poll! (fn ^:async poll! []
                   (try
                     (set-ingestion! (await (documents-api/ingestion-progress)))
                     (catch :default _
                       (set-ingestion! nil))))
           timer (js/setInterval poll! 2500)]
       (poll!)
       (fn [] (js/clearInterval timer))))
    (d/div {:class-name "border-t border-slate-700/60 p-3 space-y-3 overflow-y-auto"}
           ($ usage-section {:samples samples})
           ($ ingestion-section {:ingestion ingestion}))))
