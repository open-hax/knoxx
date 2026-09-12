(ns knoxx.frontend.pages.mail.page
  "Actor mailbox page body. Helix port of src/pages/MailPage.tsx.
   Bridge-free and router-free (auth actor id and navigation arrive as
   props) so the page is node-testable; the route wrapper lives in
   mail.view."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.pages.mail.card :as card]
            [knoxx.frontend.pages.mail.compose :as compose]
            [knoxx.frontend.pages.mail.controller :as controller]
            [knoxx.frontend.pages.mail.logic :as logic]))

(def ^:private statuses ["all" "pending" "delivered" "failed" "acknowledged"])

(hx/defnc box-toggle "Choose the authenticated actor's inbox or outbox." [{:keys [box set-box]}]
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

(hx/defnc status-filter "Filter by the durable receipt status." [{:keys [status set-status]}]
  (d/label {:class-name "flex items-center gap-2 text-sm text-slate-400"}
           "Status"
           (d/select {:value status
                      :aria-label "Status"
                      :on-change #(set-status (.. % -target -value))
                      :class-name "rounded-md border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100"}
                     (for [candidate statuses]
                       (d/option {:key candidate :value candidate} candidate)))))

(hx/defnc mail-header "Mailbox navigation and transport status."
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
                   (hx/$ box-toggle {:box box :set-box set-box})
                   (hx/$ status-filter {:status status :set-status set-status})
                   (d/span {:class-name "text-sm text-slate-500"}
                           (if (= box "inbox")
                             (str (logic/unread-count entries) " not acknowledged")
                             (str (count entries) " sent entries"))))))

(hx/defnc mail-entries "Show current scoped entries and their available actions."
  [{:keys [loading error box entries acking-id on-ack on-navigate on-read can-ack?]}]
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
                   (hx/$ card/mailbox-card {:key (:id entry)
                                    :entry entry
                                    :box box
                                    :acking (= acking-id (:id entry))
                                    :can-ack? can-ack?
                                    :on-read on-read
                                    :on-ack on-ack
                                    :on-navigate on-navigate})))))

(hx/defnc full-message "Display only canonical full content, with visible loading and refusal states."
  [{:keys [selected reading message message-error close-message!]}]
  (when selected
    (d/section {:aria-label "Full message" :class-name "mx-6 mt-4 rounded-xl border border-slate-700 p-4"}
      (d/div {:class-name "flex justify-between"}
        (d/h2 {:class-name "text-lg font-semibold"} "Full message")
        (d/button {:type "button" :on-click close-message!} "Close message"))
      (when reading (d/p {:role "status"} "Loading full message…"))
      (when message-error (d/p {:role "alert"} message-error))
      (when message (d/p {:class-name "whitespace-pre-wrap break-words py-3"} (:content message))))))

(hx/defnc mail-page-body
  "Read, compose and acknowledge through the same scoped mailbox commands available to agents."
  [{:keys [initial-actor-id navigate]}]
  (let [{:keys [box status entries actor-id loading error acking-id refresh! set-box! set-status!
                ack! read! capabilities live-status durable] :as state} (controller/use-mailbox! initial-actor-id)]
    (d/div {:class-name "flex min-h-0 flex-1 flex-col bg-slate-950 text-slate-100"}
           (hx/$ mail-header {:actor-id actor-id :loading loading :on-refresh refresh!
                           :box box :set-box set-box! :status status :set-status set-status!
                           :entries entries})
           (d/p {:role "status" :class-name "px-6 pt-3 text-xs text-slate-400"}
             (str live-status " · " (if (true? durable) "Durable mailbox" "Mailbox persistence has not been confirmed")))
           (hx/$ compose/compose-message {:capabilities capabilities :on-sent refresh!})
           (hx/$ full-message {& state})
           (hx/$ mail-entries {:loading loading :error error :box box :entries entries
                            :acking-id acking-id :on-ack ack! :on-navigate navigate
                            :on-read read! :can-ack? (true? (:acknowledge capabilities))}))))
