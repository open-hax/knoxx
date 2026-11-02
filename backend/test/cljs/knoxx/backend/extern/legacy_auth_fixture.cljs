(ns knoxx.backend.extern.legacy-auth-fixture
  "Native legacy Fastify/cookie composition and reversible fixture-only process state."
  (:require [axxium.extern.identity-http :as identity-http]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.auth.session :as session]
            [knoxx.backend.infra.http :as http]
            [knoxx.backend.infra.routes.app :as app-routes]
            [knoxx.backend.infra.routes.auth :as auth-routes]
            [knoxx.backend.shape.app-shapes :as app-shapes]))

(defn ^:async with-environment!
  "Install only named test values and restore even absent environment keys on exit."
  [values run!]
  (let [previous (into {} (map (fn [[key _]] [key (aget js/process.env key)])) values)]
    (doseq [[key value] values]
      (if (some? value) (aset js/process.env key value) (js-delete js/process.env key)))
    (try (await (run!))
         (finally
           (doseq [[key value] previous]
             (if (some? value) (aset js/process.env key value) (js-delete js/process.env key)))))))

(defn ^:async with-session-state!
  "Isolate legacy module handles; all session cryptography and Mongo reads/writes remain real."
  [run!]
  (let [secret @#'session/session-secret-mem store @#'session/db-session-store
        old-secret @secret old-store @store]
    (reset! secret nil)
    (reset! store nil)
    (try (await (run!))
         (finally (reset! secret old-secret) (reset! store old-store)))))

(defn forget-session-key!
  "Discard only this proof's cached secret before recovering it from its actual Mongo config."
  []
  (reset! @#'session/session-secret-mem nil))

(defn ^:async app!
  "Register preceding authentication and actual auth-context routes with real cookie parsing.
   The optional legacy session-header hook remains off, as in default production config."
  [policy-context]
  (let [app (identity-http/create-app)]
    (try
      (await (identity-http/ensure-cookies! app))
      (auth-routes/register-auth-routes app {:policy-context policy-context :runtime {}})
      ;; The old route registrar starts recovery without awaiting it. Waiting for
      ;; its real idempotent store initialization makes fixture readiness explicit.
      (await (session/set-db-session-store! policy-context))
      (app-routes/api-auth-context! app {} {}
                                    {:route! app-shapes/route! :json-response! http/json-response!
                                     :with-request-context! authz/with-request-context!})
      (await (.ready app))
      app
      (catch :default cause (await (identity-http/close! app)) (throw cause)))))
