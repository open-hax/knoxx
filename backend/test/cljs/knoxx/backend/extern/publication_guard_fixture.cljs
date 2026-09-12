(ns knoxx.backend.extern.publication-guard-fixture
  "Real Fastify injection and asynchronous barriers for publication route guard proofs."
  (:require ["fastify" :as Fastify]
            [knoxx.backend.extern.fastify :as http]
            [knoxx.backend.infra.auth.authz :as authz]))

(defn open!
  "Register actual application routes with an explicit test-owned authenticated context."
  [context register!]
  (let [app (Fastify #js {:logger false})
        handlers {:route! (fn [app method url handler] (http/route! app {:method method :url url :handler handler}))
                  :json-response! http/send-json! :ensure-permission! authz/ensure-permission!
                  :with-request-context! (fn [_runtime _request _reply operation] (operation @context))}]
    (.setErrorHandler ^js app (fn [error _request reply]
                               (http/send-json! reply (or (:status (ex-data error)) (http/error-status error 500))
                                                {:detail (ex-message error)})))
    (register! app handlers)
    app))

(defn ^:async request!
  "Exercise request decoding and JSON responses through actual Fastify injection."
  [app method path body]
  (let [response (await (.inject ^js app (clj->js {:method method :url path :payload body})))]
    {:status (.-statusCode ^js response) :body (js->clj (.json ^js response) :keywordize-keys true)}))

(defn close!
  "Release this test's Fastify resources."
  [app] (.close ^js app))

(defn barrier
  "Expose a deterministic asynchronous release point without sleeps."
  []
  (let [release (atom nil) pending (js/Promise. (fn [resolve _reject] (reset! release resolve)))]
    {:wait pending :release! #(@release true)}))
