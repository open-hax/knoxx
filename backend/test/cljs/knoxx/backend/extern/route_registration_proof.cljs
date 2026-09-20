(ns knoxx.backend.extern.route-registration-proof
  "Verification-only ESM fixture for the actual production app route graph."
  (:require [knoxx.backend.infra.http-server :as http]
            [knoxx.backend.infra.routes.app :as app-routes]
            [knoxx.backend.law.route-registration-proof :as law]
            [knoxx.backend.runtime.state :as runtime-state]))

(defn- ^:async close-after-failure! [app error]
  (try
    (await (http/close! app))
    (catch :default cleanup-error
      (.error js/console "[route-proof] App cleanup failed" cleanup-error)))
  (throw error))

(defn ^:async create-app!
  "Register all app routes on real Fastify with a seeded auth-context seam.
   The caller supplies an owned workspace and loopback upstream; no bootstrap
   or persistent policy database is started by this fixture."
  [options]
  (let [config (law/assert-options! (js->clj options :keywordize-keys true))
        app (http/create-app!)
        routes (atom [])
        principal {:org-id "proof-org" :user-id "proof-user"
                   :membership-id "proof-member" :actor-id "proof-actor"}]
    (try
      (runtime-state/remember-context! nil config {:verification true})
      (await (http/register-default-plugins! app))
      (.addHook app "onRoute"
                (fn [route]
                  (swap! routes conj {:method (aget route "method")
                                      :url (aget route "url")})))
      (.addHook app "onRequest"
                (fn [request _reply done]
                  (aset request "__knoxxRequestContext"
                        (assoc principal :permissions
                               (if (= "allowed" (aget request "headers" "x-proof-principal"))
                                 ["datalake.query" "agent.memory.read"] [])))
                  (done)))
      (app-routes/register-routes! nil app config (atom []))
      (await (.ready app))
      #js {:app app :routes (clj->js @routes)}
      (catch :default error
        (await (close-after-failure! app error))))))
