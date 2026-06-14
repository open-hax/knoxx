(ns knoxx.frontend.components.agent-audit.session-list
  "Agent audit session list (sidebar). Helix port of
   src/components/agent-audit/AgentAuditSessionList.tsx: contract-scoped
   merge of memory sessions + active runs, search, resume, 60s polling
   and 20-row infinite scroll. `controller` is the chat workspace
   controller JS object from the app bridge."
  (:require [clojure.string :as str]
            [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.components.ui :as ui]
            [knoxx.frontend.components.agent-audit.api :as api]
            [knoxx.frontend.components.agent-audit.logic :as logic]))

(def ^:private page-size 20)

(defn- scoped-contract-id [contract-id]
  (let [trimmed (some-> contract-id str/trim)]
    (when (and (seq trimmed) (not= trimmed "new-agent"))
      trimmed)))

(defn- run-load!
  [offset contract-id load-seq {:keys [set-loading! set-loading-more! set-sessions!
                                       set-next-offset! set-has-more! set-has-loaded!
                                       set-error!]}]
  (let [more? (pos? offset)
        seq-id (inc (.-current load-seq))]
    (set! (.-current load-seq) seq-id)
    (if more? (set-loading-more! true) (set-loading! true))
    (-> (js/Promise.all
         #js [(api/list-memory-sessions {:limit page-size
                                         :offset offset
                                         :contract-id (scoped-contract-id contract-id)})
              (api/list-operator-active-agents)])
        (.then (fn [[memory-page active-runs]]
                 (when (= seq-id (.-current load-seq))
                   (let [rows (or (:rows memory-page) [])
                         merged (logic/merge-sessions rows active-runs contract-id)]
                     (set-sessions! (fn [current]
                                      (if more?
                                        (logic/merge-session-pages current merged)
                                        merged)))
                     (set-next-offset! (+ offset (count rows)))
                     (set-has-more! (boolean (:has_more memory-page)))
                     (set-has-loaded! true)
                     (set-error! nil)))))
        (.catch (fn [^js err]
                  (when (= seq-id (.-current load-seq))
                    (set-error! (or (.-message err) (str err)))
                    (set-has-loaded! true))))
        (.finally (fn []
                    (when (= seq-id (.-current load-seq))
                      (if more? (set-loading-more! false) (set-loading! false))))))))

(defnc session-card-meta [{:keys [session]}]
  (d/div {:class-name "mt-1 flex flex-wrap gap-x-2 gap-y-1 text-[10px] text-slate-500"}
         (when (some? (:event_count session))
           (d/span (str (:event_count session) " ev")))
         (when (:model session)
           (d/span {:class-name "font-mono text-violet-300"} (:model session)))
         (when (:contract_id session)
           (d/span {:class-name "font-mono text-slate-400"} (:contract_id session)))
         (when (:actor_id session)
           (d/span {:class-name "font-mono text-slate-400"} (:actor_id session)))
         (when (:trigger_id session)
           (d/span {:class-name "font-mono text-amber-300"} (str "trigger " (:trigger_id session))))
         (when (:event_type session)
           (d/span {:class-name "font-mono text-cyan-300"} (str "event " (:event_type session))))
         (when (:schedule_id session)
           (d/span {:class-name "font-mono text-slate-400"} (str "schedule " (:schedule_id session))))
         (when (:last_ts session)
           (d/span (logic/format-maybe-date (:last_ts session))))))

(defnc session-card [{:keys [session ^js controller on-resume]}]
  (let [current? (boolean (or (and (.-conversationId controller)
                                   (= (.-conversationId controller) (:session session)))
                              (and (seq (or (.-sessionId controller) ""))
                                   (= (.-sessionId controller) (:active_session_id session)))))
        status (logic/session-status session)
        active? (or (= "active" (:auditSource session)) (boolean (:is_active session)))
        loading-this? (= (.-loadingMemorySessionId controller) (:session session))]
    (d/button {:type "button"
               :on-click #(on-resume (:session session))
               :aria-pressed current?
               :class-name (str "w-full rounded-lg border px-2 py-2 text-left transition "
                                (cond
                                  current? "border-sky-500/60 bg-sky-500/10"
                                  active? "border-emerald-500/35 bg-emerald-500/10 hover:border-emerald-400/50"
                                  :else "border-slate-800 bg-slate-950/35 hover:border-slate-700 hover:bg-slate-950/70"))}
              (d/div {:class-name "flex items-start justify-between gap-2"}
                     (d/div {:class-name "min-w-0"}
                            (d/div {:class-name "truncate text-xs font-semibold text-slate-100"}
                                   (or (not-empty (:title session)) (:session session)))
                            (d/div {:class-name "mt-0.5 truncate font-mono text-[10px] text-slate-500"}
                                   (:session session)))
                     (d/div {:class-name "flex shrink-0 flex-col items-end gap-1"}
                            (when current? ($ ui/badge {:variant :info} "Open"))
                            ($ ui/badge {:variant (:variant status)}
                               (if loading-this? "Loading" (:label status)))))
              ($ session-card-meta {:session session})
              (when (:latest_user_message session)
                (d/div {:class-name "mt-1 line-clamp-2 text-[10px] leading-4 text-slate-400"}
                       (:latest_user_message session))))))

(defnc list-header
  [{:keys [contract-id count loading query set-query on-refresh error]}]
  (d/div {:class-name "shrink-0 border-b border-slate-900/80 px-2 py-2"}
         (d/div {:class-name "flex items-center justify-between gap-2"}
                (d/div {:class-name "min-w-0"}
                       (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-500"}
                              "Sessions")
                       (d/div {:class-name "truncate font-mono text-[10px] text-slate-600"}
                              (or (not-empty contract-id) "all agents")))
                (d/div {:class-name "flex shrink-0 items-center gap-1"}
                       ($ ui/badge {} count)
                       ($ ui/button {:variant :ghost :size :sm :loading loading :on-click on-refresh} "↻")))
         (d/input {:aria-label "Search audit sessions"
                   :value query
                   :on-change #(set-query (.. % -target -value))
                   :placeholder "Search sessions…"
                   :class-name "mt-2 w-full rounded-md border border-slate-800 bg-slate-950/70 px-2 py-1.5 text-xs text-slate-100 outline-none focus:border-sky-500"})
         (when error
           (d/div {:class-name "mt-2 rounded border border-rose-500/30 bg-rose-500/10 px-2 py-1 text-[10px] text-rose-200"}
                  error))))

(defnc agent-audit-session-list
  [{:keys [^js controller built-in-contract-id class-name]}]
  (let [[sessions set-sessions!] (hooks/use-state [])
        [query set-query!] (hooks/use-state "")
        [loading set-loading!] (hooks/use-state true)
        [loading-more set-loading-more!] (hooks/use-state false)
        [has-loaded set-has-loaded!] (hooks/use-state false)
        [has-more set-has-more!] (hooks/use-state false)
        [next-offset set-next-offset!] (hooks/use-state 0)
        [error set-error!] (hooks/use-state nil)
        load-seq (hooks/use-ref 0)
        setters {:set-loading! set-loading! :set-loading-more! set-loading-more!
                 :set-sessions! set-sessions! :set-next-offset! set-next-offset!
                 :set-has-more! set-has-more! :set-has-loaded! set-has-loaded!
                 :set-error! set-error!}
        load! (fn [offset] (run-load! offset built-in-contract-id load-seq setters))
        filtered (let [q (logic/normalize-search query)]
                   (if (str/blank? q)
                     sessions
                     (filterv #(str/includes? (logic/session-search-text %) q) sessions)))
        on-scroll (fn [^js event]
                    (let [target (.-currentTarget event)
                          remaining (- (.-scrollHeight target) (.-scrollTop target) (.-clientHeight target))]
                      (when (and (< remaining 120) has-more (not loading) (not loading-more))
                        (load! next-offset))))]
    (hooks/use-effect
     [built-in-contract-id]
     (set-sessions! [])
     (set-next-offset! 0)
     (set-has-more! false)
     (set-has-loaded! false)
     (load! 0)
     (let [timer (js/setInterval #(load! 0) 60000)]
       (fn [] (js/clearInterval timer))))
    (d/div {:class-name (str "flex min-h-0 flex-1 flex-col overflow-hidden " (or class-name ""))}
           ($ list-header {:contract-id built-in-contract-id
                           :count (count filtered)
                           :loading loading
                           :query query :set-query set-query!
                           :on-refresh #(load! 0)
                           :error error})
           (d/div {:aria-label "Audit sessions list"
                   :class-name "min-h-0 flex-1 space-y-1 overflow-y-auto p-2"
                   :on-scroll on-scroll}
                  (when (and loading (not has-loaded))
                    (d/div {:class-name "p-2 text-xs text-slate-500"} "Loading sessions…"))
                  (when (and has-loaded (empty? filtered))
                    (d/div {:class-name "p-2 text-xs text-slate-500"} "No sessions match this agent."))
                  (for [session filtered]
                    ($ session-card {:key (:session session)
                                     :session session
                                     :controller controller
                                     :on-resume #(.resumeMemorySession controller %)}))
                  (when loading-more
                    (d/div {:class-name "p-2 text-xs text-slate-500"} "Loading more sessions…"))))))
