(ns knoxx.frontend.admin.event-agent-components
  "Small, reusable UI components for the event agent panel."
  (:require [helix.core :as hx :refer [$ defnc]]
            [helix.dom :as d]))

(defnc collapsible-panel
  "A details/summary panel that can be toggled open/closed."
  [{:keys [title description default-open children]}]
  (d/details
    {:open default-open
     :class-name "rounded-xl border border-slate-800 bg-slate-950/40 p-4 open:bg-slate-950/55"}
    (d/summary
      {:class-name "cursor-pointer list-none"}
      (d/div {:class-name "flex items-start justify-between gap-4"}
             (d/div
               (d/div {:class-name "text-sm font-semibold text-slate-100"} title)
               (when description
                 (d/div {:class-name "mt-1 text-xs text-slate-500"} description)))
             (d/span {:class-name "text-xs uppercase tracking-wide text-slate-500"} "toggle")))
    (d/div {:class-name "mt-4"} children)))

(defnc badge
  "Simple status badge."
  [{:keys [tone children]}]
  (let [tone-class (case tone
                     :success "border-emerald-500/30 bg-emerald-500/10 text-emerald-200"
                     :warn "border-amber-500/30 bg-amber-500/10 text-amber-200"
                     :danger "border-rose-500/30 bg-rose-500/10 text-rose-200"
                     :info "border-cyan-500/30 bg-cyan-500/10 text-cyan-200"
                     "border-slate-700 bg-slate-800 text-slate-200")]
    (d/span {:class-name (str "inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-medium " tone-class)}
            children)))

(defnc status-badge
  "Badge that shows a runtime status."
  [{:keys [status enabled running]}]
  (let [tone (cond
               (not enabled) :warn
               running :info
               (= status "ok") :success
               (= status "error") :danger
               :else :default)
        label (cond
                (not enabled) "disabled"
                running "running"
                status (str status)
                :else "idle")]
    ($ badge {:tone tone} label)))
