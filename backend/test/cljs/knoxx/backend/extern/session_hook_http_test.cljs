(ns knoxx.backend.extern.session-hook-http-test
  "Real loopback HTTP proves the session hook advances Fastify and awaits hydration.

   Session persistence is a controlled policy-DB port; crypto, cookie parsing,
   the production hook, Fastify lifecycle and the HTTP client are real."
  (:require ["@fastify/cookie" :default fastify-cookie]
            ["fastify" :default fastify]
            [cljs.test :as test]
            [knoxx.backend.infra.auth.session :as session]
            [knoxx.backend.infra.db.policy :as policy]))

(defn- deferred []
  (let [resolve* (atom nil)]
    {:promise (js/Promise. (fn [resolve _reject] (reset! resolve* resolve)))
     :resolve! (fn [value] (@resolve* value))}))

(defn- ^:async request! [base path headers]
  (let [controller (js/AbortController.)
        timer (js/setTimeout #(.abort controller) 2000)]
    (try
      (let [response (await (js/fetch (str base path)
                                     #js {:headers (clj->js headers)
                                          :signal (.-signal controller)}))]
        {:status (.-status response)
         :cookie (.get (.-headers response) "set-cookie")
         :body (js->clj (await (.json response)) :keywordize-keys true)})
      (finally (js/clearTimeout timer)))))

(defn- ^:async with-server! [register! exercise!]
  (let [app (fastify #js {:logger false :forceCloseConnections true})]
    (try
      (await (.register app fastify-cookie))
      (.addHook app "onRequest" (session/create-session-hook {}))
      (doseq [path ["/health" "/api/auth/context" "/api/auth/session"]]
        (.get app path (fn [_request reply] (.send reply #js {:anonymous true}))))
      (register! app)
      (await (.listen app #js {:host "127.0.0.1" :port 0}))
      (await (exercise! app (str "http://127.0.0.1:" (.-port (.address (.-server app))))))
      (finally (await (.close app))))))

(test/deftest ^:async anonymous-health-and-auth-requests-complete-with-session-hook
  (await
   (with-server!
    (fn [_app])
    (^:async fn [_app base]
      (doseq [path ["/health" "/api/auth/context" "/api/auth/session"]]
        (let [response (await (request! base path {}))]
          (test/is (= 200 (:status response)) path)
          (test/is (= {:anonymous true} (:body response)) path)))))))

(defn- fixture-routes! [app]
  (.get app "/api/auth/fixture-login"
        (^:async fn [_request reply]
          (await (session/create-session-from-context!
                  reply "http://127.0.0.1"
                  {:user {:id "fixture-user" :email "fixture@open-hax.local"}
                   :org {:id "fixture-org" :slug "fixture-org"}
                   :membership {:id "fixture-membership"}}
                  {:email "fixture@open-hax.local"}))
          (.send reply #js {:ok true})))
  (.get app "/hydrate"
        (fn [request reply]
          (.send reply
                 #js {:email (aget (.-headers request) "x-knoxx-user-email")
                      :org (aget (.-headers request) "x-knoxx-org-slug")
                      :membership (aget (.-headers request) "x-knoxx-membership-id")}))))

(defn- ^:async exercise-cookie! [base stored* loaded gate]
  (let [login (await (request! base "/api/auth/fixture-login" {}))
        cookie (first (.split (:cookie login) ";"))
        response* (atom nil)
        pending (^:async fn []
                  (reset! response* (await (request! base "/hydrate" {"cookie" cookie}))))
        response (pending)]
    (test/is (= 200 (:status login)))
    (await (js/Promise.race #js [(:promise loaded) response]))
    (test/is (nil? @response*) "The route cannot respond before session lookup finishes")
    ((:resolve! gate) {:session @stored*})
    (await response)
    (test/is (= 200 (:status @response*)))
    (test/is (= {:email "fixture@open-hax.local" :org "fixture-org"
                 :membership "fixture-membership"}
                (:body @response*))
             "The handler observes all persisted identity headers")))

(test/deftest ^:async signed-cookie-hydration-finishes-before-the-route-handler
  (let [stored* (atom nil) loaded (deferred) gate (deferred)]
    (with-redefs [policy/recover-session-secret! (fn [_pool] (js/Promise.resolve (apply str (repeat 64 "a"))))
                  policy/create-session! (^:async fn [_pool payload] (reset! stored* payload))
                  policy/get-session-by-token! (fn [_pool token]
                                                 (test/is (= (:token @stored*) token))
                                                 ((:resolve! loaded) true)
                                                 (:promise gate))]
      (try
        (await (session/set-db-session-store! {:pool :fixture-session-store}))
        (await (with-server! fixture-routes!
                 (^:async fn [_app base] (await (exercise-cookie! base stored* loaded gate)))))
        ;; This fixture owns its temporary session store, not a real Mongo connection.
        (finally (await (session/set-db-session-store! nil)))))))
