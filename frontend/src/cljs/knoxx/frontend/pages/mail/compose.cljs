(ns knoxx.frontend.pages.mail.compose
  "Human message composition preserves unsent work and the identity of uncertain retries."
  (:require [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.infra.navigation-guard :as navigation]
            [knoxx.frontend.pages.mail.api :as api]
            [knoxx.frontend.pages.mail.logic :as logic]))

(def ^:private modes
  [["inbox-only" "Mailbox only"] ["follow-up" "Follow-up turn"]
   ["steer" "Steer active turn"] ["event" "Event"]])
(def ^:private empty-draft {:target "" :content "" :mode "inbox-only"})
(def ^:private field-class "rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100")

(defn- ^:async send! [draft state! pending active? on-sent]
  (let [payload (update draft :target str/trim)
        operation (swap! pending logic/next-operation payload (str (random-uuid)))]
    (state! #(assoc % :busy true :error nil :notice nil))
    (try
      (let [result (await (api/send-message (assoc payload :operation_id (:id operation))))]
        (when @active?
          (reset! pending nil)
          (state! #(assoc % :draft empty-draft :notice (str "Message " (get-in result [:entry :status]) ".")))
          (on-sent)))
      (catch :default error
        (when @active? (state! #(assoc % :error (or (.-message error) (str error))))))
      (finally (when @active? (state! #(assoc % :busy false)))))))

(hx/defnc delivery-mode "Offer only delivery modes admitted for the current principal."
  [{:keys [mode allowed disabled change!]}]
  (d/label {:class-name "flex flex-col gap-1 text-sm"} "Delivery"
    (d/select {:aria-label "Delivery" :value mode :disabled disabled :class-name field-class
               :on-change #(change! (.. % -target -value))}
      (for [[value label] modes]
        (d/option {:key value :value value :disabled (not (contains? allowed value))} label)))))

(hx/defnc composer-fields "The human form maps directly to target, mode and content tool arguments."
  [{:keys [draft busy allowed change!]}]
  (d/div {:class-name "grid gap-3"}
    (d/label {:class-name "flex flex-col gap-1 text-sm"} "Recipient"
      (d/input {:aria-label "Recipient" :placeholder "actor:principal-id" :class-name field-class
                :value (:target draft) :disabled busy :required true
                :on-change #(change! :target (.. % -target -value))}))
    (d/p {:class-name "text-xs text-slate-400"} "Use actor:ID, session:ID or conversation:ID. Mailbox-only delivery needs an actor address.")
    (hx/$ delivery-mode {:mode (:mode draft) :allowed allowed :disabled busy :change! #(change! :mode %)})
    (d/label {:class-name "flex flex-col gap-1 text-sm"} "Message"
      (d/textarea {:aria-label "Message" :class-name field-class :rows 5 :required true
                   :value (:content draft) :disabled busy :on-change #(change! :content (.. % -target -value))}))))

(hx/defnc compose-message "Retain a draft on refusal or uncertain delivery; clear it only on confirmed success."
  [{:keys [capabilities on-sent]}]
  (let [[state state!] (hooks/use-state {:draft empty-draft})
        {:keys [draft busy error notice]} state
        pending (hooks/use-memo :once (atom nil)) active? (hooks/use-memo :once (atom true))
        allowed (set (:modes capabilities))
        permitted? (and (true? (:send capabilities)) (contains? allowed (:mode draft)))
        ready? (and permitted? (not busy) (not (str/blank? (:target draft))) (not (str/blank? (:content draft))))
        change! (fn [field value] (state! #(-> % (assoc-in [:draft field] value) (dissoc :notice))))]
    (navigation/use-navigation-guard! (or busy (not= draft empty-draft)))
    (hooks/use-effect :once (reset! active? true) #(reset! active? false))
    (d/form {:aria-label "Compose message" :class-name "m-6 mb-0 rounded-xl border border-slate-800 p-4"
             :on-submit (fn [event] (.preventDefault event) (when ready? (send! draft state! pending active? on-sent)))}
      (d/h2 {:class-name "mb-3 text-lg font-semibold"} "Compose message")
      (hx/$ composer-fields {:draft draft :busy busy :allowed allowed :change! change!})
      (when-not permitted? (d/p {:class-name "mt-2 text-sm text-amber-200"} "Your current mailbox capabilities do not allow this delivery mode."))
      (when error (d/p {:role "alert" :class-name "mt-2 text-sm text-red-200"} error))
      (when notice (d/p {:role "status" :class-name "mt-2 text-sm text-emerald-200"} notice))
      (d/button {:type "submit" :disabled (not ready?) :class-name "mt-3 rounded bg-blue-600 px-4 py-2 disabled:opacity-50"}
        (if busy "Sending…" "Send message")))))
