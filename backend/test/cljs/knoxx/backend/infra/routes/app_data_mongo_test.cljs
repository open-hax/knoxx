(ns knoxx.backend.infra.routes.app-data-mongo-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.routes.app :as app]))

(deftest publication-source-event-queries-are-authorized-and-tenant-scoped
  (let [checks (atom [])
        ctx {:org-id "org-a"}
        body {:collection "events"
              :filter {:kind "docs"
                       :extra.org_id "org-b"}
              :limit 20}
        scoped (app/authorize-mongo-query!
                ctx
                (fn [actual-ctx permission]
                  (swap! checks conj [actual-ctx permission]))
                body)]
    (testing "the route requires the existing datalake read permission"
      (is (= [[ctx app/data-mongo-read-permission]] @checks)))
    (testing "the authenticated tenant overrides an attacker-supplied tenant"
      (is (= {:kind "docs" :extra.org_id "org-a"}
             (:filter scoped)))
      (is (= 20 (:limit scoped))))))

(deftest authenticated-event-query-without-an-org-fails-closed
  (let [error (try
                (app/authorize-mongo-query!
                 {:permissions [app/data-mongo-read-permission]}
                 (fn [_ctx _permission] true)
                 {:collection "events" :filter {}})
                nil
                (catch :default cause cause))]
    (is (= 403 (:status (ex-data error))))
    (is (= "org_scope_denied" (:code (ex-data error))))))

(deftest policy-disabled-local-mongo-query-remains-a-trusted-boundary
  (let [body {:collection "events" :filter {:kind "docs"}}
        checked? (atom false)]
    (is (= body
           (app/authorize-mongo-query!
            nil
            (fn [_ctx _permission] (reset! checked? true))
            body)))
    (is (false? @checked?))))
