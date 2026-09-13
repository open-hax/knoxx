(ns knoxx.backend.openplanner-scope-recovery-test
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.infra.clients.openplanner :as client]
            [knoxx.backend.infra.clients.openplanner-clio :as local]
            [knoxx.backend.infra.core-memory :as core]
            [knoxx.backend.infra.openplanner.memory :as memory]
            [knoxx.backend.infra.openplanner.scope :as scope]
            [knoxx.backend.infra.openplanner-event-sink :as sink]
            [knoxx.backend.infra.routes.tools.proxy :as proxy]
            [knoxx.backend.infra.stores.openplanner-message-source :as messages]))

(def config {:session-project-name "wiki" :openplanner-org-id "untrusted-ambient"})
(def ctx-a {:org-id "org-a" :user-id "user-a" :role-slugs ["system_admin"]
            :permissions ["agent.memory.read" "agent.memory.cross_session"]})
(def ctx-b (assoc ctx-a :org-id "org-b"))
(defn- ^:async refused [operation]
  (try (await (operation)) nil (catch :default error (ex-data error))))
(defn- ^:async fixture! [operation]
  (let [root (disk/temp-directory!)]
    (try
      (let [provider (local/open!
                      {:directory root :embedding-config {:embed-provider-base-url "http://127.0.0.1:9999"
                                                          :embed-provider-model "fixture" :embed-provider-dimensions 2}
                       :embed! (fn [texts] {:model "fixture" :dimensions 2 :vectors (mapv (constantly [1 0]) texts)})})]
        (await (client/events!
                 provider (mapv (fn [org] {:id (str "event-" org) :ts "2026-09-12T12:00:00Z"
                                           :source "knoxx" :kind "knoxx.message" :text (str "Private " org)
                                           :source_ref {:session "same-conversation" :project "wiki"}
                                           :meta {:role "user"} :extra {:org_id org :user_id "user-a"}})
                                ["org-a" "org-b"])))
        (with-redefs [client/client (fn ([_] provider) ([_ _] provider))]
          (await (operation provider))))
      (catch :default error (is false (str "Unexpected scoped OpenPlanner failure: " error)))
      (finally (fs/remove-tree! root)))))

(deftest ^:async context-overwrites-ambient-scope-and-missing-authority-refuses
  (is (= {:org_id "org-a" :project "wiki"} (scope/session-options (scope/scoped-config config ctx-a))))
  (is (= 401 (:status (await (refused #(scope/scoped-config config nil))))))
  (is (= 403 (:status (await (refused #(scope/scoped-config config {}))))))
  (is (= 403 (:status (await (refused #(scope/session-options {})))))))

(deftest ^:async core-memory-and-message-source-read-only-the-bound-organization
  (await (fixture!
    (^:async fn [_provider]
      (let [a (scope/scoped-config config ctx-a) b (scope/scoped-config config ctx-b)]
        (is (= ["event-org-a"] (mapv :id (await (core/fetch-openplanner-session-rows! a "same-conversation")))))
        (is (= ["event-org-b"] (mapv :id (await (core/fetch-openplanner-session-rows! b "same-conversation")))))
        (is (= 1 (count (await (messages/fetch-openplanner-messages! a "same-conversation")))))
        (is (= 1 (count (await (memory/openplanner-recent-session-summaries! a)))))
        (is (= ["event-org-a"] (mapv :id (:hits (await (memory/openplanner-memory-search! a {:query "Private" :k 10}))))))
        (is (= 403 (:status (await (refused #(core/fetch-openplanner-session-rows! {} "same-conversation")))))))))))

(deftest ^:async proxy-session-page-ignores-caller-tenant-aliases-and-enforces-capability
  (await (fixture!
    (^:async fn [_provider]
      (let [query {:org_id "org-b" :orgId "org-b" :project "other" :limit "10"}
            result (await (proxy/openplanner-session-page! config ctx-a query))]
        (is (= 1 (count (:rows result))))
        (is (= "org-a" (get-in result [:rows 0 :org_id])))
        (is (= "wiki" (get-in result [:rows 0 :project])))
        (is (= 401 (:status (await (refused #(proxy/openplanner-session-page! config nil query))))))
        (is (= 403 (:status (await (refused #(proxy/openplanner-session-page! config (assoc ctx-a :permissions [] :role-slugs []) query))))))
        (is (= 400 (:status (await (refused #(proxy/openplanner-session-page! config ctx-a {:limit "garbage"})))))))))))

(deftest ^:async interleaved-turn-diagnostics-keep-their-owning-run-scope
  (await (fixture!
    (^:async fn [provider]
      (let [run-a {:run_id "run-a" :org_id "org-a" :conversation_id "diagnostics-a"}
            run-b {:run_id "run-b" :org_id "org-b" :conversation_id "diagnostics-b"}
            event-a {:run_id "run-a" :type "started" :at 0}
            event-b {:run_id "run-b" :type "started" :at 0}]
        (await (sink/project! config run-a event-a))
        (await (sink/project! config run-b event-b))
        (await (sink/project! config run-a event-a))
        (is (= "org-a" (get-in (sink/envelope config run-a event-a) [:extra :org_id])))
        (is (= "1970-01-01T00:00:00.000Z" (:ts (sink/envelope config run-a event-a))))
        (is (= 1 (count (:rows (await (client/session! provider "diagnostics-a" {:org_id "org-a" :project "wiki"}))))))
        (is (= [] (:rows (await (client/session! provider "diagnostics-a" {:org_id "org-b" :project "wiki"})))))
        (is (= 409 (:status (await (refused #(sink/project! config run-a (assoc event-a :org_id "org-b"))))))))))))
