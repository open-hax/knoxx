(ns knoxx.frontend.pages.mail.page
  "Actor mailbox page body. Helix port of src/pages/MailPage.tsx.
   Bridge-free and router-free (auth actor id and navigation arrive as
   props) so the page is node-testable; the route wrapper lives in
   mail.view."
  (:require [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.pages.mail.api :as api]
            [knoxx.frontend.pages.mail.card :refer [mailbox-card]]
            [knoxx.frontend.pages.mail.logic :as logic]))

(def ^:private statuses ["all" "pending" "delivered" "failed" "acknowledged"])

(defnc box-toggle [{:keys [box set-box]}]
  (d/div {:class-name "rounded-lg border border-slate-800 bg-slate-900 p-1"}
         (for [candidate ["inbox" "outbox"]]
           (d/button {:key candidate
                      :type "button"
                      :on-click #(set-box candidate)
                      :class-name (str "rounded-md px-3 py-1.5 text-sm capitalize transition "
                                       (if (= box candidate)
                                         "bg-blue-600 text-white"
                                         "text-slate-400 hover:bg-slate-800 hover:text-slate-100"))}
                     candidate))))

(defnc status-filter [{:keys [status set-status]}]
  (d/label {:class-name "flex items-center gap-2 text-sm text-slate-400"}
           "Status"
           (d/select {:value status
                      :aria-label "Status"
                      :on-change #(set-status (.. % -target -value))
                      :class-name "rounded-md border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100"}
                     (for [candidate statuses]
                       (d/option {:key candidate :value candidate} candidate)))))

(defnc mail-header
  [{:keys [actor-id loading on-refresh box set-box status set-status entries]}]
  (d/header {:class-name "border-b border-slate-800 bg-slate-950/90 px-6 py-5"}
            (d/div {:class-name "flex flex-wrap items-center justify-between gap-4"}
                   (d/div
                    (d/div {:class-name "flex items-center gap-2"}
                           (d/h1 {:class-name "text-2xl font-semibold tracking-tight"} "Mail")
                           (d/span {:class-name "rounded-full border border-slate-700 bg-slate-900 px-2 py-0.5 text-xs text-slate-300"}
                                   (if actor-id (str "actor " actor-id) "actor unavailable")))
                    (d/p {:class-name "mt-1 max-w-3xl text-sm text-slate-400"}
                         "Actor mailbox for asynchronous Knoxx messages. Users are actors too, so agents can deliver work, alerts, and handoffs here without needing a live chat turn."))
                   (d/button {:type "button"
                              :disabled loading
                              :on-click on-refresh
                              :class-name "rounded-md border border-slate-600 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-100 transition hover:bg-slate-700 disabled:opacity-60"}
                             (if loading "…" "Refresh")))
            (d/div {:class-name "mt-5 flex flex-wrap items-center gap-3"}
                   ($ box-toggle {:box box :set-box set-box})
                   ($ status-filter {:status status :set-status set-status})
                   (d/span {:class-name "text-sm text-slate-500"}
                           (if (= box "inbox")
                             (str (logic/unread-count entries) " not acknowledged")
                             (str (count entries) " sent entries"))))))

(defnc mail-entries
  [{:keys [loading error box entries acking-id on-ack on-navigate]}]
  (d/main {:class-name "min-h-0 flex-1 overflow-y-auto p-6"}
          (when error
            (d/div {:class-name "mb-4 rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-200"} error))
          (when loading
            (d/div {:class-name "rounded-xl border border-slate-800 bg-slate-900 p-6 text-sm text-slate-400"} "Loading mailbox…"))
          (when (and (not loading) (empty? entries))
            (d/div {:class-name "rounded-xl border border-dashed border-slate-800 bg-slate-900/60 p-8 text-center text-sm text-slate-400"}
                   (str "No " box " entries match this filter yet.")))
          (d/div {:class-name "grid gap-4"}
                 (for [entry entries]
                   ($ mailbox-card {:key (:id entry)
                                    :entry entry
                                    :box box
                                    :acking (= acking-id (:id entry))
                                    :on-ack on-ack
                                    :on-navigate on-navigate})))))

(defnc mail-page-body
  "Full mailbox page given an initial actor id and a navigate fn."
  [{:keys [initial-actor-id navigate]}]
  (let [[box set-box!] (hooks/use-state "inbox")
        [status set-status!] (hooks/use-state "all")
        [entries set-entries!] (hooks/use-state [])
        [actor-id set-actor-id!] (hooks/use-state initial-actor-id)
        [loading set-loading!] (hooks/use-state false)
        [error set-error!] (hooks/use-state nil)
        [acking-id set-acking-id!] (hooks/use-state nil)
        load! (fn []
                (set-loading! true)
                (set-error! nil)
                (-> (api/list-mailbox box status)
                    (.then (fn [resp]
                             (set-entries! (:entries resp))
                             (set-actor-id! (or (:actor-id resp) initial-actor-id))))
                    (.catch (fn [^js err]
                              (set-error! (or (.-message err) "Mailbox unavailable"))
                              (set-entries! [])))
                    (.finally #(set-loading! false))))
        ack! (fn [id]
               (set-acking-id! id)
               (set-error! nil)
               (-> (api/acknowledge-entry id)
                   (.then load!)
                   (.catch (fn [^js err]
                             (set-error! (or (.-message err) "Failed to acknowledge mailbox entry"))))
                   (.finally #(set-acking-id! nil))))]
    (hooks/use-effect [box status] (load!) nil)
    (d/div {:class-name "flex min-h-0 flex-1 flex-col bg-slate-950 text-slate-100"}
           ($ mail-header {:actor-id actor-id :loading loading :on-refresh load!
                           :box box :set-box set-box! :status status :set-status set-status!
                           :entries entries})
           ($ mail-entries {:loading loading :error error :box box :entries entries
                            :acking-id acking-id :on-ack ack! :on-navigate navigate}))))
