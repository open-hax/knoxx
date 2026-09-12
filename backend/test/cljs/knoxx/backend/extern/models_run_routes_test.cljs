(ns knoxx.backend.extern.models-run-routes-test
  "Actual Fastify dispatch for legacy run URLs, with an explicit authenticated-context fixture."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.extern.http-server :as server]
            [knoxx.backend.extern.provider-recovery-fixture :as fixture]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.routes.models :as routes]
            [knoxx.backend.infra.stores.clio-run-store :as clio]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.shape.session-persistence :as protocol]))

(def at "2026-09-12T12:00:00.000Z")
(def run {:run_id "stored" :session_id "session" :conversation_id "conversation"
          :org_id "org" :user_id "user" :status "running" :created_at at :updated_at at
          :answer "from durable storage"})
(def ctx {:org-id "org" :user-id "user" :permissions ["agent.chat.use" "agent.runs.read_own"]})

(deftest ^:async legacy-run-urls-use-reopened-provider-and-never-heap
  (let [directory (fixture/temporary-directory) current-ctx (atom ctx)
        app (server/create-app! {:request-logging? false})]
    (try
      (let [options {:directory directory :clock! (constantly at) :instance-id "test"}]
        (await (protocol/put-run! (clio/open! options) run))
        (#'routes/register-run-routes! app {})
        (with-redefs [registry/session-store* (atom (clio/open! options))
                      state/runs* (atom {"stored" (assoc run :answer "stale heap")})
                      state/run-order* (atom ["heap-only"])
                      authz/with-request-context! (fn [_ _ _ handler] (handler @current-ctx))]
          (let [detail (await (.inject app #js {:method "GET" :url "/api/runs/stored"}))
                listing (await (.inject app #js {:method "GET" :url "/api/runs?limit=1"}))]
            (is (= 200 (.-statusCode detail)))
            (is (= "from durable storage" (.-answer (.json detail))))
            (is (= 200 (.-statusCode listing)))
            (is (= ["stored"] (mapv :run_id (:runs (js->clj (.json listing) :keywordize-keys true))))))
          (doseq [query ["0" "501" "bad" "1.5"]]
            (is (= 400 (.-statusCode (await (.inject app #js {:method "GET" :url (str "/api/runs?limit=" query)}))))))
          (reset! current-ctx (assoc ctx :org-id "other"))
          (is (= 403 (.-statusCode (await (.inject app #js {:method "GET" :url "/api/runs/stored"})))))
          (reset! current-ctx nil)
          (is (= 401 (.-statusCode (await (.inject app #js {:method "GET" :url "/api/runs/stored"})))))
          (is (= 401 (.-statusCode (await (.inject app #js {:method "GET" :url "/api/runs"})))))))
      (finally (await (server/close! app)) (fixture/remove! directory)))))
