(ns knoxx.frontend.admin.event-agent-sidebar
  "Sidebar components for the event agent runtime panel."
  (:require [helix.core :as hx :refer [$ defnc]]
            [helix.dom :as d]
            [knoxx.frontend.admin.event-agent-utils :as u]))

(defnc job-button
  "Single job button in the sidebar list."
  [{:keys [job runtime active on-select]}]
  (let [meta [(.. job -source -kind)
              (.. job -trigger -kind)
              (if (.-contractSourceId job) "contract" "custom")]
        runtime-label (if (.-running runtime)
                        "running"
                        (or (.-lastStatus runtime) "idle"))]
    (d/button
      {:type "button"
       :on-click on-select
       :aria-pressed active
       :class-name (str "w-full rounded-lg border px-2.5 py-2 text-left transition "
                        (if active
                          "border-sky-500/60 bg-sky-500/10 shadow-[inset_2px_0_0_0_rgba(56,189,248,0.9)]"
                          "border-slate-800 bg-slate-950/35 hover:border-slate-700 hover:bg-slate-950/70"))}
      (d/div {:class-name "flex items-center justify-between gap-2"}
             (d/div {:class-name "min-w-0 truncate text-sm font-medium text-slate-100"}
                    (.-name job))
             (d/span {:class-name (str "h-2 w-2 shrink-0 rounded-full "
                                       (if (.-enabled job) "bg-emerald-400" "bg-amber-400"))
                      :aria-hidden "true"}))
      (d/div {:class-name "mt-1 flex items-center justify-between gap-2 text-[11px] leading-4 text-slate-500"}
             (d/span {:class-name "min-w-0 truncate"}
                     (clojure.string/join " · " meta))
             (d/span {:class-name "shrink-0"}
                     (str (or (.-runCount runtime) 0) "r")))
      (d/div {:class-name "mt-1 flex items-center justify-between gap-2 text-[11px] leading-4"}
             (d/span {:class-name "min-w-0 truncate font-mono text-slate-400"}
                     (u/compact-text (.-id job) 28))
             (d/span {:class-name (str "shrink-0 uppercase tracking-wide "
                                       (cond
                                         (= (.-lastStatus runtime) "ok") "text-emerald-300"
                                         (= (.-lastStatus runtime) "error") "text-rose-300"
                                         (.-running runtime) "text-sky-300"
                                         :else "text-slate-500"))}
                     runtime-label)))))
