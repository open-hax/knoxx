(ns knoxx.backend.lifecycle
  "Process-local lifecycle state shared across shadow-cljs hot reloads.

   Keep durable state here via defonce. The HTTP app can be closed/recreated on
   reload while runtime/config/policy handles survive in this namespace."
  (:require [knoxx.backend.http-server :as http-server]
            [knoxx.backend.realtime :as realtime]))

(defonce http-state*
  (atom {:app nil
         :runtime nil
         :config nil
         :policyDb nil
         :cookie-hook? false}))

(defn remember-context!
  [runtime config policyDb cookie-hook?]
  (swap! http-state* assoc
         :runtime runtime
         :config config
         :policyDb policyDb
         :cookie-hook? cookie-hook?)
  true)

(defn remember-app!
  [app]
  (swap! http-state* assoc :app app)
  app)

(defn context
  []
  (select-keys @http-state* [:runtime :config :policyDb :cookie-hook?]))

(defn current-app
  []
  (:app @http-state*))

(defn close-current-http!
  []
  (let [app (:app @http-state*)]
    (swap! http-state* assoc :app nil)
    ;; Realtime WebSocket clients and its stats interval are tied to Fastify.
    ;; Keep wider process services (Redis, Discord gateway, event runtime, MCP)
    ;; alive across hot reloads.
    (realtime/stop!)
    (if app
      (http-server/close! app)
      (js/Promise.resolve true))))
