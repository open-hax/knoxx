(ns knoxx.backend.extern.memory-routes-test
  "Memory routes through real Fastify with finite scoped provider responses."
  (:require [cljs.test :as t]
            [knoxx.backend.extern.http-server :as server]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.clients.openplanner :as planner]
            [knoxx.backend.infra.http :as http]
            [knoxx.backend.infra.openplanner-fixture :as fixture]
            [knoxx.backend.infra.routes.memory :as memory]
            [knoxx.backend.infra.stores.mongo-memory-sessions :as persistent-cache]
            [knoxx.backend.infra.stores.mongo-session-store :as sessions]
            [knoxx.backend.shape.app-shapes :as app-shapes]))

(defn- verified-context [_runtime request]
  (if (= "verified" (aget request "headers" "x-test-session"))
    {:org-id "request-org" :user-id "request-user"}
    (throw (ex-info "Authentication required" {:status 401 :code "identity_required"}))))

(def route-dependencies
  "Finite provider and native HTTP adapters used by the route fixture."
  {:route! app-shapes/route!
   :json-response! http/json-response!
   :error-response! http/error-response!
   :with-request-context! authz/with-request-context!
   :authorized-session-ids! (fn [_ _ ids] (set ids))
   :fetch-openplanner-session-rows! (fn [_ _] (throw (ex-info "No rows were seeded" {})))
   :session-matches-page-actor-filter? (fn [_ _ _ _] true)})

(t/deftest ^:async actual-session-route-refuses-anonymous-and-forwards-only-verified-tenant
  (let [app (server/create-app! {:request-logging? false})
        reads (atom [])
        active-reads (atom 0)
        client (fixture/client {:enabled? (constantly true)
                                :sessions! (fn [_ opts]
                                             (swap! reads conj opts)
                                             {:rows [] :has_more false})})]
    (memory/clear-memory-sessions-cache!)
    (try
      (with-redefs [authz/resolve-request-context! verified-context
                    planner/client (fn ([_] client) ([_ _] client))
                    persistent-cache/get-cache-entry! (fn ([_] nil) ([_ _] nil))
                    persistent-cache/set-cache-entry! (fn ([_ _] true) ([_ _ _] true))
                    sessions/list-active-session-ids
                    (fn ([] (swap! active-reads inc) [])
                        ([_] (swap! active-reads inc) []))]
        (memory/memory-sessions-route! app {} {:session-project-name "wiki"
                                              :openplanner-org-id "ambient-org"}
                                       route-dependencies)
        (let [anonymous (await (.inject app #js {:method "GET" :url "/api/memory/sessions"}))]
          (t/is (= 401 (.-statusCode anonymous)))
          (t/is (empty? @reads)))
        (let [response (await (.inject app #js {:method "GET"
                                               :url "/api/memory/sessions?org_id=other-org&limit=1"
                                               :headers #js {:x-test-session "verified"}}))
              payload (js->clj (.json response) :keywordize-keys true)]
          (t/is (= 200 (.-statusCode response)))
          (t/is (= [] (:rows payload)))
          (t/is (= [{:org_id "request-org" :project "wiki" :limit 10 :offset 0}] @reads))
          (t/is (= 1 @active-reads))))
      (finally
        (memory/clear-memory-sessions-cache!)
        (await (server/close! app))))))
