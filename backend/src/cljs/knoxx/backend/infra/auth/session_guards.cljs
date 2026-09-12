(ns knoxx.backend.infra.auth.session-guards
  "Compose the application's session resolver with its Fastify transport."
  (:require [knoxx.backend.extern.session-guard :as transport]
            [knoxx.backend.infra.auth.authz :as authz]))

(defn make-session-guard
  "Require resolved authority before allowing a protected route to execute."
  [runtime]
  (transport/pre-handler #(authz/resolve-request-context! runtime %) true))

(defn make-optional-session-guard
  "Attach nil on resolution failure for routes whose session is optional."
  [runtime]
  (transport/pre-handler #(authz/resolve-request-context! runtime %) false))
