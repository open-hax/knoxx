(ns knoxx.backend.infra.routes.auth
  "Axxium owns login, credential and OAuth routes; Knoxx adds current policy context."
  (:require [knoxx.backend.extern.axxium :as transport]
            [knoxx.backend.infra.identity :as identity]))

(defn ^:async register-auth-routes
  "Register one identity owner, retaining Knoxx policy context response paths."
  [app {:keys [policy-context]}]
  (let [context (identity/context policy-context)]
    (await (transport/register! app (:axxium-service context)))
    (transport/register-context! app "/api/me" #(identity/resolve-request! context %))
    app))
