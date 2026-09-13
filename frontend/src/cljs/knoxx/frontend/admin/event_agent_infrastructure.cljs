(ns knoxx.frontend.admin.event-agent-infrastructure
  "Runtime infrastructure panels: credentials, source defaults, event dispatch."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]))

(hx/defnc discord-credentials
  "Discord bot token configuration."
  [{:keys [configured draft-token on-update on-save can-manage saving-token]}]
  (d/div {:class-name "space-y-3 rounded-xl border border-slate-800 bg-slate-950/40 p-4"}
         (d/div
           (d/div {:class-name "text-sm font-semibold text-slate-100"} "Discord adapter credentials")
           (d/div {:class-name "text-xs text-slate-500"} "Discord-sourced jobs still use the shared bot token."))
         (d/label {:class-name "space-y-1 block"}
                  (d/div {:class-name "text-xs font-semibold uppercase tracking-wide text-slate-400"}
                         "Discord bot token")
                  (d/input {:type "password"
                            :value draft-token
                            :on-change #(on-update (.. % -target -value))
                            :disabled (or (not can-manage) saving-token)
                            :placeholder (if configured
                                           "Enter new token to replace"
                                           "Bot token from Discord Developer Portal")
                            :class-name (str "w-full rounded-lg border border-slate-800 bg-slate-950/70 "
                                             "px-3 py-2 text-sm text-slate-100 outline-none "
                                             "focus:border-sky-500 disabled:opacity-60")}))
         (d/button {:type "button"
                    :on-click on-save
                    :disabled (or (not can-manage) saving-token (empty? draft-token))
                    :class-name (str "inline-flex items-center justify-center rounded-lg bg-sky-600 "
                                     "px-4 py-2 text-sm font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60")}
                   (if saving-token "Saving…" "Save token"))))

(hx/defnc source-field
  "Edit one Discord source default through its existing update key."
  [{:keys [label value field on-update disabled]}]
  (d/label {:class-name "space-y-1 block"}
    (d/div {:class-name "text-xs font-semibold uppercase tracking-wide text-slate-400"} label)
    (d/input {:value (or value "") :on-change #(on-update field (.. % -target -value)) :disabled disabled
              :class-name "w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"})))

(hx/defnc source-defaults
  "Default source configuration values."
  [{:keys [^js discord-source on-update can-manage saving-control]}]
  (let [disabled (or (not can-manage) saving-control)]
    (d/div {:class-name "space-y-3 rounded-xl border border-slate-800 bg-slate-950/40 p-4"}
      (d/div (d/div {:class-name "text-sm font-semibold text-slate-100"} "Source defaults")
        (d/div {:class-name "text-xs text-slate-500"} "Default Discord values seed contract and custom jobs."))
      (d/div {:class-name "grid gap-3 md:grid-cols-3"}
        (hx/$ source-field {:label "Discord bot user ID" :field :bot-user-id :value (.-botUserId discord-source) :on-update on-update :disabled disabled})
        (hx/$ source-field {:label "Guild ID" :field :guild-id :value (.-guildId discord-source) :on-update on-update :disabled disabled})
        (hx/$ source-field {:label "Channel ID" :field :channel-id :value (.-channelId discord-source) :on-update on-update :disabled disabled})))))

(hx/defnc event-kind-fields
  "Select the source and event kind for an explicit dispatch."
  [{:keys [source-kind set-source-kind event-kind set-event-kind can-manage dispatching-event]}]
  (d/div {:class-name "grid gap-3 md:grid-cols-[0.8fr_1.2fr]"}
                  (d/label {:class-name "space-y-1 block"}
                           (d/div {:class-name "text-xs font-semibold uppercase tracking-wide text-slate-400"}
                                  "Source kind")
                           (d/select {:value source-kind
                                      :on-change #(set-source-kind (.. % -target -value))
                                      :disabled (or (not can-manage) dispatching-event)
                                      :class-name (str "w-full rounded-lg border border-slate-800 bg-slate-950/70 "
                                                       "px-3 py-2 text-sm text-slate-100 outline-none "
                                                       "focus:border-sky-500 disabled:opacity-60")}
                                     (d/option {:value "discord"} "discord")
                                     (d/option {:value "github"} "github")
                                     (d/option {:value "webhook"} "webhook")))
                  (d/label {:class-name "space-y-1 block"}
                           (d/div {:class-name "text-xs font-semibold uppercase tracking-wide text-slate-400"}
                                  "Event kind")
                           (d/input {:value event-kind
                                     :on-change #(set-event-kind (.. % -target -value))
                                     :disabled (or (not can-manage) dispatching-event)
                                     :placeholder "issues.opened"
                                     :class-name (str "w-full rounded-lg border border-slate-800 bg-slate-950/70 "
                                                      "px-3 py-2 text-sm text-slate-100 outline-none "
                                                      "focus:border-sky-500 disabled:opacity-60")}))))

(hx/defnc event-dispatch
  "Manual event dispatch form."
  [{:keys [can-manage dispatching-event on-dispatch]}]
  (let [[source-kind set-source-kind] (hooks/use-state "discord")
        [event-kind set-event-kind] (hooks/use-state "")
        [payload set-payload] (hooks/use-state "{}")]
    (d/div {:class-name "space-y-3"}
           (hx/$ event-kind-fields {:source-kind source-kind :set-source-kind set-source-kind :event-kind event-kind
                                     :set-event-kind set-event-kind :can-manage can-manage :dispatching-event dispatching-event})
           (d/label {:class-name "mt-3 block space-y-1"}
                    (d/div {:class-name "text-xs font-semibold uppercase tracking-wide text-slate-400"}
                           "Payload JSON")
                    (d/textarea {:value payload
                                 :on-change #(set-payload (.. % -target -value))
                                 :disabled (or (not can-manage) dispatching-event)
                                 :rows 7
                                 :class-name (str "w-full rounded-lg border border-slate-800 bg-slate-950/70 "
                                                  "px-3 py-2 text-xs font-mono text-slate-100 outline-none "
                                                  "focus:border-sky-500 disabled:opacity-60")}))
           (d/button {:type "button"
                      :on-click #(on-dispatch source-kind event-kind payload)
                      :disabled (or (not can-manage) dispatching-event)
                      :class-name (str "mt-3 inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 "
                                       "px-4 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60")}
                     (if dispatching-event "Dispatching…" "Dispatch")))))
