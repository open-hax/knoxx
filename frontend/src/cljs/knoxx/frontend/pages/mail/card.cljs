(ns knoxx.frontend.pages.mail.card
  "Mailbox entry card. Helix port of MailboxCard in src/pages/MailPage.tsx.
   Router-free: navigation arrives via :on-navigate so the card stays
   node-testable."
  (:require [helix.core :refer [$ defnc]]
            [helix.dom :as d]
            [knoxx.frontend.pages.mail.logic :as logic]))

(defn- small-button [{:keys [on-click loading variant label]}]
  (d/button
   {:type "button"
    :disabled loading
    :on-click on-click
    :class-name (str "rounded-md px-3 py-1.5 text-xs font-medium transition "
                     (if (= variant :ghost)
                       "border border-slate-700 text-slate-300 hover:bg-slate-800"
                       "border border-slate-600 bg-slate-800 text-slate-100 hover:bg-slate-700")
                     (when loading " opacity-60"))}
   (if loading "…" label)))

(defnc card-header
  [{:keys [entry box acking on-ack]}]
  (let [from (or (logic/record-string (:source entry) :actor-id :actorId :session-id :sessionId) "unknown")
        to (or (logic/record-string (:target entry) :actor-id :actorId :session-id :sessionId
                                    :conversation-id :conversationId) "unknown")
        mode (or (logic/record-string (:delivery entry) :mode) "message")
        attempts (let [a (:attempts (:delivery entry))] (when (number? a) a))]
    (d/div {:class-name "flex flex-wrap items-start justify-between gap-3"}
           (d/div {:class-name "min-w-0"}
                  (d/div {:class-name "flex flex-wrap items-center gap-2"}
                         (d/span {:class-name (str "rounded-full border px-2 py-0.5 text-xs font-semibold "
                                                   (logic/status-tone (:status entry)))}
                                 (:status entry))
                         (d/span {:class-name "rounded-full border border-slate-700 bg-slate-900 px-2 py-0.5 text-xs text-slate-300"}
                                 mode)
                         (when (some? attempts)
                           (d/span {:class-name "text-xs text-slate-500"} (str "attempts " attempts))))
                  (d/div {:class-name "mt-2 text-sm text-slate-300"}
                         (d/span {:class-name "text-slate-500"} "From ")
                         (d/span {:class-name "font-mono text-slate-100"} from)
                         (d/span {:class-name "mx-2 text-slate-600"} "→")
                         (d/span {:class-name "text-slate-500"} " To ")
                         (d/span {:class-name "font-mono text-slate-100"} to)))
           (when (and (= box "inbox") (not= "acknowledged" (:status entry)))
             (small-button {:on-click #(on-ack (:id entry))
                            :loading acking
                            :label "Acknowledge"})))))

(defn- meta-item [label value]
  (d/div
   (d/dt {:class-name "uppercase tracking-wide text-slate-600"} label)
   (d/dd (logic/format-date value))))

(defnc mailbox-card
  [{:keys [entry box acking on-ack on-navigate]}]
  (let [content-ref (js/JSON.stringify (clj->js (or (:contentRef entry) {})))
        links (logic/mailbox-links entry)]
    (d/article {:class-name "rounded-xl border border-slate-800 bg-slate-950/70 p-4 shadow-lg shadow-black/20"}
               ($ card-header {:entry entry :box box :acking acking :on-ack on-ack})
               (d/p {:class-name "mt-4 whitespace-pre-wrap rounded-lg border border-slate-800 bg-slate-900/70 p-3 text-sm leading-6 text-slate-100"}
                    (or (logic/record-string entry :preview)
                        "No preview available. Open the referenced run/event for full content."))
               (d/dl {:class-name "mt-3 grid gap-2 text-xs text-slate-500 md:grid-cols-3"}
                     (meta-item "Created" (:createdAt entry))
                     (meta-item "Delivered" (:deliveredAt entry))
                     (d/div
                      (d/dt {:class-name "uppercase tracking-wide text-slate-600"} "Content ref")
                      (d/dd {:class-name "truncate font-mono" :title content-ref} content-ref)))
               (when (seq links)
                 (d/div {:class-name "mt-3 flex flex-wrap gap-2"}
                        (for [{:keys [label path detail]} links]
                          (d/span {:key (str label ":" detail)}
                                  (small-button {:on-click #(on-navigate path)
                                                 :variant :ghost
                                                 :label label})))))
               (when (:lastError entry)
                 (d/div {:class-name "mt-3 rounded border border-red-500/30 bg-red-500/10 p-2 text-xs text-red-200"}
                        (:lastError entry))))))
