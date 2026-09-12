(ns knoxx.backend.infra.auth.session
  "Compatibility session entry points delegating all authentication to Axxium."
  (:require [axxium.infra.identity :as axxium]
            [knoxx.backend.extern.axxium :as transport]
            [knoxx.backend.infra.identity :as identity]))

(def COOKIE-NAME "Axxium owns the single browser identity cookie." "axxium_session")

(defn set-db-session-store!
  "Install the composed identity/policy context; no Knoxx session secret is created."
  [context]
  (identity/configure! context))

(defn resolve-auth-context
  "Resolve only a currently valid Axxium or principal-bound delegated credential."
  [request policy-context]
  (identity/resolve-request! policy-context request))

(defn create-session-hook
  "Create the shared cookie mutation Origin hook without minting session state."
  [policy-context]
  (transport/session-hook (get-in (identity/context policy-context) [:axxium-service :options :public-base-url])))

(defn clear-session-cookie
  "Expire the Axxium cookie through its owning native adapter."
  [reply _base-url]
  (transport/clear-cookie! reply))

(defn delete-session
  "Commit Axxium logout; legacy session IDs do not confer authority."
  [_session-id token]
  (axxium/logout! (:axxium-service (identity/context nil)) token))

(defn ensure-user-membership!
  "Retired email-based auto-provisioning refuses; callers need a verified principal."
  [& _]
  (throw (ex-info "A verified Axxium principal is required" {:status 401 :code "identity_required"})))
