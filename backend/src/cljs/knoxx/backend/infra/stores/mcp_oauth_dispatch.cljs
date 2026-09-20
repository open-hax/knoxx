(ns knoxx.backend.infra.stores.mcp-oauth-dispatch
  "Explicit selected OAuth provider; no implicit fallback after installation."
  (:require [knoxx.backend.shape.mcp-oauth-store :as protocol]))

(defonce provider (atom nil))

(defn install!
  "Install or explicitly clear a finite OAuth provider at composition time."
  [store]
  (when-not (or (nil? store) (satisfies? protocol/IMcpOAuthStore store))
    (throw (ex-info "Invalid OAuth provider" {:status 500 :code "mcp_oauth_provider_invalid"})))
  (reset! provider store))

(defn current "Read the installed provider without choosing another service." [] @provider)
