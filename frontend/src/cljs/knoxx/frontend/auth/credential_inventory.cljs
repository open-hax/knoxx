(ns knoxx.frontend.auth.credential-inventory
  "Human controls for Axxium-owned methods and guarded credential revocation."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.auth.api :as api]
            [knoxx.frontend.auth.forms :as forms]))
(defn navigate-to-login! "Reload after confirmed session termination." [reason]
  (set! (.-href js/window.location) (str "/login?reason=" reason)))
(defn use-inventory "Reload metadata after enrollment or retry; expose failed reads." [config refresh-key]
  (let [[inventory set-inventory!] (hooks/use-state nil) [error set-error!] (hooks/use-state nil)
        [revision set-revision!] (hooks/use-state 0)]
    (hooks/use-effect [config refresh-key revision]
      (let [active? (atom true)]
        (set-inventory! nil) (set-error! nil)
        ((fn ^:async load-inventory []
           (try (let [result (await (api/credential-inventory config))] (when @active? (set-inventory! result)))
                (catch :default exception (when @active? (set-error! (.-message exception)))))))
        #(reset! active? false)))
    {:inventory inventory :error error :refresh! #(set-revision! inc)}))
(defn use-revocation "Separate selection from mutation and navigate only after confirmed success." [config]
  (let [[selected set-selected!] (hooks/use-state nil) [busy set-busy!] (hooks/use-state false)
        [error set-error!] (hooks/use-state nil) [reauthenticate? set-reauthenticate!] (hooks/use-state false)
        operation! (fn ^:async perform [operation reason]
                     (set-busy! true) (set-error! nil)
                     (try (await (operation)) (navigate-to-login! reason)
                          (catch :default exception
                            (set-error! (.-message exception))
                            (when (= "reauthentication-required" (.-code exception)) (set-reauthenticate! true)))
                          (finally (set-busy! false))))]
    {:selected selected :busy busy :error error :reauthenticate? reauthenticate?
     :select! #(do (set-selected! %) (set-error! nil)) :cancel! #(do (set-selected! nil) (set-error! nil))
     :revoke! #(operation! (fn [] (api/revoke-credential config (:id selected))) "credentials-revoked")
     :sign-in! #(operation! api/logout "reauthentication-required")}))
(defn blocked-reason-message "Explain the provider's stable refusal codes." [reason]
  (get {"managed-bootstrap" "The server-managed bootstrap password cannot be removed here"
        "last-login-method" "Add another sign-in method before removing this one"} reason reason))
(hx/defnc credential-list "List public labels without exposing credential secrets." [{:keys [credentials busy fresh-login? select!]}]
  (d/ul {:class-name "space-y-3"}
        (for [{:keys [id method label revocable blockedReason] :as credential} credentials]
          (d/li {:key id :data-credential-method method :class-name "space-y-2 rounded border border-slate-800 p-3"}
                (d/p {:class-name "break-words text-sm text-slate-200"} label)
                (when blockedReason (d/p {:class-name "text-xs text-amber-200"} (blocked-reason-message blockedReason)))
                (d/button {:type "button" :class-name forms/submit-class :disabled (or busy fresh-login? (not revocable))
                           :on-click #(select! credential)} (str "Revoke " label))))))
(hx/defnc confirmation "Require a second explicit action before revocation." [{:keys [selected busy fresh-login? revoke! cancel!]}]
  (when selected
    (d/div {:role "group" :aria-label "Confirm credential revocation" :class-name "space-y-3 rounded border border-rose-800 p-3"}
           (d/p (str "Revoke " (:label selected) "? You will need another available sign-in method."))
           (d/button {:type "button" :class-name forms/submit-class :disabled (or busy fresh-login?) :on-click revoke!}
                     (if busy "Revoking…" "Confirm revocation"))
           (d/button {:type "button" :disabled busy :on-click cancel!} "Cancel revocation"))))
(hx/defnc inventory-controls "Render inventory, refusal, refresh and fresh-login controls." [{:keys [config refresh-key]}]
  (let [{:keys [inventory refresh!] inventory-error :error} (use-inventory config refresh-key)
        {:keys [busy error reauthenticate? sign-in! cancel!] :as actions} (use-revocation config)
        fresh-login? (or reauthenticate? (:reauthenticationRequired inventory))]
    (d/section {:aria-label "Manage sign-in credentials" :class-name "space-y-4 rounded-xl border border-slate-700 p-4"}
               (d/h3 {:class-name "font-semibold text-slate-100"} "Manage sign-in methods")
               (d/p {:class-name "text-sm text-slate-400"}
                    "Revoking a method removes this account's sign-in binding and ends its Axxium sessions. An external provider account remains unchanged.")
               (forms/error-box (or error inventory-error))
               (when fresh-login?
                 (d/div (d/p {:role "status"} "Sign in again to manage credentials.")
                        (d/button {:type "button" :class-name forms/submit-class :disabled busy :on-click sign-in!} "Sign in again")))
               (when (and (nil? inventory) (nil? inventory-error)) (d/p {:role "status"} "Loading sign-in credentials…"))
               (hx/$ credential-list {:credentials (:credentials inventory) :fresh-login? fresh-login? :& actions})
               (hx/$ confirmation {:fresh-login? fresh-login? :& actions})
               (d/button {:type "button" :disabled busy :on-click #(do (cancel!) (refresh!))} "Refresh credentials"))))
