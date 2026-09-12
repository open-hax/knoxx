(ns knoxx.frontend.pages.mail.controller
  "Mailbox reads reject stale completions and refresh open content on actor changes."
  (:require [helix.hooks :as hooks]
            [knoxx.frontend.pages.mail.api :as api]))

(defn- error-message [error] (or (.-message ^js error) (str error)))

(defn- ^:async load-list! [state! generation box status]
  (let [id (swap! generation inc)]
    (state! #(assoc % :loading true :error nil))
    (try
      (let [result (await (api/list-mailbox box status))]
        (when (= id @generation)
          (state! #(merge % result {:box box :status status :loading false}))))
      (catch :default error
        (when (= id @generation)
          (state! #(assoc % :error (error-message error) :loading false :entries [] :capabilities nil)))))))

(defn- ^:async load-message! [state! generation mailbox-id]
  (let [id (swap! generation inc)]
    (state! #(assoc % :selected mailbox-id :reading true :message nil :message-error nil))
    (try
      (let [entry (await (api/read-entry mailbox-id))]
        (when (= id @generation) (state! #(assoc % :message entry :reading false))))
      (catch :default error
        (when (= id @generation)
          (state! #(assoc % :message-error (error-message error) :message nil :reading false)))))))

(defn- ^:async acknowledge! [state! refresh! id]
  (state! #(assoc % :acking-id id :error nil))
  (try (await (api/acknowledge-entry id)) (refresh!)
       (catch :default error (state! #(assoc % :error (error-message error))))
       (finally (state! #(assoc % :acking-id nil)))))

(defn- use-mailbox-changes! [refresh! state! list-generation message-generation]
  (let [^js latest (hooks/use-ref refresh!)]
    (set! (.-current latest) refresh!)
    (hooks/use-effect
     :once
     (let [close! (api/subscribe! #((.-current latest)) #(state! (fn [value] (assoc value :live-status %))))]
       (fn [] (close!) (swap! list-generation inc) (swap! message-generation inc))))))

(defn- use-current-refresh! [state state! list-generation message-generation]
  (let [^js latest (hooks/use-ref state)]
    (set! (.-current latest) state)
    (hooks/use-callback :once
      (fn []
        (let [{:keys [box status selected]} (.-current latest)]
          (load-list! state! list-generation box status)
          (when selected (load-message! state! message-generation selected)))))))

(defn use-mailbox!
  "Return page state and the same read, acknowledge and invalidation paths for both interfaces."
  [initial-actor-id]
  (let [[state state!] (hooks/use-state {:box "inbox" :status "all" :entries []
                                       :actor-id initial-actor-id :loading true :live-status "Connecting…"})
        list-generation (hooks/use-memo :once (atom 0))
        message-generation (hooks/use-memo :once (atom 0))
        {:keys [box status]} state
        refresh! (use-current-refresh! state state! list-generation message-generation)
        change! (fn [field value] (swap! list-generation inc)
                  (state! #(assoc % field value :entries [] :capabilities nil :loading true)))]
    (hooks/use-effect [box status] (load-list! state! list-generation box status) nil)
    (use-mailbox-changes! refresh! state! list-generation message-generation)
    (assoc state :refresh! refresh! :set-box! #(change! :box %) :set-status! #(change! :status %)
           :read! #(load-message! state! message-generation %)
           :close-message! #(do (swap! message-generation inc) (state! (fn [value] (dissoc value :selected :message :message-error))))
           :ack! #(acknowledge! state! refresh! %))))
