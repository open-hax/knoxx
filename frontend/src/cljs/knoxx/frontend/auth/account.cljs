(ns knoxx.frontend.auth.account
  "Authenticated identity and self-service sign-in management."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.auth.context :as context]
            [knoxx.frontend.auth.credentials :as credentials]
            [knoxx.frontend.auth.forms :as forms]))
(hx/defnc account-page "Show verified identity metadata beside its supported credential controls." []
  (let [auth (context/use-auth)]
    (d/div {:class-name "mx-auto max-w-3xl space-y-6 p-6 text-slate-200"}
           (d/h1 {:class-name "text-2xl font-semibold"} "Account & sign-in")
           (d/p (or (some-> auth .-user .-displayName) (some-> auth .-user .-email)))
           (d/p {:class-name "text-sm text-slate-400"} (some-> auth .-user .-email))
           (forms/error-box (.-error auth))
           (hx/$ credentials/credential-enrollment))))
